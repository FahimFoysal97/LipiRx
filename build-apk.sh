#!/bin/bash

# Automatically extract versionName from androidApp/module.yaml
VERSION=$(grep -i "versionName" androidApp/module.yaml | head -n 1 | awk -F ':' '{print $2}' | tr -d ' "' | tr -d "'")

if [ -z "$VERSION" ]; then
    VERSION="0.1"
fi

if [ "$1" == "release" ]; then
    echo "========================================================"
    echo " Building LipiRx Release APK (v${VERSION})..."
    echo "========================================================"
    ./kotlin task :androidApp:buildAndroidRelease || exit 1
    mkdir -p apks
    cp build/tasks/_androidApp_buildAndroidRelease/gradle-project-release-unsigned.apk apks/LipiRx-v${VERSION}-release.apk
    echo ""
    echo "========================================================"
    echo " SUCCESS: Release APK created at: apks/LipiRx-v${VERSION}-release.apk"
    echo "========================================================"
else
    echo "========================================================"
    echo " Building LipiRx Debug APK (v${VERSION})..."
    echo "========================================================"
    ./kotlin task :androidApp:buildAndroidDebug || exit 1
    mkdir -p apks
    cp build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk apks/LipiRx-v${VERSION}-debug.apk
    echo ""
    echo "========================================================"
    echo " SUCCESS: Debug APK created at: apks/LipiRx-v${VERSION}-debug.apk"
    echo "========================================================"
fi
