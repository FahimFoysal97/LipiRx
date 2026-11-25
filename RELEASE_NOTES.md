# LipiRx v0.6.3 - Telemetry Optimization, Parallel Agent Dispatch & Safety Audit Fixes

LipiRx v0.6.3 enhances AgentOps telemetry precision with wall-clock turn duration calculations, enables concurrent database lookups during multi-agent parallel dispatch, and refactors clinical safety review flagging rules.

### ✨ New
- **Multi-Agent Parallel Dispatch Support**: Updated `SupervisorDecisionDto` and `RxSupervisorAgent` to parse `parallelAgents` lists and prompt the Supervisor LLM on concurrent tool dispatch (`GENERIC_SUBSTITUTE` + `SAFETY_AUDITOR`), accelerating Stage 3 analysis execution time.
- **Accurate Telemetry Session Grouping**: Refactored `groupTracesIntoScanSessions` to calculate total session pipeline latency by summing per-turn wall-clock durations (`supervisorTime + max(workerTimes)`) rather than naive trace addition.

### 🚀 Improved
- **Concurrent Runtime Configuration**: Moved `resolveRuntimeConfig` inside parallel worker `async` coroutines in `KoogRxSupervisorEngine`, executing SQLite config lookups concurrently and eliminating serial dispatch delay.
- **Individual Parallel Worker Timing**: Recorded execution duration per worker independently (`workerTimeMs`) in parallel dispatch traces.
- **Synchronized Required Checks Progress**: Updated `PipelineProgress` and `LoadingView` progress bars to track all 4 worker agents (`TRANSCRIPTION`, `ENTITY_PARSER`, `GENERIC_SUBSTITUTE`, `SAFETY_AUDITOR`) in 25% step increments (`0/4` → `4/4`).

### 🐞 Fixed
- **False Safety Warning Flags**: Removed forced `!item.brandRecognized` rule from local safety checks and updated `mergeSafety` so clean prescriptions with high confidence and zero review flags render a green checkmark instead of false amber warnings.
- **Scan Session Boundary Reset**: Added turn sequence reset detection (`trace.turnNumber < lastTrace.turnNumber`) to prevent consecutive scan sessions executed in rapid succession from merging into a single card.
- **Telemetry Final Item Count Fallback**: Used the latest non-zero item count (`lastOrNull { it.itemCount > 0 }`) for telemetry scan cards so final supervisor evaluation traces do not report `0 items`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.2...v0.6.3
