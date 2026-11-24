# LipiRx v0.6.2 - Advanced Update Engine, Hero Animations & Inline Settings UX

LipiRx v0.6.2 introduces a polished in-app update experience with custom animated vector hero icons, robust download staging, and seamless inline update management directly within the Settings view.

### ✨ New
- **`UpdateHeroIcon` Animation Suite**: Custom Compose Canvas-based vector illustrations featuring spinning loading rings, floating clouds, pulsing download arrows, checkmark reveals, and error shakes for all update lifecycle states.
- **Inline Settings Update Management**: Enhanced manual update checking and inline status presentation in the Settings About section with automatic scroll-into-view behavior via `BringIntoViewRequester`.

### 🚀 Improved
- **Staged Update Confirmation**: Refactored `UpdateManager` to transition completed downloads to `ReadyToInstall` without immediately launching the package installer, allowing users to initiate installation at their convenience.
- **Optimized Dialog & Window Transitions**: Streamlined `UpdateDialog` and `UpdateContent` layout transitions to prevent window jumping and visual jitter.

### 🐞 Fixed
- **Frame-Synchronized Layout Scrolling**: Fixed frame-timing synchronization for auto-scrolling update check results into view in the Settings screen.

### 🛠 Under the hood
- Refactored `UpdateManager` coroutine lifecycle handling and download state flows.
- Enhanced semantic descriptions and accessibility live regions for update progress announcements.

**Full Changelog**: https://github.com/FahimFoysal97/LipiRx/compare/v0.6.1...v0.6.2
