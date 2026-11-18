#!/bin/bash
VERSION=$(grep -i "versionName" androidApp/module.yaml | head -n 1 | awk -F ':' '{print $2}' | tr -d ' "' | tr -d "'")
if [ -z "$VERSION" ]; then VERSION="0.4"; fi

KEYSTORE="lipirx-release-key.jks"
ALIAS="lipirx-key"

KEYTOOL="keytool"
if ! command -v keytool &> /dev/null; then
    if [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/keytool" ]; then
        KEYTOOL="$JAVA_HOME/bin/keytool"
    fi
fi

if [ ! -f "$KEYSTORE" ]; then
    echo "========================================================"
    echo " Creating new release keystore: $KEYSTORE ..."
    echo "========================================================"
    "$KEYTOOL" -genkey -v -keystore $KEYSTORE -keyalg RSA -keysize 2048 -validity 10000 -alias $ALIAS
else
    echo "========================================================"
    echo " Using existing release keystore: $KEYSTORE"
    echo "========================================================"
fi

echo "========================================================"
echo " Building Release APK..."
echo "========================================================"
./build-apk.sh release || exit 1

APKSIGNER=""
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
    APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name "apksigner" 2>/dev/null | head -n 1)
fi

if [ -z "$APKSIGNER" ] && [ -d "$HOME/Library/Android/sdk/build-tools" ]; then
    APKSIGNER=$(find "$HOME/Library/Android/sdk/build-tools" -name "apksigner" 2>/dev/null | head -n 1)
fi

if [ -z "$APKSIGNER" ]; then
    APKSIGNER=$(command -v apksigner)
fi

if [ -z "$APKSIGNER" ]; then
    echo "ERROR: Could not locate apksigner."
    exit 1
fi

"$APKSIGNER" sign --ks $KEYSTORE --ks-key-alias $ALIAS --out "apks/LipiRx-v${VERSION}-release-signed.apk" "apks/LipiRx-v${VERSION}-release.apk"

echo "========================================================"
echo " SUCCESS: Signed Release APK created at: apks/LipiRx-v${VERSION}-release-signed.apk"
echo "========================================================"
