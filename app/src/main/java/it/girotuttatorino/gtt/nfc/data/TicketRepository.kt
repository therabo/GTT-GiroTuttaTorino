package it.girotuttatorino.gtt.nfc.data

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import android.util.Base64
import it.girotuttatorino.gtt.nfc.core.AepVToken
import it.girotuttatorino.gtt.nfc.core.ByteCodec
import it.girotuttatorino.gtt.nfc.core.TicketProduct
import it.girotuttatorino.gtt.nfc.core.TicketValidity
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/** Atomic storage under the application's private, non-backed-up data directory. */
internal class TicketRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val ticketsDirectory = File(
        applicationContext.noBackupFilesDir,
        PRIVATE_TICKETS_DIRECTORY,
    )
    private val originalTicketFile = File(ticketsDirectory, ORIGINAL_TICKET_FILE)
    private val activeTicketFile = File(ticketsDirectory, ACTIVE_TICKET_FILE)
    private val legacyCityTicketsDirectory = File(
        applicationContext.noBackupFilesDir,
        LEGACY_PRIVATE_TICKETS_DIRECTORY,
    )

    fun ensureTicket(): ByteArray = synchronized(storageLock) {
        migrateLegacyStorageLocked()
        expireValidatedIfNeededLocked(System.currentTimeMillis())
        currentTicketLocked()?.copyOf()
            ?: restoreProvisionedTicketLocked()
    }

    fun loadTicket(): ByteArray? = synchronized(storageLock) {
        migrateLegacyStorageLocked()
        expireValidatedIfNeededLocked(System.currentTimeMillis())
        (currentTicketLocked() ?: restoreProvisionedTicketLockedOrNull())?.copyOf()
    }

    fun loadBefore(): ByteArray? = synchronized(storageLock) {
        migrateLegacyStorageLocked()
        readIssuedOriginalLocked()?.copyOf()
    }

    fun loadValidated(): ByteArray? = synchronized(storageLock) {
        migrateLegacyStorageLocked()
        expireValidatedIfNeededLocked(System.currentTimeMillis())
        currentTicketLocked()?.takeIf(AepVToken::isValidated)?.copyOf()
    }

    fun compareAndCommit(expected: ByteArray, replacement: ByteArray): Boolean =
        synchronized(storageLock) {
            migrateLegacyStorageLocked()
            expireValidatedIfNeededLocked(System.currentTimeMillis())
            val current = currentTicketLocked()
            val uid = AepVToken.deviceUid(expected) ?: return@synchronized false
            if (current == null || !current.contentEquals(expected) ||
                !AepVToken.validValidatedTransition(expected, replacement, uid)
            ) {
                return@synchronized false
            }

            val wasAlreadyValidated = AepVToken.isValidated(current)
            val product = TicketProduct.fromTicket(replacement) ?: return@synchronized false
            val nowMillis = System.currentTimeMillis()
            val replacementValidation = AepVToken.validationInfo(replacement)
            val validatedAt = if (wasAlreadyValidated) {
                preferences.getLong(KEY_VALIDATED_AT, nowMillis)
            } else {
                TicketValidity.validationInstantMillis(
                    replacementValidation?.firstValidationMinutesSince2016,
                ) ?: nowMillis
            }
            val validUntil = if (wasAlreadyValidated) {
                validatedDeadlineLocked(current)
            } else {
                val durationMinutes = replacementValidation?.minutesToGo
                    ?.takeIf { it > 0 }
                    ?: product.fallbackDurationMinutes
                    ?: return@synchronized false
                TicketValidity.expiresAt(validatedAt, durationMinutes)
            } ?: return@synchronized false

            writePrivateTicket(activeTicketFile, replacement)
            val committed = preferences.edit()
                .putLong(KEY_VALIDATED_AT, validatedAt)
                .putLong(KEY_VALID_UNTIL, validUntil)
                .putInt(KEY_STORAGE_REVISION, nextStorageRevision())
                .commit()
            if (committed) {
                cachedTicket = replacement.copyOf()
            } else {
                writePrivateTicket(activeTicketFile, current)
            }
            committed
        }

    fun deviceUid(): ByteArray =
        AepVToken.deviceUid(ensureTicket())?.copyOf()
            ?: error("Private AEPV DeviceUID is missing")

    fun validatedAtMillis(): Long =
        preferences.getLong(KEY_VALIDATED_AT, System.currentTimeMillis())

    fun validatedUntilMillis(): Long? = synchronized(storageLock) {
        expireValidatedIfNeededLocked(System.currentTimeMillis())
        val current = currentTicketLocked()
        if (!AepVToken.isValidated(current)) return@synchronized null
        validatedDeadlineLocked(requireNotNull(current))
    }

    fun expireValidatedIfNeeded(nowMillis: Long = System.currentTimeMillis()): Boolean =
        synchronized(storageLock) {
            expireValidatedIfNeededLocked(nowMillis)
        }

    fun resetValidated(): Boolean = synchronized(storageLock) {
        migrateLegacyStorageLocked()
        val current = currentTicketLocked()
        if (!AepVToken.isValidated(current)) return@synchronized false
        restoreIssuedOriginalLocked(
            validatedTicket = requireNotNull(current),
            traceEvent = "TICKET RESET manual",
        )
    }

    fun observeTicketChanges(onChanged: () -> Unit): AutoCloseable {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_STORAGE_REVISION) onChanged()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return AutoCloseable {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun trace(event: String) {
        val line = "${System.currentTimeMillis()} $event"
        traceExecutor.execute {
            synchronized(traceLock) {
                val existing = preferences.getString(KEY_TRACE, null)
                    .orEmpty()
                    .lineSequence()
                    .filter(String::isNotBlank)
                    .toMutableList()
                existing += line
                preferences.edit()
                    .putString(KEY_TRACE, existing.takeLast(MAX_TRACE_LINES).joinToString("\n"))
                    .apply()
            }
        }
    }

    private fun restoreProvisionedTicketLocked(): ByteArray =
        restoreProvisionedTicketLockedOrNull()
            ?: error("The private AEPV token has not been provisioned")

    private fun restoreProvisionedTicketLockedOrNull(): ByteArray? {
        val ticket = readIssuedOriginalLocked() ?: return null
        val uid = AepVToken.deviceUid(ticket)
            ?: error("Private AEPV DeviceUID is missing")
        check(AepVToken.isIssuedToken(ticket, uid)) { "Private AEPV token is invalid" }
        check(TicketProduct.fromTicket(ticket) != null) { "Unsupported GTT fare product" }

        writePrivateTicket(activeTicketFile, ticket)
        val committed = preferences.edit()
            .remove(KEY_VALIDATED_AT)
            .remove(KEY_VALID_UNTIL)
            .putInt(KEY_STORAGE_REVISION, nextStorageRevision())
            .commit()
        check(committed) { "Unable to activate the private AEPV token" }
        cachedTicket = ticket.copyOf()
        trace("TICKET ACTIVATE bytes=${ticket.size} hash=${ByteCodec.shortHash(ticket)}")
        return ticket.copyOf()
    }

    private fun expireValidatedIfNeededLocked(nowMillis: Long): Boolean {
        val current = currentTicketLocked()
        if (!AepVToken.isValidated(current)) return false

        val validUntil = validatedDeadlineLocked(requireNotNull(current))
        if (!TicketValidity.isExpired(validUntil, nowMillis)) return false

        return restoreIssuedOriginalLocked(
            validatedTicket = current,
            traceEvent = "TICKET EXPIRED",
        )
    }

    private fun restoreIssuedOriginalLocked(
        validatedTicket: ByteArray,
        traceEvent: String,
    ): Boolean {
        val before = readIssuedOriginalLocked()
            ?: error("The pre-validation AEPV token is unavailable")

        writePrivateTicket(activeTicketFile, before)
        val committed = preferences.edit()
            .remove(KEY_VALIDATED_AT)
            .remove(KEY_VALID_UNTIL)
            .putInt(KEY_STORAGE_REVISION, nextStorageRevision())
            .commit()
        if (!committed) {
            writePrivateTicket(activeTicketFile, validatedTicket)
            error("Unable to restore the pre-validation AEPV token")
        }
        cachedTicket = before.copyOf()
        trace("$traceEvent restored=${ByteCodec.shortHash(before)}")
        return true
    }

    private fun currentTicketLocked(): ByteArray? {
        if (!activeTicketFile.isFile) cachedTicket = null
        cachedTicket?.takeIf(::isSupportedTicket)?.let { return it }
        return readPrivateTicket(activeTicketFile)
            ?.takeIf(::isSupportedTicket)
            ?.also { cachedTicket = it.copyOf() }
    }

    /** One-time upgrade from the previous MODE_PRIVATE SharedPreferences storage. */
    private fun migrateLegacyStorageLocked() {
        var changed = migrateLegacyPrivateDirectoryLocked()
        val legacyActive = decodeTicket(preferences.getString(LEGACY_KEY_TICKET, null))
            ?.takeIf(::isSupportedTicket)
        val legacyOriginal = decodeTicket(preferences.getString(LEGACY_KEY_BEFORE, null))
            ?.takeIf { ticket ->
                val uid = AepVToken.deviceUid(ticket)
                uid != null && AepVToken.isIssuedToken(ticket, uid)
            }

        if (!originalTicketFile.isFile && legacyOriginal != null) {
            writePrivateTicket(originalTicketFile, legacyOriginal)
            changed = true
        }
        if (!activeTicketFile.isFile) {
            val active = legacyActive ?: legacyOriginal
            if (active != null) {
                writePrivateTicket(activeTicketFile, active)
                cachedTicket = active.copyOf()
                changed = true
            }
        }
        if (legacyActive != null || legacyOriginal != null ||
            preferences.contains(LEGACY_KEY_VALIDATED)
        ) {
            val editor = preferences.edit()
                .remove(LEGACY_KEY_TICKET)
                .remove(LEGACY_KEY_BEFORE)
                .remove(LEGACY_KEY_VALIDATED)
            if (changed) editor.putInt(KEY_STORAGE_REVISION, nextStorageRevision())
            check(editor.commit()) { "Unable to finish private ticket migration" }
        }
        migrateLegacyUtcValidationTimingLocked()
    }

    /** Copies tickets provisioned by builds that used the product-specific city path. */
    private fun migrateLegacyPrivateDirectoryLocked(): Boolean {
        if (!legacyCityTicketsDirectory.isDirectory ||
            legacyCityTicketsDirectory == ticketsDirectory
        ) {
            return false
        }
        var changed = false
        val legacyOriginal = readPrivateTicket(
            File(legacyCityTicketsDirectory, ORIGINAL_TICKET_FILE),
        )?.takeIf { ticket ->
            val uid = AepVToken.deviceUid(ticket)
            uid != null && AepVToken.isIssuedToken(ticket, uid) &&
                TicketProduct.fromTicket(ticket) != null
        }
        val legacyActive = readPrivateTicket(
            File(legacyCityTicketsDirectory, ACTIVE_TICKET_FILE),
        )?.takeIf(::isSupportedTicket)

        if (!originalTicketFile.isFile && legacyOriginal != null) {
            writePrivateTicket(originalTicketFile, legacyOriginal)
            changed = true
        }
        if (!activeTicketFile.isFile && (legacyActive ?: legacyOriginal) != null) {
            writePrivateTicket(activeTicketFile, requireNotNull(legacyActive ?: legacyOriginal))
            cachedTicket = (legacyActive ?: legacyOriginal)?.copyOf()
            changed = true
        }
        return changed
    }

    /** Repairs deadlines persisted by builds that interpreted the AEP epoch as UTC. */
    private fun migrateLegacyUtcValidationTimingLocked() {
        val current = currentTicketLocked()?.takeIf(AepVToken::isValidated) ?: return
        val tokenValidatedAt = TicketValidity.validationInstantMillis(
            AepVToken.validationInfo(current)?.firstValidationMinutesSince2016,
        ) ?: return
        val storedValidatedAt = preferences.getLong(KEY_VALIDATED_AT, -1L)
        if (storedValidatedAt != tokenValidatedAt + LEGACY_UTC_EPOCH_OFFSET_MILLIS) return

        val editor = preferences.edit().putLong(KEY_VALIDATED_AT, tokenValidatedAt)
        preferences.getLong(KEY_VALID_UNTIL, -1L)
            .takeIf { it > LEGACY_UTC_EPOCH_OFFSET_MILLIS }
            ?.let { legacyDeadline ->
                editor.putLong(
                    KEY_VALID_UNTIL,
                    legacyDeadline - LEGACY_UTC_EPOCH_OFFSET_MILLIS,
                )
            }
        check(editor.commit()) { "Unable to migrate the validation timing epoch" }
    }

    private fun readIssuedOriginalLocked(): ByteArray? =
        readPrivateTicket(originalTicketFile)?.takeIf { ticket ->
            val uid = AepVToken.deviceUid(ticket)
            uid != null && AepVToken.isIssuedToken(ticket, uid) &&
                TicketProduct.fromTicket(ticket) != null
        }

    private fun isSupportedTicket(ticket: ByteArray): Boolean =
        AepVToken.basicShape(ticket) && TicketProduct.fromTicket(ticket) != null

    private fun readPrivateTicket(file: File): ByteArray? {
        if (!file.isFile) return null
        return runCatching { AtomicFile(file).openRead().use { it.readBytes() } }
            .getOrNull()
            ?.takeIf { it.size in 57..AepVToken.MAX_LENGTH }
    }

    private fun writePrivateTicket(file: File, ticket: ByteArray) {
        check(AepVToken.basicShape(ticket)) { "Refusing to store an invalid AEPV token" }
        preparePrivateDirectory()
        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(ticket)
            stream.fd.sync()
            atomicFile.finishWrite(stream)
            hardenPrivateFile(file)
        } catch (error: Throwable) {
            stream?.let(atomicFile::failWrite)
            throw error
        }
    }

    private fun preparePrivateDirectory() {
        check(ticketsDirectory.isDirectory || ticketsDirectory.mkdirs()) {
            "Unable to create the private ticket directory"
        }
        ticketsDirectory.setReadable(false, false)
        ticketsDirectory.setWritable(false, false)
        ticketsDirectory.setExecutable(false, false)
        check(ticketsDirectory.setReadable(true, true))
        check(ticketsDirectory.setWritable(true, true))
        check(ticketsDirectory.setExecutable(true, true))
    }

    private fun hardenPrivateFile(file: File) {
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setExecutable(false, false)
        check(file.setReadable(true, true))
        check(file.setWritable(true, true))
    }

    private fun nextStorageRevision(): Int =
        preferences.getInt(KEY_STORAGE_REVISION, 0) + 1

    private fun validatedDeadlineLocked(ticket: ByteArray): Long? {
        preferences.getLong(KEY_VALID_UNTIL, -1L)
            .takeIf { it > 0L }
            ?.let { return it }
        val validatedAt = preferences.getLong(KEY_VALIDATED_AT, -1L)
        val durationMinutes = AepVToken.validationInfo(ticket)?.minutesToGo
            ?.takeIf { it > 0 }
            ?: TicketProduct.fromTicket(ticket)?.fallbackDurationMinutes
            ?: return null
        return TicketValidity.expiresAt(validatedAt, durationMinutes)
    }

    private fun decodeTicket(encoded: String?): ByteArray? {
        if (encoded.isNullOrBlank()) return null
        return runCatching { Base64.decode(encoded, Base64.NO_WRAP) }
            .getOrNull()
            ?.takeIf { it.size in 57..AepVToken.MAX_LENGTH }
    }

    private companion object {
        const val PREFERENCES_NAME = "nfc_ticket_store"
        const val PRIVATE_TICKETS_DIRECTORY = "tickets/current"
        const val LEGACY_PRIVATE_TICKETS_DIRECTORY = "tickets/city"
        const val ORIGINAL_TICKET_FILE = "original.vtoken"
        const val ACTIVE_TICKET_FILE = "active.vtoken"
        const val LEGACY_KEY_TICKET = "city_ticket"
        const val LEGACY_KEY_BEFORE = "city_ticket_before"
        const val LEGACY_KEY_VALIDATED = "city_ticket_validated"
        const val KEY_VALIDATED_AT = "city_ticket_validated_at"
        const val KEY_VALID_UNTIL = "city_ticket_valid_until"
        const val KEY_STORAGE_REVISION = "city_ticket_storage_revision"
        const val KEY_TRACE = "protocol_trace"
        const val MAX_TRACE_LINES = 120
        const val LEGACY_UTC_EPOCH_OFFSET_MILLIS = 60L * 60L * 1_000L

        val storageLock = Any()
        val traceLock = Any()
        var cachedTicket: ByteArray? = null
        val traceExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "gtt-nfc-trace").apply { isDaemon = true }
        }
    }
}
