# ADR-055 — Protein boost of a named dish: the model proposes, the server validates

- Status: Proposed (owner decision of 08/10/2026 after the benchmark: "o reforço de proteína pode utilizar um modelo híbrido para testarmos"; accepted with the approval of S34)
- Date: 2026-10-08
- Context: `produto`
- Supersedes: partially [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) decision 4 as delivered by S24 (the server chose the foods from a fixed table of three). The floor (30 % of the remaining protein), the room (at least 60 kcal), the `(opcional)` mark and the limit of two foods survive.

## Context

S24 moved the boost to server code because at effort none the model ignored the rule; the server adds shredded chicken, boiled egg or lean ground beef from a fixed table. The owner wants the dish-appropriate variety a model can give ("o modelo é para ser mais criativo"). The benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md), family `boost`) measured the rule in the prompt: at effort none the model did no due boost and added foods when not due; at effort low it got 7 of 9 due plans right but used chicken in 4 of 5 and missed the vegetarian user in 2 of 3. Neither pure option is good: code has no variety, the model alone is not reliable.

## Decision

1. **The model proposes.** The PROTEIN BOOST rule of the target prompt (`benchmark/prompts/new_instructions.py`) asks the model to add one or two `(opcional)` protein foods, with grams and kcal as their own items, chosen to suit the dish and varied, never a food the dish has, never against a diet fact; one closing line in the reply.
2. **The server validates and completes.** `server/protein_boost.py` becomes a validator over the model's items marked `(opcional)`: it drops an item that breaks the rules (dish at or above the floor, room under 60 kcal, a third food, a food the dish already has, meat or fish against a vegetarian or exclusion fact, grams above 150 or not in kitchen steps, a plan for another day), caps the added kcal to the room, and when the model proposed nothing although the boost is due, appends one food from its table as today. Items, totals, `meal_text` and the reply line are rewritten to what survives.
3. **Diet facts are the source of exclusions.** A permanent fact of category `preference` that states vegetarian, vegan or an excluded food governs the validator; the model sees the same fact in MEMORY.
4. **Acceptance is measured.** The `boost` family of the benchmark (eight cases, three repetitions) is the acceptance test: required boosts present with the floor reached when the room allows, no boost when not due, no excluded food, variety across answers.

## Motivation

- The model gives the variety and the fit to the dish; the server guarantees the rule. Each does what the benchmark shows it does well.

## Consequences

### Positive

- A sandwich gets an egg or tuna, a soup gets chicken, a vegetarian gets tofu or chickpea, and the floor still holds.

### Negative

- Two sources of the same item list; the validator must be deterministic and tested on the model's shapes. When the model and the server disagree, the user sees the server's version. Accepted for the test; the ADR is revisited with the family's numbers after S34.

## Alternatives considered

### Model only

Rejected: measured at 0 of 10 at none and with excluded foods at low.

### Server only (as delivered)

Rejected by the owner: no variety; the fixed table repeats chicken.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md).
- Related ADRs: ADR-039, ADR-042, ADR-043, [ADR-054](../../server/adrs/ADR-054-chat-reasoning-effort-low.md) (the rule in the prompt depends on effort low).
- Consuming contexts: [server](../../server/README.md) (S34).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
