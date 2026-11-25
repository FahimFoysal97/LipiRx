# LipiRx v0.6.4 - Agent Tool Interface Refinement, Animation Polish & Evaluation Trace Logging

LipiRx v0.6.4 introduces streamlined agent tool definitions with default properties, removal of obsolete RxAgent interfaces, LoadingView animation lifecycle polish on completion, presentation state & UI component refactoring, and enhanced trace flow logging in `RxAgentEvaluator`.

### ✨ New
- **Trace Flow Parameter in `RxAgentEvaluator`**: Added contextual `flow` tracking parameter to `runEvaluationSuite()` for robust trace context logging during automated benchmark execution.
- **Streamlined Agent Tool Definitions**: Added default properties to `LipiRxAgentTool` interface and cleaned up obsolete `RxAgent` legacy interface definitions.

### 🚀 Improved
- **LoadingView Animation Lifecycle Polish**: Stopped scanning animation on pipeline completion in `LoadingView` (`ManagerCard`) so completed analysis states render cleanly without continuous pulsing.
- **Modular Presentation State & UI Refactoring**: Refactored presentation state classes and UI components across loading, results, telemetry, history, settings, and home screens for enhanced maintainability and architectural consistency.
- **Enhanced Supervisor Agent Retry Logic**: Refined supervisor agent error handling and retry mechanisms during turn-based execution loops.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.3...v0.6.4
