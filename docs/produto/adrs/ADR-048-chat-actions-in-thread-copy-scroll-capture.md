# ADR-048 — Chat actions sit in the thread; messages can be copied; long screens allow a scrolling screenshot

- Status: Proposed
- Date: 2026-10-07
- Context: `produto`
- Supersedes: partially [ADR-026](ADR-026-perguntas-antes-da-estimativa.md) (the place of **Forçar estimativa**: "in the actions position … above the composer"), [ADR-028](ADR-028-registro-autonomo.md) (the place of **Registrar**: "in the actions slot"), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (the place of **Pode passar** · **Ajustar para caber**: "in the action position"). What each button does, when it shows and when it expires stays as those ADRs and [ADR-046](ADR-046-planned-meal-reservation.md) say. Partially [ADR-020](ADR-020-estados-novos-chat-home-horario.md) (closed screen list: `chatCP`, `chatCC` new).

## Context

Testing the 0.0.18 dev build on 2026-10-07, the owner reported three defects of everyday use:

1. Messages cannot be copied. Neither the user's own text nor the Tali's reply can be selected, so a recipe or a plan cannot be pasted anywhere.
2. The phone's scrolling screenshot (Android 12+ "Capturar mais") stopped working after some updates. The owner uses it to share the day and the conversation and wants it to be part of the app.
3. **Registrar assim** and **Reservar para o {slot}** stay fixed above the composer while the conversation scrolls. Today every action of the latest answer (Registrar, Registrar assim, Reservar, Pode passar · Ajustar para caber, Forçar estimativa) lives in one slot pinned above the composer (`Footer` of the Chat screen), as ADR-026, ADR-028 and ADR-039 placed it. The receipt actions and the in-conversation cards (Excluir, Trocar refeição, `chatU`, `chatSD`) already live in the thread under their message, so the Chat has two behaviors for buttons.

## Decision

1. **Actions in the thread.** Every action of an answer sits in the thread directly below that answer (below its budget lines or its `Reservado para o {slot}` mark when present) and scrolls with it, like the receipt actions. The order of a stack stays: Registrar assim, then Reservar; the two pills side by side; Forçar estimativa below the latest question. Nothing stays pinned above the composer except the composer itself. When, how often and until when each action shows is unchanged (ADR-026, ADR-028, ADR-039, ADR-046).
2. **Messages can be copied, as in WhatsApp** (owner decision, 07/10/2026). A long press on a text bubble — the user's or the Tali's — selects the message: the whole row is highlighted and the Chat header gives way to a selection bar with ✕, the count of selected messages and **Copiar** (`chatCP`). While selecting, a tap on another text bubble adds or removes it; removing the last one, ✕ or the system back ends the selection. **Copiar** puts the selected messages' text on the clipboard in conversation order, separated by a blank line, and ends the selection. What is copied is the message text as shown: a formatted reply (ADR-045) gives its plain text (no `**`, table rows as `{item}: {gramas}`), without the estimate card or the day panel; a photo bubble copies its caption, a photo without text is not selectable. Receipts, cards, chips, buttons, the greeting and the meta card are never selected. Android 13 and later confirm the copy with the system clipboard overlay; Android 12 and earlier show the app's own short confirmation `Mensagem copiada` / `{n} mensagens copiadas` (`chatCC`). Sending a message ends the selection. Telemetry counts the copy (number of messages, and whether user and Tali messages were among them), never the text.
3. **Scrolling screenshot.** Every scrollable product screen — Home, Chat, Config and the onboarding steps — offers the system scrolling screenshot on Android 12 and later, capturing its scrollable content. It is a product behavior with an automated regression check, not a side effect of the toolkit.

## Motivation

- A button belongs to the message it acts on. Pinned, it covers the thread and reads as a screen action; in the thread it scrolls away with its message, as every other Chat button already does.
- Copying a plan or a recipe out of the app is a basic expectation of a chat; selecting whole messages and copying them in one tap is the gesture people already know from WhatsApp.
- The scrolling screenshot is how the owner and the testers share a day; losing it silently after an update is a regression that needs a gate.

## Consequences

### Positive

- One rule for Chat buttons: under their message, in the thread.
- Copy works the way people already know from WhatsApp, for one message or several at once.
- The scrolling screenshot works with system UI only.

### Negative

- The golds that draw the actions above the composer (`chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`) change through a design plan before the client changes.
- With a long reply, the actions can be below the fold until the thread is scrolled; the thread already opens at its end, so the latest answer and its actions show first.
- Two new Chat states with golds (`chatCP`, `chatCC`) and a selection bar component; a long press on a bubble now selects it (nothing else in a bubble uses long press today).
- No partial copy of a message: the whole message is copied.

## Alternatives considered

### Keep the pinned slot and add a "jump to message" affordance

Keeps two behaviors for Chat buttons and the cover over the thread. Rejected by the owner's report.

### Platform text selection inside each bubble

Long press, handles and the system bar with Copiar and Selecionar tudo: no new state and partial copy, but copying a whole message takes three steps, several messages cannot be copied at once, and it competes with the long press of the selection. The owner chose the WhatsApp model (07/10/2026).

## Relations

- Specifications affected: [Chat](../specifications/chat.md) (actions placement, message selection and Copiar), [home-timeline](../specifications/home-timeline.md), [memoria-push](../specifications/memoria-push.md) (Config), [perfil-onboarding](../specifications/perfil-onboarding.md) (scrolling screenshot).
- Related ADRs: [ADR-026](ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](ADR-028-registro-autonomo.md), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md), [ADR-045](ADR-045-rich-replies-in-chat-bubbles.md), [ADR-046](ADR-046-planned-meal-reservation.md), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) (golds first).
- Plans: [D20](../../design/plans/d20-figma-review-inline-actions.md) (golds), [A61](../../android/plans/a61-chat-copy-scroll-capture-inline-actions.md) (client).

Once accepted, the body of this ADR does not change. Only the `- Status:` line changes, to record a total or partial supersession by a new ADR.
