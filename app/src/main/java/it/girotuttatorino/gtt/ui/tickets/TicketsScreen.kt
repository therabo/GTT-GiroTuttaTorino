package it.girotuttatorino.gtt.ui.tickets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import it.girotuttatorino.gtt.nfc.NfcValidationController
import it.girotuttatorino.gtt.nfc.NfcValidationState
import it.girotuttatorino.gtt.nfc.TicketRuntimeInfo
import it.girotuttatorino.gtt.nfc.core.TicketValidity
import it.girotuttatorino.gtt.nfc.diagnostics.NfcDiagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay

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
    val narrowScreen = LocalConfiguration.current.screenWidthDp < 360
    val view = LocalView.current
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

    DisposableEffect(view, expandedTicketVisible) {
        val previousKeepScreenOn = view.keepScreenOn
        if (expandedTicketVisible) view.keepScreenOn = true
        NfcDiagnostics.mark("SCREEN_KEEP_ON", "overlay=$expandedTicketVisible enabled=${view.keepScreenOn}")
        onDispose {
            if (expandedTicketVisible) view.keepScreenOn = previousKeepScreenOn
        }
    }

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
                Lifecycle.Event.ON_PAUSE -> ticketId
                    ?.let(nfcController::onTicketOverlayPaused)
                    ?: nfcController.onTicketOverlayClosed()
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
        NfcDiagnostics.mark("UI_TICKET_DISMISS", "source=back")
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
        horizontalInset = if (narrowScreen) 16.dp else 24.dp,
        onTicketLongClick = { ticket ->
            NfcDiagnostics.mark("UI_TICKET_OPEN", "source=long_press")
            expandedTicketId = ticket.id
        },
        onResetValidatedTicket = {
            availableTicketId?.let(nfcController::resetValidatedTicket) == true
        },
        onQrPayloadRequested = nfcController::validatedQrPayload,
        onDismiss = {
            NfcDiagnostics.mark("UI_TICKET_DISMISS", "source=outside_tap")
            expandedTicketId = null
        },
        modifier = modifier,
    )
}
