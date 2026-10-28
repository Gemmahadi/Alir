package fulguris.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import fulguris.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Alir diagnostic crash reporter (v1.0.5).
 *
 * A normal force-close leaves no trace a user can share, so this catches
 * uncaught exceptions, saves the full stack trace to internal storage, and
 * shows it on the next launch with a copy button. It never hides the crash
 * itself: after saving, it chains to the previous handler so the system
 * still shows its force-close dialog.
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

    /**
     * If a crash report from a previous run exists, show it once. Either
     * button deletes the file so it does not reappear on later launches.
     */
    fun showIfPending(activity: Activity) {
        val file = File(activity.filesDir, FILE_NAME)
        if (!file.exists()) return
        val report = try {
            file.readText()
        } catch (e: Exception) {
            return
        }
        if (report.isBlank()) {
            file.delete()
            return
        }
        val text = TextView(activity).apply {
            this.text = report
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(32, 32, 32, 32)
        }
        val scroll = ScrollView(activity).apply {
            addView(
                text,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        AlertDialog.Builder(activity)
            .setTitle("Alir sempat crash")
            .setMessage(
                "Ini penyebab crash terakhir. Salin lalu kirim ke developer, " +
                    "atau tutup untuk menghapus laporan ini."
            )
            .setView(scroll)
            .setPositiveButton("Salin") { _, _ ->
                val clipboard =
                    activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Alir crash report", report)
                )
                Toast.makeText(activity, "Laporan crash disalin", Toast.LENGTH_SHORT).show()
                file.delete()
            }
            .setNegativeButton("Tutup") { _, _ ->
                file.delete()
            }
            .setCancelable(false)
            .show()
    }
}
