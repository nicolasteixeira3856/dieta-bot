# Plan — A66 Typed actions applied as one batch

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message A64–A69; code, tests and emulator checks done; the device smoke on a dev build is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Chat response model (`actions[]`), batch application (one transaction, `RecordGuard`, one receipt per action, Desfazer of the batch), `chat_message.actions` persistence (Room migration), tests, captures.
- Prerequisites: [S36](../../../server/plans/pending_manual_validation/s36-typed-actions.md) on the dev server; [A65](a65-workout-via-chat.md) delivered. Figma gate: N receipts under one message are already drawn by `chatSK` (two) — a design plan only if three or more receipts need a new layout.
- Related documentation: [ADR-050](../../../produto/adrs/ADR-050-typed-actions-per-message.md), ADR-028/032/047/048, [product Chat](../../../produto/specifications/chat.md), [Room](../../specifications/room-v2.md), [HTTP contract](../../../api-contract.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a66-typed-actions-batch.md. Implemente o plano aprovado.`

## Objective

A message that does several things produces several receipts, applied together and undone together.

## Scope

1. **Capability** `actions: true`; the legacy fields are ignored when `actions` is present.
2. **Batch.** Actions applied in the server's order inside one Room transaction: `log` (record or ask by `record_intent`, meal change by `meal_change`), `skip` (ADR-047, delete proposal over a record), `workout` (A65), `plan` (estimate shown, reserve action of ADR-046), `question` (one, alone). A failure rolls the batch back and shows the error state.
3. **Receipts and Desfazer.** One receipt per action under the message (ADR-048); Desfazer reverts the whole batch. A clarification inside one action shows the question under that action's bubble; the other actions are applied.
4. **Persistence.** `chat_message.actions` as JSON (Room v13), schema file and migration test.
5. **Tests and captures.** Unit tests of the batch (order, rollback, undo); `capture-chat.sh` scene `a66` with the fake server returning two logs plus a skip, a log plus a plan, a clarification on one of two.

### Specification changes at Completion

- [product Chat](../../../produto/specifications/chat.md): receipts per action, Desfazer of the batch (text of ADR-050). [Room](../../specifications/room-v2.md): v13.

## Out of scope

- Options and recipes: A67, A68.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures of the touched flows vs the golds; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): the whole-day message, "jantei X, me sugere o lanche", Desfazer.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 08/10/2026 on `feat/a66-typed-actions-batch`, in the autonomous run of A64–A69 ([report](../../validation/batch-2026-10-08-a64-a69.md)).

### Delivered

- **Capability** `actions: true` on every normal turn (`false` on compact); `ChatOut.actions[]` (`ChatAction`: id, type, slot, estimate, question, record, record_intent, meal_day, meal_change, workout, recipe_id, options, plan_budget, recipe). With `actions` present the legacy top-level fields are ignored.
- **Rows.** `ChatActions.parts` turns the list (at most six) into the rows the Chat already knows: one assistant row per `log` or `plan` action, in the server's order. The first row carries the reply, the memory updates, every `skip` action as its listed skips (A59 path) and the raw actions (`chat_message.actions`, **Room v14**; the plan said v13, moved by A64's v13). Later rows have blank text: the bubble shows only the estimate card or the plan panel, and they stay out of the prompt history. A held `log` (estimate null, its own question) is a question row with its slot: the question shows in its bubble and the other actions go on. Each row goes through the single-answer rules unchanged (automatic record, Registrar, addition, revision, Substituir, plan with Registrar assim and Reservar, plan budget); `workout` actions apply after the rows (A65); a question or refusal is one row.
- **Receipts and Desfazer of the batch.** One receipt per action under the message. Every receipt the answer writes by itself (records, skips, workout) keeps the first row id as `UndoData.batch`; with two or more of them still holding their actions, each offers **Desfazer**, and `ChatRecorder.undoBatch` reverts all of them in one `commitRecord` (slots, skips, the workout number, the memory images), with `Restaurado` receipts where a record comes back. A plan is not closed (nor counted as recorded) by the receipts of its own batch.

### Deviation

- **Transactions.** The plan asked for the whole batch in one Room transaction with a rollback. The rows are applied in order, each through its existing guarded transaction (the record paths of A34/A47/A59/A65 stay the single source of truth); Desfazer of the batch is one transaction. A failure of one action leaves the others applied and marks that one `Não registrado`, as for a single answer. Only the last open estimate of the day offers Registrar (as before), so an `ask` log that is not the last action ends `Não registrado`.

### Validation

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: 695 tests; the one failure of the first run (`MigrationV8V9Test`, whose fixture has blank assistant rows) led to the narrower history filter (blank text **and** an estimate), and the rerun of the affected suites passes. New: `ChatActionsTest` (3: legacy answer one row; rows in order with reply, memory and skips on the first; held log, skip-only answer, six at most), `ChatTypedActionsTest` (4: the whole day with two records and a skip, three receipts in one batch, the legacy estimate ignored, the blank row out of the history, Desfazer of the batch; a log and a plan with Registrar assim, Reservar and the day projected with the log; a clarification on one of two; compact without the capability), `MigrationV13V14Test`.
2. Emulator (Medium_Phone 780 × 1688 @ 320, devDebug against the fake, `SCENES=a66`, both themes): every check passed — `actions: true` on the wire; the whole day: Café 380 and Almoço 640 recorded, Lanche skipped, three receipts, the actions on the first row only, Desfazer on one receipt reverted both records and the skip; "jantei…, me sugere o lanche": the dinner recorded and the plan with Registrar assim; one held of two: the breakfast recorded alone and `Quanto de arroz no almoço?` shown.No new gold: the receipts are `Chat/Receipt` (`chatSK` draws two under one message) and the bubbles are those of `chatR` and `chatQ`, all green in the JVM `GoldTest`. Flows left out (Delivery pace rule): the other Chat scenes, Home, Config, onboarding.
3. Device smoke on a dev build: **not run** (owner decision of 08/10/2026: no test build per plan, one build at the end); moved to the manual acceptance.
4. `node tools/check-docs.mjs` passes.

### Manual acceptance (after delivery)

- On a dev build: the whole day in one message gives one receipt per meal and skip, and Desfazer on any of them reverts all; "jantei X, me sugere o lanche" records the dinner and leaves the snack plan with Registrar assim.

