@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_DIR=%%~fI"
set "GRADLEW=%PROJECT_DIR%\gradlew.bat"
set "APK_OUTPUT_DIR=%PROJECT_DIR%\app\build\outputs\apk"
set "BUNDLE_OUTPUT_DIR=%PROJECT_DIR%\app\build\outputs\bundle"
set "GRADLE_PROPS=%PROJECT_DIR%\gradle.properties"
set "COMMAND=%~1"

if "%COMMAND%"=="" (
    echo.
    echo [INFO] No command specified. Defaulting to release build.
    set "COMMAND=release"
)

if not exist "%GRADLEW%" (
    echo.
    echo [FAILED] gradlew.bat not found: %GRADLEW%
    exit /b 1
)

if /i "%COMMAND%"=="debug" goto :debug
if /i "%COMMAND%"=="release" goto :release
if /i "%COMMAND%"=="both" goto :both
if /i "%COMMAND%"=="clean" goto :clean
goto :usage

:debug
echo.
echo ========================================
echo   Building SnapReceipt DEBUG APK...
echo ========================================
echo.
call "%GRADLEW%" --console=plain assembleDebug --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Debug build failed.
    exit /b 1
)
echo.
echo [SUCCESS] Debug APK:
for /r "%APK_OUTPUT_DIR%\debug" %%f in (*.apk) do echo   %%f
goto :end

:release
call :load_app_version
if errorlevel 1 (
    echo.
    echo [FAILED] Unable to read app version from gradle.properties.
    exit /b 1
)
echo.
echo ========================================
echo   Building SnapReceipt RELEASE APK + AAB...
echo ========================================
echo.
call "%GRADLEW%" --console=plain assembleRelease bundleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Release build failed. Check signing config in local.properties.
    exit /b 1
)
echo.
call :print_release_outputs
goto :end

:both
call :load_app_version
if errorlevel 1 (
    echo.
    echo [FAILED] Unable to read app version from gradle.properties.
    exit /b 1
)
echo.
echo ========================================
echo   Building DEBUG APK + RELEASE APK/AAB...
echo ========================================
echo.
call "%GRADLEW%" --console=plain assembleDebug assembleRelease bundleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed.
    exit /b 1
)
echo.
echo [SUCCESS] Debug APK:
for /r "%APK_OUTPUT_DIR%\debug" %%f in (*.apk) do echo   %%f
call :print_release_outputs
goto :end

:clean
echo.
echo Cleaning build artifacts...
call "%GRADLEW%" --console=plain clean --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo [FAILED] Clean failed.
    exit /b 1
)
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
echo   release   Build signed release APK + AAB
echo   both      Build debug APK + signed release APK + AAB
echo   clean     Clean all build artifacts
echo.
echo Examples:
echo   scripts\build.bat debug
echo   scripts\build.bat release
echo   scripts\build.bat both
echo.
exit /b 1

:load_app_version
if not exist "%GRADLE_PROPS%" (
    echo.
    echo [FAILED] gradle.properties not found: %GRADLE_PROPS%
    exit /b 1
)
set "APP_VERSION_NAME="
set "APP_VERSION_CODE="
set "RELEASE_APK="
set "RELEASE_AAB="
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"app.versionName=" "%GRADLE_PROPS%"') do (
    set "APP_VERSION_NAME=%%B"
)
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"app.versionCode=" "%GRADLE_PROPS%"') do (
    set "APP_VERSION_CODE=%%B"
)
if not defined APP_VERSION_NAME (
    echo.
    echo [FAILED] app.versionName not found in gradle.properties.
    exit /b 1
)
if not defined APP_VERSION_CODE (
    echo.
    echo [FAILED] app.versionCode not found in gradle.properties.
    exit /b 1
)
set "RELEASE_APK=%APK_OUTPUT_DIR%\release\SnapReceipt-release-v%APP_VERSION_NAME%-%APP_VERSION_CODE%.apk"
set "RELEASE_AAB=%BUNDLE_OUTPUT_DIR%\release\SnapReceipt-release-v%APP_VERSION_NAME%-%APP_VERSION_CODE%.aab"
exit /b 0

:print_release_outputs
echo [SUCCESS] Release artifacts:
if exist "%RELEASE_APK%" (
    echo   %RELEASE_APK%
) else (
    echo   [WARN] APK not found at expected path: %RELEASE_APK%
)
if exist "%RELEASE_AAB%" (
    echo   %RELEASE_AAB%
) else (
    echo   [WARN] AAB not found at expected path: %RELEASE_AAB%
)
if exist "%PROJECT_DIR%\app\build\outputs\mapping\release\mapping.txt" (
    echo   %PROJECT_DIR%\app\build\outputs\mapping\release\mapping.txt
)
exit /b 0

:end
endlocal
