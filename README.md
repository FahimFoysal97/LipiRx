# LipiRx - KMP Multi-Provider Vision AI Prescription Scanner

**LipiRx** is a modern Kotlin Multiplatform (KMP) and Compose Multiplatform mobile application designed for retail pharmacies in Bangladesh. It leverages Vision AI to scan handwritten doctor prescriptions and automatically extract medicine brand names, generic compositions, local dosage schedules, and calculated tablet quantities.

It features a **Multi-Provider AI Engine** supporting **Google Gemini**, **Groq**, **OpenAI**, and **Custom OpenAI-Compatible Endpoints** (such as local Ollama or private LLM proxies).

---

## 🛠️ Tech Stack & Architecture

- **Platforms**: Android & iOS
- **UI Framework**: Compose Multiplatform with Material 3 (adaptive layouts & custom themes)
- **Multi-Provider AI Architecture**: Object-Oriented Strategy Pattern (`PrescriptionAiService` -> `GeminiAiService`, `GroqAiService`, `OpenAiAiService`, `CustomAiService`, managed by `CompositeAiService`)
- **Image Loading**: Coil 3 (`io.coil-kt.coil3:coil-compose`)
- **State Management**: MVVM + `StateFlow` + `Channel` + `kotlinx.collections.immutable` (`ImmutableList`)
- **Local Database**: SQLDelight 2.x SQLite database logging for provider keys, settings, and offline prescription history
- **Networking**: Ktor Client 3.x (`OkHttp` for Android, `Darwin` for iOS) with `kotlinx.serialization`
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`)

---

## ✨ Core Features

1. **Multi-Provider Vision AI Engine**:
   - **Google Gemini**: Dynamic model discovery via Gemini REST API (`gemini-2.0-flash`, `gemini-1.5-pro`, etc.).
   - **Groq**: High-speed Vision AI processing using Groq's multimodal models (`llama-3.2-90b-vision-instruct`, `llama-3.2-11b-vision-instruct`).
   - **OpenAI**: Direct integration with OpenAI Vision models (`gpt-4o`, `gpt-4o-mini`).
   - **Custom Endpoint**: Connect to local Ollama servers or private OpenAI-compatible proxy endpoints.

2. **Isolated Key Management & Provider Switching**:
   - Each provider stores its own separate API key in SQLite (`gemini_api_key`, `groq_api_key`, `openai_api_key`, `custom_api_key`).
   - Switch between providers with 1 tap in the settings sheet without losing previously entered keys.

3. **Two-Pass Localized Extraction**:
   - **Pass 1 (Transcription)**: Literal handwriting transcription of prescribed medicine names, forms, and schedules.
   - **Pass 2 (Enrichment)**: Local product matching against DGDA-registered Bangladeshi pharmaceuticals, generic compositions, and similar brand alternatives.

4. **Safety & UX Controls**:
   - **Clipped Accent Cards**: Visual review flags for unreadable or assumed strength items.
   - **Network & Token Fault Tolerance**: Graceful recovery and retry screen for server overload, network drops, or max token limits.
   - **POS Actions**: One-click summary copying to clipboard for quick pasting into pharmacy POS software.

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
    │       │   ├── local/
    │       │   │   └── DatabaseDriverFactory.kt     # SQLDelight database driver & config storage
    │       │   └── remote/
    │       │       ├── PrescriptionAiService.kt     # Core AI Service interface
    │       │       ├── GeminiAiService.kt           # Google Gemini Vision REST implementation
    │       │       ├── OpenAiCompatibleAiService.kt # Base class for OpenAI-compatible APIs
    │       │       ├── GroqAiService.kt             # Groq Vision REST implementation
    │       │       ├── OpenAiAiService.kt           # OpenAI Vision REST implementation
    │       │       ├── CustomAiService.kt           # Custom Endpoint REST implementation
    │       │       ├── CompositeAiService.kt        # Polymorphic AI provider router
    │       │       └── Prompts.kt                   # Two-pass prompt engineering
    │       ├── di/
    │       │   └── KoinModule.kt                    # Koin 4.x dependency injection module
    │       ├── domain/
    │       │   └── model/
    │       │       ├── AiProvider.kt                # Sealed interface for AI Providers
    │       │       └── MedicineItem.kt              # Prescription medicine model
    │       └── presentation/
    │           └── scan/
    │               ├── ScanContract.kt              # ScanUiState & ScanUiEvent contracts
    │               ├── ScanViewModel.kt             # MVVM ViewModel managing scan lifecycle
    │               ├── ScanResultScreen.kt          # Main screen composable
    │               └── components/                  # UI View components (Cards, BottomSheet, etc.)
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

### 🔑 Setting Up AI Providers in App
1. Launch **LipiRx** on your Android device or iOS simulator.
2. Tap the **model pill** at the top right of the screen.
3. Select your provider tab (**`🤖 Gemini`**, **`⚡ Groq`**, **`🌐 OpenAI`**, or **`🛠️ Custom URL`**).
4. Paste your API Key and tap **`🔑 Save Provider Config`**.

### 🔨 Building & Running

The project includes CLI wrappers `kotlin` (macOS/Linux) and `kotlin.bat` (Windows):

- **Build shared module**:
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
