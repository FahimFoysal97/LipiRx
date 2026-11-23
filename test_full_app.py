#!/usr/bin/env python3
"""
LipiRx Modular Test Suite Master Runner

Runs modular test suites for:
1. MCP Protocol & Tools Pipeline (test_mcp.py)
2. Android AppFunctions Integration (test_appfunctions.py)
3. Gemini <-> MCP Tool Bridge Integration (test_gemini_mcp.py)
4. End-to-End On-Device UI & Workflow Automation (test_full_app_ui.py)
"""

import argparse
import os
import sys
import time

# Enable VT100 ANSI escape sequences on Windows
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

# Import modular test scripts
import test_mcp
import test_appfunctions
import test_gemini_mcp
import test_full_app_ui

class Color:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    CYAN = "\033[96m"
    WHITE = "\033[97m"

def log_header(title: str) -> None:
    width = 72
    print(f"\n{Color.CYAN}{Color.BOLD}╔{'═' * (width - 2)}╗{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}║  {title.center(width - 6)}  ║{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}╚{'═' * (width - 2)}╝{Color.RESET}\n")

def run_all_modular_tests() -> None:
    log_header("LipiRx Modular System Test Suite")

    results = {}

    # Module 1: MCP Server Pipeline Test
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}▶ Module 1: MCP Protocol & Tools Registry Test (test_mcp.py){Color.RESET}")
    print(f"{Color.BOLD}============================================================{Color.RESET}")
    try:
        test_mcp.test_mcp_pipeline()
        results["MCP Server & Tools"] = True
    except Exception as e:
        print(f"{Color.RED}✖ Module 1 failed: {e}{Color.RESET}")
        results["MCP Server & Tools"] = False

    time.sleep(1)

    # Module 2: Android AppFunctions Integration Test
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}▶ Module 2: Android AppFunctions On-Device Test (test_appfunctions.py){Color.RESET}")
    print(f"{Color.BOLD}============================================================{Color.RESET}")
    try:
        test_appfunctions.test_all_app_functions()
        results["Android AppFunctions"] = True
    except Exception as e:
        print(f"{Color.RED}✖ Module 2 failed: {e}{Color.RESET}")
        results["Android AppFunctions"] = False

    time.sleep(1)

    # Module 3: Gemini <-> MCP Tool Bridge Test
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}▶ Module 3: Gemini <-> MCP Integration Test (test_gemini_mcp.py){Color.RESET}")
    print(f"{Color.BOLD}============================================================{Color.RESET}")
    try:
        test_gemini_mcp.test_gemini_mcp()
        results["Gemini MCP Bridge"] = True
    except Exception as e:
        print(f"{Color.RED}✖ Module 3 failed: {e}{Color.RESET}")
        results["Gemini MCP Bridge"] = False

    time.sleep(1)

    # Module 4: E2E UI & Workflow Automation Test
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}▶ Module 4: E2E On-Device UI Automation Test (test_full_app_ui.py){Color.RESET}")
    print(f"{Color.BOLD}============================================================{Color.RESET}")
    try:
        test_full_app_ui.test_full_ui_and_workflow()
        results["E2E UI Automation"] = True
    except Exception as e:
        print(f"{Color.RED}✖ Module 4 failed: {e}{Color.RESET}")
        results["E2E UI Automation"] = False

    # Summary
    print(f"\n{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}                  MODULAR TEST SUITE SUMMARY                 {Color.RESET}")
    print(f"{Color.BOLD}============================================================{Color.RESET}")

    passed_count = sum(1 for v in results.values() if v)
    total_count = len(results)

    for module_name, passed in results.items():
        status = f"{Color.GREEN}✔ PASSED{Color.RESET}" if passed else f"{Color.RED}✖ FAILED{Color.RESET}"
        print(f"  • {module_name:<30}: {status}")

    print(f"{Color.BOLD}------------------------------------------------------------{Color.RESET}")
    if passed_count == total_count:
        print(f"{Color.GREEN}{Color.BOLD}✨ [ALL MODULES PASSED] {passed_count}/{total_count} test modules passed successfully!{Color.RESET}\n")
    else:
        print(f"{Color.YELLOW}{Color.BOLD}⚠ [SUMMARY] {passed_count}/{total_count} test modules passed.{Color.RESET}\n")

def main():
    parser = argparse.ArgumentParser(description="LipiRx Modular Test Suite Master Runner")
    parser.add_argument("--mcp", action="store_true", help="Run MCP Server & Tools test (test_mcp.py)")
    parser.add_argument("--appfunctions", action="store_true", help="Run Android AppFunctions test (test_appfunctions.py)")
    parser.add_argument("--gemini", action="store_true", help="Run Gemini <-> MCP bridge test (test_gemini_mcp.py)")
    parser.add_argument("--ui", action="store_true", help="Run E2E UI automation test (test_full_app_ui.py)")
    parser.add_argument("--all", action="store_true", help="Run all modular test suites (default)")

    args = parser.parse_args()

    if args.mcp:
        test_mcp.test_mcp_pipeline()
    elif args.appfunctions:
        test_appfunctions.test_all_app_functions()
    elif args.gemini:
        test_gemini_mcp.test_gemini_mcp()
    elif args.ui:
        test_full_app_ui.test_full_ui_and_workflow()
    else:
        run_all_modular_tests()

if __name__ == "__main__":
    main()
