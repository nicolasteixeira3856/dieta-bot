---
name: dieta-bot-android-ui
description: Build or revise Dieta Bot Compose screens, timeline, Chat composer and Expressive controls within a named approved plan and the gold of each id's source in the inventory.
---

# Dieta Bot Compose UI

Use [AGENTS](../../../AGENTS.md), [tokens](../../../docs/tokens.md), the owning live specification and the gold of each id from its source (figma or stitch) in the [gold inventory](../../../docs/qa/README.md). Implementation follows golds only. New/changed golds need the completed design plan (Figma review gate).

Keep AeroTheme ([aero-compose](../aero-compose/SKILL.md)), both system-following palettes and semantic macro colors. No dynamic color. Use the gold/spec-specific shapes and typography: do not replace a Chat bubble or multiline composer shape with a generic card radius. The highlighted remaining value, CTA and sheet tokens remain those in the constitution.

- Use screen/flow-scoped ViewModels, lifecycle state collection and children receiving state/callbacks.
- Preserve the Home FAB opening Chat. Read [Chat](../../../docs/produto/specifications/chat.md) and [Home](../../../docs/produto/specifications/home-timeline.md) for current states rather than an obsolete screen list.
- Timeline entries are consolidated; guide and nodes follow the gold. Copy is pt-BR, numbers first, dry and without coaching.
- Day-one Home state and Chat suggestion chips are distinct. Follow their matching gold/specification; do not suppress all chips.
- State annotations must reflect real nested immutability; a read-only collection alone is not an immutable backing store.

Run verifyRoborazziDevDebug for visual regression and relevant dev behavior/build checks. Then use the [visual skill](../dieta-bot-android-visual/SKILL.md) for fresh emulator captures in both themes, measured layout bounds and the written gold comparison. Neither JVM screenshots nor layout JSON alone finish changed UI.

For uncertain Android/M3 APIs, use Android CLI documentation search when available, or authoritative Android documentation. Do not guess an API or upgrade dependencies just to apply a generic recipe.
