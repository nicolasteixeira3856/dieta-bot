# Plan — A60 Budget choice, tone and closures, rich replies, planned meal and skips (app)

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only, in five parts delivered in order on one branch: (A) the over-budget choice in the Chat; (B) tone choice at onboarding and in Config, day and week closures; (C) the reply formatting subset rendered in the bubbles; (D) reserving a plan for its meal; (E) skips next to other actions. Plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-*.sh`. One Room version for the whole plan: every column and table of the five parts goes into one migration decided at the start of part A (non-destructive, exported schema, migration test), so later parts add no version.
- Related documentation: [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md), [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md), [ADR-047](../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [product Chat](../../produto/specifications/chat.md), [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [memoria-push](../../produto/specifications/memoria-push.md), [home-timeline](../../produto/specifications/home-timeline.md), [Room](../specifications/room-v2.md), [HTTP contract](../../api-contract.md), [autonomous run](../../sdd/autonomous-run.md).
- Prerequisites:
  - golds exported: D12 (`chatRB`), D16 (`o5`, `cfgT`, `homeC`, `homeK`, `cfg`), D17 (`chatR`, `chatE`, `chatRK`), D18 (`chatR`, `chatRL`, `homeP`), D19 (`chatSK`, `chatSD`), all `Concluído` in [design history](../../design/plans/completed/);
  - server: S21 and S29 delivered and deployed (they are); [S30](../../server/plans/s30-tone-formatting-planned-slot.md) delivered and deployed to the dev server with the parts the client parts need (B needs S30 part A, C needs part B, D needs part C); a server part left out skips the matching client part;
  - [A54](completed/a54-auto-record-addition-empty-slot.md) delivered (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a60-tone-formatting-planned-skips.md. Implemente o plano aprovado.`

## Objective

Everything the Chat and the Home need for the tone, the closures, the formatted replies, the planned meal and the skips, in one plan and one dev build: the owner wakes up with the app that shows bold numbers, lists, the day summary and the hard tone when chosen. Supersedes the cancelled plans A50, A55, A57 and A58 ([`cancelled/`](cancelled/)), whose scope it carries unchanged; the skips scope was delivered by [A59](pending_manual_validation/a59-skips-with-other-actions.md) and stays here only as part E's record.

## Delivery

