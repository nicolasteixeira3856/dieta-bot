# Plan — D9 Chat meal updates

- Status: Aguardando aprovação
- Date: 04/10/2026
- Owning context: `design`
- Executable boundary: the Chat meal-update flow in Figma `Design`; no app/server code. Repository outputs: the golds listed below, their entries in `tools/export-figma.mjs`, the gold inventory and design documentation.
- Prerequisites: acceptance of [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md); [S18](../../server/plans/s18-meal-additions-and-revisions.md) and [A43](../../android/plans/completed/a43-chat-records-memory-aero.md) delivered.
- Figma MCP budget: at most 100 calls, within the ceiling and rollover rules of [ADR-031](../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d9-chat-meal-updates.md. Implemente o plano aprovado.`

## Objective

Make the added food, previous amount and resulting meal total distinguishable in Chat. Draw the approved addition, revision and addition-destination states for [A47](../../android/plans/a47-chat-meal-updates.md), reusing Aero and the existing Chat flow.

## Sources and proposed golds

Behavior and exact copy: ADR-032. Wire data: S18, then the [HTTP contract](../../api-contract.md) after its delivery. Existing behavior/layout: [product Chat](../../produto/specifications/chat.md), [gold inventory](../../qa/README.md#golds), Figma `Design`, and `apps/android/app/src/main/java/com/nutri/android/feature/chat/` after A43.

This is a successor to the Chat migration, not permission to invent features from a mockup. ADR-031 requires the product ADR first for a new feature; that prerequisite is explicit here. Mark each proposed element as `ADR-032` or existing behavior during discovery.

The following ids are reserved by this plan until they enter the inventory at export. They were unused at planning time; recheck before writing to Figma.

| Gold id | Frame title | State and required distinction |
|---|---|---|
| `chatI` | Chat — Acréscimo à refeição | Pending addition to an occupied meal; named new food and amount, previous total, resulting total, confirmation and choice of another destination. |
| `chatIC` | Chat — Correção da refeição | Pending revision of a specific record; before/after totals, Atualizar and Cancelar; no addition-only value and no action that copies the consolidated proposal elsewhere. |
| `chatTI` | Chat — Destino do acréscimo | Existing meal-selection sheet in addition mode; clearly identifies the new food being directed, without selecting or moving the old whole meal. |

No other gold changes are authorized by this plan. Use existing `chatE`, `chatF`, `chatG`, `chatU` and `chatD` as layout references and regression coverage for initial recording, receipts, whole-meal move replacement and undo. If an additional layout change becomes necessary, revise this plan before implementing it.

## Scope

1. **Discovery before any write.** Read the delivered A43 components, actual exported golds and Figma nodes. Record a per-frame inventory in Results: existing component/behavior, ADR-032 addition, exact product copy, or gold-only element to exclude. Confirm the submitted states do not change plan/skip/ordinary-new-meal flows. Resolve current node ids from the live file; do not reuse unverified ids from a previous chat.
2. **Components.** Extend the existing Chat estimate and confirmation components with addition/revision variants. Use Aero variables, semantic macro colors and existing component instances; gold remains an accent. Keep numeric rows labeled, including after wrapping of long food names. Differentiate the added calories from the resulting kcal/P/C/G; never show a bare large number whose scope depends on reading the surrounding prose.
3. **Composition.** Create one section, `Chat — Atualizações de refeições`, on `Release 1`. Draw the three Light frames first, then clone with Dark mode. Width, export scale, node spacing and component readability follow ADR-031. Frame names follow `<id> · <title> · Light|Dark`. Use fictional food/portion examples, not tester logs or the attached photo. Include the meaning of the destination action in the sheet title/content: it directs the new food only.
4. **State review.** Walk through addition to the original meal, selection of an empty meal, selection of another occupied meal, cancellation, revision and the already recorded receipt's whole-meal move. Existing golds remain the reference when layout is unchanged. Reuse the addition confirmation when the chosen occupied destination changes; labels and totals must refer to that destination. No new screen or navigation route.
5. **Owner gate.** Screenshot every composed frame through the Figma MCP. Link the exact section and frames. Move this plan to `pending_manual_validation/` and record the real screenshots and call count. The owner reviews in Figma; implement requested fixes and re-screenshot. Do not export final golds or mark the design complete before the owner's OK.
6. **Export after OK.** Add the new ids to the exporter and the inventory with source `figma`, since they have no previous Stitch gold. Export only this plan's ids with `node tools/export-figma.mjs --only chatI,chatIC,chatTI`. Existing ids keep their source and PNG bytes. Run `node tools/check-figma.mjs` and `node tools/check-docs.mjs`. Record node ids, exported files, screenshots, calls and the review evidence in Results.

## Intended documentation changes

At Completion, add the exported states to the [gold inventory](../../qa/README.md#golds) and link the delivered design from the design index. The product specification's implemented Android states are updated by A47, not inferred from design availability. No token-value edits; this flow reuses the existing design system.

## Out of scope

Android, Room, server/prompt changes, other Chat flows, Home redesign, design-system migration, Stitch changes, owner-run drawing steps or rewriting A43/D6. Do not edit the other agent's checkout or current Figma flow while its validation is running.

## Validation

1. Discovery table exists before the first Figma mutation and every new behavior has ADR-032 authority.
2. Six frame screenshots, one per state/theme; no overlapping text or detached design-system instances. Inspect long labels, number hierarchy, macro colors, CTA states, sheet radius and edge insets against the existing Aero rules.
3. Explicit owner review in Figma; requested corrections resolved. Automated screenshots alone do not close this gate.
4. The exported golds, executable maps and inventory agree; check-figma/check-docs pass. Existing golds have no unrelated diff.
5. SDD git delivery, then A47 may use these golds. A successful design gate is not Android visual QA.

## Results

Planning only. No Figma calls, screenshots, gold exports or owner visual validation have been performed for this plan. Fill discovery, call count and review/export evidence during delivery.
