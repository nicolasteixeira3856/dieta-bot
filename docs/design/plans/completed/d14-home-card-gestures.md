# Plan — D14 Home: empty card copy (register and skip)

- Status: Concluído (approved 06/10/2026; owner OK in Figma 06/10/2026)
- Date: 06/10/2026
- Owning context: `design`
- Executable boundary: the Home flow in Figma `Design`; no app/server code. Repository outputs: re-exported Home golds in `docs/qa/figma/{dark,light}/`, `tools/export-figma.mjs` only if a node id changes, and design documentation.
- Prerequisites: [ADR-040](../../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) accepted by the approval of this plan or of [D15](../d15-config-app-reset.md).
- Figma MCP budget: at most 40 calls, within the ceiling and rollover rules of [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d14-home-card-gestures.md. Implemente o plano aprovado.`

## Objective

Replace the empty meal card copy with `Nenhum registro · Toque para registrar, segura para pular` (ADR-040 decision 1) in every Home gold that shows an empty card, for [A52](../../../android/plans/a52-home-card-gestures.md).

## Sources

Behavior and copy: ADR-040. Layout: the live Home section and the `Meal card` component in Figma `Design`, [home-timeline](../../../produto/specifications/home-timeline.md) rule 4, the [gold inventory](../../../qa/README.md#golds).

| Gold | Change |
|---|---|
| `home0`, `home1`, `homeW`, `homeX` | Empty/next cards get the new copy; it may wrap to two lines; card height grows with it. |
| `chatP` | Same copy on the Home behind the blurred skip dialog. Dialog unchanged. |

The discovery confirms which of these frames actually show an empty card; a frame without one keeps its bytes.

## Scope

1. **Discovery before any write.** Read the Home frames, `chatP` and the `Meal card` component (Empty and Pending variants). Record in Results each frame that contains the old copy and the node ids.
2. **Component.** Change the description text of the Empty and Pending variants (or their instances' text override) to the new copy, wrapping allowed, existing caption style and colors. No new variant, no new token.
3. **Frames.** Light first, Dark by variable mode. Check that the timeline guide and node markers still line up with the taller card, the disclaimer still clears the FAB, and nothing else moves.
4. **Owner gate.** Screenshot each changed frame through the MCP, move this plan to `pending_manual_validation/`, record the call count. No export before the owner's OK in Figma.
5. **Export after OK.** `node tools/export-figma.mjs --only <changed ids>`, then `node tools/check-figma.mjs` and `node tools/check-docs.mjs`. Unchanged golds keep their bytes.

## Intended documentation changes

None to specs here: home-timeline changes at A52's Completion. Inventory ids do not change.

## Out of scope

Compose, Config, Chat states other than `chatP`'s Home background, any other Home element.

## Validation

1. Discovery table before the first Figma mutation.
2. One screenshot per changed frame (Light, Dark); no overlapping text or detached instances.
3. Explicit owner review in Figma with corrections resolved.
4. check-figma and check-docs pass; no unrelated gold diff.
5. SDD git delivery.

## Results

### Discovery (06/10/2026, read only, before any write)

The old copy lives only in the `Card/Meal` component set (`7:45`, page `Componentes`): the `Description` text of `State=Pending` (`7:42`) and `State=Empty` (`36:300`), not bound to the `Description#7:16` property (fixed text, `WIDTH_AND_HEIGHT`, 207 px). No instance overrides it, so the component edit reaches every frame.

| Gold | Light / Dark frame | Empty/next cards with the old copy | Classification |
|---|---|---|---|
| `home0` | `39:302` / `40:430` | 4 (Empty) | `app`, copy changes |
| `home1` | `38:229` / `40:472` | 1 (Pending) | `app`, copy changes |
| `homeW` | `39:606` / `40:557` | 1 (Pending) | `app`, copy changes |
| `chatP` | `63:2023` / `63:2201` | 4 (Empty) | `app`, copy changes |
| `homeX` | `39:451` / `40:514` | none | unchanged, keeps its bytes |

Every other card element (meal, time, node, guide) is `app` and stays. Copy source: ADR-040 decision 1.

### Figma change (06/10/2026)

- `Card/Meal` (`7:45`): `Description` of `State=Pending` (`7:42`) and `State=Empty` (`36:300`) set to `Nenhum registro · Toque para registrar, segura para pular`, `textAutoResize = HEIGHT`, width `FILL` (268 px); the variants grow from 88 to 106 px. The set description carries the new copy. No new variant, token or detached instance; instances inherit the edit.
- Frame effect, checked on dry-run exports in both themes: `home0` 2516 → 2660 px tall (4 cards, two lines each), `home1` 2828 → 2864 px, `homeW` and `chatP` keep their size (the card sits behind the sheet or the dialog blur). Timeline guide and nodes stay aligned; the disclaimer still clears the FAB. `homeX` has no empty card and is untouched.
- Figma MCP calls: 7 of 40 (whoami, 2 skill reads, 1 page list, 2 discovery reads, 1 write). Review images came from `export-figma.mjs --dry-run` (no MCP calls).
- Owner review: OK in Figma on 06/10/2026, no fixes.

### Export (06/10/2026)

`node tools/export-figma.mjs --only home0,home1,homeW,chatP`: `home0` and `home1` (both themes) and light `chatP` changed; dark `chatP` and `homeW` (both themes) were below the exporter's noise threshold and kept their bytes. `homeX` untouched. `node tools/check-figma.mjs`: 74 golds verified. `node tools/check-docs.mjs` passes. Next: [A52](../../../android/plans/a52-home-card-gestures.md).
