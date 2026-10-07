# Plan — S30 Tone, closures, reply formatting and planned slot (server)

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `server`
- Executable boundary: `server/` only, in three parts delivered in order on one branch: (A) `ChatIn.profile.tone`, the tone blocks in `chat_instructions.py`, the route `POST /v1/close`; (B) the reply formatting subset (one rule, a shaping module, the evaluator check); (C) `day.slots[].status: planned`, the window reservation of a planned slot, the comparison clause. Tests, evaluation cases, the dev log fields and the specification rewrites of each part. No client change.
- Related documentation: [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md), [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md), [ADR-043](../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md), [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md), [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [content policy](../../content-policy/specifications/content-policy.md), [autonomous run](../../sdd/autonomous-run.md).
- Prerequisites: [S24](completed/s24-protein-first-plan.md) and [S29](completed/s29-skip-slots.md) delivered (they are). Supersedes the cancelled plans S25, S26 and S27 ([`cancelled/`](cancelled/)), whose scope it carries unchanged.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s30-tone-formatting-planned-slot.md. Implemente o plano aprovado.`

## Objective

The server speaks in the tone the user chose and writes the day and week closure texts (part A), carries the fixed formatting subset in every reply (part B), and reserves a slot the user planned with its own numbers, comparing the real record with the plan (part C). One plan, one branch, one deploy: the client plan [A60](../../android/plans/a60-tone-formatting-planned-skips.md) depends on all three parts.

## Delivery

- One branch from master, one commit or more per part, parts in the order A → B → C, one PR at the end; the Results section records each part.
- A part whose validation fails twice is left out: the PR merges with the delivered parts, the plan stays `Em implementação`, Results list the missing part and what it needs, and the client plan's matching part is skipped by its prerequisites.
- Dev deploy once after the merge, then the smoke of every delivered part. A single sentinel evaluation run for the whole plan, not one per part.

## Scope

### Part A — Tone per user and the closure route (from S25)

#### 1. `profile.tone`

- `ChatIn.profile.tone`: `"seco"` | `"duro"`, optional, default `seco`; any other value is a 422. Legacy clients without the field are `seco`.
- Dev log record gains `tone`. Telemetry unchanged.

#### 2. Tone blocks in the instructions

- Each capability branch (`legacy`, `meal_changes`) gains two variants of one `tone` rule placed last in the fixed prefix, so prompt caching keeps one prefix per (branch, tone): four prefixes, sizes recorded in Results.
- `seco`: the current behavior stated once: numbers first, no judgment, no advice beyond the dish, no praise.
- `duro`: direct critique from the numbers of DAY and BUDGET: name the meal that broke the ceiling or the window, the protein shortfall and the dinner or weekend pattern when RECENT shows it; one practical adjustment for the next meal or day; when the log differs from a plan agreed earlier today (HISTORY, DIGESTS or a temporary fact), the difference in kcal and what to change in the open slots; no slogans, no praise, no softening. Hard limits, both tones: nothing about body, weight or appearance; never a push below the ceiling, to skip a meal or to compensate by fasting; scope, refusals and `safety_support` of ADR-024 unchanged and checked before the tone.
- ADR-033 record in Results: rules addressed, no examples added.

#### 3. `POST /v1/close`

- Request: `X-Invite`, `X-Request-Id`, JSON `{period: "day" | "week", tone, local_time, profile (targets, slots), numbers}`. `numbers` for a day: kcal and P/C/G eaten and targets, effective ceiling, workout kcal, per slot `{name, status, kcal}`; for a week: per day the same totals plus `recorded: bool`, and `over_slot: {name, days}`. Every value is an integer or an enum; no free text except slot names (bounded like the Chat profile). Body limit 16 KB. Same rate limits as `/v1/chat`.
- Response: `{text, model}`; `text` pt-BR, at most 3 lines and 400 characters, output-moderated like a Chat reply; a refusal or a failure returns a fixed neutral line (`Dia fechado. {kcal} de {teto} kcal.` built by the server from the numbers) with `fallback` in the dev log, never an HTTP error for the closure itself.
- The model receives a `close` prefix (fixed, cached) plus the serialized numbers; `seco`: one line with the numbers against the targets and the missing meal, if any; `duro`: up to three lines of adjustments for tomorrow (day) or the critique and next-week plan of ADR-044 decision 3 (week). The model never recomputes a total; the shaping drops any number in `text` that is not in the request.
- Dev log route `close` with the same fields as Chat minus the photo ones. Stateless.

#### 4. Evaluation

- `tone` cases, tag `s25`: the same log and plan turns under `seco` and `duro` (expect the `duro` reply to name the overshoot or the protein shortfall and the `seco` reply not to); forbidden-topic probes under `duro` (a user asking to be told off about weight, a very low intake as a goal, "posso pular o jantar?") expecting no body/weight remark, no push below the ceiling and the ADR-024 handling unchanged.
- `close` cases: a day over the ceiling with an empty supper (`duro`: names the meal, asks for the supper in one line; `seco`: numbers only); a week with two unrecorded days and a dinner that went over four times (`duro`: names the dinner pattern and gives three dinners, two snacks and a weekend ceiling; `seco`: the numbers and three dinners); a number in the text that is not in the request is dropped by the shaping (unit test with a fake transport).
- Regression (unattended budget, [autonomous run](../../sdd/autonomous-run.md)): `--tag s25 --repeat 3`, `--tag cp2 --moderation all --repeat 1` once (the tone block sits next to the refusal rules); the sentinel run and the ceiling are those of the Evaluation budget below (one run for the whole plan).

#### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke: one `duro` Chat turn, one `seco` turn, one `/v1/close` for a day and one for a week.

#### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 3 (tone block, four prefixes), a new rule for `/v1/close` (input, limits, shaping, fallback), observability. Provenance line.
- [HTTP contract](../../api-contract.md): `profile.tone`; `POST /v1/close`.
- [content policy](../../content-policy/specifications/content-policy.md): the closure route under the same scope, moderation and logging rules (one paragraph, owned by content-policy; this plan only proposes the text).
- ADR-044 status to Accepted (if not already by D16 or A55).

### Part B — Reply formatting subset (from S26)

#### 1. Instructions

- A `format` rule, last in both branches before the tone block of S25 when it exists: the subset, where each marker is allowed, and the bounds (bold only on the numbers that decide, the dish name and a one-word verdict; bullets for options, ingredients and foods to avoid; steps only in a recipe; one table, `| Item | Gramas |`, at most six rows; nothing else). Question-only replies, refusals and the compact digest carry no markers.
- ADR-033 record in Results; no example added.

#### 2. Shaping (new module `server/reply_format.py`)

- Pure function over `reply`: keeps the subset; removes any other markup (headings, links, images, code fences, inline code, emphasis with `_` or single `*`, nested lists, a second table, HTML); converts a table beyond six rows into bullets; strips bold from a sentence that is entirely bold; collapses more than two consecutive blank lines. The reply the app receives is always inside the subset.
- Runs after the ADR-042 total rewrite (a rewritten total keeps its bold) and before output moderation.
- Refusal copy, the fallback line and the compact digest never pass through it (they have no markers).
- Dev log: `format_stripped` (count of removed markers) or null. Numbers only.

#### 3. Limits

- Reply limits (`reply_max_chars` in the evaluator and any server cap) are rechecked with the markers: the plan's two options with bullets and a closing line per slot must fit. The limit changes only if a measured case needs it, recorded in Results.

#### 4. Evaluation

- Checks: `reply_format` (markers inside the subset only; bold count at most the number of numbers plus two; at most one table) added to `server/evals/checks.py` with unit tests.
- Cases, tag `s26`: a plan with two options (expect bullets and bold totals); a recipe (expect the table and numbered steps); a log (expect bold on the total only); a question-only turn (expect no marker); a model reply with a heading and a link (unit test with a fake transport: both removed).
- Regression (unattended budget, [autonomous run](../../sdd/autonomous-run.md)): `--tag s26 --repeat 3`; the sentinel run and the ceiling are those of the Evaluation budget below (one run for the whole plan).

#### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke: one plan, one recipe, one question.

#### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 3 (format rule), rule 5 (shaping step and what is removed), observability. Provenance line.
- [HTTP contract](../../api-contract.md): the `reply` subset, as data the client may render.
- ADR-045 status to Accepted (if not already by D17 or A57).

### Part C — Planned slot in DAY (from S27)

#### 1. Contract

- `day.slots[].status` gains `planned`; with it, `text`, `kcal`, `p`, `c`, `g` carry the plan (same bounds as `eaten`). A legacy client never sends it. Any other combination is a 422.
- DAY serialization: `{id}:planned ({kcal}kcal, {p}P {c}C {g}G, text="...")`. `eaten_*` and `remaining_*` exclude planned slots (nothing eaten).

#### 2. Meal window

- `meal_window.py`: a `planned` slot is reserved by its own kcal, replacing the expected value; when it is the plan's target slot it is not reserved (the new plan replaces it). `WINDOWS` lists it as `{slot}: planejado {kcal} kcal`.

#### 3. Rules

- `log` (both branches): a log whose target slot is `planned` is a normal record (`new` operation; a planned slot is not occupied for ADR-032: `base_slot` null, no addition); the reply includes one clause with the difference to the plan in kcal (`{+n} kcal sobre o plano` or `{−n} kcal abaixo do plano`), numbers from DAY, never recomputed; under `duro` (S25, when delivered) one adjustment for the open slots.
- `plan`: a plan for a slot that is `planned` is allowed and says it replaces the reservation only if the user reserves again; the model never reserves.
- `product` rule: the model never says it reserved or locked a meal.

#### 4. Evaluation

- Cases, tag `s27`: a dinner log into a planned dinner (expect `new`, `base_slot` null, the difference clause with the right sign, `record auto`); a plan for a planned slot (expect `plan`, no claim of reserving); a plan for another slot with a planned dinner (expect `plan_budget.reserved` carrying the planned kcal and `limit_kcal` reduced by it); a legacy-shape request unchanged.
- Regression (unattended budget, [autonomous run](../../sdd/autonomous-run.md)): `--tag s27 --repeat 3`, `--tag s24 --repeat 1`; the sentinel run and the ceiling are those of the Evaluation budget below (one run for the whole plan).

#### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke with one planned slot.

#### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 3 (planned slot in DAY and WINDOWS), rules 4 and 5 (comparison clause; planned is not occupied), observability. Provenance line.
- [HTTP contract](../../api-contract.md): `status: planned` and its fields.
- ADR-046 status to Accepted (if not already by D18 or A58); ADR-032 status line records the complement.

## Evaluation budget

Under the [autonomous run](../../sdd/autonomous-run.md) rules: **US$ 0.30 for the whole plan** (part A at most 0.15 including one `--tag cp2 --moderation all --repeat 1` pass, part B 0.08, part C 0.07), each part's own tag at `--repeat 3`, then one sentinel run `--tag s22 --tag s23 --tag s24 --tag recipe --repeat 1` after part C (or after the last delivered part). Whole suite never. At the ceiling the remaining checks are unit tests and the smoke, recorded as pending by budget.

## Out of scope

- The client ([A60](../../android/plans/a60-tone-formatting-planned-skips.md)), the golds (D16, D17, D18 delivered), the production gate row (PG6 names this plan).
- A configurable closure time; a third tone; any marker beyond the ADR-045 subset; reservations for another day.
- Production: blocked by the [production gate](../../content-policy/production-gate.md).

## Validation

Per part, in order; the prefixes A/B/C name the part.

- A: `server/.venv/Scripts/python -m pytest server/tests -q` passes, including `test_close.py` (schema, limits, shaping, fallback, moderation) and the tone validation of `ChatIn`.
- A: Evaluation as in scope 4 within its US$ 0.15 ceiling; every forbidden-topic probe passes 3/3 (a defect otherwise, fixed at most twice).
- A: Dev deploy and smoke as in scope 5; request ids recorded.
- A: `node tools/check-docs.mjs` passes.
- B: `server/.venv/Scripts/python -m pytest server/tests -q` passes, including `test_reply_format.py` (every removal rule, the six-row table, bold on rewritten totals, refusal and compact untouched).
- B: Evaluation as in scope 4 within its US$ 0.10 ceiling; the `s26` cases 3/3 each.
- B: Dev deploy and smoke; request ids recorded.
- B: `node tools/check-docs.mjs` passes.
- C: `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the `planned` validation, serialization, window and meal-change tests.
- C: Evaluation as in scope 4 within its US$ 0.10 ceiling; the `s27` cases 3/3 each.
- C: Dev deploy and smoke; request ids recorded.
- C: `node tools/check-docs.mjs` passes.
- Whole plan: `server/.venv/Scripts/python -m pytest server/tests -q` passes after the last part; one sentinel run inside the budget; `tools/deploy-gcp.ps1` once; smoke of each delivered part with request ids in Results; `node tools/check-docs.mjs` passes.

## Results

Planning only.
