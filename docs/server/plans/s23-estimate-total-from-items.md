# Plan — S23 Estimate total computed by the server from the items

- Status: Em implementação
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: a pure arithmetic module, its call in the `/v1/chat` generation step (before the plan budget check and shaping) and in `/v1/estimate`, the meal-change energy validation, the reply copy adjustment, the dev conversation log record, one sentence of the `reference` rule, tests and evaluation cases. No schema, route signature or client change.
- Related documentation: [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [ADR-042](../adrs/ADR-042-estimate-total-is-server-arithmetic.md) (proposed by this plan), [ADR-041](../../produto/adrs/ADR-041-reference-portions-in-chat-instructions.md), [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md), [server README](../README.md).
- Prerequisites: [S22](completed/s22-open-requests-and-estimate-stability.md) delivered (it is).

Approving this plan accepts ADR-042. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s23-estimate-total-from-items.md. Implemente o plano aprovado.`

## Objective

The kcal of an estimate is the sum of its items, computed by the server, every time. The model keeps naming the foods, their grams and their per-item energy, which S22 made stable; it stops being trusted for the addition. The same breakfast then gets the same total, and the evaluator's item-sum checks stop depending on model arithmetic.

## Discovery evidence

From the S22 Results (06/10/2026, gpt-6-luna, `effort none`):

| Measure | Result |
|---|---|
| Repeated breakfast, 8 repetitions, items | ovo 146, pão francês 150, leite 120 kcal in every repetition; cooking fat 0–45 kcal as its own item |
| Same repetitions, model `kcal` total | 422–589 kcal (spread 19–34%), for example items summing 460 with a total of 589 |
| `s19-01-first-use-0` (`item_portions`: total must equal the item sum) | 0/3 in two reruns; items 151 + 47 = 198 with a total of 192 |
| Prompt sentence "compute kcal by adding the items row by row, never by re-estimating the whole meal" | no effect on either measure |
| Meal-change `add` (S18): server computes the consolidated total from the validated delta | 3/3 on every rerun; the only path where the total is server arithmetic today |

ADR-039 already moved the budget arithmetic to the server for the same reason: the model extracts well and adds poorly.

## Scope

### 1. Arithmetic (new module `server/estimate_total.py`)

- Input: the raw model estimate (`kcal, p, c, g, items`). Applies when `items` has at least one entry with a finite positive `kcal`; otherwise the estimate is untouched.
- Each item kcal is rounded once to a whole number (ties upward, as the meal-change contract does). `kcal_items` = their sum. Items with a nonfinite or negative kcal, or nonpositive grams, are dropped before the sum; if nothing remains the estimate is untouched.
- When the model `kcal` differs from `kcal_items`: `kcal` := `kcal_items`; `p`, `c`, `g` are scaled by `kcal_items / kcal_model` when `kcal_model` > 0 (else kept), rounded once to a whole number. Energy need not equal 4P + 4C + 9G (alcohol stays valid; spec rule 4a).
- Output: the adjusted estimate and a record `{model_kcal, items_kcal}` when a change happened, `null` otherwise.
- Pure function, no I/O, unit-tested with the examples above and with edge cases (zero model kcal, missing item kcal, a single item, 100 items, ties).

### 2. Where it runs

- `/v1/chat`: in `chat_reply.generate`, right after the model call and before `_plan_budget` and `shape`, on both the first call and the adjustment call. The budget check (ADR-039) and the record gate therefore see the server total. Scope `in_scope` only; never on a refusal.
- `/v1/estimate`: on the payload before `shape_estimate`.
- Meal changes (ADR-032): for operations `new` and `revise` the energy validation "rounded item kcal must sum to the estimate kcal" becomes the same recomputation instead of a failure; `add` keeps its delta rule (the delta's own items already must sum, and the server composes the total). Invalid items (nonnumeric, negative, zero grams, empty list) still fail as today.
- Compact, refusals and fallbacks: untouched.

### 3. Reply copy

- When the total changed and the reply states the model total next to `kcal` (`589 kcal`, `589,0 kcal`, `1.050 kcal`), the server replaces that figure with the server total in the same format. Macro figures in the reply are not rewritten; the app shows the card with the server numbers.
- When the reply states a total that is neither the model's nor the server's, it is left as is. No sentence is added.

### 4. Dev log and telemetry

- The dev conversation log record ([ADR-015](../adrs/ADR-015-log-conversa-dev.md)) gains `kcal_resum: {model_kcal, items_kcal}` or `null`. Numbers only. Telemetry unchanged.

### 5. Instructions

- The `reference` rule drops its last sentence ("compute kcal, p, c and g by adding the items row by row, never by re-estimating the whole meal"), which the server now owns, and keeps "an assumed cooking fat, sugar or milk is its own item". ADR-033 record in Results; no example added.

### 6. Evaluation

- Checks `item_portions` and `estimate_values` unchanged: they now verify the server invariant.
- Cases: `consistencia-cafe-repetido` keeps `kcal_spread: 10`; `s19-01-first-use-0` unchanged; new synthetic cases: a log whose model total is far from the item sum (fixed through a fake transport in a unit test, and as an eval case judged by `estimate_values`); a log with items without kcal (untouched); a plan over budget only before the recomputation (the budget check must follow the server total); a meal-change `revise` whose item sum differs from the stated total (recomputed, not failed).
- Unit tests with a fake model transport for every path in scope 2 and for the reply replacement.

### 7. Dev deploy

After validation: `tools/deploy-gcp.ps1` to the dev VM ([ADR-013](../adrs/ADR-013-gcp-host.md)), code only.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 4a (energy of an estimate with items is the server sum of the rounded item kcal; macros scaled), rule 5a (shaping step), rule 5e (`new`/`revise` energy recomputed, not failed), rule 5f (the budget check uses the server total), the dev log field in the observability section. Provenance line for S23.
- [HTTP contract](../../api-contract.md): `estimate.kcal` is the sum of `estimate.items[].kcal` whenever items are present, for `/v1/chat` and `/v1/estimate`; the meal-change capability paragraph on `new`/`revise` energy.
- On approval: ADR-042 status to Accepted; ADR-032 and ADR-041 status lines record the complement.

## Out of scope

- Per-item macros (the item shape stays `{name, g, kcal}`), a food database on the server, or any change to the reference table rows.
- Client changes: the app already displays the server numbers.
- Prompt changes other than the one sentence above.
- Production: blocked by the [production gate](../../content-policy/production-gate.md).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the new arithmetic, route and reply tests.
2. Pilot runner, `--provider luna --repeat 3`: `consistencia-cafe-repetido` passes `kcal_spread` at 10% in its 8 repetitions, with `record ask` in 8/8; `s19-01-first-use-0` 3/3; the S22 open questions stay 12/12.
3. Full suite, `--effort none --repeat 1`, compared with the S22 runs of 06/10 (205/220 and 204/220): no case that passed in both S22 runs fails; flakes recorded by name.
4. Dev smoke after deploy: one log whose reply total equals the card total; one plan over budget whose `over_kcal` matches the server total; one legacy-shape request unchanged.
5. `node tools/check-docs.mjs` passes.

## Results

Filled at Completion.
