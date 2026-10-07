# Plan — S28 A copied record never fails the turn on its items

- Status: Concluído
- Date: 07/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: the `new`/`revise` validation in `meal_changes.py`, one unit test module, one evaluation case, the dev conversation log untouched. No prompt, schema or client change.
- Related documentation: [server Chat specification](../specifications/v1-chat.md) rule 5e, [HTTP contract](../../api-contract.md#meal-change-capability), [ADR-042](../adrs/ADR-042-estimate-total-is-server-arithmetic.md), [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md).
- Prerequisites: none. Independent of [S24](completed/s24-protein-first-plan.md).

Owner approval on 07/10/2026 in the chat: "Execute um plano de servidor pequeno, pode escrever o plano, já aprovar, executar e entregar." Authorization and delivery follow [SDD](../../sdd/README.md).

## Objective

A log whose estimate copies a recorded meal (a `PARTICULAR DAY` copy from `RECENT`, or any estimate whose items carry no usable grams) is answered with its totals instead of the fallback `nao deu pra estimar`.

## Discovery evidence

Dev conversation log, 2026-10-07 05:34 (app 0.0.17-dev, server S23), one tester with one day of `RECENT`:

| Step | What happened |
|---|---|
| User: "Café o mesmo de ontem" | `RECENT` had yesterday's breakfast as one text row with totals (490 kcal). |
| Model | Followed the `PARTICULAR DAY` rule: `log`, copied totals unchanged, slot 1, confidence high, `meal_change {new, null, null}`; `items` = one entry with the whole record text, `g: 0`, kcal 490. |
| Server | `prepare_change` → `nutrition()` → `AdditionItemIn.g` requires `> 0` → `ValidationError` → fallback `error`, reply `nao deu pra estimar`. |

ADR-042 already states that items with nonpositive grams do not count toward the total; the `new`/`revise` validation still refuses them. The evaluation case `mesmo-cafe-ontem-recent` has been recorded as unstable since S21 for the same reason.

## Scope

1. `meal_changes.prepare_change`, operations `new` and `revise`: before the nutrient validation, items with a nonfinite or nonpositive `g`, or a nonfinite or negative `kcal`, are dropped from the estimate (they never counted, ADR-042). When at least one item remains, the validation is unchanged (rounded item kcal must sum to the total, which rule 4a already guarantees). When no item remains, the totals are validated alone (`meal_text`, `kcal`, `p`, `c`, `g`: finite, nonnegative, rounded once) and `items` is `[]`. `add` is unchanged: a delta still needs its items.
2. Unit tests: a `new` log with one `g: 0` item keeps its totals and an empty item list, no fallback; a `revise` with a `g: 0` item among valid ones drops it and keeps the sum; an `add` with a `g: 0` item still fails; a `new` with items that do not sum is still recomputed (regression).
3. Evaluation: new strict case `s28-cafe-de-ontem-um-dia` (tag `s28`): one `RECENT` day, "Café o mesmo de ontem", expects `log`, estimate present with the record's kcal, slot 1, no question, no refusal. `mesmo-cafe-ontem-recent` unchanged, run alongside.
4. Dev deploy and the smoke of the server README.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md) rule 5e: `new`/`revise` drop items without usable grams or energy before validation; an estimate with no usable item keeps its totals with an empty item list.
- [HTTP contract](../../api-contract.md): the same sentence in the meal-change capability paragraph on new/revise.
- Provenance line in v1-chat.

## Out of scope

- Prompt changes (the copy rule already produces the right totals); the item shape the model chooses for a copied record is accepted as is.
- S24 decisions, tone, formatting, planned slots. Production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the new tests.
2. `--tag s28 --repeat 3` passes 3/3; `mesmo-cafe-ontem-recent` recorded as it comes. Server code only: no full-suite run (server README policy).
3. Dev deploy; smoke: the same "Café o mesmo de ontem" turn with one `RECENT` day returns the copied totals; one quantified log and one legacy-shape request unchanged.
4. `node tools/check-docs.mjs` passes.

