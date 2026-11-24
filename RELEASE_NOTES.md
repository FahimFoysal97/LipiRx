# LipiRx v0.6.0 - Room 3 KMP Database Migration, Modular Koin DI & Explicit Completion UX

LipiRx v0.6.0 migrates the local persistence layer to Room 3 KMP (`androidx.room3`), modularizes the Koin dependency injection graph, fixes Room main-thread database access guards, introduces explicit manual completion stepping for prescription scanning, and updates AgentOps telemetry controls.

### ✨ New
- **Explicit Manual Completion Step**: Replaced automatic completion navigation to results with an explicit "View Results" primary action button on `LoadingView` and `AnalysisLoadingScreen`, giving pharmacists and developers full visibility over the completed multi-agent execution feed before advancing.
- **Room 3 KMP Migration**: Migrated local database persistence from SQLDelight to Room 3 (`androidx.room3:room3-runtime:3.0.3`) with `androidx.sqlite:sqlite-bundled:2.7.1` across Android and iOS targets.
- **Koin DI Modularization**: Reorganized the Koin dependency injection graph into 6 sub-modules (`networkModule`, `databaseModule`, `aiAgentModule`, `mcpModule`, `domainModule`, and `presentationModule`).
- **Real-Time MCP Controller Toggle**: Connected `ManageProviderConfigUseCase.saveMcpServerEnabled()` to `McpServerController.setEnabled()`, allowing the MCP HTTP server on port 8080 to start/stop immediately when toggled in Settings.

### 🚀 Improved
- **Loading View UI & Live Feed Animations**: Added smooth `animateContentSize` expansion animations to `FeedCard` in `LoadingView.kt` and added a dedicated completed state Compose `@Preview` (`LoadingViewCompletePreview`).
- **Telemetry Benchmark Actions**: Updated sample benchmark action icons from `Refresh` to `PlayArrow` across `AgentTelemetryScreen.kt` for clearer execution semantics.
- **Dynamic Model Refresh in Image Preview**: `ImagePreviewViewModel` now re-fetches the latest `agentConfigs` dynamically before processing prescription images, ensuring model switches made in Settings immediately apply to analysis pipeline execution.
- **Room 3 Coroutine Safety**: Offloaded synchronous database calls in `LipiRxRoomRepository` to `Dispatchers.IO` via `runBlocking(Dispatchers.IO)`, satisfying Room 3 thread safety guards.

### 🐞 Fixed
- **Premature Loading Completion Guard**: Fixed `isComplete` evaluation in `LoadingView.kt` so `!isLoading` only flags completion when the pipeline has progressed past `PipelinePhase.PLANNING`, preventing premature navigation triggers on initial composition.
- **Redundant Back Handler Cleanup**: Removed shadowed and unreachable `AppNavKey.AnalysisLoading` back handling logic from `App.kt`.
- **Room Main-Thread Guard Crash**: Resolved `IllegalStateException: Cannot access database on the main thread` triggered during UI touch callbacks and app initialization.
- **Configuration & API Key Persistence**: Fixed API keys, custom URLs, theme modes, and model selections persisting reliably across app restarts.

### 🛠 Under the hood
- Externalized `btn_view_results` ("View Results") string resource into `composeResources/values/strings.xml`.
- Reorganized remote AI service implementations into `com.snehonir.lipirx.data.remote.service` and prompt constants into `com.snehonir.lipirx.data.remote.prompt`.
- Updated ProGuard keep rules for Room 3 database components and SQLite drivers.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.5.3...v0.6.0
