@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "GRADLEW=%PROJECT_DIR%\gradlew.bat"
set "OUTPUT_DIR=%PROJECT_DIR%\app\build\outputs\apk"
set "GRADLE_PROPS=%PROJECT_DIR%\gradle.properties"
set "COMMAND=%~1"
set "VERSION_BUMPED=0"
set "PREV_VERSION_CODE="
set "NEXT_VERSION_CODE="

if "%COMMAND%"=="" (
    echo.
    echo [INFO] No command specified. Defaulting to release build.
    set "COMMAND=release"
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
call "%GRADLEW%" assembleDebug --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Debug build failed.
    pause
    exit /b 1
)
echo.
echo [SUCCESS] Debug APK:
for /r "%OUTPUT_DIR%\debug" %%f in (*.apk) do echo   %%f
goto :end

:release
call :prepare_release_build
if errorlevel 1 (
    echo.
    echo [FAILED] Failed to prepare release build.
    pause
    exit /b 1
)
echo.
echo ========================================
echo   Building SnapReceipt RELEASE APK...
echo ========================================
echo.
call "%GRADLEW%" assembleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Release build failed. Check signing config in local.properties.
    call :rollbackVersionCode
    pause
    exit /b 1
)
echo.
echo [SUCCESS] Release APK:
for /r "%OUTPUT_DIR%\release" %%f in (*.apk) do echo   %%f
call :commit_and_push_version
if errorlevel 1 (
    echo.
    echo [FAILED] Release build succeeded, but version commit/push failed.
    pause
    exit /b 1
)
goto :end

:both
call :prepare_release_build
if errorlevel 1 (
    echo.
    echo [FAILED] Failed to prepare release build.
    pause
    exit /b 1
)
echo.
echo ========================================
echo   Building DEBUG + RELEASE APKs...
echo ========================================
echo.
call "%GRADLEW%" assembleDebug assembleRelease --warning-mode=none -p "%PROJECT_DIR%"
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed.
    call :rollbackVersionCode
    pause
    exit /b 1
)
echo.
echo [SUCCESS] Output APKs:
for /r "%OUTPUT_DIR%" %%f in (*.apk) do echo   %%f
call :commit_and_push_version
if errorlevel 1 (
    echo.
    echo [FAILED] Build succeeded, but version commit/push failed.
    pause
    exit /b 1
)
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
echo   release   Bump versionCode +1, build release APK, then commit/push gradle.properties
echo   both      Bump versionCode +1, build both APKs, then commit/push gradle.properties
echo   clean     Clean all build artifacts
echo.
echo Examples:
echo   scripts\build.bat debug
echo   scripts\build.bat release
echo   scripts\build.bat both
echo.

:prepare_release_build
call :bump_version_code
exit /b %errorlevel%

:bump_version_code
if not exist "%GRADLE_PROPS%" (
    echo.
    echo [FAILED] gradle.properties not found: %GRADLE_PROPS%
    exit /b 1
)
for /f "tokens=1,2 delims==" %%A in ('findstr /b /c:"app.versionCode=" "%GRADLE_PROPS%"') do (
    set "PREV_VERSION_CODE=%%B"
)
if not defined PREV_VERSION_CODE (
    echo.
    echo [FAILED] app.versionCode not found in gradle.properties.
    exit /b 1
)
set /a NEXT_VERSION_CODE=PREV_VERSION_CODE+1 2>nul
if errorlevel 1 (
    echo.
    echo [FAILED] app.versionCode is not numeric: %PREV_VERSION_CODE%
    exit /b 1
)
call :update_version_code "%NEXT_VERSION_CODE%"
if errorlevel 1 (
    echo.
    echo [FAILED] Unable to update app.versionCode in gradle.properties.
    exit /b 1
)
set "VERSION_BUMPED=1"
echo [INFO] app.versionCode bumped: %PREV_VERSION_CODE% -^> %NEXT_VERSION_CODE%
exit /b 0

:rollbackVersionCode
if not "%VERSION_BUMPED%"=="1" exit /b 0
if not defined PREV_VERSION_CODE exit /b 0
echo [WARN] Rolling back app.versionCode to %PREV_VERSION_CODE%...
call :update_version_code "%PREV_VERSION_CODE%"
if errorlevel 1 (
    echo [WARN] Rollback failed. Please restore gradle.properties manually.
    exit /b 1
)
echo [INFO] Rollback completed.
exit /b 0

:update_version_code
set "TARGET_VERSION_CODE=%~1"
if "%TARGET_VERSION_CODE%"=="" exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "$path = '%GRADLE_PROPS%'; $target = '%TARGET_VERSION_CODE%'; $text = [System.IO.File]::ReadAllText($path); $updated = [System.Text.RegularExpressions.Regex]::Replace($text, '(?m)^app\.versionCode=.*$', ('app.versionCode=' + $target), 1); if ($updated -eq $text) { exit 2 }; $enc = New-Object System.Text.UTF8Encoding($false); [System.IO.File]::WriteAllText($path, $updated, $enc)"
exit /b %errorlevel%

:commit_and_push_version
if not "%VERSION_BUMPED%"=="1" (
    echo [INFO] Version bump was not applied. Skip commit/push.
    exit /b 0
)
where git >nul 2>nul
if errorlevel 1 (
    echo [FAILED] Git not found in PATH.
    exit /b 1
)
pushd "%PROJECT_DIR%" >nul
git add "gradle.properties"
git diff --cached --quiet -- "gradle.properties"
if errorlevel 2 (
    echo [FAILED] Unable to inspect staged changes.
    popd >nul
    exit /b 1
)
if not errorlevel 1 (
    echo [INFO] No gradle.properties changes to commit.
    popd >nul
    exit /b 0
)
for /f "delims=" %%B in ('git rev-parse --abbrev-ref HEAD 2^>nul') do (
    set "CURRENT_BRANCH=%%B"
)
if not defined CURRENT_BRANCH (
    echo [FAILED] Unable to resolve current git branch.
    popd >nul
    exit /b 1
)
git commit -m "chore: bump app.versionCode to %NEXT_VERSION_CODE%" -- "gradle.properties"
if errorlevel 1 (
    echo [FAILED] Git commit failed.
    popd >nul
    exit /b 1
)
git rev-parse --abbrev-ref --symbolic-full-name @{u} >nul 2>nul
if errorlevel 1 (
    git push -u origin "%CURRENT_BRANCH%"
) else (
    git push
)
if errorlevel 1 (
    echo [FAILED] Git push failed.
    popd >nul
    exit /b 1
)
echo [SUCCESS] Version bump committed and pushed on branch %CURRENT_BRANCH%.
popd >nul
exit /b 0

:end
endlocal
