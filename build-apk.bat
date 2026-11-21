@echo off
chcp 65001 >nul

for /f "delims=" %%a in ('powershell -NoProfile -Command "[char]27"') do set "ESC=%%a"

set "C_CYAN=%ESC%[96m"
set "C_GREEN=%ESC%[92m"
set "C_YELLOW=%ESC%[93m"
set "C_RED=%ESC%[91m"
set "C_BOLD=%ESC%[1m"
set "C_RESET=%ESC%[0m"

set "VERSION="
for /f "tokens=2 delims=: " %%a in ('findstr /i "versionName" androidApp\module.yaml') do set "VERSION=%%a"
if defined VERSION (
    set "VERSION=%VERSION:"=%"
) else (
    set "VERSION=0.5.0"
)

set "MODE=%~1"

if /i "%MODE%"=="release" goto do_release
if /i "%MODE%"=="r" goto do_release

goto do_debug

:do_release
echo.
echo %C_CYAN%========================================================%C_RESET%
echo %C_CYAN%  %C_BOLD%%C_CYAN%LipiRx APK Build Engine%C_RESET%
echo %C_CYAN%  Target  : %C_BOLD%%C_YELLOW%Release APK%C_RESET% (v%VERSION%)
echo %C_CYAN%========================================================%C_RESET%
echo.
call .\kotlin.bat task :androidApp:buildAndroidRelease
if errorlevel 1 goto error
if not exist "apks" mkdir apks
copy /Y "build\tasks\_androidApp_buildAndroidRelease\gradle-project-release-unsigned.apk" "apks\LipiRx-v%VERSION%-release.apk" >nul
echo.
echo %C_GREEN%%C_BOLD%✔ SUCCESS: Release APK created at: apks\LipiRx-v%VERSION%-release.apk%C_RESET%
echo.
goto end

:do_debug
echo.
echo %C_CYAN%========================================================%C_RESET%
echo %C_CYAN%  %C_BOLD%%C_CYAN%LipiRx APK Build Engine%C_RESET%
echo %C_CYAN%  Target  : %C_BOLD%%C_CYAN%Debug APK%C_RESET% (v%VERSION%)
echo %C_CYAN%========================================================%C_RESET%
echo.
call .\kotlin.bat task :androidApp:buildAndroidDebug
if errorlevel 1 goto error
if not exist "apks" mkdir apks
copy /Y "build\tasks\_androidApp_buildAndroidDebug\gradle-project-debug.apk" "apks\LipiRx-v%VERSION%-debug.apk" >nul
echo.
echo %C_GREEN%%C_BOLD%✔ SUCCESS: Debug APK created at: apks\LipiRx-v%VERSION%-debug.apk%C_RESET%
echo.
goto end

:error
echo.
echo %C_RED%%C_BOLD%✖ ERROR: Build failed.%C_RESET%
echo.
exit /b 1

:end
