# Plan — A60 Budget choice, tone and closures, rich replies, planned meal and skips (app)

- Status: Pendente aprovação manual
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only, in five parts delivered in order on one branch: (A) the over-budget choice in the Chat; (B) tone choice at onboarding and in Config, day and week closures; (C) the reply formatting subset rendered in the bubbles; (D) reserving a plan for its meal; (E) skips next to other actions. Plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-*.sh`. One Room version for the whole plan: every column and table of the five parts goes into one migration decided at the start of part A (non-destructive, exported schema, migration test), so later parts add no version.
- Related documentation: [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-044](../../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [ADR-045](../../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md), [ADR-046](../../../produto/adrs/ADR-046-planned-meal-reservation.md), [ADR-047](../../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [product Chat](../../../produto/specifications/chat.md), [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md), [memoria-push](../../../produto/specifications/memoria-push.md), [home-timeline](../../../produto/specifications/home-timeline.md), [Room](../../specifications/room-v2.md), [HTTP contract](../../../api-contract.md), [autonomous run](../../../sdd/autonomous-run.md).
- Prerequisites:
  - golds exported: D12 (`chatRB`), D16 (`o5`, `cfgT`, `homeC`, `homeK`, `cfg`), D17 (`chatR`, `chatE`, `chatRK`), D18 (`chatR`, `chatRL`, `homeP`), D19 (`chatSK`, `chatSD`), all `Concluído` in [design history](../../../design/plans/completed/);
  - server: S21 and S29 delivered and deployed (they are); [S30](../../../server/plans/completed/s30-tone-formatting-planned-slot.md) delivered and deployed to the dev server with the parts the client parts need (B needs S30 part A, C needs part B, D needs part C); a server part left out skips the matching client part;
  - [A54](../completed/a54-auto-record-addition-empty-slot.md) delivered (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a60-tone-formatting-planned-skips.md. Implemente o plano aprovado.`

## Objective

Everything the Chat and the Home need for the tone, the closures, the formatted replies, the planned meal and the skips, in one plan and one dev build: the owner wakes up with the app that shows bold numbers, lists, the day summary and the hard tone when chosen. Supersedes the cancelled plans A50, A55, A57 and A58 ([`cancelled/`](../cancelled/)), whose scope it carries unchanged; the skips scope was delivered by [A59](a59-skips-with-other-actions.md) and stays here only as part E's record.

## Delivery

- One branch from master; parts in the order A → B → C → D → E; one commit or more per part; one PR at the end. Part E is delivered by [A59](a59-skips-with-other-actions.md) (owner decision, 07/10/2026: A59 merged as implemented, Room v11 `skipOutcomes`); A60 runs parts A–D.
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

At Completion: product Chat rule 16 gains the choice (when it shows, copy, actions, expiry) and `chatRB` in the state list; Room gains the column and version; Provenance links. The gold inventory stays owned by [qa](../../../qa/README.md).

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
- Dev-only trigger ([ADR-019](../../../produto/adrs/ADR-019-ferramentas-dev.md)): the broadcast `app.fibrai.android.dev.RUN_CLOSURE` with the extra `period` (`day` | `week`) runs the closure now, as the alarm would, in the dev flavor only; it is how the validation and the testers exercise the 22:00 path without waiting for it.

#### 3. Week closure

- Sunday 22:00, same scheduler: numbers of the week from Room (per day totals and whether anything was recorded; the slot that most often went over its window, computed as kcal above `ceiling / slots of that day` for days without the S24 window); `POST /v1/close` with `period: week`; `closure` row keyed by the ISO week; notification `Fechamento da semana`; Home card `homeK` above the day card: `Semana de {d} a {d}`, total and mean kcal and protein, `Dias sem registro: n`, the slot line, the text. Stays visible until Tuesday 00:00, then collapses like the day card.

#### 4. Telemetry

- Events with enums and numbers only: `tone_set` (`seco` | `duro`, from `onboarding` | `config`), `closure` (`day` | `week`, `text` | `fallback` | `offline`), `closure_opened`. The tone goes to the server in the request body only; it is not a Crashlytics key.

