# Plan — D3 Release 1: Home

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Home", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{home0,home1,homeX,homeW}.png`, the Home node ids in `tools/export-figma.mjs`, and the token mirror `docs/design/tokens.json` + `docs/tokens.md` (two new Color variables, added at the owner's review on 2026-10-04).
- Prerequisites: [D1](d1-figma-file-foundation.md) and [D2](d2-figma-tooling-stitch-deprecation.md) `Concluído`.
- Figma MCP budget: ≤ 80 calls.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d3-release1-home.md. Implemente o plano aprovado.`

## Objective

Draw the four Home states in Aero, in both themes, from the design system, with the features the app has today and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) | Behavior source |
|---|---|---|
| `home0` | Home vazia - Day 1 (V2 Expressive Timeline / V2 Light Timeline) | [home-timeline](../../../produto/specifications/home-timeline.md) |
| `home1` | Home no dia - 1300 kcal (…) | same |
| `homeX` | Home meta excedida - 2280 / 2000 kcal (…) | same |
| `homeW` | Home com treino de hoje - Bottom Sheet (…) | same + [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) (eat-back) |

Code: `apps/android/app/src/main/java/com/nutri/android/feature/home/`.

## Scope

1. **Discovery (read only):**
   - per gold, list every visible element and classify it: `app` (exists in code or spec), `gold-only` (dropped) or `copy` (exact pt-BR text from the code);
   - the table goes into Results before any write;
   - an element in the spec but missing from the gold is drawn from the spec.
2. **Components** (in `Componentes`, readability rule):
   - `Ring/Day`: variants `State=Normal|Exceeded|Empty`; consumed vs ceiling, `Exceeded` uses `status/bad`;
   - `Sheet/Bottom`: top radius `radius/sheet`, grabber, glass;
   - `Field/Number`: `Field/Number` style, unit suffix, error state if the app has one;
   - `Header/Day`: "Dia N", date and settings button.

   Reuse the D1 components (`Macro/Row`, `Card/Meal`, `Timeline/Node`, `Chip/Log`, `Button/Primary`, `IconButton/Glass`) and extend them with properties or variants where a Home state needs it (for example the day-1 zero chips).
3. **Screens** (section "Home" on `Release 1`):
   - Light row: `home0`, `home1`, `homeX`, `homeW`, in this order, 390 px wide; `homeW` shows the sheet over a dimmed Home;
   - Dark row: clones with the `Dark` mode, no other change;
   - frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`; spacing per ADR-031 § 3;
   - the pilot Home draft is replaced by `home1` and deleted.
4. **Owner review:**
   - one screenshot per frame (8) is sent to the owner;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma;
   - fixes follow, with one screenshot per changed frame.
5. **Export** after the OK:
   - fill the Home ids in `tools/export-figma.mjs`;
   - `node tools/export-figma.mjs --only home0,home1,homeX,homeW`;
   - `node tools/check-figma.mjs`.

   The inventory source stays `stitch`; [A40](../../../android/plans/a40-home-aero.md) switches it.

## Out of scope

- Compose (A40). Behavior or copy changes. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, and no overlapping nodes.
3. Visual: 8 screenshots; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

Approved by the owner on 2026-10-04 ("Aprovo o plano docs/design/plans/d3-release1-home.md. Implemente o plano aprovado.").

### Discovery (before the first write)

Sources read: `HomePanelScreen.kt`, `HomePanel.kt` (mapper and states), `feature/workout/WorkoutSheet.kt` and `WorkoutEditor.kt`, `core/designsystem` (`SheetActions`, disclaimer copy), `DayBudget.kt` (`metaOn`), the [home-timeline](../../../produto/specifications/home-timeline.md) specification and the four Stitch light golds. Class: `app` = exists in code or spec and is drawn; `gold-only` = dropped; `copy` = exact pt-BR text from the code; `spec` = missing from the gold, drawn from the spec.

**Common to `home0`, `home1`, `homeX` (and the Home behind `homeW`)**

