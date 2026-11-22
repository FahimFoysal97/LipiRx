# LipiRx v0.5.1 - Navigation 3 Architecture, Dedicated ViewModels & Smooth Theme Engine

LipiRx v0.5.1 introduces Jetpack Navigation 3 destination state persistence across device rotations, dedicated feature ViewModels, a 60/120 FPS smooth theme transition engine, and fixes embedded MCP server HTTP connection drops.

### ✨ New
- **Navigation State Persistence**: Screen navigation and back-stack states now survive device orientation changes and process restarts without resetting to Home.
- **Smooth Theme Transitions**: Switching between Light, Dark, and System theme modes now smoothly animates all app colors and card surfaces without screen flickering.
- **Spring Theme Selector**: Interactive 3-segment theme control in Settings with a spring-physics sliding highlight pill.
- **API Key Visibility Toggle**: Tap the eye icon on API key input fields to securely mask or reveal credentials inline.
- **Tap-Outside Keyboard Dismissal**: Tap anywhere outside text fields in Settings to automatically dismiss the soft keyboard.
- **Automated MCP Test Runner**: Added `test_mcp.py` script to test embedded MCP server JSON-RPC tools and resources automatically over ADB.

### 🚀 Improved
- **Prescription Session Isolation**: Viewing historical scan records no longer overwrites or clears your active prescription scan session.
- **Automatic Model Discovery**: Selecting an uncached provider tab in Settings automatically fetches available models without requiring a manual save.
- **Fallback API Key Support**: Secondary fallback AI providers now use their dedicated API keys when primary cloud requests fail.
- **Network Log Sanitization**: Automatically masks API keys, Bearer tokens, and credential headers in app logs.
- **Beautified Build Scripts**: Terminal build scripts (`build-apk`, `sign-release`, `release.py`) now display color-coded status banners.

### 🐞 Fixed
- **MCP Server Connection Drop**: Resolved an issue where embedded MCP server socket connections dropped during JSON-RPC initialization and tool execution requests.
- **Prescription Edit Persistence**: Saved pharmacist edits on fresh scan results directly to the history database.
- **Screen Rotation Navigation Reset**: Fixed in-progress prescription analysis or result views resetting to Home when rotating the device.
- **Background Thread Database Queries**: Offloaded all local database queries off the main thread to prevent UI freezing.
- **Memory Optimization**: Automatically clears raw prescription image byte arrays when returning to the home screen.

### 🛠 Under the hood
- Transitioned presentation layer to Jetpack Navigation 3 key-based routing (`AppNavKey`, `AppNavigator`) with `rememberSaveable` state serialization.
- Introduced dedicated feature ViewModels (`HomeViewModel`, `ImagePreviewViewModel`, `AnalysisLoadingViewModel`, `ScanResultsViewModel`, `HistoryViewModel`, `SettingsViewModel`) bound in Koin DI.
- Reorganized presentation layer into feature packages (`presentation/home`, `preview`, `loading`, `results`, `history`, `settings`, `common`).
- Optimized `LipiRxTheme` using single-driver `animateFloatAsState` and `LipiColors.lerp` linear color interpolation.
- Rethrew `CancellationException` in pipeline coroutine processing to maintain structured coroutine cancellation.

### 📱 Platform notes
- **Android**: Integrated Android Storage Access Framework URI resolution for custom SLM model folders and `FileProvider` package installer support.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.4.4...v0.5.1
