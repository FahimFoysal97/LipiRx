# v0.4.4 - Persistent Updates & In-App Update Engine Refinements

LipiRx v0.4.4 resolves application startup crashes, introduces persistent update dismissal preferences, enables inline update progress in Settings, and automatically cleans up downloaded update files.

### ✨ New
- **Persistent Update Dismissal**: Dismissing an update prompt now remembers your choice per version in database storage (`DISMISSED_UPDATE_VERSION`), suppressing repetitive launch popups while retaining notice cards in Settings.
- **Disk Cache Verification**: Automatically detects previously downloaded updates on disk, allowing immediate installation without re-downloading.

### 🚀 Improved
- **Inline Settings Update Flow**: Performing update checks or downloads from the Settings section displays progress, permissions, and install options entirely inline without pop-up dialogs.
- **Automatic Storage Cleanup**: Automatically removes temporary update installer files from device cache on app startup and before starting new downloads.
- **Complete Localization**: Externalized all update interface notices into string resources (`composeResources/values/strings.xml`) for full language support.

### 🐞 Fixed
- **App Startup Crash**: Resolved an initialization crash caused by missing startup database keep rules on release builds.
- **Re-Check Action Fix**: Ensured tapping "Check for updates" in Settings always performs a fresh update check even when an update is pending installation.

### 🛠 Under the hood
- Added ProGuard keep rules for Android App Startup (`androidx.startup`) and WorkManager database classes (`WorkDatabase_Impl`).
- Removed legacy `WorkManagerInitializer` manifest node (`tools:node="remove"`) to optimize cold app launch times.

### 📱 Platform notes
- **Android**: FileProvider and PackageInstaller flows updated for seamless background resume handling.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.4.3...v0.4.4
