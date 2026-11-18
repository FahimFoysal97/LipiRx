@echo off
set "VERSION="
for /f "tokens=2 delims=: " %%a in ('findstr /i "versionName" androidApp\module.yaml') do set "VERSION=%%a"
if defined VERSION (set "VERSION=%VERSION:"=%") else (set "VERSION=0.4")

set KEYSTORE=lipirx-release-key.jks
set ALIAS=lipirx-key

if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
        set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%"
    )
)

set "KEYTOOL_BIN="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" (
    set "KEYTOOL_BIN=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
) else if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" (
    set "KEYTOOL_BIN=%JAVA_HOME%\bin\keytool.exe"
) else (
    set "KEYTOOL_BIN=keytool"
)

if not exist "%KEYSTORE%" (
    echo ========================================================
    echo  Creating new release keystore: %KEYSTORE% ...
    echo ========================================================
    "%KEYTOOL_BIN%" -genkey -v -keystore "%KEYSTORE%" -keyalg RSA -keysize 2048 -validity 10000 -alias "%ALIAS%"
    if errorlevel 1 (
        if not exist "%KEYSTORE%" (
            echo ERROR: Keystore creation failed.
            exit /b 1
        )
    )
) else (
    echo ========================================================
    echo  Using existing release keystore: %KEYSTORE%
    echo ========================================================
)

echo.
echo ========================================================
echo  Building Release APK...
echo ========================================================
call .\build-apk.bat release
if errorlevel 1 exit /b 1

echo.
echo ========================================================
echo  Signing Release APK with %KEYSTORE% ...
echo ========================================================

set "APKSIGNER="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\build-tools" (
    for /f "delims=" %%i in ('where /r "%ANDROID_HOME%\build-tools" apksigner.bat 2^>nul') do set "APKSIGNER=%%i"
)
if "%APKSIGNER%"=="" if exist "%LOCALAPPDATA%\Android\Sdk\build-tools" (
    for /f "delims=" %%i in ('where /r "%LOCALAPPDATA%\Android\Sdk\build-tools" apksigner.bat 2^>nul') do set "APKSIGNER=%%i"
)

if "%APKSIGNER%"=="" (
    echo ERROR: Could not locate apksigner.bat in Android SDK.
    exit /b 1
)

call "%APKSIGNER%" sign --ks "%KEYSTORE%" --ks-key-alias "%ALIAS%" --out "apks\LipiRx-v%VERSION%-release-signed.apk" "apks\LipiRx-v%VERSION%-release.apk"

if errorlevel 0 (
    echo.
    echo ========================================================
    echo  SUCCESS: Signed Release APK created at:
    echo  apks\LipiRx-v%VERSION%-release-signed.apk
    echo ========================================================
)