- One branch from master; parts in the order A → B → C → D → E; one commit or more per part; one PR at the end. Part E is delivered by [A59](pending_manual_validation/a59-skips-with-other-actions.md) (owner decision, 07/10/2026: A59 merged as implemented, Room v11 `skipOutcomes`); A60 runs parts A–D.
- A part whose validation fails twice is left out: the PR merges with the delivered parts, the plan stays `Em implementação`, Results list the missing part and what it needs.
- One dev build at the end with `tools/distribute-dev.ps1 -Notes` (pt-BR notes per part delivered). Visual QA per part against its golds, partial validation (only the flows the part touches), fresh captures in `docs/qa/android/current/{dark,light}/`.
- Room: one version (the next free one, 12 after A59's v11), one migration, one migration test, written in part A with every field parts A–D declare below; a part left out leaves its unused columns in place (nullable, documented in the Room specification at Completion).

## Scope

### Part A — Plan over budget: the choice in the Chat (from A50)

#### 1. Capability and data

- Send `plan_budget: true` on every normal Chat turn (with `clarify_rounds` and `auto_record`, as today). Decode the response `plan_budget` `{limit_kcal, over_kcal, reserved, choice}` with nullable defaults; an older server or a malformed object means no choice UI.
- Store it on the assistant row: a nullable JSON column on `chat_message` with the next Room version discovered at start, plus the local choice (`over_ok`) once **Pode passar** is tapped. Non-destructive migration, exported schema, migration test. Existing rows stay null.

#### 2. When the choice shows

Only on the most recent plan bubble of today, when `over_kcal` > 0, `limit_kcal` ≥ 1 and the stored choice is not `over_ok`. Then, below the bubble (`chatRB`):

- `Passa {over_kcal} kcal do que sobra.`; for each reservation, `Reservei {kcal} kcal para {label}.`
- Pills **Pode passar** · **Ajustar para caber** in the action position, in place of **Registrar assim**.

Otherwise the plan is `chatR` as today, including the projected day in `bad` when over. With `limit_kcal` < 1 there is nothing to adjust to: no line, no pills, `chatR`. A new send, a day change or a wipe ends the choice, like **Forçar estimativa**.

#### 3. Actions

- **Pode passar:** no request. Store `over_ok` on the row; the pills leave and **Registrar assim** returns. Survives recreation.
- **Ajustar para caber:** sends `Ajusta para caber em {limit_kcal} kcal.` with `fit_kcal = limit_kcal` through the normal send/loading/failure/retry flow; retry keeps `fit_kcal`. The answer is a normal plan: `chatR` when it fits, the choice again when the server returns it still over.
- A typed answer needs nothing special: the server's `choice` drives the next response.
- **Registrar assim** keeps its rule-16 behavior once shown; recording an over-budget plan after **Pode passar** is allowed.

#### 4. Telemetry

One event with enums and numbers only: the choice (`over_ok` | `fit`) and `over_kcal`. No text.

#### 5. QA tooling

`tools/fake-chat-server.mjs` gains a `{"plan_budget": ...}` switch returning fits, over with a reservation, over still after fit, and `limit_kcal` ≤ 0. `tools/capture-chat.sh` gains `SCENES=a50`: onboarding, an over-budget plan (`chatRB`), **Pode passar**, **Ajustar** to an adjusted plan, and recreation with a pending choice.

#### Intended specification changes

At Completion: product Chat rule 16 gains the choice (when it shows, copy, actions, expiry) and `chatRB` in the state list; Room gains the column and version; Provenance links. The gold inventory stays owned by [qa](../../qa/README.md).

### Part B — Tone choice, day closure and week closure (from A55)

#### 1. Tone in the profile

- Room: `profile.tone` text column, default `seco`, non-destructive migration to the next version discovered at start, exported schema, migration test. Existing profiles read back as `seco`.
- `ChatIn.profile.tone` on every normal and compact request.
- O5 (`o5`), after O4: title `Como a Tali fala com você`, two full-width options with one line each: **Seco** — `Só os números. Sem opinião.` (preselected) and **Duro** — `Cobra o que estourou e o que faltou. Sem rodeio.`; footer `Dá para mudar nas configurações.`; CTA **Concluir e começar** moves from O4 to O5; `onboardingDone` only at the end of O5. Back returns to O4.
- Config (`cfgT`): row `Tom da Tali` with the current value in the profile block; tap opens a sheet with the same two options and the `Salvar` / `Cancelar` pair of the other sheets. No wipe, no confirmation: the next turn uses the new tone.

#### 2. Day closure

- Scheduler: an exact alarm (inexact without the permission, like the meal push) at 22:00 America/Sao_Paulo every day; rescheduled at 00:05, on boot, at start and on edit, in the same flow as the meal push. A closure already produced for a date is not produced again (Room row keyed by date).
- At the alarm: the app computes the numbers from Room (eaten kcal and P/C/G, targets, effective ceiling, workout, per-slot status and kcal), calls `POST /v1/close` with `period: day` and the tone, stores `{date, period, numbers, text}` in a new `closure` table, posts the notification `Fechamento do dia` with the first line of the text as content (app-controlled: small icon, title, text, accent color; the rest is the system template), and the Home shows the card `homeC` at the top of the timeline for that date: `Fechamento de {d} de {mês}`, `{kcal} de {teto} kcal`, `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}`, the missing or skipped meals as one line, workout if any, and the text. A later record of the day updates the numbers of the card from Room, not the text. Without network: the card shows the numbers and `Sem o texto: sem rede.`; the next app start retries the text once.
- Rollover: the card of yesterday stays visible until the first record of the new day, then collapses to one line `Ontem: {kcal} de {teto} kcal`; tap expands.
- Dev-only trigger ([ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md)): the broadcast `app.fibrai.android.dev.RUN_CLOSURE` with the extra `period` (`day` | `week`) runs the closure now, as the alarm would, in the dev flavor only; it is how the validation and the testers exercise the 22:00 path without waiting for it.

#### 3. Week closure

- Sunday 22:00, same scheduler: numbers of the week from Room (per day totals and whether anything was recorded; the slot that most often went over its window, computed as kcal above `ceiling / slots of that day` for days without the S24 window); `POST /v1/close` with `period: week`; `closure` row keyed by the ISO week; notification `Fechamento da semana`; Home card `homeK` above the day card: `Semana de {d} a {d}`, total and mean kcal and protein, `Dias sem registro: n`, the slot line, the text. Stays visible until Tuesday 00:00, then collapses like the day card.

#### 4. Telemetry

- Events with enums and numbers only: `tone_set` (`seco` | `duro`, from `onboarding` | `config`), `closure` (`day` | `week`, `text` | `fallback` | `offline`), `closure_opened`. The tone goes to the server in the request body only; it is not a Crashlytics key.

#### 5. QA tooling

- `tools/fake-chat-server.mjs` gains `/v1/close` with canned `seco` and `duro` texts and a failure switch; `tools/capture-*.sh` gains `SCENES=a55`: O5 both options, Config row and sheet, the day card with text, offline and collapsed, the week card.

#### Intended specification changes

At Completion: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) (5 screens, O5 rules, `tone` in the prompt prefix, CTA move), [memoria-push](../../produto/specifications/memoria-push.md) (Config tone row and sheet; closure alarms and notifications under the push rules), [home-timeline](../../produto/specifications/home-timeline.md) (`homeC`, `homeK`, collapse rules), [Room](../specifications/room-v2.md) (`profile.tone`, `closure` table, version); gold inventory stays owned by [qa](../../qa/README.md); Provenance lines; ADR-044 status to Accepted (if not already).

