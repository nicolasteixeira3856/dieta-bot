# Plan — A71 Conversational onboarding in the app

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: the onboarding feature replaced by the guided chat (welcome, chat engine with the scripted steps, summary, building, success, error), the `POST /v1/profile` client, Room v16 (`onboarding_answer` table, `profile.goalWeightKg`, `profile.goalDate`, `profile.closureTime`, `profile.notificationsEnabled`), the memory cap 50, the closure alarm at the profile time, the notification permission flow, telemetry, tests, captures. Documentation at Completion: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [memoria-push](../../produto/specifications/memoria-push.md), [home-timeline](../../produto/specifications/home-timeline.md), [chat](../../produto/specifications/chat.md), [Room](../specifications/room-v2.md), [gold inventory](../../qa/README.md).
- Prerequisites: [D27](../../design/plans/d27-conversational-onboarding.md) `Concluído`; [S41](../../server/plans/s41-onboarding-profile.md) on the dev server; [A70](pending_manual_validation/a70-option-fit-and-projection.md) merged (Room version order).
- Related documentation: [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md), [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), [ADR-053](../../produto/adrs/ADR-053-visible-memory-screen.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a71-conversational-onboarding.md. Implemente o plano aprovado.`

## Objective

A new install goes from the welcome screen to a Home that already knows the person, through a guided chat that never loses an answer, with one server call on confirmation ([ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md)).

## Scope

1. **Welcome (`ob0`).** Replaces O1 as the first screen after the splash when `onboardingDone = 0`. Animation in the theme palette with the Aero tokens (Compose animation, no Lottie dependency), copy from the gold, CTA `Vamos começar`.
2. **Guided chat (`ob1`, `ob1e`, `ob2`).** A Chat-like screen reusing the bubble, composer and quick-reply composables of `feature/chat/`, with no photo, no selection, no receipts: a `OnboardingScript` (domain) lists the steps of ADR-057 decision 3 in order with, per step, the prompt text, the kind (`choice`, `number`, `text`, `slots`, `prefill`, `yes-no-time`), the validators and the pt-BR parsers (sex, age 10–120, height 100–250 cm, weight 25–400 kg, meals 2–6, names and `HH:mm`, percentages, `sim`/`não`, a time); an invalid answer appends the `não entendi` bubble and repeats the buttons. The `slots` step offers `Usar o padrão` (the 4 default slots of the spec, or the count given with the suggested names by hour) and accepts typed `nome HH:mm` lines. The `prefill` step puts `{ceiling} kcal · P {p} · C {c} · G {g}` in the composer from the TMB (Mifflin-St Jeor, ceiling rounded to 10, 30/40/30) and parses the edited text. Free-text steps cap at 2000 code points with a live counter, `Pular` where ADR-057 allows it. Progress: `{n} de {total}` and the section label. Back goes to the previous answered step (its answer stays in the composer).
3. **Persistence and resume.** Room v16: `onboarding_answer` (`step`, `value` JSON, `createdAtEpochMs`), written on every answer in a transaction; the screen rebuilds the thread from it on open, and `MainActivity` routes to the step after the last answer (or to the summary, the building or the error screen, by a `phase` column in `profile`). The table is emptied on success, on reset and on reinstall. A device with `onboardingDone = 1` never routes here.
4. **Summary (`ob3`).** Built from the answers: body with TMB and IMC, ceiling and macros, meals, compensation, tone, restrictions, measuring, foods, dislikes, equipment, goal, notifications; each block has a pencil that reopens that step in the chat and returns to the summary. CTA `Confirmar e montar o perfil`.
5. **Build (`ob4`, `ob5`, `ob6`).** Confirm sends `POST /v1/profile` (timeout 25 s, `X-Request-Id` as the Chat); the building screen runs while it is in flight; on 200 the app writes `profile` (numbers, slots with `same` modes, tone, goal, notifications and closure time), writes the facts to the encrypted memory as `declared` with the server's numbers, sets `onboardingDone = 1`, empties the answers and shows the success screen; `goal_refused` shows the success screen with the line `Meta de peso não aplicada.`; any error (400, 422, 5xx, timeout, offline) shows the error screen with the answers kept, `Tentar de novo` repeats the same call, `Voltar ao chat` returns to the summary. Buttons stacked.
6. **Memory cap 50.** `MemoryRules` permanent cap 30 → 50; the onboarding writes at most 30; promotion keeps working with the new cap; `memL` shows the declared facts with `Declarado · dd/MM`.
7. **Notifications.** The `sim` step requests `POST_NOTIFICATIONS` on Android 13+ (and shows the exact-alarm rationale when needed); `profile.notificationsEnabled = 0` suppresses the meal reminders and the closures; the day and week closure alarm uses `profile.closureTime` (default `22:00`) instead of the fixed hour, with the same rescheduling flow; Config gains the row `Fechamentos às {HH:mm}` with the wheel dialog, and the notification toggle, in the push block (copy and layout from the existing Config golds, no new gold: the rows reuse `Config/Row`; if the discovery of A71 finds that the row needs a gold, the plan stops and asks).
8. **Goal in the prompt.** `PromptBuilder` sends `profile.goal` when set; `O que a Tali sabe` does not show it (it is a profile field, editable in Config in the goals block, same rule as item 7 about golds).
9. **Retire O1–O5.** The old composables, their ViewModel branches, the `o1`–`o5` captures and their `GoldTest` ids are removed; the Config meal editor keeps the step-by-step code it shares with the old O3 (moved to a Config package). The fake server of the capture scripts answers `POST /v1/profile` from a fixture built from the `nicolas` persona of [S40](../../server/plans/completed/s40-eval-personas.md).
10. **Telemetry.** `onboarding_started`, `onboarding_step` (`step` enum), `onboarding_resumed`, `onboarding_complete`, `onboarding_error` (`reason`: `blocked` | `network` | `server` | `timeout`); enums only.

## Out of scope

- The server (S41). The golds (D27). The Home strip and extras (A72).
- Re-running the onboarding for existing testers. Editing the free-text answers after the profile is built (the facts are edited in `memL`).
- Audio, photo or a live model turn inside the onboarding.

## Validation

1. `./gradlew :app:testDevDebugUnitTest` (script parsers and validators, TMB and IMC, resume routing per phase, `MigrationV15V16Test`, memory cap 50, closure alarm time, `GoldTest` for `ob0`–`ob6`) and `./gradlew :app:verifyRoborazziDevDebug` pass.
2. Emulator, both themes, partial rule: the onboarding flow (`ob0`, `ob1`, `ob1e`, `ob2`, `ob3`, `ob4`, `ob5`, `ob6`) captured against the fake server and compared with `node tools/diff-gold.mjs`; a kill of the process at step 9 resumes at step 9; a forced 500 shows `ob6` and `Tentar de novo` reaches `ob5`. Config, Home and Chat are not captured (JVM regression covers them).
3. One real onboarding on the dev server with the dev build (one `POST /v1/profile` call: the `nicolas` answers), then one Chat turn that uses a declared fact. Cap: **2 model calls** (within the S41 cap, no separate run).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
