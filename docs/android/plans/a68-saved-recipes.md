# Plan — A68 Saved recipes

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Room entities `recipe` and `recipe_version` (v14), local search by name and ingredient, `PromptBuilder` (`recipes[]` index, `recipe_full` when found), "Salvar receita" action under a cooking plan, Config entry, list and detail screens (`rcpL`, `rcpD`), record by recipe with receipt, tests, captures.
- Prerequisites: [S38](../../server/plans/s38-saved-recipes.md) on the dev server; [A67](a67-plan-options-and-discovery.md) delivered; [D23](../../design/plans/d23-release-2-recipes.md) `Concluído`.
- Related documentation: [ADR-052](../../produto/adrs/ADR-052-saved-recipes.md), ADR-017/048, [product Chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md), [Room](../specifications/room-v2.md), [gold inventory](../../qa/README.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a68-saved-recipes.md. Implemente o plano aprovado.`

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

- [memoria-push](../../produto/specifications/memoria-push.md) (Config entry, screens), [product Chat](../../produto/specifications/chat.md) (save action, recipe records), [Room](../specifications/room-v2.md) (v14), [gold inventory](../../qa/README.md).

## Out of scope

- Sharing or importing recipes; a food database.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures vs the golds `rcpL`, `rcpD`; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): save a recipe, "lembra a receita X?", record by recipe, delete.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
