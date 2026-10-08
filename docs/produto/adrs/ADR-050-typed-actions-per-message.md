# ADR-050 — Typed actions per Chat message

- Status: Proposed (owner direction of 07/10/2026 in `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5; accepted with the approval of S36)
- Date: 2026-10-08
- Context: `produto`
- Supersedes: partially [ADR-028](ADR-028-registro-autonomo.md) (one record per turn becomes one record per `log` action), [ADR-032](ADR-032-acrescimos-e-correcoes-de-refeicoes.md) (a meal change is a field of a `log` action) and [ADR-047](ADR-047-skips-alongside-other-actions.md) (skips become `skip` actions in the same ordered list). The receipts, Desfazer, the delete proposal over a record and the order "logs and plans first, skips last" of those ADRs survive.

## Context

A message that reports the whole day ("café X, almoço Y, lanche Z"), a meal plus a plan, or a workout plus a meal, is answered today with one `intent` and one `estimate`: the server merges or drops meals. The benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md)) measured the current prompt at 17 % on the multi-action family and the target prompt at 70–78 %, with one receipt per action.

## Decision

1. **`actions[]` replaces `intent` + `estimate` + `skip_slots`.** A Chat answer carries an ordered list of one to six actions `{id, type, slot, estimate, record_intent, meal_day, meal_change, workout, recipe_id, options}`; `type` is `log`, `plan`, `skip`, `workout`, `recipe_recall` or `question`. One action per thing the message does: each meal eaten is its own `log`, each meal skipped its own `skip`, a dish request a `plan`, a workout energy a `workout`, a saved-recipe request a `recipe_recall`; anything with nothing to estimate is one `question`.
2. **Order.** Logs, plans, workouts and recalls in the order the user stated them, then every skip in profile order (ADR-047). A slot both eaten and skipped in the same message is a log. More than six things: the first six, and the reply says which were left out.
3. **Dependency inside the message.** A plan stated after a log is sized with that log already eaten. A `question` never appears next to another action: a clarification about one meal goes into that action's `estimate.question` and the other actions are released.
4. **The app applies the batch in one transaction** with the record guard, one receipt per action in the thread (ADR-048), and Desfazer of the whole batch. Skips over a record keep the delete proposal of ADR-047.
5. **Shaping per action.** The server arithmetic of ADR-042 (total from items), ADR-032 (add/revise), ADR-043 (window, closing lines) and the protein boost apply to each action as they apply to a single estimate today.

## Motivation

- Measured gain on the owner's own messages: the multi family goes from 17 % to 70–78 %; the whole-day message and "jantei X, me sugere o lanche" are answered as the owner expects.
- The schema stops the merge of two meals into one estimate, which the testers saw as a "1074 kcal lunch".

## Consequences

### Positive

- One receipt per thing done; Desfazer of the batch; the same rules per action.

### Negative

- Contract and schema change with capability flag; the app persists `actions` (Room migration); the server evaluator and its cases move to the actions shape. Benchmark findings carried as rules: the six-action limit and the question-next-to-action rule failed at effort none and passed at low.

## Alternatives considered

### Keep one estimate and ask the user to send one meal per message

Rejected: the owner's reference experience accepts the whole day in one message and the testers write that way.

### Several server calls, one per detected meal

Rejected: the split would be the model's job anyway, at the cost of N calls.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [Room](../../android/specifications/room-v2.md).
- Related ADRs: ADR-028, ADR-032, ADR-042, ADR-043, ADR-047, ADR-048, ADR-049.
- Consuming contexts: [server](../../server/README.md) (S36), [android](../../android/README.md) (A66).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
