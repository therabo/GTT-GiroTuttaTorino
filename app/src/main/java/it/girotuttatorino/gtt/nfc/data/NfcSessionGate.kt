package it.girotuttatorino.gtt.nfc.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import it.girotuttatorino.gtt.nfc.NfcConfig

internal class NfcSessionGate(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun open(ticketId: String) = synchronized(gateLock) {
        writeLease(ticketId)
    }

    fun renew(ticketId: String): Boolean = synchronized(gateLock) {
        if (!isOpen(ticketId)) return false
        writeLease(ticketId)
        return true
    }

    @SuppressLint("ApplySharedPref")
    fun close() = synchronized(gateLock) {
        // The gate must be closed on disk before a concurrent HCE APDU is accepted.
        cachedLease = null
        preferences.edit().clear().commit()
    }

    fun isOpen(ticketId: String): Boolean = synchronized(gateLock) {
        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        cachedLease?.takeIf { lease ->
            lease.isValid(ticketId, wallNow, elapsedNow)
        }?.let { return@synchronized true }

        val storedLease = Lease(
            ticketId = preferences.getString(KEY_TICKET_ID, null),
            wallDeadline = preferences.getLong(KEY_WALL_DEADLINE, -1L),
            elapsedStart = preferences.getLong(KEY_ELAPSED_START, -1L),
        )
        val valid = storedLease.isValid(ticketId, wallNow, elapsedNow)

        if (valid) {
            cachedLease = storedLease
        } else if (storedLease.ticketId != null) {
            close()
        }
        valid
    }

    private fun writeLease(ticketId: String) {
        val lease = Lease(
            ticketId = ticketId,
            wallDeadline = System.currentTimeMillis() + NfcConfig.SESSION_LEASE_MILLIS,
            elapsedStart = SystemClock.elapsedRealtime(),
        )
        val committed = preferences.edit()
            .putString(KEY_TICKET_ID, ticketId)
            .putLong(KEY_WALL_DEADLINE, lease.wallDeadline)
            .putLong(KEY_ELAPSED_START, lease.elapsedStart)
            .commit()
        check(committed) { "Unable to persist the NFC session lease" }
        cachedLease = lease
    }

    private companion object {
        data class Lease(
            val ticketId: String?,
            val wallDeadline: Long,
            val elapsedStart: Long,
        ) {
            fun isValid(expectedTicketId: String, wallNow: Long, elapsedNow: Long): Boolean =
                ticketId == expectedTicketId &&
                    wallDeadline >= wallNow &&
                    elapsedStart in 0..elapsedNow &&
                    elapsedNow - elapsedStart <= NfcConfig.SESSION_LEASE_MILLIS
        }

        const val PREFERENCES_NAME = "nfc_validation_gate"
        const val KEY_TICKET_ID = "ticket_id"
        const val KEY_WALL_DEADLINE = "wall_deadline"
        const val KEY_ELAPSED_START = "elapsed_start"
        val gateLock = Any()

        @Volatile
        var cachedLease: Lease? = null
    }
}
