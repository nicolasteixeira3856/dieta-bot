# Plan — S35 Workout energy via the Chat

- Status: Concluído (08/10/2026; approved in the owner's batch message)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (scope and `workout` rule), `llm.py` schema, `shaping.py`, `main.py`, `evals/`, tests.
- Related documentation: [ADR-049](../../../produto/adrs/ADR-049-workout-energy-via-chat.md) (accepted with this plan), [CP10](../../../content-policy/plans/completed/cp10-workout-in-scope-and-skip-boundary.md), [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md); benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md) family `workout`.
- Prerequisites: [S33](../pending_manual_validation/s33-chat-context-effort-low.md) delivered; [CP10](../../../content-policy/plans/completed/cp10-workout-in-scope-and-skip-boundary.md) delivered (scope).

Approving this plan accepts ADR-049. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s35-workout-via-chat.md. Implemente o plano aprovado.`

## Objective

"Treino de hoje, 450 kcal" is in scope and returns a `workout {kcal, mode}` the app records; nothing is inferred.

## Scope

1. **Contract (capability `workout: true`).** Response gains `workout: {kcal, mode: replace|add} | null`. Until S36 (actions) it is a top-level field next to `intent`; with S36 it becomes the `workout` action. `record: auto` when the number is explicit.
2. **Rules.** Scope: a report of a workout done today, with or without its energy in kcal, is `in_scope`. `workout` rule: explicit kcal → `workout` with `replace` (default) or `add` ("mais", a second workout); duration, distance, heart rate, watch or health app → `question` asking the kcal number, nothing inferred; a plan in the same message uses DAY as sent; the reply never states a credit. ADR-033 record: general rule, no example.
3. **Shaping.** Integer kcal 1–5000; `mode` enum; a `workout` with a meal estimate in the same answer keeps both (until S36 there is one estimate at most).
4. **Smoke (at most 12 model calls).** Tag `s35`, synthetic, nine cases: explicit kcal, add, no number, distance, watch, training plan request (out of scope), workout plus meal plus plan, and the two CP10 boundary cases. Run once: `--tag s35 --repeat 1`.
5. **Dev deploy** and one workout turn on the dev app (the receipt appears only with A65; the server log shows the field).

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md) and [HTTP contract](../../../api-contract.md): `workout` field and capability. [content policy](../../../content-policy/specifications/content-policy.md) is rewritten by CP10.

## Out of scope

- The app recording the number and the credit: [A65](../../../android/plans/completed/a65-workout-via-chat.md). Inferred energy, exercise advice.

## Validation

1. `pytest server/tests -q` passes.
2. Smoke as in 4 (≤ 12 calls); numbers in Results.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`), right after [CP10](../../../content-policy/plans/completed/cp10-workout-in-scope-and-skip-boundary.md). ADR-049 accepted.

- **Contract (scope 1).** Request capability `workout: true` (JSON boolean, requires `clarify_rounds` and `auto_record`, ignored by compact). The `chat_turn` schema gains `workout` (`{kcal, mode}` or null) for every client, so the schema family stays one; the response carries it only with the capability. A workout-only turn (no estimate, no question, not a skip) gets `record: auto`, dev-log reason `auto_workout`; the dev log also records the returned `workout`.
- **Rules (scope 2).** Both product rules: a report of a workout done today with or without kcal is in scope; training plans, exercise prescriptions and physiology are out of scope; the CP10 skip boundary; the JSON key list names `plan_budget` and `workout`. New WORKOUT rule in both branches: explicit kcal → `replace` (default) or `add`; no number → a one-line question for the kcal, nothing inferred; a plan or meal in the same message uses DAY as sent; the reply never states a credit or a ceiling. The per-request guard line names "treino em kcal". ADR-033 record: general rules, no example.
- **Shaping (scope 3).** `shaping.shape_workout`: integer kcal 1–5000 and a known mode, else null; null on any non-in-scope turn. A workout next to a meal keeps both.
- **Unit tests:** `pytest server/tests -q` → 574 passed, 524 subtests. New `test_s35_workout.py` (8): bounds and modes, capability validation, workout-only turn recorded, absent without the capability, no number and refusal give null, workout next to a meal, the sentences in every Chat prefix, schema and guard. New `workout` evaluator check with its unit test; the strict-schema test lists the new key.
- **Smoke (9 of 12 model calls, effort low, US$ 0.0052).** `--tag s35 --repeat 1`: 9/9, p50 2.5 s. Explicit kcal → `{420, replace}`, `record: auto`; "mais 180 kcal" over a day with 300 → `{180, add}`; no number, distance and watch without a number → `workout: null` and a one-line question for the kcal; training plan → the fixed out-of-scope copy; workout + snack + dinner request → the workout kept next to the snack log (one estimate until S36); the two CP10 boundary cases as recorded in CP10.
- **Dev turn.** The installed app does not send `workout: true` until A65, so on the device the field is invisible; the deploy check is an HTTP workout turn with the capability against the dev server (batch report).
- `node tools/check-docs.mjs` passes.
