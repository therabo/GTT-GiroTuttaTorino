package it.girotuttatorino.gtt.nfc.diagnostics

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Single-writer, bounded log storage. Must only be called from a background thread. */
internal class DiagnosticLogStore(
    private val folder: File,
    private val maxFileBytes: Long = 2 * 1024 * 1024L,
    private val archives: Int = 3,
) {
    fun append(line: String, durable: Boolean) {
        val bytes = (line + "\n").toByteArray(Charsets.UTF_8)
        val current = File(folder, "nfc-events.jsonl")
        if (current.length() > 0 && current.length() + bytes.size > maxFileBytes) {
            for (index in archives downTo 1) {
                val source = if (index == 1) current else File(folder, "nfc-events.jsonl.${index - 1}")
                if (source.exists()) {
                    Files.move(source.toPath(), File(folder, "nfc-events.jsonl.$index").toPath(),
                        StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        FileOutputStream(current, true).use { stream ->
            stream.write(bytes)
            if (durable) stream.fd.sync()
        }
    }

    companion object {
        fun fileNameForPath(path: String?): String? = when (path) {
            "/events/current" -> "nfc-events.jsonl"
            "/events/1" -> "nfc-events.jsonl.1"
            "/events/2" -> "nfc-events.jsonl.2"
            "/events/3" -> "nfc-events.jsonl.3"
            else -> null
        }
    }
}
