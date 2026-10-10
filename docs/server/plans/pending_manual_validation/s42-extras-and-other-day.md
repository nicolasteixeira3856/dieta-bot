# Plan — S42 Extras outside the meals and a record in a named past day

- Status: Pendente aprovação manual (approved 09/10/2026: "Aprovo o plano docs/server/plans/s42-extras-and-other-day.md. Implemente o plano aprovado."; code, unit tests, evaluation, docs and the dev deploy done; an extra and a past-day record on the device with A72 are the owner's manual acceptance)
- Date: 09/10/2026
- Owning context: `server`
- Affected code: `server/` only: the `log` action target `slot: "extra"`, the day resolver for `meal_day: other` with an ISO `day`, the 30-day bound, the prompt rules and examples under [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), `recent` and `recent_days` accepting extras, tests, eval cases. Documentation at Completion: [HTTP contract](../../../api-contract.md), [v1-chat](../../specifications/v1-chat.md).
- Prerequisites: [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) accepted by this approval; [S40](../completed/s40-eval-personas.md) delivered.
- Related documentation: [chat](../../../produto/specifications/chat.md), [ADR-050](../../../produto/adrs/ADR-050-typed-actions-per-message.md) (typed actions), S14 (`meal_day`, [history](../completed/)).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s42-extras-and-other-day.md. Implemente o plano aprovado.`

## Objective

A message about something eaten outside the meals is recorded as an extra, and a message that names a day in the last 30 days is recorded in that day, with the day resolved by the server, never guessed ([ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) decisions 1 and 5).

## Scope

1. **Extra target.** A `log` action may carry `slot: "extra"` (new capability flag `extras: true` from the client; without it the server behaves as today). The instructions name the cues (`fora da refeição`, `entre {refeição} e {refeição}`, `um extra`, `só um {bebida ou lanche} agora`, a drink or snack named at a time that is not a meal time and that the user does not tie to a meal); the server validates that an `extra` action has an estimate and no `suggested_slot`, and that a `plan` never targets an extra. `time` on the action: the time the user states (`HH:mm`), else `null` (the app uses now).
2. **Day resolver.** Deterministic, in code, before the model: cues `ontem`, `anteontem`, a weekday name (the most recent past one), `dia {n}` (the most recent past day with that number), `{n} dias atrás`; combined with `local_time` they produce an ISO `day`. The resolved `day` goes to the model as data (`DAY_REF: 2026-10-08 (ontem, quarta)`); the model's `meal_day: other` must carry that `day` or the server rewrites it. A cue the resolver cannot bind (two candidates, a future day) is a question, never a record. A `day` older than 30 days or before `first_day` (new optional request field; absent = no bound) returns the fixed line `Só registro os últimos 30 dias.` with no action. The slot of a past day comes from `profile.slots_by_day` when the client sends it (optional; absent = today's slots).
3. **`recent` and `recent_days`.** `recent` items accept `slot_id: "extra"` with a `time`; `recent_days` totals already include them. The `RECENT` serialisation writes `Extra 15:40 · {text} · {kcal}`.
4. **Receipt copy.** The server's `reply` for an extra or an other-day record uses the existing arithmetic lines; the receipt title is the app's (A72).
5. **Tests.** Unit: resolver table (every cue, month boundary, weekday today → last week, two candidates → question), 30-day bound, extra validation, plan never extra. Eval cases, one each: energy drink between lunch and snack; coffee after supper with a stated time; "ontem jantei pizza" in the morning; "na segunda almocei X" on a Thursday; a day 40 days back; an ambiguous "dia 3" across a month.

## Out of scope

- The app (A72). The Home strip and the past-day screen (D28, A72).
- Recording in a future day. Editing the closure text of a past day.
- Reservations or routines on a past day (the memory rule of ADR-058 decision 6 is a client rule).

## Validation

1. `python -m pytest server/tests -q` passes; the resolver tests run without the model.
2. Smoke after the dev deploy: the six eval cases once, on the `nicolas` persona. Cap: **6 model calls**.
3. Three-turn HTTP smoke after the deploy (free).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 09/10/2026 in the autonomous run of [validation/autonomous-2026-10-09-s41-s42.md](../../validation/autonomous-2026-10-09-s41-s42.md). ADR-058 accepted (status line); ADR-017, ADR-021 and ADR-028 record the partial supersession on their status lines.

- **Capabilities (deviation recorded).** The plan names `extras: true`; the other-day record also needed its own flag, `other_day: true`, because an installed app that receives `record: auto` on an other-day log would write it into today. Both require `actions` (the extra target, `time` and `day` are fields of an action; only the actions view carries them) and are JSON booleans; compact ignores them. Optional `first_day` and `profile.slots_by_day` (`[{weekdays: [ISO 1-7], slots}]`) as the plan says.
- **ADR-033 record.** One new rule, `extras_other_day` (owner `server Chat 3p; ADR-058`), in the meal-change prefixes (seco and duro), inert without its input lines `EXTRAS: on` and `DAY_REF`. A general rule written from ADR-058: no food, amount, person or sentence of a conversation; the cue phrases it names are the ones ADR-058 lists. The fixed line `Só registro os últimos 30 dias.` is contracted copy.
- **Day resolver (scope 2).** `server/day_ref.py` (pure): `ontem`, `anteontem`, a weekday with a preposition, `-feira` or `passado` (most recent past; today's weekday → last week; `a segunda opção` and `na segunda vez` are ordinals), `dia {n}` (this month before today; a later `n` has two candidates, last month and a future day, unless the month is named: `dia {n} de {mês}`, `dia {n} do mês passado`; a month without that day leaves one candidate), `{n} dias atrás` (digits or one to seven in words); every cue must land on one date; an equality phrase (`o mesmo de ontem`, copy source of S33) names no day; `bound`, `ambiguous` (each candidate with its pt-BR date), `future`, `too_old` (over 30 days or before `first_day`), or none. `main.py`: the `DAY_REF` line with that day's slots; a bound other-day log is shaped as a log of today against that day's slots, empty, with no budget (`_past_body`), and carries `day`; the only-today notice is removed from the reply; `too_old` with an other-day log ends with the fixed line and one question action; anything else keeps `none_other_day`. `llm.py`: the named day's missing slot ids join the `suggested_slot` enum.
- **Extra target (scope 1).** With `extras`: input `EXTRAS: on`, action `slot` enum gains `extra`, action gains `time`; an extra log keeps `slot: "extra"`, `suggested_slot` null, `time` only as `HH:mm`, and is recorded `auto` when clear and released (`ask` otherwise; record log `auto_extra`/`ask_extra`); a plan aimed at `extra` loses the target; an extra adds to today's totals for the later actions (`_after_extra`) and answers no meal in the closing lines (`_close_day` with no target).
- **`recent` (scope 3).** `recent[].time` (`HH:mm`) and the line `{date} {weekday} Extra {time} · "{text}" · {kcal}kcal …`. `recent_days` totals are client arithmetic (unchanged).
- **Receipt copy (scope 4).** No server copy change beyond removing the only-today notice: the receipt title is A72's.
- **Unit tests:** `python -m pytest server/tests -q` → 681 passed. New `test_s42_extras_other_day.py` (20): the resolver table (every cue, today's weekday and today's `dia`, month boundary with a 31st, two candidates, two cues, future, 30 vs 40 days, `first_day`, copy source and ordinals), capability validation, `slots_by_day`, the `recent` extra line, the input lines, the schema with and without `extras`, an extra recorded with its time and closings for Lanche and Jantar, a bad time and an unsure extra, a plan never extra, no extra without the capability, an extra before a plan lowering its window, a past-day log recorded with its day and the notice removed, the past day's slot from `slots_by_day`, a past lunch while today's lunch is eaten, too old, ambiguous and unbound never recorded, unchanged without the capability. Evaluator: action expectations `day` and `time`.
- **Evaluation (6 of 6 model calls, effort low, US$ 0.0042).** `--tag s42 --repeat 1 --show` on the `nicolas` persona → 6/6 (`logs/evals/2026-10-09-223433-low.json`, p50 3.5 s): energy drink between lunch and snack → `extra`, `auto`, closings for Lanche and Jantar; coffee after supper at 22:30 → `extra`, `time 22:30`, `auto`; "ontem jantei pizza" at 08:30 → Jantar, `day 2026-10-14`, `auto`, reply "Jantar de ontem: …" without the notice; "na segunda almocei feijoada" on Thursday → Almoço, `day 2026-10-12`, `auto`; "dia 5 de setembro" (40 days) → `Só registro os últimos 30 dias.`, nothing recorded; "dia 3" on 2 November → a question naming both dates, nothing recorded. That question cited the dates as ISO; the candidates of `DAY_REF: ambiguous` now carry their pt-BR date (`2026-10-03 = 3 de outubro`), a unit-tested input change not rerun (the cap was spent).
- **Dev deploy and HTTP smoke:** see the [run report](../../validation/autonomous-2026-10-09-s41-s42.md).

Manual acceptance (after delivery, owner): with A72 on the dev app, an energy drink between lunch and snack shows as an extra at its time, and "ontem jantei …" records in yesterday.
