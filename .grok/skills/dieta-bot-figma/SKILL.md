---
name: dieta-bot-figma
description: Build or edit the Dieta Bot Figma file Design (Aero variables, components, flow screens) through the Figma MCP within an approved design plan, run the Figma review gate and export golds and tokens.
---

# Dieta Bot Figma

File `Design`, key qNiqNN3vk9GpmPL3bcV9W1, Figma Student team. Read first: the [design README](../../../docs/design/README.md), [ADR-030](../../../docs/design/adrs/ADR-030-own-design-system-aero.md), [ADR-031](../../../docs/design/adrs/ADR-031-figma-source-of-truth.md), the [Figma review gate](../../../docs/design/plans/README.md#figma-review-gate) and the named approved flow plan. Work only inside that plan.

## Budget

- Call whoami first. Continue only with the Student team seat Full; any other seat is a blocker to report, not a reason to work elsewhere.
- Count every Figma MCP call (skill reads, whoami, use_figma, get_screenshot, get_metadata). Report used and remaining against the plan's budget after each batch, and the total in Results.
- At most 120 calls a day per plan (200/day and 10/minute are the seat limits). Near the cap, stop at a clean point and resume the next day.

## Read before write

- Load the MCP skills before use_figma with get_figma_skill: skill://figma/figma-use/SKILL.md always; skill://figma/figma-generate-library/SKILL.md for variables, styles and components; skill://figma/figma-generate-design/SKILL.md for screens. Pass their names in skillNames with the resource: prefix.
- Inspect the target page, variables and components read-only before the first write. Reuse existing components and variables; extend with properties or variants instead of duplicating.
- Keep a state ledger in the scratchpad (never in the repository): created and mutated node ids, frame ids per gold id and theme, calls used, open fixes. Pass ids between calls as literals.

## Building

- Readability rule (ADR-031 § 3): every page has a title and a short description; groups and flows are titled sections; product order left to right, Light row above Dark row; 40 px between variants, 80 px between components or screens, 160 px between sections; nothing overlaps.
- Frame names: `<gold id> · <Stitch title without the theme suffix> · Light|Dark`. Components and component sets carry a description (purpose, variants, tokens).
- Build Light, then clone the frame for Dark and set the Color collection mode to Dark. No other change in the Dark clone.
- Fills and strokes bind to variables, text uses the text styles, components are instances (never detached). No raw hex outside the variables.
- Feature parity (ADR-031 § 4): the code and the live specifications are the feature inventory. Write the discovery table (`app` / `gold-only` / `copy`) into the plan's Results before any write. A gold element without code is dropped; a spec element missing from the gold is drawn from the spec. Copy is the exact pt-BR text from the code.

## Review and export

- One screenshot per composed frame, sent to the owner; one more per frame after a fix. Then the plan goes to pending_manual_validation/ until the owner's OK in Figma.
- After the OK: map the frame ids in tools/export-figma.mjs (both themes), run node tools/export-figma.mjs --only <ids> and node tools/check-figma.mjs. The inventory source in docs/qa/README.md stays stitch; the flow's client plan switches it.
- Token changes: read the variables and text styles (one use_figma read), rewrite docs/design/tokens.json, run node tools/gen-tokens.mjs. Never hand-edit the Aero section of docs/tokens.md.
- FIGMA_TOKEN lives in the user environment. Never print, echo, log or commit it; check only whether it is set. A missing or refused token is an owner step.

## Known limits

- Unpublished Community libraries (the Phosphor Community file) are invisible to the MCP. Icons are local components built from the official Phosphor SVGs.
- Frames from figma.createAutoLayout come with a white fill: clear it (fills = []) unless the frame needs a bound fill. Left in place it hides Dark rows.
- The Plugin API cannot rename the file or toggle OpenType features; those are owner steps or client checks.
- Page context resets on every use_figma call: switch once per call with setCurrentPageAsync.

Stitch is frozen: no new ST gates and no edits to Stitch Nutri. Its golds stay the layout reference of flows not yet migrated.
