# ADR-031 — Figma file `Design` replaces Stitch as the UI source of truth

- Status: Accepted (2026-10-03, with the owner's named approval of [D1](../plans/completed/d1-figma-file-foundation.md))
- Date: 2026-10-03
- Context: `design`
- Replaces: once accepted, the owner-run Stitch gate (`docs/sdd/README.md` § Gate Stitch, AGENTS "Visual QA" and "Stitch gate") for each flow whose Figma plan is completed; after the last flow, for the whole app. [ADR-008](../../decisions/008-visual-qa.md) keeps its folder law for captures and `_legacy/`. [ADR-027](../../android/adrs/ADR-027-golds-divergentes.md) keeps its divergence rules, applied to Figma golds.

## Context

Today Google Stitch project `Nutri` is the layout source of truth. Every new or changed gold needs a gate: the agent writes a prompt, the owner pastes it into Stitch, and the agent verifies and exports. During the 2026-10-03 pilot, Stitch:

- turned the design system into Material color roles;
- marked dark screens as light;
- regenerated whole screens on every edit;
- invented content (a bottom nav, badges, disclaimers, a microphone) and ignored the requested icon library;
- offered no components, no variants and no token modes.

The Figma remote MCP server reads and writes the canvas (variables, styles, components, auto layout). The owner's Figma Student team gives a Full seat: 200 MCP calls a day, 10 a minute (verified with `whoami` on 2026-10-03). The pilot used about 25 calls for the foundations, 17 icons, 10 components and 6 screens.

## Decision

1. **Source of truth.** The Figma file `Design` (Student team) is the source of truth for the design system ([ADR-030](ADR-030-own-design-system-aero.md)) and for every product screen and state. The gold of a screen is a PNG exported from its Figma frame. Code and live specifications remain the source of *behavior*.
2. **File structure** (one file, several pages, in this order):
   - `Branding`: future name, logo and brand assets. Empty until the owner defines a brand.
   - `Cores e tipografia`: every variable with its mode values, text styles, effect and paint styles, radii, spacing, motion and contrast notes.
   - `Componentes`: every component and variant, grouped and documented.
   - `Release 1`: the current app, one section per flow (Splash and Onboarding, Home, Chat, Config and push).

   A future release gets its own `Release <n>` page; earlier releases stay frozen as history.
3. **Readability rule (owner requirement).** A person who has never seen the file must be able to read it:
   - every page has a title and a short description;
   - groups and flows are Figma sections with a title;
   - items follow product order (flow order left to right, the light row above the dark row);
   - spacing is fixed: 40 px between variants, 80 px between components or screens, 160 px between sections;
   - nothing overlaps;
   - frame names are `<gold id> · <title> · Light|Dark`.
4. **Feature parity.** The Figma screens reproduce what the app does today. The feature inventory is the current code plus the live specifications. A Stitch gold only confirms the layout of a feature that exists. An element drawn only in a gold is dropped (for example the microphone in the `chatE` composer and the tune button in the Chat header). A new feature needs its own product ADR first.
5. **Who does what.**
   - The agent builds and edits the file through the Figma MCP: variables, components and screens. It keeps the readability rule and exports the golds.
   - The owner does only the visual validation in Figma. There is no prompt to paste.
   - The owner-run Stitch gate disappears for every migrated flow. It is replaced by the **Figma review gate**: build → owner visual review → fixes → owner OK → gold export → `Concluído`.
6. **One flow per plan.** Each design plan covers one flow and stays within one day of MCP budget: at most 120 calls, leaving headroom under the 200/day limit. A plan that hits the cap stops at a clean point and resumes the next day. Calls are counted in the plan's Results.
7. **Golds.**
   - Golds are exported at 2× (780 px wide) into `docs/qa/figma/{dark,light}/<id>.png` through the Figma REST image export. The script uses a personal access token from the environment (`FIGMA_TOKEN`), never printed or committed.
   - Gold ids stay the same (`home1`, `chatE`…), so client tests and captures keep their names.
   - The gold inventory in `docs/qa/README.md` records the source (`stitch` or `figma`) of each id.
   - The design plan of a flow exports its Figma golds. The flow's gate source switches to `figma` only when its **client** plan completes, because until then the app still draws the old look and must keep passing against its Stitch golds.
8. **Tokens mirror.** The agent reads the Figma variables (MCP) and writes a generated mirror to the repository. Human-readable values stay in `docs/tokens.md`, generated with a header that forbids hand edits. The Figma REST variables endpoint needs an Enterprise plan, so this sync is agent-run, not a CI job.
9. **Stitch deprecation.**
   - While flows migrate, Stitch is a frozen, read-only reference: no new ST gates, no edits to `Nutri`.
   - A flow's Stitch golds stay in `docs/qa/stitch/` until that flow's client plan switches it to `figma`.
   - After the last flow, the Stitch tooling, the `stitch` context, the `dieta-bot-stitch` skill and `docs/qa/stitch/` are retired by an archive plan (moved to history, not deleted from git).
10. **Icons in Figma.** Icons are local components built from the official Phosphor SVG package. They are not a dependency on the Phosphor Community plugin or file: the plugin is a human insertion tool, and an unpublished library is invisible to the MCP.

## Motivation

- Variables with modes, components with variants and surgical edits are what a design system needs; Stitch provides none of them.
- The agent can do the design work end to end; the owner's time goes to judging, not to pasting prompts.
- Exact values read from Figma make the client comparison more precise than pixel measuring.

## Consequences

### Positive

- One component tree for light and dark; a design system change propagates to every screen.
- Golds come from structured frames, not regenerated HTML.
- The owner's manual step shrinks to a visual check.

### Negative

- New tooling (REST export, token sync, a Figma skill) and a migration period with two gold sources.
- A dependency on the Student plan's Full seat. If the seat changes, MCP drops to 6 calls a month and design work stops until the plan is restored.
- MCP write-to-canvas is a beta that will become usage-based; costs will be revisited when that happens.
- Figma does not generate screens from a prompt; exploration still costs agent calls. Stitch may stay as an optional sketchpad, never as a source.

## Alternatives considered

### Keep Stitch as the source

Rejected for the reasons in Context.

### Figma with the owner drawing and the agent only reading

It wastes the write access and moves design labor to the owner, which is the opposite of the goal.

### Phosphor as a published team library

Possible later (publish the Community file in the Student team). It is not needed: local components from the official SVG package give the same icons with a smaller file, and MCP search works on them.

## Relations

- Specifications affected: none in behavior. `docs/qa/README.md` (inventory with source), `docs/tokens.md` (generated) and `AGENTS.md` (Visual QA, Stitch gate, skills) change in the design tooling plan.
- Related ADRs: [ADR-008](../../decisions/008-visual-qa.md), [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md), [ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md) (dev tools still have no gold), [ADR-027](../../android/adrs/ADR-027-golds-divergentes.md), [ADR-030](ADR-030-own-design-system-aero.md).
- Consumer contexts: [android](../../android/README.md), [stitch](../../stitch/README.md) (deprecated by this ADR).
