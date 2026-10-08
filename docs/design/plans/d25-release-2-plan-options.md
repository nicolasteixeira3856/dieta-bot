# Plan — D25 Release 2: Plan options in the bubble

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Opções", new component in `Componentes`. Repository: `docs/qa/figma/{dark,light}/chatO.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: [D24](pending_manual_validation/d24-release-2-visible-memory.md) `Concluído` (same release section order); [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md) accepted with S37.
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d25-release-2-plan-options.md. Implemente o plano aprovado.`

## Objective

Draw a plan bubble with two options in Aero, in both themes: each option with its name, items with grams, total and protein, and its own action row (Registrar, Reservar), as ADR-051 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatO` | `chatR` (plan bubble) and `chatRL` (reserve action row) | ADR-051 § 1: `Opção 1: {name}` / `Opção 2: {name}`, items, totals, protein; ADR-048 actions per option |

Code: `apps/android/.../feature/chat`.

## Scope

1. **Discovery (read only):** list every visible element of `chatR` and `chatRL` and classify it; table in Results before any write.
2. **Component:** option block (title, item lines, total line, action row), two instances in the bubble; readability rule of ADR-031 § 3.
3. **Screen** (section "Opções" on `Release 2`): Light `chatO`; Dark clone; naming and spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate), then **Export**: `node tools/export-figma.mjs --only chatO`, inventory in `docs/qa/README.md`, `node tools/check-figma.mjs`.

## Out of scope

- Compose ([A67](../../android/plans/a67-plan-options-and-discovery.md)). The discovery message (text only, no new layout). Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

<Filled at Completion.>
