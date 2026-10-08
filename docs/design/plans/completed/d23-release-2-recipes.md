# Plan — D23 Release 2: Recipes

- Status: Concluído (08/10/2026)
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Receitas", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{rcpL,rcpD}.png`, the changed `chatRK` (Salvar receita action), and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D22 `Concluído` ([history](../completed/)); [ADR-052](../../../produto/adrs/ADR-052-saved-recipes.md) accepted with S38.
- Figma MCP budget: ≤ 60 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d23-release-2-recipes.md. Implemente o plano aprovado.`

## Objective

Draw the recipe list and detail in Aero, in both themes, from the design system, plus the "Salvar receita" action under a cooking plan, with the features ADR-052 defines and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `rcpL` | `cfg` list rows (Config) | ADR-052 § 2: most recent first, name, kcal · P/C/G |
| `rcpD` | `chatRK` table and steps | ADR-052 § 1: ingredients with grams, steps, totals, version, delete |
| `chatRK` (changed) | current `chatRK` | ADR-048 action row + "Salvar receita" |

Code: `apps/android/.../feature/config`, `feature/chat`.

## Scope

1. **Discovery (read only):** per gold, list every visible element and classify it: `app`, `gold-only` (dropped) or `copy`; the table goes into Results before any write.
2. **Components:** recipe row (name, totals), recipe header (name, version, totals), reuse of the table and numbered list of `chatRK`.
3. **Screens** (section "Receitas" on `Release 2`): Light row `rcpL`, `rcpD`, `chatRK`; Dark clones; frame names `<id> · <title> · Light|Dark`.
4. **Owner review** (Figma review gate): screenshots per frame; plan to `pending_manual_validation/` until the owner's OK.
5. **Export** after the OK: ids in `tools/export-figma.mjs`, `node tools/export-figma.mjs --only rcpL rcpD chatRK`, inventory in `docs/qa/README.md`, `node tools/check-figma.mjs`.

## Out of scope

- Compose ([A68](../../../android/plans/a68-saved-recipes.md)). Behavior or copy changes. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

### Discovery (08/10/2026, before any write)

Read only: `whoami` (Student team, seat Full), the Figma skills, the pages, variables and styles, the `Componentes` page, and the Release 1 frames `cfg` (`78:3425`), `chatRK` (`147:5180`) and `chatRL` (`151:5326`). Code: `ConfigScreen.kt` (rows, header), `ChatFixtures.chatRK`, `AeroReply.kt` (portions table). The file has no `Release 2` page yet: this plan creates it (ADR-031 § 2), with the sections in release order (Receitas, then Memória of D24 and Opções of D25).

`rcpL` (Config → Receitas; reference `cfg` rows, layout only):

| Element | Class | Source |
|---|---|---|
| `Header/Page`: back and title `Receitas` | app (new screen) | ADR-052 § 2; A68 scope 5 (`Config → Receitas`) |
| One row per saved recipe: name, `kcal · P · C · G` with the semantic macro colours, chevron | app (new) | ADR-052 § 2 |
| Order: most recent first | app (new) | ADR-052 § 2 |
| Sample recipes: `Frango com brócolis e arroz` (520 · 46P · 41C · 17G, `chatRK`) and `Macarrão com atum ao sugo` (620 · 42P · 70C · 18G, `chatRB`), plus three more sample dishes | copy (fixtures; the extra dishes are sample data) | `ChatFixtures` |
| `cfg` block labels, value column, `Card/Note`, `Resetar app` | gold-only for this screen (dropped) | — |
| Page bubbles | app | page background |

`rcpD` (recipe detail; reference `chatRK` table and steps, layout only):

| Element | Class | Source |
|---|---|---|
| `Header/Page`: back and title `Receita` | app (new) | ADR-052 § 2 |
| Recipe header: name, `Versão 1 · salva em 25 de setembro`, totals `kcal · P · C · G` | app (new) | ADR-052 § 1 (version, totals) |
| `INGREDIENTES`: `Table/Portions` (Item, Gramas) | app (new) | ADR-052 § 1; `chatRK` table |
| `MODO DE PREPARO`: `List/Step` × 3 | app (new) | ADR-052 § 1; `chatRK` steps |
| `Excluir receita` (danger action) | app (new) | ADR-052 § 1; A68 scope 5 |
| Day projection (`Card/MealPlan`), message time, bubble, `Registrar assim` | gold-only for the detail (dropped: the detail is not a Chat turn) | — |
| Yield and portion of the entity | not drawn: outside the plan's behavior source (ingredients, steps, totals, version, delete) | ADR-052 § 1 |

`chatRK` (changed; a new frame on `Release 2`, the `Release 1` frame stays as history):

| Element | Class | Source |
|---|---|---|
| Header, date chip, user bubble, recipe bubble (table, steps, total, day projection, time), `Registrar assim`, composer | app (unchanged) | A60, A61 |
| `Salvar receita` (`Chat/ActionBar`, Phosphor `bookmark-simple` regular) under `Registrar assim` in the thread action stack | app (new) | ADR-052 § 2; ADR-048 decision 1; A68 scope 2 |

New copy for the owner's review: `Receitas`, `Receita`, `Versão 1 · salva em 25 de setembro`, `INGREDIENTES`, `MODO DE PREPARO`, `Excluir receita`, `Salvar receita` (ADR-052).

Outside this plan, listed for the owner: the `Receitas` entry row in `cfg` (A68 scope 5; no design plan draws it, `cfg` changes in D24), the delete confirmation (an existing `Dialog/Confirm`), the empty list and the receipt `Receita salva` have no gold.

Icon: `bookmark-simple` (regular) is not in the file; its official SVG comes from `@phosphor-icons/core` 2.1.1 (as D11 and D18 did).

### Build (08/10/2026)

- **Page.** `Release 2` (`192:2`) created after `Release 1`, with its title and description (`192:3`), cloned from the `Release 1` page header. Section `Receitas · D23` (`192:6`) with heading and note, at 160 px under the header.
- **Components** (`Componentes`, new section `Receitas · D23`, `190:248`):
  - `Icon/bookmark-simple (regular)` (`190:242`), a cell of the `Ícones` grid (`190:241`), from the official SVG, fill `icon/primary`;
  - `Row/Recipe` (`190:249`): Name, Kcal, P, C, G text properties; the `Row/Setting` paddings and chevron; totals in Caption with the macro colours;
  - `Header/Recipe` (`190:262`): Name (Title), Version (Caption, `text/muted`), Kcal, P, C, G (Body with the macro colours).
- **Frames** (Light row at y 208, Dark row 80 px under the tallest Light frame, 80 px between screens):

  | Gold | Light | Dark | Size |
  |---|---|---|---|
  | `rcpL` | `192:9` | `193:277` | 390 × 844 (fixed: the list does not fill the screen) |
  | `rcpD` | `192:226` | `193:356` | 390 × 871 (hug) |
  | `chatRK` | `192:438` | `193:422` | 390 × 1067 → 1125 (hug; the new action adds 48 + 10) |

  `rcpL` and `rcpD` reuse the `cfg` frame (paint style, paddings, page bubbles, `Header/Page`) and its glass `Group` and block label; `rcpD` holds `Header/Recipe` in a group, `Table/Portions`, three `List/Step` and `Chat/ReceiptAction` (Danger, trash). `chatRK` is the `Release 1` frame cloned to `Release 2` with `Chat/ActionBar` **Salvar receita** under **Registrar assim** in the thread's `Actions` stack; the `Release 1` frame stays as history.
- **Dark:** each Light frame cloned, Color mode Dark (`2:1`); 12 text ranges outside instances rebound with the Dark resolved value as placeholder (D20 gotcha).

### Validation (before the owner review)

1. Discovery tables above, written before the first write.
2. Read-back (`use_figma`):
   - instances: `rcpL` 6, `rcpD` 7, `chatRK` 12 per frame, none detached;
   - zero visible solid paints without a variable or style, in the six frames and the three new components;
   - no overlapping siblings in any frame or in the section;
   - Light frames in mode Light (`2:0`), Dark frames in mode Dark (`2:1`);
   - the three components carry a description.
3. Review images: `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls), one per frame, sent to the owner. One fix before sending: the totals separators lost their spaces (trailing spaces of an auto-width text), now `·` with a 5 px gap.

