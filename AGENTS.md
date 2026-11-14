# LipiRx Agents Guide

General engineering standards come from the global CLAUDE.md and the `android-kotlin-standards` and `refactor` skills. This file covers LipiRx-specific rules only. Where this file conflicts with the global rules, this file wins, except security and data safety.

This is the engineering guide for developing, maintaining, and extending the multi-agent transcription, safety, and AgentOps pipeline in LipiRx.

---

## 1. Core Architecture & Supervisor Loop

LipiRx uses JetBrains Koog multi-agent architecture with a supervisor-driven execution loop. Instead of a rigid sequential pipeline, `KoogRxSupervisorEngine` coordinates turns with `RxSupervisorAgent`, inspecting the workspace state after each step, deciding which tool from `RxAgentTools` to invoke next, and deciding when the prescription is in a safe state for pharmacist review.

Every turn and tool execution is automatically logged to SQLite (`AgentExecutionTrace` table) for forensic AgentOps auditing and exposed via MCP (`lipirx://history/traces` and `get_agent_execution_traces`).

```
                  ┌────────────────────────┐
                  │   Prescription Image   │
                  └───────────┬────────────┘
                              ▼
                  ┌────────────────────────┐
                  │ KoogRxSupervisorEngine │ <─── (Dynamic Turn Loop, Max 6 Turns)
                  └───────────┬────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
    [executeVisionOCR] [executeEntityParsing] [executeSafetyAudit] ...
            │                 │                 │
            └─────────────────┼─────────────────┘
                              ▼
               ┌──────────────────────────────┐
               │  Pharmacist Approval Gate    │ (Required for High Risk / Low Confidence)
               └──────────────────────────────┘
```

### Execution flow and rules
1. **Entry point:** A prescription image (or raw capture) enters the pipeline via `ProcessPrescriptionUseCase` and `KoogRxSupervisorEngine` with the user's active `OnDeviceStrategy`.
2. **Supervisor control:** `KoogRxSupervisorEngine` and `RxSupervisorAgent` evaluate the current `SupervisorState` (transcription text, parsed medicine items, substitution matches, audit flags).
3. **Turn limit:** The supervisor runs within a strict maximum of 6 turns (`AppConstants.MAX_SUPERVISOR_TURNS`) to prevent infinite loops.
4. **AgentOps Telemetry:** Every supervisor decision and worker tool execution automatically persists to the `AgentExecutionTrace` table in SQLite.
5. **Suspension and gating:** If the safety auditor detects high-risk contraindications, unreadable or ambiguous dosages, or low confidence, execution suspends automatically at the Pharmacist Approval Gate. The gate can never be bypassed.

---

## 2. Agent & Tool Registry

All agent components live in the KMP shared module:
`shared/src/commonMain/kotlin/com/snehonir/lipirx/domain/agent/`

| Agent Enum ID | Class / Tool Name | Responsibility | Default Provider | Default Model |
| :--- | :--- | :--- | :--- | :--- |
| `SUPERVISOR` | `RxSupervisorAgent` & `KoogRxSupervisorEngine` | Evaluates workspace state, coordinates tool dispatches, enforces the Pharmacist Approval Gate | Gemini | `gemini-2.0-flash` |
| `TRANSCRIPTION` | `RxAgentTools.executeVisionOCR` | Vision OCR of handwritten prescriptions: raw text, brand names, dosages | Gemini | `gemini-2.0-flash` |
| `ENTITY_PARSER` | `RxAgentTools.executeEntityParsing` | Parses raw OCR text into structured `MedicineItem` (brand, generic, dosage, strength, form, schedule) | Gemini | `gemini-1.5-pro` |
| `GENERIC_SUBSTITUTE` | `RxAgentTools.executeGenericSubstituteSearch` | Identifies active generic salts and queries Bangladeshi brand substitutes | Groq | `llama-3.2-90b-vision-instruct` |
| `SAFETY_AUDITOR` | `RxAgentTools.executeSafetyAudit` | Audits dosages, flags ambiguous or unreadable entries, evaluates drug-drug contraindications, sets clinical warnings | OpenAI | `gpt-4o-mini` |
| `--` | `McpToolHandler.get_agent_execution_traces` | Queries AgentOps SQLite execution traces for offline debugging & telemetry | -- | -- |

---

## 3. AI Execution Strategies (`OnDeviceStrategy`)

- `HYBRID_FALLBACK` (default): cloud first, automatic on-device LiteRT-LM fallback on network error or offline.
- `ON_DEVICE_FIRST`: local SLM first, cloud escalation for complex safety verification.
- `ON_DEVICE_ONLY`: 100% offline, zero cloud calls.
- `CLOUD_ONLY`: on-device SLM disabled.

Providers (`AiProvider`): `gemini`, `groq`, `openai`, `custom` (OpenAI-compatible endpoint such as Ollama, LocalAI, vLLM), `on_device` (LiteRT-LM `.litertlm`).

---

## 4. Model Context Protocol (MCP) & AgentOps Exposure

LipiRx includes an embedded HTTP MCP Server running on port `8080` (`http://localhost:8080/lipirx/mcp`).

### Exposed MCP Resources:
- `lipirx://history/scans`: Prescription scan history and medicine items.
- `lipirx://config/providers`: Active provider configurations and model selections.
- `lipirx://history/traces`: AgentOps SQLite forensic execution traces.

