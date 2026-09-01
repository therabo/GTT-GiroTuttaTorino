package it.girotuttatorino.gtt.nfc.data

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import it.girotuttatorino.gtt.nfc.core.HceProtocol
import it.girotuttatorino.gtt.nfc.hce.TicketHostApduService
import java.util.Locale

/** Verifies the static HCE route and gives it priority while the ticket UI is foreground. */
internal class NfcAidRouter(context: Context) {
    private val applicationContext = context.applicationContext
    private val foregroundActivity = context as? Activity
    private val serviceComponent by lazy {
        ComponentName(applicationContext, TicketHostApduService::class.java)
    }

    /** Clears AID groups persisted by dynamic-routing builds, then verifies the manifest route. */
    fun restoreStaticRoute(): Boolean = runCatching {
        val emulation = cardEmulation()
        emulation.removeAidsForService(serviceComponent, CardEmulation.CATEGORY_OTHER)
        val requiredAids = setOf(HceProtocol.PRIVATE_AID_HEX, HceProtocol.GTT_AID_HEX)
        currentAids(emulation).containsAll(requiredAids)
    }.getOrDefault(false)

    fun prepareGtt(): Boolean = runCatching {
        val routed = restoreStaticRoute()
        if (routed) {
            val emulation = cardEmulation()
            runCatching {
                foregroundActivity?.let { activity ->
                    emulation.setPreferredService(activity, serviceComponent)
                }
            }
        }
        routed
    }.getOrDefault(false)

    fun releasePreference(): Boolean = runCatching {
        val emulation = cardEmulation()
        runCatching {
            foregroundActivity?.let(emulation::unsetPreferredService)
        }
        true
    }.getOrDefault(false)

    private fun cardEmulation(): CardEmulation {
        val adapter = NfcAdapter.getDefaultAdapter(applicationContext)
            ?: error("NFC unavailable")
        return CardEmulation.getInstance(adapter)
    }

    private fun currentAids(emulation: CardEmulation): Set<String> =
        emulation.getAidsForService(serviceComponent, CardEmulation.CATEGORY_OTHER)
            .orEmpty()
            .mapTo(mutableSetOf()) { it.uppercase(Locale.US) }
}
