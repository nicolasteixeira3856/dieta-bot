# ADR-040 — Reference portions in the Chat instructions; open requests answered as plans

- Status: Accepted (2026-10-06, owner approval of S22 by name)
- Date: 2026-10-06
- Owner: `produto`
- Complements: [ADR-026](ADR-026-perguntas-antes-da-estimativa.md) (questions before the estimate: narrowed for a fully quantified first message), [ADR-028](ADR-028-registro-autonomo.md) (record when in doubt), [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md) (generic reference data in fixed instructions, with recorded provenance), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (cooking help). Supersedes none.
- Delivery: [S22](../../server/plans/completed/s22-open-requests-and-estimate-stability.md).

## Context

Testers reported that the same meal gets different calories and that open questions ("what should I eat tonight", "I have no idea what to have") get vague advice. A model pilot on 2026-10-06 (gpt-6-luna at `none` and `low`, grok-4.7 at `low`; evidence in the S22 plan) showed the three failure modes are independent of model and reasoning effort:

- an open request is answered with behavioural advice instead of a dish with numbers;
- a first message that already quantifies every food is recorded or questioned at random, because the instructions only define the assumed details after an answer;
- repeated estimates of the same staple foods vary by 15–25% because the model picks a different reference value per call.

The product rules are numbers first, dry tone, no coach (AGENTS). Changing the provider would cost about 125 times more per turn and 4 times the latency without fixing any of the three.

## Decision

1. **Open requests are plans with a dish.** A request about what to eat, order or make, how to organise a meal out, or a statement of having no idea what to eat, is a plan. The reply names a concrete dish or order with grams per item and the `kcal · P · C · G` total. It never gives behavioural advice and never defers to an external source. This is the existing plan rule of ADR-039 applied to open requests.
2. **A fully quantified first message is estimated, not questioned.** When every food in the message has a usable amount, cooking fat, milk type, sugar and usual coffee or tea amounts are assumed with their common value, declared in one line, and never asked. A MEMORY fact still wins over the common value. The estimate is released with the record mark the RECORD rule gives it. This narrows ADR-026 for that situation and follows ADR-028: record when in doubt, and let the receipt be corrected.
3. **Reference portions in the fixed instructions.** The Chat instructions carry a short table of staple foods of the Brazilian diet with one reference portion each and its energy and macros, taken from the Tabela Brasileira de Composição de Alimentos (TACO, 4th edition, NEPA/UNICAMP) and rounded. When a listed food appears with an amount, its values are scaled from the table; the model deviates only for a stated preparation or brand and says so. The table is generic data, not a person's, so it does not cross the per-request boundary of ADR-033 § 3; its edition and rows are recorded as provenance under ADR-033 § 4 whenever the table changes.
4. **Model and effort unchanged.** `gpt-6-luna`, `reasoning.effort=none`, as in AGENTS § LLM and the Chat specification. A future provider change needs its own ADR.

## Motivation and consequences

- The user gets a number in every open answer and a record in every quantified report; the receipt, with Desfazer and Editar, absorbs a wrong assumption cheaper than a question does.
- The reference table bounds the variance of the most frequent foods; foods outside it keep today's behaviour. The table adds about 1,200 characters per prompt branch, cached after the first call.
- Fixed instructions now carry nutrition data. It is generic and sourced; it must never be extended with values taken from a user's label, log or memory, which stay per-request (ADR-033).
- A stored memory fact keeps precedence over the common value and over the table.

## Alternatives considered

- Switch to Grok 4.7: rejected; same variance, mandatory reasoning, p95 of 31 s, 125× the cost.
- Raise `reasoning.effort` to `low` on gpt-6-luna: rejected for this purpose; it stabilised the record mark on one case but widened the kcal band and did not change the tone of open answers.
- Compute energy on the server from a food database instead of the model: not now; it needs a food-matching layer and a schema change, and the table in the instructions tests the hypothesis first. Reconsider if the spread stays above the S22 target.
- Keep asking on a quantified first message: rejected; it contradicts ADR-028 and is what testers perceive as random.

## Relationships and rollout

S22 delivers the instruction changes and the evaluation; the Chat specification is rewritten at its Completion. Accepted ADR bodies remain immutable; only status lines of ADR-026 and ADR-033 record the complement.
