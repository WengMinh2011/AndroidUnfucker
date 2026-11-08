package dev.androidunfucker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

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
            } else statusView.text = "Shizuku permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        buildUi(); updatePermissionStatus(); detectRootAndRefresh()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        executor.shutdownNow(); super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(20))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(title("AndroidUnfucker", 22))
        content.addView(subtitle("Reversible tuning \u2022 low-end friendly"))
        statusView = body("Checking Shizuku\u2026", 14, true)
        accessView = body("Access: unknown", 13, false)
        content.addView(statusView); content.addView(accessView)
        content.addView(actionButton("Grant / reconnect Shizuku") { requestShizuku() })

        content.addView(section("Device"))
        content.addView(body("${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n${Build.HARDWARE} / ${Build.BOARD}", 13, false))
        content.addView(actionButton("Refresh display size & DPI") { refreshDisplay() })

        content.addView(section("Profiles"))
        content.addView(actionButton("Gaming") { confirm("Apply Gaming?") { applyProfile("0", "7", "1") } })
        content.addView(actionButton("Balanced") { confirm("Apply Balanced?") { applyProfile("0.5", "0", "0") } })
        content.addView(actionButton("Power saving") { confirm("Apply Power saving?") { applyProfile("0.25", "0", "0") } })

        content.addView(section("Game render scale"))
        content.addView(hint("Lowers logical resolution so many games draw fewer pixels. Not an in-game slider. Short edge stays at least 480px."))
        content.addView(actionButton("Read current size") { refreshDisplay() })
        content.addView(rowButtons("Scale 75%" to { applyRenderScale(75) }, "Scale 50%" to { applyRenderScale(50) }))
        content.addView(rowButtons("Scale 85%" to { applyRenderScale(85) }, "Native size" to {
            confirm("Reset to physical resolution?") { runShell("wm size reset; wm size") }
        }))

        content.addView(section("CPU performance levels"))
        content.addView(hint("Governor only \u2014 not overclock. Root usually required."))
        content.addView(actionButton("Read CPU status") { readCpuStatus() })
        content.addView(actionButton("CPU Low") { setCpuLevel("powersave") })
        content.addView(actionButton("CPU Balanced") { setCpuLevel("schedutil interactive ondemand") })
        content.addView(actionButton("CPU High") { setCpuLevel("performance") })

        content.addView(section("GPU performance levels"))
        content.addView(hint("Adreno/Mali governors when present. Root usually required."))
        content.addView(actionButton("Read GPU status") { readGpuStatus() })
        content.addView(actionButton("GPU Low") { setGpuLevel("powersave") })
        content.addView(actionButton("GPU Balanced") { setGpuLevel("msm-adreno-tz simple_ondemand ondemand") })
        content.addView(actionButton("GPU High") { setGpuLevel("performance") })

        content.addView(section("Display"))
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        widthInput = numberField("Width"); heightInput = numberField("Height")
        sizeRow.addView(widthInput, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        sizeRow.addView(heightInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(sizeRow)
        content.addView(rowButtons("Apply size" to { applyResolution() }, "Reset size" to { runShell("wm size reset") }))
        dpiInput = numberField("DPI")
        content.addView(dpiInput)
        content.addView(rowButtons("Apply DPI" to { applyDpi() }, "Reset DPI" to { runShell("wm density reset") }))

        content.addView(section("Animations"))
        content.addView(actionButton("Off 0x") { setAnimations("0") })
        content.addView(actionButton("Smooth 0.25x") { setAnimations("0.25") })
        content.addView(actionButton("Balanced 0.5x") { setAnimations("0.5") })
        content.addView(actionButton("Stock 1x") { setAnimations("1") })

        content.addView(section("Graphics flags"))
        content.addView(actionButton("Force GPU ON") { runShell("settings put global force_gpu_rendering 1") })
        content.addView(actionButton("Force GPU OFF") { runShell("settings put global force_gpu_rendering 0") })
        content.addView(actionButton("HW overlays OFF") { runShell("settings put global disable_hw_overlays 1") })
        content.addView(actionButton("HW overlays ON") { runShell("settings put global disable_hw_overlays 0") })

        content.addView(section("Renderer and refresh"))
        content.addView(hint("UI renderer hint only (OpenGL vs Vulkan). Games keep their own API. Refresh request is ignored if the panel cannot do it."))
        content.addView(rowButtons("UI OpenGL" to { setHwuiRenderer("skiagl") }, "UI Vulkan" to { setHwuiRenderer("skiavk") }))
        content.addView(actionButton("Clear renderer hint") { setHwuiRenderer(null) })
        content.addView(rowButtons("Request 60 Hz" to { requestRefresh(60) }, "Request 90 Hz" to { requestRefresh(90) }))
        content.addView(rowButtons("Request 120 Hz" to { requestRefresh(120) }, "Refresh default" to {
            confirm("Clear min/peak refresh overrides?") {
                runShell("settings delete system peak_refresh_rate; settings delete system min_refresh_rate; echo cleared")
            }
        }))
        content.addView(rowButtons("Battery saver ON" to {
            confirm("Turn on system battery saver?") { runShell("settings put global low_power 1; echo low_power=$(settings get global low_power)") }
        }, "Battery saver OFF" to {
            runShell("settings put global low_power 0; echo low_power=$(settings get global low_power)")
        }))

        content.addView(section("Menus"))
        content.addView(actionButton("Developer options") { openDeveloperOptions() })
        content.addView(actionButton("Settings") { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e: Exception) { outputView.text = "${e.message}" } })

        content.addView(section("Thermal (read-only)"))
        content.addView(hint("Throttle disable is not supported."))
        content.addView(actionButton("Battery + thermal snapshot") {
            runShell("dumpsys battery 2>/dev/null | head -n 20; cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null")
        })

        content.addView(section("Apps"))
        packageInput = textField("package.name")
        content.addView(packageInput)
        content.addView(rowButtons("Force-stop" to { pkgAction("am force-stop") }, "Freeze" to { pkgAction("pm disable-user --user 0") }))
        content.addView(rowButtons("Unfreeze" to { pkgAction("pm enable") }, "Clear cache" to {
            val p = packageInput.text.toString().trim()
            if (validPkg(p)) confirm("Clear cache $p?") { runShell("pm clear --cache-only $p 2>/dev/null || true") }
        }))

        content.addView(section("Advanced"))
        content.addView(actionButton("Stay awake on charge") { runShell("settings put global stay_on_while_plugged_in 7") })
        content.addView(actionButton("Stay awake off") { runShell("settings put global stay_on_while_plugged_in 0") })
        content.addView(actionButton("Reset display overrides") { confirm("Reset size and DPI?") { runShell("wm size reset; wm density reset") } })
        content.addView(actionButton("Detect root") { detectRootAndRefresh() })

        content.addView(section("Output"))
        outputView = body("No commands yet.", 12, false).apply {
            setTextIsSelectable(true); setBackgroundColor(0xFF161B22.toInt()); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        content.addView(outputView)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun applyProfile(anim: String, stay: String, gpu: String) {
        runShell("settings put global window_animation_scale $anim; settings put global transition_animation_scale $anim; settings put global animator_duration_scale $anim; settings put global stay_on_while_plugged_in $stay; settings put global force_gpu_rendering $gpu; echo done")
    }

    private fun applyRenderScale(pct: Int) {
        confirm(
            "Render scale $pct% of physical size?\n" +
                "Many games then draw fewer pixels. Reset with Native size if the UI breaks."
        ) {
            val d = "$"
            val script =
                "PCT=$pct; LINE=" + d + "(wm size | grep 'Physical size' | head -n 1); " +
                    "W=" + d + "(echo \"" + d + "LINE\" | sed 's/.*: //; s/x/ /' | cut -d' ' -f1); " +
                    "H=" + d + "(echo \"" + d + "LINE\" | sed 's/.*: //; s/x/ /' | cut -d' ' -f2); " +
                    "if [ -z \"" + d + "W\" ] || [ -z \"" + d + "H\" ]; then echo 'No physical size'; wm size; exit 1; fi; " +
                    "NW=" + d + "(( (W * PCT / 100) / 2 * 2 )); " +
                    "NH=" + d + "(( (H * PCT / 100) / 2 * 2 )); " +
                    "SHORT=" + d + "NW; if [ " + d + "NH -lt " + d + "NW ]; then SHORT=" + d + "NH; fi; " +
                    "if [ " + d + "SHORT -lt 480 ]; then echo 'Refused: short edge under 480'; exit 1; fi; " +
                    "wm size " + d + "NW" + "x" + d + "NH; echo applied " + d + "NW x " + d + "NH; wm size"
            runShell(script)
        }
    }

    private fun setHwuiRenderer(mode: String?) {
        val label = mode ?: "default"
        confirm(
            "Set system UI renderer hint to $label?\n" +
                "Sets debug.hwui.renderer. Does not force a game engine to Vulkan or OpenGL. Root is often required."
        ) {
            val cmd = if (mode == null) {
                "setprop debug.hwui.renderer ''; getprop debug.hwui.renderer; echo cleared"
            } else {
                "setprop debug.hwui.renderer $mode; getprop debug.hwui.renderer"
            }
            if (hasRoot) runRoot(cmd) else runShell(cmd)
        }
    }

    private fun requestRefresh(hz: Int) {
        confirm("Request $hz Hz?\nThe panel ignores rates it does not support. This is not an overclock.") {
            runShell(
                "settings put system peak_refresh_rate $hz; settings put system min_refresh_rate $hz; " +
                    "echo peak=$(settings get system peak_refresh_rate); echo min=$(settings get system min_refresh_rate)"
            )
        }
    }

    private fun readCpuStatus() {
        val d = "$"
        val cmd = "for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo " + d + "g: " + d + "(cat " + d + "g 2>/dev/null); done; cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null; for c in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do echo " + d + "c: " + d + "(cat " + d + "c 2>/dev/null); done"
        if (hasRoot) runRoot(cmd) else runShell(cmd)
    }

    private fun setCpuLevel(prefs: String) {
        confirm("Set CPU governors from: $prefs ?\nNot overclock. Root usually required.") {
            val d = "$"
            val script = "PREFS=\"$prefs\"; AVAIL=" + d + "(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null); CHOSEN=; for g in " + d + "PREFS; do echo " + d + "AVAIL | grep -qw " + d + "g && CHOSEN=" + d + "g && break; done; if [ -z \"" + d + "CHOSEN\" ]; then echo no match; exit 1; fi; for path in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo " + d + "CHOSEN > \"" + d + "path\" 2>/dev/null && echo OK || echo FAIL; done"
            if (hasRoot) runRoot(script) else runShell(script)
        }
    }

    private fun readGpuStatus() {
        val d = "$"
        val cmd = "for p in /sys/class/kgsl/kgsl-3d0/devfreq/governor /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq /sys/class/kgsl/kgsl-3d0/devfreq/available_governors; do [ -e " + d + "p ] && echo " + d + "p: " + d + "(cat " + d + "p 2>/dev/null); done; for p in /sys/class/devfreq/*mali*/governor; do for f in " + d + "p; do [ -e " + d + "f ] && echo " + d + "f: " + d + "(cat " + d + "f 2>/dev/null); done; done"
        if (hasRoot) runRoot(cmd) else runShell(cmd)
    }

    private fun setGpuLevel(prefs: String) {
        confirm("Set GPU governors from: $prefs ?\nRoot usually required.") {
            val d = "$"
            val script = "PREFS=\"$prefs\"; for node in /sys/class/kgsl/kgsl-3d0/devfreq/governor /sys/class/devfreq/*mali*/governor /sys/class/devfreq/*gpu*/governor; do for n in " + d + "node; do [ -e " + d + "n ] || continue; AVAIL=" + d + "(cat " + d + "(dirname " + d + "n)/available_governors 2>/dev/null); CHOSEN=; for g in " + d + "PREFS; do echo " + d + "AVAIL | grep -qw " + d + "g && CHOSEN=" + d + "g && break; done; if [ -n \"" + d + "CHOSEN\" ] && [ -w \"" + d + "n\" ]; then echo " + d + "CHOSEN > \"" + d + "n\" && echo OK " + d + "n; else echo SKIP " + d + "n; fi; done; done"
            if (hasRoot) runRoot(script) else runShell(script)
        }
    }

    private fun updatePermissionStatus() {
        statusView.text = when {
            !Shizuku.pingBinder() -> "Shizuku is not running"
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Shizuku connected \u2022 permission granted"
            else -> "Shizuku connected \u2022 permission needed"
        }
        accessView.text = if (hasRoot) "Access: Shizuku + root" else "Access: Shizuku only (or none)"
    }

    private fun requestShizuku() {
        if (!Shizuku.pingBinder()) { statusView.text = "Start Shizuku first"; return }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { updatePermissionStatus(); detectRootAndRefresh() }
        else Shizuku.requestPermission(requestCode)
    }

    private fun detectRootAndRefresh() {
        executor.execute {
            val rooted = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val out = BufferedReader(InputStreamReader(p.inputStream)).readText(); p.waitFor(); out.contains("uid=0")
            } catch (_: Exception) { false }
            hasRoot = rooted
            runOnUiThread {
                updatePermissionStatus()
                if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) refreshDisplay()
            }
        }
    }

    private fun refreshDisplay() = runShell("wm size; wm density")
    private fun applyResolution() {
        val w = widthInput.text.toString().toIntOrNull(); val h = heightInput.text.toString().toIntOrNull()
        if (w == null || h == null || w !in 320..3000 || h !in 320..5000) { outputView.text = "Invalid size"; return }
        confirm("Set ${w}x${h}?") { runShell("wm size ${w}x${h}") }
    }
    private fun applyDpi() {
        val dpi = dpiInput.text.toString().toIntOrNull()
        if (dpi == null || dpi !in 100..800) { outputView.text = "Invalid DPI"; return }
        confirm("Set DPI $dpi?") { runShell("wm density $dpi") }
    }
    private fun setAnimations(scale: String) {
        runShell("settings put global window_animation_scale $scale; settings put global transition_animation_scale $scale; settings put global animator_duration_scale $scale")
    }
    private fun pkgAction(cmd: String) {
        val p = packageInput.text.toString().trim()
        if (!validPkg(p)) return
        confirm("$cmd $p?") { runShell("$cmd $p") }
    }
    private fun validPkg(p: String): Boolean {
        if (p.isEmpty() || !p.contains('.')) { outputView.text = "Enter package name"; return false }; return true
    }
    private fun openDeveloperOptions() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        catch (_: Exception) { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e: Exception) { outputView.text = "${e.message}" } }
    }

    private fun runShell(command: String) {
        if (!Shizuku.pingBinder()) { statusView.text = "Shizuku is not running"; return }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) { requestShizuku(); return }
        outputView.text = "Running\u2026"
        executor.execute {
            val result = executeViaShizuku(command)
            runOnUiThread { outputView.text = result; updatePermissionStatus() }
        }
    }
    private fun runRoot(command: String) {
        outputView.text = "Running as root\u2026"
        executor.execute {
            val result = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
                val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
                val code = p.waitFor()
                (out + err + "\nExit $code").trim()
            } catch (e: Exception) { "Root failed: ${e.message}" }
            runOnUiThread { outputView.text = result }
        }
    }
    private fun executeViaShizuku(command: String): String {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val out = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val code = process.waitFor()
            (out + err + "\nExit $code").trim().ifBlank { "Done exit $code" }
        } catch (e: Exception) { "Shizuku failed: ${e.message}" }
    }

    private fun confirm(msg: String, yes: () -> Unit) {
        AlertDialog.Builder(this).setTitle("Confirm").setMessage(msg).setPositiveButton("Yes") { _, _ -> yes() }.setNegativeButton("Cancel", null).show()
    }
    private fun title(t: String, s: Int) = TextView(this).apply { text = t; textSize = s.toFloat(); setTextColor(0xFFE6EDF3.toInt()); typeface = Typeface.DEFAULT_BOLD }
    private fun subtitle(t: String) = TextView(this).apply { text = t; textSize = 13f; setTextColor(0xFF8B949E.toInt()) }
    private fun section(t: String) = TextView(this).apply { text = t.uppercase(); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFF58A6FF.toInt()); setPadding(0, dp(18), 0, dp(6)) }
    private fun body(t: String, s: Int, bold: Boolean) = TextView(this).apply { text = t; textSize = s.toFloat(); setTextColor(0xFFC9D1D9.toInt()); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun hint(t: String) = TextView(this).apply { text = t; textSize = 12f; setTextColor(0xFF8B949E.toInt()) }
    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply { text = label; isAllCaps = false; minHeight = dp(48); setOnClickListener { action() } }
    private fun rowButtons(a: Pair<String, () -> Unit>, b: Pair<String, () -> Unit>) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(actionButton(a.first, a.second), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        addView(actionButton(b.first, b.second), LinearLayout.LayoutParams(0, -2, 1f))
    }
    private fun numberField(h: String) = EditText(this).apply {
        hint = h; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true)
        setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF6E7681.toInt()); setBackgroundColor(0xFF21262D.toInt()); minHeight = dp(48)
    }
    private fun textField(h: String) = EditText(this).apply {
        hint = h; setSingleLine(true); setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF6E7681.toInt()); setBackgroundColor(0xFF21262D.toInt()); minHeight = dp(48)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
