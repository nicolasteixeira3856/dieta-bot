# ADR-048 — Chat actions sit in the thread; messages can be copied; long screens allow a scrolling screenshot

- Status: Proposed
- Date: 2026-10-07
- Context: `produto`
- Supersedes: partially [ADR-026](ADR-026-perguntas-antes-da-estimativa.md) (the place of **Forçar estimativa**: "in the actions position … above the composer"), [ADR-028](ADR-028-registro-autonomo.md) (the place of **Registrar**: "in the actions slot"), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (the place of **Pode passar** · **Ajustar para caber**: "in the action position"). What each button does, when it shows and when it expires stays as those ADRs and [ADR-046](ADR-046-planned-meal-reservation.md) say.

## Context

Testing the 0.0.18 dev build on 2026-10-07, the owner reported three defects of everyday use:

1. Messages cannot be copied. Neither the user's own text nor the Tali's reply can be selected, so a recipe or a plan cannot be pasted anywhere.
2. The phone's scrolling screenshot (Android 12+ "Capturar mais") stopped working after some updates. The owner uses it to share the day and the conversation and wants it to be part of the app.
3. **Registrar assim** and **Reservar para o {slot}** stay fixed above the composer while the conversation scrolls. Today every action of the latest answer (Registrar, Registrar assim, Reservar, Pode passar · Ajustar para caber, Forçar estimativa) lives in one slot pinned above the composer (`Footer` of the Chat screen), as ADR-026, ADR-028 and ADR-039 placed it. The receipt actions and the in-conversation cards (Excluir, Trocar refeição, `chatU`, `chatSD`) already live in the thread under their message, so the Chat has two behaviors for buttons.

## Decision

1. **Actions in the thread.** Every action of an answer sits in the thread directly below that answer (below its budget lines or its `Reservado para o {slot}` mark when present) and scrolls with it, like the receipt actions. The order of a stack stays: Registrar assim, then Reservar; the two pills side by side; Forçar estimativa below the latest question. Nothing stays pinned above the composer except the composer itself. When, how often and until when each action shows is unchanged (ADR-026, ADR-028, ADR-039, ADR-046).
2. **Messages can be copied.** The text of every chat bubble — the user's and the Tali's — can be selected with the platform text selection (long press, handles, the system bar with Copiar and Selecionar tudo). What is copied is the text as shown: a formatted reply (ADR-045) gives its words and numbers without markup; a reply's day panel and estimate card numbers are part of the bubble. Receipts, cards, chips and buttons are not selectable. Nothing about a copy is logged or sent.
3. **Scrolling screenshot.** Every scrollable product screen — Home, Chat, Config and the onboarding steps — offers the system scrolling screenshot on Android 12 and later, capturing its scrollable content. It is a product behavior with an automated regression check, not a side effect of the toolkit.

## Motivation

- A button belongs to the message it acts on. Pinned, it covers the thread and reads as a screen action; in the thread it scrolls away with its message, as every other Chat button already does.
- Copying a plan or a recipe out of the app is a basic expectation of a chat; the platform selection gives it without a new screen.
- The scrolling screenshot is how the owner and the testers share a day; losing it silently after an update is a regression that needs a gate.

## Consequences

### Positive

- One rule for Chat buttons: under their message, in the thread.
- Copy and scrolling screenshot work with system UI only; no new product screen.

### Negative

- The golds that draw the actions above the composer (`chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`) change through a design plan before the client changes.
- With a long reply, the actions can be below the fold until the thread is scrolled; the thread already opens at its end, so the latest answer and its actions show first.
- Text selection adds a long-press gesture to the bubbles; nothing else in a bubble uses long press today.

## Alternatives considered

### Keep the pinned slot and add a "jump to message" affordance

Keeps two behaviors for Chat buttons and the cover over the thread. Rejected by the owner's report.

### A long-press menu with a single "Copiar" for the whole message

One tap fewer for a whole message, but a new product state with its own gold and component, and no partial copy. The platform selection's Selecionar tudo + Copiar covers the whole message. Can be added later as its own decision.

## Relations

- Specifications affected: [Chat](../specifications/chat.md) (actions placement, selectable bubbles), [home-timeline](../specifications/home-timeline.md), [memoria-push](../specifications/memoria-push.md) (Config), [perfil-onboarding](../specifications/perfil-onboarding.md) (scrolling screenshot).
- Related ADRs: [ADR-026](ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](ADR-028-registro-autonomo.md), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md), [ADR-045](ADR-045-rich-replies-in-chat-bubbles.md), [ADR-046](ADR-046-planned-meal-reservation.md), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) (golds first).
- Plans: [D20](../../design/plans/d20-figma-review-inline-actions.md) (golds), [A61](../../android/plans/a61-chat-copy-scroll-capture-inline-actions.md) (client).

Once accepted, the body of this ADR does not change. Only the `- Status:` line changes, to record a total or partial supersession by a new ADR.