| Element | Class | Figma |
|---|---|---|
| Status bar (clock, signal, battery) | gold-only | dropped (system UI, ignored in QA) |
| Day label `DIA {n}` (`DIA 1`) | copy | `Header/Day` |
| Date `{d} de {mês}` (`25 de setembro`) | copy | `Header/Day` |
| Settings button, top right (content description "Configurações") | app | `IconButton/Glass` + `Icon/gear` in `Header/Day` |
| Ring = kcal consumed over the day's meta | app | `Ring/Day` |
| `kcal consumidas` | copy | `Ring/Day` |
| Pill `Meta {n} kcal`, n = effective ceiling (base + workout credit, `metaOn`) | copy | `Chip/Log` Neutral in `Ring/Day` |
| Macro rows `P Proteína`, `C Carboidratos`, `G Gorduras`, `{consumed} / {target} g`, 8 px bar | copy | `Macro/Row` |
| Row `Treino de hoje` with the value and a chevron | copy | screen row, `Icon/barbell` (D1 decision replaces the Material flame) |
| Header `LINHA DO TEMPO NUTRICIONAL` + `{n} Refeições` | copy | screen row |
| Timeline: continuous guide, one node per slot, one card per slot in hour order | app | `Timeline/Node` + `Card/Meal` |
| Disclaimer `Estimativa nutricional, não substitui consulta médica ou nutricional.` | copy | screen text |
| FAB `Chat`, bottom right | app | `Button/Primary` label `Chat` + `Icon/chat-circle` |

**Per gold**

| Gold | Element | Class | Figma |
|---|---|---|---|
| `home0` | `0` consumed, track only, `Meta 2000 kcal` | app | `Ring/Day` State=Empty |
| `home0` | Macros `0 / 150 g`, `0 / 200 g`, `0 / 67 g`, empty bars | app | `Macro/Row`, `Show fill` off |
| `home0` | Coloured `P`/`C`/`G` letters | gold-only | dropped: the code colours only the dot and the bar |
| `home0` | Workout value `Informar` in the accent | copy | screen row |
| `home0` | Four empty slots `Café da manhã 07:30`, `Almoço 12:30`, `Lanche 16:00`, `Jantar 20:00`, `Nenhum registro · Toque para pular` | copy | `Card/Meal` State=Empty, `Timeline/Node` State=Empty |
| `home0` | `Toque para pular` emphasised; chevron on the text line | gold-only | dropped: the code draws the whole line muted and the chevron after the time |
| `home0` | No highlighted slot | app | the code highlights the next slot only after a filled one (`NEXT` needs `lastFilled >= 0`) |
| `home0` | FAB missing in the gold | spec | drawn (rule 7, "FAB e Config visíveis") |
| `home1` | `1300` consumed, accent arc | app | `Ring/Day` State=Normal |
| `home1` | Pill `Meta 2000 kcal` with a 350 kcal workout at 50 % | gold-only | drawn as `Meta 2175 kcal`: the code's meta is the effective ceiling (2000 + 175) |
| `home1` | Macros `76 / 150 g`, `134 / 200 g`, `40 / 67 g` | copy | `Macro/Row` Over=false |
| `home1` | Workout `350 kcal · +175 na meta` (kcal in text colour, credit muted) | copy | screen row |
| `home1` | Café da manhã logged: line `2 pães franceses, 2 ovos mexidos e café com leite` + `520 kcal`, chip `520 kcal · 28P · 52C · 22G`, check node | copy | `Card/Meal` State=Logged, `Timeline/Node` State=Done |
| `home1` | Almoço from a photo: `Prato feito: frango grelhado, arroz, feijão e salada` + `780 kcal`, chip `780 kcal · 48P · 82C · 18G`, camera node | copy | `Card/Meal` State=Logged, `Timeline/Node` State=Photo |
| `home1` | Lanche skipped: `Refeição pulada`, dashed node with a minus | copy | `Card/Meal` State=Skipped, `Timeline/Node` State=Skipped |
| `home1` | Jantar next: highlighted, `Nenhum registro · Toque para pular` | copy | `Card/Meal` State=Pending, `Timeline/Node` State=Active |
| `home1` | Divider between the log lines and the chip | app | dropped as a separate line: the chip closes the card (layout choice, not a feature) |
| `homeX` | `2280` consumed, full `status/bad` ring | app | `Ring/Day` State=Exceeded |
| `homeX` | `Meta 2000 kcal` + `Meta excedida (+280 kcal)` with a 200 kcal workout at 50 % | gold-only | drawn as `Meta 2100 kcal` + `Meta excedida (+180 kcal)` (effective ceiling, same rule as `home1`) |
| `homeX` | Macros over target in `status/bad` (`168 / 150 g`, `240 / 200 g`, `82 / 67 g`) | app | `Macro/Row` Over=true |
| `homeX` | Workout `200 kcal · +100 na meta` | copy | screen row |
| `homeX` | Café (`2 pães franceses, 2 ovos`, `380 kcal`, `380 kcal · 22P · 48C · 12G`), Almoço photo (`PF de frango grelhado, arroz e feijão`, `780 kcal`, `780 kcal · 64P · 88C · 16G`), Lanche skipped | copy | `Card/Meal` Logged/Skipped |
| `homeX` | Café node as an outlined check | gold-only | drawn as the app's logged node (`Timeline/Node` State=Done) |
| `homeX` | Jantar over: `Pizza brotinho e refrigerante` + `1120 kcal`, chip `1120 kcal · 82P · 104C · 54G`, `!` node, all in `status/bad` | copy | `Card/Meal` State=Over, `Timeline/Node` State=Over |
| `homeW` | Home behind the sheet, blurred and dimmed | app | `home1` content + layer blur + scrim |
| `homeW` | Sheet: grabber, title `Treino de hoje` | copy | `Sheet/Bottom` |
| `homeW` | Field `350` + `kcal`, focused border, workout icon | copy | `Field/Number` State=Focused |
| `homeW` | Credit line `+175 kcal na meta de hoje (compensação 50%)` | copy | `Field/Number` helper |
| `homeW` | `Salvar` / `Cancelar` | copy | `Sheet/Bottom` actions (`Button/Primary` + secondary pill) |
| `homeW` | Field error state | — | none in the app (the field keeps only digits, at most 5), so `Field/Number` has no error variant |

