# Plan — D15 Config: app reset

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `design`
- Executable boundary: the Config flow in Figma `Design`; no app/server code. Repository outputs: the changed `cfg` gold, the new gold `cfgR`, their entries in `tools/export-figma.mjs`, the gold inventory and design documentation.
- Prerequisites: [ADR-040](../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) accepted by the approval of this plan or of [D14](completed/d14-home-card-gestures.md).
- Figma MCP budget: at most 50 calls, within the ceiling and rollover rules of [ADR-031](../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d15-config-app-reset.md. Implemente o plano aprovado.`

## Objective

Draw the reset entry on Config and its confirmation (ADR-040 decision 2), for [A53](../../android/plans/a53-config-app-reset.md).

## Sources and proposed gold

Behavior: ADR-040. Layout: the live `cfg` and `wipe` frames (the `Dialog/Confirm` Tone=Danger pattern), [memoria-push](../../produto/specifications/memoria-push.md) § Config, the [gold inventory](../../qa/README.md#golds).

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

`cfgR` enters the [gold inventory](../../qa/README.md#golds). The Config spec changes at A53's Completion.

## Out of scope

Compose, Home, onboarding frames (O1 after a reset is the existing `o1`), other Config sheets.

## Validation

1. Discovery table before the first Figma mutation.
2. One screenshot per changed or new frame (Light, Dark); no overlap, no detached instances.
3. Explicit owner review in Figma with corrections resolved.
4. Exporter, inventory and PNGs agree; check-figma and check-docs pass; no unrelated gold diff.
5. SDD git delivery.

## Results

Planning only.
