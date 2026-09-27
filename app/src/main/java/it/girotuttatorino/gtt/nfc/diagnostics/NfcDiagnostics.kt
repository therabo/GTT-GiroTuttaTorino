package it.girotuttatorino.gtt.nfc.diagnostics

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import it.girotuttatorino.gtt.BuildConfig
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Metadata only. No filesystem access, waiting, or logcat writes on the HCE thread. */
internal object NfcDiagnostics {
    const val ENABLED = BuildConfig.NFC_DIAGNOSTICS
    private const val TAG = "GttNfcDiag"
    private val sequence = AtomicLong()
    private val dropped = AtomicLong()
    private val processId = UUID.randomUUID().toString()
    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1024),
        { task -> Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "gtt-nfc-diagnostics").apply { isDaemon = true } },
        { _, _ -> dropped.incrementAndGet() },
    )
    @Volatile private var initialized = false
    // Only accessed by the writer thread.
    private var store: DiagnosticLogStore? = null

    fun initialize(context: Context) {
        if (!ENABLED || initialized) return
        try {
            synchronized(this) {
                if (initialized) return
                val application = context.applicationContext
                executor.execute {
                    try {
                        val directory = File(application.noBackupFilesDir, "diagnostics").also {
                            check(it.isDirectory || it.mkdirs())
                        }
                        store = DiagnosticLogStore(directory)
                    } catch (error: Exception) {
                        Log.w(TAG, "Diagnostic storage unavailable: ${error.javaClass.simpleName}")
                    }
                }
                initialized = true
                mark("PROCESS_START", "build=${BuildConfig.BUILD_TYPE} sdk=${Build.VERSION.SDK_INT}")
            }
        } catch (_: Exception) {
            // Diagnostic failures must never change application or APDU behavior.
        }
    }

    fun mark(event: String, detail: String = "", elapsedNanos: Long = 0L) {
        if (!ENABLED) return
        try {
            val id = sequence.incrementAndGet()
            val wallMillis = System.currentTimeMillis()
            val monotonic = elapsedNanos.takeIf { it > 0L } ?: SystemClock.elapsedRealtimeNanos()
            val tid = Process.myTid()
            executor.execute {
                try {
                    val lost = dropped.getAndSet(0)
                    val line = JSONObject()
                        .put("seq", id).put("wall_ms", wallMillis)
                        .put("elapsed_ns", monotonic).put("pid", Process.myPid())
                        .put("process", processId).put("tid", tid)
                        .put("event", event).put("detail", detail)
                        .put("dropped_before", lost).toString()
                    // Logcat remains available if private storage fails.
                    Log.i(TAG, line)
                    store?.append(line, event == "RF_DEACTIVATED" || event == "OVERLAY_CLOSE")
                } catch (error: Exception) {
                    dropped.incrementAndGet()
                    Log.w(TAG, "Diagnostic write failed: ${error.javaClass.simpleName}")
                }
            }
        } catch (_: Exception) {
            dropped.incrementAndGet()
        }
    }

}
