# Plan — S36 Typed actions per message

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message; code, unit tests, smoke and dev deploy done; the two-meal message on the dev app is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (the ACTIONS rule and the rewrite of `intent`/`skip_slots`/`meal_changes` references), `llm.py` schema (`actions[]`, strict, 1–6), `shaping.py` (per action), `main.py` (arithmetic, window, boost, closing per action), dev log, `evals/` (checks and cases in the actions shape), tests.
- Related documentation: [ADR-050](../../../produto/adrs/ADR-050-typed-actions-per-message.md) (accepted with this plan), ADR-028/032/042/043/047, [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md); target schema `benchmark/prompts/new_schema.json`, rule text `benchmark/prompts/new_instructions.py` (`actions`); benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md) family `multi`.
- Prerequisites: [S33](../pending_manual_validation/s33-chat-context-effort-low.md), [S34](../pending_manual_validation/s34-protein-boost-hybrid.md), [S35](../completed/s35-workout-via-chat.md) delivered.

Approving this plan accepts ADR-050. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s36-typed-actions.md. Implemente o plano aprovado.`

## Objective

One message, several things: the server returns an ordered list of typed actions, each shaped as a single estimate is today, with one clarification at most and never next to another action.

## Scope

1. **Contract (capability `actions: true`).** Response `actions[]` (1–6) of `{id, type, slot, estimate, record_intent, meal_day, meal_change, workout, recipe_id, options, plan_budget}`; `type` enum `log|plan|skip|workout|recipe_recall|question`; `recipe_id` and `options` always null until S37/S38. Without the capability the legacy shape is built from the first action (the mapping of `benchmark/scoring.baseline_view` reversed), so an older client keeps working.
2. **Rules.** The ACTIONS rule of the benchmark: one action per thing; order (logs, plans, workouts, recalls as stated; skips last in profile order; eaten and skipped is a log); a plan after a log sized with the log eaten; a question only inside the action with the doubt; more than six things: the first six and the reply names the rest. The skip closing clause of ADR-047 ("{slot} de hoje fora") is carried into the new rule (it was lost in the benchmark's rewrite). Every rule that spoke of `intent`, `estimate`, `skip_slots` reads "the field of the action being built". ADR-033 record for every rewritten sentence.
3. **Shaping per action.** ADR-042 totals, ADR-032 add/revise, the ADR-043 window and boost (S34) for each `plan`, closing lines once per answer after the last log or plan of today; a `question` action with others → the answer is rejected and regenerated once (the benchmark saw it at `none`, not at `low`).
4. **Smoke (at most 12 model calls).** The evaluator checks accept the actions shape (greedy one-to-one match, as `benchmark/scoring.py`); existing cases are migrated by a script (one action from the legacy expectation) and verified by unit tests of the mapping, not by rerunning them. New tag `s36`, synthetic, seven cases: whole day in one message, skip plus meal, meal plus plan, workout plus meal, seven things, eaten and skipped same slot, clarification on one of two meals. Run once: `--tag s36 --repeat 1`.
5. **Dev deploy** and one two-meal message on the dev app (the app applies one action until A66; the log shows the list).

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): actions, order, shaping per action. [HTTP contract](../../../api-contract.md): `actions[]`, capability, legacy mapping. [product Chat](../../../produto/specifications/chat.md): one receipt per action (text of ADR-050), rewritten at A66.

## Out of scope

- The app applying the batch: [A66](../../../android/plans/completed/a66-typed-actions-batch.md). Options and recipes: S37, S38.

## Validation

1. `pytest server/tests -q` passes, including the legacy mapping tests.
2. Smoke as in 4 (≤ 12 calls); the migrated suite is not rerun in development.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`). ADR-050 accepted; the status lines of ADR-028, ADR-032 and ADR-047 record the partial supersession.