Figma MCP budget: 15 of 60 calls (whoami, 3 skill reads, 4 reads, 5 writes, 2 failed writes retried, 1 read-back).

### Owner review

**OK (08/10/2026)** on the first round, with no fixes ("telas aprovadas, exporte tudo e pode continuar"). Asked where the list is opened from: Config → Receitas (ADR-052 § 2). The owner accepted the proposal to draw that entry row in `cfg` within D24, in a new block with "O que a Tali sabe".

### Export (08/10/2026)

- `tools/export-figma.mjs`: `rcpL` (Light `192:9`, Dark `193:277`) and `rcpD` (Light `192:226`, Dark `193:356`) added; `chatRK` now points to the `Release 2` frames (Light `192:438`, Dark `193:422`); the map comment names both release pages.
- `node tools/export-figma.mjs --only chatRK,rcpL,rcpD`: `rcpL` 780 × 1688 and `rcpD` 780 × 1742 (new, both themes); `chatRK` 780 × 2134 → 780 × 2250 (both themes).
- Inventory: `rcpL.png · rcpD.png` added to [docs/qa/README.md](../../../qa/README.md); the golds-per-flow table of the [plans README](../README.md) gains the `Release 2` recipes row.
- `node tools/check-figma.mjs`: 104 golds verified (52 dark + 52 light). `node tools/check-docs.mjs` passes.
- Gold check of the current app (no `apps/` change; `GoldTest.chatRK*`, run with `--rerun`): the whole frame is report-only (blurred 3.30 % Dark, 2.67 % Light); the header region passes (0.22 %, 0.29 %); the bottom region fails (4.50 % Dark, 3.08 % Light), by design: the app does not draw **Salvar receita** yet.
- **Hand-over to [A68](../../../android/plans/a68-saved-recipes.md):** the action under a cooking plan, the list and detail against `rcpL` and `rcpD`, and the `chatRK` qualifier at 1125 dp. Until A68, `GoldTest.chatRK_dark` and `chatRK_light` fail on their bottom region; a client plan that runs before A68 needs a temporary exception there (owner decision). The Config entry row is drawn by [D24](d24-release-2-visible-memory.md).

Figma MCP budget: 15 of 60 calls in total (no call after the review: the export uses the REST API).
