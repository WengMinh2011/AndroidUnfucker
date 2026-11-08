# AndroidUnfucker

**A cautious Android tuning toolkit focused on older Android devices, starting with Android 8.1 / ColorOS.**

AndroidUnfucker brings practical, reversible display and UI tuning into one place: logical resolution, display density (DPI), animation scales, and device diagnostics. The first target is an OPPO A5-class Android 8.1 device, while keeping the tooling generic wherever Android supports it.

> **Status:** Early development. This repository currently contains safe ADB helper scripts and documentation; it is not yet a finished Android app or APK.

## What it can do

- Read the device's current logical display size and density.
- Change logical resolution with Android's `wm size` override.
- Change logical display density with `wm density`.
- Reset resolution and density overrides to the device defaults.
- Adjust the three standard Android animation scales.
- Restore animation scales to normal.
- Explain what touch-related settings can and cannot safely change.

## Quick start (Windows)

1. Install Google's [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools).
2. On the phone, enable **Developer options** and **USB debugging**.
3. Connect the phone and approve its debugging prompt.
4. Extract Platform-Tools, then place `scripts\\windows\\androidunfucker.bat` in the same folder as `adb.exe` (or add Platform-Tools to your PATH).
5. Run the batch file and choose an option from the menu.

Check the connection first:

```bat
adb devices
```

## Display controls

- **Resolution** changes the logical rendering size. A lower logical resolution may reduce rendering work in some games, but it can make the image softer and cause layout issues. It does not change the physical panel resolution.
- **DPI / density** changes the size of interface elements and how much content fits on screen. It is not a direct FPS or touch-sampling tweak.
- **Reset** clears the override and returns to the ROM's default display configuration.

Always note your current values before experimenting. If the interface becomes difficult to use, reconnect with ADB and run:

```bat
adb shell wm size reset
adb shell wm density reset
```

## Performance profile

The helper offers conservative animation-scale presets. These can make the interface feel quicker, but they do not increase the CPU/GPU's physical performance or guarantee higher game FPS.

AndroidUnfucker deliberately avoids undocumented kernel writes, thermal-limit changes, forced CPU/GPU clocks, blanket package disabling, and "RAM cleaner" tricks. Those can cause instability, overheating, broken notifications, or data loss.

## Touch responsiveness: important limitation

There is no universal, safe ADB command that increases a phone's touchscreen polling rate or changes its hardware sampling configuration. Touch sampling is controlled by the touchscreen controller, kernel driver, and vendor firmware. AndroidUnfucker will not write undocumented `/sys` nodes or claim that unrelated settings improve finger-touch latency.

The `pointer_speed` setting applies to a connected mouse/trackpad on supported builds; it does **not** tune finger touch.

## Compatibility

- Designed around standard ADB shell commands available on many Android versions.
- Initial testing target: Android 8.1 / ColorOS 5.x.
- Some OEM builds restrict or change individual settings. A command succeeding does not prove that a measurable performance gain occurred.
- No root access is required for the included display and animation helpers.

## Safety

- Use only on a device you own or are authorized to manage.
- Keep USB debugging approval limited to computers you trust.
- Change one setting at a time and test before continuing.
- Do not use extreme resolution or density values; tiny controls and clipped system UI can make the phone hard to operate.
- Back up important data before experimenting with system-level modifications.
- The scripts are opt-in and do not automatically run at startup.

## Roadmap

- [x] Safe ADB display and animation helper (initial)
- [x] Reset commands and compatibility notes
- [ ] Device detection and saved per-device profiles
- [ ] Android 8.1-compatible app shell
- [ ] Touch/display diagnostics (read-only where possible)
- [ ] More device-specific profiles after real-device testing

## Disclaimer

AndroidUnfucker is an independent community project and is not affiliated with Google, OPPO, Xiaomi, or the maintainers of HyperOSUnfucker. Use at your own risk. No performance result is guaranteed.

## References

- [Android Debug Bridge documentation](https://developer.android.com/tools/adb)
- [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
