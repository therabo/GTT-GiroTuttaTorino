package it.girotuttatorino.gtt.nfc

internal data class TicketRuntimeInfo(
    val validated: Boolean,
    val validUntilMillis: Long?,
    val minutesReportedByToken: Int?,
    val ridesToGo: Int?,
    val metroAccessToGo: Int?,
) {
    companion object {
        val EMPTY = TicketRuntimeInfo(
            validated = false,
            validUntilMillis = null,
            minutesReportedByToken = null,
            ridesToGo = null,
            metroAccessToGo = null,
        )
    }
}