- **Contract (scope 1).** Capability `actions: true` (JSON boolean, requires `meal_changes`). The meal-change branch always answers in the strict schema `chat_turn_actions` (`actions[]` of `{id, type, slot, estimate, record_intent, meal_day, meal_change, workout, recipe_id, options, plan_budget}`, `recipe_id` and `options` typed null until S37/S38; the six-action limit is enforced by the server, not the schema). With the capability the response adds `actions[]`, each with its shaped `estimate`, `question`, `record`, `record_intent`, `meal_day`, `meal_change`, `workout`, `plan_budget`; without it (the installed app) the top-level fields are the legacy view: the first log or plan, `skip_slots` from every skip action, `workout` from the first workout action. The legacy branch without `meal_changes` (old APKs) is unchanged. A single-estimate payload (the old shape) is read as its actions (`actions.from_legacy`, the benchmark's `baseline_view`), so every older test mock still drives the route.
- **Rules (scope 2).** `product_meal_changes` became `product_actions` (keys `reply, scope, actions, memory_updates, memory_used, digest`; a refusal is one question action); `intent_meal_changes` and SKIPS left this branch for the ACTIONS rule: one action per thing, the order, eaten-and-skipped is a log, six at most with the rest named, no question next to another action, a plan after a log sized with it eaten, every `intent`/`estimate`/`skip_slots` reference read as the field of the action, the ADR-047 skip clauses carried (`{slot} de hoje fora.`, the no-slot clause). The WORKOUT rule names its action. ADR-033 record: every rewritten sentence is a general rule from ADR-050/ADR-047, no example.
- **Shaping per action (scope 3).** `server/actions.py` normalizes; `chat_reply` (meal-change branch) applies the ADR-042 total and the ADR-055 boost per action in generation, regenerates once when a question sits next to another action (then drops the question actions), runs the plan-budget adjustment on the first plan, and shapes each action with `shape_chat_turn` in order with the reply threaded and DAY updated by the released logs before it (`_after_log`). Closing lines run once after the last released log or plan of today, with the meals skipped or held in the same message counted as skipped; the model's closing line for such a meal is removed (found in the smoke: a held snack kept a model closing line). Output moderation covers every action's text.
- **Unit tests:** `pytest server/tests -q` → 591 passed, 536 subtests. New `test_s36_actions.py` (17): order and eaten-wins, cap and drop count, empty and legacy payloads, question detection, capability validation, whole day, skip and meal in both views, plan after a log sees it eaten, the question regeneration (accepted and dropped), a clarification on one of two meals, refusal as one question action, workout and meal, closing lines once, no closing line for a held meal, the greedy `actions` check and `actions_order`, the migration verdicts. Three schema tests updated to the actions shape.
- **Evaluator.** New checks `actions` (greedy one-to-one, order-free, per-action keys judged on a single-estimate view) and `actions_order`; `checks.migrate_expect` turns a legacy expectation into one action plus one skip action per expected skip, and `python -m evals.run --actions` sends the meal-change cases with the capability and the migrated expectations. The 105 meal-change cases migrate without error (offline check); their files keep the legacy expectations, which test the legacy view the installed app uses; the migrated suite is not rerun in development (plan rule).
- **Smoke (9 of 12 model calls, effort low, US$ 0.0062).** `--tag s36 --repeat 1` (7 synthetic cases): 6/7, p50 7.8 s, p95 11.3 s. `s36-treino-e-refeicao` failed only because the model asked the size of the pizza slices (a first-message rule, not the actions): the workout and the meal were two actions; the case now accepts the meal released or held and was re-judged offline → pass. The seven-things case kept six actions and the reply said "O almoço de amanhã ficou de fora; envie-o novamente". After the closing fix, `--only s36-duvida-em-uma,s36-dia-inteiro --repeat 1`: 2/2, one closing line for the dinner and none for the held snack.
- **Dev deploy and HTTP smoke:** in the batch report.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- One two-meal message on the dev app (the installed app has no `actions` capability): the first meal is recorded, the reply covers both, the dev log lists both actions.
