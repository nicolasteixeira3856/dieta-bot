# ADR-042 — The estimate total is server arithmetic over the items

- Status: Accepted (2026-10-06, owner approval of S23 by name)
- Date: 2026-10-06
- Owner: `server`
- Complements: [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md) (budget arithmetic on the server), [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md) (meal-change deltas validated and consolidated by the server), [ADR-041](../../produto/adrs/ADR-041-reference-portions-in-chat-instructions.md) (reference portions). Supersedes none.
- Delivery: [S23](../plans/completed/s23-estimate-total-from-items.md).

## Context

S22 anchored the per-item energy of staple foods with a reference table: the items of a repeated breakfast became identical across repetitions. The total did not. The model writes a `kcal` that is not the sum of the items it just listed (460 against 589), and the spread of the total stayed between 19% and 34%. An instruction to add the items had no effect. The same failure breaks the evaluator checks that require total = sum of items.

The server already owns two pieces of arithmetic for the same reason: the budget check and the adjusted plan (ADR-039), and the consolidated total of a meal-change `add` (ADR-032). Both have been stable since delivery.

## Decision

1. **The server computes the total.** Whenever an estimate carries items with energy, `kcal` is the sum of the item kcal, each rounded once to a whole number. The model's total is replaced, not validated.
2. **Macros follow proportionally.** `p`, `c` and `g` are scaled by the ratio between the server total and the model total and rounded once. Energy is never forced to equal 4P + 4C + 9G.
3. **Everything downstream uses the server total:** the budget check and adjustment of ADR-039, the record gate, the receipt card, the dev log. The reply's stated total is rewritten to the server total when it quoted the model's.
4. **The model keeps the estimation.** Foods, grams and per-item energy remain the model's, under the reference portions of ADR-041 for listed foods. The server adds; it does not look foods up.
5. **Meal changes** `new` and `revise` recompute instead of failing on an energy mismatch; `add` keeps its delta rule. Invalid items still fail.

## Motivation and consequences

- A repeated meal gets a repeated total; the variance that remains is the model's choice of foods and grams, which the user can see and correct on the receipt.
- The evaluator's item-sum checks verify a server invariant and stop flaking.
- The reply may show a different total from the one the model intended; the rewrite keeps the text and the card consistent for the common form `N kcal`.
- An estimate without items, or with items without energy, is unchanged: the server never invents items.

## Alternatives considered

- Keep asking the model to add correctly: rejected; measured in S22 with no effect.
- Fail the turn on a mismatch, as meal-change `new`/`revise` do today: rejected for the common path; it turns a stable item list into a fallback question.
- Per-item macros and a server food database: deferred; the item shape and the client would change, and the total alone removes the measured variance.
- Lower sampling temperature or a higher reasoning effort: rejected; `low` widened the band in S22 and the API exposes no temperature for this model.

## Relationships and rollout

S23 delivers the arithmetic, the call sites and the evaluation; the Chat specification and the HTTP contract are rewritten at its Completion. Accepted ADR bodies remain immutable; only the status lines of ADR-032 and ADR-041 record the complement.
