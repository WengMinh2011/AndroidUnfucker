# Tuning notes

## Supported controls in the first helper

| Control | ADB interface | What it changes | Reversible |
|---|---|---|---|
| Logical resolution | `wm size WxH` | Android's logical display dimensions | `wm size reset` |
| Logical density | `wm density DPI` | UI scale / dp conversion | `wm density reset` |
| Window animation | `settings put global window_animation_scale` | Window animation duration | Set to `1` |
| Transition animation | `settings put global transition_animation_scale` | Activity transition duration | Set to `1` |
| Animator duration | `settings put global animator_duration_scale` | Property animator duration | Set to `1` |

## Suggested testing method

1. Record the original output of `adb shell wm size` and `adb shell wm density`.
2. Change only one setting.
3. Test the launcher, Settings, keyboard, and the game you care about.
4. If anything is clipped or uncomfortable, use the matching reset command.
5. Compare actual frame pacing with the same scene and settings; do not assume that a smaller resolution always improves FPS.

## Touch input

Do not advertise a generic "touch boost". Finger-touch sampling and controller latency are vendor / driver dependent. Changing display density or resolution does not raise the touchscreen's hardware polling rate.

Avoid writing arbitrary values to `/sys/class/input`, `/proc`, or undocumented vendor properties. Incorrect writes can be ignored, cause input instability, or require a reboot / repair to recover.

## Deliberately excluded

- CPU/GPU overclocking or governor forcing
- Thermal throttling removal
- Blanket debloating / disabling system packages
- Fake RAM expansion, memory cleaner loops, or persistent background killers
- Unverified touch firmware / kernel tweaks
