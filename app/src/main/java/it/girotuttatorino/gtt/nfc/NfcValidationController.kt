package it.girotuttatorino.gtt.nfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Handler
import android.os.Looper
import it.girotuttatorino.gtt.nfc.core.AepVToken
import it.girotuttatorino.gtt.nfc.core.TicketProduct
import it.girotuttatorino.gtt.nfc.data.NfcAidRouter
import it.girotuttatorino.gtt.nfc.data.NfcSessionGate
import it.girotuttatorino.gtt.nfc.data.TicketRepository
import java.util.concurrent.CopyOnWriteArraySet

internal class NfcValidationController(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val ticketRepository = TicketRepository(applicationContext)
    private val sessionGate = NfcSessionGate(applicationContext)
    private val aidRouter = NfcAidRouter(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val observers = CopyOnWriteArraySet<(NfcValidationState) -> Unit>()
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)

    private var requestedTicketId: String? = null
    private var receiverRegistered = false
    private var closed = false

    @Volatile
    var state: NfcValidationState = NfcValidationState.Inactive
        private set

    private val ticketObserver = ticketRepository.observeTicketChanges {
        mainHandler.post { refreshExposure(forceNotify = true) }
    }

    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                refreshExposure()
            }
        }
    }

    private val renewLease = object : Runnable {
        override fun run() {
            val ticketId = requestedTicketId ?: return
            val ticket = ticketRepository.loadTicket()
            if (canExposeTicket() && AepVToken.state(ticket) in setOf(
                    AepVToken.STATE_ISSUED,
                    AepVToken.STATE_VALIDATED,
                ) && TicketProduct.fromTicket(ticket)?.ticketId == ticketId
            ) {
                if (!sessionGate.renew(ticketId)) sessionGate.open(ticketId)
                mainHandler.postDelayed(this, NfcConfig.SESSION_RENEW_INTERVAL_MILLIS)
            } else {
                refreshExposure()
            }
        }
    }

    private val expireValidatedTicket = Runnable {
        ticketRepository.expireValidatedIfNeeded()
        refreshExposure(forceNotify = true)
    }

    private val closeAfterUiPause = Runnable {
        onTicketOverlayClosed()
    }

    init {
        registerAdapterReceiver()
        mainHandler.post { refreshExposure(forceNotify = true) }
    }

    fun observe(observer: (NfcValidationState) -> Unit): AutoCloseable {
        check(!closed) { "NFC validation controller is closed" }
        observers += observer
        observer(state)
        return AutoCloseable { observers -= observer }
    }

    /** Returns the persisted lifecycle state of a ticket, independently of NFC exposure. */
    fun isTicketValidated(ticketId: String): Boolean =
        availableTicketId() == ticketId &&
            AepVToken.state(ticketRepository.loadTicket()) == AepVToken.STATE_VALIDATED

    fun availableTicketId(): String? =
        TicketProduct.fromTicket(ticketRepository.loadTicket())?.ticketId

    fun ticketRuntimeInfo(ticketId: String): TicketRuntimeInfo {
        val ticket = ticketRepository.loadTicket()
        val product = TicketProduct.fromTicket(ticket)
        if (product?.ticketId != ticketId) return TicketRuntimeInfo.EMPTY
        val validated = AepVToken.state(ticket) == AepVToken.STATE_VALIDATED
        val validation = AepVToken.validationInfo(ticket)
        return TicketRuntimeInfo(
            validated = validated,
            validUntilMillis = if (validated) ticketRepository.validatedUntilMillis() else null,
            minutesReportedByToken = validation?.minutesToGo,
            ridesToGo = validation?.ridesToGo,
            metroAccessToGo = if (validated && product.hasSingleMetroAccess) {
                AepVToken.cityMetroRidesToGo(ticket)
            } else {
                null
            },
        )
    }

    fun resetValidatedTicket(ticketId: String): Boolean {
        if (closed || availableTicketId() != ticketId) return false
        val restored = ticketRepository.resetValidated()
        if (restored) refreshExposure(forceNotify = true)
        return restored
    }

    fun onTicketOverlayOpened(ticketId: String) {
        if (closed) return
        mainHandler.removeCallbacks(closeAfterUiPause)
        if (availableTicketId() != ticketId) {
            onTicketOverlayClosed()
            return
        }
        requestedTicketId = ticketId
        refreshExposure()
    }

    fun onTicketOverlayPaused(ticketId: String) {
        if (closed || requestedTicketId != ticketId) return
        mainHandler.removeCallbacks(closeAfterUiPause)
        mainHandler.postDelayed(closeAfterUiPause, NfcConfig.UI_PAUSE_GRACE_MILLIS)
    }

    fun onTicketOverlayClosed() {
        mainHandler.removeCallbacks(closeAfterUiPause)
        requestedTicketId = null
        mainHandler.removeCallbacks(renewLease)
        sessionGate.close()
        aidRouter.hideGtt()
        publish(NfcValidationState.Inactive)
    }

    override fun close() {
        if (closed) return
        onTicketOverlayClosed()
        closed = true
        mainHandler.removeCallbacks(expireValidatedTicket)
        ticketObserver.close()
        unregisterAdapterReceiver()
        observers.clear()
    }

    private fun refreshExposure(forceNotify: Boolean = false) {
        mainHandler.removeCallbacks(renewLease)
        mainHandler.removeCallbacks(expireValidatedTicket)
        val ticket = ticketRepository.loadTicket()
        ticket?.let(::scheduleExpiration)
        val ticketId = requestedTicketId
        if (ticketId == null) {
            sessionGate.close()
            aidRouter.hideGtt()
            publish(NfcValidationState.Inactive, forceNotify)
            return
        }
        if (ticket == null || TicketProduct.fromTicket(ticket)?.ticketId != ticketId) {
            sessionGate.close()
            aidRouter.hideGtt()
            ticketRepository.trace("CONTROLLER_ERROR ticket-unavailable")
            publish(NfcValidationState.Error, forceNotify)
            return
        }
        if (!supportsHostCardEmulation()) {
            sessionGate.close()
            aidRouter.hideGtt()
            publish(NfcValidationState.Unsupported, forceNotify)
            return
        }
        if (nfcAdapter?.isEnabled != true) {
            sessionGate.close()
            aidRouter.hideGtt()
            publish(NfcValidationState.Disabled, forceNotify)
            return
        }

        runCatching {
            sessionGate.open(ticketId)
            check(aidRouter.exposeGtt()) { "Unable to register the GTT AID" }
            mainHandler.postDelayed(renewLease, NfcConfig.SESSION_RENEW_INTERVAL_MILLIS)
            when (AepVToken.state(ticket)) {
                AepVToken.STATE_ISSUED -> NfcValidationState.Ready
                AepVToken.STATE_VALIDATED -> NfcValidationState.Validated(
                    timestampMillis = ticketRepository.validatedAtMillis(),
                )
                else -> NfcValidationState.Error
            }
        }.onSuccess { newState -> publish(newState, forceNotify) }.onFailure { error ->
            sessionGate.close()
            aidRouter.hideGtt()
            ticketRepository.trace("CONTROLLER_ERROR ${error.javaClass.simpleName}")
            publish(NfcValidationState.Error, forceNotify)
        }
    }

    private fun scheduleExpiration(ticket: ByteArray) {
        if (AepVToken.state(ticket) != AepVToken.STATE_VALIDATED) return
        val validUntil = ticketRepository.validatedUntilMillis() ?: return
        val delay = (validUntil - System.currentTimeMillis()).coerceAtLeast(0L)
        mainHandler.postDelayed(expireValidatedTicket, delay)
    }

    private fun canExposeTicket(): Boolean =
        requestedTicketId != null && supportsHostCardEmulation() && nfcAdapter?.isEnabled == true

    private fun supportsHostCardEmulation(): Boolean {
        val packageManager = applicationContext.packageManager
        return nfcAdapter != null &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
    }

    private fun publish(newState: NfcValidationState, forceNotify: Boolean = false) {
        if (!forceNotify && state == newState) return
        state = newState
        observers.forEach { observer -> observer(newState) }
    }

    private fun registerAdapterReceiver() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                adapterStateReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            applicationContext.registerReceiver(adapterStateReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterAdapterReceiver() {
        if (!receiverRegistered) return
        applicationContext.unregisterReceiver(adapterStateReceiver)
        receiverRegistered = false
    }
}