#### 5. QA tooling

- `tools/fake-chat-server.mjs` gains `/v1/close` with canned `seco` and `duro` texts and a failure switch; `tools/capture-*.sh` gains `SCENES=a55`: O5 both options, Config row and sheet, the day card with text, offline and collapsed, the week card.

#### Intended specification changes

At Completion: [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) (5 screens, O5 rules, `tone` in the prompt prefix, CTA move), [memoria-push](../../../produto/specifications/memoria-push.md) (Config tone row and sheet; closure alarms and notifications under the push rules), [home-timeline](../../../produto/specifications/home-timeline.md) (`homeC`, `homeK`, collapse rules), [Room](../../specifications/room-v2.md) (`profile.tone`, `closure` table, version); gold inventory stays owned by [qa](../../../qa/README.md); Provenance lines; ADR-044 status to Accepted (if not already).

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

At Completion: [product Chat](../../../produto/specifications/chat.md) rule 2 (what a bubble renders), rule 8 (history line without markers), rule 12 (record text without markers), rule 16 (`chatRK`, bold over the macro colours); state list gains `chatRK`; Provenance line; ADR-045 status to Accepted (if not already).

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

At Completion: [product Chat](../../../produto/specifications/chat.md) rules 16 (the pill, `chatRL`), 19 (receipt line, undo), 22 (a planned slot is not occupied); [home-timeline](../../../produto/specifications/home-timeline.md) (`homeP`, gestures); [Room](../../specifications/room-v2.md); Provenance lines; ADR-046 status to Accepted (if not already).

### Part E — Skips next to other actions in the Chat (from A59)

Delivered by [A59](a59-skips-with-other-actions.md); nothing of this part is left for A60. The scope below stays as the record of what A59 carried.

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

At Completion: [product Chat](../../../produto/specifications/chat.md) rule 4 (one record plus the skips of other slots), rule 7 (skips as a list next to any intent; a skip over a record asks), rule 19 (the skip receipt over a deleted record: Desfazer restores it), states (`chatSK`, `chatSD`) and acceptance criteria; [Room](../../specifications/room-v2.md) (new version, `skipOutcomes`); Provenance lines; ADR-047 status to Accepted (if not already).

## Out of scope

- Server (S30), design (all golds delivered), production (blocked by the [production gate](../../../content-policy/production-gate.md); the tone choice is its PG6).
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
- E: Manual acceptance (after delivery, [autonomous run](../../../sdd/autonomous-run.md)): owner or tester sends a skip next to a meal on a device (dev build) and skips a recorded meal. Not a condition for the automated Completion; the plan waits in `pending_manual_validation/`.
- Whole plan: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass after the last part; `node tools/check-docs.mjs` passes; the dev build distributed with its version in Results.

## Results

Approved by the owner on 07/10/2026 in the batch message of the [autonomous run](../../../sdd/autonomous-run.md) ("Aprovo o plano docs/android/plans/a60-tone-formatting-planned-skips.md. Implemente o plano aprovado, partes A a D."). Branch `feat/a60-tone-formatting-planned` from master `24211e5` (S30 merged and deployed to the dev server). Run report: [overnight-2026-10-08](../../../server/validation/overnight-2026-10-08.md).

| Part | State |
|---|---|
| A — over-budget choice | Delivered |
| B — tone, day and week closures | Delivered |
| C — reply formatting subset | Delivered |
| D — reserve a plan | Delivered |
| E — skips next to other actions | Entregue pelo A59, aceite manual pendente ([A59](a59-skips-with-other-actions.md)); not reimplemented here |

### Delivered (`apps/android/` and the QA tooling)

