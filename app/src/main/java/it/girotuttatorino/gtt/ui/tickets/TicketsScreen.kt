package it.girotuttatorino.gtt.ui.tickets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.nfc.NfcValidationController
import it.girotuttatorino.gtt.nfc.NfcValidationState
import it.girotuttatorino.gtt.nfc.TicketRuntimeInfo
import it.girotuttatorino.gtt.nfc.core.TicketProduct
import it.girotuttatorino.gtt.nfc.core.TicketValidity
import it.girotuttatorino.gtt.ui.theme.GTTTheme
import it.girotuttatorino.gtt.ui.theme.GttBlue
import it.girotuttatorino.gtt.ui.theme.GttCanvas
import it.girotuttatorino.gtt.ui.theme.GttCyan
import it.girotuttatorino.gtt.ui.theme.GttDarkBlue
import it.girotuttatorino.gtt.ui.theme.GttGreen
import it.girotuttatorino.gtt.ui.theme.GttInk
import it.girotuttatorino.gtt.ui.theme.GttMagenta
import it.girotuttatorino.gtt.ui.theme.GttOrange
import it.girotuttatorino.gtt.ui.theme.GttYellow
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private val SplashGradient = listOf(GttDarkBlue, GttBlue, GttCyan)
private val TicketShape = RoundedCornerShape(12.dp)

private data class TicketItem(
    val product: TicketProduct,
    val nameResource: Int,
    val imageResource: Int,
    val durationResource: Int,
    val areaResource: Int,
    val tripsResource: Int,
) {
    val id: String get() = product.ticketId
}

internal enum class TicketBadgeState {
    Available,
    Unavailable,
    Validated,
}

internal fun ticketBadgeState(available: Boolean, validated: Boolean): TicketBadgeState = when {
    !available -> TicketBadgeState.Unavailable
    validated -> TicketBadgeState.Validated
    else -> TicketBadgeState.Available
}

