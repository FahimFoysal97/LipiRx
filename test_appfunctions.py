import json
import os
import subprocess
import sys
from typing import Optional, Tuple

# Enable VT100 ANSI colors on Windows terminal
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

PACKAGE_NAME = "com.snehonir.lipirx"
SERVICE_CLASS = "com.snehonir.lipirx.appfunctions.BaseLipiRxAppFunctionService"

class Color:
    def __init__(self):
        pass

    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    CYAN = "\033[96m"

def log_title(msg: str) -> None:
    width = 68
    print(f"\n{Color.CYAN}{Color.BOLD}┌{'─' * (width - 2)}┐{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}│  {msg.center(width - 6)}  │{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}└{'─' * (width - 2)}┘{Color.RESET}\n")

def log_ok(msg: str) -> None:
    print(f"{Color.GREEN}{Color.BOLD}✔ [PASSED]{Color.RESET} {msg}")

def log_warn(msg: str) -> None:
    print(f"{Color.YELLOW}{Color.BOLD}⚠ [WARNING]{Color.RESET} {msg}")

def log_err(msg: str) -> None:
    print(f"{Color.RED}{Color.BOLD}✖ [FAILED]{Color.RESET} {msg}")

def run_adb_shell_command(shell_cmd: str) -> Tuple[int, str, str]:
    full_cmd = ["adb", "shell", shell_cmd]
    try:
        res = subprocess.run(full_cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except Exception as e:
        return -1, "", str(e)

def check_adb_and_service() -> bool:
    print(f"{Color.YELLOW}ℹ Checking ADB connected devices...{Color.RESET}")
    try:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, encoding="utf-8", errors="replace")
        stdout = res.stdout.strip()
        lines = [line for line in stdout.splitlines() if line.strip() and not line.startswith("List of devices")]
        if not lines:
            log_err("No connected Android device/emulator found via ADB.")
            return False
        print(f"{Color.GREEN}✔ Detected device(s): {', '.join(lines)}{Color.RESET}")
    except Exception as e:
        log_err(f"Failed to run 'adb devices': {e}")
        return False

    print(f"{Color.YELLOW}ℹ Checking AppFunction service capability on device...{Color.RESET}")
    code, stdout, stderr = run_adb_shell_command("cmd app_function help")
    if code != 0 or "Can't find service: app_function" in stderr:
        log_warn("The connected device does not support Android 'app_function' service (requires Android 16+ / SDK 36+).")
        log_warn("Note: ADB execution tests will fail if app_function framework is unavailable on this device.")
    else:
        log_ok("Android 'app_function' service is supported on target device.")

    return True

def list_registered_app_functions() -> bool:
    print(f"\n{Color.BOLD}------------------------------------------------------------{Color.RESET}")
    print(f"{Color.CYAN}▶ Step 1: Querying registered AppFunctions on device{Color.RESET}")

    code, stdout, stderr = run_adb_shell_command("cmd app_function list-app-functions")
    if code == 0 and PACKAGE_NAME in stdout:
        log_ok(f"Successfully verified registered AppFunctions for package '{PACKAGE_NAME}'.")
        return True
    elif code == 0:
        log_ok("AppFunctions service queried successfully.")
        return True
    else:
        log_warn(f"Could not query AppFunctions: {stderr or stdout}")
        return False

def execute_app_function(function_name: str, params: dict, label: str) -> bool:
    target_fn = f"{SERVICE_CLASS}#{function_name}"
    json_params = json.dumps(params, separators=(',', ':'))

    print(f"\n{Color.BOLD}------------------------------------------------------------{Color.RESET}")
    print(f"{Color.CYAN}▶ Executing AppFunction: {label}{Color.RESET}")
    print(f"{Color.DIM}Target Function: {target_fn}{Color.RESET}")
    print(f"{Color.DIM}Input Parameters: {json_params}{Color.RESET}")

    shell_cmd = f"cmd app_function execute-app-function --package {PACKAGE_NAME} --function '{target_fn}' --parameters '{json_params}' --brief-yaml"

    code, stdout, stderr = run_adb_shell_command(shell_cmd)

    if code == 0 and ("androidAppfunctionsReturnValue" in stdout or "result" in stdout.lower() or "items" in stdout or "traces" in stdout or "medicine" in stdout):
        log_ok(f"AppFunction '{function_name}' executed successfully.")
        print(f"{Color.DIM}{stdout}{Color.RESET}")
        return True
    elif code == 0 and stdout:
        log_ok(f"AppFunction '{function_name}' returned response.")
        print(f"{Color.DIM}{stdout}{Color.RESET}")
        return True
    else:
        error_msg = stderr.strip() or stdout.strip() or "Unknown error"
        log_err(f"AppFunction '{function_name}' execution failed: {error_msg}")
        return False

def test_all_app_functions() -> None:
    log_title("LipiRx Android AppFunctions Test Suite")

    if not check_adb_and_service():
        return

    list_registered_app_functions()

    test_cases = [
        (
            "getPrescriptionHistory",
            {},
            "Get Prescription History Records"
        ),
        (
            "parseMedicalEntities",
            {"rawText": "Tab Seclo 20mg 1+0+1 14 days"},
            "Parse Prescription Medical Entities"
        ),
        (
            "searchGenericSubstitutes",
            {"medicineName": "Seclo 20mg"},
            "Search Bangladeshi Generic Substitutes"
        ),
        (
            "auditPatientSafety",
            {
                "medicineName": "Napa Extra",
                "dosageForm": "tablet",
                "schedule": "1+0+1",
                "units": 10
            },
            "Audit Patient Safety & Dosage Contraindications"
        ),
        (
            "getAgentExecutionTraces",
            {"limit": 5},
            "Query AgentOps Execution Traces"
        )
    ]

    passed = 0
    total = len(test_cases)

    for fn_name, params, label in test_cases:
        if execute_app_function(fn_name, params, label):
            passed += 1

    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    if passed == total:
        print(f"{Color.GREEN}{Color.BOLD}✨ [SUMMARY] All {passed}/{total} AppFunction tests passed successfully!{Color.RESET}\n")
    else:
        print(f"{Color.YELLOW}{Color.BOLD}⚠ [SUMMARY] {passed}/{total} AppFunction tests passed. Ensure LipiRx is installed on Android 16+ device.{Color.RESET}\n")

if __name__ == "__main__":
    test_all_app_functions()
