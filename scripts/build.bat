@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "GRADLEW=%PROJECT_DIR%\gradlew.bat"
set "OUTPUT_DIR=%PROJECT_DIR%\app\build\outputs\apk"

if "%1"=="" goto :usage
if /i "%1"=="debug" goto :debug
if /i "%1"=="release" goto :release
if /i "%1"=="both" goto :both
if /i "%1"=="clean" goto :clean
goto :usage

:debug
echo.
echo ========================================
echo   Building SnapReceipt DEBUG APK...
echo ========================================
echo.
call "%GRADLEW%" assembleDebug --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Debug build failed.
    exit /b 1
)
echo.
echo [SUCCESS] Debug APK:
for /r "%OUTPUT_DIR%\debug" %%f in (*.apk) do echo   %%f
goto :end

:release
echo.
echo ========================================
echo   Building SnapReceipt RELEASE APK...
echo ========================================
echo.
call "%GRADLEW%" assembleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Release build failed. Check signing config in local.properties.
    exit /b 1
)
echo.
echo [SUCCESS] Release APK:
for /r "%OUTPUT_DIR%\release" %%f in (*.apk) do echo   %%f
goto :end

:both
echo.
echo ========================================
echo   Building DEBUG + RELEASE APKs...
echo ========================================
echo.
call "%GRADLEW%" assembleDebug assembleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed.
    exit /b 1
)
echo.
echo [SUCCESS] Output APKs:
for /r "%OUTPUT_DIR%" %%f in (*.apk) do echo   %%f
goto :end

:clean
echo.
echo Cleaning build artifacts...
call "%GRADLEW%" clean --warning-mode=none -p "%PROJECT_DIR%"
echo [DONE] Clean complete.
goto :end

:usage
echo.
echo SnapReceipt Build Script
echo ========================
echo.
echo Usage: scripts\build.bat [command]
echo.
echo Commands:
echo   debug     Build debug APK
echo   release   Build release APK (requires signing config in local.properties)
echo   both      Build both debug and release APKs
echo   clean     Clean all build artifacts
echo.
echo Examples:
echo   scripts\build.bat debug
echo   scripts\build.bat release
echo   scripts\build.bat both
echo.

:end
endlocal