### Part C — Rendering the reply subset in the bubbles (from A57)

#### 1. Parser (`domain/ReplyMarkup`)

- Pure Kotlin: `reply` → a list of blocks (paragraph with bold spans, bullet item, step item, table of rows). Only the ADR-045 subset is recognised; anything else is literal text. Unbalanced `**`, a table with a bad row or a second table fall back to literal lines. Bounded input (the server reply limit); no recursion.
- `plain(reply)`: the text without markers, used wherever the app keeps or sends text (receipt text of rule 12 when the reply is the source, the history line of rule 8, the `Editar` composer, telemetry lengths). Room never stores markers in record text; the assistant row keeps the raw reply for display.

#### 2. Rendering (Aero)

- `AeroRichText`: paragraphs with bold spans in the strong body style; `AeroList` rows for bullets and steps with the design-system list spacing and marker column; `AeroPortionsTable` for the two-column table inside the bubble width. Tokens from `docs/tokens.md`; no Material. The macro colours of rule 16 keep applying to the P/C/G line over the bold.
- Used by the plan bubble (`chatR`, `chatRK`), the log estimate bubble (`chatE`) and the plain assistant bubble; question bubbles, refusals, receipts and cards unchanged.

#### 3. QA tooling

- `tools/fake-chat-server.mjs` gains a `{"format": ...}` switch returning a two-option plan, a recipe with a table and steps, a log with bold, and a malformed markup sample; `tools/capture-chat.sh` gains `SCENES=a57`.

#### 4. Telemetry

- None new. `chat_result` unchanged; lengths measured on `plain(reply)`.

#### Intended specification changes

At Completion: [product Chat](../../produto/specifications/chat.md) rule 2 (what a bubble renders), rule 8 (history line without markers), rule 12 (record text without markers), rule 16 (`chatRK`, bold over the macro colours); state list gains `chatRK`; Provenance line; ADR-045 status to Accepted (if not already).

### Part D — Reserve a plan for its meal (from A58)

#### 1. Room and budget

- `planned_meal` table (or a nullable column family on the slot state, decided at start from the current schema): date, slot id, text, kcal, P/C/G, source message id. One row per date and slot; non-destructive migration, exported schema, migration test.
- `BudgetCalculator`: a planned slot contributes to `reservedUpcoming` of the projected-day panel (chat rule 16) and to nothing eaten. The Home ring ignores it.
- Rollover (00:00 SP), wipe and app reset clear reservations of the day; a record or a skip on the slot replaces or clears its reservation.

#### 2. Chat

- Plan bubble of today with a suggested slot of today: pill **Reservar para o {slot}** next to **Registrar assim** (`chatR`). Tap writes the reservation in one transaction that rechecks the day and the slot, marks the bubble `Reservado para o {slot}` (`chatRL`) and keeps **Registrar assim**. Reserving again from another plan replaces; the earlier bubble loses its marker.
- `ChatDaySlot` sends `status: planned` with the numbers on every normal turn.
- A record into a planned slot (any path of rules 5, 6, 16 or 22) replaces the reservation and the receipt gains the line `Plano: {kcal} · Registrado: {kcal} ({+n} kcal)`; Desfazer restores the reservation when the record is undone.

#### 3. Home

- Timeline slot in the planned state (`homeP`): dish text, `planejado · {kcal} kcal`, muted; tap opens the Chat like an empty slot; long press skips and clears the reservation after the usual confirmation.

