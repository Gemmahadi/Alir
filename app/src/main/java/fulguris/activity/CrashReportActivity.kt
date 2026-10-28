package fulguris.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import dagger.hilt.android.AndroidEntryPoint
import fulguris.utils.CrashReporter
import javax.inject.Inject

/**
 * Alir diagnostic: shows a saved crash report in a minimal activity.
 *
 * v1.0.5 showed the report in a dialog at the end of the browser's onCreate,
 * but the app crashed again during startup before the user could tap Salin —
 * the dialog died with the activity. This activity has no WebView, no tabs
 * and no session restore, so it cannot hit the browser startup crash: the
 * user can calmly read and copy the report, then continue to the browser.
 */
@AndroidEntryPoint
class CrashReportActivity @Inject constructor() : LocaleAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val report = CrashReporter.pendingReport(this)
        if (report == null) {
            proceed()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Alir sempat crash"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Ini penyebab crash terakhir. Salin lalu kirim ke developer."
            textSize = 14f
            setPadding(0, 16, 0, 24)
        }
        val reportView = TextView(this).apply {
            text = report
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(
                reportView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val copyButton = Button(this).apply {
            text = "Salin"
            setOnClickListener {
                val clipboard =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Alir crash report", report)
                )
                Toast.makeText(this@CrashReportActivity, "Laporan crash disalin", Toast.LENGTH_SHORT).show()
                proceed()
            }
        }
        val continueButton = Button(this).apply {
            text = "Lanjut"
            setOnClickListener { proceed() }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                copyButton,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                continueButton,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll)
        root.addView(buttons)
        setContentView(root)

        // Back means "skip the report and continue", never "stuck on this screen".
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    proceed()
                }
            }
        )
    }

    // Required by LocaleAwareActivity; the report screen needs no locale handling.
    override fun onLocaleChanged() {
    }

    private fun proceed() {
        CrashReporter.clearReport(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
