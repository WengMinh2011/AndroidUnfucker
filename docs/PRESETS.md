# AndroidUnfucker presets

Presets are convenience profiles, not magic performance boosts. The initial ADB helper only applies reversible Android animation-scale changes. It does not overclock the Snapdragon 450, change touchscreen firmware, or disable thermal protection.

## Initial presets

| Preset | Animation scales | Display resolution / density | Intended use |
|---|---:|---|---|
| Balanced | 0.5x | Unchanged | Everyday use; keeps transitions visible |
| Snappy UI | 0x | Unchanged | Minimal animation; may feel abrupt |
| Stock | 1x | Unchanged | Restore standard animation timing |
| Gaming feel | 0.5x | Unchanged | Less waiting through UI transitions before/after games |

The Gaming feel preset does not itself raise game FPS. Game rendering resolution can be configured separately using the display controls, but should be tested carefully and reset if the interface becomes distorted.

## Planned profiles

- **Display profiles:** save and restore user-tested resolution and density combinations, with a preview and confirmation.
- **Gaming profiles:** per-game settings only where Android/OEM interfaces actually support them.
- **Battery-conscious profile:** only documented, reversible settings; no background-killer loops.
- **Advanced diagnostics:** report available CPU/GPU frequency and thermal sensors without forcing clocks.

## Device-specific values

Do not ship fixed DPI or resolution numbers as universal OPPO A5 presets. Panel configuration, ROM, and current overrides must be detected on the device first. The app should record the original values and offer a one-tap reset before applying any display profile.

## Overclocking policy

CPU/GPU overclock controls remain hidden unless a compatible kernel exposes supported frequency tables and writable controls. Even then, thermal protections must remain active and the user must explicitly opt in. Monitoring is preferred over changing clocks.
