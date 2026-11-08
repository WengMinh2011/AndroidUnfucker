# AndroidUnfucker presets

Presets are convenience profiles of **safe, reversible** Android settings. They do not overclock the SoC, remove thermal limits, or change touchscreen firmware.

## One-tap profiles

| Profile | Animations | Stay awake | Force GPU rendering | Notes |
|---------|------------|------------|---------------------|-------|
| **Gaming** | 0× (off) | On while charging | On | Less UI delay; does not raise game FPS by itself |
| **Balanced** | 0.5× | Off | Off | Everyday use |
| **Power saving** | 0.25× (smooth) | Off | Off | Slightly lighter UI work |

Display resolution and DPI are **not** changed by profiles so a bad size cannot lock you out of the UI.

## Animation-only scales

| Label | Scale |
|-------|------:|
| Disable | 0 |
| Smooth | 0.25 |
| Balanced | 0.5 |
| Stock | 1 |

## Graphics flags

- `force_gpu_rendering` — developer option; may help some 2D UI paths, not a Vulkan switch.
- `disable_hw_overlays` — forces GPU composition; useful for debugging jank, can use more power.

Games choose **Vulkan / OpenGL / OpenGL ES** themselves. There is no universal safe ADB switch that forces Vulkan on every title on Android 8.1.

## Thermal / throttling

**Not implemented on purpose.** Removing thermal limits can permanently damage the device. Use the thermal snapshot (read-only) instead.

## App freeze

`pm disable-user --user 0 <package>` freezes an app until `pm enable <package>`. Avoid freezing core system packages (`com.android.*` critical services, phone, settings).
