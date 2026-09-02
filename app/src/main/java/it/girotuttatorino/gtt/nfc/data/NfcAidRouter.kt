package it.girotuttatorino.gtt.nfc.data

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import it.girotuttatorino.gtt.nfc.hce.TicketHostApduService
import java.util.Locale

/** Gives the manifest-declared HCE route priority while the ticket UI is foreground. */
internal class NfcAidRouter(context: Context) {
    private val applicationContext = context.applicationContext
    private val foregroundActivity = context as? Activity
    private val serviceComponent by lazy {
        ComponentName(applicationContext, TicketHostApduService::class.java)
    }

    fun prepareGtt(): Boolean = runCatching {
        val emulation = cardEmulation()
        removeLegacyDynamicRoute(emulation)
        val activity = requireNotNull(foregroundActivity) {
            "A foreground activity is required to prepare NFC"
        }
        emulation.setPreferredService(activity, serviceComponent)
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

    /** Dynamic registrations override the manifest and may survive an application upgrade. */
    private fun removeLegacyDynamicRoute(emulation: CardEmulation) {
        if (currentDynamicAids(emulation).isNotEmpty()) {
            check(
                emulation.removeAidsForService(
                    serviceComponent,
                    CardEmulation.CATEGORY_OTHER,
                ),
            ) { "Unable to remove the legacy dynamic AID group" }
        }
    }

    private fun currentDynamicAids(emulation: CardEmulation): Set<String> =
        emulation.getAidsForService(serviceComponent, CardEmulation.CATEGORY_OTHER)
            .orEmpty()
            .mapTo(mutableSetOf()) { it.uppercase(Locale.US) }
}