## Results

Owner approval on 07/10/2026 in the chat (the plan was written, approved and executed in one session). Branch `fix/s28-copied-record-items` from master `1a54b06`.

### Delivered

- `server/meal_changes.py`: `usable_items()` (finite positive grams, finite nonnegative kcal, dict shape), `totals_only()` (the whole-meal nutrition rounded once, `items: []`, through a `TotalsIn` model that accepts no items), and `prepare_change(..., keep)`: for `new`/`revise`, an estimate whose total equals a supplied RECENT row or eaten DAY slot is a copied record and keeps its numbers with an empty item list (the ADR-042 keep rule, which `estimate_total.apply_turn` already applied before the meal-change validation recomputed the total from invented items); otherwise the total is recomputed from the usable items (ADR-042), the unusable ones are dropped, and with none left the totals stand alone. `add` unchanged.
- `server/main.py`: the meal-change validation receives `_record_totals(body)`.
- Tests: `server/tests/test_s28_copied_items.py` (copied record without grams; copied RECENT record with invented item grams keeps 430 kcal; only unusable items dropped; revise; add still fails; totals still validated; the filter; rounding).
- Evaluation case `s28-cafe-de-ontem-um-dia` (tag `s28`, non-strict): one RECENT day, "Café o mesmo de ontem", meal-changes branch. It documents the gap; see below.
- **Deviation recorded:** the plan said no prompt change. A prompt change was tried and dropped. The `PARTICULAR DAY` rule was rewritten (date resolved first, a single RECENT day is enough, no 0 g items) and moved to the top of the `history` rule: the `s28` case went from 0/6 to 6/6 and `mesmo-cafe-ontem-recent` from 2/6 to 6/6, but `cafe-resposta-leite` fell from 3/3 to 2/6 and `ceia-completa-suco` from 3/3 to 1/6 (both legacy-branch fixtures), in every variant tried (sentence removed, habitual header neutralised, original habitual phrasing restored). The instructions shipped are the master's, unchanged. The lookup of "o mesmo de ontem" stays a prompt defect for the next instruction plan; with the server fix, a copy that the model does find no longer fails.

### Evaluation (effort none; OpenAI cost of this plan US$ 0.13)

| Run | Scope | Result |
|---|---|---|
| master prompt, `--tag history --repeat 3` | `mesmo-cafe-ontem-recent` 0/3, `s28` 0/3 | the model answers that it has no record of yesterday |
| master prompt, `--only cafe-resposta-leite,ceia-completa-suco --repeat 3` | 3/3 and 3/3 | baseline for the two cases below |
| S28 prompt variants, `--repeat 6` (four cases) | `s28` 6/6, `mesmo-cafe` 6/6, `cafe-resposta-leite` 2/6, `ceia-completa-suco` 1/6 | dropped |
| S28 prompt, full suite `--repeat 1` | 213/224 (S23 run of 06/10: 215/223); failures reran at `--repeat 3`: only the two above failed 3/3 | superseded by the revert |
| master prompt + server fix (shipped) | `pytest`: 408 passed, 415 subtests | server code only: no full-suite run (server README policy) |

Prefix sizes unchanged: legacy 33,404, meal changes 37,165, compact 3,382.

### Validation

1. `pytest -q` in `server/`: 408 passed, 415 subtests passed.
2. Evaluation as above; `s28-cafe-de-ontem-um-dia` is expected to fail on the shipped prompt (0/6) and is non-strict and documented.
3. Dev deploy from this branch with `tools/deploy-gcp.ps1`, code only; `GET /health` 200. Smoke (request ids `s28-smoke-1791364740-0`, `-1`, `-2`): "Café o mesmo de ontem" with one RECENT day returned the copied 430 kcal with `items: []` and `record auto` (the model found the row on that run); a quantified lunch returned 443 kcal equal to its item sum; a legacy-shape request returned 146 kcal with its usual keys.
4. `node tools/check-docs.mjs`: passed.
