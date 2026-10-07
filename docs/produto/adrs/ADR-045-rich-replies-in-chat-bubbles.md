# ADR-045 — Emphasis, lists and a small table inside the Chat bubbles

- Status: Proposto (owner decision of 2026-10-06; the status changes with the first approval among S26, A57 and D17)
- Date: 2026-10-06
- Context: `produto`
- Supersedes: partially [ADR-012](ADR-012-chat-home-perfil.md) rule 2 (bubbles carry plain prose) and [ADR-020](ADR-020-estados-novos-chat-home-horario.md) (closed screen list: `chatR` and `chatE` change, `chatRK` is new). Complements [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) and [ADR-044](ADR-044-assistant-tone-and-closures.md).

## Context

The Chat renders the reply as plain lines; only the P/C/G of a plan get the macro colours (chat rule 16). The reference experience the owner wants to match formats every answer: the decisive numbers in bold, options and ingredients as bullets, preparation as numbered steps, portions in a two-column table, a one-word verdict. Read on a phone, that structure is what makes a 500 kcal plan scannable in two seconds; the same content as a paragraph is not. The owner decided on 2026-10-06 that the design system gains that capability.

## Decision

1. **A fixed subset, nothing else.** The reply may contain: `**bold**` (numbers that decide: kcal, grams, protein; the name of the dish; a one-word verdict), `- ` bullets (options, ingredients, foods to avoid), `1. ` numbered steps (preparation only), and one table of two columns and at most six rows (`| Item | Gramas |`). No headings, links, images, code, emoji, nested lists or more than one table. Line limits of the reply stay; markers do not count toward the 2000 characters of the user text and count toward the reply limits.
2. **The server owns the subset.** The instructions name it; the shaping removes any marker outside it and rewrites a table beyond six rows into bullets. The dev log keeps the raw reply. Totals and macro figures rewritten by [ADR-042](../../server/adrs/ADR-042-estimate-total-is-server-arithmetic.md) keep their bold.
3. **The app renders, never interprets.** A parser of the subset, written in the app (no library), turns the markers into Aero text styles: bold → the strong body style; bullets and steps → rows with the list spacing of the design system; the table → two aligned columns inside the bubble. Unknown or broken markup falls back to the plain line. Markers never reach Room records, receipts, the timeline text or the history sent back to the server beyond the reply itself.
4. **Numbers keep their meaning.** The macro colours of rule 16 apply over the bold; the projected-day panel, receipts and cards stay as they are. Formatting never changes what is recorded.
5. **Golds.** `chatR` (plan with bold numbers, two options as bullets) and `chatE` (log with bold numbers) change; `chatRK` (recipe: ingredient table and numbered steps) is new. Every other state keeps its gold.

## Motivation

- A plan with two options, grams per item and a closing line per slot (ADR-043) is unreadable as prose; the structure is part of the answer.
- A fixed subset keeps the model from inventing layout and keeps the parser small enough to live in the app without a dependency (constitution: no new libraries without a plan).

## Consequences

### Positive

- The answer the user scans first is the number that matters, as in the reference experience.
- Recipes get the ingredient table and steps the cooking rule (ADR-039) already produces as prose.

### Negative

- Three golds change or appear; a design plan, a server plan and a client plan.
- Marker characters raise the reply length; the reply limits are rechecked in the server plan.
- A model that overuses bold makes the bubble shout; the instruction bounds bold to numbers, the dish and a verdict, and the evaluator checks it.

## Alternatives considered

### A Markdown library in the app

Rejected: a dependency for four markers, and a parser the app cannot bound.

### Keep plain prose and move structure into cards

Rejected: cards exist for numbers the app owns (panel, receipts); the structure here is the model's answer and varies per turn.

### HTML or rich text from the server

Rejected: a larger surface for injection through the reply; the subset is text with four markers.

## Relations

- Specifications affected: [chat](../specifications/chat.md) rules 2 and 16, [v1-chat](../../server/specifications/v1-chat.md) rules 3 and 5, [HTTP contract](../../api-contract.md) (`reply` format).
- Related ADRs: ADR-012, ADR-020, ADR-039, ADR-042, ADR-043, ADR-044, [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md).
- Consuming contexts: [design](../../design/README.md) ([D17](../../design/plans/d17-rich-replies.md)), [server](../../server/README.md) ([S26](../../server/plans/s26-reply-formatting-subset.md)), [android](../../android/README.md) ([A57](../../android/plans/a57-rich-reply-rendering.md)).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
