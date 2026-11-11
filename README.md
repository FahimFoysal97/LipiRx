# LipiRx - KMP Supervisor-Driven Multi-Agent AI Prescription Scanner

**LipiRx** is a modern Kotlin Multiplatform (KMP) and Compose Multiplatform mobile application designed for retail pharmacies in Bangladesh. It leverages a **Supervisor-Driven Multi-Agent AI System** to scan handwritten doctor prescriptions and automatically extract, normalize, enrich, and safety-audit medicine brand names, generic compositions, dosage schedules, and calculated quantities.

It features a **Multi-Provider AI Architecture** supporting **Google Gemini**, **Groq**, **OpenAI**, **Custom OpenAI-Compatible Endpoints** (such as local Ollama or private LLM proxies), and **Edge On-Device SLM (LiteRT-LM)** for 100% offline pharmacy operations.

---

## 🤖 Multi-Agent Architecture

Unlike fixed single-prompt systems, LipiRx uses an autonomous **Manager/Supervisor Multi-Agent Pipeline** where a master LLM agent dynamically evaluates workspace state, routes tasks to specialized worker agents, passes feedback prompts, and controls pipeline execution.

```
                      ┌─────────────────────────────────┐
                      │        RxSupervisorAgent        │
                      │    (Manager / Supervisor LLM)   │
                      └────────────────┬────────────────┘
                                       │
     Evaluates SupervisorState (turn count, medicines in memory, execution history)
     & dynamically decides: Next Agent, Reasoning, and Feedback Prompt instructions
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
[VisionTranscriptionAgent]   [MedicalEntityParserAgent]   [GenericSubstituteAgent]   [SafetyAuditorAgent]
```

### 1. Supervisor Manager Agent (`RxSupervisorAgent`)
- **Role**: Evaluates the workspace state (`SupervisorState`) on each turn.
- **Dynamic Decision Loop**: Prompts the assigned Manager LLM (e.g. Gemini 2.0 Flash / GPT-4o) to decide which agent executes next (`TRANSCRIPTION`, `ENTITY_PARSER`, `GENERIC_SUBSTITUTE`, `SAFETY_AUDITOR`, or `FINISH`), generating detailed reasoning and custom `feedbackPrompt` instructions.
- **Rule Fallback & Guardrails**: Includes robust decision fallbacks and guardrails preventing premature finishing when workspace items are empty.

### 2. Specialized Worker Agents
1. **`VisionTranscriptionAgent` (Stage 1)**: Encodes images to Base64 and executes Multimodal Vision OCR to extract raw handwriting.
2. **`MedicalEntityParserAgent` (Stage 2)**: Parses and normalizes medicine forms, strengths, units, schedules, and total unit calculations using structured `ParsedEntityDto` JSON.
3. **`GenericSubstituteAgent` (Stage 3)**: Cross-references DGDA active ingredients and matches 3–5 Bangladeshi brand substitutes (with manufacturer names) using `GenericSubstituteDto` JSON.
4. **`SafetyAuditorAgent` (Stage 4)**: Audits dosage safety, interaction risks, unreadable spellings, and sets pharmacist review flags using `SafetyAuditDto` JSON.

---

## 📱 Edge On-Device AI & LiteRT-LM Support

LipiRx includes native **On-Device SLM** execution using Google's **LiteRT-LM** (`litertlm-android`) and LiteRT runtime (`.litertlm` and `.tflite` model files).

### On-Device Execution Strategies
- **Hybrid Fallback** *(Default)*: Cloud AI First $\rightarrow$ Automatic On-Device SLM Fallback on network drops, server errors, or API quota limits.
- **On-Device First**: Local NPU First $\rightarrow$ Cloud Escalation for complex safety auditing.
- **On-Device Only**: Strict 100% Offline / Local Execution Only.
- **Cloud Only**: Disable On-Device SLM $\rightarrow$ Cloud AI Only.

---

