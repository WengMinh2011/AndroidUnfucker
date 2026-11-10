package dev.androidunfucker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
    private var richUi = false

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

    private fun prefs() = getSharedPreferences("ui", MODE_PRIVATE)

    private fun buildUi() {
        richUi = prefs().getBoolean("rich", false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (richUi) 0xFF070B10.toInt() else 0xFF0D1117.toInt())
            setPadding(dp(if (richUi) 14 else 16), dp(12), dp(if (richUi) 14 else 16), dp(20))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(title("AndroidUnfucker", if (richUi) 26 else 22))
        content.addView(subtitle(if (richUi) "Good UI  -  cards, color, same tools" else "Lite UI  -  flat, big targets, low-end"))
        statusView = body("Checking Shizuku...", 14, true)
        accessView = body("Access: unknown", 13, false)
        content.gap(statusView)
        content.gap(accessView)
        content.gap(actionButton(if (richUi) "Switch to Lite UI" else "Switch to Good UI") {
            prefs().edit().putBoolean("rich", !richUi).apply()
            buildUi()
            updatePermissionStatus()
        })
        content.gap(actionButton("Grant / reconnect Shizuku") { requestShizuku() })

        card(content, "Device") {
            gap(body("${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n${Build.HARDWARE} / ${Build.BOARD}", 13, false))
            gap(actionButton("Refresh display size and DPI") { refreshDisplay() })
        }
        card(content, "Profiles") {
            gap(hint("Gaming only turns animations off. Force GPU is not used — it slows many phones."))
            gap(actionButton("Gaming") {
                confirm("Apply Gaming?\nTurns animations off for snappier UI.\nDoes NOT force GPU rendering (that often makes low-end phones lag).") {
                    applyProfile("0", "7", "0")
                }
            })
            gap(actionButton("Balanced") { confirm("Apply Balanced?") { applyProfile("0.5", "0", "0") } })
            gap(actionButton("Power saving") { confirm("Apply Power saving?") { applyProfile("0.25", "0", "0") } })
            gap(actionButton("Undo lag (safe defaults)") {
                confirm("Turn off Force GPU, re-enable HW overlays, reset animations to 0.5x, and restore native resolution?") {
                    runShell("settings put global force_gpu_rendering 0; settings put global disable_hw_overlays 0; settings put global window_animation_scale 0.5; settings put global transition_animation_scale 0.5; settings put global animator_duration_scale 0.5; settings put global low_power 0; wm size reset; wm density reset; echo lag_undo_done")
                }
            })
        }
        card(content, "Game render scale") {
            gap(hint("Lowers logical resolution for games. Keeps panel aspect ratio so the notch stays centered. Short edge stays at least 480px."))
            gap(actionButton("Read current size") { refreshDisplay() })
            gap(rowButtons("Scale 75%" to { applyRenderScale(75) }, "Scale 50%" to { applyRenderScale(50) }))
            gap(rowButtons("Scale 85%" to { applyRenderScale(85) }, "Native size" to {
                confirm("Reset to physical resolution and density?") { runShell("wm size reset; wm density reset; wm size; wm density") }
            }))
        }
        card(content, "CPU performance levels") {
            gap(hint("Governor only. Not an overclock. Root is usually required."))
            gap(actionButton("Read CPU status") { readCpuStatus() })
            gap(actionButton("CPU Low") { setCpuLevel("powersave") })
            gap(actionButton("CPU Balanced") { setCpuLevel("schedutil interactive ondemand") })
            gap(actionButton("CPU High") { setCpuLevel("performance") })
        }
        card(content, "GPU performance levels") {
            gap(hint("Adreno or Mali governors when the kernel exposes them. Root is usually required."))
            gap(actionButton("Read GPU status") { readGpuStatus() })
            gap(actionButton("GPU Low") { setGpuLevel("powersave") })
            gap(actionButton("GPU Balanced") { setGpuLevel("msm-adreno-tz simple_ondemand ondemand") })
            gap(actionButton("GPU High") { setGpuLevel("performance") })
        }
        card(content, "Display") {
            val sizeRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            widthInput = numberField("Width"); heightInput = numberField("Height")
            sizeRow.addView(widthInput, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
            sizeRow.addView(heightInput, LinearLayout.LayoutParams(0, dp(48), 1f))
            gap(sizeRow)
            gap(rowButtons("Apply size" to { applyResolution() }, "Reset size" to { runShell("wm size reset") }))
            dpiInput = numberField("DPI")
            gap(dpiInput)
            gap(rowButtons("Apply DPI" to { applyDpi() }, "Reset DPI" to { runShell("wm density reset") }))
        }
        card(content, "Animations") {
            gap(actionButton("Off 0x") { setAnimations("0") })
            gap(actionButton("Smooth 0.25x") { setAnimations("0.25") })
            gap(actionButton("Balanced 0.5x") { setAnimations("0.5") })
            gap(actionButton("Stock 1x") { setAnimations("1") })
        }
        card(content, "Graphics flags") {
            gap(actionButton("Force GPU ON") {
                confirm("Force GPU rendering?\nOn many low-end phones this makes the UI SLOWER, not faster. Prefer leaving it OFF.") {
                    runShell("settings put global force_gpu_rendering 1; echo force_gpu=1")
                }
            })
            gap(actionButton("Force GPU OFF") { runShell("settings put global force_gpu_rendering 0") })
            gap(actionButton("HW overlays OFF") {
                confirm("Disable HW overlays?\nCan help debugging but often hurts performance. Prefer ON.") {
                    runShell("settings put global disable_hw_overlays 1; echo overlays_disabled=1")
                }
            })
            gap(actionButton("HW overlays ON") { runShell("settings put global disable_hw_overlays 0") })
        }
        card(content, "Renderer and refresh") {
            gap(hint("UI renderer hint only. Games keep their own API. Unsupported refresh rates are ignored."))
            gap(rowButtons("UI OpenGL" to { setHwuiRenderer("skiagl") }, "UI Vulkan" to { setHwuiRenderer("skiavk") }))
            gap(actionButton("Clear renderer hint") { setHwuiRenderer(null) })
            gap(rowButtons("Request 60 Hz" to { requestRefresh(60) }, "Request 90 Hz" to { requestRefresh(90) }))
            gap(rowButtons("Request 120 Hz" to { requestRefresh(120) }, "Refresh default" to {
                confirm("Clear min/peak refresh overrides?") {
                    runShell("settings delete system peak_refresh_rate; settings delete system min_refresh_rate; echo cleared")
                }
            }))
            gap(rowButtons("Battery saver ON" to {
                confirm("Turn on system battery saver?") { runShell("settings put global low_power 1; echo low_power=$(settings get global low_power)") }
            }, "Battery saver OFF" to {
                runShell("settings put global low_power 0; echo low_power=$(settings get global low_power)")
            }))
        }
        card(content, "Menus") {
            gap(actionButton("Developer options") { openDeveloperOptions() })
            gap(actionButton("Settings") { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e: Exception) { outputView.text = e.message ?: "failed" } })
        }
        card(content, "Thermal (read-only)") {
            gap(hint("Throttle disable is not supported."))
            gap(actionButton("Battery + thermal snapshot") {
                runShell("dumpsys battery 2>/dev/null | head -n 20; cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null")
            })
        }
        card(content, "Apps") {
            packageInput = textField("package.name")
            gap(packageInput)
            gap(rowButtons("Force-stop" to { pkgAction("am force-stop") }, "Freeze" to { pkgAction("pm disable-user --user 0") }))
            gap(rowButtons("Unfreeze" to { pkgAction("pm enable") }, "Clear cache" to {
                val p = packageInput.text.toString().trim()
                if (validPkg(p)) confirm("Clear cache $p?") { runShell("pm clear --cache-only $p 2>/dev/null || true") }
            }))
        }
        card(content, "Extra tools") {
            gap(hint("Normal Android settings. Every tap toasts and prints in the bottom panel."))
            gap(actionButton("Read what is applied") {
                runShell("echo '--- display ---'; wm size; wm density; echo '--- anim ---'; settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale; echo '--- flags ---'; echo low_power=$(settings get global low_power); echo gpu=$(settings get global force_gpu_rendering); echo overlays=$(settings get global disable_hw_overlays); echo peak=$(settings get system peak_refresh_rate); echo min=$(settings get system min_refresh_rate); echo font=$(settings get system font_scale); echo touches=$(settings get system show_touches); echo pointer=$(settings get system pointer_location); echo timeout=$(settings get system screen_off_timeout); echo rotate=$(settings get system accelerometer_rotation); echo freezer=$(settings get global cached_apps_freezer)")
            })
            gap(rowButtons("Show touches ON" to { runShell("settings put system show_touches 1; echo show_touches=1") }, "Show touches OFF" to { runShell("settings put system show_touches 0; echo show_touches=0") }))
            gap(rowButtons("Pointer loc ON" to { runShell("settings put system pointer_location 1; echo pointer=1") }, "Pointer loc OFF" to { runShell("settings put system pointer_location 0; echo pointer=0") }))
            gap(rowButtons("Timeout 30s" to { runShell("settings put system screen_off_timeout 30000; echo timeout=30s") }, "Timeout 10m" to { runShell("settings put system screen_off_timeout 600000; echo timeout=10m") }))
            gap(rowButtons("Font 0.85" to { confirm("Set font scale to 0.85?") { runShell("settings put system font_scale 0.85; echo font=0.85") } }, "Font 1.0" to { runShell("settings put system font_scale 1.0; echo font=1") }))
            gap(rowButtons("Night mode ON" to { runShell("cmd uimode night yes; echo night") }, "Night mode OFF" to { runShell("cmd uimode night no; echo day") }))
            gap(rowButtons("Auto rotate" to { runShell("settings put system accelerometer_rotation 1; echo rotate=auto") }, "Lock portrait" to { runShell("settings put system accelerometer_rotation 0; echo rotate=locked") }))
            gap(rowButtons("Auto brightness" to { runShell("settings put system screen_brightness_mode 1; echo brightness=auto") }, "Manual brightness" to { runShell("settings put system screen_brightness_mode 0; echo brightness=manual") }))
            gap(rowButtons("App freezer ON" to { runShell("settings put global cached_apps_freezer enabled; echo freezer=enabled") }, "App freezer OFF" to { runShell("settings put global cached_apps_freezer disabled; echo freezer=disabled") }))
            gap(actionButton("Foreground app") { runShell("dumpsys activity activities | grep -m 1 mResumedActivity; dumpsys window | grep -m 1 mCurrentFocus") })
            gap(actionButton("Frame stats for package") {
                val pkg = packageInput.text.toString().trim()
                if (validPkg(pkg)) runShell("dumpsys gfxinfo " + pkg + " | head -n 30")
            })
            gap(actionButton("Reset common tweaks") {
                confirm("Reset size, DPI, animations, refresh, touches, font, and battery saver?") {
                    runShell("wm size reset; wm density reset; settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1; settings put global force_gpu_rendering 0; settings put global disable_hw_overlays 0; settings put global low_power 0; settings put global stay_on_while_plugged_in 0; settings delete system peak_refresh_rate; settings delete system min_refresh_rate; settings put system show_touches 0; settings put system pointer_location 0; settings put system font_scale 1.0; echo reset-done")
                }
            })
        }
        card(content, "Advanced") {
            gap(actionButton("Stay awake on charge") { runShell("settings put global stay_on_while_plugged_in 7") })
            gap(actionButton("Stay awake off") { runShell("settings put global stay_on_while_plugged_in 0") })
            gap(actionButton("Reset display overrides") { confirm("Reset size and DPI?") { runShell("wm size reset; wm density reset") } })
            gap(actionButton("Detect root") { detectRootAndRefresh() })
        }
        outputView = body("Tap a button. The result stays in this panel.", 12, false).apply {
            setTextIsSelectable(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val outputBox = ScrollView(this).apply {
            background = rounded(0xFF111820.toInt(), 10)
            addView(outputView)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(outputBox, LinearLayout.LayoutParams(-1, dp(150)).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun applyProfile(anim: String, stay: String, gpu: String) {
        runShell("settings put global window_animation_scale $anim; settings put global transition_animation_scale $anim; settings put global animator_duration_scale $anim; settings put global stay_on_while_plugged_in $stay; settings put global force_gpu_rendering $gpu; echo done")
    }

    private fun applyRenderScale(pct: Int) {
        confirm(
            "Render scale $pct% of physical size?\n" +
                "Keeps the same aspect ratio so the notch stays centered.\n" +
                "Also scales DPI. Reset with Native size if the UI breaks."
        ) {
            val d = "$"
            // Width scaled + height locked to physical aspect ratio (avoids off-center notch).
            // Density scaled the same percent so system UI maps correctly on OPPO/ColorOS.
            val script =
                "PCT=$pct; " +
                    "LINE=" + d + "(wm size | grep 'Physical size' | head -n 1); " +
                    "W=" + d + "(echo \"" + d + "LINE\" | sed 's/.*: //; s/x/ /' | cut -d' ' -f1); " +
                    "H=" + d + "(echo \"" + d + "LINE\" | sed 's/.*: //; s/x/ /' | cut -d' ' -f2); " +
                    "if [ -z \"" + d + "W\" ] || [ -z \"" + d + "H\" ]; then echo 'No physical size'; wm size; exit 1; fi; " +
                    "NW=" + d + "(( (W * PCT / 100) / 8 * 8 )); " +
                    "if [ " + d + "NW -lt 480 ]; then echo 'Refused: width under 480'; exit 1; fi; " +
                    "NH=" + d + "(( NW * H / W )); " +
                    "NH=" + d + "(( NH / 8 * 8 )); " +
                    "if [ " + d + "NH -lt 480 ]; then echo 'Refused: height under 480'; exit 1; fi; " +
                    "DLINE=" + d + "(wm density | grep 'Physical density' | head -n 1); " +
                    "D=" + d + "(echo \"" + d + "DLINE\" | sed 's/.*: //'); " +
                    "if [ -z \"" + d + "D\" ]; then D=320; fi; " +
                    "ND=" + d + "(( D * PCT / 100 )); " +
                    "if [ " + d + "ND -lt 120 ]; then ND=120; fi; " +
                    "if [ " + d + "ND -gt 640 ]; then ND=640; fi; " +
                    "wm size " + d + "{NW}x" + d + "{NH}; " +
                    "wm density " + d + "ND; " +
                    "echo applied " + d + "{NW}x" + d + "{NH} density=" + d + "ND; " +
                    "wm size; wm density"
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
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Shizuku connected - permission granted"
            else -> "Shizuku connected - permission needed"
        }
        accessView.text = if (hasRoot) "Access: Shizuku + root" else "Access: Shizuku only (or none)"
    }

    private fun requestShizuku() {
        if (!Shizuku.pingBinder()) { note("Start Shizuku first, then tap Grant again."); return }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            note("Shizuku already granted")
            updatePermissionStatus(); detectRootAndRefresh()
        } else {
            note("Allow the Shizuku popup")
            Shizuku.requestPermission(requestCode)
        }
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
        if (w == null || h == null || w !in 320..3000 || h !in 320..5000) { note("Invalid size. Width 320-3000, height 320-5000."); return }
        confirm("Set ${w}x${h}?") { runShell("wm size ${w}x${h}") }
    }
    private fun applyDpi() {
        val dpi = dpiInput.text.toString().toIntOrNull()
        if (dpi == null || dpi !in 100..800) { note("Invalid DPI. Use 100-800."); return }
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
        if (p.isEmpty() || !p.contains('.')) { note("Enter a package name in the box first"); return false }; return true
    }
    private fun openDeveloperOptions() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        catch (_: Exception) { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e: Exception) { outputView.text = "${e.message}" } }
    }

    private fun note(msg: String, toast: Boolean = true) {
        outputView.text = msg
        if (toast) {
            val line = msg.lineSequence().firstOrNull().orEmpty().take(90)
            Toast.makeText(this, line.ifBlank { "done" }, Toast.LENGTH_SHORT).show()
        }
    }

    private fun runShell(command: String) {
        if (!Shizuku.pingBinder()) { note("Shizuku is not running. Open Shizuku and start it, then tap Grant."); return }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            note("Shizuku permission needed. Allow the popup.")
            requestShizuku(); return
        }
        note("Running...", toast = false)
        executor.execute {
            val result = executeViaShizuku(command)
            runOnUiThread { note(result.ifBlank { "Done, no output" }); updatePermissionStatus() }
        }
    }
    private fun runRoot(command: String) {
        note("Running as root...", toast = false)
        executor.execute {
            val result = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
                val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
                val code = p.waitFor()
                (out + err + "\nExit $code").trim()
            } catch (e: Exception) { "Root failed: ${e.message}" }
            runOnUiThread { note(result.ifBlank { "Done, no output" }) }
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

    private fun card(parent: LinearLayout, title: String, fill: LinearLayout.() -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (richUi) {
                background = rounded(0xFF141A22.toInt(), 16)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
        }
        box.addView(section(title))
        box.fill()
        val lp = LinearLayout.LayoutParams(-1, -2)
        if (richUi) lp.topMargin = dp(12)
        parent.addView(box, lp)
    }

    private fun LinearLayout.gap(v: View) {
        val lp = LinearLayout.LayoutParams(
            if (v.layoutParams is LinearLayout.LayoutParams && (v.layoutParams as LinearLayout.LayoutParams).width == 0) 0 else -1,
            -2
        )
        if (v.layoutParams is LinearLayout.LayoutParams) {
            val old = v.layoutParams as LinearLayout.LayoutParams
            if (old.width == 0) lp.width = 0
            lp.weight = old.weight
        }
        if (richUi) lp.topMargin = dp(8)
        addView(v, lp)
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun confirm(msg: String, yes: () -> Unit) {
        try {
            note("Need confirm: " + msg.lineSequence().first(), toast = false)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Confirm")
                .setMessage(msg)
                .setPositiveButton("Yes") { _, _ -> yes() }
                .setNegativeButton("Cancel") { _, _ -> note("Cancelled") }
                .show()
        } catch (e: Exception) {
            note("Dialog failed: " + (e.message ?: e.javaClass.simpleName))
        }
    }
    private fun title(t: String, s: Int) = TextView(this).apply {
        text = t; textSize = s.toFloat(); setTextColor(if (richUi) 0xFFF0F6FC.toInt() else 0xFFE6EDF3.toInt()); typeface = Typeface.DEFAULT_BOLD
    }
    private fun subtitle(t: String) = TextView(this).apply { text = t; textSize = 13f; setTextColor(0xFF8B949E.toInt()); setPadding(0, dp(2), 0, dp(8)) }
    private fun section(t: String) = TextView(this).apply {
        text = t.uppercase(); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (richUi) 0xFF7EE787.toInt() else 0xFF58A6FF.toInt())
        setPadding(0, if (richUi) 0 else dp(18), 0, dp(6))
    }
    private fun body(t: String, s: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = s.toFloat(); setTextColor(0xFFC9D1D9.toInt()); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun hint(t: String) = TextView(this).apply { text = t; textSize = 12f; setTextColor(0xFF8B949E.toInt()) }
    private fun mutedLabel(label: String) = listOf("Reset", "OFF", "Native", "Clear", "default").any { label.contains(it, true) }
    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; minHeight = dp(if (richUi) 50 else 48)
        setOnClickListener {
            try {
                note("Tapped: " + label, toast = true)
                action()
            } catch (e: Exception) {
                note("Button error: " + (e.message ?: e.javaClass.simpleName))
            }
        }
        if (richUi) {
            val color = when {
                label.startsWith("Switch to") -> 0xFF8957E5.toInt()
                mutedLabel(label) -> 0xFF21262D.toInt()
                else -> 0xFF1F6FEB.toInt()
            }
            background = rounded(color, 12)
            setTextColor(0xFFFFFFFF.toInt())
        }
    }
    private fun rowButtons(a: Pair<String, () -> Unit>, b: Pair<String, () -> Unit>) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(actionButton(a.first, a.second), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        addView(actionButton(b.first, b.second), LinearLayout.LayoutParams(0, -2, 1f))
    }
    private fun numberField(h: String) = EditText(this).apply {
        hint = h; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true)
        setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF6E7681.toInt())
        background = rounded(0xFF21262D.toInt(), 10); minHeight = dp(48); setPadding(dp(12), 0, dp(12), 0)
    }
    private fun textField(h: String) = EditText(this).apply {
        hint = h; setSingleLine(true); setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF6E7681.toInt())
        background = rounded(0xFF21262D.toInt(), 10); minHeight = dp(48); setPadding(dp(12), 0, dp(12), 0)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
