# Plan — S24 Protein-first plan inside the meal window

- Status: Concluído
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: DAY serialization, a pure arithmetic module for the meal window, the `plan` and `memory_changes` rules of `chat_instructions.py`, the plan budget check, the dev conversation log record, tests and evaluation cases. No route signature or schema change for the client; no client change.
- Related documentation: [ADR-043](../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md) (accepted on 2026-10-06 with the approval of this plan), [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-042](../adrs/ADR-042-estimate-total-is-server-arithmetic.md), [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [product Chat](../../produto/specifications/chat.md) rule 16.
- Prerequisites: S23 delivered ([history](completed/)).

Approving this plan accepts ADR-043. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/completed/s24-protein-first-plan.md. Implemente o plano aprovado.`

## Objective

A plan for today is built to close the protein gap inside the window of that meal, not merely to fit the day's remaining kcal. The server computes the remaining macros and the window; the model builds the dish to them.

## Discovery evidence

Dev conversation log of 2026-10-06 (ADR-015; request ids kept out of this file):

| Turn | Input the model had | Model answer |
|---|---|---|
| "Não sei o que comer na janta" | `remaining_kcal=824`, `p_target=165`, `eaten_p=85` (80 g missing), supper slot empty | one dish, 443 kcal, 59 g P |
| "Eu tenho pão de trigo sarraceno, rola comer com ovo olhão e uma colher de chá de maionese Hemmer?" | same numbers | the named dish as stated: 280 kcal, 13 g P |
| "Preciso de bem mais proteína do que isso..." | same | a question instead of a dish |
| "Quero manter com o ovo, você poderia adicionar mais ovos" | same | the same 280 kcal dish and a question |

The `plan` rule names `remaining_kcal` as the only budget and forbids changing a dish the user named. No rule names `p_target`, `eaten_p` or a protein objective. Supper (slot 6) was empty and never reserved: the dinner window was the whole 824 kcal.

Memory, same log, both testers: `MEMORY: permanent 0/30, dynamic 0/40` on every turn and `memory_updates: []` in all 30 turns of the day, although the logs named brands and types (a yogurt brand, a mayonnaise brand, a bread type) that the `memory_changes` rule says become dynamic facts. The empty memory itself is expected: 0.0.13 of 05/10 was a new app without data migration. The missing proposals are not.

## Scope

### 1. Remaining macros (DAY)

- `DAY` gains `remaining_p`, `remaining_c`, `remaining_g` = target − eaten, integers, may be negative, computed by the server from `profile` and `day`. Serialized right after `remaining_kcal`. Omitted when the request has no `remaining_kcal` (legacy shape).

### 2. Meal window (new module `server/meal_window.py`)

- Input: the day's slots with status, `RECENT` rows, the ceiling, `remaining_kcal`, the model's `plan_budget.reserved` and the plan's `suggested_slot`.
- Expected kcal of a slot: mean of its `RECENT` kcal over distinct days when it has records on at least 2 of the 7 days; otherwise `ceiling_kcal / number of slots of the day`, rounded once.
- `reserved_upcoming` = sum of the expected kcal of every other slot of today whose status is `empty`, excluding the plan's target slot. A model `reserved` entry whose label matches a slot name replaces that slot's expected kcal; an entry that matches no slot is added as today.
- `window_kcal = remaining_kcal − reserved_upcoming`. Pure function, no I/O, unit-tested (no RECENT; one day of RECENT; two days; a skipped slot; the target slot excluded; model reservations replacing and adding).
- Windows of every remaining slot (ADR-043 decision 6): the same expected kcal per empty slot, scaled so that they sum to `remaining_kcal` (floor 0 each), and the protein still missing. Serialized as `WINDOWS: {slot name} ~{kcal} kcal · ... | faltam {remaining_p} g P` only on a log or plan turn of today with at least one other empty slot.

### 3. Prompt

- The input of a plan-capable turn ends with `BUDGET: window_kcal={n}, reserved_upcoming={m} ({slot names})` before `CURRENT_USER_MESSAGE`. The line is per request, not in the cached prefix.
- `plan` rule: the objective of ADR-043 decision 2 (cover the remaining protein inside the window, then fat and carbohydrate; one clause in the reply about what the dish does for the day's protein, never day totals); decision 4 (a named dish keeps its foods; below 30% of the remaining protein and with room in the window, up to two `(opcional)` protein foods, counted in items and totals); decision 5 (an open request gets two options, each with grams and totals, both inside the window; `estimate` carries the first option, the second is in the reply with its own total). `BUDGET_TARGET` keeps its meaning.
- `log` and `plan` rules, closing line (ADR-043 decision 6): after a log or plan of today with a `WINDOWS` line, the reply ends with one short line per remaining slot, `{slot}: {food} ~{kcal} kcal · P {p}`, the food taken from a routine fact or a RECENT record of that slot when one exists; the numbers come from the `WINDOWS` line, never recomputed. No totals of the day. Without `WINDOWS`, no closing line.
- `plan` rule, no scale (ADR-043 decision 7): when the user says the meal cannot be weighed, each food is stated in household measures with the approximate grams beside them; items still carry grams.
- `memory_changes` rule: one sentence making the brand or product type → dynamic fact proposal explicit for a log whose text names a brand or a product type, with the base food as key. ADR-033 record in Results; no example added.

### 4. Budget check

- `plan_budget.limit_kcal` = `fit_kcal` when sent, otherwise `window_kcal` (ADR-043 decision 3). `reserved` in the response lists the computed reservations as `{label, kcal}` (slot name, expected kcal) together with the model's, so the app's `Reservei {kcal} kcal para {label}.` lines (A50) stay true.

### 5. Dev log

- The record gains `meal_window: {window_kcal, reserved_upcoming, reserved: [...]}` or null. Numbers and slot names only.

### 6. Evaluation

- New cases, tag `s24`, synthetic situations rewritten from the discovery table: an open dinner request with an 80 g protein gap (expect two options, `estimate.p` at least 40, kcal inside the window); a named low-protein dish with room in the window (expect the named foods kept, up to two `(opcional)` items, `estimate.p` above the named dish alone); the same with no room (expect no addition); a plan whose day has an empty later slot (expect `plan_budget.limit_kcal` below `remaining_kcal` by that slot's reservation); a log that names a brand (expect `memory_updates` with one `add dynamic`); a lunch log on a day with three empty slots left (expect one closing line per slot, kcal from `WINDOWS`, no day total); a restaurant plan "sem balança" (expect household measures and grams per item).
- Regression: `--tag s22 --repeat 3`, `--tag s23 --repeat 3`, `--tag recipe --repeat 3`, then the full suite at `--repeat 1` against the S23 run of 06/10 (215/223); flakes recorded by name.

### 7. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only, then the three-turn smoke of the server README.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 3 (DAY remaining macros, `BUDGET` line), rule 5 (plan objective, two options, `(opcional)` protein foods, window as `limit_kcal`, computed reservations), observability (`meal_window`). Provenance line.
- [HTTP contract](../../api-contract.md): `plan_budget.limit_kcal` and `reserved` semantics; `remaining_p/c/g` are server-computed and not sent by the client.
- [product Chat](../../produto/specifications/chat.md) rule 16: a plan is built to the meal window and the protein gap (one sentence linking ADR-043).
- ADR-043 status to Accepted; ADR-039 status line records the partial supersession.

## Out of scope

- Client changes: the projected-day panel and the A50 choice already display the server numbers.
- Tone, closures, reminders: [S25](s25-tone-and-closures.md), [A55](../../android/plans/a55-tone-choice-and-closures.md).
- The auto-record defect of the same log: [A54](../../android/plans/completed/a54-auto-record-addition-empty-slot.md).
- Per-meal caps typed by the user; a food database; production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including `test_meal_window.py` and the DAY serialization tests.
2. Evaluation as in scope 6; the `s24` cases 3/3 each; no case that passed in the S23 full run fails twice.
3. Dev deploy and smoke: one open dinner request on a day with an empty supper returns two options and `plan_budget.limit_kcal` below `remaining_kcal`; one named low-protein dish returns `(opcional)` protein items; one legacy-shape request unchanged.
4. `node tools/check-docs.mjs` passes.

## Results

Approved by the owner on 06/10/2026 by name (overnight run). The overnight agent stopped the plan on 07/10 at 05:00 with the code on `feat/s24-protein-first-plan` (two open decisions, below). The owner decided both in the chat of 07/10 and the plan was finished the same morning in a worktree on that branch, rebased on master after S28.

### Owner decisions of 07/10/2026 (recorded here; ADR-043 decisions 3 and 4 as delivered)

1. **Default reservation.** An empty meal is reserved at its expected kcal (RECENT mean with at least two days, else ceiling ÷ meals of the day) **capped at its proportional share of the remainder** (remaining × expected ÷ Σ expected of the plan's meal and the other upcoming empty meals), so the window never reaches zero while something remains. An empty meal whose time passed more than 90 minutes ago is over and reserves nothing (the lanche of 15:00 at 20:00 was skipped in practice); a meal in that grace is still the meal in progress. The plan's meal is the model's `suggested_slot`, or the nearest upcoming empty meal when it is missing or over.
2. **Protein boost on the server.** The model did not follow the `(opcional)` rule in five rounds; the owner asked for the common diet proteins of the TACO table to be suggested by the server. `server/protein_boost.py`: when a plan of today is below 30% of `remaining_p` and the window leaves at least 60 kcal, up to two of frango desfiado (159/32 per 100 g), ovo cozido (146/13, 50 g steps) and patinho moído cozido (219/36) are appended, a food the dish already has skipped, sized in steps to the room and the gap; items, totals, `meal_text` and the reply (`Para a proteína (opcional): …`) carry them; the budget check sees the boosted dish. The prompt sentence asking the model to add them was removed.

### Delivered (beyond the overnight branch)

- `meal_window.py`: `Slot.time`, `upcoming()` with the 90-minute grace, `_capped()` shares, `limit_kcal` kept negative for the ADR-039 check while `window_kcal` is floored for the model, `served_numbers()` (a stated reservation that echoes a figure the server wrote is ignored), `typed_numbers()` (a stated reservation that names a meal counts only when its number appears in the user's texts and it is larger than the computed one; a meal name that is not an upcoming other meal is dropped), `nearest_upcoming()`, `_slot_key()` (`Para a ceia` names `Ceia`), and `close_reply()` that also finds closing lines written inline, drops the answered meal's, and with `complete` (a `WINDOWS` line was served) adds the line of a meal the model left out with that meal's latest RECENT food or `a definir`.
- `main.py`: `_now`, `_typed`, `_usual_foods`, `_target` fallback, `_protein_boost` after the ADR-042 arithmetic and before the budget check, dev log `protein_boost`.
- `plan_budget.check`: the limit is `window.limit_kcal`.
- `chat_instructions.py` (`plan` rule): PROTEIN BOOST sentence removed (server-owned); `reserved` lists foods the user named, never a meal of the BUDGET/WINDOWS lines; a window of 0 or a dish larger than its window is never a reason to refuse or ask (only `BUDGET_TARGET` rebuilds). ADR-033 record: general rules from ADR-043, owner spec ADR-043; no example added.
- Tests: `test_protein_boost.py` (9), `test_meal_window.py` (cap, grace, typed numbers, echo, meal-name extras, nearest slot, closing fallback), `test_plan_budget.py` negative-limit expectation restored. `pytest`: 450 passed, 422 subtests.
- Fixtures: ADR-039 `receita-*` expectations refreshed to the window (notes in each case: `limit_kcal` 225/108/133/400/500/600, `reserved` with the computed supper); `receita-acima-ja-caber` and `receita-acima-ajusta` get a remainder a recipe can be rebuilt into (900 and 700); `receita-almoco-despensa` kcal floor 300 (five meals pending at lunch make an equal-share window of about 350); `receita-reserva-sem-kcal` names the slot instead of the yogurt label; `s24-prato-nomeado-sem-espaco` remainder 170.

### Evaluation (effort none; OpenAI cost of the morning US$ 0.17; no full-suite run by owner decision of 07/10, 10:55)

| Run | Result |
|---|---|
| `--tag s24 --repeat 3`, final code | **7/7** (open dinner with two options and P ≥ 40; named dish with `(opcional)` chicken; no room → no boost; reservations; closing lines; no scale; brand → dynamic memory) |
| `--tag recipe --repeat 3`, final code | **15/16**: `receita-acima-ajusta` 1/3 (the rebuilt dish lands 445 against 400, model arithmetic on the adjustment; flaky since S21) |
| Earlier rounds of the morning (`s24` 4/7 → 5/7 → 7/7; `recipe` 6/16 → 12/16 → 15/16) | each step is one of the server rules above; recorded in the session, not repeated here |

Known gap: without history every meal expects an equal share of the ceiling, so a lunch planned with four meals still to come gets a small window (about 350 of 1750). A weighting by meal name or time is a follow-up, not in ADR-043.

### Validation

1. `pytest -q` in `server/`: 450 passed, 422 subtests passed.
2. Evaluation as above; the full suite was not run (owner decision, cost).
3. Dev deploy with `tools/deploy-gcp.ps1` from the merged master (PR #153), then from the two smoke follow-ups: PR #166 and #167 (a closing the model writes without a food, `Lanche: ~435 kcal · P 33`, is rebuilt once instead of duplicated) and PR #168 (a closing must end its line: the answered meal's total followed by more macros is prose). Smoke on the dev URL, request ids `s24-smoke-1791381660-0..2`, `s24-smoke-1791381921-3`, `s24-smoke-1791382051-4..5`: an open dinner with 980 kcal left and a supper reserved at 150 returned two options and `limit_kcal` 830; the toast-and-jam dinner came back with `Para a proteína (opcional): 150 g de frango desfiado, +239 kcal · P 48 g` and 53 g of protein; a lunch log returned one closing line per remaining meal with the server's numbers (`a definir` where the fixture has no history).
4. `node tools/check-docs.mjs`: passed.
