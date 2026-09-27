package it.girotuttatorino.gtt.nfc.hce

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import java.util.Locale
import it.girotuttatorino.gtt.nfc.diagnostics.NfcDiagnostics

/** Gives the manifest-declared HCE route priority while the ticket UI is foreground. */
internal class NfcAidRouter(context: Context) {
    private val applicationContext = context.applicationContext
    private val foregroundActivity = context as? Activity
    private val serviceComponent by lazy {
        ComponentName(applicationContext, TicketHostApduService::class.java)
    }

    fun prepareGtt(): Boolean = runCatching {
        NfcDiagnostics.mark("ROUTE_PREPARE_ENTER",
            "activity_present=${foregroundActivity != null} finishing=${foregroundActivity?.isFinishing} destroyed=${foregroundActivity?.isDestroyed}")
        val emulation = cardEmulation()
        removeLegacyDynamicRoute(emulation)
        val activity = requireNotNull(foregroundActivity) {
            "A foreground activity is required to prepare NFC"
        }
        emulation.setPreferredService(activity, serviceComponent).also {
            NfcDiagnostics.mark("ROUTE_PREFERRED_RESULT", "accepted=$it")
        }
    }.onFailure {
        NfcDiagnostics.mark("ROUTE_PREPARE_ERROR", "error=${it.javaClass.simpleName}")
    }.getOrDefault(false)

    fun releasePreference(): Boolean = runCatching {
        NfcDiagnostics.mark("ROUTE_RELEASE_ENTER")
        val emulation = cardEmulation()
        runCatching {
            foregroundActivity?.let(emulation::unsetPreferredService).also {
                NfcDiagnostics.mark("ROUTE_RELEASE_RESULT", "accepted=$it")
            }
        }.onFailure {
            NfcDiagnostics.mark("ROUTE_RELEASE_ERROR", "error=${it.javaClass.simpleName}")
        }
        true
    }.onFailure {
        NfcDiagnostics.mark("ROUTE_RELEASE_ERROR", "error=${it.javaClass.simpleName}")
    }.getOrDefault(false)

    private fun cardEmulation(): CardEmulation {
        val adapter = NfcAdapter.getDefaultAdapter(applicationContext)
            ?: error("NFC unavailable")
        return CardEmulation.getInstance(adapter)
    }

    /** Dynamic registrations override the manifest and may survive an application upgrade. */
    private fun removeLegacyDynamicRoute(emulation: CardEmulation) {
        if (currentDynamicAids(emulation).isNotEmpty()) {
            NfcDiagnostics.mark("ROUTE_LEGACY_CLEANUP")
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
