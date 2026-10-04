# Plan — D1 Figma file foundation

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none. Figma file `Design` (key `qNiqNN3vk9GpmPL3bcV9W1`) only.
- Prerequisites: [ADR-030](../adrs/ADR-030-own-design-system-aero.md) and [ADR-031](../adrs/ADR-031-figma-source-of-truth.md) accepted by the owner (approving this plan accepts both).
- Figma MCP budget: ≤ 60 calls.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d1-figma-file-foundation.md. Implemente o plano aprovado.`

## Objective

Turn the 2026-10-03 pilot file ("Dieta Bot DS") into the organized, readable `Design` file that the flow plans build on. No new screens: only structure, foundations, component hygiene and the pilot's parity fixes.

## Starting point (pilot, 2026-10-03)

- Pages `Cover` (empty), `Foundations` (empty), `Components`, `Pilot`.
- Variables:
  - `Color`, modes `Light`/`Dark`: 23 colors, including the gradient stops `bg/top` and `bg/mid` and the sheen stops `sheen/start` and `sheen/end`;
  - `Shape`, mode `Value`: 11 radius, space, size and blur tokens.
- Styles:
  - text: 9 Nunito Sans styles;
  - paint: `Background/Page`, `Surface/Glass`, `Gloss/Button`;
  - effect: `Glass`, `Glow/Accent`.
- Components:
  - 17 Phosphor icons;
  - `Button/Primary`, `IconButton/Glass`, `Chip/Log`, `Progress/Bar`;
  - `Macro/Row` (3 variants), `Timeline/Node` (4), `Option/Card` (2), `Card/Meal` (3), `Chat/Bubble` (2), `Chat/Composer`.
- Screens: `Home`, `Chat`, `O2`, each `Light` and `Dark`. Dark is a clone with the `Dark` mode.

## Scope

1. **File and pages** ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 2):
   - rename the file to `Design`;
   - pages, in order: `Branding`, `Cores e tipografia`, `Componentes`, `Release 1`;
   - delete the empty `Cover` page; `Foundations` becomes `Cores e tipografia`, `Components` becomes `Componentes`;
   - `Branding` holds only its title and the note "Nome, logo e marca: a definir.";
   - the pilot screens move to `Release 1`, under the sections of D3 (Home), D4 (O2) and D5 (Chat). They stay drafts until their flow plan rebuilds them.
2. **`Cores e tipografia`** (complete and readable):
   - Color table: one row per variable. Columns: name, Light swatch + hex/alpha, Dark swatch + hex/alpha, usage and scope. Rows grouped by role: page, surface, border, text, icon, accent, status, macro, shadow, gradient and sheen stops.
   - Contrast table: every text and macro color against `bg/page`, `surface/glass` (composited) and `surface/2`, in both modes, with its WCAG ratio. Every pair that carries text must pass 4.5:1 (3:1 for large text or graphics). A failing pair is fixed in the variable, never only in the table.
   - Type specimen: each text style with family, weight, size, line height, letter spacing and a pt-BR sample with numbers ("1.240 kcal · 28P · 52C · 22G").
   - The `tnum` check for Nunito Sans: render "1111" and "0000" in one style; if the widths differ, record the fallback the client plan must apply.
   - Effects, paint styles and shape tokens: radii, spacing scale, bar height, blur, each with a specimen.
   - New `Motion` collection (mode `Value`): `motion/fast` 150 ms, `motion/base` 250 ms, `motion/slow` 400 ms, and the easing names `standard`, `emphasized` and `exit` with their cubic-bézier values in the description. Code syntax for Android and iOS on every variable.
3. **`Componentes`** (readability rule, ADR-031 § 3):
   - one section per family (Icons, Buttons, Chips and progress, Macros, Timeline, Cards, Options, Chat), in product order;
   - 40 px between variants, 80 px between components, 160 px between sections, nothing overlapping;
   - every component and component set has a description (purpose, variants, tokens);
   - the icon set is a grid labeled with each icon's name and weight;
   - the internal frame names left by the pilot (`Head`, `Text`, `Meta`) stay; temporary preview boards are removed.
4. **Pilot parity fixes** (feature parity, ADR-031 § 4; checked against `apps/android/.../feature/chat/ChatScreen.kt`):
   - `Chat/Composer`: remove the microphone; the placeholder becomes the app copy "Descreva sua refeição ou envie foto...";
   - Chat header: remove the tune/sliders button and keep its 44 px space, as the app does; the bot label becomes "Dieta Bot AI";
   - workout icon: keep Phosphor `barbell` (icon choice is a design decision; the app's current flame icon is Material and goes away).
5. **Repository:**
   - ADR-030 and ADR-031 status lines become `Accepted (<approval date>)`;
   - `docs/design/README.md` drops its line about the old file name;
   - this plan's Results.

## Out of scope

- New screens or states (D3–D7).
- Gold export and repository tooling (D2).
- Branding content.

## Validation

1. `whoami` confirms the Student team with a Full seat before any write.
2. Structural read-back (`use_figma`, read only):
   - page names and order;
   - variable count per collection and mode;
   - no variable with `ALL_SCOPES`;
   - every component has a description;
   - no overlapping top-level nodes on any page (bounding-box check);
   - spacing between siblings follows the readability rule.
3. One screenshot per page, sent to the owner.
4. Manual: the owner reads `Cores e tipografia` and `Componentes` without explanation and confirms they are understandable. Until then the plan stays `Pendente aprovação manual`.
5. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion: MCP calls used, counts, screenshots sent, owner confirmation.>
