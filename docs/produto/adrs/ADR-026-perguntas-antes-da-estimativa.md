# ADR-026 — Questions before the estimate, capped at 3 rounds

- Status: Accepted (30/09/2026, with [A30](../../android/plans/completed/a30-perguntas-antes-da-estimativa.md)); partially superseded by [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (decision 1: `plan` never asks, except the app-driven budget choice); complemented by [ADR-041](ADR-041-reference-portions-in-chat-instructions.md) (a first message that quantifies every food is estimated, not questioned)
- Date: 2026-09-30
- Context: `produto`
- Supersedes: partially the `AGENTS.md` Product rule "One question if confidence is not high", rule 14 of the [Chat spec](../specifications/chat.md) and rule 4 of [v1-chat](../../server/specifications/v1-chat.md) ("one question per meal"). Changes gold `chatE` (the question bubble leaves the estimate screen) and adds gold `chatQ` (27 → 28 per theme). Extends `POST /v1/chat` of the [contract](../../api-contract.md) additively.

## Context

Owner feedback after real use (30/09/2026). Dinner sent to the Chat; the AI had a doubt about one ingredient. It showed the estimate (card + Gravar/Trocar/Pular) with the question below it (`chatE`). The owner then answered, and the AI sent a second estimate for the same meal. The first number was provisional but looked final and could be recorded.

Current code, by design:

- `server/llm.py` asks for the estimate **and** one question in the same turn when confidence is not high. "One question per meal" exists only as prompt text; no code enforces it.
- The Android history sends only the assistant `reply` (`PromptBuilder.turnText`), never the question text. On the answer turn the model does not see what it asked, which invites repeated questions.

The owner also wants a hard stop: a model that degrades must not keep asking or repeat questions forever.

## Decision

### 1. Questions come before the estimate (`log` only)

For a meal the user ate (`intent = log`), when confidence is not high the Chat shows **only the question**, with no estimate card, no numbers and no Gravar/Trocar/Pular. The estimate appears once the AI has no material doubt left, or when the hard stop fires. `plan` keeps its rule (never asks: assumes and says what it assumed). `question` is unchanged.

### 2. All doubts at once

A question message lists every open doubt of the meal together (at most 3 short questions in one message). After an answer, the AI asks again only about what is still unknown **and** changes the estimate materially. It never repeats a question already asked in the conversation.

### 3. Hard stop: 3 rounds, enforced in server code

A round is one question message shown to the user for the pending meal. The server releases the estimate (question dropped, `question: null`) when any of these holds, regardless of what the model returned:

1. the client says 3 rounds were already shown (`clarify_rounds ≥ 3`): the user sees at most 3 question messages per meal;
2. the new question repeats one already asked (normalised text similarity against the assistant turns in the history);
3. the user tapped **Forçar estimativa** (`force_estimate: true`);
4. confidence is high, or the model returned no question text (no generic fallback question in this mode).

The model re-estimates the whole meal every turn, so the released estimate always includes every answer given so far. When the estimate is released with confidence not high, the `reply` states in one line what was assumed.

### 4. Forçar estimativa button

From the **second** question message of a meal on, the latest question message shows a single full-width button **Forçar estimativa** in the actions position (where Gravar/Trocar/Pular sit), above the composer. Tapping it sends the user message `Pode estimar assim.` with `force_estimate: true`. The answer is the estimate, released by rule 3.3. The button disappears as soon as another message is sent.

### 5. History carries the question

The text the app keeps for a question turn, and sends back in `messages[]`, is the draft meal plus the question (built by the server). The model therefore sees what it asked and what it had understood, including what it saw in a photo sent only once.

### 6. Compatibility

The behaviour is opt-in by the client: only a request with `clarify_rounds` gets question-only turns. Older APKs (≤ 0.0.4) keep today's estimate + question. Room needs no migration (`estimateQuestion` already exists).

### 7. Gate

One Stitch gate, [ST7](../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md), edits `chatE` (question bubble removed) and creates `chatQ` (second question with Forçar estimativa), in both themes.

## Rationale

- A number shown next to an open doubt reads as final and can be recorded. Withholding it until doubts are settled removes the duplicate estimate at the source.
- Asking everything at once keeps the typical case to one round; the cap only guards against a degraded model.
- The cap lives in server code, not in the prompt, so it holds even when the model ignores instructions. The client supplies the round count because the server is stateless (ADR-013).
- The force button hands control back to the user without typing.

## Consequences

### Positive

- One estimate per meal in the common case; nothing provisional to record by mistake.
- At most 3 question messages per meal, whatever the model does.
- The model sees its own questions in the history.

### Negative

- The first number arrives one round later when the AI has doubts.
- Every asking turn still pays a full estimate from the model (the server discards it). Accepted: no second call is needed to release.
- Two gold changes per theme (`chatE` edited, `chatQ` new): 27 → 28 golds.
- Repetition detection is heuristic; a paraphrased repeat can slip through until the 3-round cap stops it.

## Alternatives considered

- **Keep estimate + question and replace the card on answer:** still shows a provisional number that can be recorded; rejected by the owner.
- **Cap enforced only in the prompt:** does not survive a degraded model; rejected by the owner.
- **Server counts rounds by parsing history text:** fragile (assistant turns carry only text). The client counts from Room, where it already marks question turns.
- **Second model call to force the estimate:** more latency and cost; the discarded draft estimate already covers the release.
- **Button from the first question:** the owner chose "after the first round".
- **1 or 2 rounds:** the owner chose 3.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md), [api-contract](../../api-contract.md).
- Related ADRs: [ADR-012](ADR-012-chat-home-perfil.md), [ADR-017](ADR-017-registro-consolidado.md), [ADR-023](ADR-023-chat-v2-memoria-v2.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md).
- Plans: [S13](../../server/plans/completed/s13-perguntas-antes-da-estimativa.md) → [ST7](../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md) → [A30](../../android/plans/completed/a30-perguntas-antes-da-estimativa.md).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the supersession.