#### 4. Telemetry

- `plan_reserved` (`reserved` | `replaced` | `cleared_by_record` | `cleared_by_skip`), numbers only; `meal_saved` gains `had_plan: bool`.

#### 5. QA tooling

- `tools/fake-chat-server.mjs` returns a plan with a slot and, with `{"planned": ...}`, a log answer for a planned slot; `tools/capture-*.sh` gains `SCENES=a58`: plan, reserved marker, planned timeline, record with the difference line, undo.

#### Intended specification changes

At Completion: [product Chat](../../produto/specifications/chat.md) rules 16 (the pill, `chatRL`), 19 (receipt line, undo), 22 (a planned slot is not occupied); [home-timeline](../../produto/specifications/home-timeline.md) (`homeP`, gestures); [Room](../specifications/room-v2.md); Provenance lines; ADR-046 status to Accepted (if not already).

### Part E — Skips next to other actions in the Chat (from A59)

Delivered by [A59](pending_manual_validation/a59-skips-with-other-actions.md); nothing of this part is left for A60. The scope below stays as the record of what A59 carried.

#### 1. Wire

- `ChatIn.skip_slots: true` on every normal turn (always encoded, like `meal_changes`); a compact request drops it.
- `ChatOut.skipSlots: List<String>?`: absent (older server) keeps today's single `skip_slot` path; unknown ids and ids not in today's slots are dropped with telemetry `record_guard` (`reason` `slot_not_today`).

#### 2. Apply

- In `autoRecord`, after the log's own outcome (record, `chatU`/`chatI` pending, or none; ADR-028 and ADR-032 rules unchanged), each listed slot in order, reading the slot state at that moment:
  - empty (or planned, once A58 exists) → skip with receipt `Pulado {slot}` and **Desfazer** (`chatSK`);
  - skipped → nothing, no receipt;
  - with a record → a delete proposal (`chatSD`).
