# Plan — D3 Release 1: Home

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Home", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{home0,home1,homeX,homeW}.png` and the Home node ids in `tools/export-figma.mjs`.
- Prerequisites: [D1](completed/d1-figma-file-foundation.md) and [D2](d2-figma-tooling-stitch-deprecation.md) `Concluído`.
- Figma MCP budget: ≤ 80 calls.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d3-release1-home.md. Implemente o plano aprovado.`

## Objective

Draw the four Home states in Aero, in both themes, from the design system, with the features the app has today and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) | Behavior source |
|---|---|---|
| `home0` | Home vazia - Day 1 (V2 Expressive Timeline / V2 Light Timeline) | [home-timeline](../../produto/specifications/home-timeline.md) |
| `home1` | Home no dia - 1300 kcal (…) | same |
| `homeX` | Home meta excedida - 2280 / 2000 kcal (…) | same |
| `homeW` | Home com treino de hoje - Bottom Sheet (…) | same + [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) (eat-back) |

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

   The inventory source stays `stitch`; [A40](../../android/plans/a40-home-aero.md) switches it.

## Out of scope

- Compose (A40). Behavior or copy changes. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, and no overlapping nodes.
3. Visual: 8 screenshots; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
