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
if defined VERSION (set "VERSION=%VERSION:"=%") else (set "VERSION=0.6.0")

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
echo %C_YELLOW%ℹ Creating new release keystore: %KEYSTORE% ...%C_RESET%
"%KEYTOOL_BIN%" -genkey -v -keystore "%KEYSTORE%" -keyalg RSA -keysize 2048 -validity 10000 -alias "%ALIAS%"
if errorlevel 1 (
if not exist "%KEYSTORE%" (
echo %C_RED%%C_BOLD%✖ ERROR: Keystore creation failed.%C_RESET%
exit /b 1
)
)
) else (
echo %C_GREEN%✔ Using existing release keystore: %KEYSTORE%%C_RESET%
)

echo.
set "ARG1=%~1"
if /i "%ARG1%"=="r" set "ARG1=release"
call .\build-apk.bat %ARG1%
if errorlevel 1 exit /b 1

echo.
echo %C_CYAN%ℹ Signing Release APK with %KEYSTORE% ...%C_RESET%

set "APKSIGNER_JAR="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\build-tools" (
for /f "delims=" %%i in ('where /r "%ANDROID_HOME%\build-tools" apksigner.jar 2^>nul') do set "APKSIGNER_JAR=%%i"
)
if "%APKSIGNER_JAR%"=="" if exist "%LOCALAPPDATA%\Android\Sdk\build-tools" (
for /f "delims=" %%i in ('where /r "%LOCALAPPDATA%\Android\Sdk\build-tools" apksigner.jar 2^>nul') do set "APKSIGNER_JAR=%%i"
)

if "%APKSIGNER_JAR%"=="" (
echo %C_RED%%C_BOLD%✖ ERROR: Could not locate apksigner.jar in Android SDK.%C_RESET%
exit /b 1
)

java --enable-native-access=ALL-UNNAMED -jar "%APKSIGNER_JAR%" sign --ks "%KEYSTORE%" --ks-key-alias "%ALIAS%" --out "apks\LipiRx-v%VERSION%-release-signed.apk" "apks\LipiRx-v%VERSION%-release.apk"

if %errorlevel% neq 0 goto sign_error
goto sign_success

:sign_error
echo.
echo %C_RED%%C_BOLD%✖ ERROR: APK signing failed (incorrect keystore password or invalid signature configuration).%C_RESET%
echo.
exit /b 1

:sign_success
echo.
echo %C_GREEN%%C_BOLD%✔ SUCCESS: Signed Release APK created at: apks\LipiRx-v%VERSION%-release-signed.apk%C_RESET%
echo.
