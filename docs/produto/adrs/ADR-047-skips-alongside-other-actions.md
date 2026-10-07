# ADR-047 — Skips stated alongside other actions; a skip over a record asks to delete it

- Status: Accepted (2026-10-07, with the owner's approval of S29)
- Date: 2026-10-07
- Context: `produto`
- Supersedes: partially [ADR-028](ADR-028-registro-autonomo.md) decision 4 (a skip is no longer only a turn of its own; a skip of a slot with a record asks to delete it instead of changing nothing) and the "one automatic action per answer" rule of decision 2 (one record plus the skips of other slots); partially [ADR-020](ADR-020-estados-novos-chat-home-horario.md) (closed screen list: `chatSK`, `chatSD` new).

## Context

On 2026-10-07 a tester sent one Chat message with two actions: `Pular pré treino` followed by the breakfast they ate. The turn came back as `intent: log` with `skip_slot: null` (dev log, request `fbe15524`): breakfast was recorded and the pre-workout slot stayed empty, with no word about it in the reply or the receipts. The contract allows one `intent` per turn and a `skip_slot` only on `intent: skip` ([HTTP contract](../../api-contract.md), record rules 4 and 5), so a skip stated next to a meal, a plan or a question is dropped without a trace.

ADR-028 decision 4 also leaves a skip of a slot that already has a record as `Não registrado`: the user who says "não comi o almoço" after the almoço was recorded must find the receipt and tap Excluir.

The owner decided on 2026-10-07: skips travel as a list next to any intent; a skip over a record becomes a proposal to delete it; every skip leaves its own receipt; nothing a user states is dropped silently.

## Decision

1. **Skips as a list.** A turn of an opted-in client carries `skip_slots`: the ids of the profile slots of today that the message says did not happen today, or firmly will not (ADR-028 decision 4 and S15 keep defining what a skip is: a hedged or pending meal is never a skip). The list exists next to any intent (`log`, `plan`, `question`, `skip`), in profile order, without repeats. It is empty on refusals, fallbacks, meals of another day and held turns that skip nothing. The slot that the same turn's log estimate targets is never in it (the log wins). `intent: skip` stays for a message that only skips; older clients keep `skip_slot`.
2. **Applied by the client, one receipt each.** The client records the log first (ADR-028 rules unchanged), then each skip in list order, each with its own receipt `Pulado {slot}` and **Desfazer** (`chatSK`). An empty or planned slot is skipped at once (a reservation is cleared, ADR-046). An already skipped slot changes nothing and leaves no receipt. The slot state is read when the skip is applied, not when the message was sent.
3. **Skip over a record asks.** A skipped slot that holds a record shows, below the answer, `Pular {slot}?`, `{slot} tem {kcal} kcal registrados. O registro sai e o {slot} fica pulado.`, **Excluir e pular** (danger) | **Manter registro** (outline) (`chatSD`). Confirm removes the slot's records as Excluir does (memory of the record's active receipt reverted) and marks the slot skipped in one transaction that rechecks the slot; the receipt `Pulado {slot}` gets **Desfazer**, which brings the record back. Manter registro writes nothing and marks the card `Registro mantido`. The card expires like every pending action (next send, day change, wipe, the slot changed by another path). One pending card at a time per answer: a log confirmation (`chatU`, `chatI`) comes first, then each skip proposal in list order.
4. **Never silent.** The reply names every meal the user said was skipped, in one short neutral clause without claiming it was saved (rule 3f stays); a skip naming no profile slot says that no meal with that name exists today and that the Home card can be held to skip. When a skip cannot be applied by the client (write failure), the answer shows `Não registrado` as today.
5. **Nothing is inferred.** The model only lists skips the user stated. An empty earlier slot is never skipped because a later meal was logged.

## Motivation

- People report the day in one message ("pulei o pré, no café comi…"). Splitting it into turns is the app's job, not the user's.
- A skip over a record is almost always a correction; asking keeps it to one tap without deleting a record on a misunderstanding.
- One receipt per action keeps Desfazer exact and is what the receipts already do per slot.

## Consequences

### Positive

- The tester's message records breakfast and skips the pre-workout in one turn, with two receipts.
- "Pulei o almoço e o lanche" and "pulei o almoço, o que janto?" work.

### Negative

- A schema, contract and prompt change (S29), two golds (D19) and a client change with persisted skip outcomes and a new pending state (A59).
- An answer can now carry more than one receipt and a queue of pending cards.

## Alternatives considered

### Tell the user to send the skip on its own

Rejected as the fix: it keeps the user doing the app's work. It survives only as the reply for a skip that names no profile slot.

### Infer skips from empty earlier slots

Rejected: a late record or a meal the user forgot to log would be skipped by mistake (decision 5).

### One combined receipt

Rejected by the owner: two receipts keep the actions of each slot independent.

## Relations

- Specifications affected: [chat](../specifications/chat.md) rules 4, 5 and 7, states and acceptance criteria; [v1-chat](../../server/specifications/v1-chat.md) rules 3a, 3f, 5 and 5d; [HTTP contract](../../api-contract.md) (`skip_slots`); [Room](../../android/specifications/room-v2.md).
- Related ADRs: ADR-028, ADR-032, ADR-046, ADR-020.
- Consuming contexts: [server](../../server/README.md) ([S29](../../server/plans/completed/s29-skip-slots.md)), [design](../../design/README.md) ([D19](../../design/plans/pending_manual_validation/d19-skips-with-other-actions.md)), [android](../../android/README.md) ([A59](../../android/plans/a59-skips-with-other-actions.md)).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
