# Plan — A57 Rendering the reply subset in the bubbles

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (a parser in `domain`, the assistant bubble composables in `feature/chat`, Aero text and list components in `core/designsystem/aero`, Roborazzi baselines, tests) plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-chat.sh`.
- Related documentation: [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md), [product Chat](../../produto/specifications/chat.md) rules 2, 8, 12 and 16, [HTTP contract](../../api-contract.md), [aero-compose skill](../../../.claude/skills/aero-compose/SKILL.md).
- Prerequisites:
  - [D17](../../design/plans/pending_manual_validation/d17-rich-replies.md) `Concluído` with `chatR`, `chatE` and `chatRK` exported;
  - [S26](../../server/plans/s26-reply-formatting-subset.md) delivered and deployed to the dev server;
  - [A50](a50-plan-budget-choice.md), [A54](completed/a54-auto-record-addition-empty-slot.md) and [A55](a55-tone-choice-and-closures.md) delivered or cancelled (same Chat files; no parallel Android plan).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a57-rich-reply-rendering.md. Implemente o plano aprovado.`

## Objective

An assistant bubble shows bold numbers, bullets, numbered steps and the portions table exactly as the D17 golds, from the ADR-045 subset in `reply`, with no library and no change to what is recorded or sent back.

## Scope

### 1. Parser (`domain/ReplyMarkup`)

- Pure Kotlin: `reply` → a list of blocks (paragraph with bold spans, bullet item, step item, table of rows). Only the ADR-045 subset is recognised; anything else is literal text. Unbalanced `**`, a table with a bad row or a second table fall back to literal lines. Bounded input (the server reply limit); no recursion.
- `plain(reply)`: the text without markers, used wherever the app keeps or sends text (receipt text of rule 12 when the reply is the source, the history line of rule 8, the `Editar` composer, telemetry lengths). Room never stores markers in record text; the assistant row keeps the raw reply for display.

### 2. Rendering (Aero)

- `AeroRichText`: paragraphs with bold spans in the strong body style; `AeroList` rows for bullets and steps with the design-system list spacing and marker column; `AeroPortionsTable` for the two-column table inside the bubble width. Tokens from `docs/tokens.md`; no Material. The macro colours of rule 16 keep applying to the P/C/G line over the bold.
- Used by the plan bubble (`chatR`, `chatRK`), the log estimate bubble (`chatE`) and the plain assistant bubble; question bubbles, refusals, receipts and cards unchanged.

### 3. QA tooling

- `tools/fake-chat-server.mjs` gains a `{"format": ...}` switch returning a two-option plan, a recipe with a table and steps, a log with bold, and a malformed markup sample; `tools/capture-chat.sh` gains `SCENES=a57`.

### 4. Telemetry

- None new. `chat_result` unchanged; lengths measured on `plain(reply)`.

### Intended specification changes

At Completion: [product Chat](../../produto/specifications/chat.md) rule 2 (what a bubble renders), rule 8 (history line without markers), rule 12 (record text without markers), rule 16 (`chatRK`, bold over the macro colours); state list gains `chatRK`; Provenance line; ADR-045 status to Accepted (if not already).

## Out of scope

- Server (S26), golds (D17), user-typed markup (the composer sends text as typed; the user's bubble stays plain), production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. Parser tests: every marker, the fallbacks (unbalanced bold, seven-row table, second table, nested list, heading, link), `plain()` round trips, a 2000-character reply in under a millisecond.
2. Roborazzi: the three bubbles against the D17 golds (`GoldTest`), dark and light; the plain bubble unchanged.
3. Emulator, dev flavor, `SCENES=a57` against the fake server: fresh captures of `chatR`, `chatE`, `chatRK` in `docs/qa/android/current/{dark,light}/` compared with the golds under the QA rules of AGENTS; a record from a formatted plan shows plain text on the timeline and in the receipt. Partial validation: only the flows this plan touches.
4. One real recipe through the dev server (S26 deployed): the table and steps render; the history of the next turn carries no markers (request body inspected). Request id recorded.
5. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.

## Results

Planning only.
