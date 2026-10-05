# Plan — D9 Chat meal updates

- Status: Concluído
- Date: 04/10/2026
- Owning context: `design`
- Executable boundary: the Chat meal-update flow in Figma `Design`; no app/server code. Repository outputs: the golds listed below, their entries in `tools/export-figma.mjs`, the gold inventory and design documentation.
- Prerequisites: acceptance of [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md); [S18](../../../server/plans/completed/s18-meal-additions-and-revisions.md) and [A43](../../../android/plans/completed/a43-chat-records-memory-aero.md) delivered.
- Figma MCP budget: at most 100 calls, within the ceiling and rollover rules of [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d9-chat-meal-updates.md. Implemente o plano aprovado.`

## Objective

Make the added food, previous amount and resulting meal total distinguishable in Chat. Draw the approved addition, revision and addition-destination states for [A47](../../../android/plans/pending_manual_validation/a47-chat-meal-updates.md), reusing Aero and the existing Chat flow.

## Sources and proposed golds

Behavior and exact copy: ADR-032. Wire data: S18, then the [HTTP contract](../../../api-contract.md) after its delivery. Existing behavior/layout: [product Chat](../../../produto/specifications/chat.md), [gold inventory](../../../qa/README.md#golds), Figma `Design`, and `apps/android/app/src/main/java/com/nutri/android/feature/chat/` after A43.

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

At Completion, add the exported states to the [gold inventory](../../../qa/README.md#golds) and link the delivered design from the design index. The product specification's implemented Android states are updated by A47, not inferred from design availability. No token-value edits; this flow reuses the existing design system.

## Out of scope

Android, Room, server/prompt changes, other Chat flows, Home redesign, design-system migration, Stitch changes, owner-run drawing steps or rewriting A43/D6. Do not edit the other agent's checkout or current Figma flow while its validation is running.

## Validation

1. Discovery table exists before the first Figma mutation and every new behavior has ADR-032 authority.
2. Six frame screenshots, one per state/theme; no overlapping text or detached design-system instances. Inspect long labels, number hierarchy, macro colors, CTA states, sheet radius and edge insets against the existing Aero rules.
3. Explicit owner review in Figma; requested corrections resolved. Automated screenshots alone do not close this gate.
4. The exported golds, executable maps and inventory agree; check-figma/check-docs pass. Existing golds have no unrelated diff.
5. SDD git delivery, then A47 may use these golds. A successful design gate is not Android visual QA.

## Results

Approved by the owner on 2026-10-05 ("Aprovo o plano docs/design/plans/d9-chat-meal-updates.md. Implemente o plano aprovado."). Prerequisites checked: ADR-032 `Accepted`, [S18](../../../server/plans/completed/s18-meal-additions-and-revisions.md) and [A43](../../../android/plans/completed/a43-chat-records-memory-aero.md) in `completed/`. The ids `chatI`, `chatIC` and `chatTI` are unused in the inventory, `tools/export-figma.mjs` and the code.

### Discovery (before the first write)

Sources read: ADR-032, the [meal-change capability](../../../api-contract.md#meal-change-capability), [product Chat](../../../produto/specifications/chat.md) rules 5, 19 and 22, A47, `ChatRecordComponents.kt` (`ReplaceCard`, receipt), `ChatScreen.kt` (`SlotSheet`), the golds `chatE`, `chatU`, `chatT`, and the live Figma nodes (read 2026-10-05): `Chat/Estimate` `60:172` (single component), `Chat/Receipt` `70:228` (State = Saved | Undone | ReplacePending), `Sheet/Bottom` `37:78`, `Sheet/SlotList` `61:121`, `Row/SlotPick` `61:118`, `Button/Primary` `6:2`, frames `chatU` `72:2727` and `chatT` `63:1911`.

Classes: `app` (existing component or behavior, reused), `ADR-032` (new behavior or copy with ADR-032 authority), `copy` (exact pt-BR text), `gold-only` (excluded).

| Element | Class | Figma |
|---|---|---|
| Header, date chip, user bubble, bot label, composer, background bubbles | app | instances as in `chatU` |
| Bot bubble with the estimate card | app | `Chat/Estimate` (`chatE`, `chatU`) |
| Addition estimate: the added food named with its quantity and `+{kcal} kcal`; its own P/C/G with a `+` sign; long names wrap and keep the value on the row | ADR-032 § 3, contract `addition` | new `Chat/Estimate` Kind = Addition |
| Revision estimate: the revised meal text and the whole-meal `NOVO TOTAL` with its P/C/G; no `+` value | ADR-032 § 1, § 3 | new `Chat/Estimate` Kind = Revision |
| Ordinary estimate (`ENERGIA TOTAL`, `~kcal`) | app | existing variant, Kind = Meal, unchanged |
| `Já registrado no {slot}: {kcal} kcal`, `Total do {slot}: {kcal} kcal` with the resulting P/C/G, `Adicionar ao {slot}?`, **Adicionar**, **Escolher outra refeição** | ADR-032 § 3, copy | new `Chat/Receipt` State = AddPending (the destination accounting lives on the card, so a chosen occupied destination reuses it with its own labels and totals) |
| `Atualizar {slot}?`, `Antes: {kcal} kcal`, `Novo total: {kcal} kcal`, **Atualizar**, **Cancelar** | ADR-032 § 3, copy | new `Chat/Receipt` State = RevisePending; no destination action |
| `Substituir {slot}?` card and Outra refeição | app (legacy, rollout without the capability) | unchanged `chatU` |
| Meal picker: `Sheet/Bottom`, four `Row/SlotPick`, Confirmar refeição, Cancelar | app | as `chatT` |
| Addition mode of the picker: title `Escolher outra refeição`, subtitle saying only the addition moves and the source meal stays, the added food and `+{kcal} kcal`; no source preselected, no `(atual)` mark | ADR-032 § 4 (the plan authorizes the wording) | new `Sheet/SlotList` Mode = Addition |
| Receipt `Trocar refeição` (whole meal), Desfazer, `Não registrado` | app | unchanged (`chatG`, `chatD`); no new gold |
| Status bar, navigation bar | gold-only | not drawn |

State review (no new screen or route): addition to the original meal = `chatI`; Escolher outra refeição = `chatTI`; an empty destination records only the addition with the existing receipt (`chatG` layout); another occupied destination shows a new `chatI` pair for that destination; Cancelar on a revision leaves `Não registrado` (existing mark); the whole-meal move stays the receipt's Trocar refeição (`chatT`, `chatU` below a receipt). Plan, skip and ordinary new-meal flows are not touched.

Example data (fictional): Jantar already 380 kcal (22P · 40C · 14G); addition `Pudim de leite, 1 fatia média (100 g)` +240 kcal (+6g P, +38g C, +7g G); total 620 kcal · 28P · 78C · 21G. Revision: `Arroz, feijão e omelete de 3 ovos`, 380 → 455 kcal (28P · 40C · 19G).


### Delivered in Figma (2026-10-05)

**`Componentes`, section `Chat`** (variants added; existing variants, properties and instances unchanged):

- `Chat/Estimate` became a component set `122:216`, Kind = Meal (`60:172`, the former component) | Addition (`122:183`) | Revision (`122:199`). Addition: the food and quantity (Body, `text/primary`, wraps) label `+{kcal}` (Title) + `kcal` on the same baseline row; the boxes carry the added macros with a `+` sign. Revision: the meal text above `NOVO TOTAL` with the whole-meal kcal and macros. The Kcal/P/C/G text properties drive Kind = Meal only; the new kinds take direct text overrides, so no shared default changes an existing instance.
- `Chat/Receipt` `70:228` gained State = AddPending (`122:217`) and RevisePending (`122:237`). AddPending: `Já registrado no {slot}` (Body, muted) / `{kcal} kcal`, `Total do {slot}` / `{kcal} kcal` (Body/Strong), the resulting `{P}P · {C}C · {G}G` in the macro colors (Caption/Strong, right-aligned), `Adicionar ao {slot}?`, then Adicionar (`Button/Primary`) above Escolher outra refeição (outline pill), both 58 px and full width because the second label does not fit beside the first. RevisePending: `Atualizar {slot}?`, `Antes` / `Novo total` rows, Atualizar and Cancelar side by side.
- `Sheet/SlotList` became a component set `122:310`, Mode = Record (`61:121`, the former component) | Addition (`122:255`): subtitle `Só o acréscimo vai para a refeição escolhida. O {slot} fica como está.`, the added food and `+{kcal} kcal` on a `surface/2` box, the four `Row/SlotPick` rows with no source preselected and no `(atual)`. The `Sheet/Bottom` Content swap now offers the set.
- Set, component and variant descriptions updated. No variable, text style or icon added.

**`Release 1` → section `Chat — Atualizações de refeições`** (`123:3799`, below `Chat · D6`, 160 px apart):

| Gold | Light | Dark |
|---|---|---|
| `chatI` | `123:3802` | `123:4230` |
| `chatIC` | `123:3909` | `123:4308` |
| `chatTI` | `123:4014` | `123:4385` |

- Heading and note (`123:3800`, `123:3801`); frames 390 px wide, 80 px apart, Light row above Dark row. `chatI` and `chatIC` start from the `chatU` column; `chatTI` is the `chatT` shell (scrim + `Sheet/Bottom`) over the `chatI` thread, with title `Escolher outra refeição` and Content = Mode Addition.
- `chatI` grows to 930 px to show the whole thread (rule of D6); `chatIC` and `chatTI` keep 844 px.
- The bot bubble carries no prose: the server reply is the ADR-032 template, rendered from the structured fields by the cards.
- Dark frames are clones with the Color mode set to Dark (also on the inner `Chat` frame of `chatTI`) and no other change.

### Figma MCP budget

14 calls of the 100 budgeted: 1 `whoami`, 3 skill reads, 10 `use_figma` (4 read-only inspections, 1 audit with a re-spacing, 5 writes, 1 of them failed with no canvas change and was retried). Review images come from the REST export (`tools/export-figma.mjs --dry-run`), no MCP call.

### Validation

1. Discovery table: written above before the first Figma write; every new element cites ADR-032.
2. Read-back of the six frames: 122 instances, 0 orphan; 0 solid fills or strokes without a variable or paint style and 0 text without a text style outside instances; 0 overlapping section children. Long food names wrap and keep the value on the row (`chatI` estimate and `chatTI` box).
3. Regression: `chatE`, `chatU`, `chatT`, `chatF`, `chatG`, `chatD`, `chatM` exported again by REST dry run after the change; byte-identical to the committed golds in both themes.
4. One visual fix before the review: the AddPending button group had collapsed to the old 58 px row height; it now hugs two 58 px buttons.
5. Review images of the six frames sent to the owner on 2026-10-05. Owner review in Figma done the same day, no fixes requested.

### For the owner review (accepted)

Figma: [section `Chat — Atualizações de refeições`](https://www.figma.com/design/qNiqNN3vk9GpmPL3bcV9W1/Design?node-id=123-3799).

1. The added amount is the bot's estimate card; the previous amount, the resulting total and the question are the confirmation card, so a chosen occupied destination reuses the card with its own labels.
2. Every addition number has a `+` (`+240 kcal`, `+6g P`); the resulting macros use the consolidated-log format `28P · 78C · 21G`.
3. The new totals follow the ADR-032 copy without the `~` of the ordinary estimate (`620 kcal`, `455`).
4. Adicionar and Escolher outra refeição are stacked (the long label does not fit beside the CTA); Atualizar and Cancelar stay side by side.
5. `chatTI` uses the button label as the sheet title and a new subtitle (`Só o acréscimo vai para a refeição escolhida. O Jantar fica como está.`), wording authorized by this plan's scope 3.
6. Revision shows `NOVO TOTAL` in the estimate card and again in the confirmation card next to `Antes`.

### Owner review (2026-10-05)

The owner reviewed the section in Figma and closed the plan: "Revisei no Figma, pode exportar os golds e concluir o D9." The six points above were accepted with no changes.

### Exported golds

- `tools/export-figma.mjs`: `chatI`, `chatIC`, `chatTI` mapped in `DARK_FRAMES` and `LIGHT_FRAMES` (node ids in the table above).
- `docs/qa/README.md` § Golds: the three ids added to the inventory. The inventory has no source column since D8 (every gold comes from Figma), so scope 6's `figma` source needs no entry.
- `node tools/export-figma.mjs --only chatI,chatIC,chatTI`: six new files in `docs/qa/figma/{dark,light}/`, 780 px wide (`chatI` 1860, `chatIC` and `chatTI` 1688 px tall). Five are byte-identical to the images reviewed by the owner; Dark `chatI` differs only in PNG encoding (0 pixels with Δ > 40).
- No other gold has a diff.
- `node tools/check-figma.mjs`: 74 Figma gold PNGs verified (37 dark + 37 light). `node tools/check-docs.mjs`: passed.
- Next: [A47](../../../android/plans/pending_manual_validation/a47-chat-meal-updates.md) implements these states against the golds; the product specification changes at its Completion.

### Figma MCP budget, total

14 of 100. The review and the closure used no MCP call (export through the REST API).