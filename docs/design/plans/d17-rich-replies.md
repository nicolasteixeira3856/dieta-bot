# Plan — D17 Emphasis, lists and a table in the Chat bubbles

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Chat core" and "Chat records and memory"; new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatR,chatE,chatRK}.png` and the node ids in `tools/export-figma.mjs`.
- Prerequisites: D5 and D6 `Concluído` ([history](completed/)); [D12](pending_manual_validation/d12-plan-budget-choice.md) and [D16](pending_manual_validation/d16-tone-and-closures.md) may run in parallel (other frames); `chatR` is shared with D12, so this plan starts after D12 is `Concluído` or draws on its frame.
- Figma MCP budget: ≤ 70 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d17-rich-replies.md. Implemente o plano aprovado.`

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

Planning only.
