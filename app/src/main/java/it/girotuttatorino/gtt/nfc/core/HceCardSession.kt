package it.girotuttatorino.gtt.nfc.core

import java.io.ByteArrayOutputStream

internal class HceCardSession(
    private val backend: CardBackend,
) {
    private enum class Phase {
        IDLE,
        SELECTED,
        PROFILED,
        READING,
        READ_COMPLETE,
        WRITING,
        COMPLETE,
        FAILED,
    }

    private var phase = Phase.IDLE
    private var selectedAid = ""
    private var snapshot: ByteArray? = null
    private var correlation: ByteArray? = null
    private var expectedReadOffset = 0
    private var activeReadMode = -1
    private var activeOperationMode = -1
    private var expectedWriteOffset = 0
    private var activeWriteMode = -1
    private var writeBuffer: ByteArrayOutputStream? = null

    @Synchronized
    fun process(rawApdu: ByteArray?): ByteArray {
        val command = HceProtocol.parse(rawApdu)
            ?: return fail("MALFORMED", HceProtocol.SW_WRONG_LENGTH, rollback = false)

        if (command.instruction == 0xA4) return select(command)
        if (!backend.isValidationSessionActive()) {
            return fail("SESSION_INACTIVE", HceProtocol.SW_CONDITIONS, rollback = true)
        }
        if (command.cla != 0x80) return fail("BAD_CLA", HceProtocol.SW_CLA, rollback = false)

        return when (command.instruction) {
            0xA5 -> profile(command)
            0xA6 -> read(command)
            0xA7 -> write(command)
            else -> fail("BAD_INS", HceProtocol.SW_INS, rollback = false)
        }
    }

    @Synchronized
    fun deactivate() {
        writeBuffer?.takeIf { phase == Phase.WRITING && it.size() > 0 }?.let {
            backend.trace("ROLLBACK link-loss bytes=${it.size()}")
        }
        reset()
    }

    private fun select(command: HceProtocol.Command): ByteArray {
        reset()
        if (command.cla != 0x00) return fail("SELECT_CLA", HceProtocol.SW_CLA, rollback = false)
        if (command.parameter1 != 0x04 || command.parameter2 != 0x00) {
            return fail("SELECT_PARAMS", HceProtocol.SW_PARAMETERS, rollback = false)
        }
        val aid = ByteCodec.toHex(command.data)
        if (aid != HceProtocol.PRIVATE_AID_HEX && aid != HceProtocol.GTT_AID_HEX) {
            return fail("SELECT_UNKNOWN", HceProtocol.SW_NOT_FOUND, rollback = false)
        }
        if (!backend.isValidationSessionActive()) {
            return fail("SESSION_INACTIVE", HceProtocol.SW_CONDITIONS, rollback = false)
        }

        val ticket = backend.loadTicket()
        val uid = backend.deviceUid()
        if (!AepVToken.isToken(ticket, uid)) {
            return fail("NO_AEPV_TICKET", HceProtocol.SW_SECURITY, rollback = false)
        }

        selectedAid = aid
        phase = Phase.SELECTED
        backend.trace(
            "A4 SELECT ${if (aid == HceProtocol.GTT_AID_HEX) "GTT_ARMED" else "PRIVATE"} " +
                "bytes=${requireNotNull(ticket).size} hash=${ByteCodec.shortHash(ticket)}",
        )
        return HceProtocol.fci(aid, uid, ticket.size)
    }

    private fun profile(command: HceProtocol.Command): ByteArray {
        if (phase != Phase.SELECTED) return fail("A5_ORDER", HceProtocol.SW_CONDITIONS, rollback = true)
        if (command.parameter1 != 0x10 || command.parameter2 != 0 || command.data.size != 5) {
            return fail("A5_FORMAT", HceProtocol.SW_WRONG_LENGTH, rollback = true)
        }
        val cipher = ByteCodec.unsigned(command.data[2])
        val keyIndex = ByteCodec.unsignedShort(command.data[3], command.data[4])
        if (cipher != 0 || keyIndex != 0) {
            return fail("A5_PLAINTEXT_ONLY", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        phase = Phase.PROFILED
        backend.trace("A5 profile type=${ByteCodec.unsigned(command.data[0])}/${ByteCodec.unsigned(command.data[1])}")
        return ByteCodec.fromHex("801203019000")
    }

    private fun read(command: HceProtocol.Command): ByteArray {
        if (phase != Phase.PROFILED && phase != Phase.READING) {
            return fail("A6_ORDER", HceProtocol.SW_CONDITIONS, rollback = true)
        }
        if (command.data.size != 7) return fail("A6_LENGTH", HceProtocol.SW_WRONG_LENGTH, rollback = true)

        val operationMode = ByteCodec.unsigned(command.data[0])
        val readMode = ByteCodec.unsigned(command.data[1])
        var requestedLength = ByteCodec.unsigned(command.data[2])
        if (operationMode !in setOf(1, 2) || readMode !in setOf(0, 1) ||
            requestedLength > HceProtocol.MAX_FRAGMENT_SIZE
        ) {
            return fail("A6_MODE", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        if (requestedLength == 0) requestedLength = HceProtocol.MAX_FRAGMENT_SIZE
        val suppliedCorrelation = command.data.copyOfRange(3, 7)

        if (phase == Phase.PROFILED) {
            if (command.offset != 0) return fail("A6_FIRST_OFFSET", HceProtocol.SW_PARAMETERS, rollback = true)
            snapshot = backend.loadTicket()
            if (!AepVToken.isToken(snapshot, backend.deviceUid())) {
                return fail("A6_TICKET_CHANGED", HceProtocol.SW_SECURITY, rollback = true)
            }
            correlation = suppliedCorrelation
            expectedReadOffset = 0
            activeReadMode = readMode
            activeOperationMode = operationMode
            phase = Phase.READING
        }

        val currentSnapshot = requireNotNull(snapshot)
        if (readMode != activeReadMode || operationMode != activeOperationMode) {
            return fail("A6_MODE_CHANGED", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        if (readMode == 0 && (command.offset != 0 || requestedLength < currentSnapshot.size)) {
            return fail("A6_UNPAGINATED_CAPACITY", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        if (!ByteCodec.constantTimeEquals(correlation, suppliedCorrelation) || command.offset != expectedReadOffset) {
            return fail("A6_SEQUENCE", HceProtocol.SW_PARAMETERS, rollback = true)
        }

        val remaining = currentSnapshot.size - expectedReadOffset
        if (remaining <= 0) return fail("A6_PAST_END", HceProtocol.SW_PARAMETERS, rollback = true)
        val count = minOf(requestedLength, remaining)
        val fragment = currentSnapshot.copyOfRange(expectedReadOffset, expectedReadOffset + count)
        expectedReadOffset += count
        if (expectedReadOffset == currentSnapshot.size) phase = Phase.READ_COMPLETE
        backend.trace("A6 offset=${command.offset} bytes=$count corr=${ByteCodec.toHex(correlation)}")
        return HceProtocol.readResponse(requireNotNull(correlation), fragment)
    }

    private fun write(command: HceProtocol.Command): ByteArray {
        if (phase != Phase.READ_COMPLETE && phase != Phase.WRITING) {
            return fail("A7_ORDER", HceProtocol.SW_CONDITIONS, rollback = true)
        }
        if (command.data.size < 8) return fail("A7_LENGTH", HceProtocol.SW_WRONG_LENGTH, rollback = true)

        val completion = ByteCodec.unsigned(command.data[0])
        val writeMode = ByteCodec.unsigned(command.data[1])
        val count = ByteCodec.unsigned(command.data[2])
        val suppliedCorrelation = command.data.copyOfRange(3, 7)
        if (completion !in setOf(0, 1) || writeMode !in setOf(0, 1) || count == 0 ||
            count > HceProtocol.MAX_FRAGMENT_SIZE || command.data.size != 7 + count
        ) {
            return fail("A7_FORMAT", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        if (!ByteCodec.constantTimeEquals(correlation, suppliedCorrelation)) {
            return fail("A7_CORRELATION", HceProtocol.SW_PARAMETERS, rollback = true)
        }

        if (phase == Phase.READ_COMPLETE) {
            if (command.offset != 0) return fail("A7_FIRST_OFFSET", HceProtocol.SW_PARAMETERS, rollback = true)
            writeBuffer = ByteArrayOutputStream(requireNotNull(snapshot).size)
            expectedWriteOffset = 0
            activeWriteMode = writeMode
            phase = Phase.WRITING
        }
        if (writeMode != activeWriteMode) {
            return fail("A7_MODE_CHANGED", HceProtocol.SW_WRONG_DATA, rollback = true)
        }
        if (command.offset != expectedWriteOffset || expectedWriteOffset + count > AepVToken.MAX_LENGTH) {
            return fail("A7_SEQUENCE", HceProtocol.SW_PARAMETERS, rollback = true)
        }

        val isFinalChunk = completion == 1
        backend.trace(
            "A7 RX offset=${command.offset} bytes=$count complete=$completion mode=$writeMode " +
                "corr=${ByteCodec.toHex(suppliedCorrelation)}",
        )
        writeBuffer?.write(command.data, 7, count)
        expectedWriteOffset += count

        if (isFinalChunk) {
            val candidate = requireNotNull(writeBuffer).toByteArray()
            val original = requireNotNull(snapshot)
            if (!AepVToken.validValidatedTransition(original, candidate, backend.deviceUid())) {
                return fail("A7_AEPV_REJECTED", HceProtocol.SW_SECURITY, rollback = true)
            }
            if (!backend.compareAndCommit(original, candidate)) {
                return fail("A7_COMPARE_COMMIT_FAILED", HceProtocol.SW_CONDITIONS, rollback = true)
            }
            writeBuffer = null
            phase = Phase.COMPLETE
            backend.trace("A7 COMMIT corr=${ByteCodec.toHex(correlation)} hash=${ByteCodec.shortHash(candidate)}")
            backend.onCommitted()
        } else {
            backend.trace("A7 BUFFER total=$expectedWriteOffset")
        }
        return HceProtocol.writeAcknowledgement(requireNotNull(correlation))
    }

    private fun fail(event: String, status: ByteArray, rollback: Boolean): ByteArray {
        val pendingBytes = writeBuffer?.size() ?: 0
        if (rollback && pendingBytes > 0) {
            backend.trace("ROLLBACK $event bytes=$pendingBytes")
        } else {
            backend.trace("REJECT $event")
        }
        if (rollback) {
            writeBuffer = null
            phase = Phase.FAILED
        }
        return status.copyOf()
    }

    private fun reset() {
        phase = Phase.IDLE
        selectedAid = ""
        snapshot = null
        correlation = null
        expectedReadOffset = 0
        activeReadMode = -1
        activeOperationMode = -1
        expectedWriteOffset = 0
        activeWriteMode = -1
        writeBuffer = null
    }
}
