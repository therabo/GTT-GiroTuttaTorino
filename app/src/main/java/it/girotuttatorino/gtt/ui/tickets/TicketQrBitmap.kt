package it.girotuttatorino.gtt.ui.tickets

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.nio.charset.StandardCharsets

/** Turns opaque verification bytes into a sharp, white-backed QR without persisting them. */
internal object TicketQrBitmap {
    private const val BITMAP_SIZE = 512

    fun create(payload: ByteArray): Bitmap? = runCatching {
        val data = String(payload, StandardCharsets.ISO_8859_1)
        val matrix = QRCodeWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            BITMAP_SIZE,
            BITMAP_SIZE,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to 4,
            ),
        )
        val pixels = IntArray(BITMAP_SIZE * BITMAP_SIZE) { index ->
            if (matrix[index % BITMAP_SIZE, index / BITMAP_SIZE]) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
    }.getOrNull()
}
