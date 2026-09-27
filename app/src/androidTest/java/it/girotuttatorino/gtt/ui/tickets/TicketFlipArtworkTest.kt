package it.girotuttatorino.gtt.ui.tickets

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.ui.theme.GTTTheme
import java.io.File
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Isolated UI QA: no ticket repository or NFC state is touched. */
@RunWith(AndroidJUnit4::class)
class TicketFlipArtworkTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun narrowArtworkFlipsAndRendersQr() = checkArtwork(widthDp = 300)

    @Test
    fun wideArtworkFlipsAndRendersQr() = checkArtwork(widthDp = 480)

    @Test
    fun issuedArtworkKeepsFullSizeWithoutQrReverse() {
        composeRule.setContent {
            GTTTheme(darkTheme = false) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FA)),
                    contentAlignment = Alignment.Center,
                ) {
                    TicketFlipArtwork(
                        imageResource = R.drawable.ticket_city,
                        ticketName = "City su APP",
                        ticketId = "synthetic-issued-qa",
                        validated = false,
                        refreshKey = null,
                        onQrPayloadRequested = { null },
                        modifier = Modifier.width(480.dp),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("ticket_artwork_front").assertIsDisplayed()
        val artworkBounds = composeRule.onNodeWithTag("ticket_artwork_front")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeRule.onNodeWithTag("ticket_artwork_image", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(artworkBounds.width, imageBounds.width, 1f)
        assertEquals(artworkBounds.height, imageBounds.height, 1f)
        saveScreenshot("ticket-issued-480dp.png")
        composeRule.onNodeWithTag("ticket_artwork_front")
            .performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("ticket_artwork_front").assertIsDisplayed()
        composeRule.onNodeWithTag("ticket_qr_reverse").assertDoesNotExist()
    }

    private fun checkArtwork(widthDp: Int) {
        composeRule.setContent {
            GTTTheme(darkTheme = false) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FA)),
                    contentAlignment = Alignment.Center,
                ) {
                    TicketFlipArtwork(
                        imageResource = R.drawable.ticket_city,
                        ticketName = "City su APP",
                        ticketId = "synthetic-qa",
                        validated = true,
                        refreshKey = null,
                        onQrPayloadRequested = { syntheticQrData() },
                        modifier = Modifier.width(widthDp.dp),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("ticket_artwork_front").assertIsDisplayed()
        val artworkBounds = composeRule.onNodeWithTag("ticket_artwork_front")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeRule.onNodeWithTag("ticket_artwork_image", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(artworkBounds.width, imageBounds.width, 1f)
        assertEquals(artworkBounds.height, imageBounds.height, 1f)
        saveScreenshot("ticket-front-${widthDp}dp.png")
        composeRule.onNodeWithTag("ticket_artwork_front")
            .performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("ticket_qr_reverse").assertIsDisplayed()
        composeRule.onNodeWithTag("ticket_qr_image", useUnmergedTree = true).assertIsDisplayed()
        val reverseBounds = composeRule.onNodeWithTag("ticket_reverse_surface", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(artworkBounds.width, reverseBounds.width, 1f)
        assertEquals(artworkBounds.height, reverseBounds.height, 1f)
        composeRule.waitForIdle()
        val reverseScreenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val whiteX = (reverseBounds.left + reverseBounds.width * 0.1f).roundToInt()
        val whiteY = (reverseBounds.top + reverseBounds.height * 0.1f).roundToInt()
        assertEquals(android.graphics.Color.WHITE, reverseScreenshot.getPixel(whiteX, whiteY))
        saveScreenshot("qr-reverse-${widthDp}dp.png")
        composeRule.onNodeWithTag("ticket_qr_reverse").performClick()
        composeRule.onNodeWithTag("ticket_artwork_front").assertIsDisplayed()
    }

    private fun saveScreenshot(fileName: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val destination = File(context.getExternalFilesDir(null) ?: context.cacheDir, fileName)
        destination.outputStream().use { stream ->
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun syntheticQrData(): ByteArray =
        byteArrayOf(86, 84, 83, 81, 1, 0, 0, 0, 0, 0, 0, 0) +
            ByteArray(110) { index -> (index * 23).toByte() }
}
