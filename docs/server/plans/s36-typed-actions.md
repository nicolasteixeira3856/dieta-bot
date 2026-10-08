# Plan — S36 Typed actions per message

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (the ACTIONS rule and the rewrite of `intent`/`skip_slots`/`meal_changes` references), `llm.py` schema (`actions[]`, strict, 1–6), `shaping.py` (per action), `main.py` (arithmetic, window, boost, closing per action), dev log, `evals/` (checks and cases in the actions shape), tests.
- Related documentation: [ADR-050](../../produto/adrs/ADR-050-typed-actions-per-message.md) (accepted with this plan), ADR-028/032/042/043/047, [v1-chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md); target schema `benchmark/prompts/new_schema.json`, rule text `benchmark/prompts/new_instructions.py` (`actions`); benchmark [results](../../../benchmark/RESULTADOS_08_10_2026.md) family `multi`.
- Prerequisites: [S33](pending_manual_validation/s33-chat-context-effort-low.md), [S34](s34-protein-boost-hybrid.md), [S35](s35-workout-via-chat.md) delivered.

Approving this plan accepts ADR-050. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s36-typed-actions.md. Implemente o plano aprovado.`

## Objective

One message, several things: the server returns an ordered list of typed actions, each shaped as a single estimate is today, with one clarification at most and never next to another action.

## Scope

1. **Contract (capability `actions: true`).** Response `actions[]` (1–6) of `{id, type, slot, estimate, record_intent, meal_day, meal_change, workout, recipe_id, options, plan_budget}`; `type` enum `log|plan|skip|workout|recipe_recall|question`; `recipe_id` and `options` always null until S37/S38. Without the capability the legacy shape is built from the first action (the mapping of `benchmark/scoring.baseline_view` reversed), so an older client keeps working.
2. **Rules.** The ACTIONS rule of the benchmark: one action per thing; order (logs, plans, workouts, recalls as stated; skips last in profile order; eaten and skipped is a log); a plan after a log sized with the log eaten; a question only inside the action with the doubt; more than six things: the first six and the reply names the rest. The skip closing clause of ADR-047 ("{slot} de hoje fora") is carried into the new rule (it was lost in the benchmark's rewrite). Every rule that spoke of `intent`, `estimate`, `skip_slots` reads "the field of the action being built". ADR-033 record for every rewritten sentence.
3. **Shaping per action.** ADR-042 totals, ADR-032 add/revise, the ADR-043 window and boost (S34) for each `plan`, closing lines once per answer after the last log or plan of today; a `question` action with others → the answer is rejected and regenerated once (the benchmark saw it at `none`, not at `low`).
4. **Smoke (at most 12 model calls).** The evaluator checks accept the actions shape (greedy one-to-one match, as `benchmark/scoring.py`); existing cases are migrated by a script (one action from the legacy expectation) and verified by unit tests of the mapping, not by rerunning them. New tag `s36`, synthetic, seven cases: whole day in one message, skip plus meal, meal plus plan, workout plus meal, seven things, eaten and skipped same slot, clarification on one of two meals. Run once: `--tag s36 --repeat 1`.
5. **Dev deploy** and one two-meal message on the dev app (the app applies one action until A66; the log shows the list).

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): actions, order, shaping per action. [HTTP contract](../../api-contract.md): `actions[]`, capability, legacy mapping. [product Chat](../../produto/specifications/chat.md): one receipt per action (text of ADR-050), rewritten at A66.

## Out of scope

- The app applying the batch: [A66](../../android/plans/a66-typed-actions-batch.md). Options and recipes: S37, S38.

## Validation

1. `pytest server/tests -q` passes, including the legacy mapping tests.
2. Smoke as in 4 (≤ 12 calls); the migrated suite is not rerun in development.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
