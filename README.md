# AndroidUnfucker

A cautious, reversible Android tuning app, aimed at older and low-end devices (Android 8.1+ / ColorOS 5.x and similar).

## What it does

- Device and build information
- Logical display resolution (`wm size`) with confirmation + reset
- Display density / DPI (`wm density`) with confirmation + reset
- Animation scale presets: Balanced 0.5×, Snappy (off), Stock 1×
- Shizuku-powered advanced tools (no root required):
  - Stay awake while charging
  - Battery + thermal snapshot
  - Running process list
  - Force-stop / clear-cache for a package
  - One-tap reset of all display overrides
- Optional **root extras** (only when `su` is available):
  - Deeper sysfs CPU frequency + thermal zone readout
  - `dmesg` tail
- Large touch targets and a simple dark UI for low-end / older screens

The app **never** applies changes silently. Destructive or hard-to-reverse actions ask for confirmation.

## Requirements

- **Shizuku** for privileged shell commands without root  
  → https://shizuku.rikka.app/
- Minimum Android 8.1 (API 27)
- Initial test target: OPPO A5 / Snapdragon 450 / ColorOS 5.x

Root is optional. Extra diagnostics appear when root is detected; overclocking, thermal-limit removal, and aggressive debloat are intentionally **not** included.

## Build an APK

1. Open the repository **Actions** tab
2. Select **Android APK**
3. Run the workflow (or push to `main`)
4. Download the **AndroidUnfucker-debug** artifact

Local build (JDK 17 + Android SDK):

```bash
gradle :app:assembleDebug
```

## Shizuku setup (especially Android 8.1)

1. Install Shizuku from the official site.
2. Start the Shizuku service (wireless debugging or ADB from a PC on older devices).
3. Open AndroidUnfucker → **Grant / reconnect Shizuku** → allow permission.

## Safety

| Action | Reversible? |
|--------|-------------|
| Logical resolution | `wm size reset` |
| Logical density | `wm density reset` |
| Animation scales | set back to `1` |
| Stay awake | set `stay_on_while_plugged_in` to `0` |
| Force-stop / clear cache | app-specific; data is not wiped by clear-cache path used here |

Still excluded on purpose:

- CPU/GPU overclock or governor forcing
- Thermal throttling removal
- Blanket system package debloat
- Undocumented touch / kernel sysfs writes
- Background-killer loops

If the UI becomes hard to use after a display change:

```text
wm size reset
wm density reset
```

## License

See LICENSE (add one if missing).
