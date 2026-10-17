# Lipi Rx - KMP Vision AI Prescription Scanner

**Lipi Rx** is a Kotlin Multiplatform (KMP) and Compose Multiplatform mobile application designed for retail pharmacies in Bangladesh. It leverages Vision AI (**Google Gemini 1.5 Pro**) to scan handwritten doctor prescriptions and automatically extract medicine brand names, local dosage schedules, and calculated tablet quantities.

---

## 🛠️ Tech Stack & Architecture

- **Platforms**: Android & iOS
- **UI Framework**: Compose Multiplatform with Material 3
- **Navigation**: Navigation Compose KMP (`org.jetbrains.androidx.navigation:navigation-compose`)
- **Image Loading**: Coil3 (`io.coil-kt.coil3:coil-compose`)
- **QR Code Generation**: `qrose` (`io.github.alexzhirkevich:qrose`)
- **State Management**: MVVM + `StateFlow` + `Channel` (one-time UI events) + `kotlinx.collections.immutable` (`ImmutableList`)
- **Local Database**: SQLDelight 2.x (`AndroidSqliteDriver` for Android, `NativeSqliteDriver` for iOS)
- **Networking**: Ktor Client 3.x (`OkHttp` for Android, `Darwin` for iOS) with `kotlinx.serialization`
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`)

---

## ✨ Core Features

1. **Vision AI Prescription Analysis**:
   - Encodes prescription images to Base64 and issues structured requests to the **Gemini 1.5 Pro REST API**.
   - Uses localized system prompt engineering tailored to Bangladeshi pharmacy shorthand (`1 + 0 + 1 (ac)`, `1 + 1 + 1 (pc)`, etc.).

2. **Structured Extraction**:
   - `medicine_name`: Full brand name, strength, and form (e.g., *Sergel 20 mg (Cap)*).
   - `schedule`: Dosage schedule shorthand (e.g., *1 + 0 + 1 (ac)*).
   - `total_tablets`: Total calculated quantity as an integer.

3. **POS Integration & Actions**:
   - **Copy to Clipboard**: Formats extracted list into plain text for quick pasting into pharmacy management software.
   - **QR Code Generation**: Interactive QR code rendered via `qrose` inside a Material 3 `ModalBottomSheet` for instant scanner integration.

4. **Local Storage**:
   - SQLDelight 2.x SQLite database logging for offline prescription history tracking.

---

## 📂 Project Structure

```
.
├── androidApp/                                      # Android Application entry point
│   ├── src/
│   │   ├── MainActivity.kt                          # ComponentActivity displaying App()
│   │   ├── LipiRxApplication.kt                     # Application class initializing Koin
│   │   └── AndroidManifest.xml
│   └── module.yaml
│
├── iosApp/                                          # iOS Application entry point
│   ├── src/
│   │   ├── iosApp.swift                             # SwiftUI App entry point initializing Koin
│   │   └── ViewController.kt                        # ComposeUIViewController wrapper
│   └── module.yaml
│
└── shared/                                          # KMP Shared Module
    ├── src/                                         # Common Kotlin code
    │   ├── App.kt                                   # Main Compose entry point & KoinContext wrapper
    │   └── com/snehonir/lipirx/
    │       ├── data/
    │       │   ├── remote/
    │       │   │   └── GeminiService.kt             # Ktor 3.x Gemini 1.5 Pro REST client
    │       │   └── local/
    │       │       └── DatabaseDriverFactory.kt     # SQLDelight database driver factory (expect)
    │       ├── di/
    │       │   └── KoinModule.kt                    # Koin 4.x dependency injection modules
    │       ├── domain/
    │       │   └── model/
    │       │       └── MedicineItem.kt              # @Serializable @Immutable DTO
    │       └── presentation/
    │           └── scan/
    │               ├── ScanContract.kt              # ScanUiState & ScanUiEvent contracts
    │               ├── ScanViewModel.kt             # MVVM ViewModel using StateFlow & Channel
    │               └── ScanResultScreen.kt          # Material 3 UI & QR Code sheet
    ├── src@android/                                 # Android-specific implementations
    │   └── com/snehonir/lipirx/
    │       ├── data/local/DatabaseDriverFactory.android.kt
    │       └── di/PlatformModule.android.kt
    └── src@ios/                                     # iOS-specific implementations
        └── com/snehonir/lipirx/
            ├── data/local/DatabaseDriverFactory.ios.kt
            └── di/PlatformModule.ios.kt
```

---

## 🚀 Getting Started

### 🔑 Gemini API Key Configuration
Pass your Gemini API Key into `GeminiService` when initializing or update the default key constant in `GeminiService.kt`:
```kotlin
class GeminiService(
    private val httpClient: HttpClient,
    private val apiKey: String = "YOUR_GEMINI_API_KEY"
)
```

### 🔨 Building & Running

The project includes CLI wrappers `kotlin` (macOS/Linux) and `kotlin.bat` (Windows):

- **Build shared library**:
  ```bash
  ./kotlin task :shared:compileAndroidDebug
  ```

- **Run Android App**:
  ```bash
  ./kotlin run -m androidApp
  ```

- **Run iOS App**:
  ```bash
  ./kotlin run -m iosApp
  ```
