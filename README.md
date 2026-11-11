# LipiRx - KMP Supervisor-Driven Multi-Agent AI Prescription Scanner

**LipiRx** is a modern Kotlin Multiplatform (KMP) and Compose Multiplatform mobile application designed for retail pharmacies in Bangladesh. It leverages a **Supervisor-Driven Multi-Agent AI System** to scan handwritten doctor prescriptions and automatically extract, normalize, enrich, and safety-audit medicine brand names, generic compositions, dosage schedules, and calculated quantities.

It features a **Multi-Provider AI Architecture** supporting **Google Gemini**, **Groq**, **OpenAI**, and **Custom OpenAI-Compatible Endpoints** (such as local Ollama or private LLM proxies).

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
- **Rule Fallback**: Includes a rule-based decision matrix if remote LLM calls fail or credentials are unconfigured.

### 2. Specialized Worker Agents
1. **`VisionTranscriptionAgent` (Stage 1)**: Encodes images to Base64 and executes Multimodal Vision OCR to extract raw handwriting.
2. **`MedicalEntityParserAgent` (Stage 2)**: Parses and normalizes medicine forms, strengths, units, schedules, and total unit calculations using structured `ParsedEntityDto` JSON.
3. **`GenericSubstituteAgent` (Stage 3)**: Cross-references DGDA active ingredients and matches 3–5 Bangladeshi brand substitutes (with manufacturer names) using `GenericSubstituteDto` JSON.
4. **`SafetyAuditorAgent` (Stage 4)**: Audits dosage safety, interaction risks, unreadable spellings, and sets pharmacist review flags using `SafetyAuditDto` JSON.

---

## 🛠️ Tech Stack & Architecture

- **Platforms**: Android & iOS
- **UI Framework**: Compose Multiplatform with Material 3 (adaptive layouts & custom themes)
- **Multi-Agent Pipeline**: `RxSupervisorAgent` + `MultiAgentRxOrchestrator` managing specialized `RxAgent` instances
- **Multi-Provider AI Architecture**: Strategy Pattern (`PrescriptionAiService` -> `GeminiAiService`, `GroqAiService`, `OpenAiAiService`, `CustomAiService`, managed by `CompositeAiService`)
- **Image Loading**: Coil 3 (`io.coil-kt.coil3:coil-compose`)
- **State Management**: MVVM + `StateFlow` + `Channel` + `kotlinx.collections.immutable` (`ImmutableList`, `ImmutableMap`)
- **Local Database**: SQLDelight 2.x SQLite database logging provider keys, per-provider models, agent matrix configurations, and offline prescription history
- **Networking**: Ktor Client 3.x (`OkHttp` for Android, `Darwin` for iOS) with `kotlinx.serialization`
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`)

---

## ✨ Core Features

1. **Supervisor-Driven Multi-Agent Orchestration**:
   - Configurable **Agent Settings Matrix** allowing developers to assign specific providers and models per agent (e.g. Gemini for OCR, Groq Llama-3.2 for Substitutes, OpenAI GPT-4o for Safety Audit).
   - Dynamic 5-step analysis UI (`LoadingView`) showing live Supervisor reasoning and agent progress checklist.

2. **Multi-Provider Vision AI Engine**:
   - **Google Gemini**: Dynamic model discovery via Gemini REST API (`gemini-2.0-flash`, `gemini-1.5-pro`, etc.).
   - **Groq**: High-speed Vision AI processing using Groq's multimodal models (`llama-3.2-90b-vision-instruct`, `llama-3.2-11b-vision-instruct`).
   - **OpenAI**: Integration with OpenAI Vision models (`gpt-4o`, `gpt-4o-mini`).
   - **Custom Endpoint**: Connect to local Ollama servers or private OpenAI-compatible proxy endpoints.

3. **Isolated Per-Provider Keys & Model Persistence**:
   - Each provider stores its own API key (`gemini_api_key`, `groq_api_key`, `openai_api_key`, `custom_api_key`) and selected model (`last_selected_model_gemini`, `last_selected_model_groq`, etc.) in SQLite.
   - Switching provider tabs isolatedly loads the saved model for that specific provider without cross-provider leakage.

4. **Safety & UX Controls**:
   - **Pharmacist Review Flags**: Visual review cards for low confidence, unreadable text, or dosage anomalies.
   - **Network & Token Fault Tolerance**: Graceful recovery and retry logic for server overload, network drops, or max token limits.
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
    │       │   │   └── DatabaseDriverFactory.kt     # SQLDelight database driver & agent config storage
    │       │   └── remote/
    │       │       ├── PrescriptionAiService.kt     # Core AI Service interface
    │       │       ├── GeminiAiService.kt           # Google Gemini Vision REST implementation
    │       │       ├── OpenAiCompatibleAiService.kt # Base class for OpenAI-compatible APIs
    │       │       ├── GroqAiService.kt             # Groq Vision REST implementation
    │       │       ├── OpenAiAiService.kt           # OpenAI Vision REST implementation
    │       │       ├── CustomAiService.kt           # Custom Endpoint REST implementation
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
    │       │   │   └── MultiAgentRxOrchestrator.kt  # Manager-driven orchestration loop
    │       │   └── model/
    │       │       ├── AiProvider.kt                # Sealed interface for AI Providers
    │       │       ├── MedicineItem.kt              # Prescription medicine model
    │       │       └── ProcessingStep.kt            # Analysis progress step enum
    │       └── presentation/
    │           └── scan/
    │               ├── ScanContract.kt              # ScanUiState & ScanUiEvent contracts
    │               ├── ScanViewModel.kt             # MVVM ViewModel managing scan lifecycle
    │               ├── ScanResultScreen.kt          # Main screen composable
    │               └── components/                  # UI View components (LoadingView, Cards, Matrix, etc.)
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

### 🔑 Setting Up AI Providers & Multi-Agent Matrix
1. Launch **LipiRx** on your Android device or iOS simulator.
2. Tap the **model pill** at the top right of the screen.
3. Configure your API keys for your preferred providers (**`🤖 Gemini`**, **`⚡ Groq`**, **`🌐 OpenAI`**, or **`🛠️ Custom URL`**).
4. Tap **`⚙️ Multi-Agent Settings`** to customize agent assignments and models for the Supervisor, Vision OCR, Entity Parser, Substitute Finder, and Safety Auditor.

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
