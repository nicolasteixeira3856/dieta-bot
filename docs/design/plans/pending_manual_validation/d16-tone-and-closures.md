# Plan — D16 Tone choice and closures

- Status: Pendente aprovação manual (drawn 07/10/2026; waiting for the owner's review in Figma)
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Splash and onboarding", "Config and push", "Home"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{o5,cfgT,homeC,homeK,cfg}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D4, D3 and D7 `Concluído` ([history](../completed/)); [D12](d12-plan-budget-choice.md) may run in parallel (other frames).
- Figma MCP budget: ≤ 90 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-044](../../../produto/adrs/ADR-044-assistant-tone-and-closures.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d16-tone-and-closures.md. Implemente o plano aprovado.`

## Objective

Draw the tone choice (onboarding O5 and the Config row and sheet) and the day and week closure cards of the Home in Aero, both themes, from the design system, with the behavior of ADR-044 and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `o5` | `o2` (two-option screen of the onboarding) | ADR-044 decision 1; [A55](../../../android/plans/a55-tone-choice-and-closures.md) scope 1 copy |
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

Approved by the owner on 06/10/2026 (overnight run, message naming this file). Built by the agent the same night.

### Discovery (read only, before the first write)

Sources: the Light golds `o2`, `cfg`, `cfgS`, `homeW` in `docs/qa/figma/light/`, their Figma frames (structure read by the MCP), `feature/onboarding/OnboardingAeroScreens.kt` (O2 uses `AeroOptionCard` + `AeroNoteCard`; O4 holds the CTA `Concluir e começar` with the arrow) and A55 scope 1–3.

| Gold | Element | Class | Decision |
|---|---|---|---|
| `o5` | Top bar (Back, `FIBRAI INTAKE`) as in `o2` | app | kept |
| `o5` | `Stepper/Progress` | app | new variant `Step=5` (five segments) |
| `o5` | Eyebrow `ONBOARDING 5/5 • TOM` | copy | the section word is not in A55: agent choice, open for the owner's review |
| `o5` | Title `Como a Tali fala com você` | copy | A55 |
| `o5` | Subtitle (present in `o2`) | gold-only | dropped: A55 gives none |
| `o5` | `Option/Card` **Seco** — `Só os números. Sem opinião.`, selected, badge `PADRÃO` | copy + app | A55; the default badge as `o2` and `AeroOptionCard(badge)` |
| `o5` | `Option/Card` **Duro** — `Cobra o que estourou e o que faltou. Sem rodeio.` | copy | A55 |
| `o5` | Third option of `o2` | gold-only | dropped |
| `o5` | `Card/Note` `Dá para mudar nas configurações.` | copy | A55 footer, drawn as the `o2` note |
| `o5` | CTA `Concluir e começar` with the arrow | copy | moves from O4 (A55) |
| `cfg` | Row `Tom da Tali` · `Seco` (muted value, chevron) in the METAS E LIMITES block, after Macronutrientes | copy | A55 "profile block" |
| `cfgT` | `cfg` behind the scrim + `Sheet/Bottom` `Tom da Tali`, the two options, `Salvar` / `Cancelar` | copy + app | A55; the sheet pattern is the one of `homeW` (the `cfgS` gold has no sheet, the plan's reference was wrong) |
| `homeC` | `homeW` Home without the sheet (header, ring, macros, workout line, timeline) | app | kept |
| `homeC` | `Card/Closure` above the timeline: `Fechamento de 25 de setembro`, `1300 de 2175 kcal`, `P · C · G` against targets in the macro colors, missing meals, workout, text | copy | A55 scope 2 (numbers equal to the Home behind it) |
| `homeK` | `homeC` with a Sunday date and the week card above the day card: `Semana de 28 de setembro a 4 de outubro`, total and mean kcal, mean protein, `Dias sem registro: n`, the slot line, text | copy | A55 scope 3 |
| `homeC`/`homeK` | Collapsed card `Ontem: {kcal} de {teto} kcal` | copy | variant of `Card/Closure` only (no screen: A55 lists no gold for it) |

Components: `Option/Card`, `Card/Note`, `Row/Setting`, `Sheet/Bottom`, `Button/Primary`, `Header/Day`, `Ring/Day`, `Macro/Row` are reused. New: `Card/Closure` (State `Expanded` | `Collapsed`) and `Option/Tone` (the two tone options as one group of `Option/Card` instances, needed as `Sheet/Bottom` content; the plan's two-state `Option/Tone` would duplicate `Option/Card`).

Open points for the owner (not drawn, outside the plan's gold list): `o1`–`o4` still show `ONBOARDING n/4` and four segments; with O5 they become `n/5` (A55 changes the count). Existing defect seen in discovery: the `cfgR` frames (D15) sit at page level over the onboarding section instead of inside "Config e push".

### Build (Figma `Design`, 07/10/2026)

- Components (`Componentes`): `Stepper/Progress` gains `Step=5` (139:217, five segments); `Card/Closure` set 139:237 in "Cards" (`State=Expanded` 139:223, `State=Collapsed` 139:235; TEXT `Title`, `Kcal`, `P`, `C`, `G`, `Detail`, `Extra`, `Text`, `Line`; BOOLEAN `Show macros`, `Show extra`; Card/Note glass look, every fill and text bound to variables and text styles); `Option/Tone` 139:310 in "Opções" (two `Option/Card` instances, Seco with `PADRÃO`). Each carries a description.
- Screens (`Release 1`), Light row then Dark row, Dark = clone with the Color mode `Dark`:

| Gold | Light | Dark | Section |
|---|---|---|---|
| `o5` | 140:4452 | 140:4556 | Splash e onboarding · D4 (after `o4`) |
| `cfg` (changed: row `Tom da Tali` · `Seco`) | 78:3425 | 78:3664 | Config e push · D7 |
| `cfgT` | 140:4593 | 140:4720 | Config e push · D7 (after `push`) |
| `homeC` | 141:4742 | 141:4902 | Home · D3 (after `homeW`) |
| `homeK` | 141:4802 | 141:4945 | Home · D3 |

- Layout: the onboarding section grew by one frame and the Home section by two, so every section to their right moved right (160 px between sections kept); the Dark rows of Home and Config moved below their tallest Light frame; the `cfgR` frames (D15) moved from page level into "Config e push" at their own slot (fix of the defect found in discovery; ids unchanged).
- Read-back (one call): every D16 frame has zero solid paints without a variable, components are instances (5 to 18 per frame), no overlapping frames in the three sections, no raw solid in `Card/Closure`.
- Review images: the ten frames exported through the REST API with `node tools/export-figma.mjs --only <node ids> --dry-run --out <scratchpad>` (no MCP calls, nothing written to `docs/qa/`), checked by the agent in both themes and sent with the overnight report.
- Figma MCP calls: 12 (whoami 1, MCP skills 3, reads 3, writes 5 including one failed and rolled back call). Night budget 120 for all design plans.

### Owner review (the only manual step)

Open `Design` → `Release 1` and check `o5`, `cfg`, `cfgT`, `homeC`, `homeK` in both rows, plus `Card/Closure` and `Option/Tone` in `Componentes`. Points to decide: the eyebrow word `TOM`; no subtitle on `o5`; the closure texts are sample copy; `o1`–`o4` counters (`n/4`, four segments) are not part of this plan. After the OK: map the ids above in `tools/export-figma.mjs`, `node tools/export-figma.mjs --only o5 cfg cfgT homeC homeK`, add `o5`, `cfgT`, `homeC`, `homeK` to the inventory in `docs/qa/README.md`, `node tools/check-figma.mjs`.
