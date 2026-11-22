# LipiRx v0.5.2 - Native KMP Test Suite, Screen Container Architecture & MCP Toggle Automation

LipiRx v0.5.2 introduces a native Kotlin Multiplatform unit test suite, standardizes 1:1 stateful screen container architecture, fixes singleton ViewModel state propagation for theme changes, and enhances end-to-end UI automation.

### ✨ New
- **Native KMP Unit Test Suite**: Added comprehensive native unit & integration test coverage in `shared/test/` using `kotlin.test` and `kotlinx-coroutines-test`:
  - `AppNavKeyTest`: Tests Navigation 3 route string serialization and fallback resolution.
  - `PrescriptionSessionStateTest`: Tests session state isolation, active scan persistence, and history item opening.
  - `SemVerTest`: Tests semantic versioning parsing and version comparison.
  - `LipiRxMcpServerTest`: Tests JSON-RPC 2.0 request parsing and error response handling.
- **Custom Image Testing Support**: `test_full_app.py` and `test_full_app_ui.py` now support custom prescription image CLI arguments (`--image`) and interactive image prompts.
- **MCP Server Endpoint Toggle Test**: Added automated UI testing for toggling the MCP server endpoint switch ON/OFF in Settings.

### 🚀 Improved
- **Stateful Screen Container Architecture**: Standardized presentation screens into 1:1 stateful container composables (`HomeScreen`, `ImagePreviewScreen`, `AnalysisLoadingScreen`, `ScanResultsScreen`, `HistoryScreen`, `SettingsScreen`, `AgentTelemetryScreen`) delegating to stateless views (`HistoryView`, `SettingsView`).
- **Theme Mode State Propagation**: Changed `SettingsViewModel` scope from `factory` to `single` in Koin DI module, ensuring `App.kt` and `SettingsScreen` share the same ViewModel instance so theme mode switches immediately apply across the app.
- **Formatted Model Display Names**: Applied model name formatting in `ImagePreviewAndProcessView` to clean up model labels in the pipeline summary.
- **Release Automation Formatting**: Refactored `release.py` to parse Line 1 of `RELEASE_NOTES.md` as the GitHub Release Title and the remaining body as description notes.

### 🐞 Fixed
- **Theme Change Propagation Fix**: Fixed theme mode switches in Settings not reflecting globally due to duplicate ViewModel instance creation.
- **MCP Connection Drop Fix**: Resolved socket stream reading desynchronization in `McpHttpServer.kt` by consuming body bytes directly on `Dispatchers.IO`.
- **UTF-8 ADB Console Encoding**: Handled non-ASCII character decoding in ADB test runner output (`test_full_app_ui.py`).

### 🛠 Under the hood
- Added `kotlin-test` and `kotlinx-coroutines-test` dependencies to `libs.versions.toml` and `shared/module.yaml`.
- Added `/screenshots/` to `.gitignore`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.5.1...v0.5.2
