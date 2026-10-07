# Plan — D19 Skips next to other actions: two receipts and the delete proposal

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat · D6" (records and memory); components in `Componentes` only if a variant is missing. Repository: `docs/qa/figma/{dark,light}/{chatSK,chatSD}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: D6 and D9 `Concluído` ([history](completed/)); independent of S29 (the copy is fixed by ADR-047).
- Figma MCP budget: ≤ 30 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-047](../../produto/adrs/ADR-047-skips-alongside-other-actions.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d19-skips-with-other-actions.md. Implemente o plano aprovado.`

## Objective

Draw the two Chat states ADR-047 adds, in Aero, both themes: an answer that recorded a meal and skipped another one (two receipts), and the proposal to delete a record when the user skips a meal that has one.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatSK` | `chatG` (answer with the receipt and its action stack) | ADR-047 decision 2: the log receipt with its actions, then `Pulado {slot}` with **Desfazer** only |
| `chatSD` | `chatU` (the in-conversation confirmation card below an answer) | ADR-047 decision 3: `Pular {slot}?`, `{slot} tem {kcal} kcal registrados. O registro sai e o {slot} fica pulado.`, **Excluir e pular** (danger) · **Manter registro** (outline) |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/chat`.

## Scope

1. **Discovery (read only):** per gold, every visible element classified `app`, `gold-only` or `copy`; the table goes into Results before any write. Sample texts are synthetic (no tester text): `chatSK` uses a message like `Pulei o pré-treino. No café comi 2 ovos mexidos e 1 pão francês.`, with a profile that has a `Pré-treino` slot.
2. **Components:** reuse the receipt row, `Chat/ReceiptAction` and the `chatU` card. The danger primary takes the `status/bad` treatment already used by Excluir and `Dialog/Confirm` Tone=Danger; add a card variant only if the `chatU` card has no danger tone. The `Registro mantido` mark is the existing receipt mark style (icon + `muted` label), not drawn as its own gold.
3. **Screens** (section "Chat · D6", after `chatRL`): Light row `chatSK`, `chatSD`, 390 px; Dark row as clones with the `Dark` mode; frame names `<id> · <title> · Light|Dark`; spacing per ADR-031 § 3. Long label check: a 40-character slot name in `Pular {slot}?` and two-line wrapping of **Excluir e pular** at large text.
4. **Owner review** (Figma review gate): one screenshot per frame; `pending_manual_validation/` until the owner's OK; fixes with one screenshot per changed frame.
5. **Export** after the OK: ids in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only chatSK,chatSD`; both ids added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`. Existing golds keep their bytes.

## Out of scope

- Compose (A59). Server (S29). Home. Other Chat states; `chatG` and `chatU` themselves.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back: instances for every DS component, no raw hex outside the variables, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
