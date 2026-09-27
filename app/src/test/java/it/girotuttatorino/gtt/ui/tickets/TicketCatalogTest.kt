package it.girotuttatorino.gtt.ui.tickets

import it.girotuttatorino.gtt.nfc.core.TicketProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class TicketCatalogTest {
    @Test fun everySupportedProductHasExactlyOneCard() {
        assertEquals(TicketProduct.entries.toSet(), MainTickets.map { it.product }.toSet())
        assertEquals(MainTickets.size, MainTickets.map { it.id }.distinct().size)
    }
}
