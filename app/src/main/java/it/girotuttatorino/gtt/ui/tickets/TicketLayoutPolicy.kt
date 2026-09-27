package it.girotuttatorino.gtt.ui.tickets

/** Decisions are based on the space a component actually receives, not device categories. */
internal object TicketLayoutPolicy {
    fun stackListCard(contentWidthDp: Float, fontScale: Float): Boolean =
        contentWidthDp < 270f || fontScale >= 1.3f

    fun stackExpandedControls(contentWidthDp: Float, fontScale: Float): Boolean =
        contentWidthDp < 310f || fontScale >= 1.3f

    fun compactOverlay(widthDp: Float, heightDp: Float, fontScale: Float): Boolean =
        widthDp < 360f || heightDp < 620f || fontScale >= 1.3f
}
