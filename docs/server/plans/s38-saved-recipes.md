# Plan — S38 Saved recipes in the Chat

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`recipes` rule), `llm.py` schema (`recipe_id`, `recipe` structure in a plan), `main.py` (`RECIPES`, `RECIPE_FULL` serialization, recipe log copy), `evals/`, tests.
- Related documentation: [ADR-052](../../produto/adrs/ADR-052-saved-recipes.md) (accepted with this plan), ADR-039/042/043, [v1-chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md); rule text `benchmark/prompts/new_instructions.py` (`recipes`); benchmark [results](../../../benchmark/RESULTADOS_08_10_2026.md) family `recipe`.
- Prerequisites: [S37](pending_manual_validation/s37-plan-options-and-discovery.md) delivered.

Approving this plan accepts ADR-052. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s38-saved-recipes.md. Implemente o plano aprovado.`

## Objective

The model sees the recipe index on every turn and the full recipe when the message names one; recalls, logs and adaptations of a saved recipe use the app's numbers.

## Scope

1. **Request.** `recipes[]` (id, name, kcal, p, c, g, key foods; at most 30) serialized as `RECIPES:` after `RECENT_DAYS`; `recipe_full` (one recipe: ingredients with grams and kcal, steps, totals) serialized as `RECIPE_FULL:` only when sent.
2. **Response.** A cooking plan returns `recipe: {name, ingredients[{name, g, kcal}], steps[]}` next to the estimate so the app can save it; `recipe_recall` action with `recipe_id`; a `log` by recipe copies the version's totals (server copies from `recipe_full`, confidence high, no re-estimate); "a mesma, mas com Y" is a `log` with `meal_change` over the recipe; "ajustada pra hoje" is a `plan` from `recipe_full` under the protein and window rules with `recipe_id`.
3. **Rules.** The `recipes` rule of the benchmark: index only names and totals; a recipe not in the index is answered as not saved, never invented; "ficou boa" → `liked` fact naming the recipe (S37 category); "salva" is answered that the app saves from the bubble (the model never saves). ADR-033 record.
4. **Smoke (at most 12 model calls).** Tag `s38`, synthetic, eight cases: recipe request with structured output, recall by name, ambiguous name, missing recipe, log by recipe, log with change, adapt for today, "ficou boa". Run once: `--tag s38 --repeat 1`.
5. **Dev deploy** and one recall on the dev app once A68 sends the index.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md): `recipes[]`, `recipe_full`, `recipe`, `recipe_recall`, `recipe_id`.

## Out of scope

- Storage, search, screens and the bubble action: [D23](../../design/plans/completed/d23-release-2-recipes.md), [A68](../../android/plans/a68-saved-recipes.md).

## Validation

1. `pytest server/tests -q` passes.
2. Smoke as in 4 (≤ 12 calls); numbers in Results.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
