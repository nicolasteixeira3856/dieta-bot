# Plan — A69 "O que a Tali sabe": memory screen in Config

- Status: Concluído (09/10/2026, owner acceptance: "todos os planos pendentes de minha revisão no Android estão aprovados")
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Config entry and screen (`memL`), fact list with origin, delete and correct, tombstones for deleted facts (Room v15), removal of the dev tool A23, tests, captures.
- Prerequisites: [D24](../../../design/plans/completed/d24-release-2-visible-memory.md) `Concluído`; [A68](a68-saved-recipes.md) delivered (Room version order).
- Related documentation: [ADR-053](../../../produto/adrs/ADR-053-visible-memory-screen.md), ADR-019/023/029, [memoria-push](../../../produto/specifications/memoria-push.md), [Room](../../specifications/room-v2.md), [gold inventory](../../../qa/README.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a69-visible-memory.md. Implemente o plano aprovado.`

## Objective

The user sees every fact Tali keeps, with its origin, and can delete or correct it; a deleted fact never comes back from a digest.

## Scope

1. **Screen.** Config → O que a Tali sabe (`memL`): facts grouped by kind (fixas, rotinas, temporárias), each with category, slot, text, macros, origin (`declarado`, `registrado N dias`, `até {date}`) and date; actions delete (confirmation) and correct (text field, same category and slot).
2. **Tombstones.** A deleted fact's key is kept with a deletion date; `MemoryRules` rejects a `memory_updates` add with the same key until the next compaction; digests never recreate it.
3. **Dev tool.** `Memória da IA (dev)` (A23) removed from the dev flavor; `hide_dev_tools` keeps its meaning.
4. **Tests and captures.** Unit tests of tombstones and correction; migration test; capture of `memL` vs the gold; `capture-config.sh` updated.

### Specification changes at Completion

- [memoria-push](../../../produto/specifications/memoria-push.md) (screen, tombstones), [Room](../../specifications/room-v2.md) (v15), [gold inventory](../../../qa/README.md), [ADR-019](../../../produto/adrs/ADR-019-ferramentas-dev.md) status line.

## Out of scope

- O6 onboarding (conditional). Goal weight ([A56](../out_of_scope/a56-goal-weight.md), deferred).

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Capture of `memL` vs the gold; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): delete a routine, send "café de sempre", the routine is not recreated; correct a preference and see it in the next prompt.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 08/10/2026 on `feat/a69-visible-memory`, in the autonomous run of A64–A69 ([report](../../validation/batch-2026-10-08-a64-a69.md)). ADR-053 accepted with this approval (status line); ADR-019's status line records the partial supersession.

### Delivered

- **Screen (`memL`).** Config → `Da Tali` → **O que a Tali sabe** (second row of the D24 block; `cfg` now gated at its full 1271 dp). Groups `FIXAS` (permanent, not a routine), `ROTINAS` (routines, liked dishes and what was learned) and `TEMPORÁRIAS`, each with its line; empty groups hidden; an empty memory shows one line (no gold). Per fact: chip (`Preferência`, `Porção`, `Equipamento`, `Rotina · {slot}`, `Prato aprovado · {slot}`), pencil and trash, text, the numbers of a routine or liked dish, origin (`Declarado · dd/MM`, `Registrado N dias · último dd/MM`, `Até dd/MM · criado dd/MM`). Correct: the text in a field with Cancelar | Salvar (`FactMemory.correct`: text only, same category, slot and kind; blank changes nothing). Delete: `Dialog/Confirm` (`Apagar da memória?`). Telemetry `memory_fact_deleted`, `memory_fact_corrected` (`kind`, `category`); screen id `memL`.
- **Tombstones.** Deleting a non-temporary fact keeps `{key, deleted}`; `MemoryRules.apply` refuses an `add` with the same key (temp facts never blocked) until the next stored compaction, when `ChatViewModel` clears them. Digests therefore cannot bring the fact back while the deletion is in the raw history.
- **Dev tool.** `Memória da IA (dev)` (A23: `DevMemoryScreen`, its ViewModel, parsers and tests) removed; the dev flavor now adds no route and no Config row; `debug.fibrai.hide_dev_tools` keeps its meaning (AGENTS updated); `capture-config.sh` checks there is no dev row and that the `Da Tali` rows exist.

### Deviation

- **Tombstones live in the encrypted memory file, not in Room (the plan said Room v15).** The memory file already owns every fact (`memory.bin`, JSON `v: 2`), so the tombstones sit next to them (`"tombstones": [{"key", "deleted"}]`; older files read as none): one locked write per delete, the same lifetime as the facts (reset and uninstall remove both, `wipeToday` keeps both) and no Room version or migration for it. The Room schema stays at v15.

### Validation

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: 692 tests (the A23 tests left with the tool); the one failure, the Roborazzi app baseline `cfgWorkout` (the second `Da Tali` row), was re-recorded and the Roborazzi suite rerun passes. New: `MemoryViewModelTest` (4: groups, chips, macros and origins; delete keeps a tombstone, an add with the same key refused, another key and a temp fact accepted, accepted again after the compaction; correction changes only the text and a blank changes nothing; the rules alone, no tombstone for a temp fact), `GoldTest.memL_*` (new, 0.14 % / 0.24 % after the layout pass); `cfg_*` at the full D24 height; `FactMemoryTest` expects `tombstones` in the stored format. The A23 tests left with the tool.
2. Emulator (Medium_Phone 780 × 1688 @ 320, devDebug against the fake, both themes): `SCENES=a69` — facts made in the Chat listed with their groups, chips and origins, the correction saved and sent in the next prompt, the preference deleted after the confirmation and not recreated when the model adds it again (tombstone); `memL` gated on its content-independent top (header and the first group label, 0.00 % / 0.00 %; the facts are the device's own, the encrypted memory cannot be seeded, so the rest is reported, and the JVM `GoldTest` gates the whole frame with the gold's facts). `tools/capture-config.sh`: no dev tool row, both `Da Tali` rows, `cfg` 0.00 % / 0.00 %, `wipe` 0.20 % / 0.21 %, `cfgR` 0.00 % / 0.00 %.Flows left out (Delivery pace rule): the Chat scenes other than `a69`, Home, onboarding.
3. Device smoke on a dev build: **not run** (owner decision of 08/10/2026: no test build per plan, one build at the end); moved to the manual acceptance.
4. `node tools/check-docs.mjs` passes.

### Manual acceptance (after delivery)

- On a dev build: delete a routine in O que a Tali sabe, send "café de sempre": the routine is not recreated; correct a preference and see it used in the next answer.