private val MainTickets = listOf(
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

private fun currentValidatedTicketIds(
    nfcController: NfcValidationController,
): Set<String> = MainTickets
    .asSequence()
    .filter { ticket -> nfcController.isTicketValidated(ticket.id) }
    .map(TicketItem::id)
    .toSet()

@Composable
fun TicketsScreen(modifier: Modifier = Modifier) {
    var expandedTicketId by remember { mutableStateOf<String?>(null) }
    val expandedTicket = MainTickets.firstOrNull { it.id == expandedTicketId }
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val nfcController = remember(context) {
        NfcValidationController(context)
    }
    var nfcValidationState by remember(nfcController) {
        mutableStateOf<NfcValidationState>(nfcController.state)
    }
    var validatedTicketIds by remember(nfcController) {
        mutableStateOf(currentValidatedTicketIds(nfcController))
    }
    var availableTicketId by remember(nfcController) {
        mutableStateOf(nfcController.availableTicketId())
    }
    var activeTicketRuntimeInfo by remember(nfcController) {
        val ticketId = nfcController.availableTicketId()
        mutableStateOf(
            ticketId?.let(nfcController::ticketRuntimeInfo) ?: TicketRuntimeInfo.EMPTY,
        )
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val topBarHeight = 104.dp + statusBarHeight
    val footerHeight = 38.dp + navigationBarHeight
    val expandedTicketVisible = expandedTicket != null

    DisposableEffect(nfcController) {
        val observation = nfcController.observe { state ->
            nfcValidationState = state
            validatedTicketIds = currentValidatedTicketIds(nfcController)
            availableTicketId = nfcController.availableTicketId()
            activeTicketRuntimeInfo = availableTicketId
                ?.let(nfcController::ticketRuntimeInfo)
                ?: TicketRuntimeInfo.EMPTY
        }
        onDispose {
            observation.close()
            nfcController.close()
        }
    }

    LaunchedEffect(activeTicketRuntimeInfo.validUntilMillis) {
        val validUntil = activeTicketRuntimeInfo.validUntilMillis ?: return@LaunchedEffect
        nowMillis = System.currentTimeMillis()
        while (nowMillis < validUntil) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val remainingValiditySeconds = if (activeTicketRuntimeInfo.validated) {
        TicketValidity.remainingSeconds(activeTicketRuntimeInfo.validUntilMillis, nowMillis)
    } else {
        null
    }

    DisposableEffect(nfcController, lifecycleOwner, expandedTicket?.id, availableTicketId) {
        val ticketId = expandedTicket?.id?.takeIf { it == availableTicketId }
        val lifecycle = lifecycleOwner?.lifecycle
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> ticketId?.let(nfcController::onTicketOverlayOpened)
                Lifecycle.Event.ON_PAUSE -> nfcController.onTicketOverlayClosed()
                else -> Unit
            }
        }

        lifecycle?.addObserver(lifecycleObserver)
        if (ticketId != null && (
                lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            )
        ) {
            nfcController.onTicketOverlayOpened(ticketId)
        } else {
            nfcController.onTicketOverlayClosed()
        }

        onDispose {
            lifecycle?.removeObserver(lifecycleObserver)
            nfcController.onTicketOverlayClosed()
        }
    }

    BackHandler(enabled = expandedTicketVisible) {
        expandedTicketId = null
    }

    TicketsScene(
        expandedTicket = expandedTicket,
        availableTicketId = availableTicketId,
        nfcValidationState = nfcValidationState,
        validatedTicketIds = validatedTicketIds,
        remainingValiditySeconds = remainingValiditySeconds,
        ridesToGo = activeTicketRuntimeInfo.ridesToGo,
        metroAccessToGo = activeTicketRuntimeInfo.metroAccessToGo,
        topBarHeight = topBarHeight,
        footerHeight = footerHeight,
        onTicketLongClick = { ticket -> expandedTicketId = ticket.id },
        onResetValidatedTicket = {
            availableTicketId?.let(nfcController::resetValidatedTicket) == true
        },
        onDismiss = { expandedTicketId = null },
        modifier = modifier,
    )
}

@Composable
private fun TicketsScene(
    expandedTicket: TicketItem?,
    availableTicketId: String?,
    nfcValidationState: NfcValidationState,
    validatedTicketIds: Set<String>,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    topBarHeight: androidx.compose.ui.unit.Dp,
    footerHeight: androidx.compose.ui.unit.Dp,
    onTicketLongClick: (TicketItem) -> Unit,
    onResetValidatedTicket: () -> Unit,
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
                        start = 24.dp,
                        top = topBarHeight + 16.dp,
                        end = 24.dp,
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
                        modifier = Modifier
                            .fillMaxWidth()
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
private fun SplashTopBar() {
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp + statusBarHeight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 16.dp.toPx())
                cubicTo(
                    size.width * 0.83f,
                    size.height + 3.dp.toPx(),
                    size.width * 0.64f,
                    size.height - 27.dp.toPx(),
                    size.width * 0.44f,
                    size.height - 11.dp.toPx(),
                )
                cubicTo(
                    size.width * 0.27f,
                    size.height + 2.dp.toPx(),
                    size.width * 0.12f,
                    size.height - 23.dp.toPx(),
                    0f,
                    size.height - 8.dp.toPx(),
                )
                close()
            }
            drawPath(
                path = barPath,
                brush = Brush.linearGradient(
                    colors = SplashGradient,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )

            drawCircle(
                color = GttOrange,
                radius = 8.dp.toPx(),
                center = Offset(size.width * 0.79f, size.height - 14.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.84f, size.height - 4.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 2.5.dp.toPx(),
                center = Offset(size.width * 0.75f, size.height - 2.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = 3.5.dp.toPx(),
                center = Offset(size.width * 0.06f, size.height - 20.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 2.dp.toPx(),
                center = Offset(size.width * 0.22f, size.height - 14.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 3.dp.toPx(),
                center = Offset(size.width * 0.92f, size.height - 29.dp.toPx()),
            )
            drawCircle(
                color = GttOrange,
                radius = 4.5.dp.toPx(),
                center = Offset(size.width * 0.14f, size.height - 34.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 2.25.dp.toPx(),
                center = Offset(size.width * 0.18f, size.height - 23.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 1.6.dp.toPx(),
                center = Offset(size.width * 0.11f, size.height - 13.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.84f),
                radius = 1.8.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height - 18.dp.toPx()),
            )
            drawCircle(
                color = GttOrange,
                radius = 1.7.dp.toPx(),
                center = Offset(size.width * 0.95f, size.height - 12.dp.toPx()),
            )
        }

        Text(
            text = stringResource(R.string.tickets_title),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarHeight + 25.dp),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
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
    Box(modifier = modifier.background(GttCanvas)) {
        ContentSplashes(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tickets_list"),
            contentPadding = PaddingValues(
                start = 24.dp,
                top = 22.dp,
                end = 24.dp,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketCard(
    ticket: TicketItem,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val longPressLabel = stringResource(R.string.hold_for_ticket_details)
    val ticketName = stringResource(ticket.nameResource)
    val shape = TicketShape
    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (available && isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "ticketPressScale",
    )
    val interactionModifier = if (available) {
        Modifier.combinedClickable(
            interactionSource = pressInteractionSource,
            indication = null,
            onClick = {},
            onLongClickLabel = longPressLabel,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag("ticket_card_${ticket.id}")
            .then(interactionModifier),
    ) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = if (available) 7.dp else 3.dp,
                    shape = shape,
                )
                .clip(shape),
            shape = shape,
            color = Color.White,
        ) {}

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.6.dp),
        ) {
            val usesCompactLayout = maxWidth < 420.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    if (usesCompactLayout) 12.dp else 18.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val artworkLayoutModifier = if (usesCompactLayout) {
                    Modifier.weight(0.90f)
                } else {
                    Modifier.width(244.dp)
                }
                TicketArtwork(
                    imageResource = ticket.imageResource,
                    contentDescription = ticketName,
                    available = available,
                    modifier = artworkLayoutModifier,
                )
                TicketActions(
                    ticketName = ticketName,
                    available = available,
                    validated = validated,
                    remainingValiditySeconds = remainingValiditySeconds,
                    durationResource = ticket.durationResource,
                    showHoldHint = available && !usesCompactLayout,
                    compact = usesCompactLayout,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ExpandedTicketCard(
    ticketName: String,
    ticketImageResource: Int,
    durationResource: Int,
    areaResource: Int,
    tripsResource: Int,
    showMetroAccess: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    nfcValidationState: NfcValidationState,
    onResetValidatedTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = TicketShape

    Surface(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = GttCyan.copy(alpha = 0.35f),
                spotColor = GttDarkBlue.copy(alpha = 0.55f),
            )
            .border(
                width = 1.dp,
                color = GttCyan.copy(alpha = 0.30f),
                shape = shape,
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = shape,
        color = Color.Transparent,
    ) {
        BoxWithConstraints(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF0F9FF),
                        Color(0xFFFFFBF2),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
        ) {
            val usesCompactOverlay = maxHeight < 620.dp
            ExpandedTicketContent(
                ticketName = ticketName,
                ticketImageResource = ticketImageResource,
                durationResource = durationResource,
                areaResource = areaResource,
                tripsResource = tripsResource,
                showMetroAccess = showMetroAccess,
                validated = validated,
                remainingValiditySeconds = remainingValiditySeconds,
                ridesToGo = ridesToGo,
                metroAccessToGo = metroAccessToGo,
                nfcValidationState = nfcValidationState,
                onResetValidatedTicket = onResetValidatedTicket,
                compact = usesCompactOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (usesCompactOverlay) 17.64.dp else 28.dp),
            )
        }
    }
}

@Composable
private fun ExpandedTicketContent(
    ticketName: String,
    ticketImageResource: Int,
    durationResource: Int,
    areaResource: Int,
    tripsResource: Int,
    showMetroAccess: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    nfcValidationState: NfcValidationState,
    onResetValidatedTicket: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(validated) {
        if (!validated) showResetConfirmation = false
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetValidatedTicket()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.reset_ticket_confirm),
                        color = GttBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(
                        text = stringResource(R.string.reset_ticket_cancel),
                        color = GttInk.copy(alpha = 0.70f),
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.reset_ticket_title),
                    color = GttInk,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_ticket_message),
                    color = GttInk.copy(alpha = 0.76f),
                )
            },
            shape = TicketShape,
            containerColor = Color.White,
            modifier = Modifier.testTag("reset_ticket_confirmation"),
        )
    }

    Column(modifier = modifier) {
        TicketArtwork(
            imageResource = ticketImageResource,
            contentDescription = ticketName,
            available = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(if (compact) 10.dp else 16.dp))
        TicketSummary(
            ticketName = ticketName,
            available = true,
            validated = validated,
            remainingValiditySeconds = remainingValiditySeconds,
            durationResource = durationResource,
            compact = compact,
            uniformExpandedBadgeSize = true,
            trailingBadgeContent = if (validated) {
                {
                    ResetValidatedTicketButton(
                        compact = compact,
                        onClick = { showResetConfirmation = true },
                    )
                }
            } else {
                null
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = if (compact) 8.dp else 13.dp),
            color = GttBlue.copy(alpha = 0.12f),
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_validity_label),
            value = ticketValidityText(
                validated,
                remainingValiditySeconds,
                durationResource,
            ),
            compact = compact,
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_area_label),
            value = stringResource(areaResource),
            compact = compact,
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_trips_label),
            value = if (validated && ridesToGo != null) {
                stringResource(R.string.ticket_rides_remaining_value, ridesToGo)
            } else {
                stringResource(tripsResource)
            },
            compact = compact,
        )
        if (validated && showMetroAccess) {
            TicketDetailRow(
                label = stringResource(R.string.ticket_metro_access_label),
                value = stringResource(
                    R.string.ticket_metro_access_value,
                    metroAccessToGo ?: 0,
                ),
                compact = compact,
            )
        }
        NfcValidationBanner(
            state = nfcValidationState,
            compact = compact,
            modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp),
        )
        Text(
            text = stringResource(R.string.tap_outside_to_close_ticket),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = if (compact) 6.dp else 10.dp),
            color = GttBlue.copy(alpha = 0.62f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResetValidatedTicketButton(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = GttBlue.copy(alpha = 0.18f),
                shape = TicketShape,
            )
            .clip(TicketShape)
            .testTag("reset_validated_ticket")
            .clickable(
                onClickLabel = stringResource(R.string.reset_validated_ticket),
                onClick = onClick,
            ),
        shape = TicketShape,
        color = GttCyan.copy(alpha = 0.13f),
        contentColor = GttBlue,
    ) {
        UniformExpandedControlContent(
            text = stringResource(R.string.regenerate_ticket),
            compact = compact,
            color = GttBlue,
        )
    }
}

