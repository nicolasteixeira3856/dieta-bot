# Plan — A66 Typed actions applied as one batch

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Chat response model (`actions[]`), batch application (one transaction, `RecordGuard`, one receipt per action, Desfazer of the batch), `chat_message.actions` persistence (Room migration), tests, captures.
- Prerequisites: [S36](../../server/plans/pending_manual_validation/s36-typed-actions.md) on the dev server; [A65](a65-workout-via-chat.md) delivered. Figma gate: N receipts under one message are already drawn by `chatSK` (two) — a design plan only if three or more receipts need a new layout.
- Related documentation: [ADR-050](../../produto/adrs/ADR-050-typed-actions-per-message.md), ADR-028/032/047/048, [product Chat](../../produto/specifications/chat.md), [Room](../specifications/room-v2.md), [HTTP contract](../../api-contract.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a66-typed-actions-batch.md. Implemente o plano aprovado.`

## Objective

A message that does several things produces several receipts, applied together and undone together.

## Scope

1. **Capability** `actions: true`; the legacy fields are ignored when `actions` is present.
2. **Batch.** Actions applied in the server's order inside one Room transaction: `log` (record or ask by `record_intent`, meal change by `meal_change`), `skip` (ADR-047, delete proposal over a record), `workout` (A65), `plan` (estimate shown, reserve action of ADR-046), `question` (one, alone). A failure rolls the batch back and shows the error state.
3. **Receipts and Desfazer.** One receipt per action under the message (ADR-048); Desfazer reverts the whole batch. A clarification inside one action shows the question under that action's bubble; the other actions are applied.
4. **Persistence.** `chat_message.actions` as JSON (Room v13), schema file and migration test.
5. **Tests and captures.** Unit tests of the batch (order, rollback, undo); `capture-chat.sh` scene `a66` with the fake server returning two logs plus a skip, a log plus a plan, a clarification on one of two.

### Specification changes at Completion

- [product Chat](../../produto/specifications/chat.md): receipts per action, Desfazer of the batch (text of ADR-050). [Room](../specifications/room-v2.md): v13.

## Out of scope

- Options and recipes: A67, A68.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures of the touched flows vs the golds; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): the whole-day message, "jantei X, me sugere o lanche", Desfazer.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