Not drawn (no gold id): the skip confirmation `Pular {nome}?` and the `Outros` block. They exist in the app and get a gold only through a product decision.

### Delivered in Figma (2026-10-04)

**`Componentes`**

- New section `Cabeçalho e anel`:
  - `Header/Day` (props Day, Date; nested `IconButton/Glass` with `Icon/gear`);
  - `Ring/Day`, State = Normal | Exceeded | Empty (prop Consumed; nested `Chip/Log` Neutral for the meta; Empty is always `0`).
- New section `Campos e sheets`:
  - `Field/Number`, State = Default | Focused (props Value, Unit, Helper; no error variant, the app has none);
  - `Sheet/Bottom` (top radius `radius/sheet`, grabber, Surface/Glass + Glass; props Title, Content (instance swap, preferred `Field/Number`), Secondary; exposed nested Content and Primary). The secondary pill is a layer inside the sheet, not a separate component.
- Extended D1 components (instances stay linked):
  - `Chip/Log` became a set, Tone = Accent | Neutral | Bad (Bad adds a dot);
  - `Macro/Row`: Over = false | true (`status/bad` dot, bar and consumed value) and the boolean `Show fill` (day-1 zero bars);
  - `Timeline/Node`: new Over (`!`) and Empty states; Skipped now carries the minus, as the app does;
  - `Card/Meal`: Logged gets the log line (Description + new prop Kcal); new Over and Empty states; the skipped head lost its minus icon.
- New icon component `Icon/exclamation-mark (bold)` (Phosphor core 2.1.1) for the over node.
- Every section refitted to its content, 160 px apart; every component and set has a description.

**Variables.** Two Color variables, because a bound paint takes the variable's alpha and drops its own opacity, so a tint cannot be drawn without a raw value:

- `overlay/scrim`: Light `#03122566`, Dark `#00000099` (dims the Home behind the sheet);
- `status/bad-tint`: Light `#b93b2c1f`, Dark `#ff7a6b29` (Meta excedida pill, over node).

