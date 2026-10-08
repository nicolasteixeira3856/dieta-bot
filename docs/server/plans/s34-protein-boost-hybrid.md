# Plan — S34 Protein boost: the model proposes, the server validates

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`plan` rule, PROTEIN BOOST), `protein_boost.py` (validator), `main.py` (call site), tests, `evals/` cases. No contract change for the client.
- Related documentation: [ADR-055](../../produto/adrs/ADR-055-protein-boost-hybrid.md) (accepted with this plan), [ADR-043](../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md), [ADR-054](../adrs/ADR-054-chat-reasoning-effort-low.md), [v1-chat](../specifications/v1-chat.md); benchmark [results](../../../benchmark/RESULTADOS_08_10_2026.md) § reforço de proteína, rule text in `benchmark/prompts/new_instructions.py`.
- Prerequisites: [S33](s33-chat-context-effort-low.md) delivered (effort low; the rule failed at none).

Approving this plan accepts ADR-055. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s34-protein-boost-hybrid.md. Implemente o plano aprovado.`

## Objective

A named dish below the protein floor gets one or two `(opcional)` foods chosen by the model to suit the dish, and the server guarantees the rule (floor, room, two foods, no repeat, no excluded food) by validating and completing the model's items.

## Scope

1. **Prompt.** The `plan` rule gains the PROTEIN BOOST paragraph of the benchmark (`BUDGET` gives `protein_floor` and `window_kcal`; below the floor and with 60 kcal of room, one or two common foods marked `(opcional)`, varied, never a food the dish has nor against a diet fact; kitchen steps, at most 150 g; items, `meal_text` and one reply line; nothing when not due). ADR-033 record: general rule from ADR-055, no example.
2. **Validator.** `protein_boost.py`: `validate(payload, window_kcal, remaining_p, facts)` reads the `(opcional)` items of a plan of today, drops the ones that break the rules of ADR-055 § 2, caps the kcal to the room, recomputes the totals (ADR-042 path), rewrites `meal_text` and the reply line to the surviving items; when nothing survives and the boost is due, `apply()` of S24 appends one food from the table. Pure function, unit-tested on the model's shapes (item label with and without the mark, two foods, repeated food, vegetarian fact, no room, another day).
3. **Diet facts.** A permanent `preference` fact whose text states vegetarian, vegan or an excluded food feeds the validator's exclusion list; a short synonym table in code.
4. **Smoke (at most 12 model calls).** The `boost` family of the benchmark rewritten as eight evaluator cases with independent synthetic texts (ADR-033; the benchmark cases stay in `benchmark/` and are never copied into `server/evals/`): due with room, due with a food the dish has, above the floor, no room, vegetarian, `duro`, "tá bom?", another day. Run once: `--tag s34 --repeat 1`; the four remaining calls may repeat a failed case after a fix. No regression set.
5. **Dev deploy** and one named low-protein dinner on the dev app.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md) rule 5: boost as proposed by the model and validated by the server; provenance line. ADR-043 status line records the partial supersession.

## Out of scope

- Options, recipes, actions. A food database. Client changes.

## Validation

1. `pytest server/tests -q` passes (validator tests included).
2. Smoke as in 4 (≤ 12 calls); numbers in Results, variety counted (distinct foods across the due cases).
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
