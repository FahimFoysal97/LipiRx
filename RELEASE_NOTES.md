# LipiRx v0.6.3 - Reactive Telemetry, Localized Reasoning & Parallel Dispatch Optimization

LipiRx v0.6.3 introduces real-time reactive Room database observation for AgentOps telemetry and scan history, localized multi-agent reasoning via Compose Resources, wall-clock turn duration calculations, and multi-agent parallel dispatch optimization.

### ✨ New
- **Reactive Room Database Observation (`Flow`)**: Integrated Room 3 `Flow` queries across DAOs (`AgentExecutionTraceDao`, `PrescriptionHistoryDao`), `PrescriptionRepository`, `GetScanHistoryUseCase`, and ViewModels (`AgentTraceViewModel`, `HistoryViewModel`, `HomeViewModel`). Newly executed prescription analysis traces, scan history items, and dashboard counts automatically update in real-time across screens (`AgentTelemetryScreen`, `HistoryScreen`, `HomeScreen`) without requiring app restarts or manual re-queries.
- **Localized Multi-Agent Reasoning**: Migrated all Supervisor reasoning, turn planning, agent dispatch descriptions, and pipeline state messages to Compose Resources (`Res.string.reason_*`).
- **Multi-Agent Parallel Dispatch Support**: Updated `SupervisorDecisionDto` and `RxSupervisorAgent` to parse `parallelAgents` lists and prompt the Supervisor LLM on concurrent tool dispatch (`GENERIC_SUBSTITUTE` + `SAFETY_AUDITOR`), accelerating Stage 3 analysis execution time.
- **Accurate Telemetry Session Grouping**: Refactored `groupTracesIntoScanSessions` to calculate total session pipeline latency by summing per-turn wall-clock durations (`supervisorTime + max(workerTimes)`) rather than naive trace addition.

### 🚀 Improved
- **Turn-by-Turn Dynamic Reasoning Feed**: Updated `KoogRxSupervisorEngine` to emit localized `PipelineEvent.Planning` strings at the start of each turn loop, instantly updating the UI reasoning text while the supervisor evaluates the workspace.
- **Enhanced Loading View Reasoning**: Increased collapsed `maxLines` in `LoadingView` (`ManagerCard`) from `1` to `2` for multi-line reasoning visibility.
- **Concurrent Runtime Configuration**: Moved `resolveRuntimeConfig` inside parallel worker `async` coroutines in `KoogRxSupervisorEngine`, executing SQLite config lookups concurrently and eliminating serial dispatch delay.
- **Individual Parallel Worker Timing**: Recorded execution duration per worker independently (`workerTimeMs`) in parallel dispatch traces.
- **Synchronized Required Checks Progress**: Updated `PipelineProgress` and `LoadingView` progress bars to track all 4 worker agents (`TRANSCRIPTION`, `ENTITY_PARSER`, `GENERIC_SUBSTITUTE`, `SAFETY_AUDITOR`) in 25% step increments (`0/4` → `4/4`).

### 🐞 Fixed
- **Stale Telemetry List Bug**: Fixed issue where recently executed prescription analysis traces were not visible in `AgentTelemetryScreen` until app restart by converting trace queries to reactive Room `Flow` streams and launching observation on screen load.
- **Stale Reasoning Text during Supervisor Thinking**: Fixed bug where reasoning text remained frozen on the previous turn's agent message while the supervisor LLM planned the next turn.
- **False Safety Warning Flags**: Removed forced `!item.brandRecognized` rule from local safety checks and updated `mergeSafety` so clean prescriptions with high confidence and zero review flags render a green checkmark instead of false amber warnings.
- **Scan Session Boundary Reset**: Added turn sequence reset detection (`trace.turnNumber < lastTrace.turnNumber`) to prevent consecutive scan sessions executed in rapid succession from merging into a single card.
- **Telemetry Final Item Count Fallback**: Used the latest non-zero item count (`lastOrNull { it.itemCount > 0 }`) for telemetry scan cards so final supervisor evaluation traces do not report `0 items`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.2...v0.6.3
