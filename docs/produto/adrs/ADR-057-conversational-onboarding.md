# ADR-057 — Conversational onboarding: Tali builds the profile on day 0

- Status: Accepted (2026-10-09, owner approval of [D27](../../design/plans/pending_manual_validation/d27-conversational-onboarding.md) in the batch message; owner direction of 09/10/2026 in the brainstorm session)
- Date: 2026-10-09
- Context: `produto`
- Supersedes: partially [ADR-012](ADR-012-chat-home-perfil.md) (the onboarding of O1–O4 and its rule 4: the five numeric screens are replaced by the guided chat; Chat as a screen, Home as the panel and the named profile survive); partially [ADR-044](ADR-044-assistant-tone-and-closures.md) (O5 becomes a step of the guided chat; the closure alarms become a profile time with 22:00 as default; the tones, their rules and the closure cards survive); partially [ADR-053](ADR-053-visible-memory-screen.md) (decision 4: the O6 condition is waived by owner decision, the routine entry is the onboarding itself; the memory screen survives); partially [ADR-023](ADR-023-chat-v2-memoria-v2.md) and [ADR-029](ADR-029-fatos-temporarios-compactacao.md) (the permanent cap goes from 30 to 50; every other memory rule survives). Reactivates [A56](../../android/plans/out_of_scope/a56-goal-weight.md) (goal weight and date) inside this decision.

## Context

The app is useful only after a few days of use: O1–O5 collect numbers (body, ceiling, eat-back, slots, macros, tone) and nothing about how the person eats. The 07/10/2026 brainstorm (`benchmark/MELHORIAS_CHAT_07_10_2026.md`) measured that the Grok transcripts worked on day 0 because the owner wrote the routine, the preferences, the equipment and the weak spot by hand. [ADR-051](ADR-051-plan-option-identity-and-chat-discovery.md) added a skippable discovery on the first Chat opening; [ADR-053](ADR-053-visible-memory-screen.md) kept a routine screen (O6) conditional on measuring that discovery. The owner decided on 09/10/2026 not to wait for that measurement: the product is a chat with an assistant, so the assistant builds the profile from the first minute, and the profile must be a JSON structure the app uses internally.

Constraints that do not change: LLM only on the server, user state only on the device (Room and the encrypted memory), the server persists no user text, the invite gate, the content-policy scope and moderation of [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md), the formulas of `AGENTS.md`, one record per meal, the production gate.

## Decision

1. **One onboarding, as a guided chat.** The first run shows a welcome screen (animation in the theme palette, `Bem-vindo ao Fibrai`, a one-line pitch, CTA `Vamos começar`) and then a simplified Chat surface: text only, no photo, no audio, 2000 characters per answer with a visible counter, Tali's bubbles and the user's bubbles, quick-reply buttons where the answer is closed. It replaces O1–O5. Everything the old screens collected is still collected or derived.
2. **The chat is scripted on the device.** The questions, their order, the validation of closed answers (sex, age, height, weight, number of meals, meal names and times, eat-back, tone, notifications and their time) and the "não entendi" retry are app logic, no model call. A closed answer accepts the buttons and pt-BR text variants; an invalid answer repeats the question with the buttons. Free-text answers (dietary restrictions, measuring style, usual foods and their times, dislikes, equipment, goal weight and date) are stored as typed and validated only for length.
3. **Question list** (one message each, in this order; the design plan owns the exact copy): sex; age; height; weight; number of meals; name and time of each meal, with a `Usar o padrão` option that fills the names and times the app suggests by hour; workout compensation (`0 %`, `50 %`, `100 %`, or a typed percentage); tone (`seco` preselected, both described with one example each); dietary restrictions (buttons for the frequent ones: lactose, gluten, vegetarian, vegan, diabetes, hypertension, plus free text); measuring style (scale or household measures); usual foods and when; dislikes (skippable); equipment (air fryer, microwave, pressure cooker, etc.); goal weight and date (skippable); notifications on or off and, when on, the closure time (default 22:00). The ceiling and the macro targets are proposed, not asked: after the body answers the app computes the TMB (Mifflin-St Jeor) and the IMC and puts `{ceiling} kcal · P {p} · C {c} · G {g}` (ceiling = TMB rounded to 10, macros 30/40/30) as prefilled text in the composer for the user to edit or send; the ceiling mode (`same`) and the week mode of the slots (`same`) are derived and editable in Config as today.
4. **Summary before the call.** When the last question is answered the app shows a summary built from the answers: body with TMB and IMC, ceiling and macros, meals, compensation, tone, restrictions, measuring style, equipment, goal, notifications. Each block has a correction entry that reopens that question in the chat and returns to the summary. No model call happens before the user confirms. The IMC is shown as information, never as a judgment.
5. **One model call builds the profile.** Confirming sends one `POST /v1/profile` with the structured answers and the free-text answers, at `reasoning.effort=low`, under the Chat scope and moderation rules of the content policy. The server returns a typed profile: the numeric fields the app already stores (echoed and validated), the slots, the tone, the goal, and up to 30 memory facts in the existing fact schema (categories `preference`, `portion`, `routine` with the slot and estimated macros, `equipment`), prioritised restrictions first, then routines, then equipment, then preferences; the rest is dropped. The app writes the profile to Room and the facts to the encrypted memory as declared facts (`source: declared`), marks the onboarding done and deletes the raw answers. The server persists nothing. A health restriction (diabetes, hypertension, allergies) becomes a permanent preference fact used only to avoid or prefer foods; it never turns Tali into medical advice, the disclaimer stays and [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md) applies unchanged.
6. **Full-screen states after confirming.** `Montando o seu perfil` (full screen, logo, animation) while the call runs; success (green, full screen, one CTA to the Home); error (red, full screen, `Tente de novo mais tarde`, two stacked buttons: `Tentar de novo` above `Voltar ao chat`). Retrying repeats the same call. Buttons on these screens are always stacked, never side by side.
7. **The onboarding is resumable.** Every answer is saved on the device as it is given. Closing the app at any step, before or after confirming, reopens the chat at the same step with the earlier bubbles; an error leaves the user on the error screen with the answers kept. The onboarding is left only by a successful profile build, by the app reset or by a reinstall. A device that already has `onboardingDone = 1` never sees the new onboarding.
8. **Memory cap.** The permanent cap is 50 facts (was 30); the dynamic and temporary caps, the promotion, the expiry and the tombstones are unchanged. The onboarding contributes at most 30.
9. **Goal weight.** The profile gains an optional goal weight (kg) and date, sent to the server in `PROFILE` on every turn. The closures and the `duro` tone may cite the overshoot against the deficit the goal implies, in kcal and days, never in remarks about the body; a goal below the safety limits of [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md) (very low target, unrealistic date) is refused in the onboarding with the fixed safety handling and the profile is built without it. The deficit is the ceiling gap: no TDEE.
10. **Notifications.** One answer turns on both the meal reminders and the closure notifications; a `sim` asks the system permission (Android 13+) right there. The closure time is a profile field (default 22:00, editable in Config) that replaces the fixed 22:00 of [ADR-044](ADR-044-assistant-tone-and-closures.md) for the day and the week closures.
11. **Telemetry.** `onboarding_started`, `onboarding_step` (step enum), `onboarding_resumed`, `onboarding_complete` (exists), `onboarding_error` (reason enum), only enums; never an answer.

