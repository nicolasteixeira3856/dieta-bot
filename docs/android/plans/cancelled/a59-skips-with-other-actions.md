# Plan — A59 Skips next to other actions in the Chat

- Status: Cancelado (07/10/2026, dono: "ao invés de ter vários planos, concentre tudo num plano só, 1 para server e 1 para app"); scope carried unchanged into [A60](../a60-tone-formatting-planned-skips.md)
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (network DTOs and request flag, Chat ViewModel and recorder, the skip receipts, the delete-and-skip proposal card, Room column and migration, telemetry, tests) plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-*.sh`.
- Related documentation: [ADR-047](../../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [product Chat](../../../produto/specifications/chat.md) rules 4, 5, 7 and 19, [Room](../../specifications/room-v2.md), [HTTP contract](../../../api-contract.md).
- Prerequisites:
  - [D19](../../../design/plans/completed/d19-skips-with-other-actions.md) `Concluído` with `chatSK` and `chatSD` exported;
  - [S29](../../../server/plans/completed/s29-skip-slots.md) delivered and deployed to the dev server;
  - no parallel Android plan. A59 does not depend on A50, A55, A57 or A58: whichever runs later rebases and takes the next Room version. With A58 delivered, a planned slot is skipped at once (ADR-046); without it, that clause has nothing to do.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a59-skips-with-other-actions.md. Implemente o plano aprovado.`

## Objective

Apply every skip the server lists next to a log, plan, question or skip turn, each with its own receipt, and turn a skip of a meal that has a record into a one-tap proposal to delete it, so a message like the tester's (`Pular pré treino` + breakfast, 2026-10-07) records breakfast and skips the pre-workout.

## Scope

### 1. Wire

- `ChatIn.skip_slots: true` on every normal turn (always encoded, like `meal_changes`); a compact request drops it.
- `ChatOut.skipSlots: List<String>?`: absent (older server) keeps today's single `skip_slot` path; unknown ids and ids not in today's slots are dropped with telemetry `record_guard` (`reason` `slot_not_today`).

### 2. Apply

- In `autoRecord`, after the log's own outcome (record, `chatU`/`chatI` pending, or none; ADR-028 and ADR-032 rules unchanged), each listed slot in order, reading the slot state at that moment:
  - empty (or planned, once A58 exists) → skip with receipt `Pulado {slot}` and **Desfazer** (`chatSK`);
  - skipped → nothing, no receipt;
  - with a record → a delete proposal (`chatSD`).
- The skip outcomes of an answer are stored with the answer before any write, so recreation, retry and process death never apply a skip twice or lose a queued proposal. One pending card per answer: the log's confirmation first, then each delete proposal in list order; a resolved card reveals the next.
- **Excluir e pular**: one transaction rechecks the request day, the latest wipe, and the slot's records against the captured state; removes the slot's records as Excluir does (memory of the record's active receipt reverted) and marks the slot skipped; receipt `Pulado {slot}` with **Desfazer**, which restores the records and the memory as the replacement undo does. **Manter registro**: nothing written, card marked `Registro mantido`. Expiry: next send, day change, wipe, the slot changed by another path → `Não registrado` on that card.
- A write failure on a skip marks only that skip `Não registrado`; the log receipt stays.

### 3. Room

- Next schema version (10 → 11 today; the next free number at start if another plan migrated first): nullable `chat_message.skipOutcomes`, a versioned JSON of the answer's listed slots with their state at capture and their outcome (`skipped` | `already` | `pending_delete` | `deleted` | `kept` | `expired` | `failed`); `recordState` unchanged for the log part. Non-destructive migration, exported schema, migration test with messages, logs and an active receipt.

### 4. Telemetry

- `meal_skipped` (`from` `chat`) gains `with` (`log` | `plan` | `question` | `skip`); new `skip_delete` (`shown` | `confirmed` | `kept` | `expired` | `undone`). Enums only.

### 5. QA tooling

- `tools/fake-chat-server.mjs` answers a log with `skip_slots` and a skip of a recorded slot; `tools/capture-chat.sh` (or the script that owns `chatG`) gains `SCENES=a59`: `chatSK`, `chatSD`, confirm, keep, undo.

### Intended specification changes

At Completion: [product Chat](../../../produto/specifications/chat.md) rule 4 (one record plus the skips of other slots), rule 7 (skips as a list next to any intent; a skip over a record asks), rule 19 (the skip receipt over a deleted record: Desfazer restores it), states (`chatSK`, `chatSD`) and acceptance criteria; [Room](../../specifications/room-v2.md) (new version, `skipOutcomes`); Provenance lines; ADR-047 status to Accepted (if not already).

## Out of scope

- Server (S29), design (D19), Home gestures, skips of another day, inferring skips, production (blocked by the [production gate](../../../content-policy/production-gate.md)).

## Validation

1. Unit tests (fake service + real Room): the incident shape (log + one skip) records once and skips once with two receipts; two skips; skip + plan and skip + question; already skipped slot; skip over a record → proposal, confirm (records gone, skipped, memory reverted), keep, expiry by send, day change, wipe and a change by another path; Desfazer restores the record; a log `chatU` pending plus a delete proposal queue in order; retry and recreation apply nothing twice; older server without `skip_slots` keeps today's flow; migration.
2. Roborazzi and emulator captures of `chatSK` and `chatSD` against the D19 golds under the QA rules of AGENTS, with a written diff list; regress `chatG` and `chatU` (partial validation: Chat records flows only).
3. Dev server (S29 deployed): one synthetic message with a skip and a breakfast; request id recorded.
4. `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.
5. Manual acceptance (after delivery, [autonomous run](../../../sdd/autonomous-run.md)): owner or tester sends a skip next to a meal on a device (dev build) and skips a recorded meal. Not a condition for the automated Completion; the plan waits in `pending_manual_validation/`.

## Results

Planning only.
