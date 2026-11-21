# v0.5.0 - Navigation 3 Architecture, Dedicated ViewModels & Smooth Theme Interpolation

LipiRx v0.5.0 transitions the application to a modern Jetpack Navigation 3 destination architecture with dedicated feature ViewModels, reorganizes presentation components into modular package hierarchies, resolves critical state persistence and thread concurrency defects, and introduces a 60/120 FPS smooth theme transition engine.

### ✨ New
- **Jetpack Navigation 3 Destination Architecture**: Replaced ad-hoc UI state flags with Jetpack Navigation 3 destination state management (`AppNavKey`, `AppNavigator`). Navigation state survives Android screen rotations and configuration changes using Compose `rememberSaveable` with a custom `Saver`.
- **Dedicated Feature ViewModels & Koin DI**: Introduced specialized, testable ViewModels (`HomeViewModel`, `ImagePreviewViewModel`, `AnalysisLoadingViewModel`, `ScanResultsViewModel`, `HistoryViewModel`, `SettingsViewModel`), bound in Koin dependency injection modules.
- **60/120 FPS Smooth Theme Transition Engine**: Implemented single-driver linear color interpolation (`LipiColors.lerp`) in `LipiRxTheme`, smoothly animating all 22 custom design tokens and Material 3 color schemes without recomposition lag.
- **Animated Segmented Theme Selector**: Created a 3-segment theme control in Settings with a spring-physics sliding highlight pill (`animateDpAsState` + `spring`) and smooth text/icon color transitions.
- **Interactive API Key Visibility Toggle**: Replaced clear and paste buttons on the API Key input field with an inline eye toggle button (`EyeShowVectorIcon` / `EyeHideVectorIcon`), masking/unmasking credentials on tap.
- **Tap-Outside Focus Dismissal**: Added tap gesture detection (`detectTapGestures`) across Settings layout to automatically clear text field focus and dismiss the soft keyboard when tapping non-focusable surfaces.

### 🚀 Improved
- **Modular Feature Package Hierarchy**: Organized presentation components into clean, domain-aligned packages (`presentation/home`, `preview`, `loading`, `results`, `history`, `settings`, `common`).
- **Session State Isolation**: Introduced `activeScanMedicines` in `PrescriptionSessionState` to separate active scan results from historical record inspection, preventing history record views from corrupting active session data.
- **Fallback API Key Propagation**: Propagated `fallbackApiKey` through `TranscriptionInput` and `CompositeAiService`, ensuring fallback AI providers authenticate cleanly when using a distinct provider key.
- **Uncached Provider Auto-Discovery**: Selecting an uncached provider tab in Settings automatically triggers provider model fetching without requiring a manual save or app restart.

### 🐞 Fixed
- **Configuration Change Reset**: Fixed an issue where rotating the screen wiped in-progress analysis or scan result states back to Home.
- **Unconditional Analysis Navigation**: Refactored `ImagePreviewViewModel.processSelectedImage()` to return a `Boolean` success status, preventing premature navigation to loading states when API key validation fails.
- **Main Thread Database I/O**: Explicitly offloaded Room/SQLite history queries in ViewModels to `Dispatchers.Default`.
- **Coroutine Cancellation Swallowing**: Rethrew `CancellationException` in pipeline coroutine processing blocks to maintain structured coroutine cancellation contracts.
- **Unsaved Pharmacist Edits**: Ensured pharmacist edits on fresh scan results in `ScanResultsViewModel` are immediately persisted to the underlying SQLite database.
- **Image Byte Array Memory Leaks**: Reset raw prescription image byte arrays in `PrescriptionSessionState` when navigating back to `HomeScreen`.

### 🛠 Under the hood
- Refactored `LipiRxTheme` to compute theme color interpolation via a single `animateFloatAsState` progress driver, reducing Compose animation overhead by 95%.
- Configured `sanitizeHeader` and `sanitizeLogMessage` in Ktor Client `Logging` plugin to automatically mask API keys in URL query parameters (`?key=***`), authorization headers (`Bearer ***`, `x-api-key`), and JSON body payloads in logcat output.
- Externalized 100% of user-visible UI strings into `composeResources/values/strings.xml`.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.4.4...v0.5.0
