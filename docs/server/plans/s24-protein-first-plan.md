# Plan — S24 Protein-first plan inside the meal window

- Status: Em implementação
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: DAY serialization, a pure arithmetic module for the meal window, the `plan` and `memory_changes` rules of `chat_instructions.py`, the plan budget check, the dev conversation log record, tests and evaluation cases. No route signature or schema change for the client; no client change.
- Related documentation: [ADR-043](../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md) (proposed by this plan), [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-042](../adrs/ADR-042-estimate-total-is-server-arithmetic.md), [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [product Chat](../../produto/specifications/chat.md) rule 16.
- Prerequisites: S23 delivered ([history](completed/)).

Approving this plan accepts ADR-043. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s24-protein-first-plan.md. Implemente o plano aprovado.`

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

Approved by the owner on 06/10/2026 (overnight run, message naming this file). **Stopped in implementation** under the owner's overnight rule (a validation that does not pass after two attempts stays `Em implementação`; S25, S26 and S27 depend on S24 and were not started). Code on branch `feat/s24-protein-first-plan` (draft PR, not merged, not deployed). The dev server still runs S23.

### Delivered on the branch

- `server/meal_window.py`: expected kcal per slot (mean per distinct RECENT day with at least two days, else `ceiling / slots`, rounded once), `window()` with computed reservations of the other empty slots, stated reservations replacing a slot by name (case- and accent-insensitive) or added, `window_kcal = max(0, remaining − reserved_upcoming)` (AGENTS `windowBudget`); `windows()` scaled to the remainder with protein in the same shares (largest remainder, exact sums); `budget_line()` and `windows_line()`; `close_reply()`.
- `main.py`: DAY gains `remaining_p/c/g` after `remaining_kcal` (omitted without it); per-request `WINDOWS:` (two or more empty slots) and `BUDGET: protein_floor=…; window_kcal by meal: …` lines before `CURRENT_USER_MESSAGE`; `plan_budget.limit_kcal` = `fit_kcal`, else the window of the plan's slot, `reserved` = computed + stated; dev log `meal_window`; closing lines of a log or plan of today rewritten with the server's numbers after the model's answer.
- `chat_instructions.py`: `plan` (two options for an open request; WINDOW and PROTEIN; NAMED DISH and PROTEIN BOOST with `(opcional)`; NO SCALE; `reserved` never copies BUDGET/WINDOWS numbers), new rule `closing`, log rules point to CLOSING, `memory_changes` brand/type sentence. ADR-033 record: general rules from ADR-043 decisions 2–7, owner spec ADR-043; no example added (a symbolic `protein-boost-table-v1` example was tried in run 5 and removed: it did not change the outcome).
- Evaluator: checks `estimate_min`, `reply_options`, `closing_lines`, `reply_any` with unit tests; seven `s24` cases (synthetic situations independent of the tester log).
- Tests: `server/tests/test_meal_window.py` and `test_evals_s24.py` (27 tests); `pytest server/tests`: 425 passed.

### Decisions not covered by the plan (agent, most conservative reading)

- The plan's single `BUDGET: window_kcal=…` line needs the plan's slot, which the server only knows after the answer: the line lists the window of each empty meal and of "any other meal"; the post-answer check uses the real `suggested_slot`.
- Closing-line numbers: the model writes the lines from `WINDOWS`; the server rewrites the numbers after the answer (the other empty slots share what is left after this meal, by expected kcal), so the lines always sum to the day's remainder (ADR-043 decision 6). `WINDOWS` adds a per-slot `P`, needed by the line format.
- No target slot (a plan without `suggested_slot`): no computed reservation, ADR-039 behavior.
- `protein_floor` (30% of `remaining_p`, rounded up) is served in the `BUDGET` line so the model does not compute it.

### Evaluation (effort none; OpenAI cost of this plan US$ 0.117)

| Run | Scope | Result | Cost |
|---|---|---|---|
| 1 | `--tag s24 --repeat 3` | 5/7; closing lines 1/3, named dish with room 0/3 | US$ 0.0067 |
| 2 | same, after the open-request and `(opcional)` wording | 4/7; reservations echoed from BUDGET (code bug in the rule, fixed in 3), closing lines lost to the one-line assumption rule | US$ 0.0067 |
| 3 | same, `reserved` rule, trailing punctuation, case fix | 5/7; named dish 0/3, lunch closing 1/3 | US$ 0.0065 |
| 4 | same, CLOSING in the assumption lines, `protein_floor` | 5/7; closing 3/3, reservations 3/3; named dish 0/3; the no-room fixture was wrong (it had room) | US$ 0.0067 |
| 5 | same, symbolic example (then removed) | 5/7; named dish 0/3; lunch closing back to 1/3 | US$ 0.0066 |
| 6 | full suite `--repeat 1` (prompt of run 4) | 201/230 (S23 run of 06/10: 215/223) | US$ 0.0841 |

Full-run failures by cause:

- ADR-043 window against the ADR-039 fixtures (11): `receita-acima-ajusta`, `receita-acima-com-reserva`, `receita-acima-ja-caber`, `receita-acima-ja-liberado`, `receita-acima-pergunta`, `receita-acima-pode-passar`, `receita-despensa-extras`, `receita-reserva-lanche`, `receita-reserva-sem-kcal`, `receita-sem-margem` (`limit_kcal` now the window, often 0 in evening fixtures where the equal-share reservation of the empty slots exceeds the remainder) and `receita-almoco-despensa` (kcal). They need either rewritten fixtures or a different default reservation: an owner decision.
- `s24` (2): `s24-prato-nomeado-com-espaco` (the model keeps a named low-protein dish and never adds the `(opcional)` foods: 0/3 in every run), `s24-almoco-fecha-o-dia` (closing lines unstable).
- Provider JSON errors (2): `s18-interleaved-correction`, `unavailable-weight-and-size`.
- Known unstable or recurring (14): `cafe-resposta-leite`, `criativo-cafe-diferente-do-de-sempre`, `de-sempre-quantidade-diferente`, `de-sempre-um-dia`, `dia-pedi-estimar-ontem`, `duvida-whey`, `memoria-cheia`, `pergunta-todas-de-uma-vez`, `registro-lanche-dois-pontos`, `registro-noturno-ontem`, `s19-04-recent-brands-0`, `s19-11-add-continuation-1`, `slot-cafe-20h`, `slot-cafe-repete-almoco`. Recorded by name, no tiebreak runs.

### What is missing to close

1. Owner decision on the default reservation (equal share of the ceiling per empty slot zeroes many evening windows) and on the eleven ADR-039 fixtures.
2. ADR-043 decision 4 (named dish + `(opcional)` protein foods) is not followed by the model with prompt rules alone; options: accept the limitation, a server-side suggestion, or a different approach decided by the owner.
3. Then: `s24` 3/3, one full run, deploy and smoke, Completion of the specs (v1-chat rules 3 and 5, HTTP contract, product Chat rule 16), ADR-043 status Accepted and ADR-039 partial supersession.
