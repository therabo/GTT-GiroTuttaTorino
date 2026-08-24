package it.girotuttatorino.gtt.nfc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketProductTest {
    private val deviceUid = ByteCodec.fromHex("1122334455667788")

    @Test
    fun everyDisplayedGttTariffResolvesToItsProduct() {
        TicketProduct.entries.forEach { product ->
            val ticket = issuedToken(
                providerId = product.providerId,
                tariffId = product.tariffId,
            )

            assertEquals(product, TicketProduct.fromTicket(ticket))
            assertEquals(
                AepVToken.ProductIdentity(product.providerId, product.tariffId),
                AepVToken.productIdentity(ticket),
            )
            assertTrue(AepVToken.isIssuedToken(ticket, deviceUid))
        }
    }

    @Test
    fun unknownTariffRemainsUnsupported() {
        val ticket = issuedToken(providerId = 1, tariffId = 9999)

        assertEquals(AepVToken.ProductIdentity(1, 9999), AepVToken.productIdentity(ticket))
        assertNull(TicketProduct.fromTicket(ticket))
    }

    @Test
    fun productRecognitionDoesNotDependOnTheCitySampleLength() {
        val ticket = issuedToken(
            providerId = TicketProduct.TOUR_72.providerId,
            tariffId = TicketProduct.TOUR_72.tariffId,
            payloadSize = 80,
            trailerSize = 51,
        )

        assertEquals(228, ticket.size)
        assertTrue(AepVToken.basicShape(ticket))
        assertEquals(TicketProduct.TOUR_72, TicketProduct.fromTicket(ticket))
    }

    @Test
    fun inconsistentDeclaredLengthsAreRejected() {
        val ticket = issuedToken(providerId = 1, tariffId = 771)
        ticket[56] = 46

        assertFalse(AepVToken.basicShape(ticket))
        assertNull(TicketProduct.fromTicket(ticket))
    }

    private fun issuedToken(
        providerId: Int,
        tariffId: Int,
        payloadSize: Int = 64,
        trailerSize: Int = 47,
    ): ByteArray {
        val validationSize = 40
        return ByteArray(57 + payloadSize + validationSize + trailerSize).also { ticket ->
            ByteCodec.fromHex("414550560000000001F679D60101000001010000").copyInto(ticket, 0)
            writeBigShort(ticket, 20, 1)
            ticket[22] = 1
            deviceUid.copyInto(ticket, 23)
            ByteCodec.fromHex("008713D392142368").copyInto(ticket, 31)
            ticket[39] = 2
            ticket[40] = 1
            writeBigInt(ticket, 41, 3)
            writeBigInt(ticket, 45, 0x00FE0D4F)
            writeBigInt(ticket, 49, payloadSize)
            writeBigShort(ticket, 53, validationSize)
            writeBigShort(ticket, 55, trailerSize)
            ticket[57 + 20] = 1
            ticket[57 + 21] = providerId.toByte()
            writeBigShort(ticket, 57 + 22, tariffId)
            ticket[57 + payloadSize] = 1
        }
    }

    private fun writeBigShort(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 8).toByte()
        target[offset + 1] = value.toByte()
    }

    private fun writeBigInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }
}
