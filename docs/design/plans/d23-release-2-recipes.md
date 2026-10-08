# Plan — D23 Release 2: Recipes

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Receitas", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{rcpL,rcpD}.png`, the changed `chatRK` (Salvar receita action), and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D22 `Concluído` ([history](completed/)); [ADR-052](../../produto/adrs/ADR-052-saved-recipes.md) accepted with S38.
- Figma MCP budget: ≤ 60 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d23-release-2-recipes.md. Implemente o plano aprovado.`

## Objective

Draw the recipe list and detail in Aero, in both themes, from the design system, plus the "Salvar receita" action under a cooking plan, with the features ADR-052 defines and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `rcpL` | `cfg` list rows (Config) | ADR-052 § 2: most recent first, name, kcal · P/C/G |
| `rcpD` | `chatRK` table and steps | ADR-052 § 1: ingredients with grams, steps, totals, version, delete |
| `chatRK` (changed) | current `chatRK` | ADR-048 action row + "Salvar receita" |

Code: `apps/android/.../feature/config`, `feature/chat`.

## Scope

1. **Discovery (read only):** per gold, list every visible element and classify it: `app`, `gold-only` (dropped) or `copy`; the table goes into Results before any write.
2. **Components:** recipe row (name, totals), recipe header (name, version, totals), reuse of the table and numbered list of `chatRK`.
3. **Screens** (section "Receitas" on `Release 2`): Light row `rcpL`, `rcpD`, `chatRK`; Dark clones; frame names `<id> · <title> · Light|Dark`.
4. **Owner review** (Figma review gate): screenshots per frame; plan to `pending_manual_validation/` until the owner's OK.
5. **Export** after the OK: ids in `tools/export-figma.mjs`, `node tools/export-figma.mjs --only rcpL rcpD chatRK`, inventory in `docs/qa/README.md`, `node tools/check-figma.mjs`.

## Out of scope

- Compose ([A68](../../android/plans/a68-saved-recipes.md)). Behavior or copy changes. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

<Filled at Completion.>
