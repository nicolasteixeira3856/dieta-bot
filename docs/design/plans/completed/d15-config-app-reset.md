# Plan — D15 Config: app reset

- Status: Concluído (approved 06/10/2026; owner OK in Figma 06/10/2026)
- Date: 06/10/2026
- Owning context: `design`
- Executable boundary: the Config flow in Figma `Design`; no app/server code. Repository outputs: the changed `cfg` gold, the new gold `cfgR`, their entries in `tools/export-figma.mjs`, the gold inventory and design documentation.
- Prerequisites: [ADR-040](../../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) accepted by the approval of this plan or of [D14](d14-home-card-gestures.md).
- Figma MCP budget: at most 50 calls, within the ceiling and rollover rules of [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d15-config-app-reset.md. Implemente o plano aprovado.`

## Objective

Draw the reset entry on Config and its confirmation (ADR-040 decision 2), for [A53](../../../android/plans/completed/a53-config-app-reset.md).

## Sources and proposed gold

Behavior: ADR-040. Layout: the live `cfg` and `wipe` frames (the `Dialog/Confirm` Tone=Danger pattern), [memoria-push](../../../produto/specifications/memoria-push.md) § Config, the [gold inventory](../../../qa/README.md#golds).

The id below is reserved by this plan until it enters the inventory at export. It was unused at planning time; recheck before writing to Figma.

| Gold | Frame title | State |
|---|---|---|
| `cfg` (changed) | Config | A last block `Dados` below the note card, one setting row `Resetar app` with the caption `Apaga tudo e refaz o onboarding`, in the danger tone used by `wipe` for its icon only; no value on the right. |
| `cfgR` (new) | Config — Resetar app | `Dialog/Confirm` Tone=Danger over the blurred Config: title `Resetar o app?`; body `Apaga perfil, metas, refeições, registros, conversa e memória deste aparelho. Depois você refaz o onboarding. Não dá para desfazer.`; primary `Apagar tudo`; secondary `Cancelar`. |

Proposed copy is final once the owner approves the frames; A53 copies it from the gold. If the full `cfg` list no longer fits the 390 × 844 frame, the frame grows with the content as the other long golds do; `cfgS` and `wipe` keep their bytes unless the discovery shows the new block in their blurred background.

## Scope

1. **Discovery before any write.** Read `cfg`, `cfgS`, `wipe` and the `Dialog/Confirm` and setting-row components. Record in Results each element as existing or ADR-040, and the node ids.
2. **Components.** Reuse the setting row, `Label/Section` and `Dialog/Confirm` Tone=Danger; no new component, no new token. The accent stays an accent.
3. **Frames.** Light first, Dark by variable mode. Frame names `cfgR · Config — Resetar app · Light|Dark`. Check the dialog body at large text.
4. **Owner gate.** Screenshot each changed or new frame, move this plan to `pending_manual_validation/`, record the call count. No export before the owner's OK in Figma.
5. **Export after OK.** Add `cfgR` to the exporter and the inventory; `node tools/export-figma.mjs --only cfg,cfgR` (plus any id the discovery added); `node tools/check-figma.mjs`, `node tools/check-docs.mjs`.

## Intended documentation changes

`cfgR` enters the [gold inventory](../../../qa/README.md#golds). The Config spec changes at A53's Completion.

## Out of scope

Compose, Home, onboarding frames (O1 after a reset is the existing `o1`), other Config sheets.

## Validation

1. Discovery table before the first Figma mutation.
2. One screenshot per changed or new frame (Light, Dark); no overlap, no detached instances.
3. Explicit owner review in Figma with corrections resolved.
4. Exporter, inventory and PNGs agree; check-figma and check-docs pass; no unrelated gold diff.
5. SDD git delivery.

## Results

### Discovery (06/10/2026, read only, before any write)

Section `Config e push · D7` (`78:3422`, `Release 1`). Light / Dark frames: `cfg` `78:3425` / `78:3664`, `cfgS` `78:3520` / `78:3695`, `wipe` `78:3596` / `78:3723`, `push` `78:3646` / `78:3757`.

| Element | Source | Classification |
|---|---|---|
| `cfg`: Header, blocks Metas e limites, Horários das refeições, Treino de hoje, `Card/Note`, bubbles | code (`ConfigScreen`) | `app`, unchanged |
| `cfg`: block `DADOS` with `Row` `Resetar app` / `Apaga tudo e refaz o onboarding`, no value | ADR-040 decision 2 | new, built by cloning the Treino de hoje block (same `Label/Section`, group and setting-row instance) |
| `cfgR`: blurred Config behind `overlay/scrim`, `Dialog` Tone=Danger | the `wipe` frame (`WipeDialog` in code) | `app` pattern, cloned |
| `cfgR`: title, body, action, secondary text | ADR-040, copy of this plan | new copy |
| `cfgS`, `wipe` | each holds its own Config copy; the new block would sit below the 844 px fold | unchanged, keep their bytes |

The setting row has no icon or danger tone (`Value tone` = Accent \| Muted only), so the reset row uses the plain row; danger shows only in the dialog, as in `wipe`. No new variant.

### Figma change (06/10/2026)

- `cfg` Light `78:3425` / Dark `78:3664`: new last block `Block · DADOS` (`Label/Section` `DADOS`, one setting-row instance `Resetar app`, detail `Apaga tudo e refaz o onboarding`, no value, chevron), cloned from the Treino de hoje block. The frame grows from 917 to 1047 px.
- `cfgR` Light `135:4306` / Dark `135:4375` (Dark = clone with the `Dark` mode of the color collection): clone of `wipe` with the same block in its Config copy, scrim and `Dialog` Tone=Danger, title `Resetar o app?`, body `Apaga perfil, metas, refeições, registros, conversa e memória deste aparelho. Depois você refaz o onboarding. Não dá para desfazer.`, action `Apagar tudo`, secondary `Cancelar`; dialog centered (y 224, 396 px). The danger icon is the Tone's fixed counter-clockwise arrow, as in `wipe`.
- Section `Config e push · D7` widened by 470 px; `cfgR` sits after `wipe`, `push` moved right. No new component, variant or token; all instances attached (26 per `cfgR` frame).
- Figma MCP calls: 4 of 50 (whoami, 1 discovery read, 1 write, 1 fix plus read-back). Review images from `export-figma.mjs --dry-run`.
- Owner review: OK in Figma on 06/10/2026, no fixes.

### Export (06/10/2026)

`cfgR` mapped in `tools/export-figma.mjs` (Light `135:4306`, Dark `135:4375`) and added to the [gold inventory](../../../qa/README.md#golds). `node tools/export-figma.mjs --only cfg,cfgR`: `cfg` 1834 → 2094 px (both themes), `cfgR` new (both themes). `node tools/check-figma.mjs`: 76 golds verified. `node tools/check-docs.mjs` passes. Next: [A53](../../../android/plans/completed/a53-config-app-reset.md).
