# Plan — S42 Extras outside the meals and a record in a named past day

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `server`
- Affected code: `server/` only: the `log` action target `slot: "extra"`, the day resolver for `meal_day: other` with an ISO `day`, the 30-day bound, the prompt rules and examples under [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), `recent` and `recent_days` accepting extras, tests, eval cases. Documentation at Completion: [HTTP contract](../../api-contract.md), [v1-chat](../specifications/v1-chat.md).
- Prerequisites: [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) accepted by this approval; [S40](completed/s40-eval-personas.md) delivered.
- Related documentation: [chat](../../produto/specifications/chat.md), [ADR-050](../../produto/adrs/ADR-050-typed-actions-per-message.md) (typed actions), S14 (`meal_day`, [history](completed/)).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s42-extras-and-other-day.md. Implemente o plano aprovado.`

## Objective

A message about something eaten outside the meals is recorded as an extra, and a message that names a day in the last 30 days is recorded in that day, with the day resolved by the server, never guessed ([ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) decisions 1 and 5).

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

<Filled at Completion.>
