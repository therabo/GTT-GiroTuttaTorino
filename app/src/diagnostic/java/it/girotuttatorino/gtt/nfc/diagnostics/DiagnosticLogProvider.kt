package it.girotuttatorino.gtt.nfc.diagnostics

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/** Diagnostic build only. Android enforces the shell's DUMP permission before access. */
class DiagnosticLogProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Read-only diagnostic log")
        val name = DiagnosticLogStore.fileNameForPath(uri.path)
            ?: throw FileNotFoundException("Unknown diagnostic log")
        val folder = File(requireNotNull(context).noBackupFilesDir, "diagnostics")
        return ParcelFileDescriptor.open(File(folder, name), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "application/x-ndjson"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Read-only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read-only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Read-only")
}
