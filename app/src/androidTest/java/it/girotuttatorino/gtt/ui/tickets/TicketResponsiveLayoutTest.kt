package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.girotuttatorino.gtt.nfc.NfcValidationState
import it.girotuttatorino.gtt.ui.theme.GTTTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated layout tests: no repository access and no private ticket cleanup. */
@RunWith(AndroidJUnit4::class)
class TicketResponsiveLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun narrowCardStacksArtworkAboveDetails() {
        renderCard(widthDp = 288)
        val card = composeRule.onNodeWithTag("ticket_card_city")
            .fetchSemanticsNode().boundsInRoot
        val artwork = composeRule.onNodeWithTag("ticket_list_artwork", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Artwork should use the card width on compact screens",
            artwork.width > card.width * 0.8f)
    }

    @Test fun standardCardKeepsSideBySideComposition() {
        renderCard(widthDp = 312)
        val card = composeRule.onNodeWithTag("ticket_card_city")
            .fetchSemanticsNode().boundsInRoot
        val artwork = composeRule.onNodeWithTag("ticket_list_artwork", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Standard portrait composition should remain side by side",
            artwork.width < card.width * 0.6f)
    }

    @Test fun phoneWidthPlacesRegenerateToTheRightOfValidated() {
        renderExpandedCard(widthDp = 335)
        val badge = composeRule.onNodeWithTag("ticket_status_badge", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithTag("reset_validated_ticket", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Regenerate should share the badge row", button.left > badge.right)
        assertTrue("Regenerate should align vertically with the badge",
            kotlin.math.abs(button.center.y - badge.center.y) < 2f)
    }

    @Test fun veryNarrowWidthStacksExpandedControlsWithoutOverlap() {
        renderExpandedCard(widthDp = 260)
        val badge = composeRule.onNodeWithTag("ticket_status_badge", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithTag("reset_validated_ticket", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Controls should stack only when they cannot fit", button.top >= badge.bottom)
    }

    private fun renderCard(widthDp: Int) {
        composeRule.setContent {
            GTTTheme(darkTheme = false) {
                Box(modifier = Modifier.width(widthDp.dp)) {
                    TicketCard(
                        ticket = MainTickets.first(),
                        available = true,
                        validated = false,
                        remainingValiditySeconds = null,
                        onLongClick = {},
                    )
                }
            }
        }
    }

    private fun renderExpandedCard(widthDp: Int) {
        val ticket = MainTickets.first()
        composeRule.setContent {
            GTTTheme(darkTheme = false) {
                Box(modifier = Modifier.width(widthDp.dp).height(700.dp)) {
                    ExpandedTicketCard(
                        ticketId = ticket.id,
                        ticketName = "City su APP",
                        ticketImageResource = ticket.imageResource,
                        durationResource = ticket.durationResource,
                        areaResource = ticket.areaResource,
                        tripsResource = ticket.tripsResource,
                        showMetroAccess = false,
                        validated = true,
                        remainingValiditySeconds = 6000,
                        ridesToGo = 0,
                        metroAccessToGo = null,
                        nfcValidationState = NfcValidationState.Ready,
                        onResetValidatedTicket = {},
                        onQrPayloadRequested = { null },
                    )
                }
            }
        }
    }
}
