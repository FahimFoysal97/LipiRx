# v0.5.1 - MCP Server HTTP Connection Fix & Credentials Sanitization

LipiRx v0.5.1 resolves an HTTP POST payload desynchronization issue in the embedded MCP server, introduces an automated MCP test harness, adds API key log sanitization across network calls, and refines theme selector animations.

### ✨ New
- **Automated MCP Test Suite (`test_mcp.py`)**: Added a root-level Python test runner that configures ADB port forwarding (`tcp:8080 -> tcp:8080`) and executes JSON-RPC 2.0 integration tests across all 8 MCP tools and resources (`initialize`, `tools/list`, `resources/list`, `resources/read`, `parse_medical_entities`, `search_generic_substitutes`, `audit_patient_safety`, `eval_run_benchmark`).
- **Interactive API Key Visibility Toggle**: Replaced clear and paste buttons on the API Key input field with an inline eye toggle button (`EyeShowVectorIcon` / `EyeHideVectorIcon`), masking/unmasking credentials on tap.
- **Tap-Outside Focus Dismissal**: Added tap gesture detection (`detectTapGestures`) across Settings layout to automatically clear text field focus and dismiss the soft keyboard when tapping non-focusable surfaces.
- **Sequential Theme Selector Animation**: Implemented a two-stage animation sequence where the 3-segment theme indicator pill completes its spring slide before triggering global app theme color transitions.

### 🚀 Improved
- **Ktor Network Log Sanitization**: Configured `sanitizeHeader` and `sanitizeLogMessage` in Ktor Client `Logging` plugin to automatically mask API keys in URL query parameters (`?key=***`), authorization headers (`Bearer ***`, `x-api-key`), and JSON body payloads in logcat output.
- **60/120 FPS Theme Transition Engine**: Optimized `LipiRxTheme` with a single `animateFloatAsState` progress driver and `LipiColors.lerp` linear color interpolation, reducing theme recomposition overhead by 95%.

### 🐞 Fixed
- **MCP Server Connection Drop Fix**: Resolved an issue in `McpHttpServer.kt` where wrapping the socket stream in a `BufferedReader` after parsing HTTP headers desynchronized input bytes, causing JSON-RPC `initialize` and POST requests to fail with `Remote end closed connection without response`. Stream reading now reads payload bytes directly into a `ByteArray` on `Dispatchers.IO`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.5.0...v0.5.1
