# Plan — D16 Tone choice and closures

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Splash and onboarding", "Config and push", "Home"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{o5,cfgT,homeC,homeK,cfg}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D4, D3 and D7 `Concluído` ([history](completed/)); [D12](d12-plan-budget-choice.md) may run in parallel (other frames).
- Figma MCP budget: ≤ 90 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d16-tone-and-closures.md. Implemente o plano aprovado.`

## Objective

Draw the tone choice (onboarding O5 and the Config row and sheet) and the day and week closure cards of the Home in Aero, both themes, from the design system, with the behavior of ADR-044 and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `o5` | `o2` (two-option screen of the onboarding) | ADR-044 decision 1; [A55](../../android/plans/a55-tone-choice-and-closures.md) scope 1 copy |
| `cfg` (changed) | current `cfg`: one row added to the profile block | ADR-044 decision 1 |
| `cfgT` | `cfgS` (Config sheet with the Salvar / Cancelar pair) | ADR-044 decision 1 |
| `homeC` | `homeW` (Home with the workout line) plus one card above the timeline | ADR-044 decision 2; A55 scope 2 copy |
| `homeK` | `homeC` with the week card above the day card | ADR-044 decision 3; A55 scope 3 copy |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/{onboarding,config,home}`.

## Scope

1. **Discovery (read only):** per gold, list every visible element and classify it `app`, `gold-only` or `copy`; the table goes into Results before any write.
2. **Components** (in `Componentes`, readability rule): `Option/Tone` (two states: selected with the accent ring, unselected; title + one-line description; macro colors unused), `Card/Closure` (title, kcal line, P/C/G line in the semantic macro colors, one `muted` line for missing meals or days, text block of up to three lines, collapsed variant of one line). Reuse `Chip/Log Neutral`, `Sheet/Bottom`, `Button/Primary`.
3. **Screens:** Light row `o5`, `cfg`, `cfgT`, `homeC`, `homeK`, 390 px wide, in product order; Dark row as clones with the `Dark` mode; frame names `<id> · <title> · Light|Dark`; spacing per ADR-031 § 3. Copy exactly as A55 scope 1–3 (pt-BR).
4. **Owner review** (Figma review gate): one screenshot per frame; the plan goes to `pending_manual_validation/` until the owner's OK; fixes with one screenshot per changed frame.
5. **Export** after the OK: ids in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only o5 cfg cfgT homeC homeK`; new ids added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`.

## Out of scope

- Compose (A55). Server (S25). Behavior or copy changes beyond ADR-044. The closure notifications (system template; `push` stays their gold). Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances for every DS component, no raw hex fills outside the variables, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
