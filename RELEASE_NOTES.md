# LipiRx v0.6.0 - Parallel Multi-Agent Execution, Redesigned Loading UX, AppFunctions & Emerald Theme

LipiRx v0.6.0 introduces parallel multi-agent tool execution in the Koog Supervisor Engine, a completely redesigned 5-stage analysis loading screen with explicit "View Results" manual completion, Android AppFunctions & App Actions deep link shortcuts, an upgraded Emerald Green design palette with system bar synchronization, and refactored AI fallback resilience.

### ✨ New
- **Parallel Multi-Agent Execution**: `KoogRxSupervisorEngine` now supports dispatching independent worker agents (e.g., `GENERIC_SUBSTITUTE` and `SAFETY_AUDITOR`) concurrently using Kotlin coroutines, significantly reducing overall prescription analysis latency.
- **Explicit Manual Completion Step**: Replaced automatic completion transitions with an explicit "View Results" primary action on `LoadingView` and `AnalysisLoadingScreen`, allowing pharmacists and developers to inspect live Supervisor reasoning and the full multi-agent feed prior to navigating to results.
- **Android AppFunctions & Launcher Shortcuts**: Integrated `androidx.appfunctions` with App Actions deep links and launcher shortcuts (`shortcuts.xml`) for instant prescription scan triggers.
- **System Bar Sync & Emerald Green Palette**: Refactored the design system with a new Emerald Green palette (`Color(0xFF0B7A6E)`), updated launcher icon graphics, paper illustration tokens, and system status/navigation bar theme synchronization across platforms.

### 🚀 Improved
- **5-Stage Analysis Loading UI (`LoadingView`)**: Redesigned loading view featuring segmented prescription scanner card animations, expandable Supervisor reasoning card, worker status indicators (`0% → 100%`), and an animated live progress feed (`FeedCard` with `animateContentSize`).
- **AI Service Retry & Fallback Architecture**: Refactored `CompositeAiService` fallback routing, improved 3-attempt retry rules, and sanitized prompt building in `RxAgentTools`.
- **Settings Update UI & Accessibility**: Redesigned inline update check UI in Settings, enhanced accessibility screen semantics, and improved provider setup feedback.
- **Markdown Link Rendering**: Migrated UI markdown text rendering to Compose `LinkAnnotation` with refined link parsing logic.
- **Telemetry Benchmark Actions**: Updated sample benchmark action icons from `Refresh` to `PlayArrow` across `AgentTelemetryScreen.kt` for clearer execution semantics.

### 🐞 Fixed
- **Premature Loading Completion Guard**: Fixed `isComplete` evaluation in `LoadingView.kt` so `!isLoading` only flags completion when the pipeline has progressed past `PipelinePhase.PLANNING`, preventing premature navigation triggers on initial composition.
- **Redundant Back Handler Cleanup**: Removed shadowed and unreachable `AppNavKey.AnalysisLoading` back handling logic from `App.kt`.
- **Cancellation & Back Navigation in Loading Screen**: Streamlined `BackHandler` logic during analysis loading to properly cancel active supervisor jobs and return to preview cleanly.

### 🛠 Under the hood
- Refactored KMP unit test suite into modular architecture and automated MCP server toggle in tests (`test_mcp.py`, `build_and_install.py`).
- Externalized `btn_view_results` ("View Results") string resource into `composeResources/values/strings.xml`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.5.3...v0.6.0
