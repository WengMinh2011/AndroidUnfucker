# AndroidUnfucker

A cautious, reversible Android tuning app, initially targeting older devices including Android 8.1 / ColorOS 5.x.

## App status

The first native Android app implementation is in the repository. It provides:
- Device and Android build information
- Logical display resolution controls
- Display density (DPI) controls
- Balanced, Snappy UI, and Stock animation presets
- Read-only CPU / thermal diagnostics where Android permits access

**The app requires Shizuku** to run privileged ADB-shell commands without root. Start Shizuku, grant AndroidUnfucker permission, then use the controls. The app does not silently apply changes.

## Compatibility

- Minimum Android: 8.1 (API 27)
- Initial target: OPPO A5 / Snapdragon 450 / ColorOS 5.x
- Build: Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 17
- APK builds are produced by GitHub Actions.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Android APK**.
3. Choose **Run workflow** (or wait for a push build).
4. Open the completed workflow run.
5. Download the **AndroidUnfucker-debug** artifact and extract the APK.

## Shizuku setup

Install Shizuku from its official project page: https://shizuku.rikka.app/
Follow the setup instructions for your Android version. Shizuku's wireless debugging setup differs by Android release; Android 8.1 may require starting the service through a computer using ADB, depending on the device.

## Safety and limitations

- Resolution and density are logical Android overrides, not changes to the physical panel.
- Animation presets change UI animation timing; they do not increase CPU/GPU performance.
- Touchscreen sampling is controlled by the controller, kernel, and vendor firmware. There is no universal safe ADB touch-boost command.
- CPU/GPU frequency and thermal data are shown only if the device exposes readable interfaces.
- No overclocking, thermal-limit removal, undocumented kernel writes, package debloating, or background-killer loops are included.
- Use conservative display values and reset overrides if the interface becomes difficult to use: `wm size reset` and `wm density reset`.

## License

See LICENSE.
