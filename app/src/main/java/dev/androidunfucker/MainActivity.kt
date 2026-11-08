package dev.androidunfucker

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var dpiInput: EditText
    private val requestCode = 4102

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == requestCode) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                status.text = "Shizuku permission granted"
                refreshDisplay()
            } else status.text = "Shizuku permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        buildUi()
        updatePermissionStatus()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
            setBackgroundColor(0xFF101318.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("ANDROIDUNFUCKER", 25, true))
        content.addView(label("Reversible Android tuning • Early access", 13, false))
        status = label("Checking Shizuku...", 14, true)
        content.addView(status)
        content.addView(button("Grant / reconnect Shizuku") { requestShizuku() })

        content.addView(section("DEVICE"))
        content.addView(label("Model: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nHardware: ${Build.HARDWARE}", 14, false))
        content.addView(button("Read display size and DPI") { refreshDisplay() })

        content.addView(section("DISPLAY"))
        content.addView(label("Logical resolution (width × height)", 14, true))
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        widthInput = edit("Width")
        heightInput = edit("Height")
        sizeRow.addView(widthInput, weightParams())
        sizeRow.addView(heightInput, weightParams())
        content.addView(sizeRow)
        content.addView(button("Apply resolution") { applyResolution() })
        content.addView(button("Reset resolution") { runShell("wm size reset") })

        content.addView(label("Display density (DPI)", 14, true))
        dpiInput = edit("DPI")
        content.addView(dpiInput)
        content.addView(button("Apply DPI") { applyDpi() })
        content.addView(button("Reset DPI") { runShell("wm density reset") })

        content.addView(section("PRESETS"))
        content.addView(label("These presets change animation timing only. They do not overclock the CPU/GPU or increase touchscreen sampling.", 13, false))
        content.addView(button("Balanced • 0.5× animations") { setAnimations("0.5") })
        content.addView(button("Snappy UI • animations off") { setAnimations("0") })
        content.addView(button("Stock • 1× animations") { setAnimations("1") })

        content.addView(section("ADVANCED DIAGNOSTICS"))
        content.addView(button("Read CPU / thermal information") {
            runShell("getprop ro.board.platform; getprop ro.hardware; cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null; cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null")
        })
        content.addView(label("Diagnostics are read-only where the OS permits access. No clock, governor, thermal-limit, or undocumented touch-driver writes are performed.", 13, false))

        content.addView(section("OUTPUT"))
        output = label("No commands run yet.", 12, false)
        output.setTextIsSelectable(true)
        content.addView(output)
        content.addView(label("Requires Shizuku running and permission granted. Use the official Shizuku app and follow its setup instructions for your Android version. Changes are made only after you tap an action.", 12, false))

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun updatePermissionStatus() {
        status.text = when {
            !Shizuku.pingBinder() -> "Shizuku is not running"
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED -> "Shizuku connected • permission granted"
            else -> "Shizuku connected • permission needed"
        }
    }

    private fun requestShizuku() {
        if (!Shizuku.pingBinder()) {
            status.text = "Start Shizuku first, then return here"
            return
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            updatePermissionStatus()
        } else Shizuku.requestPermission(requestCode)
    }

    private fun refreshDisplay() {
        runShell("wm size; wm density")
    }

    private fun applyResolution() {
        val w = widthInput.text.toString().toIntOrNull()
        val h = heightInput.text.toString().toIntOrNull()
        if (w == null || h == null || w !in 320..3000 || h !in 320..5000) {
            output.text = "Enter a valid width (320–3000) and height (320–5000)."
            return
        }
        runShell("wm size ${w}x${h}")
    }

    private fun applyDpi() {
        val dpi = dpiInput.text.toString().toIntOrNull()
        if (dpi == null || dpi !in 100..800) {
            output.text = "Enter a DPI value between 100 and 800."
            return
        }
        runShell("wm density $dpi")
    }

    private fun setAnimations(scale: String) {
        runShell("settings put global window_animation_scale $scale; settings put global transition_animation_scale $scale; settings put global animator_duration_scale $scale")
    }

    private fun runShell(command: String) {
        if (!Shizuku.pingBinder()) {
            status.text = "Shizuku is not running"
            output.text = "Open Shizuku, start its service, then reconnect."
            return
        }
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestShizuku()
            return
        }
        Thread {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val text = process.inputStream.bufferedReader().use { it.readText() }
                val err = process.errorStream.bufferedReader().use { it.readText() }
                val code = process.waitFor()
                runOnUiThread {
                    output.text = (text + if (err.isNotBlank()) "\n$err" else "" + "\nExit code: $code").ifBlank { "Command completed (no output). Exit code: $code" }
                    updatePermissionStatus()
                }
            } catch (e: Exception) {
                runOnUiThread { output.text = "Command failed: ${e.message}" }
            }
        }.start()
    }

    private fun section(text: String) = label(text, 16, true).apply {
        setPadding(0, dp(22), 0, dp(6))
        setTextColor(0xFF80CBC4.toInt())
    }

    private fun label(text: String, size: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(0xFFE8EDF2.toInt())
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun edit(hintText: String) = EditText(this).apply {
        hint = hintText
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        setSingleLine(true)
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF9AA4AE.toInt())
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(6) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