Both are scoped to fills and have Web/Android/iOS code syntax. Mirror regenerated: `docs/design/tokens.json` and the Aero section of `docs/tokens.md` (`node tools/gen-tokens.mjs`, `--check` in sync). The owner kept both colors and added the two mirror files to the plan's "Affected code" line at the review.

**`Release 1` → section `Home · D3`** (1960 × 3400; the `Chat · D5` section moved right to keep 160 px):

| Gold | Light | Dark |
|---|---|---|
| `home0` | `39:302` | `40:430` |
| `home1` | `38:229` | `40:472` |
| `homeX` | `39:451` | `40:514` |
| `homeW` | `39:606` | `40:557` |

- Frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`, 390 px wide, Light row above Dark row, 80 px apart. Dark frames are clones with the Color mode set to Dark and no other change.
- `homeW` is the `home1` content with an 8 px layer blur, an `overlay/scrim` layer and a `Sheet/Bottom` instance at the bottom.
- The hero card, the macro card, the workout row and the timeline rows are layout frames that hold instances; the three edge bubbles of each frame are decoration (ADR-030), absolutely positioned and never behind text.
- The pilot drafts `rascunho · Home · Light|Dark` were deleted.

### Figma MCP budget

28 calls of the 80 budgeted: 1 `whoami`, 3 skill reads, 22 `use_figma` (7 read-only inspections and audits, 14 writes, 1 failed write rolled back by Figma and retried), 2 `get_screenshot`. The review screenshots come from the REST export (`tools/export-figma.mjs --dry-run`), which does not use the MCP.

### Validation

1. Discovery table: written above before the first Figma write.
2. Read-back of the eight frames and of `Componentes`:
   - every DS element is an instance (`Header/Day`, `Ring/Day`, `Macro/Row`, `Timeline/Node`, `Card/Meal`, `Chip/Log`, `Button/Primary`, `Sheet/Bottom`, icons); no detached copy;
   - 0 solid fills or strokes without a variable or a paint style, in the frames and in the touched components. The only unbound gradient is the gloss of the D1 `Button/Primary` (FAB);
   - 0 overlapping in-flow siblings; the absolute layers are the timeline guide and the edge bubbles;
   - `Componentes`: 0 components without a description, gaps between sections 160 (also from the page header), every section fits its content, 0 variables with `ALL_SCOPES`.
3. Visual: the eight frames exported at 2× and sent to the owner on 2026-10-04. Owner review in Figma done the same day, no fixes requested.
4. Export and checks after the OK:
   - `node tools/export-figma.mjs --only home0,home1,homeX,homeW`: 8 new files, 780 px wide (`home0` 2516, `home1` 2828, `homeX` 3032, `homeW` 2828 px tall), byte-identical to the images reviewed by the owner;
   - `node tools/check-figma.mjs`: 8 Figma gold PNGs verified (4 dark + 4 light);
   - `node tools/check-docs.mjs`: passed; `node --test tools/check-docs.test.mjs`: 15/15.

### Owner review (2026-10-04)

The owner reviewed the section `Home · D3` in Figma and closed the plan: "Revisei no Figma, pode exportar os golds e concluir o D3." Decisions on the three review points:

1. Meta values that differ from the Stitch golds (`Meta 2175 kcal` in `home1`, `Meta 2100 kcal` + `Meta excedida (+180 kcal)` in `homeX`): accepted. The Figma golds may show different states from the Stitch golds without affecting the gold validation flow.
2. `overlay/scrim` and `status/bad-tint`: kept; the token mirror files were added to "Affected code".
3. Dropped gold-only elements and the `home0` FAB drawn from the spec: accepted.

### Exported golds

`docs/qa/figma/dark/` and `docs/qa/figma/light/`: `home0.png`, `home1.png`, `homeX.png`, `homeW.png`, mapped in `tools/export-figma.mjs` (`DARK_FRAMES` / `LIGHT_FRAMES`). The inventory source of the four ids stays `stitch` in `docs/qa/README.md`; [A40](../../../android/plans/a40-home-aero.md) switches it.

### Figma MCP budget, total

28 of 80. The review and the closure used no MCP call (export through the REST API).
