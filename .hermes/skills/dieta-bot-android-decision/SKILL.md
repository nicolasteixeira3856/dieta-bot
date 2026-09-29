---
name: dieta-bot-android-decision
description: Evaluate a Dieta Bot product or architecture proposal before a specification or implementation plan, especially when it touches a frozen decision.
---

# Dieta Bot decisions

Start at the [documentation matrix](../../../docs/README.md), then read [AGENTS](../../../AGENTS.md), the owning context and [SDD precedence](../../../docs/sdd/README.md#precedência).

- Explain whether the request implements an existing decision, proposes an unvalidated hypothesis, or changes a frozen decision. Cite the relevant source; offer alternatives only when they help resolve an actual open choice.
- The latest explicit owner decision has precedence. If the owner intentionally changes a frozen decision, document the new specification and, when architectural, a successor ADR; do not reject the owner's request merely because the older rule was frozen.
- Without such an owner decision, retain the current product boundaries. Use [ADR-012](../../../docs/produto/adrs/ADR-012-chat-home-perfil.md), its accepted successors, live specifications and Stitch gates rather than obsolete T0–T3 screen lists.
- New ADRs belong to docs/<context>/adrs/. Accepted ADRs 001–011 stay unchanged in docs/decisions/.
- Planning is documentary. Code starts only after explicit approval identifying the implementation plan. A design requiring new/changed golds first needs a separate owner-run Stitch gate completed in the Stitch context.
- One agent; scope the delivery to its owner. Android and server implementation do not share a client goal. Flutter and RN remain retired.

Do not invent a new screen, stack migration, TDEE integration or eat-back cap as a routine implementation choice.
