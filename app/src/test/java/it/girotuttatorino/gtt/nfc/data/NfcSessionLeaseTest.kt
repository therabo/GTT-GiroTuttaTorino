package it.girotuttatorino.gtt.nfc.data

import it.girotuttatorino.gtt.nfc.NfcConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcSessionLeaseTest {
    private val lease = NfcSessionLease(
        ticketId = "city",
        processInstanceId = "process-a",
        wallDeadline = 1_120_000L,
        elapsedStart = 1_000_000L,
    )

    @Test
    fun leaseIsValidOnlyInsideTheProcessThatOpenedIt() {
        assertTrue(lease.isValid("city", "process-a", 1_010_000L, 1_010_000L))
        assertFalse(lease.isValid("city", "process-b", 1_010_000L, 1_010_000L))
    }

    @Test
    fun legacyLeaseWithoutProcessIdentityIsRejected() {
        val legacyLease = lease.copy(processInstanceId = null)

        assertFalse(legacyLease.isValid("city", "process-a", 1_010_000L, 1_010_000L))
    }

    @Test
    fun leaseExpiresWhenItIsNotRenewed() {
        val elapsedAfterLease = lease.elapsedStart + NfcConfig.SESSION_LEASE_MILLIS + 1L

        assertFalse(lease.isValid("city", "process-a", lease.wallDeadline + 1L, elapsedAfterLease))
    }

    @Test
    fun leaseIsRejectedAfterDeviceReboot() {
        assertFalse(lease.isValid("city", "process-a", 1_010_000L, 100L))
    }
}
