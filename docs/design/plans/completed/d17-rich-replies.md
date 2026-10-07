# Plan — D17 Emphasis, lists and a table in the Chat bubbles

- Status: Concluído (07/10/2026, owner OK, golds exported)
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Chat core" and "Chat records and memory"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatR,chatE,chatRK}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D5 and D6 `Concluído` ([history](../completed/)); [D12](d12-plan-budget-choice.md) and [D16](d16-tone-and-closures.md) may run in parallel (other frames); `chatR` is shared with D12, so this plan starts after D12 is `Concluído` or draws on its frame.
- Figma MCP budget: ≤ 70 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-045](../../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/completed/d17-rich-replies.md. Implemente o plano aprovado.`

## Objective

Draw how bold, bullets, numbered steps and the two-column table look inside an assistant bubble, in Aero, both themes, with the content a plan, a log and a recipe produce today and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatR` (changed) | current `chatR` (plan bubble with the projected-day panel) | ADR-045 decisions 1, 4; ADR-043 decision 5 (two options as bullets) |
| `chatE` (changed) | current `chatE` (log estimate with Registrar) | ADR-045 decision 1 (bold numbers only) |
| `chatRK` | `chatR` with a recipe: ingredient table and numbered steps | ADR-045 decision 1; ADR-039 cooking rule |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/chat`.

## Scope

1. **Discovery (read only):** per gold, every visible element classified `app`, `gold-only` or `copy`; the table goes into Results before any write. The sample texts are synthetic plans and recipes written for the frames, not tester text.
2. **Components** (in `Componentes`, readability rule): `Text/BubbleStrong` (the strong body style inside a bubble, both themes), `List/Bullet` and `List/Step` rows (marker column, text column, list spacing token), `Table/Portions` (two columns, header row, up to six rows, inside the bubble width). Macro colours stay on the P/C/G line.
3. **Screens:** Light row `chatR`, `chatE`, `chatRK`, 390 px wide; Dark row as clones with the `Dark` mode; frame names `<id> · <title> · Light|Dark`; spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate): one screenshot per frame; `pending_manual_validation/` until the owner's OK; fixes with one screenshot per changed frame.
5. **Export** after the OK: ids in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only chatR chatE chatRK`; `chatRK` added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`.

## Out of scope

- Compose (A57). Server (S26). Any marker beyond the ADR-045 subset. Other bubbles and states.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances for every DS component, no raw hex fills outside the variables, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Approved by the owner on 06/10/2026 (overnight run, message naming this file). Built by the agent the same night. Prerequisite: D12 is in `pending_manual_validation/` and drew a new frame (`chatRB`) without touching `chatR`, so this plan draws on `chatR` without conflict.

### Discovery (read only, before the first write)

Sources: the live `chatR` (72:2959) and `chatE` (62:1930) frames, ADR-045 decisions 1, 4 and 5, ADR-043 decision 5, ADR-039 cooking rule. Sample texts are synthetic, written for the frames.

| Gold | Element | Class | Decision |
|---|---|---|---|
| `chatR` | Header, date chip, composer, **Registrar assim**, `Card/MealPlan` | app | kept |
| `chatR` | User message (a named dish) | copy | rewritten as an open request (`Não sei o que jantar. Me dá umas ideias?`): ADR-045 draws `chatR` with two options as bullets, which ADR-043 gives only to an open request |
| `chatR` | Plan text | copy | `Duas opções para o jantar:`, two `List/Bullet` rows (dish name and kcal in bold), `Primeira opção: ~420 kcal · 40P · 38C · 12G` (bold numbers, macro colors over the bold) |
| `chatE` | Everything but the message | app | kept |
| `chatE` | Message | copy | grams in bold: `Identifiquei 2 pães franceses (100 g) e 2 ovos mexidos (100 g). A estimativa total é de:` |
| `chatRK` | `chatR` frame with a recipe: dish name in bold, `Table/Portions` (Item · Gramas, six rows, one `(opcional)`), three `List/Step` rows, total line, projected day, **Registrar assim** | copy + app | new gold (ADR-045 decision 5) |

Components: the strong body style is the existing text style `Body/Strong` (a `Text/BubbleStrong` component would duplicate it); new `List/Bullet`, `List/Step` and `Table/Portions` in `Componentes` → "Chat".

### Build (Figma `Design`, 07/10/2026)

- Components (`Componentes` → "Chat"): `List/Bullet` 146:233 (TEXT `Text`), `List/Step` 146:236 (TEXT `Number`, `Text`), `Table/Portions` 146:239 (header Item · Gramas, divider `border/line`, six rows with TEXT `Item n` / `Grams n`, BOOLEAN `Show row 5/6`); marker and text columns `space/sm` apart; each with a description.
- Screens (`Release 1`):

| Gold | Light | Dark | Section |
|---|---|---|---|
| `chatR` (changed) | 72:2959 | 72:3312 | Chat · D6 |
| `chatE` (changed) | 62:1930 | 63:2136 | Chat · D5 |
| `chatRK` (new) | 147:5180 | 147:5274 | Chat · D6 (after `chatS`) |

- Bold ranges use the `Body/Strong` text style; the P/C/G figures keep `macro/*` over the bold. Layout: Chat · D6 grew by one frame (Config e push moved right, 160 px kept), its Dark row moved below the tallest Light frame, the Chat column re-stacked with 160 px between sections; no overlapping sections.
- Read-back: zero solid paints or text ranges without a variable in the six frames; components as instances (7 to 11 per frame).
- Review images: the six frames exported with the REST dry run (no MCP calls) and checked in both themes. Note for the owner: in `chatR` the long first option wraps `(60 g)` across lines and `· 12G` goes to its own line at 270 px; that is how the subset wraps in a bubble.
- Figma MCP calls: 4 (two reads, two writes). Night total after D17: 18 of 120.

### Owner review (the only manual step)

Open `Design` → `Release 1` → "Chat · D5" (`chatE`) and "Chat · D6" (`chatR`, `chatRK`), plus the three components. After the OK: map `chatRK` (147:5180 / 147:5274) in `tools/export-figma.mjs`, `node tools/export-figma.mjs --only chatR chatE chatRK`, add `chatRK` to the inventory of `docs/qa/README.md`, `node tools/check-figma.mjs`.

### Owner OK and export (07/10/2026)

The owner gave the Figma review OK on 07/10/2026 (chat message asking to conclude every plan pending manual validation). Mapped in `tools/export-figma.mjs`: `chatRK` (Light 147:5180, Dark 147:5274), added to the inventory; `chatR` and `chatE` keep their ids. `node tools/export-figma.mjs --only chatR,chatE,chatRK`: `chatRK` new in both themes; `chatE` changed (2.9% dark, 5.0% light); `chatR` exported together with D18 (one frame carries both changes). `node tools/check-figma.mjs`: 92 golds verified (46 dark + 46 light). `node tools/check-docs.mjs` passes. Plan moved to `completed/`. Until [A57](../../../android/plans/a57-rich-reply-rendering.md) lands, the app `GoldTest` for `chatR` and `chatE` diverges from the new golds (expected).
