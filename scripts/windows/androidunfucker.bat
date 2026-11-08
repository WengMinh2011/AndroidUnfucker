@echo off
setlocal EnableExtensions
title AndroidUnfucker - Safe ADB Tuner

where adb >nul 2>nul
if errorlevel 1 (
  echo [ERROR] adb.exe was not found.
  echo Put this file beside adb.exe or add Platform-Tools to PATH.
  pause
  exit /b 1
)

:menu
cls
echo ==========================================
echo          ANDROIDUNFUCKER
echo       Safe ADB display / UI tools
echo ==========================================
echo.
adb devices
echo.
echo [1] Show current display size and DPI
echo [2] Set logical resolution
echo [3] Reset resolution to device default
echo [4] Set logical DPI / density
echo [5] Reset DPI to device default
echo [6] Balanced UI animations (0.5x)
echo [7] Disable UI animations (0x)
echo [8] Restore normal UI animations (1x)
echo [9] Show basic device information
echo [0] Exit
echo.
set /p choice=Choose an option: 

if "%choice%"=="1" goto status
if "%choice%"=="2" goto setsize
if "%choice%"=="3" goto resetsize
if "%choice%"=="4" goto setdpi
if "%choice%"=="5" goto resetdpi
if "%choice%"=="6" goto animhalf
if "%choice%"=="7" goto animzero
if "%choice%"=="8" goto animnormal
if "%choice%"=="9" goto info
if "%choice%"=="0" exit /b 0
goto menu

:status
echo.
echo --- Logical display size ---
adb shell wm size
echo --- Logical display density ---
adb shell wm density
echo.
pause
goto menu

:setsize
echo.
echo Enter a logical width and height, for example 720x1520.
echo Avoid extreme values. This is not the panel's physical resolution.
set /p resolution=Resolution (WIDTHxHEIGHT): 
if "%resolution%"=="" goto menu
echo Applying %resolution% ...
adb shell wm size %resolution%
echo If the display becomes hard to use, choose Reset resolution.
pause
goto menu

:resetsize
echo.
adb shell wm size reset
echo Resolution override cleared.
pause
goto menu

:setdpi
echo.
echo Enter a density value in DPI. First check your current value with option 1.
set /p density=Density (DPI): 
if "%density%"=="" goto menu
echo Applying density %density% ...
adb shell wm density %density%
echo If the UI becomes hard to use, choose Reset DPI.
pause
goto menu

:resetdpi
echo.
adb shell wm density reset
echo Density override cleared.
pause
goto menu

:animhalf
echo.
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
echo Animation scales set to 0.5x.
pause
goto menu

:animzero
echo.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
echo Animation scales set to 0x. This changes UI animation only.
pause
goto menu

:animnormal
echo.
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
echo Animation scales restored to 1x.
pause
goto menu

:info
echo.
echo --- Manufacturer / model ---
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
echo --- Android version ---
adb shell getprop ro.build.version.release
echo --- SDK level ---
adb shell getprop ro.build.version.sdk
echo --- Build fingerprint ---
adb shell getprop ro.build.fingerprint
echo.
pause
goto menu

:presets
cls
echo ==========================================
echo             PRESET PROFILES
echo ==========================================
echo These profiles only change Android animation scales.
echo They do NOT overclock the CPU/GPU or change touch sampling.
echo.
echo [1] Balanced - 0.5x animations
echo [2] Snappy UI - animations off
echo [3] Stock - normal 1x animations
echo [4] Gaming feel - 0.5x animations, display unchanged
echo [0] Back
echo.
set /p preset=Choose a preset: 
if "%preset%"=="1" goto animhalf
if "%preset%"=="2" goto animzero
if "%preset%"=="3" goto animnormal
if "%preset%"=="4" goto animhalf
goto menu
