# Plan — A54 Auto-record of an addition into an empty meal

- Status: Concluído (07/10/2026)
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Chat ViewModel and recorder, `domain/MealChanges`, telemetry, unit tests) plus the Chat QA tooling `tools/fake-chat-server.mjs` for the reproduction scene.
- Related documentation: [product Chat](../../../produto/specifications/chat.md) rules 4, 5 and 22, [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), [HTTP contract](../../../api-contract.md#meal-change-capability).
- Prerequisites: [A47](../completed/a47-chat-meal-updates.md) delivered (it is, in 0.0.13); no parallel plan on the same Chat files.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a54-auto-record-addition-empty-slot.md. Implemente o plano aprovado.`

## Objective

An answer marked `record: auto` with `meal_change {operation: add, base_slot: null}` into an empty meal of today is recorded on the device, as rules 4 and 5 require. When the record cannot happen, the app says so (`Não registrado`) and reports why; it never leaves the reply's "Total do {slot}" text standing over an empty slot.

## Discovery evidence

Dev conversation log of 2026-10-06 (ADR-015), app 0.0.16-dev, one tester, six-slot profile:

| Time | Turn | Server answer | DAY of the next request |
|---|---|---|---|
| 16:53:56 | "Vamos com 3 ovos mexidos" | `intent log`, `record auto`, `suggested_slot 5`, `meal_change {add, base_slot null, addition 263 kcal}`; reply rewritten by the server to `+263 kcal` / `Total do Jantar: 263 kcal` | slot 5 `empty` at 16:54:33, 16:54:59 and 16:55:33 |
| 16:54:59 | "Pode acrescentar tbm, me retorno então tudo estimado certinho" | model sent `add` with `base_slot "5"`; server `ValueError` (`invalid meal base`) → fallback `nao deu pra estimar` | slot 5 still `empty` |
| 16:55:33 | a full dinner list | same `ValueError`, same fallback | — |

The record of 16:53:56 should have been written (empty target, valid addition, numbers equal to the addition, `record auto`). It was not. With HISTORY saying "Total do Jantar: 263 kcal" and DAY saying the dinner was empty, the model inferred an occupied dinner and sent `base_slot "5"` twice; the server rejected both turns. The two fallbacks are the "erros constantes" the owner reported.

Code paths read for this plan, none of which proves the cause alone: `ChatViewModel.recordModeOf` (client guards), `MealChanges.proposal`/`check` (metadata equality against the captured states), `proposalHoldsNow` (freshness), `autoRecord` (`proposal.target ?: return` and `slot ?: return` exit silently), `addInto` (`compose`, `recorder.record` with `expected = destination`), and the A47 freshness rule (a proposal that arrived after the day, wipe, source or destination changed is `Não registrado`).

## Scope

### 1. Reproduction first

- A unit test on the ViewModel with a fake service that returns the exact shape of the 16:53:56 answer (synthetic foods, same fields: whole-number totals, `g` as a decimal, `meal_change` with `base_slot` null and an `addition` whose items sum to its kcal) against a day where the target slot is empty and other slots are eaten. The test asserts the record, the receipt and `meal_auto_recorded`.
- `tools/fake-chat-server.mjs` gains a `{"a54": ...}` switch returning that answer; `tools/capture-chat.sh` gains `SCENES=a54` for an emulator run of the same turn.
- The test is written before the fix and must fail or expose the silent exit; the cause goes into Results.

### 2. Fix

- Whatever the reproduction shows: the equality check in `MealChanges.check` for an addition into an empty target (text or number normalization), the freshness check, or the silent exits of `autoRecord`. No change to the ADR-032 semantics: an addition into an empty meal records only the added food; an occupied destination still asks **Adicionar**.

### 3. Never silent

- Every path where an `auto` answer ends without a record sets `recordState` `NOT_RECORDED` on the row (the bubble shows `Não registrado`, rule 22) and emits `record_guard` with an enum reason (`no_target`, `stale`, `contradicts`, `overflow`, `write_failed`). Today `autoRecord` returns without either in two places.

### 4. History consistency

- When an `auto` answer is not recorded, the assistant row keeps the server reply but the history line sent on the next turn does not carry the server's `Total do {slot}` sentence: the app sends the first line of the reply (the addition and its `+kcal`) and the `[refeição sugerida]` marker, so DAY and HISTORY agree. Rule 8 of the Chat specification gains this sentence at Completion.

### Intended specification changes

At Completion: [product Chat](../../../produto/specifications/chat.md) rule 8 (history line of an unrecorded `auto` answer) and rule 22 (reasons reported); Provenance line. No Room change expected; if the reproduction needs a column, the plan stops and comes back to Planning.

## Out of scope

- Server rules or the `invalid meal base` rejection (a server plan may later turn an `add` with a stale `base_slot` into `base_slot null` instead of failing the turn; not this plan).
- Tone, closures, protein objective: [S24](../../../server/plans/s24-protein-first-plan.md), [A55](../a55-tone-choice-and-closures.md).
- Golds, layout, production distribution.

## Validation

1. The reproduction test of scope 1 fails before the fix and passes after; the cause is written in Results.
2. ViewModel tests for every `record_guard` reason of scope 3 and for the history line of scope 4.
3. Emulator, dev flavor, `SCENES=a54` against the fake server: the dinner is recorded, the receipt shows, the Home timeline has the slot; a second turn's request body carries the slot as `eaten`. Partial validation: only this flow.
4. One real turn through the dev server from a fresh day: a plan, then "vamos com X" that the server marks `auto` with an addition into the empty slot; the record exists before the next send. Request id recorded, no user content committed.
5. `testDevDebugUnitTest` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.

## Results

Approved by the owner on 06/10/2026 (overnight run, message naming this file). Implemented the same night.

### Cause (scope 1)

- The reproduction test `ChatAutoAdditionTest.autoAdditionIntoTheEmptyDinner_afterPlans_isRecorded_withReceipt_andTheNextDaySaysEaten` replays the 16:53:56 shape (synthetic foods: six slots, four eaten, three plans and a question for the dinner, then `log`/`auto`/`add` with `base_slot` null, whole totals, item grams as decimals, server-rewritten reply). It **passed before any change**: the record path was not broken.
- The Crashlytics breadcrumbs of the two `ChatFallback` non-fatals of that session (0.0.16-dev) show what happened: `meal_update {op=add, action=shown}`, `meal_update {action=confirmed}`, `meal_saved {kcal=263}`, `meal_auto_recorded {slot_state=empty}` at 16:53:59, then Home, Chat, and `receipt_action {action=delete, receipt=logged, age_s=13}` at 16:54:13. The dinner was recorded and the tester removed it with **Excluir** 13 s later.
- The discovery's reading ("should have been written ... it was not") was therefore wrong; the slot was empty in the next DAY because of the Excluir. The real defect is the one the Objective names: the reply's `Total do Jantar: 263 kcal` kept standing in HISTORY over an empty dinner, the model inferred an occupied dinner and sent `base_slot "5"` twice, and the server rejected both turns.
- Decision not covered by the plan (taken by the agent, owner asleep, most conservative reading of the Objective): the scope 4 history rule applies whenever an addition answer's destination is **not eaten in today's DAY**, which covers both an unrecorded `auto` answer and a recorded one whose record was removed (Excluir, Desfazer, Editar). No change to the record path of scope 2 (`MealChanges.check`, freshness), since the reproduction showed none was needed. The server-side alternative (turning a stale `base_slot` into `null`) stays out of scope as the plan says.

### Delivered

- `PromptBuilder.chatTurn`: an assistant row with an addition proposal whose destination slot has no log today goes to HISTORY (and to compact blocks) as its first line plus `[refeição sugerida: …]`.
- `ChatViewModel.autoRecord`: no silent exit. Missing slot or target → `no_target`; a stale write or skip → `stale`; a composed overflow → `overflow`; an exception in the write → `write_failed`. Each sets `Não registrado` (when still open) and emits `record_guard {reason}`. In `post`, an `auto` answer marked `Não registrado` up front emits `record_guard` with `malformed`, `contradicts`, `overflow` (invalid proposal) or `stale` (arrived after the day, wipe or slot changed). `malformed` is an added enum value next to the five the plan listed (the proposal reason already exists; enum only).
- `tools/fake-chat-server.mjs` `{"a54": true}` and `/__calls.day`; `tools/capture-chat.sh` `SCENES=a54`.
- Specs: [product Chat](../../../produto/specifications/chat.md) rule 8 (history line) and rule 22 (reasons), Provenance.

### Validation

1. Reproduction test: passes before and after (cause above). The Excluir test `excluirAfterTheAutoRecord_nextHistoryKeepsOnlyTheAddedFood_notTheMealTotal` **fails without** the `PromptBuilder` change (checked by stashing it: 7 tests, 1 failed) and passes with it.
2. `ChatAutoAdditionTest`, 7 tests: recorded addition keeps the whole reply in HISTORY; Excluir drops the total; `stale` (dinner written while the answer was on its way), `contradicts` (estimate 300 vs addition 263), `malformed` (`addition` key missing), `overflow` (1990-character dinner + addition). `no_target` and `write_failed` are defensive paths: `recordModeOf` already turns an answer without a slot of today into `ask`, and Robolectric has no injectable write failure; both are covered by code review only.
3. Emulator (`emulator-5554`, API 36, gold geometry), dev flavor against the fake server, `SCENES=a54 tools/capture-chat.sh dark`: receipt in the Chat, no `Não registrado`, `meal_log` `[('Jantar', 263, 20, 1, 24)]`, answer `auto`/`recorded`, Home `263` kcal consumidas, next request DAY dinner `eaten:263`. Partial validation: only this flow; onboarding ran as the script's setup and its captures were not kept. Other flows left out: no shared component or layout changed, and the JVM regression below passed.
4. One real turn through the dev server from a fresh day (emulator, devDebug, 0.0.16-dev build of this branch): a dinner plan, then "vamos com isso, ja comi agora" → `log`/`auto`, recorded into Jantar (507 kcal) before the next send. The server chose `operation: new` (a whole meal, not an addition), so the addition path itself was exercised only by tests 1–3. Request ids `6f20f09b-d145-44d7-aa7f-d4c5f75d4a19`, `3d6209de-3afb-43fa-8a35-0098a1a60fa8`. No user content committed.
5. `testDevDebugUnitTest`: 568 tests, 0 failures (includes `GoldTest`); `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.
