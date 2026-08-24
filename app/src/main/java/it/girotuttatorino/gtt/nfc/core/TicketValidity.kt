package it.girotuttatorino.gtt.nfc.core

internal object TicketValidity {
    const val DEFAULT_DURATION_MINUTES = 100
    // AEP encodes validation instants as elapsed minutes from 01/01/2016 00:00
    // in the Italian local time used by the transport system (UTC+1 on that date).
    // This matches the Calendar-based conversion performed by the original VTS SDK.
    private const val EPOCH_2016_ITALY_MILLIS = 1_451_602_800_000L

    fun validationInstantMillis(minutesSince2016: Long?): Long? {
        if (minutesSince2016 == null || minutesSince2016 <= 0L) return null
        val deltaMillis = minutesSince2016 * 60L * 1_000L
        if (deltaMillis / (60L * 1_000L) != minutesSince2016) return null
        return if (EPOCH_2016_ITALY_MILLIS > Long.MAX_VALUE - deltaMillis) {
            null
        } else {
            EPOCH_2016_ITALY_MILLIS + deltaMillis
        }
    }

    fun expiresAt(validatedAtMillis: Long, durationMinutes: Int = DEFAULT_DURATION_MINUTES): Long? {
        if (validatedAtMillis <= 0L || durationMinutes <= 0) return null
        val durationMillis = durationMinutes.toLong() * 60L * 1_000L
        return if (validatedAtMillis > Long.MAX_VALUE - durationMillis) {
            Long.MAX_VALUE
        } else {
            validatedAtMillis + durationMillis
        }
    }

    fun isExpired(validUntilMillis: Long?, nowMillis: Long): Boolean =
        validUntilMillis == null || nowMillis >= validUntilMillis

    fun remainingSeconds(validUntilMillis: Long?, nowMillis: Long): Long {
        if (validUntilMillis == null || nowMillis >= validUntilMillis) return 0L
        return ((validUntilMillis - nowMillis) + 999L) / 1_000L
    }
}
