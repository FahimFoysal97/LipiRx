# LipiRx v0.5.3 - Room 3 KMP Database Migration, Modular Koin DI & Dynamic Model Refresh

LipiRx v0.5.3 migrates the local persistence layer to Room 3 KMP (`androidx.room3`), modularizes the Koin dependency injection graph, fixes Room main-thread database access guards, and ensures model changes in Settings immediately take effect during image analysis.

### ✨ New
- **Room 3 KMP Migration**: Migrated local database persistence from SQLDelight to Room 3 (`androidx.room3:room3-runtime:3.0.3`) with `androidx.sqlite:sqlite-bundled:2.7.1` across Android and iOS targets.
- **Koin DI Modularization**: Reorganized the Koin dependency injection graph into 6 sub-modules (`networkModule`, `databaseModule`, `aiAgentModule`, `mcpModule`, `domainModule`, and `presentationModule`).
- **Real-Time MCP Controller Toggle**: Connected `ManageProviderConfigUseCase.saveMcpServerEnabled()` to `McpServerController.setEnabled()`, allowing the MCP HTTP server on port 8080 to start/stop immediately when toggled in Settings.

### 🚀 Improved
- **Dynamic Model Refresh in Image Preview**: `ImagePreviewViewModel` now re-fetches the latest `agentConfigs` dynamically before processing prescription images, ensuring model switches made in Settings immediately apply to analysis pipeline execution.
- **Room 3 Coroutine Safety**: Offloaded synchronous database calls in `LipiRxRoomRepository` to `Dispatchers.IO` via `runBlocking(Dispatchers.IO)`, satisfying Room 3 thread safety guards.

### 🐞 Fixed
- **Room Main-Thread Guard Crash**: Resolved `IllegalStateException: Cannot access database on the main thread` triggered during UI touch callbacks and app initialization.
- **Configuration & API Key Persistence**: Fixed API keys, custom URLs, theme modes, and model selections persisting reliably across app restarts.

### 🛠 Under the hood
- Reorganized remote AI service implementations into `com.snehonir.lipirx.data.remote.service` and prompt constants into `com.snehonir.lipirx.data.remote.prompt`.
- Updated ProGuard keep rules for Room 3 database components and SQLite drivers.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.5.2...v0.5.3
