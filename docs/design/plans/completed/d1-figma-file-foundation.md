# Plan — D1 Figma file foundation

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none. Figma file `Design` (key `qNiqNN3vk9GpmPL3bcV9W1`) only.
- Prerequisites: [ADR-030](../../adrs/ADR-030-own-design-system-aero.md) and [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md) accepted by the owner (approving this plan accepts both).
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

1. **File and pages** ([ADR-031](../../adrs/ADR-031-figma-source-of-truth.md) § 2):
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

Implemented 2026-10-03 after the owner's named approval. Closed `Concluído` on 2026-10-04 after the owner's manual review (validation item 4).

### Figma MCP budget

24 calls of the 60 budgeted: 1 skill read, 1 `whoami`, 17 `use_figma` (3 read-only inspections, 5 writes, 1 failed write rolled back by Figma and retried, 4 read-backs, 4 fix-ups), 5 `get_screenshot`.

### Delivered

1. **File and pages.** Pages in order: `Branding`, `Cores e tipografia`, `Componentes`, `Release 1`. `Cover` deleted. `Branding` holds its title and "Nome, logo e marca: a definir.". Every page opens with a title and a short description.
   - **Not done by the agent: the file rename to `Design`.** The Plugin API refuses it ("Setting the document name is currently not supported"). The owner renames the file in Figma; the file key does not change.
   - `Release 1` (the former `Pilot` page) has three sections in product order, left to right: `Splash e onboarding · D4` (O2), `Home · D3`, `Chat · D5`, Light row above Dark row. The pilot frames are named `rascunho · <title> · Light|Dark` instead of a gold id, because they are drafts until their flow plan rebuilds them.
2. **`Cores e tipografia`.** Six sections: Cores (23 rows in 11 role groups, Light and Dark swatches over their own `bg/page`, hex with alpha, description, scopes, Android code syntax), Contraste, Tipografia, Efeitos e pinturas, Forma e espaço, Movimento.
   - Contrast fixes made in the variables, never only in the table (worst pair before → after):
     - Light `text/dim` #6E889E → #546A7D (3.03 → 4.61 on `surface/2`);
     - Dark `text/dim` #6F90AE → #819EB8 (3.88 → 4.65 on `surface/2`);
     - Light `status/good` #1F7A34 → #1E7733 (4.42 → 4.60);
     - Light `status/bad` #C8402F → #B93B2C (4.07 → 4.62);
     - Light `macro/carbs` #A8520A → #A4500A (4.44 → 4.61);
     - Light `macro/fat` #866A00 → #7F6400 (4.22 → 4.62).
   - After the fixes all 20 text pairs pass 4.5:1 on `bg/page`, `surface/glass` (composited over `bg/page`) and `surface/2`, in both modes. Worst pair: Light `macro/protein` on `surface/2`, 4.55:1. The CTA label `accent/on` on `accent/default`: 5.77:1 Light, 8.24:1 Dark.
   - Side effect: Light `text/dim` is now close to `text/muted` (#486781). Flow plans keep the hierarchy through size and weight, not through that color gap.
   - Out of the table: glass over `bg/top` (top of the page gradient) lowers the ratios (for example Light `text/muted` about 4.49:1). Each flow plan checks its own screens.
   - `tnum`: in Hero/Number, "1111" and "0000" both measure 82.0 px, so no fallback is needed. The Plugin API cannot toggle OpenType features. The client still sets `fontFeatureSettings "tnum"` and measures the bundled font.
   - New `Motion` collection (mode `Value`, 6 variables): `motion/fast` 150, `motion/base` 250, `motion/slow` 400 (FLOAT); `easing/standard` cubic-bezier(0.2, 0, 0, 1), `easing/emphasized` cubic-bezier(0.05, 0.7, 0.1, 1), `easing/exit` cubic-bezier(0.3, 0, 1, 1) (STRING, value and description). Scopes are empty (no Figma property takes them). Android, iOS and Web code syntax are set on every variable.
   - Every variable in the three collections now has a description.
3. **`Componentes`.** 8 family sections in the planned order. Component sets use 40 px between variants (auto layout, padding 40), components sit 80 px apart and sections 160 px apart. The icon set is a 6-column grid of 17 labeled cells (name + weight). The old `Icons (Phosphor)` board was removed; there were no other preview boards. All 41 components and component sets have a description: purpose, variants, props and the tokens and styles read from their bindings.
   - The `microphone` and `sliders-horizontal` icon components stay in the set, unused. Deleting them was not in scope.
4. **Parity fixes.**
   - `Chat/Composer`: microphone removed; the placeholder is "Descreva sua refeição ou envie foto...".
   - Chat Light and Dark: the tune button is replaced by an empty 44 px spacer, and the bot label reads "Dieta Bot AI".
   - `barbell` kept.
   - Open for D5: at 16 px (style Body) the placeholder truncates in the 350 px composer; the app uses 14 sp (`bodyMd`).
5. **Repository:** ADR-030 and ADR-031 `Accepted`; `docs/design/README.md` lost the old-name line; this plan moved to `pending_manual_validation/` and the links were updated.

### Validation

1. `whoami`: Figma Student, seat Full.
2. Structural read-back:
   - page names and order as planned;
   - Color 23 variables (Light/Dark), Shape 11 (Value), Motion 6 (Value);
   - 0 variables with `ALL_SCOPES`, 0 without Android/iOS code syntax;
   - 0 components without a description;
   - 0 overlapping top-level nodes and 0 overlapping siblings inside sections or component sets, on every page;
   - gaps between sections exactly 160 (also from the page header to the first section);
   - component rows 80, component sets 40, Light/Dark rows 80.
3. Screenshots: one per page, sent to the owner in the session. The first pass found two legibility defects, both fixed and screenshotted again:
   - default white fills on auto-layout cells hid the Dark rows of the contrast table and the Dark effect labels;
   - a truncated typography table header.
4. Manual (done 2026-10-04): the owner renamed the file to `Design`, reviewed `Cores e tipografia` and `Componentes` and closed the plan: "Renomeei e revisei, pode concluir o D1."
5. `node tools/check-docs.mjs`: passed (52 live files, 55 link-checked).
