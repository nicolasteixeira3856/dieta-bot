# ADR-035 — Fibrai and Tali in the app: visible name, assistant label and avatar bubble

- Status: Accepted (2026-10-05, with the owner's named approval of [D10](../../design/plans/completed/d10-fibrai-tali-rename.md))
- Date: 2026-10-05
- Context: `produto`
- Replaces: when [A49](../../android/plans/a49-fibrai-tali-visible-rename.md) completes, the visible-name list of [ADR-016](ADR-016-nome-dieta-bot.md) § Decisão (launcher, splash wordmark, onboarding header, "Chat Dieta Bot", "Dieta Bot AI", the model's self-introduction). It also replaces the [Chat specification](../specifications/chat.md) rule "Sem foto de perfil". The technical IDs are decided in [ADR-036](../../android/adrs/ADR-036-fibrai-technical-identity.md).

## Context

[ADR-034](ADR-034-fibrai-brand-tali-assistant.md) named the product "Fibrai" and the in-app assistant "Tali". The app still shows "Dieta Bot" everywhere, and the Chat shows the assistant only as a lightning icon plus "Dieta Bot AI". The owner wants the assistant to have a face. The owner will draw a cartoon woman, style not decided yet (anime or not). Until then, a temporary image bubble holds the place.

## Decision

1. **Brand surfaces show "Fibrai":**
   - launcher label "Fibrai" (dev: "Fibrai Dev");
   - the splash wordmark;
   - the onboarding header "Fibrai" and its section label "Fibrai Intake".
2. **Assistant surfaces show "Tali":**
   - The Chat header title changes from "Chat Dieta Bot" to "Tali", with the avatar bubble next to it.
   - The AI label above replies changes from "Dieta Bot AI" with the lightning icon to the avatar bubble plus "Tali".
   - No badge, no tagline, no status text.
3. **Avatar bubble:**
   - A circular image slot, one component with two sizes: header and reply label.
   - It is glass-framed per Aero and decorative, with no tap action.
   - For now it shows a temporary image: a "T" monogram on the accent gradient, drawn in Figma and shipped as a drawable.
   - The owner's final art later replaces only the image fill or drawable. Same component, same sizes.
4. **The model introduces itself as Tali, the Fibrai assistant**, and only when the user asks who it is. It has no persona. The tone stays "numbers first, dry, no coach" (AGENTS).
5. **Out of this ADR:**
   - the final avatar art and its style;
   - a brand logo or app icon (the [ADR-034](ADR-034-fibrai-brand-tali-assistant.md) open item);
   - any new screen, navigation or behavior. Layout changes are limited to the label and avatar slots.

## Motivation

- One name per role: the user sees "Fibrai" on brand surfaces and "Tali" where the assistant speaks. This follows the ADR-034 split.
- A placeholder bubble lets Figma, golds and Compose ship now. The final art becomes an asset swap, not a layout change.

## Consequences

### Positive

- Swapping the final art changes one Figma component image and one drawable, then re-exports the affected golds.

### Negative

- The Chat label grows by about one avatar width. [D10](../../design/plans/completed/d10-fibrai-tali-rename.md) checks long-label wrapping.
- Every gold that shows the old name must be redrawn and re-exported: splash, onboarding and Chat. [D10](../../design/plans/completed/d10-fibrai-tali-rename.md) confirms the exact list during discovery.
- The final art needs a small follow-up design and client delivery when it exists.

## Alternatives considered

- **Text label only ("Tali"), no image:** rejected, because the owner asked for an image bubble.
- **Wait for the final art before renaming:** rejected, because it blocks the rename on an undated decision.
- **Phosphor icon instead of a monogram:** possible, but a generic icon carries no name. The owner can choose it at the D10 review.

## Relations

- [ADR-034](ADR-034-fibrai-brand-tali-assistant.md) (names), [ADR-016](ADR-016-nome-dieta-bot.md) (visible-name predecessor), [ADR-012](ADR-012-chat-home-perfil.md) (screens), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) (Figma gate).
- Plans: [D10](../../design/plans/completed/d10-fibrai-tali-rename.md), [S20](../../server/plans/s20-tali-prompt-identity.md), [A49](../../android/plans/a49-fibrai-tali-visible-rename.md).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
