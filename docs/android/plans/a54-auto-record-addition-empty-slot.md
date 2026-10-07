# Plan — A54 Auto-record of an addition into an empty meal

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Chat ViewModel and recorder, `domain/MealChanges`, telemetry, unit tests) plus the Chat QA tooling `tools/fake-chat-server.mjs` for the reproduction scene.
- Related documentation: [product Chat](../../produto/specifications/chat.md) rules 4, 5 and 22, [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [HTTP contract](../../api-contract.md#meal-change-capability).
- Prerequisites: [A47](pending_manual_validation/a47-chat-meal-updates.md) delivered (it is, in 0.0.13); no parallel plan on the same Chat files.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a54-auto-record-addition-empty-slot.md. Implemente o plano aprovado.`

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

At Completion: [product Chat](../../produto/specifications/chat.md) rule 8 (history line of an unrecorded `auto` answer) and rule 22 (reasons reported); Provenance line. No Room change expected; if the reproduction needs a column, the plan stops and comes back to Planning.

## Out of scope

- Server rules or the `invalid meal base` rejection (a server plan may later turn an `add` with a stale `base_slot` into `base_slot null` instead of failing the turn; not this plan).
- Tone, closures, protein objective: [S24](../../server/plans/s24-protein-first-plan.md), [A55](a55-tone-choice-and-closures.md).
- Golds, layout, production distribution.

## Validation

1. The reproduction test of scope 1 fails before the fix and passes after; the cause is written in Results.
2. ViewModel tests for every `record_guard` reason of scope 3 and for the history line of scope 4.
3. Emulator, dev flavor, `SCENES=a54` against the fake server: the dinner is recorded, the receipt shows, the Home timeline has the slot; a second turn's request body carries the slot as `eaten`. Partial validation: only this flow.
4. One real turn through the dev server from a fresh day: a plan, then "vamos com X" that the server marks `auto` with an addition into the empty slot; the record exists before the next send. Request id recorded, no user content committed.
5. `testDevDebugUnitTest` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.

## Results

Planning only.
