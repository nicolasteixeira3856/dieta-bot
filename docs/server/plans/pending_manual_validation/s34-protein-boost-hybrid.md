# Plan — S34 Protein boost: the model proposes, the server validates

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message; code, unit tests, smoke and dev deploy done; the named low-protein dinner on the dev app is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`plan` rule, PROTEIN BOOST), `protein_boost.py` (validator), `main.py` (call site), tests, `evals/` cases. No contract change for the client.
- Related documentation: [ADR-055](../../../produto/adrs/ADR-055-protein-boost-hybrid.md) (accepted with this plan), [ADR-043](../../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md), [ADR-054](../../adrs/ADR-054-chat-reasoning-effort-low.md), [v1-chat](../../specifications/v1-chat.md); benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md) § reforço de proteína, rule text in `benchmark/prompts/new_instructions.py`.
- Prerequisites: [S33](../pending_manual_validation/s33-chat-context-effort-low.md) delivered (effort low; the rule failed at none).

Approving this plan accepts ADR-055. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s34-protein-boost-hybrid.md. Implemente o plano aprovado.`

## Objective

A named dish below the protein floor gets one or two `(opcional)` foods chosen by the model to suit the dish, and the server guarantees the rule (floor, room, two foods, no repeat, no excluded food) by validating and completing the model's items.

## Scope

1. **Prompt.** The `plan` rule gains the PROTEIN BOOST paragraph of the benchmark (`BUDGET` gives `protein_floor` and `window_kcal`; below the floor and with 60 kcal of room, one or two common foods marked `(opcional)`, varied, never a food the dish has nor against a diet fact; kitchen steps, at most 150 g; items, `meal_text` and one reply line; nothing when not due). ADR-033 record: general rule from ADR-055, no example.
2. **Validator.** `protein_boost.py`: `validate(payload, window_kcal, remaining_p, facts)` reads the `(opcional)` items of a plan of today, drops the ones that break the rules of ADR-055 § 2, caps the kcal to the room, recomputes the totals (ADR-042 path), rewrites `meal_text` and the reply line to the surviving items; when nothing survives and the boost is due, `apply()` of S24 appends one food from the table. Pure function, unit-tested on the model's shapes (item label with and without the mark, two foods, repeated food, vegetarian fact, no room, another day).
3. **Diet facts.** A permanent `preference` fact whose text states vegetarian, vegan or an excluded food feeds the validator's exclusion list; a short synonym table in code.
4. **Smoke (at most 12 model calls).** The `boost` family of the benchmark rewritten as eight evaluator cases with independent synthetic texts (ADR-033; the benchmark cases stay in `benchmark/` and are never copied into `server/evals/`): due with room, due with a food the dish has, above the floor, no room, vegetarian, `duro`, "tá bom?", another day. Run once: `--tag s34 --repeat 1`; the four remaining calls may repeat a failed case after a fix. No regression set.
5. **Dev deploy** and one named low-protein dinner on the dev app.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md) rule 5: boost as proposed by the model and validated by the server; provenance line. ADR-043 status line records the partial supersession.

## Out of scope

- Options, recipes, actions. A food database. Client changes.

## Validation

1. `pytest server/tests -q` passes (validator tests included).
2. Smoke as in 4 (≤ 12 calls); numbers in Results, variety counted (distinct foods across the due cases).
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`). ADR-055 accepted; ADR-043 status line records the partial supersession of decision 4.

- **Prompt (scope 1).** The `plan` rule gains PROTEIN BOOST (floor and room from the BUDGET line, one or two `(opcional)` foods as their own items, kitchen steps, at most 150 g, the `Para a proteína (opcional): …` sentence, no boost when not due). ADR-033 record: general rule from ADR-055, no example. After the first smoke chose chicken in most answers, the food list became a pairing by kind of dish (bread or wrap → a filling; pasta, rice or soup → a hot protein; fruit or oats → yogurt or whey; vegetarian → egg, cottage, tofu, legumes; chicken only when nothing else suits): generic guidance, no user data.
- **Validator (scope 2).** `protein_boost.validate(payload, window_kcal, remaining_p, facts, another_day=)`: the foods of the model's sentence (found anywhere in the reply, bold or not) are matched to items (marked, unmarked or with a bare `opcional`) and to a table of fifteen foods (`CANDIDATES`, per 100 g, TACO or USDA FoodData Central). Drops with a reason: `above_floor`, `no_room`, `other_day`, `not_due`, `unknown`, `diet`, `in_dish`, `repeat`, `third`, `grams`; a survivor is shrunk by its step to the room and the gap (`room` when nothing is left). Items, totals (base dish plus table numbers), `meal_text` (the boost span is cut out of a part, so a one-part `meal_text` keeps the dish) and the reply sentence are rewritten; nothing left and due → `apply()` of S24 appends from its table. `main._protein_boost` calls it for every in-scope turn; a plan for another day (`meal_day` other or `amanhã` / `semana que vem` in the message) drops the model's foods and appends nothing. The `BUDGET_TARGET` adjustment plan is not boosted (unchanged).
- **Diet facts (scope 3).** `excluded(facts)`: a permanent `preference` fact with `vegetarian`/`vegan`, or a negative cue (`não como`, `sem`, `alergia`, `intolerância`, `evito`, `não gosto`) with a group word (carne, frango/aves, peixe/frutos do mar, ovo, leite/lactose/laticínio, soja) or a table food. It also governs the server completion (found by a unit test: the S24 table had added chicken for a vegetarian).
- **Unit tests:** `pytest server/tests -q` → 565 passed, 515 subtests. New `test_s34_protein_boost.py` (13): survivor with table numbers, unmarked item found by the sentence, two kept with a repeat and a third dropped, vegetarian drops fish and the completion picks egg, exclusion facts, food the dish has, above the floor, no room and room cap, off-step and unknown, another day and other intents, no proposal falls back to the table, inline sentence with a bare `opcional`, one-part `meal_text`.
- **Smoke (12 of 12 model calls, effort low, US$ 0.0096).** `--tag s34 --repeat 1` (8 synthetic cases): 8/8, p50 4.2 s. Reading the outputs found three defects that the checks missed, all fixed in code with unit tests: a one-part `meal_text` lost the dish when the boost span was removed; a boost sentence inside the dish line was not seen, so the server appended a second boost; the model chose chicken in 3 of 4 proposals. After the fixes and the pairing sentence, `--only` the four due cases `--repeat 1`: 4/4. Variety over the due answers of the second run: cottage, canned tuna, egg (model) and shredded chicken (server completion where the model proposed nothing): 4 distinct foods in 6 food slots, against 1 of 4 in the first run.
- **Dev deploy and three-turn HTTP smoke:** in the batch report.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- One named low-protein dinner on the dev app: the plan shows one or two `(opcional)` foods that suit the dish and the line `Para a proteína (opcional): …`.
