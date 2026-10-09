# Plan — S38 Saved recipes in the Chat

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message; code, unit tests, smoke and dev deploy done; the recall on the dev app waits for A68 to send the index)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`recipes` rule), `llm.py` schema (`recipe_id`, `recipe` structure in a plan), `main.py` (`RECIPES`, `RECIPE_FULL` serialization, recipe log copy), `evals/`, tests.
- Related documentation: [ADR-052](../../../produto/adrs/ADR-052-saved-recipes.md) (accepted with this plan), ADR-039/042/043, [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md); rule text `benchmark/prompts/new_instructions.py` (`recipes`); benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md) family `recipe`.
- Prerequisites: [S37](../pending_manual_validation/s37-plan-options-and-discovery.md) delivered.

Approving this plan accepts ADR-052. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s38-saved-recipes.md. Implemente o plano aprovado.`

## Objective

The model sees the recipe index on every turn and the full recipe when the message names one; recalls, logs and adaptations of a saved recipe use the app's numbers.

## Scope

1. **Request.** `recipes[]` (id, name, kcal, p, c, g, key foods; at most 30) serialized as `RECIPES:` after `RECENT_DAYS`; `recipe_full` (one recipe: ingredients with grams and kcal, steps, totals) serialized as `RECIPE_FULL:` only when sent.
2. **Response.** A cooking plan returns `recipe: {name, ingredients[{name, g, kcal}], steps[]}` next to the estimate so the app can save it; `recipe_recall` action with `recipe_id`; a `log` by recipe copies the version's totals (server copies from `recipe_full`, confidence high, no re-estimate); "a mesma, mas com Y" is a `log` with `meal_change` over the recipe; "ajustada pra hoje" is a `plan` from `recipe_full` under the protein and window rules with `recipe_id`.
3. **Rules.** The `recipes` rule of the benchmark: index only names and totals; a recipe not in the index is answered as not saved, never invented; "ficou boa" → `liked` fact naming the recipe (S37 category); "salva" is answered that the app saves from the bubble (the model never saves). ADR-033 record.
4. **Smoke (at most 12 model calls).** Tag `s38`, synthetic, eight cases: recipe request with structured output, recall by name, ambiguous name, missing recipe, log by recipe, log with change, adapt for today, "ficou boa". Run once: `--tag s38 --repeat 1`.
5. **Dev deploy** and one recall on the dev app once A68 sends the index.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md): `recipes[]`, `recipe_full`, `recipe`, `recipe_recall`, `recipe_id`.

## Out of scope

- Storage, search, screens and the bubble action: [D23](../../../design/plans/completed/d23-release-2-recipes.md), [A68](../../../android/plans/completed/a68-saved-recipes.md).

## Validation

1. `pytest server/tests -q` passes.
2. Smoke as in 4 (≤ 12 calls); numbers in Results.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`). ADR-052 accepted; the ADR-012 status line records the partial supersession (the recipe screens, delivered by A68).

- **Request (scope 1).** `recipes[]` (≤ 30; `R[0-9]{1,4}` unique ids, name ≤ 60, totals, ≤ 3 key foods) serialized as `RECIPES` after `RECENT_DAYS`; `recipe_full` (ingredients 1–30 with grams and kcal, ≤ 10 steps; its id must be in `recipes`) serialized as `RECIPE_FULL` only when sent.
- **Response (scope 2).** The action schema enumerates the request's recipe ids in `recipe_id` (null-only without recipes) and adds `recipe {name, ingredients[{name, g, kcal}], steps[]}`; `shaping.shape_recipe` bounds it (plan only). The server keeps a `recipe_id` only when it is in `recipes`, and a log of a saved recipe at confidence high takes the recipe's totals (`recipe_full`, else its index line) with no items, as a copied record (S28); recipe totals join the copied-record set of ADR-042. A change ("com o dobro de frango") stays a re-estimate (confidence medium). Neither field reaches the legacy view.
- **Rules (scope 3).** New RECIPES rule (meal-change branch): recall with the id; two matches ask which one; none says it is not saved and offers to build one, never invented; log by recipe copies, a change re-estimates, "for today" rebuilds under PROTEIN and WINDOW; a cooking plan fills `recipe`; a save request is answered that the app saves from the button; approval proposes `liked` (S37). Found while writing it: the ACTIONS rule of S37 still said "options are null"; it now says options follow the OPEN REQUEST plan and recipe fields follow RECIPES. ADR-033 record: general rules from ADR-052, no example.
- **Unit tests:** `pytest server/tests -q` → 610 passed, 546 subtests. New `test_s38_recipes.py` (10): RECIPES and RECIPE_FULL lines before DAY, validation (unknown full id, duplicates, bad id, more than 30), schema enum, copy from the full recipe and from the index line, a change is not copied, an unknown id is dropped, recall and its check, `shape_recipe` bounds, the rule only in the actions branch. New evaluator checks `recipe_id` and `recipe` (also per-action keys).
- **Smoke (8 of 12 model calls, effort low, US$ 0.0043).** `--tag s38 --repeat 1` (8 synthetic cases): 8/8, p50 3.5 s. A recipe request returned a plan with `recipe`; "lembra a receita da omelete de forno?" → `recipe_recall` R1; "escondidinho" → `recipe_recall` with null id asking frango or carne seca; an unsaved lasanha → not saved, an offer to build one; "jantei a omelete de forno" → log R1 at exactly 380 kcal; "com o dobro de frango" → log R2 re-estimated at 730 kcal; "ajustado pra hoje" → plan R2; "ficou boa demais" → a `liked` fact.
- **Dev deploy and HTTP smoke:** in the batch report.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- Once A68 sends the recipe index: on the dev app, "lembra a receita de …" opens the saved recipe, and "jantei a …" records it with the saved numbers.