### Exposed MCP Tools:
- `scan_prescription_vision_ocr`
- `parse_medical_entities`
- `search_generic_substitutes`
- `audit_patient_safety`
- `get_agent_execution_traces`

---

## 5. Adding a New Tool / Agent Capability

Follow these 4 steps.

### Step 1: Register in `RxAgentType.kt`
`shared/src/commonMain/kotlin/com/snehonir/lipirx/domain/agent/RxAgentType.kt`
```kotlin
enum class RxAgentType(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultProvider: AiProvider,
    val defaultModel: String
) {
    // ... existing agents ...

    CUSTOM_SKILL(
        id = "custom_skill",
        displayName = "5. Custom Medical Skill",
        description = "Specialized clinical compliance and interaction audit",
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-2.0-flash"
    )
}
```

### Step 2: Implement Tool in `RxAgentTools.kt`
Add the function to `RxAgentTools`:
```kotlin
suspend fun executeCustomSkill(
    items: List<MedicineItem>,
    params: ToolExecutionParams
): Result<List<MedicineItem>> {
    // 1. Perform tool processing
    // 2. Return updated items without inventing clinical values
    return Result.success(items)
}
```

### Step 3: Wire into `KoogRxSupervisorEngine.kt`
Add a branch in `executeToolForAgent(...)`. The `when` is exhaustive over `RxAgentType`, so the compiler flags any missing branch.
```kotlin
private suspend fun executeToolForAgent(
    targetAgentType: RxAgentType,
    input: TranscriptionInput,
    items: List<MedicineItem>,
    params: ToolExecutionParams
): Result<List<MedicineItem>> =
    when (targetAgentType) {
        RxAgentType.SUPERVISOR -> Result.success(items)
        RxAgentType.TRANSCRIPTION -> rxAgentTools.executeVisionOCR(input)
        RxAgentType.ENTITY_PARSER -> rxAgentTools.executeEntityParsing(items, params)
        RxAgentType.GENERIC_SUBSTITUTE -> rxAgentTools.executeGenericSubstituteSearch(items, params)
        RxAgentType.SAFETY_AUDITOR -> rxAgentTools.executeSafetyAudit(items, params)
        RxAgentType.CUSTOM_SKILL -> rxAgentTools.executeCustomSkill(items, params)
    }
```

### Step 4: UI (automatic)
`ModelSelectionBottomSheet.kt` iterates over `RxAgentType.entries` and renders provider and model selection per agent tool. No manual UI change is needed.

---

## 6. Error Handling and Failure Propagation

- **No silent fallbacks.** Never use `getOrDefault` or similar with fake clinical values (for example a dosage of `1 + 0 + 1` or a quantity of `10`).
- **Explicit failures.** On a parsing error, network timeout, or rate limit (HTTP 429), return `Result.failure(...)` with a descriptive `IllegalStateException` or a custom domain exception.
- **Cancellation.** Never swallow `CancellationException`.
- **Graceful degradation.** `KoogRxSupervisorEngine` captures `Result.failure`, records it in the execution history and `AgentExecutionTrace` table, then triggers the Hybrid Fallback strategy or surfaces the error to the UI.

---

## 7. Clinical Safety & Data Invariants (absolute, never weaken, including during refactors)

1. **Pharmacist Approval Gate.** Never bypassed, disabled, or made optional. Any prescription flagged with warnings requires active pharmacist sign-off.
2. **Zero hallucination.** If parsing fails or text is unreadable, never invent dosage schedules or tablet quantities. Leave fields empty or unknown, or fail explicitly.
3. **Preserve safety flags.** Safety Auditor warnings, low-confidence scores (`confidence = "low"`), and unreadable markers are never silently downgraded, defaulted, or overwritten.
4. **Privacy and offline isolation.** In `ON_DEVICE_ONLY` mode, all agent execution is local via LiteRT-LM with zero external network calls. Patient images and data never leave configured user boundaries.
5. Never log prescription images or patient data.
6. Never hard-code or commit API keys; read them from secure config.
7. Do not change the supervisor turn limit or approval thresholds without explicit approval.
8. Dosage parsing, approval gate logic, and safety checks must have unit tests, even if tests are otherwise out of scope.

---

## 8. Compose Guardrails

`DropdownMenu`, `ModalBottomSheet`, and any container using `Modifier.height(IntrinsicSize.Min)` MUST NOT contain `SubcomposeLayout` components (`LazyColumn`, `LazyRow`, `BoxWithConstraints`, `TabRow`).
- Scrollable menus: plain `Column` with `verticalScroll(rememberScrollState())` and `heightIn(max = 220.dp)`.
- Auto-scroll to the selected item: record its offset with `Modifier.onGloballyPositioned { coordinates -> selectedY = coordinates.positionInParent().y.toInt() }`, then call `scrollState.scrollTo(...)`.

---

## 9. Project Rules

- Domain models and agent implementations live in `:shared` (`commonMain`).
- Use the `Lipi` design system in `com.snehonir.lipirx.ui.theme`. Never create a second theme, palette, or typography.
- No blocking network or DB calls on the main thread; use injected dispatchers (`Dispatchers.IO` / `Dispatchers.Default` in production wiring).

---

## 10. Build & Verify

- Build: `./gradlew :shared:build` and `./gradlew :composeApp:assembleDebug`
- Tests: `./gradlew :shared:allTests`
- Run build and tests after changes and report the actual results.