@Composable
private fun NfcValidationBanner(
    state: NfcValidationState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state == NfcValidationState.Inactive) return

    val title: String
    val message: String
    val accent: Color
    when (state) {
        NfcValidationState.Ready -> {
            title = stringResource(R.string.nfc_ready_title)
            message = stringResource(R.string.nfc_ready_message)
            accent = GttBlue
        }
        is NfcValidationState.Validated -> {
            title = stringResource(R.string.nfc_validated_title)
            val validationTime = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(Date(state.timestampMillis))
            message = stringResource(R.string.nfc_validated_message, validationTime)
            accent = GttCyan
        }
        NfcValidationState.Disabled -> {
            title = stringResource(R.string.nfc_disabled_title)
            message = stringResource(R.string.nfc_disabled_message)
            accent = GttOrange
        }
        NfcValidationState.Unsupported -> {
            title = stringResource(R.string.nfc_unsupported_title)
            message = stringResource(R.string.nfc_unsupported_message)
            accent = GttInk.copy(alpha = 0.70f)
        }
        NfcValidationState.Error -> {
            title = stringResource(R.string.nfc_error_title)
            message = stringResource(R.string.nfc_error_message)
            accent = GttMagenta
        }
        NfcValidationState.Inactive -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_validation_status"),
        shape = TicketShape,
        color = accent.copy(alpha = 0.10f),
        contentColor = GttInk,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 8.dp else 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "NFC",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accent,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 2.dp),
                    color = GttInk.copy(alpha = 0.76f),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = if (compact) 14.sp else 16.sp,
                )
            }
        }
    }
}

