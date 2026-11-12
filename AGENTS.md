# LipiRx Agents Guide

General engineering standards come from the global CLAUDE.md and the `android-kotlin-standards` and `refactor` skills. This file covers LipiRx-specific rules only. Where this file conflicts with the global rules, this file wins, except security and data safety.

This is the engineering guide for developing, maintaining, and extending the multi-agent transcription and safety pipeline in LipiRx.

---

## 1. Core Architecture & Supervisor Loop

LipiRx uses a supervisor-driven multi-agent architecture. Instead of a rigid sequential pipeline, a central `RxSupervisorAgent` inspects the workspace state after each step, decides which worker agent to invoke next, and decides when the prescription is in a safe state for pharmacist review.

```
                  ┌────────────────────────┐
                  │   Prescription Image   │
                  └───────────┬────────────┘
                              ▼
                  ┌────────────────────────┐
                  │   RxSupervisorAgent    │ <─── (Dynamic Turn Loop, Max 6 Turns)
                  └───────────┬────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
     [TRANSCRIPTION]   [ENTITY_PARSER]   [SAFETY_AUDITOR] ...
            │                 │                 │
            └─────────────────┼─────────────────┘
                              ▼
               ┌──────────────────────────────┐
               │  Pharmacist Approval Gate    │ (Required for High Risk / Low Confidence)
               └──────────────────────────────┘
```

### Execution flow and rules
1. **Entry point:** A prescription image (or raw capture) enters the pipeline with the user's active `OnDeviceStrategy`.
2. **Supervisor control:** `RxSupervisorAgent` evaluates the current `RxWorkspaceState` (transcription text, parsed medicine items, substitution matches, audit flags).
3. **Turn limit:** The supervisor runs within a strict maximum of 6 turns to prevent infinite loops.
4. **Suspension and gating:** If the safety auditor detects high-risk contraindications, unreadable or ambiguous dosages, or low confidence, execution suspends automatically at the Pharmacist Approval Gate. The gate can never be bypassed.

---

## 2. Agent Registry

All agents live in the KMP shared module:
`shared/src/commonMain/kotlin/com/snehonir/lipirx/domain/agent/`

| Agent Enum ID | Class Name | Responsibility | Default Provider | Default Model |
| :--- | :--- | :--- | :--- | :--- |
| `SUPERVISOR` | `RxSupervisorAgent` | Evaluates workspace state, coordinates dispatch, enforces the Pharmacist Approval Gate | Gemini | `gemini-2.0-flash` |
| `TRANSCRIPTION` | `TranscriptionAgent` | Vision OCR of handwritten prescriptions: raw text, brand names, dosages | Gemini | `gemini-2.0-flash` |
| `ENTITY_PARSER` | `EntityParserAgent` | Parses raw OCR text into structured `MedicineItem` (brand, generic, dosage, strength, form, schedule) | Gemini | `gemini-1.5-pro` |
| `GENERIC_SUBSTITUTE` | `GenericSubstituteAgent` | Identifies active generic salts and queries the local SQLite database for verified Bangladeshi brand substitutes | Groq | `llama-3.1-8b-instant` |
| `SAFETY_AUDITOR` | `SafetyAuditorAgent` | Audits dosages, flags ambiguous or unreadable entries, evaluates drug-drug contraindications, sets clinical warnings | OpenAI | `gpt-4o-mini` |

---

## 3. AI Execution Strategies (`OnDeviceStrategy`)

- `HYBRID_FALLBACK` (default): cloud first, automatic on-device LiteRT-LM fallback on network error or offline.
- `ON_DEVICE_FIRST`: local SLM first, cloud escalation for complex safety verification.
- `ON_DEVICE_ONLY`: 100% offline, zero cloud calls.
- `CLOUD_ONLY`: on-device SLM disabled.

Providers (`AiProvider`): `gemini`, `groq`, `openai`, `custom` (OpenAI-compatible endpoint such as Ollama, LocalAI, vLLM), `on_device` (LiteRT-LM `.litertlm`).

---

## 4. The `RxAgent` Contract

Every agent implements the polymorphic `RxAgent` interface, which gives uniform orchestration, testability, and error handling across local and cloud providers.

```kotlin
interface RxAgent {
    suspend fun execute(state: RxWorkspaceState): Result<RxWorkspaceState>
}
```

### Error handling and failure propagation
- **No silent fallbacks.** Never use `getOrDefault` or similar with fake clinical values (for example a dosage of `1 + 0 + 1` or a quantity of `10`).
- **Explicit failures.** On a parsing error, network timeout, or rate limit (HTTP 429), return `Result.failure(...)` with a descriptive `IllegalStateException` or a custom domain exception.
- **Cancellation.** Never swallow `CancellationException`. `runCatching` catches it, so rethrow it.
- **Graceful degradation.** The orchestrator captures `Result.failure`, records it in the audit trail, then triggers the Hybrid Fallback strategy or surfaces the error to the UI.

