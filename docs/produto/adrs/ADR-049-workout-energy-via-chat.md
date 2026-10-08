# ADR-049 — Workout energy reported through the Chat

- Status: Accepted (2026-10-08, owner approval of S35 in the batch message; owner direction of 07/10/2026 in `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5)
- Date: 2026-10-08
- Context: `produto`
- Supersedes: none. Complements the constitution rule "Workout is a typed number. No number that day → credit = 0" and [ADR-028](ADR-028-registro-autonomo.md) (autonomous record with a reversible receipt). Content-policy scope: [CP10](../../content-policy/plans/completed/cp10-workout-in-scope-and-skip-boundary.md).

## Context

Today the workout energy of the day is typed in the Home dialog only. In the Chat, "treino de hoje, 450 kcal" is out of scope: the benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md)) measured the current prompt at 10 % on the workout family and the target prompt at 97–100 %. The owner's reference experience logs the workout in the same conversation as the meals and reads the day with the credit applied.

## Decision

1. **A workout report is a Chat action.** A message that states the energy of a workout done today, with a number in kcal, is in scope and produces a `workout` action `{kcal, mode}`; `mode` is `replace` (default: "treino de hoje 450 kcal") or `add` ("mais 200 kcal de treino", a second workout). A report without a number is in scope and is answered with the request for the number; nothing is inferred from duration, distance, heart rate, watches or health platforms.
2. **The app owns the day's number and the credit.** The app writes `day.workoutKcal` (replace or sum) with a receipt and Desfazer, exactly as the Home dialog does, and recomputes the credit by the eat-back rule in force. The server never returns a credit.
3. **The plan that follows uses the day as sent.** A plan in the same message is sized with the DAY the app sent; the credit appears on the next turn. The reply never promises a credit.
4. **Nothing else moves.** Training plans, exercise advice and physiology remain out of scope ([ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md)).

## Motivation

- The constitution already defines the workout as a typed number; the Chat only changes where it is typed.
- Measured: the target prompt handles the whole workout family at effort none and low.

## Consequences

### Positive

- One conversation for the day; the credit rule is unchanged and stays in the app.

### Negative

- A new action type in the contract, one more receipt, a content-policy scope change (CP10). Accepted.

## Alternatives considered

### Infer the energy from duration or distance

Rejected: the constitution forbids inferred credit, and the benchmark shows the model inventing numbers when allowed to.

### Keep the workout in the Home dialog only

Rejected: the gap is the first one the owner named in the brainstorm.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [content policy](../../content-policy/specifications/content-policy.md).
- Related ADRs: ADR-028, ADR-050.
- Consuming contexts: [content-policy](../../content-policy/README.md) (CP10), [server](../../server/README.md) (S35), [android](../../android/README.md) (A65).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