@Composable
private fun TicketDetailRow(
    label: String,
    value: String,
    compact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 5.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(if (compact) 70.dp else 76.dp),
            color = GttBlue.copy(alpha = 0.72f),
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = GttInk,
            fontSize = if (compact) 13.sp else 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TicketArtwork(
    imageResource: Int,
    contentDescription: String,
    available: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(imageResource),
        contentDescription = contentDescription,
        modifier = modifier
            .aspectRatio(300f / 183f)
            .alpha(if (available) 1f else 0.62f)
            .clip(TicketShape),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun TicketActions(
    ticketName: String,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
    showHoldHint: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TicketSummary(
            ticketName = ticketName,
            available = available,
            validated = validated,
            remainingValiditySeconds = remainingValiditySeconds,
            durationResource = durationResource,
            compact = compact,
        )
        if (showHoldHint) {
            Text(
                text = stringResource(R.string.hold_for_ticket_details),
                modifier = Modifier.padding(top = 10.dp),
                color = GttBlue.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun TicketSummary(
    ticketName: String,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
    compact: Boolean = false,
    uniformExpandedBadgeSize: Boolean = false,
    trailingBadgeContent: (@Composable () -> Unit)? = null,
) {
    val badgeState = ticketBadgeState(available = available, validated = validated)

    Column {
        if (trailingBadgeContent == null) {
            TicketStatusBadge(
                badgeState = badgeState,
                available = available,
                compact = compact,
                uniformExpandedSize = uniformExpandedBadgeSize,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TicketStatusBadge(
                    badgeState = badgeState,
                    available = available,
                    compact = compact,
                    uniformExpandedSize = true,
                )
                trailingBadgeContent()
            }
        }
        Text(
            text = ticketName,
            modifier = Modifier.padding(top = if (compact) 6.dp else 10.dp),
            color = if (available) GttInk else GttInk.copy(alpha = 0.68f),
            fontSize = if (compact) 17.sp else 23.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (compact) 20.sp else 28.sp,
            maxLines = 2,
        )
        if (available) {
            Text(
                text = ticketValidityText(
                    validated,
                    remainingValiditySeconds,
                    durationResource,
                ),
                color = GttBlue.copy(alpha = 0.72f),
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun TicketStatusBadge(
    badgeState: TicketBadgeState,
    available: Boolean,
    compact: Boolean,
    uniformExpandedSize: Boolean,
    modifier: Modifier = Modifier,
) {
    val badgeContentColor = when (badgeState) {
        TicketBadgeState.Available -> GttBlue
        TicketBadgeState.Unavailable -> GttMagenta
        TicketBadgeState.Validated -> GttGreen
    }
    val badgeContainerColor = when (badgeState) {
        TicketBadgeState.Available -> GttCyan.copy(alpha = 0.13f)
        TicketBadgeState.Unavailable -> GttMagenta.copy(alpha = 0.10f)
        TicketBadgeState.Validated -> GttGreen.copy(alpha = 0.14f)
    }
    val badgeTextResource = when (badgeState) {
        TicketBadgeState.Available -> R.string.ticket_available
        TicketBadgeState.Unavailable -> R.string.currently_unavailable
        TicketBadgeState.Validated -> R.string.ticket_validated
    }

    Surface(
        modifier = modifier,
        shape = TicketShape,
        color = badgeContainerColor,
    ) {
        if (uniformExpandedSize) {
            UniformExpandedControlContent(
                text = stringResource(badgeTextResource).uppercase(),
                compact = compact,
                color = badgeContentColor,
                raiseText = badgeState == TicketBadgeState.Validated,
            )
        } else {
            Text(
                text = stringResource(badgeTextResource).uppercase(),
                modifier = Modifier
                    .padding(
                        horizontal = when {
                            compact && !available -> 6.dp
                            compact -> 8.dp
                            else -> 10.dp
                        },
                        vertical = if (compact) 4.dp else 5.dp,
                    )
                    .offset(y = if (badgeState == TicketBadgeState.Validated) (-1).dp else 0.dp),
                color = badgeContentColor,
                fontSize = when {
                    compact && available -> 9.sp
                    compact -> 7.sp
                    available -> 11.sp
                    else -> 9.sp
                },
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines = if (compact) 1 else 2,
            )
        }
    }
}

@Composable
private fun UniformExpandedControlContent(
    text: String,
    compact: Boolean,
    color: Color,
    raiseText: Boolean = false,
) {
    val horizontalPadding = if (compact) 8.dp else 10.dp
    val verticalPadding = if (compact) 4.dp else 5.dp
    val fontSize = if (compact) 9.sp else 11.sp

    Box(contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.ticket_available).uppercase(),
            modifier = Modifier
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                )
                .clearAndSetSemantics {},
            color = Color.Transparent,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
        Text(
            text = text,
            modifier = Modifier.offset(y = if (raiseText) (-1).dp else 0.dp),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ticketValidityText(
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
): String {
    if (!validated || remainingValiditySeconds == null) {
        return stringResource(durationResource)
    }
    val minutes = remainingValiditySeconds / 60L
    val seconds = remainingValiditySeconds % 60L
    return stringResource(R.string.ticket_countdown, minutes, seconds)
}

@Composable
private fun ContentSplashes(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = GttCyan.copy(alpha = 0.07f),
            radius = 82.dp.toPx(),
            center = Offset(size.width + 12.dp.toPx(), size.height * 0.20f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.07f),
            radius = 9.dp.toPx(),
            center = Offset(size.width - 28.dp.toPx(), size.height * 0.39f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.08f),
            radius = 5.dp.toPx(),
            center = Offset(size.width - 59.dp.toPx(), size.height * 0.42f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.08f),
            radius = 7.dp.toPx(),
            center = Offset(25.dp.toPx(), size.height * 0.57f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.10f),
            radius = 3.dp.toPx(),
            center = Offset(49.dp.toPx(), size.height * 0.54f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.08f),
            radius = 4.dp.toPx(),
            center = Offset(16.dp.toPx(), size.height * 0.51f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.10f),
            radius = 2.5.dp.toPx(),
            center = Offset(65.dp.toPx(), size.height * 0.59f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.07f),
            radius = 6.dp.toPx(),
            center = Offset(size.width - 23.dp.toPx(), size.height * 0.64f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.09f),
            radius = 3.5.dp.toPx(),
            center = Offset(size.width - 54.dp.toPx(), size.height * 0.68f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.08f),
            radius = 2.dp.toPx(),
            center = Offset(size.width - 76.dp.toPx(), size.height * 0.62f),
        )
    }
}

@Composable
private fun SplashFooter() {
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp + navigationBarHeight),
    ) {
        val footerPath = Path().apply {
            moveTo(0f, 12.dp.toPx())
            cubicTo(
                size.width * 0.18f,
                -2.dp.toPx(),
                size.width * 0.34f,
                22.dp.toPx(),
                size.width * 0.52f,
                8.dp.toPx(),
            )
            cubicTo(
                size.width * 0.69f,
                -3.dp.toPx(),
                size.width * 0.84f,
                19.dp.toPx(),
                size.width,
                5.dp.toPx(),
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = footerPath,
            brush = Brush.linearGradient(
                colors = listOf(GttDarkBlue, GttBlue, GttMagenta, GttOrange),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
            ),
        )
        drawCircle(
            color = GttCyan,
            radius = 4.dp.toPx(),
            center = Offset(size.width * 0.18f, 5.dp.toPx()),
        )
        drawCircle(
            color = GttOrange,
            radius = 2.5.dp.toPx(),
            center = Offset(size.width * 0.72f, 3.dp.toPx()),
        )
        drawCircle(
            color = GttYellow,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.10f, 11.dp.toPx()),
        )
        drawCircle(
            color = GttMagenta,
            radius = 3.5.dp.toPx(),
            center = Offset(size.width * 0.88f, 9.dp.toPx()),
        )
        drawCircle(
            color = GttCyan,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.26f, 14.dp.toPx()),
        )
        drawCircle(
            color = GttYellow,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.31f, 7.dp.toPx()),
        )
        drawCircle(
            color = GttOrange,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.66f, 12.dp.toPx()),
        )
        drawCircle(
            color = GttMagenta,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.94f, 15.dp.toPx()),
        )
    }
}

@Preview(showBackground = true, widthDp = 540, heightDp = 960)
@Composable
private fun TicketsScreenPreview() {
    GTTTheme(darkTheme = false) {
        TicketsScreen(modifier = Modifier.widthIn(max = 540.dp))
    }
}