---

## 5. Adding a New Agent

Follow these 5 steps.

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

### Step 2: Implement the agent
`shared/src/commonMain/kotlin/com/snehonir/lipirx/domain/agent/CustomSkillAgent.kt`
```kotlin
class CustomSkillAgent(
    private val database: LipiRxLocalDatabase
) : RxAgent {
    override suspend fun execute(state: RxWorkspaceState): Result<RxWorkspaceState> =
        runCatching {
            // 1. Perform agent-specific processing
            // 2. Validate data without inventing clinical values
            // 3. Return updated state immutably
            state.copy(
                auditTrail = state.auditTrail + AgentExecutionRecord("CustomSkillAgent", success = true)
            )
        }.recoverCatching { error ->
            if (error is CancellationException) throw error
            throw IllegalStateException("CustomSkillAgent execution failed: ${error.message}", error)
        }
}
```
All business logic stays in `commonMain` so it compiles for every target.

### Step 3: Wire into `MultiAgentRxOrchestrator.kt`
Inject the agent and add a branch in `dispatchToWorkerAgent(...)`. The `when` is exhaustive over `RxAgentType`, so the compiler flags any missing branch. No other dispatch sites need changes.
```kotlin
private suspend fun dispatchToWorkerAgent(
    agentType: RxAgentType,
    state: RxWorkspaceState
): Result<RxWorkspaceState> =
    when (agentType) {
        RxAgentType.SUPERVISOR -> Result.success(state) // handled by the supervisor loop
        RxAgentType.TRANSCRIPTION -> transcriptionAgent.execute(state)
        RxAgentType.ENTITY_PARSER -> entityParserAgent.execute(state)
        RxAgentType.GENERIC_SUBSTITUTE -> genericSubstituteAgent.execute(state)
        RxAgentType.SAFETY_AUDITOR -> safetyAuditorAgent.execute(state)
        RxAgentType.CUSTOM_SKILL -> customSkillAgent.execute(state)
    }
```

### Step 4: Update `RxSupervisorAgent.kt`
Update the supervisor system prompt and decision logic so it knows when to invoke `CUSTOM_SKILL`.

### Step 5: UI (automatic)
`ModelSelectionBottomSheet.kt` iterates over `RxAgentType.entries` and renders provider and model selection per agent. No manual UI change is needed.

---

## 6. Clinical Safety & Data Invariants (absolute, never weaken, including during refactors)

1. **Pharmacist Approval Gate.** Never bypassed, disabled, or made optional. Any prescription flagged with warnings requires active pharmacist sign-off.
2. **Zero hallucination.** If parsing fails or text is unreadable, never invent dosage schedules or tablet quantities. Leave fields empty or unknown, or fail explicitly.
3. **Preserve safety flags.** Safety Auditor warnings, low-confidence scores (`confidence = "low"`), and unreadable markers are never silently downgraded, defaulted, or overwritten.
4. **Privacy and offline isolation.** In `ON_DEVICE_ONLY` mode, all agent execution is local via LiteRT-LM with zero external network calls. Patient images and data never leave configured user boundaries.
5. Never log prescription images or patient data.
6. Never hard-code or commit API keys; read them from secure config.
7. Do not change the supervisor turn limit or approval thresholds without explicit approval.
8. Dosage parsing, approval gate logic, and safety checks must have unit tests, even if tests are otherwise out of scope.

---

## 7. Compose Guardrails

`DropdownMenu`, `ModalBottomSheet`, and any container using `Modifier.height(IntrinsicSize.Min)` MUST NOT contain `SubcomposeLayout` components (`LazyColumn`, `LazyRow`, `BoxWithConstraints`, `TabRow`).
- Scrollable menus: plain `Column` with `verticalScroll(rememberScrollState())` and `heightIn(max = 220.dp)`.
- Auto-scroll to the selected item: record its offset with `Modifier.onGloballyPositioned { coordinates -> selectedY = coordinates.positionInParent().y.toInt() }`, then call `scrollState.scrollTo(...)`.

---

## 8. Project Rules

- Domain models and agent implementations live in `:shared` (`commonMain`).
- Use the `Lipi` design system in `com.snehonir.lipirx.ui.theme`. Never create a second theme, palette, or typography.
- No blocking network or DB calls on the main thread; use injected dispatchers (`Dispatchers.IO` / `Dispatchers.Default` in production wiring).

---

## 9. Build & Verify

Confirm these match the real module names.
- Build: `./gradlew :shared:build` and `./gradlew :composeApp:assembleDebug`
- Tests: `./gradlew :shared:allTests`
- Lint: `./gradlew ktlintCheck detekt` (if configured)
- Run build and tests after changes and report the actual results.