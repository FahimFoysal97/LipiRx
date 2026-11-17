#!/bin/bash
VERSION="0.3"

if [ "$1" == "release" ]; then
    echo "========================================================"
    echo " Building LipiRx Release APK..."
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
    echo " Building LipiRx Debug APK..."
    echo "========================================================"
    ./kotlin task :androidApp:buildAndroidDebug || exit 1
    mkdir -p apks
    cp build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk apks/LipiRx-v${VERSION}-debug.apk
    echo ""
    echo "========================================================"
    echo " SUCCESS: Debug APK created at: apks/LipiRx-v${VERSION}-debug.apk"
    echo "========================================================"
fi
