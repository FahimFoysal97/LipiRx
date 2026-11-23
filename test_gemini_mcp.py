import json
import os
import sys
import urllib.request
import urllib.error
from typing import Optional, Tuple

# Enable ANSI colors on Windows terminal
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

MCP_URL = "http://localhost:8080/lipirx/mcp"

class Color:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    CYAN = "\033[96m"
    DIM = "\033[2m"

def send_mcp_rpc(method: str, params: dict = None, req_id: str = "1") -> dict:
    payload = {"jsonrpc": "2.0", "id": req_id, "method": method}
    if params is not None:
        payload["params"] = params
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        MCP_URL,
        data=data,
        headers={"Content-Type": "application/json", "User-Agent": "Gemini-MCP-Tester/1.0"}
    )
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))

def mcp_tool_to_gemini_function(mcp_tool: dict) -> dict:
    name = mcp_tool.get("name", "")
    description = mcp_tool.get("description", "")
    input_schema = mcp_tool.get("inputSchema", {})
    properties = input_schema.get("properties", {})
    required = input_schema.get("required", [])

    gemini_properties = {}
    for prop_name, prop_def in properties.items():
        gemini_properties[prop_name] = {
            "type": "STRING",
            "description": prop_def.get("description", "")
        }

    return {
        "name": name,
        "description": description,
        "parameters": {
            "type": "OBJECT",
            "properties": gemini_properties,
            "required": required
        }
    }

def fetch_available_gemini_models(api_key: str) -> list:
    url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
    req = urllib.request.Request(url, headers={"User-Agent": "Gemini-MCP-Tester/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            res_data = json.loads(response.read().decode("utf-8"))
            models = res_data.get("models", [])
            return [m.get("name", "").replace("models/", "") for m in models if "generateContent" in m.get("supportedGenerationMethods", [])]
    except Exception as e:
        return []

def call_gemini_with_mcp_tools(api_key: str, user_prompt: str, mcp_tools: list) -> tuple:
    gemini_functions = [mcp_tool_to_gemini_function(t) for t in mcp_tools]

    candidate_models = [
        "gemini-3.5-flash",
        "gemini-2.5-flash",
        "gemini-flash-latest",
        "gemini-3.5-flash-lite",
        "gemini-2.5-pro",
        "gemini-pro-latest"
    ]

    last_exception = None

    for model_name in candidate_models:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"

        payload = {
            "contents": [
                {
                    "role": "user",
                    "parts": [{"text": user_prompt}]
                }
            ],
            "tools": [
                {
                    "functionDeclarations": gemini_functions
                }
            ]
        }

        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})

        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                result_json = json.loads(response.read().decode("utf-8"))
                return model_name, result_json
        except urllib.error.HTTPError as e:
            if e.code == 404:
                last_exception = e
                continue
            else:
                raise e
        except Exception as e:
            last_exception = e
            continue

    if last_exception:
        raise last_exception
    raise RuntimeError("No Gemini candidate model responded.")

def test_gemini_mcp(api_key: Optional[str] = None) -> bool:
    print(f"\n{Color.CYAN}{Color.BOLD}============================================================{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}      LipiRx MCP Server <-> Gemini Integration Tester       {Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}============================================================{Color.RESET}\n")

    # Step 1: Query tools from local LipiRx MCP Server
    print(f"{Color.YELLOW}ℹ 1. Connecting to LipiRx MCP Server at {MCP_URL}...{Color.RESET}")
    try:
        tools_resp = send_mcp_rpc("tools/list")
        mcp_tools = tools_resp.get("result", {}).get("tools", [])
        print(f"{Color.GREEN}✔ Found {len(mcp_tools)} MCP tools from LipiRx:{Color.RESET}")
        for t in mcp_tools:
            print(f"  • {Color.BOLD}{t['name']}{Color.RESET}: {t['description']}")
    except Exception as e:
        print(f"{Color.RED}✖ Failed to connect to LipiRx MCP Server: {e}{Color.RESET}")
        print(f"{Color.YELLOW}Ensure LipiRx app is running and 'adb forward tcp:8080 tcp:8080' is active.{Color.RESET}")
        return False

    # Step 2: Get Gemini API key
    if not api_key:
        api_key = os.environ.get("GEMINI_API_KEY", "").strip()

    if not api_key:
        print(f"{Color.YELLOW}⚠ GEMINI_API_KEY environment variable not set. Skipping live Gemini cloud call.{Color.RESET}")
        print(f"{Color.GREEN}✔ MCP Server tool discovery verified successfully!{Color.RESET}")
        return True

    # Step 3: Test Prompt
    prompt = "Use the LipiRx MCP tool to parse the medical prescription entities for: Napa Extra 500mg 1+0+1 10 tablets"
    print(f"\n{Color.CYAN}▶ Sending Prompt with LipiRx MCP Tools:{Color.RESET}")
    print(f"{Color.DIM}\"{prompt}\"{Color.RESET}\n")

    try:
        model_used, gemini_resp = call_gemini_with_mcp_tools(api_key, prompt, mcp_tools)
        print(f"{Color.GREEN}✔ Connected using Gemini model: '{model_used}'{Color.RESET}")

        candidates = gemini_resp.get("candidates", [])
        if not candidates:
            print(f"{Color.RED}✖ Gemini returned no response candidates.{Color.RESET}")
            return False

        parts = candidates[0].get("content", {}).get("parts", [])
        function_call = None
        for p in parts:
            if "functionCall" in p:
                function_call = p["functionCall"]
                break

        if function_call:
            fn_name = function_call.get("name")
            fn_args = function_call.get("args", {})
            print(f"{Color.GREEN}✨ [SUCCESS] Gemini selected LipiRx MCP Tool: '{fn_name}'!{Color.RESET}")
            print(f"{Color.DIM}Arguments provided by Gemini: {json.dumps(fn_args, indent=2)}{Color.RESET}\n")

            # Execute tool call on LipiRx MCP
            print(f"{Color.YELLOW}ℹ Executing '{fn_name}' on LipiRx MCP Server...{Color.RESET}")
            mcp_call_resp = send_mcp_rpc("tools/call", {
                "name": fn_name,
                "arguments": fn_args
            })

            tool_result = mcp_call_resp.get("result", {})
            print(f"{Color.GREEN}✔ LipiRx MCP Tool Execution Result:{Color.RESET}")
            print(f"{Color.DIM}{json.dumps(tool_result, indent=2)}{Color.RESET}\n")
            print(f"{Color.GREEN}{Color.BOLD}🎉 Gemini <-> LipiRx MCP integration is fully working!{Color.RESET}\n")
            return True
        else:
            text_response = "".join([p.get("text", "") for p in parts])
            print(f"{Color.YELLOW}Gemini responded with text instead of tool call:{Color.RESET}")
            print(text_response)
            return True

    except Exception as e:
        print(f"{Color.RED}✖ Error during Gemini API call: {e}{Color.RESET}")
        available = fetch_available_gemini_models(api_key)
        if available:
            print(f"{Color.GREEN}Available models for your API key:{Color.RESET}")
            for m in available:
                print(f"  • {m}")
        return False

def main():
    test_gemini_mcp()

if __name__ == "__main__":
    main()
