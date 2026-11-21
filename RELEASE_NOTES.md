# v0.4.4 - Persistent Updates, Custom SLM Folder & Interactive Medicine Editing

LipiRx v0.4.4 resolves application startup crashes, introduces persistent update dismissal preferences, enables custom SLM model folder selection with auto-scanning, and adds interactive medicine item editing.

### ✨ New
- **Interactive Medicine Item Editing**: Edit scanned brand names, generic compositions, strength values/units, dosage schedules, duration, quantities, instructions, and review flags via modal `EditMedicineDialog`. Edits automatically persist to SQLite history and update POS copy summaries (read-only in audit history mode).
- **Custom SLM Model Folder Selection**: Interactively browse and select custom storage directories on Android (`ACTION_OPEN_DOCUMENT_TREE`), expanding local model discovery to custom folders and `.litertlm`, `.tflite`, `.bin`, and `.gguf` weight files with auto-scanning.
- **Persistent Update Dismissal**: Dismissing an update prompt now remembers your choice per version in database storage (`DISMISSED_UPDATE_VERSION`), suppressing repetitive launch popups while retaining notice cards in Settings.
- **Disk Cache Verification**: Automatically detects previously downloaded updates on disk, allowing immediate installation without re-downloading.

### 🚀 Improved
- **Inline Settings Update Flow**: Performing update checks or downloads from the Settings section displays progress, permissions, and install options entirely inline without pop-up dialogs.
- **Automatic Storage Cleanup**: Automatically removes temporary update installer files from device cache on app startup and before starting new downloads.
- **Complete Localization**: Externalized all update and settings interface notices into string resources (`composeResources/values/strings.xml`) for full language support.

### 🐞 Fixed
- **App Startup Crash**: Resolved an initialization crash caused by missing startup database keep rules on release builds.
- **Re-Check Action Fix**: Ensured tapping "Check for updates" in Settings always performs a fresh update check even when an update is pending installation.
- **Clear Folder Path Fix**: Corrected state flow equality comparison so tapping the clear button (`✕`) immediately resets custom folder paths and UI text.
- **Unconfigured Provider Defaults**: Removed hardcoded default model pre-selections (`gemini-2.5-flash`) for unconfigured providers, showing clear API key configuration notices instead.

### 🛠 Under the hood
- Added ProGuard keep rules for Android App Startup (`androidx.startup`) and WorkManager database classes (`WorkDatabase_Impl`).
- Removed legacy `WorkManagerInitializer` manifest node (`tools:node="remove"`) to optimize cold app launch times.

### 📱 Platform notes
- **Android**: Integrated Android Storage Access Framework (`OpenDocumentTree`) for folder selection and tree Uri resolution.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.4.3...v0.4.4
