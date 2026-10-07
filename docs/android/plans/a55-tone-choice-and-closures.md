# Plan — A55 Tone choice, day closure and week closure

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (onboarding O5, Config row and sheet, profile entity and Room migration, network DTOs, closure scheduler and notifications, Home cards, telemetry, tests) plus the QA tooling `tools/fake-chat-server.mjs` and `tools/capture-*.sh`.
- Related documentation: [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [memoria-push](../../produto/specifications/memoria-push.md), [home-timeline](../../produto/specifications/home-timeline.md), [Room](../specifications/room-v2.md), [HTTP contract](../../api-contract.md).
- Prerequisites:
  - [D16](../../design/plans/completed/d16-tone-and-closures.md) `Concluído` with `o5`, `cfgT`, `homeC`, `homeK` and the changed `cfg` exported;
  - [S25](../../server/plans/s25-tone-and-closures.md) delivered and deployed to the dev server;
  - [A50](a50-plan-budget-choice.md) and [A54](completed/a54-auto-record-addition-empty-slot.md) delivered or cancelled (same Chat and Room files; no parallel Android plan).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a55-tone-choice-and-closures.md. Implemente o plano aprovado.`

## Objective

The user chooses the assistant's tone once at onboarding and can change it in Config; every Chat turn carries it. At 22:00 the app closes the day, and on Sunday the week, with the app's numbers on a Home card and a short server text in the chosen tone.

## Sources

ADR-044 owns the behavior and the copy below. D16 golds own the layout. S25 owns `profile.tone` and `/v1/close`.

## Scope

### 1. Tone in the profile

- Room: `profile.tone` text column, default `seco`, non-destructive migration to the next version discovered at start, exported schema, migration test. Existing profiles read back as `seco`.
- `ChatIn.profile.tone` on every normal and compact request.
- O5 (`o5`), after O4: title `Como a Tali fala com você`, two full-width options with one line each: **Seco** — `Só os números. Sem opinião.` (preselected) and **Duro** — `Cobra o que estourou e o que faltou. Sem rodeio.`; footer `Dá para mudar nas configurações.`; CTA **Concluir e começar** moves from O4 to O5; `onboardingDone` only at the end of O5. Back returns to O4.
- Config (`cfgT`): row `Tom da Tali` with the current value in the profile block; tap opens a sheet with the same two options and the `Salvar` / `Cancelar` pair of the other sheets. No wipe, no confirmation: the next turn uses the new tone.

### 2. Day closure

- Scheduler: an exact alarm (inexact without the permission, like the meal push) at 22:00 America/Sao_Paulo every day; rescheduled at 00:05, on boot, at start and on edit, in the same flow as the meal push. A closure already produced for a date is not produced again (Room row keyed by date).
- At the alarm: the app computes the numbers from Room (eaten kcal and P/C/G, targets, effective ceiling, workout, per-slot status and kcal), calls `POST /v1/close` with `period: day` and the tone, stores `{date, period, numbers, text}` in a new `closure` table, posts the notification `Fechamento do dia` with the first line of the text as content (app-controlled: small icon, title, text, accent color; the rest is the system template), and the Home shows the card `homeC` at the top of the timeline for that date: `Fechamento de {d} de {mês}`, `{kcal} de {teto} kcal`, `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}`, the missing or skipped meals as one line, workout if any, and the text. A later record of the day updates the numbers of the card from Room, not the text. Without network: the card shows the numbers and `Sem o texto: sem rede.`; the next app start retries the text once.
- Rollover: the card of yesterday stays visible until the first record of the new day, then collapses to one line `Ontem: {kcal} de {teto} kcal`; tap expands.

### 3. Week closure

- Sunday 22:00, same scheduler: numbers of the week from Room (per day totals and whether anything was recorded; the slot that most often went over its window, computed as kcal above `ceiling / slots of that day` for days without the S24 window); `POST /v1/close` with `period: week`; `closure` row keyed by the ISO week; notification `Fechamento da semana`; Home card `homeK` above the day card: `Semana de {d} a {d}`, total and mean kcal and protein, `Dias sem registro: n`, the slot line, the text. Stays visible until Tuesday 00:00, then collapses like the day card.

### 4. Telemetry

- Events with enums and numbers only: `tone_set` (`seco` | `duro`, from `onboarding` | `config`), `closure` (`day` | `week`, `text` | `fallback` | `offline`), `closure_opened`. The tone goes to the server in the request body only; it is not a Crashlytics key.

### 5. QA tooling

- `tools/fake-chat-server.mjs` gains `/v1/close` with canned `seco` and `duro` texts and a failure switch; `tools/capture-*.sh` gains `SCENES=a55`: O5 both options, Config row and sheet, the day card with text, offline and collapsed, the week card.

### Intended specification changes

At Completion: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) (5 screens, O5 rules, `tone` in the prompt prefix, CTA move), [memoria-push](../../produto/specifications/memoria-push.md) (Config tone row and sheet; closure alarms and notifications under the push rules), [home-timeline](../../produto/specifications/home-timeline.md) (`homeC`, `homeK`, collapse rules), [Room](../specifications/room-v2.md) (`profile.tone`, `closure` table, version); gold inventory stays owned by [qa](../../qa/README.md); Provenance lines; ADR-044 status to Accepted (if not already).

## Out of scope

- Server (S25), design (D16), production (blocked by the [production gate](../../content-policy/production-gate.md); the tone choice is its PG6).
- A configurable closure time; a closure for a day without any record (the card shows `Nenhum registro.` and no text is requested); editing records from the cards.

## Validation

1. Unit tests: migration from the then-current Room version with profiles and chat rows intact; `tone` default and round trip; O5 and Config state; the closure numbers from fixture days and weeks; scheduler dates across the SP rollover and a boot; `closure` idempotence per date and week; offline and fallback states; telemetry params.
2. Emulator, dev flavor, against the fake server: fresh captures of `o5`, `cfgT`, `cfg`, `homeC`, `homeK` in `docs/qa/android/current/{dark,light}/` compared with the D16 golds under the QA rules of AGENTS; the two notifications checked by `android` CLI screen capture against `push` for the app-controlled parts. Partial validation: only the flows this plan touches.
3. One real day on the dev server: tone `duro` set in Config, one Chat turn whose reply names an overshoot; the 22:00 closure arrives with a text; the next morning the card collapsed. Request ids recorded, no user content committed.
4. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.

## Results

Planning only.
