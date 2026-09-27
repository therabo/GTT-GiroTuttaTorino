package it.girotuttatorino.gtt.nfc.hce

import android.content.Context
import it.girotuttatorino.gtt.nfc.core.CardBackend
import it.girotuttatorino.gtt.nfc.core.TicketProduct
import it.girotuttatorino.gtt.nfc.data.NfcSessionGate
import it.girotuttatorino.gtt.nfc.data.TicketRepository
import it.girotuttatorino.gtt.nfc.diagnostics.NfcDiagnostics

internal class AndroidCardBackend(context: Context) : CardBackend {
    private val applicationContext = context.applicationContext
    private val ticketRepository = TicketRepository(applicationContext)
    private val sessionGate = NfcSessionGate(applicationContext)

    override fun isValidationSessionActive(): Boolean {
        val ticketId = TicketProduct.fromTicket(ticketRepository.loadTicket())?.ticketId
            ?: return false
        return sessionGate.isOpen(ticketId)
    }

    override fun deviceUid(): ByteArray = ticketRepository.deviceUid()

    override fun loadTicket(): ByteArray? = ticketRepository.loadTicket()

    override fun compareAndCommit(expected: ByteArray, replacement: ByteArray): Boolean =
        ticketRepository.compareAndCommit(expected, replacement)

    override fun onCommitted() {
        ticketRepository.trace("A7 completato: ticket validato disponibile per la verifica NFC")
    }

    override fun trace(event: String) {
        // Include protocol phase/reject reason, never hash, correlation or payload.
        if (NfcDiagnostics.ENABLED) {
            NfcDiagnostics.mark("CORE_EVENT", event.split(' ', limit = 3).take(2).joinToString(" "))
        }
        ticketRepository.trace(event)
    }
}
