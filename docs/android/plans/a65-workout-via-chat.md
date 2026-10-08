# Plan — A65 Workout energy recorded from the Chat

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Chat response handling (`workout`), `commitRecord` path for `day.workoutKcal`, receipt and Desfazer, tests, captures.
- Prerequisites: [S35](../../server/plans/completed/s35-workout-via-chat.md) on the dev server; [A64](a64-chat-context-fields-day-balance.md) delivered. Figma gate: a design plan only if the workout receipt does not fit the receipt golds (`chatF`, `chatSK`).
- Related documentation: [ADR-049](../../produto/adrs/ADR-049-workout-energy-via-chat.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [product Chat](../../produto/specifications/chat.md), [home-timeline](../../produto/specifications/home-timeline.md), [HTTP contract](../../api-contract.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a65-workout-via-chat.md. Implemente o plano aprovado.`

## Objective

A `workout {kcal, mode}` in the answer writes the day's workout number exactly as the Home dialog does, with a receipt and Desfazer; the credit follows the eat-back rule.

## Scope

1. **Capability** `workout: true` in the request.
2. **Record.** `replace` sets `day.workoutKcal`; `add` sums; the same `RecordGuard` and transaction as a meal record; the Home timeline and the ceiling update through the existing flows.
3. **Receipt.** `Treino registrado · {kcal} kcal` (or `somado`), with Desfazer restoring the previous number; the Home dialog keeps working and shows the Chat value.
4. **Tests and captures.** Unit tests of replace/add/undo; `capture-chat.sh` scene `a65` with the fake server returning a workout; `capture-home.sh` shows the credit.

### Specification changes at Completion

- [product Chat](../../produto/specifications/chat.md) and [home-timeline](../../produto/specifications/home-timeline.md): workout from the Chat (text of ADR-049).

## Out of scope

- Inferred energy, health platforms, exercise advice. Several actions in one message: A66.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures of the touched flows vs the golds; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): "treino de hoje 450 kcal", receipt, Home credit, Desfazer.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
