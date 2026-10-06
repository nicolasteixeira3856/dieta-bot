# Plan — S23 Estimate total computed by the server from the items

- Status: Concluído
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: a pure arithmetic module, its call in the `/v1/chat` generation step (before the plan budget check and shaping) and in `/v1/estimate`, the meal-change energy validation, the reply copy adjustment, the dev conversation log record, one sentence of the `reference` rule, tests and evaluation cases. No schema, route signature or client change.
- Related documentation: [server Chat specification](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md), [ADR-042](../../adrs/ADR-042-estimate-total-is-server-arithmetic.md) (proposed by this plan), [ADR-041](../../../produto/adrs/ADR-041-reference-portions-in-chat-instructions.md), [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md), [server README](../../README.md).
- Prerequisites: [S22](s22-open-requests-and-estimate-stability.md) delivered (it is).

Approving this plan accepts ADR-042. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s23-estimate-total-from-items.md. Implemente o plano aprovado.`

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

- The dev conversation log record ([ADR-015](../../adrs/ADR-015-log-conversa-dev.md)) gains `kcal_resum: {model_kcal, items_kcal}` or `null`. Numbers only. Telemetry unchanged.

### 5. Instructions

- The `reference` rule drops its last sentence ("compute kcal, p, c and g by adding the items row by row, never by re-estimating the whole meal"), which the server now owns, and keeps "an assumed cooking fat, sugar or milk is its own item". ADR-033 record in Results; no example added.

### 6. Evaluation

- Checks `item_portions` and `estimate_values` unchanged: they now verify the server invariant.
- Cases: `consistencia-cafe-repetido` keeps `kcal_spread: 10`; `s19-01-first-use-0` unchanged; new synthetic cases: a log whose model total is far from the item sum (fixed through a fake transport in a unit test, and as an eval case judged by `estimate_values`); a log with items without kcal (untouched); a plan over budget only before the recomputation (the budget check must follow the server total); a meal-change `revise` whose item sum differs from the stated total (recomputed, not failed).
- Unit tests with a fake model transport for every path in scope 2 and for the reply replacement.

### 7. Dev deploy

After validation: `tools/deploy-gcp.ps1` to the dev VM ([ADR-013](../../adrs/ADR-013-gcp-host.md)), code only.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): rule 4a (energy of an estimate with items is the server sum of the rounded item kcal; macros scaled), rule 5a (shaping step), rule 5e (`new`/`revise` energy recomputed, not failed), rule 5f (the budget check uses the server total), the dev log field in the observability section. Provenance line for S23.
- [HTTP contract](../../../api-contract.md): `estimate.kcal` is the sum of `estimate.items[].kcal` whenever items are present, for `/v1/chat` and `/v1/estimate`; the meal-change capability paragraph on `new`/`revise` energy.
- On approval: ADR-042 status to Accepted; ADR-032 and ADR-041 status lines record the complement.

## Out of scope

- Per-item macros (the item shape stays `{name, g, kcal}`), a food database on the server, or any change to the reference table rows.
- Client changes: the app already displays the server numbers.
- Prompt changes other than the one sentence above.
- Production: blocked by the [production gate](../../../content-policy/production-gate.md).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the new arithmetic, route and reply tests.
2. Pilot runner, `--provider luna --repeat 3`: `consistencia-cafe-repetido` passes `kcal_spread` at 10% in its 8 repetitions, with `record ask` in 8/8; `s19-01-first-use-0` 3/3; the S22 open questions stay 12/12.
3. Full suite, `--effort none --repeat 1`, compared with the S22 runs of 06/10 (205/220 and 204/220): no case that passed in both S22 runs fails; flakes recorded by name.
4. Dev smoke after deploy: one log whose reply total equals the card total; one plan over budget whose `over_kcal` matches the server total; one legacy-shape request unchanged.
5. `node tools/check-docs.mjs` passes.

## Results

Owner approval on 2026-10-06 by name. Work ran in the `luna-grok-pilot-comparison-9a8eb3` worktree on branch `feat/s23-estimate-total`, from master `a83ca47`.

### Delivered

- `server/estimate_total.py`: `items_total`, `apply` (sum of the item kcal rounded once, ties upward; macros scaled by the ratio to the model total and rounded once; item kcal rounded in place; items with nonfinite or negative kcal or nonpositive grams do not count; untouched without usable item energy or when the total already matches), `apply_turn` (also rewrites the quoted total in the reply) and `rewrite_reply` (`N kcal`, `N,0 kcal`, `1.050 kcal`; other figures untouched).
- **Deviation recorded:** `apply_turn` takes the kcal of the supplied RECENT rows and eaten DAY slots and leaves a model total equal to one of them unchanged. Found in the first full run: `igual-almoco-segunda` copies the recorded 620 kcal (rule 3d) while the model invents items that do not sum to it (a 0 g salad with 113 kcal); summing the items would have broken the copy rule. Spec rule 4a states it.
- `server/main.py`: the arithmetic runs in `chat_reply.generate` right after each model call (first and adjustment), before `_plan_budget` and shaping; `/v1/estimate` applies it before `shape_estimate`; the dev log record carries `kcal_resum` (`{model_kcal, items_kcal}` or null).
- `server/meal_changes.py`: `new`/`revise` recompute the total from the items before the energy validation instead of failing on a mismatch; `add` unchanged.
- `server/chat_instructions.py`: the `reference` rule drops the row-by-row addition sentence (the server owns it) and names the common value for an unstated cooking fat in fried, scrambled or sautéed food (5 g of óleo). ADR-033 record: rules addressed are Chat 4a/5a/5e/5f under ADR-042; no example added; both Chat prefixes reviewed in full.
- Tests: `server/tests/test_estimate_total.py` (arithmetic, edge cases, reply rewrite, kept copies, chat route, estimate without items, plan budget on the server total, meal-change revise recomputed). Shared fixtures of `test_api`, `test_chat`, `test_clarify`, `test_record` and `test_evals` made item-consistent; the `test_meal_changes` subcase that expected a revise energy mismatch to fail was removed (now recomputed, covered by the new test).
- Evaluation: three `s23` cases (`s23-soma-itens-almoco` with exact `estimate_values` on reference foods, `s23-orcamento-pelo-total-do-servidor`, `s23-mensagem-sem-itens-intacta`).

Prefix sizes: legacy 33,404 characters (`5a1e965a`), meal changes 37,165 (`626cfecc`), compact 3,382 unchanged.

### Validation

1. `pytest -q` in `server/`: 398 passed, 414 subtests passed.
2. Targeted evaluation, `--effort none --repeat 3`: `consistencia-cafe-repetido` 8 repetitions at 442–460 kcal (spread 4%, S22: 19–34%), `record ask` 7/8 (one `auto`), items identical in every repetition; `s19-01-first-use-0` 3/3 and `-1` 3/3 (0/3 in S22); the three `s23` cases 3/3 each; `s22-almoco-quantificado-registra` 3/3; `criativo-rodizio-pizza` 3/3. Pilot runner `--set creative rule --repeat 3`: 14/14 (the twelve open questions and the two first-message rules). After the copied-record rule: `igual-almoco-segunda`, `de-sempre-recent-igual`, `hard-correcao-bife`, `s18-recorded-second-portion` 3/3 each; `mesmo-cafe-ontem-recent` 1/3 (unstable on every prefix since S21).
3. Full suite, `--effort none --repeat 1`: 215/223 (S22 runs the same day: 205/220 and 204/220). Failures: `igual-almoco-segunda` (fixed by the copied-record rule above, 3/3 after it), `s18-dinner-digest-initial` and `s18-unusual-time` (provider 429), `receita-reserva-lanche` (reserved meal named in meal_text, unrelated), `s19-13-compact-open-0` (digest marker, unrelated), `mesmo-cafe-ontem-recent`, `receita-acima-ja-caber`, `slot-cafe-repete-almoco` (flaky in the S22 runs too). p50 2.7 s, p95 3.9 s; about 8,070 input tokens per call (6,790 cached), 188 output; US$ 0.07.
4. Dev deploy and smoke: `tools/deploy-gcp.ps1` from master `7032f0c` after the merge of [PR #147](https://github.com/nicolasteixeira3856/dieta-bot/pull/147), code only; `GET /health` 200. Three turns through the dev URL (request ids `s23-smoke-1791311775-0`, `-1`, `-2`): a quantified lunch returned kcal 703 equal to its item sum, the same figure quoted in the reply, `record auto`; an opted-in plan with 540 kcal left returned a 302 kcal dish equal to its item sum with `plan_budget {limit_kcal: 540, over_kcal: 0}`; the legacy-shape request returned the usual keys, kcal equal to its item sum, no `record` or `plan_budget`.
5. `node tools/check-docs.mjs`: passed.
