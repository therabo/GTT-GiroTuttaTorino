package it.girotuttatorino.gtt.ui.tickets

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketLayoutPolicyTest {
    @Test fun normalPhonePreservesSideBySideCard() {
        assertFalse(TicketLayoutPolicy.stackListCard(287f, 1f))
    }

    @Test fun compactPhoneAndLargeTextAvoidCrowdedCard() {
        assertTrue(TicketLayoutPolicy.stackListCard(247f, 1f))
        assertTrue(TicketLayoutPolicy.stackListCard(350f, 1.4f))
    }

    @Test fun expandedControlsAndContentAdaptIndependently() {
        assertTrue(TicketLayoutPolicy.stackExpandedControls(220f, 1f))
        assertFalse(TicketLayoutPolicy.stackExpandedControls(300f, 1f))
        assertTrue(TicketLayoutPolicy.stackExpandedControls(300f, 1.3f))
        assertFalse(TicketLayoutPolicy.stackExpandedControls(440f, 1f))
        assertTrue(TicketLayoutPolicy.compactOverlay(340f, 700f, 1f))
        assertTrue(TicketLayoutPolicy.compactOverlay(440f, 700f, 1.5f))
        assertFalse(TicketLayoutPolicy.compactOverlay(440f, 700f, 1f))
    }
}
