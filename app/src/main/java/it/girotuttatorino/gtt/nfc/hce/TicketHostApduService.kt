package it.girotuttatorino.gtt.nfc.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.SystemClock
import it.girotuttatorino.gtt.BuildConfig
import it.girotuttatorino.gtt.nfc.core.HceCardSession
import it.girotuttatorino.gtt.nfc.diagnostics.NfcDiagnostics

class TicketHostApduService : HostApduService() {
    private lateinit var backend: AndroidCardBackend
    private lateinit var cardSession: HceCardSession
    private var commandSequence = 0L
    private var rfSession = 0L

    override fun onCreate() {
        super.onCreate()
        NfcDiagnostics.initialize(applicationContext)
        NfcDiagnostics.mark("HCE_CREATE_ENTER")
        backend = AndroidCardBackend(applicationContext)
        cardSession = HceCardSession(backend, BuildConfig.GTT_HCE_AID)
        NfcDiagnostics.mark("HCE_CREATE_EXIT")
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (!NfcDiagnostics.ENABLED) return cardSession.process(commandApdu)
        val entered = SystemClock.elapsedRealtimeNanos()
        val instruction = commandApdu?.getOrNull(1)?.toInt()?.and(0xff) ?: -1
        if (instruction == 0xA4) rfSession++
        val commandId = ++commandSequence
        val reference = "rf=$rfSession command=$commandId ins=$instruction"
        NfcDiagnostics.mark("APDU_ENTER", "$reference request_bytes=${commandApdu?.size ?: 0}", entered)
        try {
            val response = cardSession.process(commandApdu)
            val returned = SystemClock.elapsedRealtimeNanos()
            val status = if (response.size >= 2) {
                ((response[response.size - 2].toInt() and 0xff) shl 8) or
                    (response.last().toInt() and 0xff)
            } else -1
            NfcDiagnostics.mark("APDU_EXIT",
                "$reference response_bytes=${response.size} status=$status duration_us=${(returned - entered) / 1000}",
                returned)
            return response
        } catch (error: Exception) {
            NfcDiagnostics.mark("APDU_EXCEPTION", "$reference error=${error.javaClass.simpleName}")
            throw error
        }
    }

    override fun onDeactivated(reason: Int) {
        NfcDiagnostics.mark("RF_DEACTIVATED", "rf=$rfSession reason=$reason")
        backend.trace("RF_DEACTIVATED reason=$reason")
        cardSession.deactivate()
    }

    override fun onDestroy() {
        NfcDiagnostics.mark("HCE_DESTROY", "rf=$rfSession")
        cardSession.deactivate()
        super.onDestroy()
    }
}
