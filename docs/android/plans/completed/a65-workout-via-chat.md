# Plan — A65 Workout energy recorded from the Chat

- Status: Concluído (09/10/2026, owner acceptance: "todos os planos pendentes de minha revisão no Android estão aprovados")
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Chat response handling (`workout`), `commitRecord` path for `day.workoutKcal`, receipt and Desfazer, tests, captures.
- Prerequisites: [S35](../../../server/plans/completed/s35-workout-via-chat.md) on the dev server; [A64](a64-chat-context-fields-day-balance.md) delivered. Figma gate: a design plan only if the workout receipt does not fit the receipt golds (`chatF`, `chatSK`).
- Related documentation: [ADR-049](../../../produto/adrs/ADR-049-workout-energy-via-chat.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), [product Chat](../../../produto/specifications/chat.md), [home-timeline](../../../produto/specifications/home-timeline.md), [HTTP contract](../../../api-contract.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a65-workout-via-chat.md. Implemente o plano aprovado.`

## Objective

A `workout {kcal, mode}` in the answer writes the day's workout number exactly as the Home dialog does, with a receipt and Desfazer; the credit follows the eat-back rule.

## Scope

1. **Capability** `workout: true` in the request.
2. **Record.** `replace` sets `day.workoutKcal`; `add` sums; the same `RecordGuard` and transaction as a meal record; the Home timeline and the ceiling update through the existing flows.
3. **Receipt.** `Treino registrado · {kcal} kcal` (or `somado`), with Desfazer restoring the previous number; the Home dialog keeps working and shows the Chat value.
4. **Tests and captures.** Unit tests of replace/add/undo; `capture-chat.sh` scene `a65` with the fake server returning a workout; `capture-home.sh` shows the credit.

### Specification changes at Completion

- [product Chat](../../../produto/specifications/chat.md) and [home-timeline](../../../produto/specifications/home-timeline.md): workout from the Chat (text of ADR-049).

## Out of scope

- Inferred energy, health platforms, exercise advice. Several actions in one message: A66.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures of the touched flows vs the golds; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): "treino de hoje 450 kcal", receipt, Home credit, Desfazer.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 08/10/2026 on `feat/a65-workout-via-chat`, in the autonomous run of A64–A69 ([report](../../validation/batch-2026-10-08-a64-a69.md)).

### Delivered

- **Capability** `workout: true` on every normal turn (`false` on compact); `ChatOut.workout {kcal, mode}`.
- **Record.** `ChatRecorder.workout`: `replace` sets `day.workoutKcal`, `add` sums it to the day's number; one `commitRecord` transaction with the request's `RecordGuard` (day and wipe) that also rechecks the number it read (`WorkoutChange` before → after). A number outside 1–5000 or an unknown mode writes nothing. Home ring, meta and the `Treino de hoje` line update through the existing flows; the Home dialog keeps writing the same field.
- **Receipt.** Role `workout`, `Treino registrado` + chip `{kcal} kcal` (`add`: `Treino somado` + `+{kcal} kcal`), Barbell icon, on the `Chat/Receipt` card (no new component: no Figma gate). Only **Desfazer**, which restores the previous number (or none) in one transaction while the day still holds the number the receipt wrote; the newest workout receipt of the day owns the action (`ReceiptRules.WORKOUT_SLOT`), and a change from the Home dialog removes it. A workout-only answer (`record: auto`, no estimate) no longer ends as `Não registrado`; next to a meal each gets its receipt. A workout receipt never closes an open plan. Telemetry `workout_saved` (`from`, `mode`, `kcal`). The day balance of A64 rides on the newest receipt, workout included.

### Validation

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: 687 tests, 0 failures. New `ChatWorkoutTest` (5: replace then add with receipts and telemetry, Desfazer back to the previous number, the Home dialog removes Desfazer, a meal and a workout in one answer with two receipts, out-of-bounds or unknown mode writes nothing, compact sends no capability).
2. Emulator (Medium_Phone 780 × 1688 @ 320, devDebug against the fake, `SCENES=a65`, both themes): every check passed — `workout: true` on the wire, receipt `Treino registrado` · `450 kcal` with Desfazer, `day.workoutKcal` 450, Home `Treino de hoje` `450 kcal · +0 na meta` (eat-back 0 %), Desfazer back to none (`Desfeito`), replace 300 then add 200 → `Treino somado` · `+200 kcal` and 500. `capture-chat.sh` gained `SKIP_ONBOARDING=1` for reruns on an onboarded emulator. No gold covers a workout receipt; the `Chat/Receipt` golds (`chatF`, `chatSK`, `chatG`, `chatD`) stay green in the JVM `GoldTest`. Flows left out (Delivery pace rule): every other Chat scene, Home captures, Config, onboarding.
3. Device smoke on a dev build: **not run**. Owner decision of 08/10/2026 during the run: no test build per plan; the owner asks for one build at the end. The smoke moves to the manual acceptance below.
4. `node tools/check-docs.mjs` passes.

### Manual acceptance (after delivery)

- On a dev build: "treino de hoje 450 kcal" → receipt `Treino registrado · 450 kcal`, Home `Treino de hoje` and the meta with the credit, Desfazer back to the previous number.

