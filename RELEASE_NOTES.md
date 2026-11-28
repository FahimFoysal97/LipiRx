# LipiRx v0.6.5 - Merged Analysis & Scan Results Screen, Supervisor Status Line, Compact View Mode & Pipeline UseCase Consolidation

LipiRx v0.6.5 introduces a merged prescription analysis and scan results experience. The standalone loading screen has been unified into `ScanResultsScreen`, featuring a pinned `AnalysisHeader` with a dedicated **Supervisor Status Line**, worker agent status strips, real-time streaming medicine cards with progressive field shimmer placeholders (`MedicineCardProgress`), a **Compact vs. Detailed View Mode**, custom canvas-drawn metadata glyphs, and direct card editing.

### ✨ New
- **Merged Analysis & Results Screen**: Unified `AnalysisLoadingScreen` directly into `ScanResultsScreen` with a pinned `AnalysisHeader`, rendering live supervisor turns, progress bars, and streaming parsed medicine cards in real time.
- **Supervisor Status Line (`SupervisorLine`)**: Added a dedicated supervisor status bar inside `AnalysisHeader` displaying live manager status, active turn count badges (`Turn N`), and real-time reasoning context.
- **Compact vs. Detailed View Mode**: Introduced a view mode toggle (`Compact` / `Detailed`) in `MedicineListView` filter bar with active pill selection (`selected = compactView`). Completed medicine cards collapse into single-line summaries (displaying inline review notes for flagged items) with tap-to-expand details.
- **Custom Canvas Metadata Glyphs & Pill Badges**: Refactored `MedicineItemCard` with custom canvas-drawn metadata glyphs (`MetaGlyph` for meal plates, quantity boxes, and capsules; `SlotGlyph` for morning/noon/night sun & moon dose slots), status pill badges (`StatusCorner`, `StatusBadge`), brand name/strength splitting, and inline edit actions (`btn_edit`).
- **Progressive Field Loading (`MedicineCardProgress`)**: Medicine cards display field-level shimmer placeholders for dosage schedules, generic compositions, and safety verification until worker agents (`ENTITY_PARSER`, `GENERIC_SUBSTITUTE`, `SAFETY_AUDITOR`) complete execution.
- **Real-Time Streaming Event (`PipelineEvent.ItemsUpdated`)**: Emits intermediate parsed medicine lists from worker tools directly to `PrescriptionSessionState` during analysis turns.
- **Consolidated Pipeline Runner (`LaunchPrescriptionPipelineUseCase`)**: Unified pipeline initialization, provider config resolution, API key validation, session state management, and scan summary persistence into a single shared use case consumed across ViewModels.

### 🚀 Improved
- **Header Summary Counts**: `AnalysisHeader` automatically shifts from a linear progress indicator during analysis to summary statistics (`summary_counts`, `summary_flagged`, `summary_ok`) upon pipeline completion.
- **Clean Navigation & Exit Behavior**: Replaced the multi-screen transition (`ImagePreviewScreen` -> `AnalysisLoadingScreen` -> `ScanResultsScreen`) with a direct route to results (`AppNavKey.Results`) and aligned Back button & "Done" button navigation.
- **History Record Isolation**: Enhanced `PrescriptionSessionState` state management to ensure background streaming updates and completion results never overwrite historical prescription records opened from History.
- **Session Reset on Analysis Start**: Updated `startPipelineAnalysis` to clear stale medicines from prior scans upon initiating new analyses.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.4...v0.6.5
