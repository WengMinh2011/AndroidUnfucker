# AndroidUnfucker presets & performance levels

## One-tap profiles

| Profile | Animations | Stay awake | Force GPU rendering |
|---------|------------|------------|---------------------|
| Gaming | 0× | On while charging | On |
| Balanced | 0.5× | Off | Off |
| Power saving | 0.25× | Off | Off |

Display resolution/DPI are never changed by profiles.

## CPU performance levels

These change the **cpufreq governor** only (how aggressively the CPU uses existing clocks). They do **not** raise `cpuinfo_max_freq`.

| Level | Preferred governors (first available wins) |
|-------|--------------------------------------------|
| Low | `powersave` |
| Balanced | `schedutil` → `interactive` → `ondemand` → `conservative` |
| High | `performance` |

**Root is usually required** to write `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor`. Without root the app still tries and reports failures.

Thermal limits remain active.

## GPU performance levels

Best-effort writes to common nodes:

- Adreno: `/sys/class/kgsl/kgsl-3d0/devfreq/governor`
- Mali / generic: `/sys/class/devfreq/*mali*/governor`, `*gpu*/governor`

| Level | Preferred governors |
|-------|---------------------|
| Low | `powersave` |
| Balanced | `msm-adreno-tz` / `simple_ondemand` / `ondemand` |
| High | `performance` |

If the node is missing or not writable, the action is skipped and reported in Output.

## Not supported

- Overclocking (raising max frequency tables)
- Disabling thermal throttling
- Forcing Vulkan vs OpenGL ES inside games (app decides)
