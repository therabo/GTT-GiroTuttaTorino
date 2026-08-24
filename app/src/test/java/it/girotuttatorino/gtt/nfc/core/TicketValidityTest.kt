package it.girotuttatorino.gtt.nfc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketValidityTest {
    @Test
    fun validationExpiresAfterOneHundredMinutes() {
        val validatedAt = 1_700_000_000_000L
        val validUntil = validatedAt + (100L * 60L * 1_000L)

        assertEquals(validUntil, TicketValidity.expiresAt(validatedAt))
        assertFalse(TicketValidity.isExpired(validUntil, validUntil - 1L))
        assertTrue(TicketValidity.isExpired(validUntil, validUntil))
        assertEquals(1L, TicketValidity.remainingSeconds(validUntil, validUntil - 1_000L))
    }

    @Test
    fun missingValidationTimestampIsExpired() {
        assertNull(TicketValidity.expiresAt(-1L))
        assertTrue(TicketValidity.isExpired(null, 1_700_000_000_000L))
        assertEquals(0L, TicketValidity.remainingSeconds(null, 1_700_000_000_000L))
    }

    @Test
    fun vTokenMinutesAreConvertedFromThe2016ItalianLocalEpoch() {
        assertEquals(
            1_451_602_800_000L + (0x005544A2L * 60_000L),
            TicketValidity.validationInstantMillis(0x005544A2L),
        )
        assertNull(TicketValidity.validationInstantMillis(0L))
    }

    @Test
    fun realValidatedTicketStartsWithOneHundredMinutesRemaining() {
        val validatedAt = requireNotNull(TicketValidity.validationInstantMillis(5_588_130L))
        val validUntil = TicketValidity.expiresAt(validatedAt, 100)

        assertEquals(1_786_890_600_000L, validatedAt)
        assertEquals(6_000L, TicketValidity.remainingSeconds(validUntil, validatedAt))
    }
}
