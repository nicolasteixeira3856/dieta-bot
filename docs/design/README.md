# design

This README routes. State, version and date live in the owning file.

## Purpose

Design source for the Dieta Bot UI: the own design system "Aero" ([ADR-030](adrs/ADR-030-own-design-system-aero.md)) and every product screen and state, kept in the Figma file `Design` ([ADR-031](adrs/ADR-031-figma-source-of-truth.md)).

## Type and ownership

- Type: design source.
- Executor: the agent, through the Figma MCP (variables, components, screens, gold export). The owner does the visual validation in Figma.
- Code: no app code. Design plans may touch `docs/qa/figma/`, the design tooling in `tools/` and the generated token mirror, as each plan states.
- Figma: file `Design`, team "Figma Student" (`team::1688381275478105695`), file key `qNiqNN3vk9GpmPL3bcV9W1`.

## Scope

- Variables (colors per mode, shape, spacing, sheen, gradient stops, motion), text, effect and paint styles.
- Components and variants, Phosphor icon components.
- Screens per release (`Release 1` page), one section per flow, with Light and Dark rows.
- Gold PNG export and the token mirror.

## Out of scope

- Behavior and copy decisions: [produto](../produto/README.md). A design plan draws only features that exist in the code and live specifications (ADR-031 § feature parity).
- Compose implementation: [android](../android/README.md).
- Branding: the name is [ADR-034](../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md); the logo (oat seed) enters the `Branding` page by [D13](plans/d13-oat-seed-logo.md). Source files: `design/brand/` ([android README § Marca](../android/README.md#marca)).

## Boundaries

- One flow per design plan, within one day of Figma MCP budget ([ADR-031](adrs/ADR-031-figma-source-of-truth.md)).
- A design plan unblocks the matching client plan in [android](../android/README.md).

## How to use this documentation

Follows [docs/sdd/README.md](../sdd/README.md).

1. [Matrix](../README.md) and this README.
2. [ADR-030](adrs/ADR-030-own-design-system-aero.md) and [ADR-031](adrs/ADR-031-figma-source-of-truth.md).
3. The active plan.
4. The Figma file through the MCP (`whoami`, then read before any write).

## Index

### ADRs

Status: the status line of each ADR.

- [ADR-030](adrs/ADR-030-own-design-system-aero.md) — own design system "Aero" replaces Material 3 Expressive.
- [ADR-031](adrs/ADR-031-figma-source-of-truth.md) — Figma file `Design` replaces Stitch as the UI source of truth.

### Plans

Active: the files at the root of [`plans/`](plans/). Execution order and dependencies: [plans/README.md](plans/README.md).

- Completed: [`plans/completed/`](plans/completed/).
