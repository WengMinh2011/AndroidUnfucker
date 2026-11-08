# Tuning notes

## Supported controls

| Control | Interface | What it changes | Reversible |
|---------|-----------|-----------------|------------|
| Logical resolution | `wm size WxH` | Android logical display size | `wm size reset` |
| Logical density | `wm density DPI` | UI scale / dp | `wm density reset` |
| Window animation | `settings put global window_animation_scale` | Window animation duration | set to `1` |
| Transition animation | `settings put global transition_animation_scale` | Activity transition duration | set to `1` |
| Animator duration | `settings put global animator_duration_scale` | Property animator duration | set to `1` |
| Stay awake | `settings put global stay_on_while_plugged_in` | Screen stays on while charging | set to `0` |
| Force-stop | `am force-stop <pkg>` | Stops a running package | relaunch app |
| Clear cache | `pm clear --cache-only` (or trim) | Package cache only | app rebuilds cache |

## Non-root (Shizuku) vs root

- **Shizuku / ADB shell**: everything in the main UI except the “Root extras” section.
- **Root**: deeper `/sys` thermal and frequency dumps, `dmesg`. Still read-only diagnostics in the current design.

## Suggested testing method

1. Record original `wm size` and `wm density`.
2. Change **one** setting at a time.
3. Test launcher, Settings, keyboard, and your main apps/games.
4. Reset immediately if anything is clipped or unusable.
5. Do not assume a smaller resolution always improves game FPS.

## Touch input

There is no universal safe “touch boost” via ADB. Sampling rate is controller / kernel / vendor firmware. Changing DPI or resolution does **not** raise the hardware touch polling rate.

Avoid writing arbitrary values under `/sys/class/input`, `/proc`, or undocumented vendor properties.

## Deliberately excluded

- CPU/GPU overclocking or governor forcing
- Thermal throttling removal
- Blanket debloating / disabling system packages
- Fake RAM expansion or persistent memory cleaners
- Unverified touch firmware / kernel tweaks
