package it.girotuttatorino.gtt.nfc.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import it.girotuttatorino.gtt.nfc.core.HceCardSession

class TicketHostApduService : HostApduService() {
    private lateinit var backend: AndroidCardBackend
    private lateinit var cardSession: HceCardSession

    override fun onCreate() {
        super.onCreate()
        backend = AndroidCardBackend(applicationContext)
        cardSession = HceCardSession(backend)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
        cardSession.process(commandApdu)

    override fun onDeactivated(reason: Int) {
        backend.trace("RF_DEACTIVATED reason=$reason")
        cardSession.deactivate()
    }

    override fun onDestroy() {
        cardSession.deactivate()
        super.onDestroy()
    }
}
