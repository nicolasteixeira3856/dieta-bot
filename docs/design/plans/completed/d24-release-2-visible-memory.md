# Plan — D24 Release 2: "O que a Tali sabe"

- Status: Concluído (08/10/2026)
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Memória", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/memL.png`, the changed `cfg` (entry row), and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D23 `Concluído` ([history](./)) (same release section order); [ADR-053](../../../produto/adrs/ADR-053-visible-memory-screen.md) accepted with A69.
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d24-release-2-visible-memory.md. Implemente o plano aprovado.`

## Objective

Draw the memory screen in Aero, in both themes: the facts Tali keeps, grouped, with origin and the delete and correct actions, as ADR-053 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `memL` | `cfg` rows; the dev tool A23 list (layout only, not a gold) | ADR-053 § 1: groups, category, slot, text, macros, origin, date; delete, correct |
| `cfg` (changed) | current `cfg` | one more entry row "O que a Tali sabe" |

Code: `apps/android/.../feature/config`.

## Scope

1. **Discovery (read only):** per gold, list every visible element and classify it; table in Results before any write.
2. **Components:** fact row (category chip, text, origin line), group header, correction field state.
3. **Screens** (section "Memória" on `Release 2`): Light `memL`, `cfg`; Dark clones; naming and spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate), then **Export**: `node tools/export-figma.mjs --only memL cfg`, inventory, `check-figma`.

## Out of scope

- Compose ([A69](../../../android/plans/a69-visible-memory.md)). O6 onboarding. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

### Owner decision (08/10/2026, D23 review)

Asked where the recipe list is opened from, the owner accepted the proposal to draw the Config entry of [A68](../../../android/plans/pending_manual_validation/a68-saved-recipes.md) here, since `cfg` changes in this plan anyway: a new block `DA TALI` between the note and `DADOS`, with two `Row/Setting` rows, **Receitas** and **O que a Tali sabe**, each with a chevron.

### Discovery (08/10/2026, before any write)

Read only: `whoami` (Student team, seat Full), the `Componentes` page (`Chip/Log`, `Field/Number`, `Card/Note`, `Chip/Memory`), the `cfg` frame read in D23. Code and specifications: [memoria-push](../../../produto/specifications/memoria-push.md) rules 2–7 (fact `id` `P`/`D`/`T`, `category` `preference` | `portion` | `routine`, `slot` and kcal/P/C/G of a routine, days seen, temporary facts expire 3 days after creation), the dev tool `DevMemoryScreen.kt` (A23, layout only), [A69](../../../android/plans/a69-visible-memory.md) scope 1.

`memL` (Config → O que a Tali sabe):

| Element | Class | Source |
|---|---|---|
| `Header/Page`: back and title `O que a Tali sabe` | app (new screen) | ADR-053 § 1 |
| Groups `FIXAS` (preferences and portions), `ROTINAS`, `TEMPORÁRIAS`, each with a one-line explanation | app (new) | A69 scope 1 (fixas, rotinas, temporárias); memoria-push rules 2, 4 |
| Fact row: category chip (`Preferência`, `Porção`, `Rotina · {slot}`), text, macros of a routine (`kcal · P · C · G`, macro colours), origin and date (`Declarado · dd/MM`, `Registrado N dias · último dd/MM`, `Até dd/MM · criado dd/MM`) | app (new) | ADR-053 § 1; memoria-push rules 2, 4 |
| Per fact: correct (pencil) and delete (trash) | app (new) | ADR-053 § 1 |
| Correction state: the text in a focused field, `Cancelar` and `Salvar`; category and slot unchanged | app (new) | ADR-053 § 1 (same category and slot) |
| Sample facts from the fixtures (`leite semidesnatado`, the `chatM` breakfast 430 · 26P · 36C · 20G) plus sample data | copy (fixtures and sample data) | `ChatFixtures.chatM` |
| Dev tool counters (`Permanente n/30 …`), `key`, ids, the raw line editor | gold-only of the dev tool (dropped) | — |
| Page bubbles | app | page background |

`cfg` (changed; a new frame on `Release 2`, the `Release 1` frame stays as history):

| Element | Class | Source |
|---|---|---|
| Every current block, row and the note | app (unchanged) | memoria-push |
| Block `DA TALI` with **Receitas** and **O que a Tali sabe** (chevron, no value) | app (new) | ADR-052 § 2, ADR-053 § 1; owner decision above |

New copy for the owner's review: `O que a Tali sabe`, `FIXAS`, `ROTINAS`, `TEMPORÁRIAS` and their explanations, `Preferência`, `Porção`, `Rotina · {slot}`, the origin lines, `Cancelar`, `Salvar` (existing app words), `DA TALI`, `Receitas`.

Outside this plan, listed for the owner: the delete confirmation (an existing `Dialog/Confirm`) and the empty state have no gold. The `cfg` change sits below the first screen, so `wipe`, `cfgR`, `cfgT` and `cfgS` (the Config under a dialog or sheet, cut at the first screen) do not change.

### Build (08/10/2026)

- **Components** (`Componentes`, new section `Memória · D24`, `197:242`):
  - `Header/MemoryGroup` (`197:243`): Label (Label/Section, `text/muted`) and Detail (Caption, `text/dim`);
  - `Row/Fact` (`197:277`), variants `State=Default` (`197:246`) and `State=Editing` (`197:267`); properties Text, Kcal, P, C, G, Origin, `Show macros`; the category is an exposed `Chip/Log` (Neutral); Corrigir (pencil) and Apagar (trash) in `icon/muted`; Editing puts the text in a focused field (`Surface/Glass`, `accent/default` 1.5, `radius/card`) with **Cancelar** and **Salvar**.
- **Frames** (`Release 2`, section `Memória · D24`, `198:382`, 160 px under `Receitas · D23`):

  | Gold | Light | Dark | Size |
  |---|---|---|---|
  | `memL` | `198:385` | `198:867` | 390 × 1170 (hug) |
  | `cfg` | `198:740` | `198:988` | 390 × 1104 → 1271 (hug) |

  `memL` reuses the `cfg` frame, header and glass group; three blocks `FIXAS` (two preferences and a portion in the correction state), `ROTINAS` (breakfast and lunch with macros) and `TEMPORÁRIAS` (one fact). `cfg` is the `Release 1` frame cloned to `Release 2` with the block `DA TALI` (**Receitas**, **O que a Tali sabe**, chevron only) between the note and `DADOS`; the `Release 1` frame stays as history.
- **Dark:** clones in mode Dark; 5 text ranges outside instances rebound with the Dark resolved value.

### Validation (before the owner review)

1. Owner decision and discovery tables above, written before the first write.
2. Read-back (`use_figma`): instances `memL` 10, `cfg` 14 per frame; zero visible solid paints without a variable or style in the four frames and the two new components; no overlapping siblings in any frame or in the section; Light frames in mode Light, Dark frames in mode Dark; both components carry a description.
3. Review images from `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls), one per frame, sent to the owner.

