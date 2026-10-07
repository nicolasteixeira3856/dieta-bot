# Plan — A59 Skips next to other actions in the Chat

- Status: Pendente aprovação manual
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
5. Manual: owner or tester sends a skip next to a meal on a device (dev build) and skips a recorded meal.

## Results

### Implementation (07/10/2026)

- **Wire:** `ChatIn.skipSlots` (`skip_slots`, always encoded, `true` on normal turns, `false` on a compact request); `ChatOut.skipSlots: List<String>?` (null = older server: the single `skip_slot` path stays as it was). Unknown ids or ids that are not slots of today are dropped with `record_guard` `slot_not_today`; the log's own suggested slot is dropped too.
- **Domain:** `domain/SkipOutcomes.kt`, versioned JSON stored with the answer before any write (request day, latest wipe, the answer's intent, each listed slot with its state when applied and its outcome).
- **Apply (`ChatViewModel.applySkips`):** after the log's own outcome, in list order, reading the slot now: empty → `ChatRecorder.skipListed` (receipt `Pulado {slot}` + Desfazer, the outcome moves in the same transaction); skipped → `already`; with a record → `pending_delete` with the state shown. A slot that moves under the write is read again (3 attempts, then `expired`); a write failure marks that skip `failed`. Skips left pending by a dead screen or process are applied when the Chat opens the same day (`resumeSkips`); every move is conditional on the stored outcome, so two callers never apply one skip twice.
- **chatSD:** `SkipDeleteCard` (`ChatRecordComponents.kt`): `Pular {slot}?`, the copy of ADR-047, **Excluir e pular** (`AeroDangerButton`, now public with a `content` color: `statusOnBad`, white in both themes; the dialog keeps `accentOn`) over **Manter registro** (`OutlinePill`). It renders after the answer's receipts; one card at a time: the answer's own `chatU` / `chatI` / `chatIC` first. `confirmSkipDelete` → `ChatRecorder.deleteAndSkip` (one transaction: slot from the shown state to skipped, `RecordGuard` day + wipe, outcome `pending_delete` → `deleted`, the record's active receipt marked `deleted`), then the memory of that receipt is reverted and the revert images are kept on the skip receipt, so Desfazer brings the record and the memory back (`restored` receipt). `keepRecord` → `kept` (`Registro mantido`, Check icon, muted). Expiry: next send (`expireOpen`), day change, wipe, slot changed by another path (render check + Room recheck) → `expired` (`Não registrado`).
- **Room v11:** `chat_message.skipOutcomes` (`MIGRATION_10_11`, schema `11.json` exported), `ChatMessageDao.skipOutcomesOf` / `setSkipOutcomes` / `getOpenSkips`, `DayRepository.moveSkip`, `openSkips`, `commitRecord(skip = SkipMove)`.
- **Telemetry:** `meal_skipped` (`from` `chat`) gains `with` (`log` | `plan` | `question` | `skip`, the legacy skip path sends `skip`); new `skip_delete` (`shown` | `confirmed` | `kept` | `expired` | `undone`). Enums only.
- **QA tooling:** `tools/fake-chat-server.mjs` `{"skips": [prefixes]}` adds `skip_slots` for a client that sent the flag (`/__calls` reports `skipSlots`); `tools/capture-chat.sh` `SCENES=a59`; `tools/diff-gold.mjs` gates `chatSK` as a long thread (header from the top, tail from the bottom) and `chatSD` as a whole screen.
- **Specifications:** product Chat rules 4, 7 and 19, states, acceptance criteria, related ADR-047 and Provenance; Room v11 (rule 15, migration, Provenance); `docs/qa/README.md` (`SCENES=a59`).

### Validation

1. **Unit tests** (`ChatSkipsTest`, 14 tests, fake service + real Room): incident shape (one record, one skip, two receipts, `skip_slots` true on the wire, `meal_skipped` `with` `log`); two skips in order with the log's slot never skipped; skips next to a plan and a question; already skipped; unknown ids; skip over a record → proposal, confirm (record gone, skipped, routine reverted, receipt `Excluído`, double tap inert), Desfazer (record and routine back, `Restaurado`); keep; expiry by send, day change, wipe and another path; the log's `chatU` before the delete proposal, which then renders last; recreation and a simulated process death apply each skip once; an older server keeps the single skip path. `MigrationV10V11Test`: v10 rows survive, `skipOutcomes` null, a migrated receipt still undoes. `MigrationV1V2Test`, `MigrationV9V10Test` and `RoomV2Test` follow the version (11).
2. **`testDevDebugUnitTest`:** 591 tests, 6 failures, all GoldTest `chatE`, `chatR` and `cfg` (both themes). The same 6 fail on `master` without A59 (their golds changed in the PR #161 closures and wait for A55/A57/A58); nothing else fails. **`verifyRoborazziDevDebug`:** new baselines `chatSK`, `chatSD` (both themes, `chatSK` at 1050 dp); `RoborazziSmokeTest` verifies clean. **`assembleDevRelease`:** OK.
3. **JVM gold (GoldTest):** `chatSK` 0.00 % / 0.00 % (dark / light, blurred), `chatSD` 0.33 % / 1.37 %; `chatG`, `chatU` regress clean (≤ 0.03 %).
4. **Emulator** (Medium_Phone at 780 × 1688 @ 320, `SCENES=a59`, both themes): every check passed (two receipts, `skip_slots` true, lunch skipped, proposal with nothing changed before the tap, Excluir e pular, Desfazer, Manter registro, expiry on the next send, seeded `chatSK` and `chatSD`). `node tools/diff-gold.mjs`: `chatSK` header 0.00 % / 0.15 %, thread tail 0.01 % / 0.40 %; `chatSD` whole screen 0.38 % / 1.69 % (max 2 %).
5. **Diff list** (captures `docs/qa/android/current/{dark,light}/chatSK.png`, `chatSD.png` against the D19 golds): layout, tokens, type sizes, radius, receipt rows and action stack, danger CTA (`status/bad`, white label and icon in both themes), outline pill and semantic macro colours match. Differences: clock, status bar, date chip and message times (ignored); `chatSK` on the phone shows the newest part of the 1050 dp thread (long-thread rule); the short user bubble of `chatSD` hugs its text in the app while the gold keeps the 288 px width of the `chatU` frame it was cloned from (Figma artifact, inside the gate; the app keeps the bubble behaviour of every other Chat gold).
6. **Dev server** (S29 deployed): a devDebug build sent `pulei o almoco. no cafe comi 2 ovos mexidos e 1 pao frances` from the emulator; the breakfast was recorded (`+340 kcal`, Excluir · Trocar refeição · Editar) and the lunch skipped with `Pulado Almoço` + Desfazer. Request `5c8a525b-274e-4b4e-8574-b1f5a378988b` (`skips` 1, `record` `auto_log`).
7. `node tools/check-docs.mjs` passes.

### Pending manual validation

Validation 5 of the plan: the owner or a tester sends a skip next to a meal on a device (dev build) and skips a recorded meal. Needs a new dev distribution (`tools/distribute-dev.ps1`) with A59.
