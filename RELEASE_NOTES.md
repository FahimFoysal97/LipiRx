# LipiRx v0.6.1 - Architectural Modularization, UseCases, Standardized Scaffold & ViewModel

LipiRx v0.6.1 introduces a comprehensive architectural refactoring across the Kotlin Multiplatform (KMP) shared and Android application modules. This release modularizes business logic into explicit domain UseCases, standardizes presentation layers with `LipiRxViewModel` and `LipiRxScaffold`, and enhances database error propagation safety in repositories.

### ✨ New
- **Domain UseCases Architecture**: Standardized business logic into explicit single-responsibility `LipiRxUseCase` and `LipiRxSuspendUseCase` implementations (`GetScanHistoryUseCase`, `SaveScanSummaryUseCase`, `DeleteScanHistoryUseCase`, etc.).
- **Standardized `LipiRxScaffold`**: Reusable screen scaffolding handling window insets, top bars, and consistent navigation actions across feature views.

### 🚀 Improved
- **Base `LipiRxViewModel` & Safe Coroutine Launching**: Introduced `LipiRxViewModel` featuring built-in state management and `launchSafe` coroutine exception handling across all feature ViewModels.
- **Repository Error Handling & Propagation**: Refactored database query methods in `LipiRxRoomRepository` to propagate database exceptions correctly instead of swallowing them via empty defaults.
- **Agent Tool Modularity**: Streamlined agent tools behind the `LipiRxAgentTool` interface for improved testability and compile-time exhaustiveness.

### 🐞 Fixed
- **Database Query Error Masking**: Prevented silent failure masking in room repositories by ensuring query failures propagate up to domain UseCases for proper handling.

### 🛠 Under the hood
- Standardized AI services under `LipiRxAiService` and `safeApiCall` / `safeDbCall` execution wrappers.
- Updated UI automation script (`test_full_app_ui.py`) with improved scrolling logic and robust back navigation helpers.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.0...v0.6.1
