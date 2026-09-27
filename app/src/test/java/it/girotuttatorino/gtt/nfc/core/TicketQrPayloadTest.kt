package it.girotuttatorino.gtt.nfc.core

import com.google.zxing.DecodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.Decoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketQrPayloadTest {
    @Test
    fun formatFourContainsTheCurrentValidatedPayloadAndMetadata() {
        val token = token(validated = true)
        val expected = ByteCodec.concat(
            byteArrayOf(86, 84, 83, 81, 1, 0, 0, 0, 0, 0, 0, 0),
            byteArrayOf(0x65, 0x43, 0x21, 0x10),
            token.copyOfRange(4, 12),
            byteArrayOf(0x12, 0x34),
            byteArrayOf(0, 0x56),
            token.copyOfRange(23, 31),
            token.copyOfRange(31, 39),
            byteArrayOf(2),
            byteArrayOf(0, 3),
            byteArrayOf(0x11, 0x22, 0x33),
            ByteArray(6),
        )

        assertArrayEquals(
            expected,
            TicketQrPayload.fromValidatedToken(token, 0x65432110),
        )
    }

    @Test
    fun unvalidatedAndInvalidTokensAreNotExposed() {
        assertNull(TicketQrPayload.fromValidatedToken(token(validated = false), 123))
        assertNull(TicketQrPayload.fromValidatedToken(ByteArray(20), 123))
        assertNull(TicketQrPayload.fromValidatedToken(token(validated = true), 0))
    }

    @Test
    fun binaryQrDataRoundTripsWithoutAnExtraCharacterSetHeader() {
        val payload = requireNotNull(
            TicketQrPayload.fromValidatedToken(token(validated = true), 0x65432110),
        )
        val qr = Encoder.encode(
            String(payload, StandardCharsets.ISO_8859_1),
            ErrorCorrectionLevel.L,
        )
        val matrix = requireNotNull(qr.matrix)
        val bits = BitMatrix(matrix.width, matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y].toInt() == 1) bits.set(x, y)
            }
        }
        val decoded = Decoder().decode(
            bits,
            mapOf(DecodeHintType.CHARACTER_SET to StandardCharsets.ISO_8859_1.name()),
        )
        assertArrayEquals(payload, decoded.text.toByteArray(StandardCharsets.ISO_8859_1))
    }

    private fun token(validated: Boolean): ByteArray = ByteArray(100).also { value ->
        byteArrayOf(65, 69, 80, 86).copyInto(value, 0)
        byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8).copyInto(value, 4)
        value[20] = 0x12
        value[21] = 0x34
        value[22] = 0x56
        byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16).copyInto(value, 23)
        byteArrayOf(17, 18, 19, 20, 21, 22, 23, 24).copyInto(value, 31)
        value[39] = 2
        value[52] = 3
        value[54] = 40
        byteArrayOf(0x11, 0x22, 0x33).copyInto(value, 57)
        value[60] = 1
        value[61] = if (validated) 2 else 0
    }
}