- The skip outcomes of an answer are stored with the answer before any write, so recreation, retry and process death never apply a skip twice or lose a queued proposal. One pending card per answer: the log's confirmation first, then each delete proposal in list order; a resolved card reveals the next.
- **Excluir e pular**: one transaction rechecks the request day, the latest wipe, and the slot's records against the captured state; removes the slot's records as Excluir does (memory of the record's active receipt reverted) and marks the slot skipped; receipt `Pulado {slot}` with **Desfazer**, which restores the records and the memory as the replacement undo does. **Manter registro**: nothing written, card marked `Registro mantido`. Expiry: next send, day change, wipe, the slot changed by another path → `Não registrado` on that card.
- A write failure on a skip marks only that skip `Não registrado`; the log receipt stays.

#### 3. Room

- Room v11 (done in A59): nullable `chat_message.skipOutcomes`, a versioned JSON of the answer's listed slots with their state at capture and their outcome (`skipped` | `already` | `pending_delete` | `deleted` | `kept` | `expired` | `failed`); `recordState` unchanged for the log part. Non-destructive migration, exported schema, migration test with messages, logs and an active receipt.

#### 4. Telemetry

- `meal_skipped` (`from` `chat`) gains `with` (`log` | `plan` | `question` | `skip`); new `skip_delete` (`shown` | `confirmed` | `kept` | `expired` | `undone`). Enums only.

#### 5. QA tooling

- `tools/fake-chat-server.mjs` answers a log with `skip_slots` and a skip of a recorded slot; `tools/capture-chat.sh` (or the script that owns `chatG`) gains `SCENES=a59`: `chatSK`, `chatSD`, confirm, keep, undo.

#### Intended specification changes

At Completion: [product Chat](../../produto/specifications/chat.md) rule 4 (one record plus the skips of other slots), rule 7 (skips as a list next to any intent; a skip over a record asks), rule 19 (the skip receipt over a deleted record: Desfazer restores it), states (`chatSK`, `chatSD`) and acceptance criteria; [Room](../specifications/room-v2.md) (new version, `skipOutcomes`); Provenance lines; ADR-047 status to Accepted (if not already).

## Out of scope

- Server (S30), design (all golds delivered), production (blocked by the [production gate](../../content-policy/production-gate.md); the tone choice is its PG6).
- A configurable closure time; a closure for a day without any record (the card shows `Nenhum registro.` and no text is requested); editing records from the cards; user-typed markup (the user's bubble stays plain); reservations for another day; a reservation without a plan bubble.

## Validation

Per part, in order; the prefixes A–E name the part. Every Room item below refers to the single migration of the plan.

- A: DTO and ViewModel tests: response with and without `plan_budget`; malformed fields; every show/hide condition above; **Pode passar** persisted and surviving recreation; **Ajustar** sends the text and `fit_kcal` once, retry keeps it; still-over response shows the choice again; expiry on send, day change and wipe.
- A: Room migration test from the then-current version with chat rows, receipts and A47 proposals intact.
- A: Emulator, dev flavor, against the fake server: fresh `chatRB` captures in `docs/qa/android/current/{dark,light}/` compared with the D12 gold under the QA rules of AGENTS; `chatR` regression after **Pode passar** and after an adjusted plan. Partial validation: only the flows this plan touches.
- A: One real turn through the dev server (S21 deployed): an over-budget recipe shows the choice; **Ajustar** returns a plan at or under the limit or the choice again. Request ids recorded, no user content committed.
- A: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.
- B: Unit tests: migration from the then-current Room version with profiles and chat rows intact; `tone` default and round trip; O5 and Config state; the closure numbers from fixture days and weeks; scheduler dates across the SP rollover and a boot; `closure` idempotence per date and week; offline and fallback states; telemetry params.
- B: Emulator, dev flavor, against the fake server: fresh captures of `o5`, `cfgT`, `cfg`, `homeC`, `homeK` in `docs/qa/android/current/{dark,light}/` compared with the D16 golds under the QA rules of AGENTS; the two notifications checked by `android` CLI screen capture against `push` for the app-controlled parts. Partial validation: only the flows this plan touches.
- B: Emulator against the dev server (S25 deployed), no real clock: tone `duro` set in Config, one Chat turn whose reply names an overshoot; the day closure run through the dev-only broadcast arrives with a text and its card; the week closure likewise; the collapse rule covered by a unit test with a fixed clock. Request ids recorded, no user content committed.
- B: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.
- C: Parser tests: every marker, the fallbacks (unbalanced bold, seven-row table, second table, nested list, heading, link), `plain()` round trips, a 2000-character reply in under a millisecond.
- C: Roborazzi: the three bubbles against the D17 golds (`GoldTest`), dark and light; the plain bubble unchanged.
- C: Emulator, dev flavor, `SCENES=a57` against the fake server: fresh captures of `chatR`, `chatE`, `chatRK` in `docs/qa/android/current/{dark,light}/` compared with the golds under the QA rules of AGENTS; a record from a formatted plan shows plain text on the timeline and in the receipt. Partial validation: only the flows this plan touches.
- C: One real recipe through the dev server (S26 deployed): the table and steps render; the history of the next turn carries no markers (request body inspected). Request id recorded.
- C: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.
- D: Unit tests: migration; reserve, replace, clear by record, skip, rollover, wipe and reset; `BudgetCalculator` with a planned slot; the request DTO with `planned`; the receipt difference and its undo; idempotent double tap.
- D: Roborazzi and emulator captures of `chatR`, `chatRL`, `homeP` against the D18 golds under the QA rules of AGENTS; partial validation.
- D: One emulator session on the dev server (S27 deployed): reserve a dinner, ask for a snack plan (the panel and the server reservation agree), record the dinner (difference line), undo. Request ids recorded.
- D: `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.
- E (done in A59): Unit tests (fake service + real Room): the incident shape (log + one skip) records once and skips once with two receipts; two skips; skip + plan and skip + question; already skipped slot; skip over a record → proposal, confirm (records gone, skipped, memory reverted), keep, expiry by send, day change, wipe and a change by another path; Desfazer restores the record; a log `chatU` pending plus a delete proposal queue in order; retry and recreation apply nothing twice; older server without `skip_slots` keeps today's flow; migration.
- E: Roborazzi and emulator captures of `chatSK` and `chatSD` against the D19 golds under the QA rules of AGENTS, with a written diff list; regress `chatG` and `chatU` (partial validation: Chat records flows only).
- E: Dev server (S29 deployed): one synthetic message with a skip and a breakfast; request id recorded.
- E: `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.
- E: Manual acceptance (after delivery, [autonomous run](../../sdd/autonomous-run.md)): owner or tester sends a skip next to a meal on a device (dev build) and skips a recorded meal. Not a condition for the automated Completion; the plan waits in `pending_manual_validation/`.
- Whole plan: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass after the last part; `node tools/check-docs.mjs` passes; the dev build distributed with its version in Results.

## Results

Planning only.
