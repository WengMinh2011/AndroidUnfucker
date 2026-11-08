package dev.androidunfucker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
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

/**
 * AndroidUnfucker — cautious, reversible tuning for older / low-end devices.
 * Privileged actions use Shizuku (no root). Optional CPU/GPU governor levels when root is present.
 * No overclocking and no thermal-limit removal.
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
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(title("AndroidUnfucker", 22))
        content.addView(subtitle("Reversible tuning • low-end friendly"))

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

        // Profiles
        content.addView(section("Profiles"))
        content.addView(hint("Combines animation + safe settings. Display size/DPI left unchanged."))
        content.addView(actionButton("Gaming") {
            confirm("Apply Gaming profile?\n• Animations off\n• Stay awake while charging\n• Force GPU rendering on") {
                applyProfileGaming()
            }
        })
        content.addView(actionButton("Balanced") {
            confirm("Apply Balanced profile?\n• Animations 0.5×\n• Stay awake off\n• Force GPU rendering off") {
                applyProfileBalanced()
            }
        })
        content.addView(actionButton("Power saving") {
            confirm("Apply Power saving profile?\n• Smooth animations 0.25×\n• Stay awake off\n• Force GPU rendering off") {
                applyProfilePowerSaving()
            }
        })

        // CPU performance levels
        content.addView(section("CPU performance levels"))
        content.addView(
            hint(
                "Levels map to CPU governors (not overclock). " +
                    "Writing governors usually needs root. Thermal limits stay active."
            )
        )
        content.addView(actionButton("Read CPU status") { readCpuStatus() })
        content.addView(actionButton("CPU • Low (powersave)") {
            setCpuLevel("low")
        })
        content.addView(actionButton("CPU • Balanced (schedutil/interactive)") {
            setCpuLevel("balanced")
        })
        content.addView(actionButton("CPU • High (performance)") {
            setCpuLevel("high")
        })

        // GPU performance levels
        content.addView(section("GPU performance levels"))
        content.addView(
            hint(
                "Tries Adreno (kgsl) and common Mali devfreq governors. " +
                    "Does not raise max clocks beyond kernel tables. Root usually required to write."
            )
        )
        content.addView(actionButton("Read GPU status") { readGpuStatus() })
        content.addView(actionButton("GPU • Low") {
            setGpuLevel("low")
        })
        content.addView(actionButton("GPU • Balanced") {
            setGpuLevel("balanced")
        })
        content.addView(actionButton("GPU • High") {
            setGpuLevel("high")
        })

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

        // Animations
        content.addView(section("Animations"))
        content.addView(hint("Window / transition / animator scales. Does not raise game FPS."))
        content.addView(actionButton("Disable animations (0×)") { setAnimations("0") })
        content.addView(actionButton("Smooth • 0.25×") { setAnimations("0.25") })
        content.addView(actionButton("Balanced • 0.5×") { setAnimations("0.5") })
        content.addView(actionButton("Stock • 1×") { setAnimations("1") })
        content.addView(actionButton("Read current animation scales") {
            runShell(
                "settings get global window_animation_scale; " +
                    "settings get global transition_animation_scale; " +
                    "settings get global animator_duration_scale"
            )
        })

        // Graphics flags
        content.addView(section("Graphics (GPU / GLES flags)"))
        content.addView(
            hint(
                "Developer graphics flags only. Apps still choose Vulkan / OpenGL / GLES themselves."
            )
        )
        content.addView(actionButton("Force GPU rendering ON") {
            runShell("settings put global force_gpu_rendering 1; settings get global force_gpu_rendering")
        })
        content.addView(actionButton("Force GPU rendering OFF") {
            runShell("settings put global force_gpu_rendering 0; settings get global force_gpu_rendering")
        })
        content.addView(actionButton("Disable HW overlays ON") {
            runShell("settings put global disable_hw_overlays 1; settings get global disable_hw_overlays")
        })
        content.addView(actionButton("Disable HW overlays OFF") {
            runShell("settings put global disable_hw_overlays 0; settings get global disable_hw_overlays")
        })
        content.addView(actionButton("Show GPU / render info (dumpsys)") {
            runShell(
                "dumpsys SurfaceFlinger 2>/dev/null | head -n 40; " +
                    "echo ---; " +
                    "getprop ro.hardware.egl; getprop ro.hardware.vulkan; " +
                    "getprop debug.egl.hw; getprop debug.sf.hw"
            )
        })

        // System menus
        content.addView(section("System menus"))
        content.addView(hint("Opens built-in screens when the OEM exposes them."))
        content.addView(actionButton("Open Developer options") { openDeveloperOptions() })
        content.addView(actionButton("Open system Settings") {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                outputView.text = "Could not open Settings: ${e.message}"
            }
        })
        content.addView(actionButton("Try OEM performance / game menu") { openOemPerformanceMenu() })

        // Thermal
        content.addView(section("Thermal / throttling"))
        content.addView(
            hint(
                "Disabling thermal throttling is not supported. " +
                    "It can overheat and damage the SoC. Read-only diagnostics only."
            )
        )
        content.addView(actionButton("Battery + thermal snapshot") {
            runShell(
                "dumpsys battery 2>/dev/null | head -n 25; " +
                    "echo ---; " +
                    "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null; " +
                    "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null; " +
                    "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null"
            )
        })

        // Apps
        content.addView(section("Apps: stop / freeze"))
        content.addView(
            hint(
                "Force-stop ends the process. Freeze (disable-user) hides the app until unfrozen. " +
                    "Do not freeze critical system packages."
            )
        )
        packageInput = textField("package.name.here")
        content.addView(packageInput)
        content.addView(rowButtons(
            "Force-stop" to { forceStopPackage() },
            "Clear cache" to { clearPackageCache() }
        ))
        content.addView(rowButtons(
            "Freeze app" to { freezePackage() },
            "Unfreeze app" to { unfreezePackage() }
        ))
        content.addView(actionButton("List disabled (frozen) packages") {
            runShell("pm list packages -d 2>/dev/null | head -n 40")
        })

        // Advanced Shizuku
        content.addView(section("Advanced (Shizuku)"))
        content.addView(actionButton("Stay awake while charging") {
            runShell("settings put global stay_on_while_plugged_in 7")
        })
        content.addView(actionButton("Disable stay awake") {
            runShell("settings put global stay_on_while_plugged_in 0")
        })
        content.addView(actionButton("Running processes (top 30)") {
            runShell("ps -A 2>/dev/null | head -n 30 || ps | head -n 30")
        })
        content.addView(actionButton("Reset ALL display overrides") {
            confirm("Reset resolution and density to defaults?") {
                runShell("wm size reset; wm density reset")
            }
        })

        // Root extras
        content.addView(section("Root extras"))
        content.addView(hint("Extra readouts. Governor writes are in the CPU/GPU sections above."))
        content.addView(actionButton("Detect root") { detectRootAndRefresh() })
        content.addView(actionButton("Deep sysfs / thermal (root)") {
            if (!hasRoot) {
                outputView.text = "Root not detected."
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
                "Requires Shizuku running + permission for most actions. " +
                    "CPU/GPU level writes usually need root. " +
                    "If the UI becomes unusable: wm size reset && wm density reset"
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(root)
    }

    // ─── CPU / GPU levels ─────────────────────────────────────────────────

    private fun readCpuStatus() {
        val cmd =
            "echo '=== CPU governors ==='; " +
                "for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do " +
                "  echo \$g: \$(cat \$g 2>/dev/null); " +
                "done; " +
                "echo '=== available (cpu0) ==='; " +
                "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null; " +
                "echo '=== current freqs ==='; " +
                "for c in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do " +
                "  echo \$c: \$(cat \$c 2>/dev/null); " +
                "done; " +
                "echo '=== min/max (cpu0) ==='; " +
                "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq 2>/dev/null; " +
                "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq 2>/dev/null"
        if (hasRoot) runRoot(cmd) else runShell(cmd)
    }

    /**
     * low -> powersave
     * balanced -> schedutil, then interactive, then ondemand
     * high -> performance
     */
    private fun setCpuLevel(level: String) {
        val label = when (level) {
            "low" -> "Low (powersave)"
            "balanced" -> "Balanced (schedutil/interactive)"
            else -> "High (performance)"
        }
        val script = when (level) {
            "low" -> cpuGovernorScript(listOf("powersave"))
            "balanced" -> cpuGovernorScript(listOf("schedutil", "interactive", "ondemand", "conservative"))
            else -> cpuGovernorScript(listOf("performance"))
        }
        confirm(
            "Set CPU level to $label?\n" +
                "This only changes the governor (how often the CPU runs fast). " +
                "It does not raise the max clock. Root is usually required.\n" +
                "Thermal limits stay active."
        ) {
            if (hasRoot) runRoot(script) else {
                // Try via Shizuku shell; often permission denied without root
                runShell(script + "; echo '(If writes failed, root is required on this device.)'")
            }
        }
    }

    private fun cpuGovernorScript(preferred: List<String>): String {
        val prefs = preferred.joinToString(" ")
        return """
            echo "Preferred governors: $prefs"
            AVAIL=\$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null)
            echo "Available: \$AVAIL"
            CHOSEN=""
            for g in $prefs; do
              echo "\$AVAIL" | grep -qw "\$g" && CHOSEN="\$g" && break
            done
            if [ -z "\$CHOSEN" ]; then
              echo "No preferred governor available on this kernel."
              exit 1
            fi
            echo "Applying governor: \$CHOSEN"
            for path in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
              echo \$CHOSEN > "\$path" 2>/dev/null && echo "OK \$path" || echo "FAIL \$path"
            done
            echo "=== result ==="
            for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
              echo "\$g: \$(cat \$g 2>/dev/null)"
            done
        """.trimIndent()
    }

    private fun readGpuStatus() {
        val cmd =
            "echo '=== Adreno kgsl ==='; " +
                "for p in /sys/class/kgsl/kgsl-3d0/devfreq/governor " +
                "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq " +
                "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors " +
                "/sys/class/kgsl/kgsl-3d0/gpuclk " +
                "/sys/class/kgsl/kgsl-3d0/max_gpuclk; do " +
                "  if [ -e \$p ]; then echo \$p: \$(cat \$p 2>/dev/null); fi; " +
                "done; " +
                "echo '=== Mali / generic devfreq ==='; " +
                "for p in /sys/class/devfreq/*mali*/governor " +
                "/sys/class/devfreq/*mali*/cur_freq " +
                "/sys/class/devfreq/*mali*/available_governors " +
                "/sys/class/devfreq/*gpu*/governor; do " +
                "  for f in \$p; do if [ -e \$f ]; then echo \$f: \$(cat \$f 2>/dev/null); fi; done; " +
                "done; " +
                "echo '=== props ==='; " +
                "getprop ro.hardware.egl; getprop ro.hardware.vulkan"
        if (hasRoot) runRoot(cmd) else runShell(cmd)
    }

    /**
     * GPU governor preference lists for Adreno and Mali-style nodes.
     */
    private fun setGpuLevel(level: String) {
        val label = when (level) {
            "low" -> "Low"
            "balanced" -> "Balanced"
            else -> "High"
        }
        val adrenoPrefs = when (level) {
            "low" -> listOf("powersave")
            "balanced" -> listOf("msm-adreno-tz", "simple_ondemand", "ondemand", "userspace")
            else -> listOf("performance")
        }
        val maliPrefs = when (level) {
            "low" -> listOf("powersave")
            "balanced" -> listOf("simple_ondemand", "ondemand", "userspace")
            else -> listOf("performance")
        }
        val script = gpuGovernorScript(adrenoPrefs, maliPrefs)
        confirm(
            "Set GPU level to $label?\n" +
                "Writes device governors if the kernel exposes them. " +
                "Not an overclock. Root is usually required."
        ) {
            if (hasRoot) runRoot(script) else {
                runShell(script + "; echo '(If writes failed, root is required on this device.)'")
            }
        }
    }

    private fun gpuGovernorScript(adreno: List<String>, mali: List<String>): String {
        val a = adreno.joinToString(" ")
        val m = mali.joinToString(" ")
        return """
            apply_gov() {
              NODE="\$1"; shift
              AVAIL=\$(cat "\$(dirname \$NODE)/available_governors" 2>/dev/null)
              [ -z "\$AVAIL" ] && AVAIL=\$(cat "\$NODE" 2>/dev/null)
              CHOSEN=""
              for g in \$@; do
                echo "\$AVAIL" | grep -qw "\$g" && CHOSEN="\$g" && break
              done
              if [ -n "\$CHOSEN" ] && [ -w "\$NODE" ]; then
                echo \$CHOSEN > "\$NODE" 2>/dev/null && echo "OK \$NODE -> \$CHOSEN" || echo "FAIL \$NODE"
              elif [ -e "\$NODE" ]; then
                echo "SKIP \$NODE (no match or not writable). avail=\$AVAIL"
              fi
            }

            echo "=== Adreno ==="
            for node in /sys/class/kgsl/kgsl-3d0/devfreq/governor /sys/class/kgsl/kgsl-3d0/pwrlevel; do
              [ -e \$node ] && apply_gov \$node $a
            done

            echo "=== Mali / other GPU devfreq ==="
            for node in /sys/class/devfreq/*mali*/governor /sys/class/devfreq/*gpu*/governor; do
              for n in \$node; do
                [ -e \$n ] && apply_gov \$n $m
              done
            done

            echo "=== status after ==="
            for p in /sys/class/kgsl/kgsl-3d0/devfreq/governor /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq; do
              [ -e \$p ] && echo \$p: \$(cat \$p 2>/dev/null)
            done
            for p in /sys/class/devfreq/*mali*/governor /sys/class/devfreq/*mali*/cur_freq; do
              for f in \$p; do [ -e \$f ] && echo \$f: \$(cat \$f 2>/dev/null); done
            done
        """.trimIndent()
    }

    // ─── Profiles ─────────────────────────────────────────────────────────

    private fun applyProfileGaming() {
        runShell(
            "settings put global window_animation_scale 0; " +
                "settings put global transition_animation_scale 0; " +
                "settings put global animator_duration_scale 0; " +
                "settings put global stay_on_while_plugged_in 7; " +
                "settings put global force_gpu_rendering 1; " +
                "echo PROFILE=gaming; " +
                "settings get global window_animation_scale; " +
                "settings get global force_gpu_rendering; " +
                "settings get global stay_on_while_plugged_in"
        )
    }

    private fun applyProfileBalanced() {
        runShell(
            "settings put global window_animation_scale 0.5; " +
                "settings put global transition_animation_scale 0.5; " +
                "settings put global animator_duration_scale 0.5; " +
                "settings put global stay_on_while_plugged_in 0; " +
                "settings put global force_gpu_rendering 0; " +
                "echo PROFILE=balanced; " +
                "settings get global window_animation_scale; " +
                "settings get global force_gpu_rendering"
        )
    }

    private fun applyProfilePowerSaving() {
        runShell(
            "settings put global window_animation_scale 0.25; " +
                "settings put global transition_animation_scale 0.25; " +
                "settings put global animator_duration_scale 0.25; " +
                "settings put global stay_on_while_plugged_in 0; " +
                "settings put global force_gpu_rendering 0; " +
                "echo PROFILE=power_saving; " +
                "settings get global window_animation_scale; " +
                "settings get global force_gpu_rendering"
        )
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
        if (!validPackage(pkg)) return
        confirm("Force-stop $pkg?") { runShell("am force-stop $pkg") }
    }

    private fun clearPackageCache() {
        val pkg = packageInput.text.toString().trim()
        if (!validPackage(pkg)) return
        confirm("Clear cache for $pkg? (app data kept)") {
            runShell("pm clear --cache-only $pkg 2>/dev/null || pm trim-caches 999G")
        }
    }

    private fun freezePackage() {
        val pkg = packageInput.text.toString().trim()
        if (!validPackage(pkg)) return
        confirm("Freeze (disable-user) $pkg?\nYou can unfreeze later. Avoid system packages.") {
            runShell("pm disable-user --user 0 $pkg")
        }
    }

    private fun unfreezePackage() {
        val pkg = packageInput.text.toString().trim()
        if (!validPackage(pkg)) return
        confirm("Unfreeze (enable) $pkg?") { runShell("pm enable $pkg") }
    }

    private fun validPackage(pkg: String): Boolean {
        if (pkg.isEmpty() || !pkg.contains('.')) {
            outputView.text = "Enter a valid package name (e.g. com.android.chrome)."
            return false
        }
        return true
    }

    private fun openDeveloperOptions() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            outputView.text = "Opened Developer options."
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                outputView.text = "Developer options intent missing; opened main Settings."
            } catch (e: Exception) {
                outputView.text = "Could not open Settings: ${e.message}"
            }
        }
    }

    private fun openOemPerformanceMenu() {
        val candidates = listOf(
            Component("com.coloros.gamespace", "com.coloros.gamespace.GameSpaceActivity"),
            Component("com.oplus.games", "com.oplus.games.GameSpaceActivity"),
            Component("com.samsung.android.game.gamehome", "com.samsung.android.game.gamehome.MainActivity"),
            Component("com.miui.securitycenter", "com.miui.gamebooster.GameBoosterMainActivity"),
            Component("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
            Component("com.android.settings", "com.android.settings.Settings\$DevelopmentSettingsActivity"),
            Component("com.android.settings", "com.android.settings.DevelopmentSettings")
        )
        val tried = StringBuilder()
        for (c in candidates) {
            try {
                val intent = Intent().setClassName(c.pkg, c.cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                outputView.text = "Opened: ${c.pkg}/${c.cls}"
                return
            } catch (e: Exception) {
                tried.append(c.pkg).append(" — ").append(e.javaClass.simpleName).append('\n')
            }
        }
        try {
            val uri = Uri.parse("package:com.android.settings")
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
            outputView.text = "No OEM performance activity found.\nTried:\n$tried"
        } catch (e: Exception) {
            outputView.text = "No OEM performance menu found.\nTried:\n$tried"
        }
    }

    private data class Component(val pkg: String, val cls: String)

    // ─── Shell ────────────────────────────────────────────────────────────

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
            "Shizuku command failed: ${e.message}\nUpdate Shizuku or reinstall the app if this persists."
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────

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
            addView(
                actionButton(left.first, left.second),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            )
            addView(
                actionButton(right.first, right.second),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
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
