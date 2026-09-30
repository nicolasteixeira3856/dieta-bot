# Plan — A30 Questions before the estimate, Forçar estimativa

- Status: Awaiting approval
- Date: 30/09/2026
- Owning context: `android`
- Affected code: `apps/android/` (`core/network/ChatModels.kt`, `feature/chat/*`, `core/telemetry` events, tests and captures)
- Prerequisites: **[S13](../../server/plans/s13-perguntas-antes-da-estimativa.md) deployed on the dev server** and **[ST7](../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md) in `stitch/plans/completed/`** (golds `chatE` edited, `chatQ` new). Executes [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) decisions 1, 3, 4 and 5; accepts ADR-026 on completion.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a30-perguntas-antes-da-estimativa.md`. Implemente o plano aprovado.

**First implementation step:** confirm `docs/stitch/plans/completed/st7-pergunta-antes-da-estimativa.md`, golds `chatE` and `chatQ` in `docs/qa/stitch/{dark,light}/`, and S13 in `docs/server/plans/completed/` (or `pending_manual_validation/`) with the dev server answering `clarify_rounds`. Otherwise stop and tell the owner.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

The Chat shows no estimate while the AI still has doubts about a logged meal: only question bubbles, at most 3, then the estimate. From the second question on, **Forçar estimativa** releases the estimate in one tap.

## Sources of truth

- Golds `chatE` and `chatQ` (dark and light) from ST7.
- [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [chat](../../produto/specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md).
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

- [chat](../../produto/specifications/chat.md) rules 4, 12, 14 and the states list (`chatQ`); `AGENTS.md` Product line "One question if confidence is not high" → "Questions before the estimate, all doubts at once, at most 3 rounds; Forçar estimativa from the second"; `AGENTS.md` Chat screen list gains `chatQ`; ADR-026 status → Accepted; README and matrix.

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

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../sdd/README.md#fora-de-escopo).
