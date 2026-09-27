package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.nfc.NfcValidationState
import it.girotuttatorino.gtt.ui.theme.GTTTheme
import it.girotuttatorino.gtt.ui.theme.GttBlue
import it.girotuttatorino.gtt.ui.theme.GttCanvas
import it.girotuttatorino.gtt.ui.theme.GttInk

/** Presentation-only scene; session and ticket ownership stay in [TicketsScreen]. */
@Composable
internal fun TicketsScene(
    expandedTicket: TicketItem?,
    availableTicketId: String?,
    nfcValidationState: NfcValidationState,
    validatedTicketIds: Set<String>,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    topBarHeight: androidx.compose.ui.unit.Dp,
    footerHeight: androidx.compose.ui.unit.Dp,
    horizontalInset: androidx.compose.ui.unit.Dp,
    onTicketLongClick: (TicketItem) -> Unit,
    onResetValidatedTicket: () -> Unit,
    onQrPayloadRequested: (String) -> ByteArray?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlayRevealed by remember(expandedTicket?.id) { mutableStateOf(false) }
    val scrimRevealProgress by animateFloatAsState(
        targetValue = if (overlayRevealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 160,
            easing = FastOutSlowInEasing,
        ),
        label = "ticketOverlayScrimReveal",
    )
    val cardRevealProgress by animateFloatAsState(
        targetValue = if (overlayRevealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing,
        ),
        label = "ticketOverlayCardReveal",
    )

    LaunchedEffect(expandedTicket?.id) {
        overlayRevealed = expandedTicket != null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GttCanvas),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = GttCanvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { SplashTopBar() },
            bottomBar = { SplashFooter() },
        ) { contentPadding ->
            TicketsContent(
                availableTicketId = availableTicketId,
                validatedTicketIds = validatedTicketIds,
                remainingValiditySeconds = remainingValiditySeconds,
                onTicketLongClick = onTicketLongClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        if (expandedTicket != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GttInk.copy(alpha = 0.30f))
                    .graphicsLayer {
                        alpha = scrimRevealProgress
                    }
                    .zIndex(1f),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ticket_overlay_scrim")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.close_ticket_details),
                        onClick = onDismiss,
                    )
                    .padding(
                        start = horizontalInset,
                        top = topBarHeight + 16.dp,
                        end = horizontalInset,
                        bottom = footerHeight + 24.dp,
                    )
                    .zIndex(2f),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val overlayMaxHeight = maxHeight
                    ExpandedTicketCard(
                        ticketId = expandedTicket.id,
                        ticketName = stringResource(expandedTicket.nameResource),
                        ticketImageResource = expandedTicket.imageResource,
                        durationResource = expandedTicket.durationResource,
                        areaResource = expandedTicket.areaResource,
                        tripsResource = expandedTicket.tripsResource,
                        showMetroAccess = expandedTicket.product.hasSingleMetroAccess,
                        validated = expandedTicket.id in validatedTicketIds,
                        remainingValiditySeconds = remainingValiditySeconds,
                        ridesToGo = ridesToGo,
                        metroAccessToGo = metroAccessToGo,
                        nfcValidationState = nfcValidationState,
                        onResetValidatedTicket = onResetValidatedTicket,
                        onQrPayloadRequested = { onQrPayloadRequested(expandedTicket.id) },
                        modifier = Modifier
                            .width(maxWidth.coerceAtMost(540.dp))
                            .heightIn(max = overlayMaxHeight)
                            .graphicsLayer {
                                alpha = cardRevealProgress
                                val revealScale = 0.96f + (0.04f * cardRevealProgress)
                                scaleX = revealScale
                                scaleY = revealScale
                            }
                            .testTag("ticket_overlay"),
                    )
                }
            }
        }
    }
}


@Composable
private fun TicketsContent(
    availableTicketId: String?,
    validatedTicketIds: Set<String>,
    remainingValiditySeconds: Long?,
    onTicketLongClick: (TicketItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(GttCanvas)) {
        val horizontalInset = if (maxWidth < 360.dp) 16.dp else 24.dp
        ContentSplashes(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(maxWidth.coerceAtMost(600.dp))
                .fillMaxHeight()
                .testTag("tickets_list"),
            contentPadding = PaddingValues(
                start = horizontalInset,
                top = 22.dp,
                end = horizontalInset,
                bottom = 24.dp,
            ),
        ) {
            val availableTicketCount = if (availableTicketId == null) 0 else 1
            item {
                Text(
                    text = stringResource(R.string.all_tickets),
                    color = GttInk,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(
                        if (availableTicketCount == 1) {
                            R.string.ticket_types_summary_one
                        } else {
                            R.string.ticket_types_summary_many
                        },
                        MainTickets.size,
                        availableTicketCount,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                    color = GttBlue.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            items(
                count = MainTickets.size,
                key = { MainTickets[it].id },
            ) { ticketIndex ->
                val ticket = MainTickets[ticketIndex]
                val available = ticket.id == availableTicketId
                TicketCard(
                    ticket = ticket,
                    available = available,
                    validated = ticket.id in validatedTicketIds,
                    remainingValiditySeconds = if (available) {
                        remainingValiditySeconds
                    } else {
                        null
                    },
                    onLongClick = { onTicketLongClick(ticket) },
                )
                if (ticketIndex < MainTickets.lastIndex) {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 540, heightDp = 960)
@Composable
private fun TicketsScreenPreview() {
    GTTTheme(darkTheme = false) {
        TicketsScreen(modifier = Modifier.widthIn(max = 540.dp))
    }
}
