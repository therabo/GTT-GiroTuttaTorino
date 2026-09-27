package it.girotuttatorino.gtt.nfc.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class DiagnosticLogStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun consecutiveWritesAndRotationPreserveRecentSessionsWithBoundedDiskUsage() {
        val folder = temporary.newFolder()
        val store = DiagnosticLogStore(folder, maxFileBytes = 12)
        repeat(20) { store.append("session-$it", durable = true) }
        val files = requireNotNull(folder.listFiles())
        assertEquals(4, files.size)
        assertTrue(files.all { it.length() <= 12 })
        val lines = listOf("nfc-events.jsonl.3", "nfc-events.jsonl.2", "nfc-events.jsonl.1", "nfc-events.jsonl")
            .flatMap { File(folder, it).readLines() }
        assertEquals((16..19).map { "session-$it" }, lines)
    }

    @Test fun newProcessAppendsInsteadOfErasingPreviousProcessLogs() {
        val folder = temporary.newFolder()
        DiagnosticLogStore(folder).append("before-restart", durable = true)
        DiagnosticLogStore(folder).append("after-restart", durable = true)
        assertEquals(listOf("before-restart", "after-restart"), File(folder, "nfc-events.jsonl").readLines())
    }

    @Test fun diagnosticExportOnlyResolvesTheFourWhitelistedLogFiles() {
        assertEquals("nfc-events.jsonl", DiagnosticLogStore.fileNameForPath("/events/current"))
        assertEquals("nfc-events.jsonl.3", DiagnosticLogStore.fileNameForPath("/events/3"))
        listOf(null, "/events/4", "/events/../tickets", "/tickets/original.vtoken",
            "/events/current/../original.vtoken").forEach {
            assertNull(DiagnosticLogStore.fileNameForPath(it))
        }
    }

    @Test fun unavailableDiagnosticDirectoryDoesNotOverwriteAnotherFile() {
        val occupied = temporary.newFile()
        occupied.writeText("existing")
        try {
            DiagnosticLogStore(occupied).append("log", durable = false)
            throw AssertionError("Expected storage error")
        } catch (_: IOException) {
            assertEquals("existing", occupied.readText())
        }
    }
}
