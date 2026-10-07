# Plan — D12 Plan over budget: the choice

- Status: Pendente aprovação manual (drawn 07/10/2026; waiting for the owner's review in Figma)
- Date: 05/10/2026
- Owning context: `design`
- Executable boundary: the plan-over-budget state in Figma `Design`; no app/server code. Repository outputs: the gold `chatRB`, its entry in `tools/export-figma.mjs`, the gold inventory and design documentation.
- Prerequisites: acceptance of [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md) (by the approval of [S21](../../../server/plans/completed/s21-plan-cooking-and-budget-choice.md)); [D10](../completed/d10-fibrai-tali-rename.md) golds in place, so the frame uses the Tali identity.
- Figma MCP budget: at most 40 calls, within the ceiling and rollover rules of [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d12-plan-budget-choice.md. Implemente o plano aprovado.`

## Objective

Draw the one new state ADR-039 needs: a plan whose dish is over what is left of the day, with the fixed line and the **Pode passar** · **Ajustar para caber** pills in place of **Registrar assim**, for [A50](../../../android/plans/a50-plan-budget-choice.md).

## Sources and proposed gold

Behavior and exact copy: ADR-039 decision 4. Wire data: S21, then the [HTTP contract](../../../api-contract.md) after its delivery. Existing layout: the `chatR` frame in Figma `Design`, [product Chat](../../../produto/specifications/chat.md) rule 16 and the [gold inventory](../../../qa/README.md#golds).

The id below is reserved by this plan until it enters the inventory at export. It was unused at planning time; recheck before writing to Figma.

| Gold id | Frame title | State and required distinction |
|---|---|---|
| `chatRB` | Chat — Plano acima do orçamento | A recipe plan bubble as in `chatR` (numbered steps, an `(opcional)` item, P/C/G in macro colors) with the projected day in `bad`; below it the fixed line `Passa {over} kcal do que sobra.` and, in this frame, `Reservei {kcal} kcal para {label}.`; two pills in the action position: **Pode passar** (secondary) · **Ajustar para caber** (primary). No **Registrar assim** in this state. |

No other gold changes. The states around it reuse existing golds: after **Pode passar** the plan looks like `chatR` with the projected day in `bad` (rule 16, unchanged); sending **Ajustar** uses the existing loading state; the adjusted plan is `chatR`. A plan with nothing left to adjust to shows no choice (A50) and is also `chatR`.

## Scope

1. **Discovery before any write.** Read the live `chatR` frame, the action-pill pattern of **Forçar estimativa** (`chatQ`) and the projected-day component. Record in Results which element is existing and which is ADR-039. Resolve node ids from the live file.
2. **Components.** Reuse the plan bubble and the existing pill component; add a two-pill row variant if the system lacks one. Aero variables only; the accent stays an accent; macros keep their semantic colors. The fixed lines use the existing secondary text style, not `bad`; only the projected-day kcal line is `bad`.
3. **Composition.** One section, `Chat — Plano acima do orçamento`, on `Release 1`. Light first, then Dark by variable mode. Width, export scale and spacing per ADR-031. Frame names `chatRB · Chat — Plano acima do orçamento · Light|Dark`. Fictional dish and numbers, independent of any tester log. Long labels: check a reservation label at the 40-character limit and two-line pill wrapping at large text.
4. **Owner gate.** Screenshot both frames through the MCP, link the section and frames, move this plan to `pending_manual_validation/` and record the call count. The owner reviews in Figma; fixes are applied and re-screenshotted. No export before the owner's OK.
5. **Export after OK.** Add `chatRB` to the exporter and the inventory with source `figma`. `node tools/export-figma.mjs --only chatRB`, then `node tools/check-figma.mjs` and `node tools/check-docs.mjs`. Existing golds keep their bytes.

## Intended documentation changes

At Completion: `chatRB` enters the [gold inventory](../../../qa/README.md#golds); the design index links the delivered design. Product Chat rule 16 is updated by A50, not here. No token edits.

## Out of scope

Android, server, prompt, other Chat states, `chatR` itself, new navigation, settings, Home. Do not touch the Figma flows or checkouts of other plans in progress.

## Validation

1. Discovery table before the first Figma mutation; every new element traced to ADR-039.
2. Two frame screenshots (Light, Dark); no overlapping text or detached instances; long label and large-text check recorded.
3. Explicit owner review in Figma with corrections resolved.
4. Exported golds, exporter map and inventory agree; check-figma and check-docs pass; no unrelated gold diff.
5. SDD git delivery; then A50 may use the gold.

## Results

Approved by the owner on 06/10/2026 (overnight run, message naming this file). Built by the agent the same night. The id `chatRB` is still unused in the inventory and in `tools/export-figma.mjs` (checked before writing).

### Discovery (read only, before the first write)

Sources: the live `chatR` Light frame (72:2959), `Chat/ActionBar` (60:149), `Button/Primary` (6:2), `Card/MealPlan` (71:184, `Over` variant), ADR-039 decision 4 and A50 scope.

| Element | Existing / ADR-039 | Decision |
|---|---|---|
| Header, date chip, user bubble, bot label, composer | existing (`chatR`) | kept; fictional user text about a recipe and a later slice of cake |
| Plan bubble with ingredients, an `(opcional)` item, numbered steps, `Total` with P/C/G in macro colors | existing (`chatR`, ADR-039 decisions 1–2) | text rewritten for a fictional dish (numbered steps, one `(opcional)` food) |
| `Card/MealPlan` projected day | existing, `Over=true` | `1.640 → 2.260 de 2.200 kcal` in `bad`; macros with the dish |
| `Passa 310 kcal do que sobra.` | ADR-039 decision 4 | Caption, `text/muted`, below the bubble |
| `Reservei 250 kcal para fatia de bolo.` | ADR-039 decision 4 | same style, below the first line |
| **Pode passar** (secondary) · **Ajustar para caber** (primary) in the action position | ADR-039 decision 4 | two `Chat/ActionBar` instances side by side, no icon; the primary one takes the fills of `Button/Primary` (accent + gloss) and an `accent/on` label; no new component (the system already has the pill) |
| **Registrar assim** | existing | not in this state (ADR-039) |

Numbers: eaten 1.640, ceiling 2.200, left 560, stated reservation 250, limit 310, dish 620 kcal, over 310.

### Build (Figma `Design`, 07/10/2026)

- New section `Chat — Plano acima do orçamento` (144:5078) on `Release 1`, below "Chat — Atualizações de refeições", 160 px apart: heading, note, `chatRB · … · Light` (144:5081) and `chatRB · … · Dark` (144:5179, Color mode `Dark`).
- Choice row `Choice` (144:5155) in the footer: `Pode passar` (144:5163) and `Ajustar para caber` (144:5167), `Chat/ActionBar` instances with the icon hidden; the primary takes the `Button/Primary` fills, effect and label paint. No component created or changed; nothing detached.
- Long label: `Reservei 250 kcal para` + a 40-character label wraps to two lines in the bubble column (18 → 36 px), no clipping.
- Large text (labels at 130%): `Pode passar` 119 px in a 170 px pill; `Ajustar para caber` 178 px in a 170 px pill: at large font the primary label needs two lines; A50 must let the pill label wrap (no truncation).
- Review images: both frames exported with `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls) and checked in both themes.
- Figma MCP calls: 2 (one read, one write). Night total after D12: 14 of 120.

### Owner review (the only manual step)

Open `Design` → `Release 1` → `Chat — Plano acima do orçamento`. After the OK: map 144:5081 / 144:5179 as `chatRB` in `tools/export-figma.mjs`, add `chatRB` to the inventory in `docs/qa/README.md`, `node tools/export-figma.mjs --only chatRB`, `node tools/check-figma.mjs`, `node tools/check-docs.mjs`.
