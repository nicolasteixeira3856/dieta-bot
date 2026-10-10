# Plan — A72 Extras, the 30-day strip and a record in a past day

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Room v17 (`meal_log.kind`, `meal_log.time`), the Home day strip and the past-day state, the timeline ordered by time with extra nodes, the Trocar sheet `Extra` entry, the extra and other-day receipts, `commitRecord` for a past date, the `extras` and `first_day` request fields, `recent`/`recent_days` with extras, the fake server fixtures, tests, captures. Documentation at Completion: [home-timeline](../../produto/specifications/home-timeline.md), [chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md), [Room](../specifications/room-v2.md), [gold inventory](../../qa/README.md).
- Prerequisites: [D28](../../design/plans/pending_manual_validation/d28-home-extras-and-history.md) `Concluído`; [S42](../../server/plans/s42-extras-and-other-day.md) on the dev server; [A71](a71-conversational-onboarding.md) merged (Room version order).
- Related documentation: [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-040](../../produto/adrs/ADR-040-home-card-gestures-app-reset.md), [ADR-050](../../produto/adrs/ADR-050-typed-actions-per-message.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a72-extras-and-history.md. Implemente o plano aprovado.`

## Objective

The user records something eaten outside the meals and sees it at its time on the Home; sees any of the last 30 days; and records a forgotten meal in a named past day from the Chat ([ADR-058](../../produto/adrs/ADR-058-extras-and-history.md)).

## Scope

1. **Room v17.** `meal_log.kind TEXT NOT NULL DEFAULT 'slot'` (`slot` | `extra`) and nullable `meal_log.time` (`HH:mm`, São Paulo). Migration adds the two columns; old rows are `slot`. `DayRepository.recentLogs` and the `recent` builder send `slot_id: "extra"` with `time`; `recent_days` totals include extras. `SlotState` is unchanged; extras are a list of their own in `DaySnapshot`.
2. **Extra from the Chat.** A `log` action with `slot: "extra"` records an extra (`commitRecord` with `kind = extra`, `time` = the action's or now) in the auto and ask paths; the receipt reads `Registrado como extra · {hora}` (`chatGX`) with Desfazer, Excluir, Editar and Trocar refeição; the Trocar sheet gains `Extra · fora das refeições` at the end (a slot record moved there becomes an extra and vice versa, one transaction, `Movido` receipt). The request carries `extras: true`. An extra never reserves, never fills a slot, never feeds the routine or `liked` rules, never counts in a meal window; it counts in the ring, macros and balance.
3. **Timeline by time (`home1`).** The day's slots and extras are merged and sorted by time (a slot by its profile time, an extra by `time`); an extra draws the `extra` node and card `Extra · {HH:mm}`; `Outros` keeps only the logs of removed or foreign slots. The over-ceiling `status/bad` rule walks the merged list.
4. **Day strip (`home1`, `homeE`).** `Home/DayStrip` above the ring: the last 30 days (or since the first day of the app, `profile.firstDay`, whichever is shorter), today at the right and selected, month label where it changes, `LazyRow` scrolled to the end on open; a circle per state (`today-selected`, `past-selected`, `past`, `no-record`) with the contrast measured in D28; accessibility label `{d} de {mês}, {kcal} kcal`.
5. **Past day (`homeH`).** Selecting a day switches `observeDay(date)`: ring, macros, workout line (read only), closure card of that day when it exists, timeline of that day with that day's profile group; cards without tap or long press; no `O de sempre` card; FAB and Config as today; the day rollover and `wipeToday` return to today. `screen_view` gains `home_past`.
6. **Other-day record.** A `log` action with `meal_day: other` and `day` inside the last 30 days records in that date: `commitRecord(date)` with the `RecordGuard` for that date (no `wiped` after the action for that date, slot of that day's group), receipt `Registrado em {slot} · {d de mês}` (or `como extra`), the usual actions; the closure row of that date is updated in `numbers` from Room (text untouched); today's budget unchanged; the routine rules count that date as a day seen, no temporary fact, no `liked`. A `day` outside the bound never reaches the app (the server answers the fixed line); a client-side guard drops it anyway.
7. **Fake server fixtures.** The capture scripts answer the extra and the other-day cases from the `nicolas` persona of [S40](../../server/plans/completed/s40-eval-personas.md).
8. **Telemetry.** `meal_saved` gains `kind` (`slot` | `extra`) and `day` (`today` | `other`); `home_day_selected` (`offset` bucket: `1`, `2-7`, `8-30`); enums only.

## Out of scope

- The server (S42). The golds (D28). The onboarding (A71).
- Editing a past day from the Home. Reservations, skips by long press or the routine card on a past day. The closure text of a past day.
- Days beyond 30 or before the first day of the app.

## Validation

1. `./gradlew :app:testDevDebugUnitTest` (`MigrationV16V17Test`, merged timeline order, strip day list and month boundaries, `commitRecord` on a past date with the guard, extras excluded from windows and routines, `recent` with extras, `GoldTest` for `home1`, `homeH`, `homeE`, `chatGX`) and `./gradlew :app:verifyRoborazziDevDebug` pass.
2. Emulator, both themes, partial rule: Home (`home1`, `homeH`, `homeE`) and the extra receipt and Trocar (`chatGX`) against the fake server, compared with `node tools/diff-gold.mjs`; `homeW`, `homeC`, `homeK`, `homeP` captured once because the strip sits above them. The onboarding and the other Chat states are not captured.
3. On the dev server with the dev build: one extra ("um Monster branco agora") and one other-day record ("ontem jantei 2 fatias de pizza"), then the Home on yesterday. Cap: **2 model calls** (within the S42 cap, no separate run).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
