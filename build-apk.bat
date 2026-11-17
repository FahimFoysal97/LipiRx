@echo off
set "VERSION="

for /f "tokens=2 delims=: " %%a in ('findstr /i "versionName" androidApp\module.yaml') do (
    set "VERSION=%%a"
)

if defined VERSION (
    set "VERSION=%VERSION:"=%"
) else (
    set "VERSION=0.1"
)

if /i "%1"=="release" (
    echo ========================================================
    echo  Building LipiRx Release APK ^(v%VERSION%^)...
    echo ========================================================
    call .\kotlin.bat task :androidApp:buildAndroidRelease
    if errorlevel 1 goto error
    if not exist "apks" mkdir apks
    copy /Y "build\tasks\_androidApp_buildAndroidRelease\gradle-project-release-unsigned.apk" "apks\LipiRx-v%VERSION%-release.apk" >nul
    echo.
    echo ========================================================
    echo  SUCCESS: Release APK created at: apks\LipiRx-v%VERSION%-release.apk
    echo ========================================================
    goto end
)

echo ========================================================
echo  Building LipiRx Debug APK ^(v%VERSION%^)...
echo ========================================================
call .\kotlin.bat task :androidApp:buildAndroidDebug
if errorlevel 1 goto error
if not exist "apks" mkdir apks
copy /Y "build\tasks\_androidApp_buildAndroidDebug\gradle-project-debug.apk" "apks\LipiRx-v%VERSION%-debug.apk" >nul
echo.
echo ========================================================
echo  SUCCESS: Debug APK created at: apks\LipiRx-v%VERSION%-debug.apk
echo ========================================================
goto end

:error
echo.
echo ERROR: Build failed.
exit /b 1

:end
