# Plan — D26 Release 2: Decision line and per-option fit in the plan bubble

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Opções" (frame `chatO` redrawn), the option component in `Componentes` extended. Repository: `docs/qa/figma/{dark,light}/chatO.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D25 `Concluído` ([history](completed/)); [ADR-056](../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md) accepted with [S39](../../server/plans/s39-plan-decision-line-and-option-budget.md).
- Figma MCP budget: ≤ 30 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d26-release-2-option-decision.md. Implemente o plano aprovado.`

## Objective

Redraw the plan bubble with options in Aero, in both themes: the decision line above the option blocks, under each option's totals the fit line and the day line, and the rest of the reply below the blocks, as ADR-056 § 8 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatO` | current `chatO` (D25: option block, action row), `chatRB` (caption style of the budget note), `chatE` (the projection line) | ADR-056 § 8: lead text, fit line `Cabe na janela do {meal}` / `Passa {n} kcal da janela do {meal}`, day line `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}`, trailing text (critique, skip clause, closing lines) |

Code: `apps/android/.../feature/chat` ([A70](../../android/plans/a70-option-fit-and-projection.md) implements it).

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

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
