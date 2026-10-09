# Plan — A68 Saved recipes

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message A64–A69; code, tests, the `rcpL`, `rcpD` and `chatRK` gold comparisons and emulator checks done; the device smoke on a dev build is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Room entities `recipe` and `recipe_version` (v14), local search by name and ingredient, `PromptBuilder` (`recipes[]` index, `recipe_full` when found), "Salvar receita" action under a cooking plan, Config entry, list and detail screens (`rcpL`, `rcpD`), record by recipe with receipt, tests, captures.
- Prerequisites: [S38](../../../server/plans/pending_manual_validation/s38-saved-recipes.md) on the dev server; [A67](a67-plan-options-and-discovery.md) delivered; [D23](../../../design/plans/completed/d23-release-2-recipes.md) `Concluído`.
- Related documentation: [ADR-052](../../../produto/adrs/ADR-052-saved-recipes.md), ADR-017/048, [product Chat](../../../produto/specifications/chat.md), [memoria-push](../../../produto/specifications/memoria-push.md), [Room](../../specifications/room-v2.md), [gold inventory](../../../qa/README.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a68-saved-recipes.md. Implemente o plano aprovado.`

## Objective

A recipe the model built can be saved, listed, opened, sent back to the model by name and recorded with its own numbers.

## Scope

1. **Storage.** `recipe` (id, name, origin message id, created) and `recipe_version` (version, ingredients JSON with grams and kcal, steps, kcal, p, c, g, yield, portion); editing creates a version; a record keeps `recipe_version_id`.
2. **Save.** "Salvar receita" under a plan that carries `recipe` (ADR-048 action row); receipt `Receita salva`.
3. **Prompt.** `recipes[]` index on every turn (≤ 30, most recent first); when the message names a saved recipe (normalized name or a key ingredient match), `recipe_full` of its current version.
4. **Record.** A `log` with `recipe_id` records the version's totals without re-estimation; a `log` with `meal_change` over a recipe records the changed numbers.
5. **Screens.** Config → Receitas: list (`rcpL`) and detail (`rcpD`) per D23 golds; delete with confirmation.
6. **Tests and captures.** Migration test, search tests, `PromptBuilder` tests; captures of `rcpL`, `rcpD` and the save action vs the golds.

### Specification changes at Completion

- [memoria-push](../../../produto/specifications/memoria-push.md) (Config entry, screens), [product Chat](../../../produto/specifications/chat.md) (save action, recipe records), [Room](../../specifications/room-v2.md) (v14), [gold inventory](../../../qa/README.md).

## Out of scope

- Sharing or importing recipes; a food database.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures vs the golds `rcpL`, `rcpD`; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): save a recipe, "lembra a receita X?", record by recipe, delete.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 08/10/2026 on `feat/a68-saved-recipes`, in the autonomous run of A64–A69 ([report](../../validation/batch-2026-10-08-a64-a69.md)).

### Delivered

- **Storage (Room v15; the plan said v14, moved by A64's v13).** `recipe` (id, name, origin plan row, created) and `recipe_version` (version, ingredients JSON with grams and kcal, steps JSON, kcal, P, C, G, nullable yield and portion, created; cascade on the recipe); the current version is the highest. `meal_log.recipeVersionId` (and `SlotRecord.recipeVersionId`, so Desfazer and moves keep it): the version a record by recipe used; deleting a recipe keeps the record. Editing a recipe (a new version) has no UI in the D23 golds, so only version 1 is written today.
- **Save.** **Salvar receita** under a plan whose own action carries `recipe` (the `chatRK` stack, under Registrar assim), once per plan; it saves version 1 with the plan's totals and leaves the `Receita salva: {nome}` receipt; nothing is recorded. Telemetry `recipe_saved`.
- **Prompt.** `recipes[]` on every normal turn (≤ 30, most recent first: `R{n}`, name ≤ 60, current totals, the three ingredients with most energy as `key_foods`); `recipe_full` when the message names a saved recipe (`Recipes.named`: the name without accents, all its meaningful words, or a key food with the word "receita"), with ingredients (g > 0, ≤ 30) and steps (≤ 10). Never on compact.
- **Record.** A `log` with `recipe_id` records the numbers the server copied and keeps the current version id; a `log` with `meal_change` over a recipe records the changed numbers through the normal paths.
- **Screens.** Config → `Da Tali` → **Receitas** (the `cfg` block of D24, its second row is A69) → list (`rcpL`) → recipe (`rcpD`) with **Excluir receita** behind a `Dialog/Confirm`; an empty list shows one line (no gold). Telemetry `recipe_deleted`; screen ids `rcpL`, `rcpD`.

### Gate changes

- `GoldTest.chatRK` back to its full second box (now to the bottom of the D23 frame, 1125 dp) with **Salvar receita** in the fixture: the temporary exception of A64 is gone.
- `cfgS`, `cfgR` and `wipe` predate the D24 `cfg`: their JVM gate moved to a region (`cfgS` above the frame's bottom edge, where the new block's label starts; `cfgR` and `wipe` on the dialog box the emulator gate uses), all at 0.00 %.
- Roborazzi app baselines `chatRK` and `cfgWorkout` re-recorded (the new action and the new block).

### Validation

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: 711 tests; the 4 failures of the first run (`wipe_light` and the three Roborazzi baselines above) were the expected effects of the new block and action, fixed as listed, and the rerun of the GoldTest and Roborazzi suites passes. New: `ChatRecipesTest` (2: save once with version 1 and totals, nothing recorded, the index with key foods, `recipe_full` on the naming turn, a record by recipe with its version; the Config list, detail and delete), `RecipesTest` (2), `MigrationV14V15Test`, `GoldTest.rcpL_*`, `rcpD_*` (new).
2. Emulator (Medium_Phone 780 × 1688 @ 320, devDebug against the fake, `SCENES=a68`, both themes): every check passed — Salvar receita under the plan, `Receita salva: Frango com brócolis e arroz`, version 1 at 520 kcal, nothing recorded, the index and `recipe_full` on the wire, the record by recipe with its version, five rows in `rcpL`, the version line in `rcpD`, the delete confirmation and one recipe deleted (the light run was repeated after a cleanup fix in the script: Python's sqlite does not cascade). `node tools/diff-gold.mjs`: `rcpL` 0.38 % / 0.39 %, `rcpD` 0.18 % / 0.20 % (pass). Flows left out (Delivery pace rule): the other Chat scenes, Home, the rest of Config, onboarding.
3. Device smoke on a dev build: **not run** (owner decision of 08/10/2026: no test build per plan, one build at the end); moved to the manual acceptance.
4. `node tools/check-docs.mjs` passes.

### Manual acceptance (after delivery)

- On a dev build: save a recipe from a cooking plan, "lembra a receita X?" brings it back, "jantei a X" records its numbers, and Config → Receitas lists, opens and deletes it.

