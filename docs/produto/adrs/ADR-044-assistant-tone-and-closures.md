# ADR-044 — Assistant tone chosen by the user; day and week closure

- Status: Accepted (2026-10-06, with the owner's approval of D16 and S25)
- Date: 2026-10-06
- Context: `produto`
- Supersedes: partially `AGENTS.md` ("Dry tone. No coach." becomes a tone the user chooses); partially [ADR-012](ADR-012-chat-home-perfil.md) (onboarding gains O5; the Home gains the closure cards); partially [ADR-020](ADR-020-estados-novos-chat-home-horario.md) (closed screen list extended with `o5`, `cfgT`, `homeC`, `homeK`). The reminders of [ADR-012](ADR-012-chat-home-perfil.md) rule 8 stay as they are.

## Context

The owner built Fibrai to bring to other people the experience of a general chat bot configured by hand as a diet coach: a written profile with priorities, reminders per meal time, a closing message every night with totals against the targets and three concrete adjustments for the next day, a weekly closing on Sunday with the numbers of the week and a plan for the next one, and a tone that is direct and hard on the weak meal of the day. The constitution forbade the tone ("Dry tone. No coach.") and the product had none of the closures. The reminder per meal time already exists (push rule: `{nome}. Ainda não registrou.`, only while the slot has no record or skip).

On 2026-10-06 the owner decided to bring those pieces in, with one condition: the tone is the user's choice, not the product's, and that choice is a production blocker.

## Decision

1. **Tone is a profile field.** `tone` is `seco` (default) or `duro`. `seco` is today's behavior: numbers first, no judgment, no advice beyond the dish. `duro` is direct critique from the numbers: it names the meal that broke the ceiling or the protein shortfall, it is hard on the dinner and the weekend when they are what breaks the week, it gives practical adjustments for the next day or week; when a log differs from a plan agreed earlier in the same day (in HISTORY, a digest or a temporary fact), it says by how much and what to change in the meals still open. Both tones: never about body, weight or appearance; never a push below the ceiling, to skip a meal or to compensate by fasting; content-policy scope, refusals and safety signals unchanged; no slogans, no praise. Chosen at onboarding (O5, one screen, two options, `seco` preselected) and editable in Config. Sent to the server as `profile.tone` on every turn; the server keeps one instruction block per tone.
2. **Day closure.** At 22:00 America/Sao_Paulo the app closes the day: a notification "Fechamento do dia" and a Home card (`homeC`) with kcal and P/C/G against the targets, meals without record or skipped, the workout of the day, and a short text from the server (`POST /v1/close`, `period: day`): with `seco`, one line of numbers; with `duro`, up to three lines of what to adjust tomorrow. When the last slot of the day has no record yet, the text asks for it in one line and the closure still runs; a later record of that slot updates the numbers of the card, not the text. The closure records nothing and changes nothing.
3. **Week closure.** On Sunday at 22:00 the app closes the week: a Home card (`homeK`) with total and mean kcal and protein, days without any record, the slot that most often went over its window, and a server text (`period: week`): with `duro`, critique from the data and a plan for the next week (three dinners, two afternoon snacks, a weekend kcal ceiling); with `seco`, the numbers and the three dinners without critique.
4. **Numbers are the app's.** The app computes every number of both closures from Room and sends them to the server; the model writes prose over those numbers and never recomputes them. The server stores nothing.
5. **Reminders stay.** The meal-time push of the product is already the reminder of the reference experience; this ADR adds no reminder and changes no copy.
6. **Production gate.** The tone choice (O5, Config, `profile.tone` honored by the server) is a production blocker: PG6 in [production-gate](../../content-policy/production-gate.md), added by owner decision although it is not a content-policy item.

## Motivation

- The reference experience worked for the owner because of the written profile and the closures, not because of the model. The protein objective is ADR-043; the closures and the tone are this ADR.
- A tone imposed on ten testers with different goals is noise or offense; a tone chosen by the user is a feature. `seco` stays the default so nobody gets critique they did not ask for.
- The closures are the only place where "critique the day" has data: the day is over and the numbers are final.

## Consequences

### Positive

- The product gains the two moments the owner valued most, the nightly close and the weekly close, with numbers.
- Users who want pressure get it; users who do not keep today's product.

### Negative

- One new onboarding screen, one Config row, two Home cards and two notifications: a design plan, a client plan and a server plan.
- Two tone blocks double the fixed prefixes the server caches (2 capability branches × 2 tones).
- A `duro` text is model prose over numbers: it needs evaluation cases for the forbidden topics (body, weight, fasting, skipping) before dev distribution.

## Alternatives considered

### Keep "Dry tone. No coach." for everyone

Rejected by the owner: the hard tone is what worked for him, and other users may want it.

### `duro` as the only tone

Rejected: the owner is not the only user; the choice is the product.

### Closures computed by the model from the conversation

Rejected: the day is in Room, not in the chat; the server is stateless and the model adds poorly (ADR-042).

### Closure time configurable

Deferred. 22:00 fixed in this cut; a setting may come with its own plan.

## Relations

- Specifications affected: [perfil-onboarding](../specifications/perfil-onboarding.md) (O5, prompt prefix), [memoria-push](../specifications/memoria-push.md) (Config tone row, closure notifications), [home-timeline](../specifications/home-timeline.md) (`homeC`, `homeK`), [chat](../specifications/chat.md) (tone in the prompt), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md) (`profile.tone`, `/v1/close`), [Room](../../android/specifications/room-v2.md).
- Related ADRs: ADR-012, ADR-020, [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md), [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md).
- Consuming contexts: [design](../../design/README.md) ([D16](../../design/plans/completed/d16-tone-and-closures.md)), [server](../../server/README.md) ([S25](../../server/plans/s25-tone-and-closures.md)), [android](../../android/README.md) ([A55](../../android/plans/a55-tone-choice-and-closures.md)).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
