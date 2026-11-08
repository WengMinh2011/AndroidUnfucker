package dev.androidunfucker

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

/**
 * AndroidUnfucker — cautious, reversible tuning for older / low-end devices.
 * Privileged actions use Shizuku (no root). Optional extra diagnostics when root is present.
 */
class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var accessView: TextView
    private lateinit var outputView: TextView
    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var dpiInput: EditText
    private lateinit var packageInput: EditText

    private val requestCode = 4102
    private val executor = Executors.newSingleThreadExecutor()
    private var hasRoot = false

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == requestCode) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                statusView.text = "Shizuku permission granted"
                detectRootAndRefresh()
            } else {
                statusView.text = "Shizuku permission denied"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        buildUi()
        updatePermissionStatus()
        detectRootAndRefresh()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        executor.shutdownNow()
        super.onDestroy()
    }

    // ─── UI ───────────────────────────────────────────────────────────────

    private fun buildUi() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(20))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)

        // Header
        content.addView(title("AndroidUnfucker", 22))
        content.addView(subtitle("Reversible tuning • low-end friendly"))

        // Status card
        statusView = body("Checking Shizuku…", 14, true)
        accessView = body("Access: unknown", 13, false)
        content.addView(statusView)
        content.addView(accessView)
        content.addView(actionButton("Grant / reconnect Shizuku") { requestShizuku() })

        // Device
        content.addView(section("Device"))
        content.addView(
            body(
                "${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                    "${Build.HARDWARE} / ${Build.BOARD}",
                13, false
            )
        )
        content.addView(actionButton("Refresh display size & DPI") { refreshDisplay() })

        // Display
        content.addView(section("Display (logical)"))
        content.addView(hint("Logical size only — not the physical panel. Reset if UI breaks."))

        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        widthInput = numberField("Width")
        heightInput = numberField("Height")
        sizeRow.addView(widthInput, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        sizeRow.addView(heightInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(sizeRow)
        content.addView(rowButtons(
            "Apply size" to { applyResolution() },
            "Reset size" to { runShell("wm size reset") }
        ))

        dpiInput = numberField("DPI (100–800)")
        content.addView(dpiInput)
        content.addView(rowButtons(
            "Apply DPI" to { applyDpi() },
            "Reset DPI" to { runShell("wm density reset") }
        ))

        // Animation presets
        content.addView(section("Animation presets"))
        content.addView(hint("Only changes UI timing. Does not raise FPS or touch rate."))
        content.addView(actionButton("Balanced • 0.5×") { setAnimations("0.5") })
        content.addView(actionButton("Snappy • off (0×)") { setAnimations("0") })
        content.addView(actionButton("Stock • 1×") { setAnimations("1") })

        // Non-root advanced (Shizuku)
        content.addView(section("Advanced (Shizuku)"))
        content.addView(hint("These need Shizuku permission. Safe, reversible system settings."))
        content.addView(actionButton("Stay awake while charging") {
            runShell("settings put global stay_on_while_plugged_in 7")
        })
        content.addView(actionButton("Disable stay awake") {
            runShell("settings put global stay_on_while_plugged_in 0")
        })
        content.addView(actionButton("Show immersive / full-screen policy") {
            runShell("settings get global policy_control; settings get global immersive_mode_confirmations")
        })
        content.addView(actionButton("Battery + thermal snapshot") {
            runShell(
                "dumpsys battery 2>/dev/null | head -n 25; " +
                    "echo ---; " +
                    "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null; " +
                    "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null"
            )
        })
        content.addView(actionButton("Running processes (top 30)") {
            runShell("ps -A 2>/dev/null | head -n 30 || ps | head -n 30")
        })
        content.addView(actionButton("Reset ALL display overrides") {
            confirm("Reset resolution and density to defaults?") {
                runShell("wm size reset; wm density reset")
            }
        })

        // Package tools
        content.addView(section("Package tools"))
        content.addView(hint("Force-stop or clear cache for a package name (e.g. com.android.settings)."))
        packageInput = textField("package.name.here")
        content.addView(packageInput)
        content.addView(rowButtons(
            "Force-stop" to { forceStopPackage() },
            "Clear cache" to { clearPackageCache() }
        ))

        // Root extras
        content.addView(section("Root extras (optional)"))
        content.addView(hint("Shown only when su is available. Still no overclock / thermal disable."))
        content.addView(actionButton("Detect root") { detectRootAndRefresh() })
        content.addView(actionButton("Deep sysfs / thermal (root)") {
            if (!hasRoot) {
                outputView.text = "Root not detected. Grant root or ignore this section."
                return@actionButton
            }
            runRoot(
                "echo '=== platform ==='; getprop ro.board.platform; " +
                    "echo '=== cpu freqs ==='; " +
                    "for c in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do echo \$c: \$(cat \$c 2>/dev/null); done; " +
                    "echo '=== thermal zones ==='; " +
                    "for t in /sys/class/thermal/thermal_zone*/temp; do echo \$t: \$(cat \$t 2>/dev/null); done | head -n 20"
            )
        })
        content.addView(actionButton("dmesg tail (root)") {
            if (!hasRoot) {
                outputView.text = "Root not detected."
                return@actionButton
            }
            runRoot("dmesg 2>/dev/null | tail -n 40")
        })

        // Output
        content.addView(section("Output"))
        outputView = body("No commands run yet.", 12, false).apply {
            setTextIsSelectable(true)
            setBackgroundColor(0xFF161B22.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        content.addView(outputView)
        content.addView(
            hint(
                "Requires Shizuku running + permission. " +
                    "Changes only happen when you tap a button. " +
                    "If the UI becomes unusable: wm size reset && wm density reset"
            )
        )

        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    // ─── Actions ──────────────────────────────────────────────────────────

    private fun updatePermissionStatus() {
        statusView.text = when {
            !Shizuku.pingBinder() -> "Shizuku is not running — start it first"
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                "Shizuku connected • permission granted"
            else -> "Shizuku connected • permission needed"
        }
        accessView.text = if (hasRoot) "Access: Shizuku + root" else "Access: Shizuku only (or none)"
    }

    private fun requestShizuku() {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Start Shizuku first, then return here"
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updatePermissionStatus()
            detectRootAndRefresh()
        } else {
            Shizuku.requestPermission(requestCode)
        }
    }

    private fun detectRootAndRefresh() {
        executor.execute {
            val rooted = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
                p.waitFor()
                out.contains("uid=0")
            } catch (_: Exception) {
                false
            }
            hasRoot = rooted
            runOnUiThread {
                updatePermissionStatus()
                if (Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                ) {
                    refreshDisplay()
                }
            }
        }
    }

    private fun refreshDisplay() {
        runShell("wm size; wm density")
    }

    private fun applyResolution() {
        val w = widthInput.text.toString().toIntOrNull()
        val h = heightInput.text.toString().toIntOrNull()
        if (w == null || h == null || w !in 320..3000 || h !in 320..5000) {
            outputView.text = "Enter width 320–3000 and height 320–5000."
            return
        }
        confirm("Set logical resolution to ${w}x${h}?") {
            runShell("wm size ${w}x${h}")
        }
    }

    private fun applyDpi() {
        val dpi = dpiInput.text.toString().toIntOrNull()
        if (dpi == null || dpi !in 100..800) {
            outputView.text = "Enter DPI between 100 and 800."
            return
        }
        confirm("Set density to $dpi DPI?") {
            runShell("wm density $dpi")
        }
    }

    private fun setAnimations(scale: String) {
        runShell(
            "settings put global window_animation_scale $scale; " +
                "settings put global transition_animation_scale $scale; " +
                "settings put global animator_duration_scale $scale; " +
                "settings get global window_animation_scale; " +
                "settings get global transition_animation_scale; " +
                "settings get global animator_duration_scale"
        )
    }

    private fun forceStopPackage() {
        val pkg = packageInput.text.toString().trim()
        if (pkg.isEmpty() || !pkg.contains('.')) {
            outputView.text = "Enter a valid package name (e.g. com.android.chrome)."
            return
        }
        confirm("Force-stop $pkg?") {
            runShell("am force-stop $pkg")
        }
    }

    private fun clearPackageCache() {
        val pkg = packageInput.text.toString().trim()
        if (pkg.isEmpty() || !pkg.contains('.')) {
            outputView.text = "Enter a valid package name."
            return
        }
        confirm("Clear cache for $pkg? (data is kept)") {
            runShell("pm clear --cache-only $pkg 2>/dev/null || pm trim-caches 999G")
        }
    }

    // ─── Shell execution ──────────────────────────────────────────────────

    private fun runShell(command: String) {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Shizuku is not running"
            outputView.text = "Open Shizuku, start its service, then reconnect."
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            requestShizuku()
            return
        }
        outputView.text = "Running…"
        executor.execute {
            val result = executeViaShizuku(command)
            runOnUiThread {
                outputView.text = result
                updatePermissionStatus()
            }
        }
    }

    private fun runRoot(command: String) {
        outputView.text = "Running as root…"
        executor.execute {
            val result = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val stdout = BufferedReader(InputStreamReader(p.inputStream)).readText()
                val stderr = BufferedReader(InputStreamReader(p.errorStream)).readText()
                val code = p.waitFor()
                buildString {
                    if (stdout.isNotBlank()) append(stdout.trim()).append('\n')
                    if (stderr.isNotBlank()) append(stderr.trim()).append('\n')
                    append("Exit code: $code")
                }.ifBlank { "Command completed (no output). Exit code: $code" }
            } catch (e: Exception) {
                "Root command failed: ${e.message}"
            }
            runOnUiThread { outputView.text = result }
        }
    }

    /**
     * Prefer Shizuku's process helper via reflection (API 13.x keeps
     * newProcess private). Falls back to a clear error instead of crashing.
     */
    private fun executeViaShizuku(command: String): String {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val code = process.waitFor()
            buildString {
                if (stdout.isNotBlank()) append(stdout.trim()).append('\n')
                if (stderr.isNotBlank()) append(stderr.trim()).append('\n')
                append("Exit code: $code")
            }.ifBlank { "Command completed (no output). Exit code: $code" }
        } catch (e: Exception) {
            "Shizuku command failed: ${e.message}\n" +
                "Update Shizuku or reinstall the app if this persists."
        }
    }

    // ─── Small UI helpers ─────────────────────────────────────────────────

    private fun confirm(message: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun title(text: String, sizeSp: Int) = TextView(this).apply {
        this.text = text
        textSize = sizeSp.toFloat()
        setTextColor(0xFFE6EDF3.toInt())
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(2))
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF8B949E.toInt())
        setPadding(0, 0, 0, dp(10))
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text.uppercase()
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(0xFF58A6FF.toInt())
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun body(text: String, sizeSp: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = sizeSp.toFloat()
        setTextColor(0xFFC9D1D9.toInt())
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun hint(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(0xFF8B949E.toInt())
        setPadding(0, dp(2), 0, dp(8))
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        minHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun rowButtons(left: Pair<String, () -> Unit>, right: Pair<String, () -> Unit>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(actionButton(left.first, left.second), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            })
            addView(actionButton(right.first, right.second), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun numberField(hintText: String) = EditText(this).apply {
        hint = hintText
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        setSingleLine(true)
        textSize = 15f
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF6E7681.toInt())
        setBackgroundColor(0xFF21262D.toInt())
        setPadding(dp(12), dp(10), dp(12), dp(10))
        minHeight = dp(48)
    }

    private fun textField(hintText: String) = EditText(this).apply {
        hint = hintText
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        setSingleLine(true)
        textSize = 14f
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF6E7681.toInt())
        setBackgroundColor(0xFF21262D.toInt())
        setPadding(dp(12), dp(10), dp(12), dp(10))
        minHeight = dp(48)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
