package fulguris.utils

import android.content.Context
import android.os.Build
import android.util.Log
import fulguris.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Alir diagnostic crash reporter (v1.0.5+).
 *
 * A normal force-close leaves no trace a user can share, so this catches
 * uncaught exceptions, saves the full stack trace to internal storage, and
 * chains to the previous handler so the system still shows its force-close
 * dialog. The saved report is shown on the next launch by
 * [fulguris.activity.CrashReportActivity], a minimal activity that cannot
 * hit the browser startup crash (v1.0.6 moved it out of a dialog at the end
 * of the browser's onCreate, which died with the activity when the app
 * crashed again during startup).
 *
 * It only covers Kotlin/Java exceptions. A native crash (e.g. inside the
 * WebView renderer) will NOT produce a report — that absence is itself a
 * diagnostic signal.
 */
object CrashReporter {

    private const val FILE_NAME = "alir_crash.txt"
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val appContext = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val report = buildString {
                    appendLine("Alir crash report")
                    appendLine(
                        "Waktu: " +
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    )
                    appendLine(
                        "Aplikasi: ${BuildConfig.VERSION_NAME} " +
                            "(code ${BuildConfig.VERSION_CODE})"
                    )
                    appendLine(
                        "HP: ${Build.MANUFACTURER} ${Build.MODEL} — " +
                            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
                    )
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(Log.getStackTraceString(error))
                }
                File(appContext.filesDir, FILE_NAME).writeText(report)
            } catch (ignored: Exception) {
                // The reporter must never crash the crash path.
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    /** The saved report from a previous crashed run, or null when there is none. */
    fun pendingReport(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val report = try {
            file.readText()
        } catch (e: Exception) {
            return null
        }
        if (report.isBlank()) {
            file.delete()
            return null
        }
        return report
    }

    fun clearReport(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (ignored: Exception) {
            // Best effort; a leftover file just shows the report once more.
        }
    }
}
