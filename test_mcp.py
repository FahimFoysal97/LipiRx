import json
import os
import subprocess
import sys
import time
import urllib.request
import urllib.error
from typing import Optional, Tuple

# Enable VT100 ANSI colors on Windows
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

MCP_URL = "http://localhost:8080/lipirx/mcp"
PACKAGE_NAME = "com.snehonir.lipirx"

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
    width = 64
    print(f"\n{Color.CYAN}{Color.BOLD}┌{'─' * (width - 2)}┐{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}│  {msg.center(width - 6)}  │{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}└{'─' * (width - 2)}┘{Color.RESET}\n")

def log_ok(msg: str) -> None:
    print(f"{Color.GREEN}{Color.BOLD}✔ [PASSED]{Color.RESET} {msg}")

def log_info(msg: str) -> None:
    print(f"{Color.CYAN}{Color.BOLD}ℹ [INFO]{Color.RESET} {msg}")

def log_warn(msg: str) -> None:
    print(f"{Color.YELLOW}{Color.BOLD}⚠ [WARN]{Color.RESET} {msg}")

def log_err(msg: str) -> None:
    print(f"{Color.RED}{Color.BOLD}✖ [FAILED]{Color.RESET} {msg}")

def run_adb(args: list) -> Tuple[int, str, str]:
    cmd = ["adb"] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.returncode, (res.stdout or "").strip(), (res.stderr or "").strip()

def setup_adb_forward() -> bool:
    log_info("Configuring ADB port forwarding (tcp:8080 -> tcp:8080)...")
    code, stdout, stderr = run_adb(["forward", "tcp:8080", "tcp:8080"])
    if code == 0:
        log_ok("ADB port 8080 forwarded successfully.")
        return True
    else:
        log_warn(f"ADB port forward warning: {stdout or stderr}")
        return False

def send_mcp_request(rpc_id: str, method: str, params: Optional[dict] = None) -> Optional[dict]:
    payload = {
        "jsonrpc": "2.0",
        "id": rpc_id,
        "method": method
    }
    if params is not None:
        payload["params"] = params

    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        MCP_URL,
        data=data,
        headers={"Content-Type": "application/json", "User-Agent": "LipiRx-MCP-Tester/1.0"}
    )

    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            body = response.read().decode("utf-8")
            return json.loads(body)
    except Exception:
        return None

def ensure_mcp_server_running() -> bool:
    print(f"\n{Color.BOLD}------------------------------------------------------------{Color.RESET}")
    print(f"{Color.CYAN}▶ Step 0: MCP Server Status Check & Automated UI Switch Toggle{Color.RESET}")

    setup_adb_forward()

    # 1. Quick probe to see if MCP Server is already ON and responding
    probe_resp = send_mcp_request("probe", "initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "LipiRxProbe", "version": "1.0"}
    })

    if probe_resp and "result" in probe_resp:
        log_ok("MCP Server is ALREADY enabled and running on port 8080.")
        return True

    log_warn("MCP Server is OFF or unreachable. Launching LipiRx Settings UI to toggle MCP Server switch ON...")

    # 2. Wake screen & launch Settings screen via intent
    run_adb(["shell", "input", "keyevent", "224"])
    run_adb(["shell", "input", "keyevent", "82"])
    time.sleep(0.5)
    run_adb(["shell", "am", "start", "-W", "-a", "android.intent.action.VIEW", "-d", "lipirx://feature/settings", PACKAGE_NAME])
    time.sleep(1.5)

    # 3. Scroll down to the MCP Server section
    run_adb(["shell", "input", "swipe", "600", "2000", "600", "600", "300"])
    time.sleep(1.0)

    # 4. Tap the MCP Server Switch toggle
    log_info("Toggling 'Local Server Endpoint' switch ON in Settings UI...")
    run_adb(["shell", "input", "tap", "1000", "1860"])
    time.sleep(1.5)

    # 5. Verify connection again after toggle
    verify_resp = send_mcp_request("probe_2", "initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "LipiRxProbe", "version": "1.0"}
    })

    if verify_resp and "result" in verify_resp:
        log_ok("MCP Server successfully enabled via Settings UI and running on port 8080!")
        return True
    else:
        log_err("Failed to start MCP Server via Settings UI toggle. Please verify LipiRx app status.")
        return False

def test_mcp_pipeline() -> None:
    log_title("LipiRx MCP Server Test Suite")

    if not ensure_mcp_server_running():
        log_err("Aborting MCP test suite because MCP Server is not active.")
        return

    tests = [
        ("1", "initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "LipiRxTester", "version": "1.0"}
        }, "Initialize MCP Session"),
        ("2", "tools/list", None, "List Available MCP Tools"),
        ("3", "resources/list", None, "List Available MCP Resources"),
        ("4", "resources/read", {"uri": "lipirx://config/providers"}, "Read Resource: AI Provider Configurations"),
        ("5", "resources/read", {"uri": "lipirx://history/traces"}, "Read Resource: AgentOps Execution Traces"),
        ("6", "tools/call", {
            "name": "parse_medical_entities",
            "arguments": {"itemsJson": json.dumps([{"medicineName": "Napa Extra 500mg", "schedule": "1+0+1", "units": 10}])}
        }, "Call Tool: Medical Entity Parser"),
        ("7", "tools/call", {
            "name": "search_generic_substitutes",
            "arguments": {"itemsJson": json.dumps([{"medicineName": "Napa Extra", "genericName": "Paracetamol"}])}
        }, "Call Tool: Search Generic Substitutes"),
        ("8", "tools/call", {
            "name": "audit_patient_safety",
            "arguments": {"itemsJson": json.dumps([{"medicineName": "Napa Extra", "strengthValue": "500mg", "schedule": "1+0+1"}])}
        }, "Call Tool: Audit Patient Safety")
    ]

    passed = 0
    total = len(tests)

    for req_id, method, params, label in tests:
        print(f"\n{Color.BOLD}------------------------------------------------------------{Color.RESET}")
        print(f"{Color.CYAN}▶ Testing [{req_id}/{total}]: {label}{Color.RESET} ({method})")
        resp = send_mcp_request(req_id, method, params)

        if resp and "result" in resp:
            log_ok(f"Received response for '{method}'")
            print(f"{Color.DIM}{json.dumps(resp['result'], indent=2)}{Color.RESET}")
            passed += 1
        elif resp and "error" in resp:
            log_err(f"RPC Error for '{method}': {resp['error']}")
        else:
            log_err(f"No valid response for '{method}'")

    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    if passed == total:
        print(f"{Color.GREEN}{Color.BOLD}✨ [SUMMARY] All {passed}/{total} MCP tests passed successfully!{Color.RESET}\n")
    else:
        print(f"{Color.YELLOW}{Color.BOLD}⚠ [SUMMARY] {passed}/{total} MCP tests passed. Ensure LipiRx app is running on port 8080.{Color.RESET}\n")

if __name__ == "__main__":
    test_mcp_pipeline()
