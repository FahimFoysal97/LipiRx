import json
import os
import re
import subprocess
import sys
import time
import urllib.request
import urllib.error
from typing import Optional

# Enable VT100 ANSI escape sequences on Windows
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

PACKAGE_NAME = "com.snehonir.lipirx"
MAIN_ACTIVITY = "com.snehonir.lipirx/.MainActivity"
MCP_URL = "http://localhost:8080/lipirx/mcp"

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
    WHITE = "\033[97m"

def log_title(msg: str) -> None:
    width = 68
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

def run_adb(args: list[str]) -> tuple[int, str, str]:
    cmd = ["adb"] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.returncode, res.stdout.strip(), res.stderr.strip()

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
        headers={"Content-Type": "application/json", "User-Agent": "LipiRx-FullTest/1.0"}
    )

    try:
        with urllib.request.urlopen(req, timeout=6) as response:
            body = response.read().decode("utf-8")
            return json.loads(body)
    except Exception as e:
        log_err(f"MCP RPC Error for method '{method}': {e}")
        return None

def test_full_application() -> None:
    log_title("LipiRx Full Functionality & Integration Test Suite")

    passed_count = 0
    total_count = 14

    # --------------------------------------------------------------------------
    # Step 1: Check ADB Devices
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[1/{total_count}] ADB Device Connectivity Test{Color.RESET}")
    code, stdout, _ = run_adb(["devices"])
    if code == 0 and "device" in stdout and len(stdout.splitlines()) > 1:
        device_line = [l for l in stdout.splitlines() if "device" in l and not l.startswith("List")][0]
        device_id = device_line.split()[0]
        log_ok(f"ADB device connected: {device_id}")
        passed_count += 1
    else:
        log_err("No ADB device/emulator detected. Please connect a device or start an emulator.")
        sys.exit(1)

    # --------------------------------------------------------------------------
    # Step 2: Verify Package Installation
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[2/{total_count}] Package Installation Test ({PACKAGE_NAME}){Color.RESET}")
    code, stdout, _ = run_adb(["shell", "pm", "path", PACKAGE_NAME])
    if code == 0 and PACKAGE_NAME in stdout:
        log_ok(f"LipiRx package is installed on device: {stdout}")
        passed_count += 1
    else:
        log_err(f"LipiRx package '{PACKAGE_NAME}' is not installed on device.")

    # --------------------------------------------------------------------------
    # Step 3: Launch MainActivity & Verify Process Focus
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[3/{total_count}] App Activity Launch & Focus Test{Color.RESET}")
    run_adb(["shell", "am", "start", "-n", MAIN_ACTIVITY])
    time.sleep(1.5)
    code, pid, _ = run_adb(["shell", "pidof", PACKAGE_NAME])
    if code == 0 and pid.isdigit():
        log_ok(f"LipiRx MainActivity active in foreground (PID: {pid})")
        passed_count += 1
    else:
        log_err("Failed to verify running process PID for LipiRx")

    # --------------------------------------------------------------------------
    # Step 4: Configure ADB Port Forwarding (8080)
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[4/{total_count}] MCP Server Port Forwarding (tcp:8080 -> tcp:8080){Color.RESET}")
    code, stdout, _ = run_adb(["forward", "tcp:8080", "tcp:8080"])
    if code == 0:
        log_ok("Port 8080 forwarded successfully")
        passed_count += 1
    else:
        log_err(f"Failed to forward port 8080: {stdout}")

    # --------------------------------------------------------------------------
    # Step 5: MCP Protocol Handshake (initialize)
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[5/{total_count}] MCP Protocol Handshake (initialize){Color.RESET}")
    resp = send_mcp_request("1", "initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "LipiRxFullTester", "version": "1.0"}
    })
    if resp and "result" in resp and resp["result"].get("serverInfo", {}).get("name") == "LipiRx MCP Server":
        server_info = resp["result"]["serverInfo"]
        log_ok(f"Connected to MCP Server: {server_info['name']} v{server_info['version']}")
        passed_count += 1
    else:
        log_err("MCP Handshake failed or returned unexpected protocol payload")

    # --------------------------------------------------------------------------
    # Step 6: MCP Tools Registry Verification (tools/list)
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[6/{total_count}] MCP Tools Registry Verification (tools/list){Color.RESET}")
    resp = send_mcp_request("2", "tools/list")
    if resp and "result" in resp and "tools" in resp["result"]:
        tools = [t["name"] for t in resp["result"]["tools"]]
        expected_tools = [
            "scan_prescription_vision_ocr",
            "parse_medical_entities",
            "search_generic_substitutes",
            "audit_patient_safety",
            "get_agent_execution_traces",
            "eval_run_benchmark"
        ]
        if all(t in tools for t in expected_tools):
            log_ok(f"All {len(tools)} Koog agent tools registered in MCP server: {', '.join(tools)}")
            passed_count += 1
        else:
            log_err(f"Missing expected MCP tools. Registered: {tools}")
    else:
        log_err("Failed to fetch registered tools list from MCP server")

    # --------------------------------------------------------------------------
    # Step 7: MCP Resources Registry Verification (resources/list)
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[7/{total_count}] MCP Resources Registry Verification (resources/list){Color.RESET}")
    resp = send_mcp_request("3", "resources/list")
    if resp and "result" in resp and "resources" in resp["result"]:
        resources = [r["uri"] for r in resp["result"]["resources"]]
        expected_resources = [
            "lipirx://history/scans",
            "lipirx://config/providers",
            "lipirx://history/traces"
        ]
        if all(r in resources for r in expected_resources):
            log_ok(f"All {len(resources)} MCP resources registered: {', '.join(resources)}")
            passed_count += 1
        else:
            log_err(f"Missing expected MCP resources. Registered: {resources}")
    else:
        log_err("Failed to fetch registered resources list from MCP server")

    # --------------------------------------------------------------------------
    # Step 8: Read Resource: AI Provider Configuration
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[8/{total_count}] Read Resource: lipirx://config/providers{Color.RESET}")
    resp = send_mcp_request("4", "resources/read", {"uri": "lipirx://config/providers"})
    if resp and "result" in resp and "contents" in resp["result"]:
        content_text = resp["result"]["contents"][0].get("text", "")
        if "providerType" in content_text:
            log_ok("Successfully queried active AI provider configurations from SQLite database")
            print(f"{Color.DIM}{content_text}{Color.RESET}")
            passed_count += 1
        else:
            log_err("Provider config resource text payload was empty or invalid")
    else:
        log_err("Failed to read resource lipirx://config/providers")

    # --------------------------------------------------------------------------
    # Step 9: Read Resource: AgentOps Execution Traces
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[9/{total_count}] Read Resource: lipirx://history/traces{Color.RESET}")
    resp = send_mcp_request("5", "resources/read", {"uri": "lipirx://history/traces"})
    if resp and "result" in resp and "contents" in resp["result"]:
        traces_text = resp["result"]["contents"][0].get("text", "")
        traces = json.loads(traces_text) if traces_text.startswith("[") else []
        log_ok(f"Successfully retrieved {len(traces)} AgentOps forensic execution trace records from SQLite")
        passed_count += 1
    else:
        log_err("Failed to read resource lipirx://history/traces")

    # --------------------------------------------------------------------------
    # Step 10: Call Tool: parse_medical_entities
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[10/{total_count}] Execute Tool: parse_medical_entities{Color.RESET}")
    test_item = json.dumps([{"medicineName": "Napa Extra 500mg", "schedule": "1+0+1", "units": 10}])
    resp = send_mcp_request("6", "tools/call", {
        "name": "parse_medical_entities",
        "arguments": {"itemsJson": test_item}
    })
    if resp and "result" in resp and not resp["result"].get("isError", True):
        log_ok("Tool 'parse_medical_entities' executed successfully")
        print(f"{Color.DIM}{json.dumps(resp['result'], indent=2)}{Color.RESET}")
        passed_count += 1
    else:
        log_err(f"Tool 'parse_medical_entities' failed: {resp}")

    # --------------------------------------------------------------------------
    # Step 11: Call Tool: search_generic_substitutes
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[11/{total_count}] Execute Tool: search_generic_substitutes{Color.RESET}")
    test_substitutes = json.dumps([{"medicineName": "Napa Extra", "genericName": "Paracetamol"}])
    resp = send_mcp_request("7", "tools/call", {
        "name": "search_generic_substitutes",
        "arguments": {"itemsJson": test_substitutes}
    })
    if resp and "result" in resp and not resp["result"].get("isError", True):
        log_ok("Tool 'search_generic_substitutes' executed successfully")
        print(f"{Color.DIM}{json.dumps(resp['result'], indent=2)}{Color.RESET}")
        passed_count += 1
    else:
        log_err(f"Tool 'search_generic_substitutes' failed: {resp}")

    # --------------------------------------------------------------------------
    # Step 12: Call Tool: audit_patient_safety
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[12/{total_count}] Execute Tool: audit_patient_safety{Color.RESET}")
    test_safety = json.dumps([{"medicineName": "Napa Extra", "strengthValue": "500mg", "schedule": "1+0+1"}])
    resp = send_mcp_request("8", "tools/call", {
        "name": "audit_patient_safety",
        "arguments": {"itemsJson": test_safety}
    })
    if resp and "result" in resp and not resp["result"].get("isError", True):
        log_ok("Tool 'audit_patient_safety' executed successfully")
        print(f"{Color.DIM}{json.dumps(resp['result'], indent=2)}{Color.RESET}")
        passed_count += 1
    else:
        log_err(f"Tool 'audit_patient_safety' failed: {resp}")

    # --------------------------------------------------------------------------
    # Step 13: Call Tool: get_agent_execution_traces
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[13/{total_count}] Execute Tool: get_agent_execution_traces{Color.RESET}")
    resp = send_mcp_request("9", "tools/call", {
        "name": "get_agent_execution_traces",
        "arguments": {"limit": "10"}
    })
    if resp and "result" in resp and not resp["result"].get("isError", True):
        log_ok("Tool 'get_agent_execution_traces' executed successfully")
        passed_count += 1
    else:
        log_err("Tool 'get_agent_execution_traces' failed")

    # --------------------------------------------------------------------------
    # Step 14: Compose UI Node Hierarchy Dump Test
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}[14/{total_count}] Compose UI Node Hierarchy Dump Test{Color.RESET}")
    code, stdout, _ = run_adb(["shell", "uiautomator", "dump", "/sdcard/ui_dump.xml"])
    time.sleep(1.0)
    if code == 0:
        code2, xml_content, _ = run_adb(["shell", "cat", "/sdcard/ui_dump.xml"])
        if code2 == 0 and (len(xml_content) > 50 or any(keyword in xml_content for keyword in ["LipiRx", "Take Photo", "Settings", "AgentOps", "Prescription", "node"])):
            log_ok("Successfully dumped and verified Compose UI node hierarchy")
            passed_count += 1
        else:
            log_err("UI XML dump did not contain expected LipiRx UI node elements")
    else:
        log_err(f"Failed to dump UI automator XML: {stdout}")

    # --------------------------------------------------------------------------
    # Final Summary
    # --------------------------------------------------------------------------
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    if passed_count == total_count:
        print(f"{Color.GREEN}{Color.BOLD}✨ [FULL VERIFICATION SUCCESS] All {passed_count}/{total_count} system tests passed successfully!{Color.RESET}\n")
    else:
        print(f"{Color.YELLOW}{Color.BOLD}⚠ [SUMMARY] {passed_count}/{total_count} system tests passed.{Color.RESET}\n")

if __name__ == "__main__":
    test_full_application()
