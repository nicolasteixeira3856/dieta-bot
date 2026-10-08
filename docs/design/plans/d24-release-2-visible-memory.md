# Plan — D24 Release 2: "O que a Tali sabe"

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Memória", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/memL.png`, the changed `cfg` (entry row), and the node ids in `tools/export-figma.mjs`.
- Prerequisites: [D23](pending_manual_validation/d23-release-2-recipes.md) `Concluído` (same release section order); [ADR-053](../../produto/adrs/ADR-053-visible-memory-screen.md) accepted with A69.
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d24-release-2-visible-memory.md. Implemente o plano aprovado.`

## Objective

Draw the memory screen in Aero, in both themes: the facts Tali keeps, grouped, with origin and the delete and correct actions, as ADR-053 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `memL` | `cfg` rows; the dev tool A23 list (layout only, not a gold) | ADR-053 § 1: groups, category, slot, text, macros, origin, date; delete, correct |
| `cfg` (changed) | current `cfg` | one more entry row "O que a Tali sabe" |

Code: `apps/android/.../feature/config`.

## Scope

1. **Discovery (read only):** per gold, list every visible element and classify it; table in Results before any write.
2. **Components:** fact row (category chip, text, origin line), group header, correction field state.
3. **Screens** (section "Memória" on `Release 2`): Light `memL`, `cfg`; Dark clones; naming and spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate), then **Export**: `node tools/export-figma.mjs --only memL cfg`, inventory, `check-figma`.

## Out of scope

- Compose ([A69](../../android/plans/a69-visible-memory.md)). O6 onboarding. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

<Filled at Completion.>