Figma MCP budget: 6 of 40 calls (whoami, 1 read, 2 writes, 1 failed write retried, 1 read-back); the Figma skills were already loaded in this session by D23.

### Owner review

**OK (08/10/2026)** on the first round, with no fixes ("Está perfeito, pode exportar e concluir o plano"), including the correction state inside `memL`.

### Export (08/10/2026)

- `tools/export-figma.mjs`: `memL` added (Light `198:385`, Dark `198:867`); `cfg` now points to the `Release 2` frames (Light `198:740`, Dark `198:988`).
- `node tools/export-figma.mjs --only cfg,memL`: `memL` 780 × 2340 (new, both themes); `cfg` 780 × 2208 → 780 × 2542 (both themes).
- Inventory: `memL.png` added to [docs/qa/README.md](../../../qa/README.md); the golds-per-flow table of the [plans README](../README.md) gains the visible-memory row.
- `node tools/check-figma.mjs`: 106 golds verified (53 dark + 53 light). `node tools/check-docs.mjs` passes.
- Gold check of the current app (no `apps/` change; `GoldTest.cfg*`, `--rerun`): all eight pass. `cfg` blurred 0.24 % Dark and 0.11 % Light (the test draws the frame at its 1104 dp qualifier, above the new block); `cfgR`, `cfgS`, `cfgT` unchanged.
- **Hand-over:** [A68](../../../android/plans/pending_manual_validation/a68-saved-recipes.md) and [A69](../../../android/plans/a69-visible-memory.md) draw the `DA TALI` block (the first of them adds the block with its own row) and move the `cfg` qualifier to 1271 dp; A69 draws `memL`.

Figma MCP budget: 6 of 40 calls in total.
