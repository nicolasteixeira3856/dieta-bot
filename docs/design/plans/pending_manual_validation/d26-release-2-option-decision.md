# Plan — D26 Release 2: Decision line and per-option fit in the plan bubble

- Status: Pendente aprovação manual
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Opções" (frame `chatO` redrawn), the option component in `Componentes` extended. Repository: `docs/qa/figma/{dark,light}/chatO.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D25 `Concluído` ([history](../completed/)); [ADR-056](../../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md) accepted with [S39](../../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md).
- Figma MCP budget: ≤ 30 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d26-release-2-option-decision.md. Implemente o plano aprovado.`

## Objective

Redraw the plan bubble with options in Aero, in both themes: the decision line above the option blocks, under each option's totals the fit line and the day line, and the rest of the reply below the blocks, as ADR-056 § 8 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatO` | current `chatO` (D25: option block, action row), `chatRB` (caption style of the budget note), `chatE` (the projection line) | ADR-056 § 8: lead text, fit line `Cabe na janela do {meal}` / `Passa {n} kcal da janela do {meal}`, day line `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}`, trailing text (critique, skip clause, closing lines) |

Code: `apps/android/.../feature/chat` ([A70](../../../android/plans/a70-option-fit-and-projection.md) implements it).

## Scope

1. **Discovery (read only):** list every visible element of the current `chatO`, `chatRB` and `chatE` and classify it; table in Results before any write.
2. **Component:** the option block gains two caption lines under the totals (fit, day), muted; readability rule of ADR-031 § 3; the two instances differ (one `Cabe`, one `Passa`).
3. **Screen** (section "Opções" on `Release 2`): `chatO` Light redrawn (decision line, two blocks, trailing text with a closing line); Dark clone; naming and spacing per ADR-031 § 3. Copy with synthetic dishes and numbers (ADR-033).
4. **Owner review** (Figma review gate), then **Export**: `node tools/export-figma.mjs --only chatO`, `node tools/check-figma.mjs`. The inventory id is unchanged.

## Out of scope

- Compose (A70). Behavior or copy changes beyond ADR-056 § 8. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances for every DS component, no raw hex fills outside the variables, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

### Discovery (09/10/2026, before any write)

Read only: the `chatO` frame and the `Chat/PlanOption` component (D25), the budget note of `chatRB` (`Passa 310 kcal do que sobra.`, Caption, `text/muted`), the `chatE` frame (its projection is the `Card/MealPlan` panel, no caption line), `ChatFixtures.chatO`, [ADR-056](../../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md) § 1 and § 8, the decision-line form and the app preview of [S39](../../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md), [A70](../../../android/plans/a70-option-fit-and-projection.md) scopes 1–3.

`chatO` (open request answered with two options; references `chatO`, `chatRB`, `chatE`, layout only):

| Element | Class | Source |
|---|---|---|
| Header, date chip, user bubble `Não sei o que jantar. Me dá umas ideias?`, `Tali` label, time, composer, page bubbles | app (unchanged) | `chatO` |
| Option block: title, items with grams, total `~kcal · P · C · G` in the macro colours, **Registrar** / **Reservar** row | app (unchanged) | `chatO`, ADR-051 § 1 |
| Lead line `Duas opções para o jantar:` | dropped: the lead text is the decision line | ADR-056 § 1, § 8 |
| Decision line above the blocks: `Vai de Omelete de forno: ~360 kcal · P 28 g, cabe na janela do Jantar. Pizza de pão sírio passa ~20 kcal.` | app (new, A70 scope 3) | ADR-056 § 1; S39 form `Vai de {name}: ~{kcal} kcal · P {p} g, cabe na janela do {meal}. {other} passa ~{n} kcal.` |
| Fit line under each option's totals: `Passa 20 kcal da janela do Jantar` (option 1) / `Cabe na janela do Jantar` (option 2), Caption, `text/muted` | app (new, A70 scope 2) | ADR-056 § 8 (`over_kcal`); caption style of the `chatRB` budget note |
| Day line under the fit line: `Dia: ~2.060 de 2.200 kcal · P 126 de 167` / `Dia: ~2.000 de 2.200 kcal · P 114 de 167`, Caption, `text/muted` | app (new, A70 scope 2) | ADR-056 § 8 (`DayBalance` projection, A64) |
| Trailing text below the blocks: `Ainda faltam 53 g de proteína; ajuste a ceia para priorizar proteína.` and the closing line `Ceia: iogurte natural com whey ~160 kcal · P 22` | app (new position, A70 scope 3) | ADR-056 § 8; S39 reply |
| Day projection `Card/MealPlan` after the text: now the chosen option (option 2), `1.640 → 2.000 de 2.200 kcal`, P 114 · C 166 · G 66 | app (A70 scope 1) | ADR-056 § 2 |
| Budget note `Passa {over} kcal do que sobra.` and the Pode passar / Ajustar choice | not drawn: the chosen option fits (A70 scope 4 asserts the note is absent) | ADR-039, `chatRB` |
| Skip clause | not drawn: the user message skips nothing | ADR-056 § 6 |

Sample data (synthetic, ADR-033): eaten 1.640 of 2.200 kcal, Ceia reserved 160 kcal, so the Jantar window is 400 kcal; option 1 (420 kcal, 40 P) passes it by 20, option 2 (360 kcal, 28 P) fits and is chosen; protein target 167 g, eaten 86 g. New copy for the owner's review: the decision line, the fit and day lines and the trailing text above (ADR-056 forms).

### Build (09/10/2026)

- **Component** (`Componentes`, section renamed `Opções · D25–D26`, `201:246`): `Chat/PlanOption` (`201:247`) gains `Budget lines` (`205:262`, vertical, gap 2) between the totals and the action row: `Fit` (`205:263`) and `Day` (`205:300`), Caption, `text/muted` (the style and variable of the `chatRB` budget note), wrapping at the block width; new TEXT properties `Fit#205:0` and `Day#205:1`; description extended. The block grows from 286 to 332 px (five items).
- **Frame** (`Release 2`, section renamed `Opções · D25–D26`, `202:750`): `chatO` Light `202:753` redrawn in place: lead text = the decision line; option 1 `Passa 20 kcal da janela do Jantar` / `Dia: ~2.060 de 2.200 kcal · P 126 de 167`; option 2 `Cabe na janela do Jantar` / `Dia: ~2.000 de 2.200 kcal · P 114 de 167`; new `Trailing` text (`205:8285`, Body, `text/primary`) after the blocks; `Card/MealPlan` on the chosen option (`1.640 → 2.000`, P 114 · C 166 · G 66). 390 × 1348 (hug).
- **Dark:** the old Dark frame `202:916` removed; Light cloned as `205:8286` in mode Dark, 80 px under Light; 3 text paints outside instances rebound with the Dark resolved value. The section grows to 1360 × 3064.

### Validation (before the owner review)

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`): instances for every DS component (header, chip, bubbles, `Chat/PlanOption` × 2, `Card/MealPlan`, composer); zero visible solid paints without a variable or style in both frames; no overlapping siblings in the section and no page node overlapping it; Light in mode Light, Dark in mode Dark.
3. Review images from `node tools/export-figma.mjs --only 202:753,205:8286,201:247 --dry-run` (no MCP calls), one per frame, sent to the owner.

Figma MCP budget: 7 of 30 calls (whoami, 3 skill reads, 1 read, 2 writes with read-back).

### Owner review

<Pending: the owner's OK in Figma, section `Opções · D25–D26` on `Release 2`.>
