package it.girotuttatorino.gtt.nfc.data

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import it.girotuttatorino.gtt.nfc.core.HceProtocol
import it.girotuttatorino.gtt.nfc.hce.TicketHostApduService
import java.util.Locale

/** Dynamically exposes the GTT AID only while the ticket UI gate is open. */
internal class NfcAidRouter(context: Context) {
    private val applicationContext = context.applicationContext
    private val foregroundActivity = context as? Activity
    private val serviceComponent by lazy {
        ComponentName(applicationContext, TicketHostApduService::class.java)
    }

    fun exposeGtt(): Boolean = runCatching {
        val emulation = cardEmulation()
        val requiredAids = setOf(HceProtocol.PRIVATE_AID_HEX, HceProtocol.GTT_AID_HEX)
        val alreadyRegistered = currentAids(emulation).containsAll(requiredAids)
        val registered = alreadyRegistered || emulation.registerAidsForService(
            serviceComponent,
            CardEmulation.CATEGORY_OTHER,
            requiredAids.toList(),
        )
        val routed = registered && currentAids(emulation).containsAll(requiredAids)
        if (routed) {
            runCatching {
                foregroundActivity?.let { activity ->
                    emulation.setPreferredService(activity, serviceComponent)
                }
            }
        }
        routed
    }.getOrDefault(false)

    fun hideGtt(): Boolean = runCatching {
        val emulation = cardEmulation()
        runCatching {
            foregroundActivity?.let(emulation::unsetPreferredService)
        }
        val current = currentAids(emulation)
        if (current.contains(HceProtocol.GTT_AID_HEX)) {
            emulation.removeAidsForService(serviceComponent, CardEmulation.CATEGORY_OTHER)
            emulation.registerAidsForService(
                serviceComponent,
                CardEmulation.CATEGORY_OTHER,
                listOf(HceProtocol.PRIVATE_AID_HEX),
            )
        }
        !currentAids(emulation).contains(HceProtocol.GTT_AID_HEX)
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
