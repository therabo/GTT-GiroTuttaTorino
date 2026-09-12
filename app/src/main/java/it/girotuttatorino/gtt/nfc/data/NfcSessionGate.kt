package it.girotuttatorino.gtt.nfc.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import it.girotuttatorino.gtt.nfc.NfcConfig
import java.util.UUID

internal data class NfcSessionLease(
    val ticketId: String?,
    val processInstanceId: String?,
    val wallDeadline: Long,
    val elapsedStart: Long,
) {
    fun isValid(
        expectedTicketId: String,
        expectedProcessInstanceId: String,
        wallNow: Long,
        elapsedNow: Long,
    ): Boolean =
        ticketId == expectedTicketId &&
            processInstanceId == expectedProcessInstanceId &&
            wallDeadline >= wallNow &&
            elapsedStart in 0..elapsedNow &&
            elapsedNow - elapsedStart <= NfcConfig.SESSION_LEASE_MILLIS
}

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
            lease.isValid(ticketId, processInstanceId, wallNow, elapsedNow)
        }?.let { return@synchronized true }

        val storedLease = NfcSessionLease(
            ticketId = preferences.getString(KEY_TICKET_ID, null),
            processInstanceId = preferences.getString(KEY_PROCESS_INSTANCE_ID, null),
            wallDeadline = preferences.getLong(KEY_WALL_DEADLINE, -1L),
            elapsedStart = preferences.getLong(KEY_ELAPSED_START, -1L),
        )
        val valid = storedLease.isValid(ticketId, processInstanceId, wallNow, elapsedNow)

        if (valid) {
            cachedLease = storedLease
        } else if (storedLease.ticketId != null) {
            close()
        }
        valid
    }

    private fun writeLease(ticketId: String) {
        val lease = NfcSessionLease(
            ticketId = ticketId,
            processInstanceId = processInstanceId,
            wallDeadline = System.currentTimeMillis() + NfcConfig.SESSION_LEASE_MILLIS,
            elapsedStart = SystemClock.elapsedRealtime(),
        )
        val committed = preferences.edit()
            .putString(KEY_TICKET_ID, ticketId)
            .putString(KEY_PROCESS_INSTANCE_ID, processInstanceId)
            .putLong(KEY_WALL_DEADLINE, lease.wallDeadline)
            .putLong(KEY_ELAPSED_START, lease.elapsedStart)
            .commit()
        check(committed) { "Unable to persist the NFC session lease" }
        cachedLease = lease
    }

    private companion object {
        const val PREFERENCES_NAME = "nfc_validation_gate"
        const val KEY_TICKET_ID = "ticket_id"
        const val KEY_PROCESS_INSTANCE_ID = "process_instance_id"
        const val KEY_WALL_DEADLINE = "wall_deadline"
        const val KEY_ELAPSED_START = "elapsed_start"
        val gateLock = Any()
        val processInstanceId: String = UUID.randomUUID().toString()

        @Volatile
        var cachedLease: NfcSessionLease? = null
    }
}