## 🛠️ Tech Stack & Architecture

- **Platforms**: Android & iOS
- **UI Framework**: Compose Multiplatform with Material 3 (adaptive layouts & custom themes)
- **Multi-Agent Pipeline**: `RxSupervisorAgent` + `MultiAgentRxOrchestrator` managing specialized `RxAgent` instances
- **Multi-Provider AI Architecture**: Strategy Pattern (`PrescriptionAiService` -> `GeminiAiService`, `GroqAiService`, `OpenAiAiService`, `CustomAiService`, `OnDeviceAiService`, managed by `CompositeAiService`)
- **On-Device LLM Runtime**: Google LiteRT-LM (`com.google.ai.edge.litertlm`) and LiteRT CompiledModel
- **Image Loading**: Coil 3 (`io.coil-kt.coil3:coil-compose`)
- **State Management**: MVVM + `StateFlow` + `Channel` + `kotlinx.collections.immutable` (`ImmutableList`, `ImmutableMap`)
- **Local Database**: SQLDelight 2.x SQLite database logging provider keys, per-provider models, agent matrix configurations, on-device strategy, and offline prescription history
- **Networking**: Ktor Client 3.x (`OkHttp` for Android, `Darwin` for iOS) with `kotlinx.serialization`
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`)

---

## ✨ Core Features

1. **Supervisor-Driven Multi-Agent Orchestration**:
   - Configurable **Agent Settings Matrix** allowing developers to assign specific providers and models per agent (including `📱 On-Device SLM`).
   - Dynamic 5-step analysis UI (`LoadingView`) showing live Supervisor reasoning, progress checklist, and per-agent execution timing logs.

2. **Multi-Provider & Edge Vision AI Engine**:
   - **Google Gemini**: Dynamic model discovery via Gemini REST API (`gemini-2.0-flash`, etc.).
   - **Groq**: High-speed Vision AI processing (`llama-3.2-90b-vision-instruct`, etc.).
   - **OpenAI**: Integration with OpenAI Vision models (`gpt-4o`, etc.).
   - **Custom Endpoint**: Connect to local Ollama servers or private OpenAI-compatible proxy endpoints.
   - **On-Device SLM**: Local offline inference via LiteRT-LM and `.litertlm` weight files stored on device storage.

3. **Scan History & Local Storage Management**:
   - SQLDelight SQLite storage saving prescription scan history and cross-session episodic dispensing memory.
   - Interactive `HistoryScreen` supporting single-item deletion (with confirmation dialog) and bulk **Clear All** removal.

4. **Safety & UX Controls**:
   - **Pharmacist Review Flags**: Visual review cards for low confidence, unreadable text, or dosage anomalies.
   - **Scrollable & Expandable Error Handling**: Image preview view with vertical scrolling and collapsible error notices.
   - **POS Actions**: One-click summary copying to clipboard for quick pasting into pharmacy POS software.

---

## 📂 Project Structure

```
.
├── androidApp/                                      # Android Application entry point
│   ├── src/
│   │   ├── MainActivity.kt                          # ComponentActivity displaying App()
│   │   ├── LipiRxApplication.kt                     # Application class initializing Koin
│   │   └── AndroidManifest.xml                      # Includes OpenCL & Vndk native library declarations
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
    │       │   │   └── DatabaseDriverFactory.kt     # SQLDelight database driver & agent/strategy config storage
    │       │   └── remote/
    │       │       ├── PrescriptionAiService.kt     # Core AI Service interface
    │       │       ├── GeminiAiService.kt           # Google Gemini Vision REST implementation
    │       │       ├── OpenAiCompatibleAiService.kt # Base class for OpenAI-compatible APIs
    │       │       ├── GroqAiService.kt             # Groq Vision REST implementation
    │       │       ├── OpenAiAiService.kt           # OpenAI Vision REST implementation
    │       │       ├── CustomAiService.kt           # Custom Endpoint REST implementation
    │       │       ├── OnDeviceAiService.kt         # On-Device SLM service implementation
    │       │       ├── OnDeviceLlmEngine.kt         # LiteRT-LM expect/actual engine interface
    │       │       ├── CompositeAiService.kt        # Polymorphic AI provider router
    │       │       └── Prompts.kt                   # Vision prompt engineering
    │       ├── di/
    │       │   └── KoinModule.kt                    # Koin 4.x dependency injection module
    │       ├── domain/
    │       │   ├── agent/
    │       │   │   ├── RxAgent.kt                   # Base Agent interface
    │       │   │   ├── RxAgentType.kt               # Agent type enum & default configs
    │       │   │   ├── RxSupervisorAgent.kt         # Master Supervisor LLM Agent
    │       │   │   ├── VisionTranscriptionAgent.kt  # Stage 1 Multimodal Vision OCR Agent
    │       │   │   ├── MedicalEntityParserAgent.kt  # Stage 2 Medical Entity Parser Agent
    │       │   │   ├── GenericSubstituteAgent.kt    # Stage 3 Generic & Substitute Agent
    │       │   │   ├── SafetyAuditorAgent.kt        # Stage 4 Patient Safety Auditor Agent
    │       │   │   └── MultiAgentRxOrchestrator.kt  # Manager-driven orchestration loop with timing logs
    │       │   └── model/
    │       │       ├── AiProvider.kt                # Sealed interface for AI Providers (including OnDevice)
    │       │       ├── OnDeviceStrategy.kt          # On-device execution strategy enum
    │       │       ├── MedicineItem.kt              # Prescription medicine model
    │       │       └── ProcessingStep.kt            # Analysis progress step enum
    │       └── presentation/
    │           └── scan/
    │               ├── ScanContract.kt              # ScanUiState & ScanUiEvent contracts
    │               ├── ScanViewModel.kt             # MVVM ViewModel managing scan lifecycle
    │               ├── ScanResultScreen.kt          # Main screen composable with back handlers
    │               └── components/                  # UI View components (HistoryScreen, LoadingView, Cards, Matrix, etc.)
    ├── src@android/                                 # Android-specific implementations
    │   └── com/snehonir/lipirx/
    │       ├── data/local/DatabaseDriverFactory.android.kt
    │       ├── data/remote/OnDeviceLlmEngine.android.kt # Android LiteRT-LM Engine implementation
    │       └── di/PlatformModule.android.kt
    └── src@ios/                                     # iOS-specific implementations
        └── com/snehonir/lipirx/
            ├── data/local/DatabaseDriverFactory.ios.kt
            ├── data/remote/OnDeviceLlmEngine.ios.kt     # iOS stub implementation
            └── di/PlatformModule.ios.kt
```

---

## 🚀 Getting Started

### 🔑 Setting Up AI Providers & On-Device Strategy
1. Launch **LipiRx** on your Android device.
2. Tap the **AI Pipeline & Engine Studio** configuration card on the home or preview screen.
3. Configure your API keys for your preferred cloud providers (**`🤖 Gemini`**, **`⚡ Groq`**, **`🌐 OpenAI`**, or **`🛠️ Custom URL`**) or select your **On-Device AI & Fallback Strategy** (**`Hybrid Fallback`**, **`On-Device First`**, **`On-Device Only`**, or **`Cloud Only`**).
4. Customize agent assignments and models in the **Per-Agent AI Assignments** matrix.
5. Easily navigate back using the topbar back button on both the prescription preview and results screens.

### 📱 Installing Local SLM Weights (Optional for Offline Mode)
To run local models offline:
1. Download a `.litertlm` model file (e.g., `Qwen3-0.6B.litertlm` from Hugging Face `litert-community`).
2. Push the file to your device storage via `adb`:
   ```bash
   adb push Qwen3-0.6B.litertlm /sdcard/Android/data/com.snehonir.lipirx/files/model.litertlm
   ```

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
