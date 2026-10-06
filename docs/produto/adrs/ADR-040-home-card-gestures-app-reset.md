# ADR-040 — Home card gestures and app reset

- Status: Accepted (2026-10-06, with the owner's approval of [D14](../../design/plans/completed/d14-home-card-gestures.md))
- Date: 2026-10-06
- Context: `produto`
- Supersedes on acceptance: partially [ADR-012](ADR-012-chat-home-perfil.md), only where it makes the FAB the single way into the Chat from the Home and makes a tap on an empty slot skip it. The rest remains: the Chat is the only place that records; Home has no text field; a tap on a logged meal opens nothing. Changes golds `home0`, `home1`, `homeW`, `homeX`, `chatP` and `cfg` where they show the affected elements (confirmed by each design plan's discovery). Adds gold `cfgR`.

## Context

Owner request on 2026-10-06, closed test:

1. Config needs a way to reset the app so the onboarding can be done again.
2. An empty meal card on the Home says "Toque para pular". A tap should open the Chat to record instead.
3. Skipping stays available on the same card through a long press, with today's skip rule.

The owner chose a full reset, equivalent to a reinstall, and the card copy "Toque para registrar, segura para pular", allowed to wrap.

## Decision

### 1. Empty meal card on the Home

- Applies to the cards that today accept the skip tap: an empty slot of the day (`NEXT` or `EMPTY`) with a slot id.
- Copy: `Nenhum registro · Toque para registrar, segura para pular`. It may wrap to two lines.
- Tap opens the Chat, exactly like the FAB: same screen, same thread, no prefilled text, no slot context sent to the server.
- Long press opens today's confirmation `Pular {nome}?` (`Pular` / `Cancelar`), with a long-press haptic. Confirming records the skip as today.
- Accessibility actions: click labeled `Registrar`, long click labeled `Pular`.
- Logged, skipped, over and `Outros` cards keep no action.

### 2. App reset in Config

- A Config row `Resetar app` opens the confirmation `cfgR`. Cancel or back changes nothing.
- Confirming erases everything the app keeps for the user on this device, like a reinstall: profile, ceilings, macro targets, eat-back policy, meal slots, days, workouts, records, skips, Chat thread, digests, the memory file, Chat photos and push state (scheduled alarms cancelled, shown notifications cleared). Then the app opens O1 empty, with no back stack.
- Kept: the installation id ([ADR-025](../../content-policy/adrs/ADR-025-safety-correlation-audit.md) pseudonymous correlation; a reset is not a way to obtain a new safety identity) and dev-flavor settings that are not user data. Server data is not touched: the server keeps no user state beyond what its contract already logs.
- The reset is idempotent: if interrupted, the next cold start without a finished onboarding completes it before O1.
- Exact copy of the row, the dialog and its buttons is fixed by D15 and recorded in the product spec at the client plan's Completion.

## Motivation

The tap on an empty card is the most natural "I ate this" gesture; using it for skip made recording one step further away than skipping. Long press keeps skip one gesture away while making it deliberate. A full reset gives testers a clean start without reinstalling.

## Consequences

- Home spec rules 4, 7 and the acceptance criteria change at the Android Completion.
- Config spec gains the reset rule; perfil-onboarding states that a reset leads to O1.
- New gold `cfgR`; Home and Config golds are redrawn by D14 and D15 before the client plans.
- The reset deletes data irreversibly; the dialog says so.

## Alternatives

- Redo onboarding keeping records, Chat and memory: rejected by the owner.
- Redo onboarding plus `wipeToday`: rejected by the owner.
- Swipe to skip: hidden and conflicts with the vertical scroll; rejected for long press.
