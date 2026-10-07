# Plan — D18 Planned meal: reserve action and timeline state

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Chat records and memory" and "Home"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatR,chatRL,homeP}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D3, D6 `Concluído` ([history](completed/)); `chatR` is shared with [D12](d12-plan-budget-choice.md) and [D17](d17-rich-replies.md): this plan draws on the latest `chatR` frame and runs after them.
- Figma MCP budget: ≤ 60 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d18-planned-meal.md. Implemente o plano aprovado.`

## Objective

Draw the **Reservar para o {slot}** action on the plan bubble, the reserved marker, and the planned slot on the Home timeline, in Aero, both themes.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatR` (changed) | latest `chatR` frame (after D12 and D17) | ADR-046 decision 1: second pill **Reservar para o {slot}** next to **Registrar assim** |
| `chatRL` | `chatR` with the marker `Reservado para o {slot}` below the bubble | ADR-046 decision 4 |
| `homeP` | `home1` with one slot in the planned state | ADR-046 decision 1: dish text and `planejado · {kcal} kcal`, muted, not in the ring |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/{chat,home}`.

## Scope

1. **Discovery (read only):** per gold, every visible element classified `app`, `gold-only` or `copy`; table into Results before any write.
2. **Components:** `Chat/ReceiptAction` reused for the pill pair; `Chip/Reserved` (marker with the calendar-check icon, muted); `Timeline/SlotPlanned` (node marker outlined, muted text, kcal with the `planejado` prefix).
3. **Screens:** Light row `chatR`, `chatRL`, `homeP`, 390 px; Dark row as clones with the `Dark` mode; frame names `<id> · <title> · Light|Dark`.
4. **Owner review** (Figma review gate): one screenshot per frame; `pending_manual_validation/` until the owner's OK.
5. **Export** after the OK: ids in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only chatR chatRL homeP`; `chatRL`, `homeP` added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`.

## Out of scope

- Compose (A58). Server (S27). The receipt difference line (text inside the existing receipt, no new gold).

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back: instances only, no raw hex outside the variables, no overlaps.
3. Visual: one screenshot per frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
