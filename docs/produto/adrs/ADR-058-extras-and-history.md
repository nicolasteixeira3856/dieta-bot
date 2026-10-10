# ADR-058 — Extras outside the meals; the last 30 days on the Home; recording another day

- Status: Accepted (2026-10-09, owner approval of [D28](../../design/plans/pending_manual_validation/d28-home-extras-and-history.md) in the batch message; owner direction of 09/10/2026 in the brainstorm session)
- Date: 2026-10-09
- Context: `produto`
- Supersedes: partially [ADR-028](ADR-028-registro-autonomo.md) (decision 7: the Chat records only today's meals; from this ADR a named past day inside the last 30 days is recorded too); partially [ADR-017](ADR-017-registro-consolidado.md) (one record per meal stays for the slots; an extra is one record per item and is never consolidated); partially [ADR-021](ADR-021-refeicoes-por-dia.md) (rule 7: logs without a slot of the day are "Outros" only when they belong to a removed or foreign slot; a log recorded as an extra has its own kind). Reactivates [A35](../../android/plans/out_of_scope/a35-registro-retroativo.md) inside this decision.

## Context

Two gaps reported by the owner as a daily user, 09/10/2026. First, something consumed between meals (an energy drink after lunch, a coffee after the last meal) has no place: it is not a meal, it must not replace the afternoon snack, and today the Chat only offers the slots of the day through the Trocar sheet. Second, the user cannot see what was recorded on an earlier day and cannot ask Tali to record something in an earlier day; [A35](../../android/plans/out_of_scope/a35-registro-retroativo.md) deferred the retroactive record on 01/10/2026 and the Home shows only today, with yesterday's closure card as the only window into the past. The server already classifies the eating day (`meal_day: today | other`, S14) and the app already sends the last 7 days (`recent`, `recent_days`). Room keeps every `meal_log` row with its date.

## Decision

1. **Extra.** A record can be an extra: a `meal_log` row with `slotId = null` and `kind = extra`, its own time (the time it was recorded, or the time the user states) and the usual text and numbers. Extras come from the Chat: the server returns `slot: "extra"` on a `log` action when the message says so (`fora da refeição`, `entre o almoço e o lanche`, `um extra`, `só um café agora`, a named drink or snack outside any meal), and the Trocar sheet gains the entry `Extra · fora das refeições` for any record. An extra counts in the ring, in the macros, in the day balance, in `recent` and in `recent_days`; it never fills a slot, never counts in a meal window, never reserves anything and never becomes a routine or a liked dish. Several extras a day are several rows; one per item, never consolidated. The receipt reads `Registrado como extra · {hora}` with the usual actions (Desfazer, Excluir, Editar, Trocar refeição, which can move an extra into a slot or a slot record into an extra).
2. **Timeline by time.** The Home timeline orders the slots of the day and the extras by time: an extra is a node of its own between the meals it fell between, labelled `Extra · {HH:mm}` with its text and numbers, in the regular node style, no number. The block `Outros` at the end keeps only the logs of removed or foreign slots ([ADR-021](ADR-021-refeicoes-por-dia.md) rule 7).
3. **Day strip.** Above the ring the Home shows a horizontal strip of the last 30 days: one circle per day with the day number, the month written above the strip where it changes, today at the right end and selected, the strip scrollable to the left; no day before the first day of the app. Selected and unselected states follow the tokens and WCAG contrast, in the Aero style of the gold.
4. **A past day on the Home.** Selecting a day shows that day's ring, macros, workout line, closure card (when it exists) and timeline, with that day's effective ceiling and credit, read from Room. Cards of a past day do not react to tap or long press; the reminder gestures, the reservation and the `O de sempre` card belong to today only. The FAB stays and opens the Chat as today, without a day selected: the Chat is always today's. The Config icon stays. Rolling back to today is a tap on today's circle; the day rollover (00:00 São Paulo) returns to today.
5. **Recording another day from the Chat.** A message that names a day inside the last 30 days (`ontem`, `anteontem`, `segunda`, `dia 3`, `na janta de ontem`) records in that day: the server returns `meal_day: other` with the resolved ISO `day`, the app records in that date with the slot of that day's profile group (or as an extra) and the receipt says `Registrado em {slot} · {d de mês}`; the usual receipt actions apply. The record changes that day's totals, `recent`, `recent_days` and the numbers of that day's closure card (from Room, the text is not regenerated); it never changes today's budget. A day older than 30 days, or a day before the first day of the app, is answered with `Só registro os últimos 30 dias.` and nothing is written. Ambiguity (two possible days, no slot) is a question, never a guess.
6. **Memory.** A record in a past day counts for the routine rules with that day's date (a day seen); it never creates a temporary fact and never triggers a `liked` fact.

## Rationale

The extra is the cheapest honest model: a row without a slot already exists in Room, the Home already draws it, and the server only needs a target it can name. Ordering the timeline by time puts the extra where it happened without inventing a slot. Thirty days is enough to look back and correct, keeps the strip light and bounds the retroactive record so the server never reasons about an arbitrary past. Keeping the Chat always on today avoids a second mode in the composer: the day is said, not selected.

## Consequences

### Positive

- The owner's case (an energy drink between lunch and snack) is recorded in one message and shown at the right time of the day.
- The user sees any of the last 30 days and can fix a forgotten dinner the morning after.
- No new table: `meal_log` gains a `kind` and a time; closures already exist per day.

### Negative

- The Home gold changes (`home1` redrawn with the strip and an extra node) and a past-day gold is born; the Trocar sheet and the receipt change.
- The server prompt gains a day resolver and the extra target; the eval cases for `meal_day` grow.
- A past-day record can move a routine's `days_seen`; accepted, it is the truth of that day.

## Alternatives considered

### Numbered extras (`Extra 1`, `Extra 2`)

Proposed by the owner and withdrawn on 09/10/2026: the time says more than a counter.

### Selecting the day in the Chat composer

A second mode in the Chat, a new state to design and to carry in the prompt. Rejected: the day is named in the message.

### History without the retroactive record

Half the feature; the owner asked for both on 09/10/2026.

## Relations

- Specifications affected: [home-timeline](../specifications/home-timeline.md) (strip, past day, timeline by time, extra node), [chat](../specifications/chat.md) (extra target, other-day record, receipts), [memoria-push](../specifications/memoria-push.md) (past-day memory rule), [v1-chat](../../server/specifications/v1-chat.md) and the [HTTP contract](../../api-contract.md) (`slot: "extra"`, `meal_day` with `day`), [Room](../../android/specifications/room-v2.md), [gold inventory](../../qa/README.md).
- Related ADRs: [ADR-017](ADR-017-registro-consolidado.md), [ADR-021](ADR-021-refeicoes-por-dia.md), [ADR-028](ADR-028-registro-autonomo.md), [ADR-040](ADR-040-home-card-gestures-app-reset.md), [ADR-044](ADR-044-assistant-tone-and-closures.md), [ADR-050](ADR-050-typed-actions-per-message.md).
- Consuming contexts: [design](../../design/README.md) (D28), [server](../../server/README.md) (S42), [android](../../android/README.md) (A72).

Once accepted, the body of this ADR is not edited. Only the `- Status:` line changes, to record a total or partial supersession by a newer ADR.
