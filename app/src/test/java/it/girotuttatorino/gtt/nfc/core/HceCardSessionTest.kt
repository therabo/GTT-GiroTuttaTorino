package it.girotuttatorino.gtt.nfc.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HceCardSessionTest {
    private val correlation = ByteCodec.fromHex("01020304")

    @Test
    fun selectIsRejectedWhenTheTicketOverlayIsNotActive() {
        val backend = FakeCardBackend(active = false)

        val response = HceCardSession(backend).process(HceProtocol.select())

        assertArrayEquals(HceProtocol.SW_CONDITIONS, response)
        assertArrayEquals(backend.originalTicket, backend.storedTicket)
    }

    @Test
    fun privateAndGttAidProduceTheExpectedAepvFci() {
        val backend = FakeCardBackend()
        val privateSession = HceCardSession(backend)
        assertEquals(
            "6F28840E${HceProtocol.PRIVATE_AID_HEX}" +
                "A516BF0C13C70811223344556677885307010100D01001299000",
            ByteCodec.toHex(privateSession.process(HceProtocol.select())),
        )
        assertArrayEquals(ByteCodec.fromHex("801203019000"), privateSession.process(HceProtocol.profile()))

        val gttSession = HceCardSession(backend)
        assertEquals(
            "6F2A8410${HceProtocol.GTT_AID_HEX}" +
                "A516BF0C13C70811223344556677885307010100D01001299000",
            ByteCodec.toHex(gttSession.process(HceProtocol.select(HceProtocol.GTT_AID_HEX))),
        )
    }

    @Test
    fun paginatedReadReturnsTheCompleteAepvTicket() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)

        val readTicket = readTicket(session)

        assertArrayEquals(backend.originalTicket, readTicket)
        assertTrue(AepVToken.isToken(readTicket, backend.uid))
        assertFalse(AepVToken.isValidated(readTicket))
    }

    @Test
    fun activeSessionPresentsThePersistedValidatedTicket() {
        val backend = FakeCardBackend()
        val validated = validatedCopy(backend.originalTicket)
        backend.storedTicket = validated.copyOf()
        val session = selectedAndProfiledSession(backend)

        val readTicket = readTicket(session)

        assertArrayEquals(validated, readTicket)
        assertTrue(AepVToken.isValidated(readTicket))
    }

    @Test
    fun validationFieldsAreDecodedFromThePersistedVToken() {
        val ticket = validatedCopy(issuedToken(ByteCodec.fromHex("1122334455667788")))
        ByteCodec.fromHex(
            "010E005544A2005544A2AE110BE200B3A0960105000000000321" +
                "0000000800000000006400000000",
        ).copyInto(ticket, AepVToken.validationOffset(ticket))

        val validation = requireNotNull(AepVToken.validationInfo(ticket))

        assertEquals(0x0E, validation.flags)
        assertEquals(0x005544A2L, validation.firstValidationMinutesSince2016)
        assertEquals(0x005544A2L, validation.lastValidationMinutesSince2016)
        assertEquals(100, validation.minutesToGo)
        assertEquals(0, validation.ridesToGo)
        assertEquals(801L, validation.rideId)
        assertEquals(1, AepVToken.cityMetroRidesToGo(ticket))
    }

    @Test
    fun metroPayloadFlagConsumesTheSingleMetroAccessIndependentlyOfRideId() {
        val ticket = validatedCopy(issuedToken(ByteCodec.fromHex("1122334455667788")))
        ByteCodec.fromHex(
            "010E005544A2005544A2AE110BE200B3A0960105000000000321" +
                "0000000800000000006400000000",
        ).copyInto(ticket, AepVToken.validationOffset(ticket))
        val metroUsageByteOffset = 57 + 12
        ticket[metroUsageByteOffset] =
            (ByteCodec.unsigned(ticket[metroUsageByteOffset]) or 0x80).toByte()

        val validation = requireNotNull(AepVToken.validationInfo(ticket))

        assertEquals(801L, validation.rideId)
        assertEquals(0, AepVToken.cityMetroRidesToGo(ticket))
    }

    @Test
    fun activeValidatedTicketAcceptsACompleteValidatorWriteBack() {
        val backend = FakeCardBackend()
        val firstValidation = validatedCopy(backend.originalTicket)
        backend.storedTicket = firstValidation.copyOf()
        val session = selectedAndProfiledSession(backend)
        val presented = readTicket(session)
        val updated = presented.copyOf().also { ticket ->
            val validationOffset = AepVToken.validationOffset(ticket)
            ticket[validationOffset + 10] = 0x21
        }

        val response = session.process(
            HceProtocol.write(
                offset = 0,
                complete = true,
                correlation = correlation,
                fragment = updated,
                writeMode = 0,
            ),
        )

        assertTrue(HceProtocol.isSuccessful(response))
        assertEquals(1, backend.commitCount)
        assertArrayEquals(updated, backend.storedTicket)
    }

    @Test
    fun unpaginatedReadAndWriteMatchTheObservedValidatorFlow() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)
        val response = session.process(
            HceProtocol.read(
                offset = 0,
                length = 208,
                correlation = correlation,
                readMode = 0,
            ),
        )
        val issued = readPayload(response)
        assertArrayEquals(backend.originalTicket, issued)

        val validated = validatedCopy(issued)
        val writeResponse = session.process(
            HceProtocol.write(
                offset = 0,
                complete = true,
                correlation = correlation,
                fragment = validated,
                writeMode = 0,
            ),
        )

        assertTrue(HceProtocol.isSuccessful(writeResponse))
        assertEquals(1, backend.commitCount)
        assertArrayEquals(validated, backend.storedTicket)
    }

    @Test
    fun realShaped206ByteWriteBackCommitsOnlyAfterFinalPage() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)
        val issued = readTicket(session)
        val validated = validatedCopy(issued)

        assertTrue(HceProtocol.isSuccessful(session.process(writePage(0, false, validated, 0, 96))))
        assertArrayEquals(issued, backend.storedTicket)
        assertTrue(HceProtocol.isSuccessful(session.process(writePage(96, false, validated, 96, 192))))
        assertArrayEquals(issued, backend.storedTicket)
        assertTrue(HceProtocol.isSuccessful(session.process(writePage(192, true, validated, 192, 206))))

        assertEquals(1, backend.commitCount)
        assertEquals(1, backend.committedCallbackCount)
        assertEquals(206, backend.storedTicket.size)
        assertArrayEquals(validated, backend.storedTicket)
        assertTrue(AepVToken.isValidated(backend.storedTicket))
    }

    @Test
    fun fragmentedWriteModeZeroIsSupported() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)
        val validated = validatedCopy(readTicket(session))

        assertTrue(HceProtocol.isSuccessful(session.process(writePage(0, false, validated, 0, 96, 0))))
        assertTrue(HceProtocol.isSuccessful(session.process(writePage(96, false, validated, 96, 192, 0))))
        assertTrue(HceProtocol.isSuccessful(session.process(writePage(192, true, validated, 192, 206, 0))))

        assertEquals(1, backend.commitCount)
        assertArrayEquals(validated, backend.storedTicket)
    }

    @Test
    fun linkLossDuringWriteRollsBackThePendingValidation() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)
        val issued = readTicket(session)
        val validated = validatedCopy(issued)

        assertTrue(HceProtocol.isSuccessful(session.process(writePage(0, false, validated, 0, 96))))
        session.deactivate()

        assertEquals(0, backend.commitCount)
        assertArrayEquals(issued, backend.storedTicket)
        assertTrue(backend.events.any { it.startsWith("ROLLBACK link-loss") })
    }

    @Test
    fun changedDeviceUidIsRejectedWithoutCommit() {
        val backend = FakeCardBackend()
        val session = selectedAndProfiledSession(backend)
        val validated = validatedCopy(readTicket(session)).also { it[23] = 0x55 }

        session.process(writePage(0, false, validated, 0, 96))
        session.process(writePage(96, false, validated, 96, 192))
        val response = session.process(writePage(192, true, validated, 192, 206))

        assertArrayEquals(HceProtocol.SW_SECURITY, response)
        assertEquals(0, backend.commitCount)
        assertArrayEquals(backend.originalTicket, backend.storedTicket)
    }

    private fun selectedAndProfiledSession(backend: FakeCardBackend): HceCardSession =
        HceCardSession(backend).also { session ->
            assertTrue(HceProtocol.isSuccessful(session.process(HceProtocol.select())))
            assertTrue(HceProtocol.isSuccessful(session.process(HceProtocol.profile())))
        }

    private fun readTicket(session: HceCardSession): ByteArray =
        readPayload(session.process(HceProtocol.read(0, 96, correlation))) +
            readPayload(session.process(HceProtocol.read(96, 96, correlation))) +
            readPayload(session.process(HceProtocol.read(192, 16, correlation)))

    private fun readPayload(response: ByteArray): ByteArray {
        assertTrue(HceProtocol.isSuccessful(response))
        val fragmentLength = ByteCodec.unsigned(response[2])
        assertEquals(fragmentLength + 9, response.size)
        assertArrayEquals(correlation, response.copyOfRange(3, 7))
        return response.copyOfRange(7, 7 + fragmentLength)
    }

    private fun writePage(
        offset: Int,
        complete: Boolean,
        source: ByteArray,
        from: Int,
        to: Int,
        writeMode: Int = 1,
    ): ByteArray = HceProtocol.write(
        offset = offset,
        complete = complete,
        correlation = correlation,
        fragment = source.copyOfRange(from, to),
        writeMode = writeMode,
    )

    private fun issuedToken(uid: ByteArray): ByteArray = ByteArray(208).also { ticket ->
        ByteCodec.fromHex("41455056").copyInto(ticket, 0)
        ByteCodec.fromHex("01F679D6").copyInto(ticket, 8)
        ticket[12] = 1
        ticket[13] = 1
        ticket[16] = 1
        ticket[17] = 1
        ticket[21] = 1
        ticket[22] = 1
        uid.copyInto(ticket, 23)
        ByteCodec.fromHex("008713D392142368").copyInto(ticket, 31)
        ticket[39] = 2
        ticket[40] = 1
        ticket[44] = 3
        ByteCodec.fromHex("00FE0D4F").copyInto(ticket, 45)
        ticket[52] = 64
        ticket[54] = 40
        ticket[56] = 47
        // Real City issued tokens use 0x7F here: metro-used bit 0x80 is still clear.
        ticket[57 + 12] = 0x7F
        ticket[57 + 21] = 1
        ByteCodec.fromHex("0303").copyInto(ticket, 57 + 22)
        ticket[AepVToken.validationOffset(ticket)] = 1
    }

    private fun validatedCopy(issued: ByteArray): ByteArray = issued.copyOf(206).also { validated ->
        validated[44] = 4
        validated[56] = 45
        val offset = AepVToken.validationOffset(validated)
        validated[offset + 1] = 0x0E
        ByteCodec.fromHex("00553E3C00553E3C").copyInto(validated, offset + 2)
    }

    private inner class FakeCardBackend(active: Boolean = true) : CardBackend {
        val uid = ByteCodec.fromHex("1122334455667788")
        val originalTicket = issuedToken(uid)
        val events = mutableListOf<String>()
        var storedTicket = originalTicket.copyOf()
        var commitCount = 0
        var committedCallbackCount = 0
        var sessionActive = active

        override fun isValidationSessionActive(): Boolean = sessionActive

        override fun deviceUid(): ByteArray = uid.copyOf()

        override fun loadTicket(): ByteArray = storedTicket.copyOf()

        override fun compareAndCommit(expected: ByteArray, replacement: ByteArray): Boolean {
            if (!storedTicket.contentEquals(expected)) return false
            storedTicket = replacement.copyOf()
            commitCount += 1
            return true
        }

        override fun onCommitted() {
            committedCallbackCount += 1
        }

        override fun trace(event: String) {
            events += event
        }
    }
}
