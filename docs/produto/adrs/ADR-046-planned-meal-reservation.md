# ADR-046 — A plan can be reserved for its meal before it is eaten

- Status: Proposto (owner decision of 2026-10-06; the status changes with the first approval among S27, A58 and D18)
- Date: 2026-10-06
- Context: `produto`
- Supersedes: partially [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (a plan has a third outcome besides Registrar assim and expiring: reserved); partially [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) decision 3 (a reserved plan is the slot's reservation, replacing the computed one); partially [ADR-020](ADR-020-estados-novos-chat-home-horario.md) (closed screen list: `chatRL`, `homeP` new; `chatR` changed).

## Context

In the reference experience the assistant asks "Quer que eu trave a Opção A como plano e só registre quando você mandar os pesos?". Locking a plan makes it the meal of that slot before it is eaten: the rest of the day is computed with it reserved, a treat is negotiated on top of it, and the real record is compared with it ("deu 100 kcal a mais que o combinado"). In Fibrai a plan lives only until the next message: either it is recorded as eaten (**Registrar assim**) or it is nothing. The owner decided on 2026-10-06 that the option exists.

## Decision

1. **Reserve.** A plan of today for a slot of today gains the action **Reservar para o {slot}** next to **Registrar assim**. Reserving writes a `planned` state on the slot: the dish text, kcal and P/C/G of the plan, nothing eaten. A slot has at most one reservation; reserving again replaces it; a record on the slot replaces the reservation; a skip clears it; the day rollover clears it. The Home timeline shows the slot as planned (`homeP`): the dish and `planejado · {kcal} kcal`, in the muted style, never counted in the ring.
2. **Budget.** A reserved slot is reserved by its plan's kcal in `reservedUpcoming` (ADR-043), replacing the computed value of that slot; the projected-day panel of a later plan counts it. The server receives the slot as `planned` with its numbers in `DAY`.
3. **Record against the reservation.** A log into a planned slot records the log, replaces the reservation and shows on the receipt the difference to the plan: `Plano: {kcal} · Registrado: {kcal} ({+n} kcal)`. The server reply may name the difference in one clause under both tones; under `duro` it says what to change in the open slots (ADR-044).
4. **Chat state.** The plan bubble that was reserved shows the marker `Reservado para o {slot}` (`chatRL`) and keeps **Registrar assim** for the moment the meal is eaten; it never records by itself. A reservation is not a record for memory purposes: no routine is applied until the record.
5. **Nothing recorded without a tap.** Reserving and recording are explicit taps; the model never reserves.

## Motivation

- The day's remaining budget becomes the one the user actually agreed to, not a statistical guess, as soon as they decide what dinner is.
- The comparison of the record with the plan is what lets a tone hold the user to what was agreed, with numbers.

## Consequences

### Positive

- The closing lines of ADR-043 and the week closure of ADR-044 read the reservations the user made.
- One tap, no new screen: an action on the plan bubble and a state on the timeline.

### Negative

- A new slot state in Room, in the request `DAY` and in the server rules; a design plan for two golds.
- A reservation forgotten at night is cleared at rollover without a record; the day closure of ADR-044 names a planned-but-unrecorded slot as missing.

## Alternatives considered

### Treat the last plan as implicitly reserved

Rejected: the user asks for options and discards most; an implicit reservation would reserve the wrong one.

### Reserve as a temporary fact only

Rejected: a temp fact feeds the model, not the budget arithmetic or the timeline.

## Relations

- Specifications affected: [chat](../specifications/chat.md) rules 16 and 19, [home-timeline](../specifications/home-timeline.md), [Room](../../android/specifications/room-v2.md), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md) (`day.slots[].status: planned` and its numbers).
- Related ADRs: ADR-039, ADR-043, ADR-044, ADR-028.
- Consuming contexts: [design](../../design/README.md) ([D18](../../design/plans/d18-planned-meal.md)), [server](../../server/README.md) ([S27](../../server/plans/s27-planned-slot.md)), [android](../../android/README.md) ([A58](../../android/plans/a58-planned-meal-reservation.md)).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
