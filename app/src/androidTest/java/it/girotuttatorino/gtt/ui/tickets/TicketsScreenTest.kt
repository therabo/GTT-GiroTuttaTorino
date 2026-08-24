package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.girotuttatorino.gtt.nfc.data.TicketRepository
import it.girotuttatorino.gtt.ui.theme.GTTTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TicketsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        clearPrivateTicket()
    }

    @After
    fun tearDown() {
        clearPrivateTicket()
    }

    private fun render() {
        composeRule.setContent {
            GTTTheme(darkTheme = false) {
                TicketsScreen()
            }
        }
    }

    @Test
    fun longPressExpandsTicketWithoutLeavingScreen() {
        provisionTicket(tariffId = 771)
        render()
        val secondTicketTopBefore = composeRule
            .onNodeWithTag("ticket_card_multi_daily_7")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        composeRule
            .onNodeWithTag("ticket_card_city")
            .performTouchInput { longClick() }

        val secondTicketTopAfter = composeRule
            .onNodeWithTag("ticket_card_multi_daily_7")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertEquals(secondTicketTopBefore, secondTicketTopAfter, 0f)
        composeRule.onNodeWithText("Validità").assertIsDisplayed()
        composeRule.onNodeWithText("€ 1,90").assertDoesNotExist()
        composeRule.onNodeWithText("GiroTuttaTorino").assertIsDisplayed()
        composeRule.onNodeWithText("USA").assertDoesNotExist()
        composeRule.onNodeWithTag("nfc_validation_status").assertIsDisplayed()

        composeRule.onNodeWithTag("ticket_overlay_scrim").performClick()

        composeRule.onNodeWithText("Validità").assertDoesNotExist()
        composeRule.onNodeWithText("GiroTuttaTorino").assertIsDisplayed()
    }

    @Test
    fun cleanInstallHasNoAvailableTickets() {
        render()
        composeRule
            .onNodeWithTag("ticket_card_city")
            .assertIsDisplayed()
            .assertHasNoClickAction()

        composeRule.onNodeWithText("8 tipologie · 0 disponibili").assertIsDisplayed()
        composeRule.onNodeWithText("Validità").assertDoesNotExist()
    }

    @Test
    fun tariffIdSelectsTheMatchingTicketCard() {
        provisionTicket(tariffId = 773)
        render()

        composeRule.onNodeWithTag("ticket_card_city").assertHasNoClickAction()
        composeRule
            .onNodeWithTag("ticket_card_daily")
            .assertHasClickAction()
            .performTouchInput { longClick() }

        composeRule.onNodeWithText("Fino al termine del servizio").assertIsDisplayed()
    }

    @Test
    fun validatedCityReadsConsumedMetroAdmissionFromThePayload() {
        provisionValidatedCity(metroAlreadyUsed = true, rideId = 801)
        render()

        composeRule
            .onNodeWithTag("ticket_card_city")
            .performTouchInput { longClick() }

        composeRule.onNodeWithText("Accesso metro").assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun validatedCityKeepsMetroAdmissionWhenThePayloadBitIsClear() {
        provisionValidatedCity(metroAlreadyUsed = false, rideId = 0)
        render()

        composeRule
            .onNodeWithTag("ticket_card_city")
            .performTouchInput { longClick() }

        composeRule.onNodeWithText("Accesso metro").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun allTicketTypesAreInTheSingleMainList() {
        render()
        composeRule
            .onNodeWithTag("tickets_list")
            .performScrollToIndex(8)

        composeRule
            .onNodeWithTag("ticket_card_tour_72")
            .assertIsDisplayed()
    }

    private fun provisionTicket(tariffId: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "tickets/current")
        check(directory.mkdirs() || directory.isDirectory)
        val ticket = issuedToken(tariffId)
        File(directory, "original.vtoken").writeBytes(ticket)
        File(directory, "active.vtoken").writeBytes(ticket)
    }

    private fun provisionValidatedCity(metroAlreadyUsed: Boolean, rideId: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "tickets/current")
        check(directory.mkdirs() || directory.isDirectory)
        val issued = issuedToken(tariffId = 771)
        val validated = issued.copyOf().also { ticket ->
            val payloadOffset = 57
            val metroUsageOffset = payloadOffset + 12
            ticket[metroUsageOffset] = if (metroAlreadyUsed) 0x80.toByte() else 0
            val validationOffset = payloadOffset + 64
            ticket[validationOffset + 1] = 0x0E
            ticket[validationOffset + 22] = (rideId ushr 24).toByte()
            ticket[validationOffset + 23] = (rideId ushr 16).toByte()
            ticket[validationOffset + 24] = (rideId ushr 8).toByte()
            ticket[validationOffset + 25] = rideId.toByte()
            ticket[validationOffset + 34] = 0
            ticket[validationOffset + 35] = 100
            ticket[validationOffset + 36] = 0
        }
        File(directory, "original.vtoken").writeBytes(issued)
        File(directory, "active.vtoken").writeBytes(validated)
        val now = System.currentTimeMillis()
        context.getSharedPreferences("nfc_ticket_store", 0).edit()
            .putLong("city_ticket_validated_at", now)
            .putLong("city_ticket_valid_until", now + 100L * 60L * 1_000L)
            .commit()
    }

    private fun clearPrivateTicket() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.noBackupFilesDir, "tickets/current").deleteRecursively()
        File(context.noBackupFilesDir, "tickets/city").deleteRecursively()
        context.getSharedPreferences("nfc_ticket_store", 0).edit().clear().commit()
        TicketRepository(context).loadTicket()
    }

    private fun issuedToken(tariffId: Int): ByteArray = ByteArray(208).also { ticket ->
        byteArrayOf(0x41, 0x45, 0x50, 0x56).copyInto(ticket, 0)
        ticket[8] = 0x01
        ticket[9] = 0xF6.toByte()
        ticket[10] = 0x79
        ticket[11] = 0xD6.toByte()
        ticket[12] = 1
        ticket[13] = 1
        ticket[21] = 1
        ticket[22] = 1
        byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x08)
            .copyInto(ticket, 23)
        byteArrayOf(0x00, 0x01, 0x13, 0x44, 0x25, 0x36, 0x47, 0x58)
            .copyInto(ticket, 31)
        ticket[39] = 2
        ticket[40] = 1
        ticket[44] = 3
        ticket[52] = 64
        ticket[54] = 40
        ticket[56] = 47
        ticket[57 + 20] = 1
        ticket[57 + 21] = 1
        ticket[57 + 22] = (tariffId ushr 8).toByte()
        ticket[57 + 23] = tariffId.toByte()
        ticket[57 + 64] = 1
    }
}
