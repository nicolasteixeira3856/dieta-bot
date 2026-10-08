# Plan — D25 Release 2: Plan options in the bubble

- Status: Concluído (08/10/2026)
- Date: 08/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Opções", new component in `Componentes`. Repository: `docs/qa/figma/{dark,light}/chatO.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D24 `Concluído` ([history](./)) (same release section order); [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md) accepted with S37.
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d25-release-2-plan-options.md. Implemente o plano aprovado.`

## Objective

Draw a plan bubble with two options in Aero, in both themes: each option with its name, items with grams, total and protein, and its own action row (Registrar, Reservar), as ADR-051 defines.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatO` | `chatR` (plan bubble) and `chatRL` (reserve action row) | ADR-051 § 1: `Opção 1: {name}` / `Opção 2: {name}`, items, totals, protein; ADR-048 actions per option |

Code: `apps/android/.../feature/chat`.

## Scope

1. **Discovery (read only):** list every visible element of `chatR` and `chatRL` and classify it; table in Results before any write.
2. **Component:** option block (title, item lines, total line, action row), two instances in the bubble; readability rule of ADR-031 § 3.
3. **Screen** (section "Opções" on `Release 2`): Light `chatO`; Dark clone; naming and spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate), then **Export**: `node tools/export-figma.mjs --only chatO`, inventory in `docs/qa/README.md`, `node tools/check-figma.mjs`.

## Out of scope

- Compose ([A67](../../../android/plans/a67-plan-options-and-discovery.md)). The discovery message (text only, no new layout). Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back confirms instances, no raw hex fills, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

### Discovery (08/10/2026, before any write)

Read only: the `chatR` and `chatRL` golds and frames, `ChatFixtures.optionsBubble` (the two dinner options), [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md) § 1, [ADR-048](../../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md) § 1, [A67](../../../android/plans/a67-plan-options-and-discovery.md) scope 1.

`chatO` (open request answered with two options; references `chatR` and `chatRL`, layout only):

| Element | Class | Source |
|---|---|---|
| Header, date chip, user bubble `Não sei o que jantar. Me dá umas ideias?`, `Tali` label, time, composer, page bubbles | app (unchanged) | `chatR` |
| Lead line `Duas opções para o jantar:` | app | `ChatFixtures.optionsBubble` |
| Option block × 2 inside the bubble: title `Opção 1: Pizza de pão sírio` / `Opção 2: Omelete de forno`, one line per item with its grams, total `~kcal · P · C · G` with the macro colours | app (new layout) | ADR-051 § 1; items and grams from the fixture |
| Per option, its own action row: **Registrar** and **Reservar** side by side | app (new) | ADR-051 § 1 ("fiz a 2", "travar a 1"); ADR-048 actions; A67 scope 1 |
| Day projection (`Card/MealPlan`, `Dia: 1.640 → 2.060 de 2.200 kcal`) after the options | app (unchanged: the estimate describes option 1) | ADR-051 § 1 |
| Bullets with the bold name and `· 420 kcal` (prose options) | gold-only after this plan (dropped: replaced by the option blocks) | `chatR` |
| `Primeira opção: ~420 kcal · 40P · 38C · 12G` | gold-only after this plan (dropped: each option shows its own total) | `chatR` |
| **Registrar assim** and **Reservar para o Jantar** under the bubble | gold-only for a plan with options (dropped: the actions move into each option) | `chatR` |
| `Reservado para o Jantar` mark | not drawn (state after a reservation: `chatRL`) | ADR-046 |

Option 2 totals are sample data: `~360 kcal · 28P · 14C · 20G` (the fixture gives only 360 kcal). New copy for the owner's review: `Opção 1: …`, `Opção 2: …`, `Registrar`, `Reservar` (existing app words).

### Build (08/10/2026)

- **Component** (`Componentes`, new section `Opções · D25`, `201:246`): `Chat/PlanOption` (`201:247`), 270 wide (the bubble content), outline `border/line` 1, `radius/card`, padding 12. Title (Body/Strong), up to five exposed `List/Bullet` items (`Show item 4`, `Show item 5`), total `~kcal · P · C · G` (Body, macro colours) and its own action row: two `Chat/ActionBar` side by side, **Registrar** (check-circle) and **Reservar** (calendar-check), 118 px each.
- **Frame** (`Release 2`, section `Opções · D25`, `202:750`, 160 px under `Memória · D24`): `chatO` Light `202:753`, Dark `202:916`, 390 × 1100 (hug). Cloned from `chatR` (`Release 1`, unchanged): in the bubble the lead line, `Option 1` (five items, ~420 kcal · 40P · 38C · 12G) and `Option 2` (three items, ~360 kcal · 28P · 14C · 20G), then the day projection and the time; the bullets, the `Primeira opção` line and the action stack under the bubble removed.
- **Dark:** clone in mode Dark; 2 text ranges outside instances rebound with the Dark resolved value.

### Validation (before the owner review)

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`): 8 instances per frame; zero visible solid paints without a variable or style in both frames and the component; no overlapping siblings in the frames or the section; Light in mode Light, Dark in mode Dark; the component carries a description.
3. Review images from `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls), one per frame, sent to the owner.

Figma MCP budget: 6 of 40 calls (whoami, 2 reads, 1 failed read retried, 2 writes, 1 read-back).

### Owner review

**OK (08/10/2026)** on the first round, with no fixes ("Aprovado, pode exportar e concluir o plano"), with the short labels **Registrar** and **Reservar** and the day projection kept after the options.

### Export (08/10/2026)

- `tools/export-figma.mjs`: `chatO` added (Light `202:753`, Dark `202:916`).
- `node tools/export-figma.mjs --only chatO`: 780 × 2200 (new, both themes).
- Inventory: `chatO.png` added to [docs/qa/README.md](../../../qa/README.md); the golds-per-flow table of the [plans README](../README.md) gains the plan-options row.
- `node tools/check-figma.mjs`: 108 golds verified (54 dark + 54 light). `node tools/check-docs.mjs` passes.
- No app gate changes: `chatO` has no `GoldTest` yet, and `chatR` and `chatRL` keep their golds.
- **Hand-over to [A67](../../../android/plans/a67-plan-options-and-discovery.md):** the option block per `options[]` entry, the action row per option, the `GoldTest` and capture of `chatO`.

Figma MCP budget: 6 of 40 calls in total.
