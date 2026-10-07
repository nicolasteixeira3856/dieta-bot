# Plan — A58 Reserve a plan for its meal

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Room entity and migration for the planned state, `BudgetCalculator`, Chat plan bubble and ViewModel, receipts, Home timeline, network DTOs, telemetry, tests) plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-*.sh`.
- Related documentation: [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md), [product Chat](../../produto/specifications/chat.md) rules 16 and 19, [home-timeline](../../produto/specifications/home-timeline.md), [Room](../specifications/room-v2.md), [HTTP contract](../../api-contract.md).
- Prerequisites:
  - [D18](../../design/plans/d18-planned-meal.md) `Concluído` with `chatR`, `chatRL`, `homeP` exported;
  - [S27](../../server/plans/s27-planned-slot.md) delivered and deployed to the dev server;
  - [A50](a50-plan-budget-choice.md), [A54](completed/a54-auto-record-addition-empty-slot.md), [A55](a55-tone-choice-and-closures.md) and [A57](a57-rich-reply-rendering.md) delivered or cancelled (same Chat, Home and Room files; no parallel Android plan).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a58-planned-meal-reservation.md. Implemente o plano aprovado.`

## Objective

One tap reserves a plan for its meal: the slot shows as planned on the timeline, the day's budget reserves its kcal, the request carries it as `planned`, and the real record replaces it and shows the difference.

## Scope

### 1. Room and budget

- `planned_meal` table (or a nullable column family on the slot state, decided at start from the current schema): date, slot id, text, kcal, P/C/G, source message id. One row per date and slot; non-destructive migration, exported schema, migration test.
- `BudgetCalculator`: a planned slot contributes to `reservedUpcoming` of the projected-day panel (chat rule 16) and to nothing eaten. The Home ring ignores it.
- Rollover (00:00 SP), wipe and app reset clear reservations of the day; a record or a skip on the slot replaces or clears its reservation.

### 2. Chat

- Plan bubble of today with a suggested slot of today: pill **Reservar para o {slot}** next to **Registrar assim** (`chatR`). Tap writes the reservation in one transaction that rechecks the day and the slot, marks the bubble `Reservado para o {slot}` (`chatRL`) and keeps **Registrar assim**. Reserving again from another plan replaces; the earlier bubble loses its marker.
- `ChatDaySlot` sends `status: planned` with the numbers on every normal turn.
- A record into a planned slot (any path of rules 5, 6, 16 or 22) replaces the reservation and the receipt gains the line `Plano: {kcal} · Registrado: {kcal} ({+n} kcal)`; Desfazer restores the reservation when the record is undone.

### 3. Home

- Timeline slot in the planned state (`homeP`): dish text, `planejado · {kcal} kcal`, muted; tap opens the Chat like an empty slot; long press skips and clears the reservation after the usual confirmation.

### 4. Telemetry

- `plan_reserved` (`reserved` | `replaced` | `cleared_by_record` | `cleared_by_skip`), numbers only; `meal_saved` gains `had_plan: bool`.

### 5. QA tooling

- `tools/fake-chat-server.mjs` returns a plan with a slot and, with `{"planned": ...}`, a log answer for a planned slot; `tools/capture-*.sh` gains `SCENES=a58`: plan, reserved marker, planned timeline, record with the difference line, undo.

### Intended specification changes

At Completion: [product Chat](../../produto/specifications/chat.md) rules 16 (the pill, `chatRL`), 19 (receipt line, undo), 22 (a planned slot is not occupied); [home-timeline](../../produto/specifications/home-timeline.md) (`homeP`, gestures); [Room](../specifications/room-v2.md); Provenance lines; ADR-046 status to Accepted (if not already).

## Out of scope

- Server (S27), design (D18), reservations for another day, a reservation without a plan bubble, production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. Unit tests: migration; reserve, replace, clear by record, skip, rollover, wipe and reset; `BudgetCalculator` with a planned slot; the request DTO with `planned`; the receipt difference and its undo; idempotent double tap.
2. Roborazzi and emulator captures of `chatR`, `chatRL`, `homeP` against the D18 golds under the QA rules of AGENTS; partial validation.
3. One real day on the dev server (S27 deployed): reserve a dinner, ask for a snack plan (the panel and the server reservation agree), record the dinner (difference line), undo. Request ids recorded.
4. `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
