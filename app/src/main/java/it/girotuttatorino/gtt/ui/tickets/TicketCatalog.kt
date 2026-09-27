package it.girotuttatorino.gtt.ui.tickets

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.nfc.core.TicketProduct

/** UI metadata for one product; ticket availability still comes from the NFC facade. */
internal data class TicketItem(
    val product: TicketProduct,
    @StringRes val nameResource: Int,
    @DrawableRes val imageResource: Int,
    @StringRes val durationResource: Int,
    @StringRes val areaResource: Int,
    @StringRes val tripsResource: Int,
) {
    val id: String get() = product.ticketId
}

internal val MainTickets = listOf(
    TicketItem(
        product = TicketProduct.CITY,
        nameResource = R.string.ticket_name,
        imageResource = R.drawable.ticket_city,
        durationResource = R.string.ticket_duration,
        areaResource = R.string.ticket_area_value,
        tripsResource = R.string.ticket_trips_value,
    ),
    TicketItem(
        product = TicketProduct.MULTI_DAILY_7,
        nameResource = R.string.ticket_multi_daily_7,
        imageResource = R.drawable.ticket_multi_daily_7,
        durationResource = R.string.ticket_duration_multi_daily_7,
        areaResource = R.string.ticket_area_value,
        tripsResource = R.string.ticket_trips_multi_daily_7,
    ),
    TicketItem(
        product = TicketProduct.DAILY,
        nameResource = R.string.ticket_daily,
        imageResource = R.drawable.ticket_daily,
        durationResource = R.string.ticket_duration_daily,
        areaResource = R.string.ticket_area_value,
        tripsResource = R.string.ticket_trips_daily,
    ),
    TicketItem(
        product = TicketProduct.EXTRAURBAN_MULTI_6,
        nameResource = R.string.ticket_extraurban_multi_6,
        imageResource = R.drawable.ticket_extraurban_multi_6,
        durationResource = R.string.ticket_duration_extraurban,
        areaResource = R.string.ticket_area_extraurban,
        tripsResource = R.string.ticket_trips_extraurban_multi_6,
    ),
    TicketItem(
        product = TicketProduct.EXTRAURBAN_1,
        nameResource = R.string.ticket_extraurban_1,
        imageResource = R.drawable.ticket_extraurban_1,
        durationResource = R.string.ticket_duration_extraurban,
        areaResource = R.string.ticket_area_extraurban,
        tripsResource = R.string.ticket_trips_extraurban_1,
    ),
    TicketItem(
        product = TicketProduct.DAILY_X4,
        nameResource = R.string.ticket_daily_x4,
        imageResource = R.drawable.ticket_daily_x4,
        durationResource = R.string.ticket_duration_daily,
        areaResource = R.string.ticket_area_value,
        tripsResource = R.string.ticket_trips_daily_x4,
    ),
    TicketItem(
        product = TicketProduct.TOUR_48,
        nameResource = R.string.ticket_tour_48,
        imageResource = R.drawable.ticket_tour_48,
        durationResource = R.string.ticket_duration_tour_48,
        areaResource = R.string.ticket_area_tour,
        tripsResource = R.string.ticket_trips_tour,
    ),
    TicketItem(
        product = TicketProduct.TOUR_72,
        nameResource = R.string.ticket_tour_72,
        imageResource = R.drawable.ticket_tour_72,
        durationResource = R.string.ticket_duration_tour_72,
        areaResource = R.string.ticket_area_tour,
        tripsResource = R.string.ticket_trips_tour,
    ),
)