## Rationale

The product is a chat with an assistant; letting the assistant build the profile is the shortest path to a useful day 0, and it is cheap: one call per user. Scripting the questions on the device keeps the cost at zero while the user answers, keeps the validation deterministic and keeps the resumable state local. One call at the end, instead of a call per answer, keeps the server stateless and the smoke within the Delivery pace cap. Prefilling the ceiling and macros in the composer keeps the "numbers first" rule and the owner's requirement that the user edits a ready text. Raising the permanent cap to 50 gives the onboarding room without starving the facts learned later.

## Consequences

### Positive

- Day 0 works: restrictions, routines, equipment and preferences are in the memory before the first Chat turn.
- The profile is a JSON the app owns; the server stays stateless; the memory rules do not change.
- A single funnel (`onboarding_started` → `onboarding_complete`) measures the conversion the owner asked for.

### Negative

- Five golds retire (`o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `o5`) and eight new ones are drawn; the Config editors keep their own golds.
- A longer first run (about 17 questions). The default answers and the skippable questions keep it short for whoever wants to.
- One more route, one more table (the saved answers) and one Room migration.

## Alternatives considered

### Keep O1–O5 and add O6 for the routine

Keeps the numeric screens the owner wants to drop and splits the story in two surfaces. Rejected by the owner on 09/10/2026.

### A live model turn per answer

Costs one call per question, needs server state or a growing history per request, and gives the model the validation the app does better. Rejected.

### Wait for the measurement of the Chat discovery (ADR-053 decision 4)

The owner decided not to wait; the discovery of [ADR-051](ADR-051-plan-option-identity-and-chat-discovery.md) stays for a memory that is empty after a reset of the facts.

## Relations

- Specifications affected: [perfil-onboarding](../specifications/perfil-onboarding.md) (rewritten: the guided chat, the summary, the states, the resume rule), [memoria-push](../specifications/memoria-push.md) (cap 50, closure time, notification choice), [chat](../specifications/chat.md) (goal in the prefix, discovery unchanged), [home-timeline](../specifications/home-timeline.md) (closure time), [v1-chat](../../server/specifications/v1-chat.md) and the [HTTP contract](../../api-contract.md) (`POST /v1/profile`, `profile.goal`), [Room](../../android/specifications/room-v2.md), [content policy](../../content-policy/specifications/content-policy.md) (the route under the Chat scope and moderation), [gold inventory](../../qa/README.md).
- Related ADRs: [ADR-012](ADR-012-chat-home-perfil.md), [ADR-021](ADR-021-refeicoes-por-dia.md), [ADR-023](ADR-023-chat-v2-memoria-v2.md), [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md), [ADR-044](ADR-044-assistant-tone-and-closures.md), [ADR-051](ADR-051-plan-option-identity-and-chat-discovery.md), [ADR-053](ADR-053-visible-memory-screen.md), [ADR-054](../../server/adrs/ADR-054-chat-reasoning-effort-low.md).
- Consuming contexts: [design](../../design/README.md) (D27), [server](../../server/README.md) (S41), [android](../../android/README.md) (A71).

Once accepted, the body of this ADR is not edited. Only the `- Status:` line changes, to record a total or partial supersession by a newer ADR.
