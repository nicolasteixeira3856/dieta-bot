# Plan — S21 Cooking help and the over-budget choice in a plan

- Status: Concluído
- Date: 05/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: the `plan` rule of the Chat instructions, the `chat_turn` schema and shaping, the plan budget check and adjusted-plan verification in the `/v1/chat` route, request validation, the dev conversation log record, tests, and evaluation cases/checks/runner.
- Related documentation: [server Chat specification](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md), [server README](../../README.md) plan list, [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md) and the status lines of [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) and [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md).
- Prerequisites:
  - [S19](s19-generalizable-chat-instructions.md) and [S20](s20-tali-prompt-identity.md) delivered or cancelled: all three edit the same instructions and must not run at the same time.
- Followed by: design plan D12 (gold `chatRB`) and Android plan A50, which consumes the capability below. A50 starts after D12 is `Concluído` and this plan is deployed to dev.

Approving this plan accepts ADR-039. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s21-plan-cooking-and-budget-choice.md. Implemente o plano aprovado.`

## Objective

A recipe plan becomes cooking help: a real dish, optional additions, steps. When the dish is over what is left of the day, the server says so from its own arithmetic, reports the user's choice when one was given, and never returns an adjusted plan above its target.

## Discovery evidence

Collected on 05/10/2026 in a worktree experiment, not delivered. Model `gpt-6-luna`, dev evaluator, synthetic cases per [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md). A diagnostic replay of the owner's logged turn (dev log, [ADR-015](../../adrs/ADR-015-log-conversa-dev.md)) was used only as a diagnostic; it is not a fixture and nothing from it enters the instructions.

| Measure | Result |
|---|---|
| Diagnostic turn, current rule, effort none / low / medium (3 each) | 0 of 9 added any food to the listed ones; medium took 14–32 s |
| 3 recipe cases, current rule, none / low | 1 of 3 and 0 of 3 cases passed the addition check |
| Same cases with a general cooking rule, none / low | 3 of 3 and 3 of 3 |
| Full suite, 1 run, none: current rule / cooking rule | 183/194 and 189/194; non-recipe failures differ between runs (flakes) |
| 10 recipe and over-budget cases with a prompt-only budget rule, none / low | 24/30 and 26/30 runs |
| Same, when the user had already chosen (go over, adjust, upfront) | every run followed the choice |
| Same, deciding to ask when over | 1–3 of 3 runs per case; 0 of 6 on the diagnostic turn |
| Estimate of one fixed dish across runs | 570–1080 kcal |
| `low` vs `none` on these cases | p95 about 11.7 s vs 6.7 s; cost about 1.3× |

Conclusion carried by ADR-039: the model extracts the choice well and does the budget arithmetic poorly, so the server owns the arithmetic.

## Scope

### 1. Instructions (one branch for every client)

The `plan` rule changes in the registry (`server/chat_instructions.py`), shared by the legacy and meal-change branches. No example is added.

- **Cooking:** a recipe or "what do I make" plan names a real dish, uses the user's foods as the base, may add up to 3 common, low-cost foods that improve flavor, volume, protein or satiety for few kcal, each marked `(opcional)` and included in items. Reply lists ingredients with grams and up to 5 numbered steps with temperature and time.
- **Reservations:** another meal still to be eaten today, stated in the message or MEMORY, is reported in `plan_budget.reserved` (label, estimated kcal) and never enters items or `meal_text`.
- **Choice:** `plan_budget.choice` is `over_ok` when the user said going over is fine, `fit` when they asked for the dish to fit what is left, otherwise null; read from the current message and earlier turns about this dish.
- **No budget arithmetic in reply:** the reply never says whether the dish fits or by how much. This removes "otherwise states the excess" from rule 3c for every client; the app already shows the projected day (chat spec rule 16).
- **Adjusting:** when the input carries `BUDGET_TARGET`, rebuild the same dish within it, shrinking calorie-dense foods first and keeping the additions where possible.
- **ADR-033 record:** the delivery records, in Results, the general rule addressed (rule 3c and ADR-039), its source specification, and that no example was added. The assembled prompt is reviewed, not only the edited constant. The existing inventory checks must pass unchanged.

### 2. Model schema

Every `chat_turn` schema gains a required `plan_budget`: object or null, `{reserved: [{label, kcal}], choice: "over_ok" | "fit" | null}`. The same shape for every client keeps one schema family for prompt caching. Shaping: null unless intent is `plan` with an estimate; at most 3 reservations; label trimmed, 1–40 characters; kcal an integer 1–3000; invalid entries dropped; an unknown choice becomes null.

### 3. Capability and request

- `plan_budget: true` opts in. It requires `clarify_rounds` and `auto_record: true` (v4), like `meal_changes`, and accepts only JSON booleans; other combinations return 422. Absent or false keeps every older response shape; the model fields are stripped.
- `fit_kcal`: optional integer 1–5000, only with the capability (otherwise 422). The app sends it with **Ajustar para caber**.

### 4. Budget check (server, deterministic)

For an opted-in `plan` with an estimate, today's DAY and a non-null `remaining_kcal`:

- `limit_kcal` = `fit_kcal` when present, otherwise `remaining_kcal` − sum of reservation kcal.
- `over_kcal` = max(0, estimate.kcal − `limit_kcal`).
- **Adjusting** (`fit_kcal` present, or model choice `fit`) with `limit_kcal` ≥ 1 and `over_kcal` > 0: one more model call with the same input plus the per-request line `BUDGET_TARGET: {limit_kcal} kcal` after the current message (per-request context, not fixed instructions). The second result replaces the first only if it is a valid plan; the check runs again on it. Still over: return it with `over_kcal` > 0, so the app shows the choice again. No third call.
- `limit_kcal` ≤ 0: no adjustment call; the over state is returned.
- Response, capable clients only: top-level `plan_budget` `{limit_kcal, over_kcal, reserved, choice}` or null. Null for every other intent, a missing `remaining_kcal`, another meal day or a refusal.
- The record gate is unchanged: a plan stays `record: none`.

### 5. Log and timing

- The dev conversation log adds `plan_budget` and an `adjust_retry` boolean to the chat record. Telemetry is unchanged.
- The adjustment call shares the route deadline; on timeout the first plan is returned with its over state.

### 6. Evaluation

- Bring in the worktree experiment as the starting point: the `items_beyond` check and the 10 synthetic `recipe` cases. Convert the over-budget expectations from reply text to the structured result.
- New check `plan_budget` with keys `over` (bool), `choice`, `reserved_has` (label terms), `reserved_kcal_range`; the runner sends `plan_budget: true` for cases tagged `over-budget`.
- Cases: fits with no choice; over with no choice; user answered go over; user answered adjust; choice stated upfront (both); reservation with given kcal; reservation the model must estimate; `fit_kcal` from the pill; `limit_kcal` ≤ 0; a legacy client (no capability) gets no `plan_budget` and no excess sentence. All synthetic, with foods, profiles and phrasing independent of the diagnostic turn.
- Unit tests with a fake model transport: arithmetic, the adjustment retry and its cap, shaping limits, 422 combinations, legacy shapes byte-for-byte unchanged, log fields.

### 7. Dev deploy

After the tests and the evaluator pass: `tools/deploy-gcp.ps1` to the dev VM ([ADR-013](../../adrs/ADR-013-gcp-host.md)). Dev only.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): rule 3c rewritten (cooking help, no budget arithmetic, reservations, choice); rule 5 (`plan_budget` in the schema); rule 5a (its shaping); a new rule for the budget check and adjustment.
- [HTTP contract](../../../api-contract.md): `plan_budget` and `fit_kcal` in the request, `plan_budget` in the response, a capability section like meal changes.
- On approval: ADR-039 status to Accepted; ADR-023 and ADR-026 status lines record the partial supersession.
- The product Chat specification (rule 16) and the gold inventory change with D12 and A50, not here.

## Out of scope

- Android UI, pills, the fixed line and the capability flag in the client (A50); the `chatRB` gold (D12).
- The Home budget and `reservedUpcoming`.
- Model or provider changes. `reasoning.effort` stays `none`; adopting `low` still follows ADR-023 decision 6 on the full suite.
- Calorie accuracy of estimates in general (the dish-estimate spread above) beyond what the check enforces.
- Production: blocked by the [production gate](../../../content-policy/production-gate.md).

## Validation

1. `server/.venv/Scripts/python -m unittest discover -s server/tests` passes.
2. Evaluator, `--effort none --repeat 3`, `recipe` tag: every recipe case passes `items_beyond` in at least 2 of 3 runs; `over` and `limit_kcal` match the server arithmetic in every run; `choice` is right in at least 90% of runs where the user stated one; adjustment cases (`fit` choice or `fit_kcal`) end at or below their target in at least 2 of 3 runs, and every run that ends above it reports `over_kcal` > 0.
3. Full evaluator suite, `--effort none --repeat 3`: no case that passed before this plan drops below 2 of 3; numbers recorded in Results next to a baseline run on the same day.
4. Dev smoke through the dev URL after deploy: one opted-in over-budget recipe turn returns `plan_budget` with `over_kcal` > 0; one with `fit_kcal` returns a plan at or below it; one legacy-shape request is unchanged.
5. `node tools/check-docs.mjs` passes.

## Results

Owner approval on 2026-10-05 by name, with the note that another session was implementing S20 at the same time. The S20 prerequisite was therefore not met when this plan started; the owner's explicit approval takes precedence. The two plans touch different rules of the registry (`plan` here; the product name lines there), and this delivery merged current master before its pull request. Work ran in the isolated `s21-plan-cooking-budget` worktree on `feat/s21-plan-cooking-budget`, from master `52eac76`.

### Delivered

- `server/plan_budget.py`: the model-side shape (`SCHEMA`, `shape_model_budget`), `plan_kcal`, `check` and `needs_adjustment`. Pure functions, unit-tested.
- `server/llm.py`: every `chat_turn` schema requires `plan_budget` (object or null), the same shape for both capability branches.
- `server/main.py`: `plan_budget` and `fit_kcal` request fields with the validation of scope step 3; the deterministic check of step 4; one adjustment call with `BUDGET_TARGET: {limit_kcal} kcal` appended to the per-request text, sharing the route deadline, keeping the first plan on failure, timeout or an invalid second result, and carrying the first call's reservations and choice into the second check; top-level `plan_budget` for capable clients, null outside a plan of today with an estimate and `remaining_kcal`; log fields `plan_budget` and `adjust_retry`.
- `server/shaping.py`: output moderation covers reservation labels.
- `server/chat_instructions.py`: the `plan` rule rewritten (cooking help, reservations, choice, no budget arithmetic in reply, `BUDGET_TARGET`). Shared by both Chat branches; compact unchanged. The ADR-033 record: the general rule addressed is Chat 3c under ADR-039; no example was added; the assembled legacy and meal-change prefixes were reviewed in full, not only the edited constant; the inventory, cue and provenance tests pass unchanged. The `product` rules and their key lists were not edited (the strict schema carries the new key), which also kept this delivery apart from S20's name lines.
- Evaluation: `items_beyond` and `plan_budget` checks in `server/evals/checks.py`, with unit coverage; the runner opts cases tagged `over-budget` into the capability (`case_request`); the ten experiment cases converted to structured expectations (`plan_budget` with `over`, `choice`, `limit_kcal`, `reserved_has`, `reserved_kcal_range`; the unshrunk-dish lower bound set to 600 kcal because the dish estimate varies while the over state is asserted separately); three new cases: `receita-ajuste-alvo` (`fit_kcal` from the pill, risotto, custom profile), `receita-sem-margem` (`limit_kcal` ≤ 0 with a stated reservation) and `receita-sem-capability` (no capability: no `plan_budget`, no excess sentence).
- Tests: `server/tests/test_plan_budget.py` (shaping limits, arithmetic, no check outside a plan of today, adjustment conditions, schema, shared rule text, output moderation, evaluator check, route: one call when fitting or `over_ok`, retry with the exact target and identical instructions, `fit_kcal` target with a still-over second plan and no third call, invalid or failed retry keeps the first plan, no call with `limit_kcal` < 1, null cases, clients without the capability byte-for-byte unchanged, 422 combinations, compact ignores the fields, log fields).

Prefix sizes: legacy 30,096 characters (`2caf4d79…b720`), meal changes 33,857 (`54dfde6e…d5e6`), compact 3,382 unchanged. Growth versus master: +1,628 characters in each Chat branch.

### Validation

1. `server/.venv/Scripts/python -m unittest discover -s server/tests`: 249 tests OK. `python -m pytest server/tests -q`: 363 passed, 396 subtests passed (the pytest-style tests are not collected by unittest discovery).
2. Evaluator, recipe set, `--effort none --repeat 3`, 13 cases: **39/39 repetitions, 13/13 cases** after the bound change (36/39 before it; the three misses were unshrunk dishes at 670–690 kcal against a 700 kcal lower bound, each with `over_kcal` > 0). Every `items_beyond` case 3/3; `over_kcal` and `limit_kcal` consistent with the returned estimate in every repetition (the check recomputes them); `choice` right in 100% of the repetitions where the user stated one; the two adjustment cases (`fit` choice and `fit_kcal`) ended at or below their target in 3/3 each; `receita-sem-margem` returned the over state with `limit_kcal` −120 and no adjustment call in 3/3; `receita-sem-capability` had no `plan_budget` and no question mark in 3/3. p50 3,989 ms, p95 7,627 ms (the adjustment cases carry two model calls); estimated US$0.0127.
3. Full suite, `--effort none --repeat 3`, same day, baseline = today's run of master's prompt from S19 (`577cfea1…`, real moderation) versus this candidate:

| Set | Baseline (master) | S21 candidate |
| --- | --- | --- |
| existing, 155 × 3 | 152/155 cases, 458/465 reps | 153/155 cases, 452/465 reps |
| pairs, 28 × 3 (baseline 28 × 6) | 26/28 cases, 166/168 reps | 28/28 cases, 84/84 reps |
| reserved, 8 × 3 (baseline not valid today) | — | 8/8 cases, 24/24 reps |

   Two incumbents dropped below 2 of 3 in the candidate run: `correcao-slot-sugerido` (3/3 → 1/3, one repetition malformed model JSON) and `mesmo-cafe-ontem-recent` (3/3 → 0/3). Ten-repetition comparison on both prefixes, same day: `correcao-slot-sugerido` master 9/10, candidate 9/10 and 7/10 in two runs; `mesmo-cafe-ontem-recent` master 3/10, candidate 5/10 and 5/10; `hard-nao-jantei-ainda` 9/10 vs 10/10; `correcao-dois-pratos` 9/10 vs 10/10; `registro-lanche-dois-pontos` 8/10 vs 9/10 and 10/10. No drop is attributable to this plan; `mesmo-cafe-ontem-recent` is unstable on every prefix measured today. Candidate usage: 3,398,958 input / 3,279,596 cached / 79,583 output tokens, p95 3,263 ms, estimated US$0.0845.

   **Deviation:** the candidate runs used a local clean moderation verdict instead of the provider's moderation endpoint, because that endpoint's daily request cap (10,000 per day, shared with the dev server) was exhausted by the day's evaluations. Model calls were real. Cases whose expected outcome depends on moderation flagging the input or output are therefore not re-evidenced by these runs; their route tests with mocked verdicts are unchanged. The baseline run earlier today used real moderation. The provider also rate-limited model calls at two workers once moderation stopped pacing them; the final runs used one worker and six retries.
4. Dev deploy and HTTP smoke: recorded below.
5. `node tools/check-docs.mjs`: see below.

### Deploy and smoke

- Dev deploy: `tools/deploy-gcp.ps1` from master `c60c372` after the merge of [PR #132](https://github.com/nicolasteixeira3856/dieta-bot/pull/132), code only; `GET /health` 200 `{"ok": true, "model": "gpt-6-luna", "safety_id": "on"}`.
- HTTP smoke (validation 4): first attempted after the deploy and blocked for the rest of that evening: every Chat turn through the dev URL returned 503 `content_policy_unavailable` because the provider's moderation endpoint had reached its daily request cap, which is counted per project and was shared by the evaluator and the dev server. After the owner created a key in a separate project and `tools/deploy-gcp.ps1 -Env` shipped it (same master, request ids `s21-smoke-1791248397-0`, `-1`, `-2`), the three turns passed: an opted-in over-budget recipe returned `plan_budget {limit_kcal: 450, over_kcal: 340, reserved: [], choice: null}` with a 790 kcal dish and `record: none`; the `fit_kcal` turn from the pill returned a 500 kcal plan with `{limit_kcal: 500, over_kcal: 0, choice: "fit"}`; the legacy-shape request returned the usual keys with no `plan_budget`.
- Follow-up outside this plan: the evaluator's two moderation calls per repetition consume the shared daily cap; pacing or a separate key for evaluation would protect the dev server.
