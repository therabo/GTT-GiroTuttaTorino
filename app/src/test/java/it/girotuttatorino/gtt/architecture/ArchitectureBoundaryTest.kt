package it.girotuttatorino.gtt.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level fitness function for the application's package boundaries. */
class ArchitectureBoundaryTest {
    private val sourceRoot: File = sequenceOf(
        File("src/main/java/it/girotuttatorino/gtt"),
        File("app/src/main/java/it/girotuttatorino/gtt"),
    ).firstOrNull(File::isDirectory)
        ?: error("Android main source directory was not found")

    @Test
    fun coreDoesNotDependOnAndroidOrOuterLayers() {
        assertNoImports(
            directory = "nfc/core",
            forbidden = listOf(
                "android.", "androidx.",
                "it.girotuttatorino.gtt.BuildConfig",
                "it.girotuttatorino.gtt.ui.",
                "it.girotuttatorino.gtt.nfc.data.",
                "it.girotuttatorino.gtt.nfc.hce.",
            ),
        )
    }

    @Test
    fun persistenceAndPresentationDoNotReachIntoHceInternals() {
        assertNoImports("nfc/data", listOf("it.girotuttatorino.gtt.nfc.hce.",
            "it.girotuttatorino.gtt.ui."))
        assertNoImports("ui", listOf("it.girotuttatorino.gtt.nfc.data.",
            "it.girotuttatorino.gtt.nfc.hce."))
    }

    private fun assertNoImports(directory: String, forbidden: List<String>) {
        val files = File(sourceRoot, directory).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertTrue("No Kotlin files found in $directory", files.isNotEmpty())
        val violations = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val name = line.trim().removePrefix("import ")
                if (line.trim().startsWith("import ") && forbidden.any(name::startsWith)) {
                    "${file.relativeTo(sourceRoot)}:${index + 1}: $line"
                } else null
            }
        }
        assertTrue("Architecture boundary violations:\n${violations.joinToString("\n")}",
            violations.isEmpty())
    }
}
