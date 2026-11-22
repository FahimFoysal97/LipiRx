#!/bin/bash

# ANSI Color Codes
CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
BOLD='\033[1m'
NC='\033[0m'

VERSION=$(grep -i "versionName" androidApp/module.yaml | head -n 1 | awk -F ':' '{print $2}' | tr -d ' "' | tr -d "'")
if [ -z "$VERSION" ]; then
    VERSION="0.5.1"
fi

BUILD_TYPE="debug"
if [ "$1" == "release" ] || [ "$1" == "r" ]; then
    BUILD_TYPE="release"
fi

if [ "$BUILD_TYPE" == "release" ]; then
    echo -e "\n${CYAN}┌──────────────────────────────────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC} ${BOLD}${CYAN}LipiRx APK Build Engine${NC}                                 ${CYAN}│${NC}"
    echo -e "${CYAN}│${NC} Target  : ${BOLD}${YELLOW}Release APK${NC} (v${VERSION})                         ${CYAN}│${NC}"
    echo -e "${CYAN}└──────────────────────────────────────────────────────────┘${NC}\n"
    ./kotlin task :androidApp:buildAndroidRelease || exit 1
    mkdir -p apks
    cp build/tasks/_androidApp_buildAndroidRelease/gradle-project-release-unsigned.apk apks/LipiRx-v"${VERSION}"-release.apk
    echo -e "\n${GREEN}${BOLD}✔ SUCCESS: Release APK created at: apks/LipiRx-v${VERSION}-release.apk${NC}\n"
else
    echo -e "\n${CYAN}┌──────────────────────────────────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC} ${BOLD}${CYAN}LipiRx APK Build Engine${NC}                                 ${CYAN}│${NC}"
    echo -e "${CYAN}│${NC} Target  : ${BOLD}${CYAN}Debug APK${NC} (v${VERSION})                           ${CYAN}│${NC}"
    echo -e "${CYAN}└──────────────────────────────────────────────────────────┘${NC}\n"
    ./kotlin task :androidApp:buildAndroidDebug || exit 1
    mkdir -p apks
    cp build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk apks/LipiRx-v"${VERSION}"-debug.apk
    echo -e "\n${GREEN}${BOLD}✔ SUCCESS: Debug APK created at: apks/LipiRx-v${VERSION}-debug.apk${NC}\n"
fi