- **Room v12 (one migration for A–D):** `MIGRATION_11_12`: `chat_message.planBudget` (nullable JSON: the server `plan_budget` plus the local `over_ok`), `profile.tone` (`TEXT NOT NULL DEFAULT 'seco'`), tables `closure` (key per day or week, numbers JSON, text, status `text` | `fallback` | `offline` | `empty`, one retry flag) and `planned_meal` (one reservation per date and slot). Schema `12.json` exported; `MigrationV11V12Test`; the version asserts of `MigrationV1V2Test` and `RoomV2Test` and every older migration test follow v12.
- **Part A:** `domain/PlanBudget.kt` (lenient decode; a missing or malformed object means no choice); `plan_budget: true` on every normal turn and `fit_kcal` on the Ajustar turn and its retry; `ChatViewModel.acceptOver` (stores `over_ok`, no POST) and `adjustToFit` (sends `Ajusta para caber em {limit} kcal.` through the normal send path); `AeroChoiceBar` (Pode passar · Ajustar para caber) in the action slot and the budget lines below the bubble; the choice ends on a new send, the day change or a wipe. Telemetry `plan_budget_choice`.
- **Part B:** O5 `ToneScreen` (`ONBOARDING 5/5 • TOM`, Seco preselected with `PADRÃO`, Duro, the note, **Concluir e começar**; O4's CTA is now **Continuar**); Config row `Tom da Tali` and its sheet (`ToneOptions` on glass, Salvar / Cancelar, no wipe); `profile.tone` on every normal and compact turn. Closures: `domain/Closures.kt` (day and week numbers, keys, due periods, card states, copy), `core/closure/ClosureRunner.kt` (one closure per key, `/v1/close`, the numbers alone when offline, one retry at the next app start), `ClosureNotifications.kt` (channel `closures`, `Fechamento do dia` / `Fechamento da semana`), the 22:00 alarm in `SlotAlarmScheduler` (America/Sao_Paulo, rescheduled at start and boot), the dev-only broadcast `RUN_CLOSURE` (dev manifest only), Home `ClosureCard` (day; week above it on Sunday; collapsed to `Ontem: {kcal} de {teto} kcal` after the first record of the next day). Telemetry `tone_set`, `closure`, `closure_opened` (enums and numbers only).
- **Part C:** `domain/ReplyMarkup.kt` (own parser: bold, `- ` bullets, `1. ` steps, one two-column table up to six rows; anything else literal; `plain()` for history, timeline and receipts); `core/designsystem/aero/AeroReply.kt` (blocks with the 12 dp gap, marker columns, the portions table, the macro colours laid over bold). The user's bubble, questions, refusals, receipts and cards are unchanged.
- **Part D:** **Reservar para o {slot}** under Registrar assim on today's plan for an open meal; `DayRepository.reserve` (one transaction checking day, wipe and slot; a new reservation replaces the old one and the old bubble loses `Reservado para o {slot}`); the reservation goes to the server as `status: planned`; Home node `planejado · {kcal} kcal` (new icon `CalendarCheck`); a record into a reserved meal replaces the reservation, the receipt adds `Plano: {kcal} · Registrado: {kcal} ({±n} kcal)` and Desfazer brings it back; a skip clears it and its Desfazer brings it back. Telemetry `plan_reserved`, `meal_saved.had_plan`.
- **QA tooling:** `tools/fake-chat-server.mjs` (`plan_budget` over / still over / zero, `/v1/close` and `close_fail`, the `format` replies, `planned`; `/__calls` reports the new request fields); `tools/capture-chat.sh` `SCENES=a60`; `tools/capture-onboarding.sh` (O5, back to O4, finish); `tools/diff-gold.mjs` (A60 long threads `chatRB` from the day panel down, `chatRK`, `chatRL`; the two gold conflicts below).
- **Specifications:** product [Chat](../../../produto/specifications/chat.md) (rules 2, 12, 16, 19, states, acceptance, related ADRs, provenance), [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) (O5), [memoria-push](../../../produto/specifications/memoria-push.md) (rule 12, the tone in Config), [home-timeline](../../../produto/specifications/home-timeline.md) (closure cards, planned node), [Room](../../specifications/room-v2.md) (v12); [QA](../../../qa/README.md) (`SCENES=a60`).

### Validation

1. **Unit tests (new):** `ChatBudgetChoiceTest` (6: with and without `plan_budget`, malformed, every show and hide rule, Pode passar persisted across recreation, Ajustar sends text and `fit_kcal` once and the retry keeps it, still over shows the choice again, expiry on send, day change and wipe), `ClosuresTest` (7: the 22:00 São Paulo alarm, now when missed and tomorrow when done; due periods; day and ISO-week keys; day and week numbers from fixture days; card states and the collapse rule with a fixed clock; copy), `ClosureRunnerTest` (6: numbers and tone sent, text stored, one notification; offline then one retry at start; server fallback and a day without records asking nothing; the week from Monday; live and collapsed cards; a planned meal counts as missing), `ReplyMarkupTest` (4: every marker, the fallbacks — unbalanced bold, seven-row table, second table, nested list, heading, link —, `plain()` round trips, a 2000-character reply parsed in under a millisecond), `ChatPlannedTest` (5: reserve marks the bubble and goes in DAY and the projected day; reserving again replaces and the earlier bubble loses its marker; a record replaces the reservation with the receipt line and Desfazer restores it; a skip, a wipe and the next day clear it; no pill for a recorded meal or while the choice is open), `MigrationV11V12Test`; plus O5 and the Config tone in `OnboardingViewModelTest` and `ConfigViewModelTest`, `PushHandlerTest` (closure action) and the dev `ProfileTextTest`.
2. **`testDevDebugUnitTest`**, **`verifyRoborazziDevDebug`** and **`assembleDevRelease`**: 638 tests, 0 failures (the six GoldTest failures A59 recorded on `chatE`, `chatR` and `cfg` are gone: their golds now match); Roborazzi verifies clean; the release APK builds. Roborazzi baselines re-recorded for the screens this plan changes: `chatE`, `chatR` (dark, light) and `cfgWorkout` (the tone row moves the sheet's page).
3. **JVM gold (`GoldTest`)**: new `chatRB`, `o5`, `cfgT`, `homeC`, `homeK`, `chatRL`, `chatRK`, `homeP` (dark, light) and the changed `chatR`, `chatE`, `cfg` pass. The emulator pass found one defect the JVM gate tolerated: the P/C/G colours were missing on formatted replies (the decor read the builder's `toString()`, not the text); fixed in `AeroReply.kt` and checked on the render and on the emulator.
4. **Emulator** (Medium_Phone at 780 × 1688 @ 320, devDebug against `tools/fake-chat-server.mjs`, `SCENES=a60`, both themes): 48 of 48 checks passed in each theme — `plan_budget` on the wire, the lines and pills, Pode passar stored and kept after recreation, `fit_kcal` 310, still over, adjusted plan fits, limit 0 (no pills); bullets, table and steps, no `**` on screen, the next turn's history and `meal_log` without markers, malformed markup literal; Reservar, the marker, `planned_meal`, DAY `planned`, receipt `Plano 420 · Registrado 610 (+190 kcal)`, Desfazer restores the reservation, `homeP`; Config `Seco` → `Duro` stored and on the wire; Sunday 22:05 through the real alarm: both notifications, both closures with text, the broadcast adds nothing, `homeC`, `homeK`, offline card `Sem o texto: sem rede.`, the retry at start, the collapsed card the next day. O5 by `tools/capture-onboarding.sh`.
5. **`node tools/diff-gold.mjs`** (max 2 %): `chatRB` header 0.00 % / 0.00 % (dark / light), tail 0.01 % / 0.06 %; `chatR` header 0.00 / 0.00, tail 0.63 / 0.25; `chatRK` header 0.00 / 0.11, tail 0.44 / 0.79; `chatRL` header 0.00 / 0.00, tail 0.08 / 0.19; `chatE` light 0.25; `homeP` 0.83 / 0.63; `cfg` 0.19 / 0.19; `cfgT` 0.32 (dark), sheet 0.00 (light); `homeC` 0.73 / 0.53; `homeK` 1.23 / 0.91; `o5` 1.00 / 0.69.
6. **Diff list** (captures in `docs/qa/android/current/{dark,light}/` against the D12, D16, D17 and D18 golds): layout, tokens, type sizes, radii, the action stack (choice pills in the action slot, Registrar assim over Reservar), CTA colours, sheet radius, the closure cards, the planned node and the semantic macro colours match. Ignored: clock, status bar, date chip and message times. Differences, all reported and none changed in the app:
   - `chatRB`: the D12 frame (drawn before D17) writes the plan as plain lines with no gap and no hanging indent; the app renders the D17 blocks (12 dp gaps, marker columns) required by ADR-045. Gated from the day panel down (panel, budget lines, pills, composer).
   - `chatR`, `chatRK`, `chatRL`, `chatRB`: the frames show the whole thread; the phone shows its newest part (long-thread rule). The bullet glyph and the reserved-marker icon sit about 3 dp right of the frame.
   - `chatE` and `chatR` dark: the frames paint the reply's first paragraph in the bubble colour (invisible); light is gated and passes.
   - `cfgT` light: the frame keeps Config sharp under the scrim; the app's shared Sheet/Bottom blurs the screen behind it, as every Config sheet and the `chatT` frame do. The sheet itself is gated and passes.
   - `o1`–`o4`: the D16 change to five screens left those frames reading `n/4`; the app reads `n/5`, and O4's CTA is **Continuar** per this plan (the O4 frame still reads Concluir e começar).
   - `cfgS`: the frame predates the `Tom da Tali` row (report only in `GoldTest`).
7. **Dev server** (S30 deployed; devDebug pointed at the dev server; synthetic messages, no user content committed):
   - A: an over-budget lasagna recipe at 20:30 showed the choice (`5904a9ff-8d7c-410a-b299-f2311434bf88`, `limit_kcal` 380, `over_kcal` 740); Ajustar returned a plan that fits with Registrar assim and Reservar (`1d24afd6-2321-4667-95aa-1d90b802f3b9`, `choice` `fit`).
   - C: the recipe rendered the portions table and steps; the next turn (`6f902da6-8260-4270-a60e-b5ef9f5a0f3f`) went through. The dev log does not keep the prompt, so the history without markers rests on the fake-server request body (check 4) and the unit tests.
   - B: tone `duro`, a dinner over the window (`3aa681df-45f0-4f07-aae9-a997c968463f`): the reply named the overshoot and the protein missing; day and week closures through the dev broadcast returned text in `duro` (`877666d6-6588-48ed-b165-f8498ad369da`, `720f4b38-adc5-46d7-8e2e-4fc09c64c781`) and both cards showed.
   - D: a dinner plan reserved (`ad4fb7c1-3092-487c-b5d4-18c087dc6e63`), a snack plan with the dinner reserved at 350 kcal (`1cbed76c-a297-4789-9821-a110883d0b67`, `reserved` Jantar 350), the dinner recorded (`fe5f5733-3e72-4866-89eb-7918065e7c3e`, `plan_difference` +30: `+30 kcal sobre o plano.` and the receipt line `Plano: 350 · Registrado: 380 (+30 kcal)`), Desfazer brought the reservation back.
8. `node tools/check-docs.mjs` passes.
9. Dev build: distributed after the merge with `tools/distribute-dev.ps1`; the version is recorded in the [run report](../../../server/validation/overnight-2026-10-08.md).

### Pending manual validation

On a device with the dev build, by the owner or a tester:

1. A59 (part E): send a skip next to a meal and skip a recorded meal ([A59](a59-skips-with-other-actions.md)).
2. Tone: finish onboarding on O5 (or change it in Config → Tom da Tali) and see a `duro` reply name an overshoot; `seco` stays numbers only.
3. Closures: at 22:00 the day notification and card arrive (Sunday also the week); the next day the card collapses after the first record. Allow notifications when Android asks.
4. Over budget: a plan that passes the day shows Pode passar · Ajustar para caber.
5. Reservation: Reservar para o Jantar on a plan, the Home shows `planejado`, recording the dinner shows the difference in the receipt.
6. Visual review of the gold differences in item 6 (whether the D12 `chatRB`, the `o1`–`o4`, O4 CTA and `cfgS` frames and the light `cfgT` scrim should be redrawn).

PG6 of the [production gate](../../../content-policy/production-gate.md) stays open until items 2 and 3 are done on the dev build.
