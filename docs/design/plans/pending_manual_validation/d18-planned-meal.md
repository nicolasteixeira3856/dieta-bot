# Plan — D18 Planned meal: reserve action and timeline state

- Status: Pendente aprovação manual (drawn 07/10/2026; waiting for the owner's review in Figma)
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Chat records and memory" and "Home"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatR,chatRL,homeP}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D3, D6 `Concluído` ([history](../completed/)); `chatR` is shared with [D12](d12-plan-budget-choice.md) and [D17](d17-rich-replies.md): this plan draws on the latest `chatR` frame and runs after them.
- Figma MCP budget: ≤ 60 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-046](../../../produto/adrs/ADR-046-planned-meal-reservation.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d18-planned-meal.md. Implemente o plano aprovado.`

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

Approved by the owner on 06/10/2026 (overnight run, message naming this file). Built by the agent the same night, on the `chatR` frame left by D17 (D12 did not touch `chatR`).

### Discovery (read only, before the first write)

Sources: `chatR` after D17 (72:2959), `home1` (38:229), `Card/Meal` (7:45: Logged, Over, Skipped, Pending, Empty), `Timeline/Node` (6:45), `Chat/ActionBar`, ADR-046 decisions 1 and 4. The Phosphor `calendar-check` (regular) icon is not in the file: its official SVG comes from `@phosphor-icons/core` 2.1.1.

| Gold | Element | Class | Decision |
|---|---|---|---|
| `chatR` | Everything drawn by D17 | app | kept |
| `chatR` | **Registrar assim** + **Reservar para o Jantar** | copy | ADR-046 decision 1; two full-width `Chat/ActionBar` pills stacked (the reserve label carries the slot name and does not fit beside the other at 350 px); calendar-check icon on the new pill |
| `chatRL` | `chatR` + marker `Reservado para o Jantar` below the bubble, **Registrar assim** only | copy | ADR-046 decision 4; new `Chip/Reserved` (calendar-check, `icon/muted`, Caption `text/muted`) |
| `homeP` | `home1` with Jantar planned: dish text and `planejado · 360 kcal`, muted, no chip; ring and macros unchanged (1300) | copy + app | ADR-046 decision 1 |

Components: `Chat/ActionBar` reused for the pill pair (the plan named `Chat/ReceiptAction`, which is the small receipt pill; the action position of `chatR` already uses `Chat/ActionBar`); new `Icon/calendar-check (regular)` and `Chip/Reserved`; the planned slot is a new `State=Planned` variant of `Card/Meal` and of `Timeline/Node` (extending the existing sets instead of a separate `Timeline/SlotPlanned` that would duplicate them).

### Build (Figma `Design`, 07/10/2026)

- Components: `Icon/calendar-check (regular)` 150:235 (official Phosphor SVG, vector bound to `icon/primary`); `Chip/Reserved` 150:236 in "Chat" (TEXT `Label`); `Card/Meal` gains `State=Planned` 150:240 (title and dish in `text/muted`, kcal line muted, no chip; property references rebound after the clone); `Timeline/Node` gains `State=Planned` 150:249 (the skipped outline with calendar-check in `icon/muted`).
- Screens (`Release 1`):

| Gold | Light | Dark | Section |
|---|---|---|---|
| `chatR` (changed: **Reservar para o Jantar** under **Registrar assim**) | 72:2959 | 72:3312 | Chat · D6 |
| `chatRL` (new) | 151:5326 | 151:5399 | Chat · D6 (after `chatRK`) |
| `homeP` (new) | 151:5424 | 151:5539 | Home · D3 (after `homeK`) |

- Layout: Home and Chat · D6 grew by one frame each; the Chat column and "Config e push" moved right (160 px kept); Dark rows below their tallest Light frame; Chat column re-stacked; no overlapping sections.
- Read-back: zero solid paints or text ranges without a variable in the six frames; components as instances (10 to 16 per frame). One fix call: the Jantar node of `homeP` sits inside a `NodeSlot` frame and was set to `Planned` afterwards.
- Review images: the six frames exported with the REST dry run (no MCP calls) and checked in both themes. For the owner: the planned node keeps the dashed outline of the skipped node (with its own icon); the `planejado · 360 kcal` label narrows the dish column to four lines in `homeP`.
- Figma MCP calls: 4 (one read, three writes). Night total after D18: 22 of 120.

### Owner review (the only manual step)

Open `Design` → `Release 1` → "Chat · D6" (`chatR`, `chatRL`) and "Home · D3" (`homeP`), plus the new components and variants. After the OK: map `chatRL` and `homeP` in `tools/export-figma.mjs`, `node tools/export-figma.mjs --only chatR chatRL homeP`, add `chatRL` and `homeP` to the inventory of `docs/qa/README.md`, `node tools/check-figma.mjs`.
