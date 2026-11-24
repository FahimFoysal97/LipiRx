#!/usr/bin/env python3
"""
LipiRx Build, Sign, Install & Launch Tool

Workflow:
1. Parse current version from androidApp/module.yaml.
2. Build and sign release APK via sign-release.bat (or sign-release.sh).
3. Install the generated signed release APK onto connected Android device via ADB.
4. Set up port forwarding (tcp:8080 -> tcp:8080) for embedded MCP server.
5. Launch com.snehonir.lipirx/.MainActivity on the device.
"""

import os
import sys
import re
import subprocess
from pathlib import Path

class Color:
    def __init__(self):
        pass

    CYAN = "\033[1;36m"
    GREEN = "\033[1;32m"
    YELLOW = "\033[1;33m"
    RED = "\033[1;31m"
    BOLD = "\033[1m"
    RESET = "\033[0m"

def log_info(msg: str):
    print(f"{Color.CYAN}ℹ {msg}{Color.RESET}")

def log_ok(msg: str):
    print(f"{Color.GREEN}✔ {msg}{Color.RESET}")

def log_warn(msg: str):
    print(f"{Color.YELLOW}⚠ {msg}{Color.RESET}")

def log_err(msg: str):
    print(f"{Color.RED}✖ {msg}{Color.RESET}")

def get_app_version() -> str:
    yaml_path = Path("androidApp/module.yaml")
    if yaml_path.exists():
        text = yaml_path.read_text(encoding="utf-8")
        match = re.search(r'versionName:\s*["\']?([^"\']+)["\']?', text)
        if match:
            return match.group(1).strip()
    return "0.6.1"

def run_cmd(cmd: list, check: bool = True) -> int:
    log_info(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, shell=(os.name == "nt"))
    if check and result.returncode != 0:
        log_err(f"Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)
    return result.returncode

def main():
    print(f"\n{Color.CYAN}{Color.BOLD}┌──────────────────────────────────────────────────────────────┐{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}│         LipiRx Build, Sign, Install & Launch Engine          │{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}└──────────────────────────────────────────────────────────────┘{Color.RESET}\n")

    root_dir = Path(__file__).parent.resolve()
    os.chdir(root_dir)

    version = get_app_version()
    log_info(f"Detected LipiRx version: v{version}")

    # 1. Run sign-release script
    if os.name == "nt":
        script_cmd = [".\\sign-release.bat", "release"]
    else:
        script_cmd = ["./sign-release.sh", "release"]

    log_info("Building and signing release APK...")
    run_cmd(script_cmd, check=True)

    # 2. Locate generated signed release APK
    apk_candidates = [
        root_dir / "apks" / f"LipiRx-v{version}-release-signed.apk",
        root_dir / "apks" / f"LipiRx-v{version}-release.apk",
    ]

    target_apk = None
    for candidate in apk_candidates:
        if candidate.exists():
            target_apk = candidate
            break

    if not target_apk:
        apks_dir = root_dir / "apks"
        if apks_dir.exists():
            signed_apks = list(apks_dir.glob("*release-signed.apk"))
            if signed_apks:
                target_apk = sorted(signed_apks, key=lambda p: p.stat().st_mtime, reverse=True)[0]

    if not target_apk or not target_apk.exists():
        log_err("Could not find generated signed release APK in apks/ directory!")
        sys.exit(1)

    log_ok(f"Found signed release APK: {target_apk.name}")

    # 3. Install APK via ADB
    log_info(f"Installing {target_apk.name} onto connected device...")
    adb_cmd = ["adb", "install", "-r", str(target_apk)]
    run_cmd(adb_cmd, check=True)
    log_ok("APK installed successfully.")

    # 4. Port Forwarding for MCP Server
    log_info("Configuring ADB port forwarding for MCP server (tcp:8080 -> tcp:8080)...")
    subprocess.run(["adb", "forward", "tcp:8080", "tcp:8080"], check=False)

    # 5. Launch Main Activity
    log_info("Starting com.snehonir.lipirx/.MainActivity...")
    launch_cmd = ["adb", "shell", "am", "start", "-n", "com.snehonir.lipirx/.MainActivity"]
    run_cmd(launch_cmd, check=False)

    print(f"\n{Color.GREEN}{Color.BOLD}✨ [SUCCESS] LipiRx v{version} built, signed, installed, and launched!{Color.RESET}\n")

if __name__ == "__main__":
    main()
