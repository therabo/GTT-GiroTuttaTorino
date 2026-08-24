package it.girotuttatorino.gtt.nfc.core

/** GTT products observed in the TO Move catalog and represented by the ticket list UI. */
internal enum class TicketProduct(
    val ticketId: String,
    val providerId: Int,
    val tariffFamilyId: Int,
    val tariffFamilyType: Int,
    val tariffId: Int,
    val fallbackDurationMinutes: Int?,
    val hasSingleMetroAccess: Boolean = false,
) {
    CITY(
        ticketId = "city",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 771,
        fallbackDurationMinutes = 100,
        hasSingleMetroAccess = true,
    ),
    MULTI_DAILY_7(
        ticketId = "multi_daily_7",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 776,
        fallbackDurationMinutes = null,
    ),
    DAILY(
        ticketId = "daily",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 773,
        fallbackDurationMinutes = null,
    ),
    EXTRAURBAN_MULTI_6(
        ticketId = "extraurban_multi_6",
        providerId = 1,
        tariffFamilyId = 12,
        tariffFamilyType = 3,
        tariffId = 1000,
        fallbackDurationMinutes = 90,
    ),
    EXTRAURBAN_1(
        ticketId = "extraurban_1",
        providerId = 1,
        tariffFamilyId = 12,
        tariffFamilyType = 3,
        tariffId = 3000,
        fallbackDurationMinutes = 90,
    ),
    DAILY_X4(
        ticketId = "daily_x4",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 790,
        fallbackDurationMinutes = null,
    ),
    TOUR_48(
        ticketId = "tour_48",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 654,
        fallbackDurationMinutes = 48 * 60,
    ),
    TOUR_72(
        ticketId = "tour_72",
        providerId = 1,
        tariffFamilyId = 10,
        tariffFamilyType = 1,
        tariffId = 655,
        fallbackDurationMinutes = 72 * 60,
    );

    companion object {
        fun fromTicket(ticket: ByteArray?): TicketProduct? =
            AepVToken.productIdentity(ticket)?.let(::fromIdentity)

        fun fromIdentity(identity: AepVToken.ProductIdentity): TicketProduct? =
            entries.firstOrNull { product ->
                product.providerId == identity.providerId &&
                    product.tariffId == identity.tariffId
            }

        fun fromTicketId(ticketId: String): TicketProduct? =
            entries.firstOrNull { it.ticketId == ticketId }
    }
}
