# Plan — A30 Questions before the estimate, Forçar estimativa

- Status: Concluído
- Approval: 30/09/2026 (owner: "Aprovo o plano `docs/android/plans/a30-perguntas-antes-da-estimativa.md`. Implemente o plano aprovado.")
- Date: 30/09/2026
- Owning context: `android`
- Affected code: `apps/android/` (`core/network/ChatModels.kt`, `feature/chat/*`, `core/telemetry` events, tests and captures)
- Prerequisites: **[S13](../../../server/plans/completed/s13-perguntas-antes-da-estimativa.md) deployed on the dev server** and **[ST7](../../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md) in `stitch/plans/completed/`** (golds `chatE` edited, `chatQ` new). Executes [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) decisions 1, 3, 4 and 5; accepts ADR-026 on completion.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a30-perguntas-antes-da-estimativa.md`. Implemente o plano aprovado.

**First implementation step:** confirm `docs/stitch/plans/completed/st7-pergunta-antes-da-estimativa.md`, golds `chatE` and `chatQ` in `docs/qa/stitch/{dark,light}/`, and S13 in `docs/server/plans/completed/` (or `pending_manual_validation/`) with the dev server answering `clarify_rounds`. Otherwise stop and tell the owner.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

The Chat shows no estimate while the AI still has doubts about a logged meal: only question bubbles, at most 3, then the estimate. From the second question on, **Forçar estimativa** releases the estimate in one tap.

## Sources of truth

- Golds `chatE` and `chatQ` (dark and light) from ST7.
- [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [chat](../../../produto/specifications/chat.md), [v1-chat](../../../server/specifications/v1-chat.md).
- `AGENTS.md` tokens; skills `dieta-bot-android-ui`, `material3-expressive`, `dieta-bot-android-visual`, `compose-stability`, `screenshot-testing`.

## Implementation scope

### 1. Wire

- `ChatIn`: `clarifyRounds: Int` (always sent, 0–3) and `forceEstimate: Boolean = false`.
- `ChatOut`: `question: String? = null`.

### 2. Storing a question turn

- `ChatOut.question` non-blank and `estimate == null`: insert the assistant message with `text = reply` (the server's history text), `estimateQuestion = question`, `intent = log`, no estimate columns. No Room migration.
- History (`PromptBuilder.turnText`): unchanged for these rows, because `text` already holds draft + question. For older rows with estimate + question (pre-A30), append the question to the sent text so the model sees what was asked.

### 3. Round count

- `clarifyRounds` = number of consecutive question-only assistant messages of today at the end of the conversation, walking back and skipping the user answers between them; stops at an estimate, a receipt, a non-question assistant message or the start of the day. Capped at 3. Pure function, unit-tested.

### 4. Rendering

- Question-only message: the "Dieta Bot AI" label and the existing `QuestionBubble` (gold bar, help icon), time under it, no reply bubble, no estimate card, no action group — as `chatQ`.
- Estimate after questions: card and actions as `chatE`, time under the bubble; no question bubble (the server sends none to this client). Old stored estimate + question rows keep rendering as before.
- Text for the timeline (chat rule 12): the chain walk-back already skips answers to questions; verified for 3 rounds (the recorded text is `meal_text`, never an answer).

### 5. Forçar estimativa

- Shown only when the last item is a question-only message whose round is ≥ 2, nothing is loading, and the composer has no photo attachment. Position, size and style as `chatQ`: single full-width pill in the actions slot above the composer, fast-forward icon, label `Forçar estimativa`.
- Tap: sends the user message `Pode estimar assim.` through the normal send flow with `forceEstimate = true` (loading `chatL`, failure bubble and retry as today; retry keeps the flag).
- Hidden after any send.
- Telemetry: event `chat_force_estimate` with `round` (number). `chat_result` gains `question_only` (boolean). Enums and numbers only.

### 6. Docs in the same delivery

- [chat](../../../produto/specifications/chat.md) rules 4, 12, 14 and the states list (`chatQ`); `AGENTS.md` Product line "One question if confidence is not high" → "Questions before the estimate, all doubts at once, at most 3 rounds; Forçar estimativa from the second"; `AGENTS.md` Chat screen list gains `chatQ`; ADR-026 status → Accepted; README and matrix.

## Affected files and areas

- `apps/android/app/src/main/java/com/nutri/android/core/network/ChatModels.kt`
- `apps/android/app/src/main/java/com/nutri/android/feature/chat/` (`ChatViewModel.kt`, `ChatUiState.kt`, `ChatScreen.kt`, `PromptBuilder.kt`)
- Telemetry event constants.
- Unit tests, Roborazzi baselines, `docs/qa/android/current/{dark,light}/chatE.png` and `chatQ.png`.

## Planned validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: round count (0, 1, 2, 3, cap, stops at estimate/receipt/day), question-only storage, history text for old rows, button visibility (round 1 hidden, round 2 shown, loading hidden, attachment hidden), force send carries the flag, recorded text after 3 rounds is `meal_text`.
2. `./gradlew.bat :app:verifyRoborazziDevDebug` with new/updated baselines for `chatQ` and `chatE`.
3. Emulator captures `chatQ` and `chatE` in both themes vs the ST7 golds; written diff list; iterate until they match (AGENTS Visual QA).
4. `./gradlew.bat :app:assembleDevRelease`.
5. Manual on the dev server: dinner with a doubt → question only; answer → estimate once; force from the second question; 3-round cap reached.

## Out of scope

- Server changes (S13), Stitch (ST7).
- Force button on the first question (owner decision: from the second).
- Changing `plan` or `question` intents.
- Room schema changes.

## Risks and controls

- **Server not deployed:** the request always carries `clarify_rounds`; an old server ignores it and answers estimate + question as today, which still renders. The prerequisite check prevents shipping ahead.
- **Wrong round count after a day turn or a deleted message:** the count stops at the start of the day; the server cap is the final guard.
- **Double estimate on retry:** retry resends the same text and flag; nothing is recorded without a tap.

## Acceptance criteria

- A logged meal with a doubt shows only the question bubble, matching `chatQ`.
- The estimate appears after the answers, once, with no question bubble, matching `chatE`.
- At most 3 question messages per meal; the 4th turn is always the estimate.
- Forçar estimativa appears from the second question and yields the estimate in one tap.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../../sdd/README.md#fora-de-escopo).

## Results (30/09/2026)

Prerequisites confirmed: ST7 in `stitch/plans/completed/`, golds `chatE` and `chatQ` in both themes, S13 `Concluído` with its dev-server check (`clarify: asked`).

Implemented:

- Wire: `ChatIn.clarifyRounds` has no default on purpose. The app's `Json` does not encode defaults, so a defaulted `0` would never be sent and the server would treat the app as a legacy client. A unit test asserts `"clarify_rounds":0` on the wire. `forceEstimate` is sent only when true.
- `PromptBuilder.clarifyRounds` (pure) and `isQuestionOnly`. History: an old estimate row with a question sends `reply + "\n" + question`; a question-only row goes as stored.
- `ChatViewModel`: a question-only answer is stored as `text = reply`, `estimateQuestion = question`, `intent = log`, no estimate columns (no migration). `forceEstimate()` sends `Pode estimar assim.` through the normal flow, with `pendingForce` kept for retry. The composer draft is left untouched. Telemetry: `chat_force_estimate {round}`, and `chat_result` gains `question_only`.
- UI: `ChatItem.Question(standalone = true)` draws the "Dieta Bot AI" label plus the question bubble, with no reply bubble. `ForceBar` (`chat-force-estimate`) takes the actions slot. The question bubble follows the ST7 gold: text 18 sp / 24.5, icon on the first line, CSS-style frame (3 dp gold on the left following the radius, 1 dp line elsewhere) and 19 dp bottom padding.
- QA tools: `fake-chat-server.mjs` gets `{"clarify": true}`, and `capture-chat.sh` gets `SCENES=a30`. The chatE seed no longer has a question. `diff-gold.mjs` gates the `chatQ` Forçar bar region and reports `light/chatQ` (see below).

Validation:

1. `:app:testDevDebugUnitTest`: **354** tests, 0 failures. New: round count (0/1/2/3, cap, stops at estimate, old estimate with question, receipt, wipe, other reply), wire encoding, history text, question-only storage and rendering, Forçar from round 2 (flag, `round`, draft kept), hidden with an attachment, while sending and after failure, retry keeps the flag, 3 rounds then estimate recording `meal_text`, and without `meal_text` the first message (never an answer). Telemetry maps were updated for `question_only`. ✅
2. `:app:verifyRoborazziDevDebug` green. New baselines `chatE` (replaces `chatQuestion`, whose fixture lost its question) and `chatQ`, dark and light. ✅
3. JVM `StitchGoldTest`: chatE 1.79% / 1.52%, chatQ dark 1.35% (Forçar bar region 0.17%). chatQ light 6.39% is reported only, with its region gated at 0.12%. Emulator `SCENES=a30` dark and light: every flow check ✓, including round 1 with no actions and no Forçar, `clarify_rounds` 0/1/2/3 on the wire, Forçar with one POST and `force_estimate` at round 2, the 4th turn always an estimate, and the recorded text being the dinner, not an answer. `node tools/diff-gold.mjs`: chatQ dark 1.16% (region 0.19%), chatE 1.51% / 1.16%, chatQ light region 0.12% with the screen at 5.91% reported. Whole run exit 0. ✅
4. `:app:assembleDevRelease` ✅
5. Dev server (gpt-6-luna): a direct request returned question only at rounds 0 and 1, an estimate with Forçar at round 2 (743 kcal, `question` null) and an estimate at round 3 (cap). The dev release APK on the emulator: dinner → question only (no card, no actions) → answer → one estimate with Gravar jantar | Trocar | Pular and no question bubble. The live model did not ask a second question in that run, so Forçar on the live app was covered by the API request and the fake-server flow. ✅

Diff list `chatQ` (dark gold vs emulator): layout, label/bubble/time spacing, bubble width, gold bar, CTA pill (48 dp, radius 18, card + line, fast-forward icon, semibold label, no gold background) and composer all match. Question type was 16 → 18 sp after the first comparison, and bottom padding 16 → 19 dp. Known and ignored: the header "tune" button and the composer mic (no such features; same on every chat gold).

Known gold conflict, reported and not gated: the light `chatQ` gold draws the question bubbles tighter than dark from the same prompt (line ~22 dp vs 24.5 dp, icon gap 10 vs 12 dp, ~3 dp less padding). One component follows dark. The light Forçar bar stays gated. Regenerating light `chatQ` in Stitch would close it.
