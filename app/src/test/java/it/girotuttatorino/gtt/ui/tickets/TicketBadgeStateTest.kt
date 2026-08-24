package it.girotuttatorino.gtt.ui.tickets

import org.junit.Assert.assertEquals
import org.junit.Test

class TicketBadgeStateTest {
    @Test
    fun validatedAvailableTicketUsesValidatedBadge() {
        assertEquals(
            TicketBadgeState.Validated,
            ticketBadgeState(available = true, validated = true),
        )
    }

    @Test
    fun issuedAvailableTicketUsesAvailableBadge() {
        assertEquals(
            TicketBadgeState.Available,
            ticketBadgeState(available = true, validated = false),
        )
    }

    @Test
    fun unavailableTicketCannotBeShownAsValidated() {
        assertEquals(
            TicketBadgeState.Unavailable,
            ticketBadgeState(available = false, validated = true),
        )
    }
}
