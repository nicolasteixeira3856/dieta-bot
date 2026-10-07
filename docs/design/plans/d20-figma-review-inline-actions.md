# Plan — D20 Figma review: stale frames and Chat actions in the thread

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Splash e onboarding", "Chat", "Chat · D6" and "Config e push" (only the frames listed below); components in `Componentes` only if a variant is missing. Repository: `docs/qa/figma/{dark,light}/<ids>.png` of the changed frames, `tools/export-figma.mjs` only if a node id changes, and `tools/diff-gold.mjs` rules that exist only because of a stale frame.
- Prerequisites: D12, D16, D17, D18 and D19 `Concluído` ([history](completed/)); A60 merged (the app the frames are compared with, 0.0.18-dev).
- Figma MCP budget: ≤ 90 calls (at most 120 a day, ADR-031 § 6).

One review plan for several flows by owner decision (07/10/2026): the frames are small corrections of existing golds plus one layout change (the Chat actions), and the client plan [A61](../../android/plans/a61-chat-copy-scroll-capture-inline-actions.md) waits for all of them. Approving this plan accepts [ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d20-figma-review-inline-actions.md. Implemente o plano aprovado.`

## Objective

Bring every gold that no longer matches the app or the live specifications back in line, in both themes, and redraw the Chat frames whose actions sit pinned above the composer so the actions sit in the thread under their message (ADR-048 decision 1). After this plan, the A60 visual QA exceptions (`tools/diff-gold.mjs` gold conflicts added for stale frames, the `GoldTest` report-only `cfgS`) are no longer needed.

## Sources (feature parity, ADR-031 § 4)

| Gold | What is stale or changes | Behavior source |
|---|---|---|
| `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` | Actions (Forçar estimativa; Registrar; Registrar assim + Reservar; Pode passar · Ajustar para caber) drawn in the slot pinned above the composer | ADR-048 decision 1; ADR-026, ADR-028, ADR-039, ADR-046 for what each action is |
| `chatRB` | D12 drew the plan as plain lines; the app renders the D17 blocks (bullets with the 17 dp marker column, numbered steps with the 21 dp column, 12 dp between blocks, P/C/G colours) | ADR-045, [Chat](../../produto/specifications/chat.md) rule 2 |
| `chatR`, `chatE` (Dark) | The Dark clones paint the reply's first paragraph in the bubble colour, so it is invisible | Light frames of the same ids |
| `o1`, `o1e`, `o2`, `o3`, `o3s`, `o3t`, `o4` | Progress reads `n/4`; onboarding has 5 steps since D16 (`n/5`); `o4` CTA reads Concluir e começar, the app reads **Continuar** (Concluir e começar moved to `o5`) | [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) rules 4 and 4a, ADR-044 |
| `cfgS` | Predates the `Tom da Tali` row of the targets card | [memoria-push](../../produto/specifications/memoria-push.md) rule 12 |
| `cfgT` (Light) | Config behind the sheet is sharp under the scrim; the app's shared Sheet/Bottom blurs the screen behind it, as the `chatT` frame draws | `chatT` frame; the shared `AeroSheet` |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/{chat,onboarding,config}`; latest app captures in `docs/qa/android/current/{dark,light}/` (A60 run, 0.0.18-dev).

## Scope

1. **Discovery (read only):**
   - **Sweep:** every gold of the inventory in [docs/qa/README.md](../../qa/README.md) against its latest app capture and the live specifications; each difference classified `stale gold` (the app follows the spec; the frame changes here), `app gap` (the frame follows the spec; the app changes in A61), `ignored` (clock, status bar, sample text, long-thread crop) or `known` (the table above). A stale gold outside the table is added to this plan's frames only if it is a correction of the same kind (copy, counter, colour, a row the spec added); anything that changes a layout or a behavior is listed for the owner and stays out.
   - **Per changed frame:** every visible element classified `app`, `gold-only` or `copy`, as in the template.
   - Both tables go into Results before any write.
2. **Chat actions in the thread** (`chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`):
   - the action stack moves from the composer slot into the thread, under its message: after the bubble, or after its budget lines (`chatRB`) or its `Reservado para o Jantar` mark (`chatRL`); gaps from the spacing tokens (the 8–12 dp of the existing under-bubble elements to the first action, 10 dp between stacked actions as today, then the thread's 16 dp to anything below);
   - components unchanged: `Chat/ActionBar` (full width of the thread, 20 dp margins), the choice pills, the `Forçar estimativa` bar;
   - the composer sits alone at the bottom with the frame's existing bottom margin; when the thread is short the stack sits right under the message, not above the composer;
   - long-thread frames keep showing the whole thread (the export height follows the content).
3. **Corrections** (no layout change): `chatRB` plan text in D17 blocks with the macro colours; `chatR`, `chatE` Dark first paragraph in `text/primary`; `o1`–`o4` counter `n/5` and the `o4` CTA **Continuar** (same component and arrow as the other steps); `cfgS` with the `Tom da Tali` · `Seco` row after the macros, as `cfg`; `cfgT` Light with the page behind the sheet blurred as `chatT` (Dark only if the sweep finds the same).
4. **Owner review** (Figma review gate): one screenshot per changed frame; the plan goes to `pending_manual_validation/` until the owner's OK in Figma; fixes follow with one screenshot per changed frame.
5. **Export** after the OK:
   - `node tools/export-figma.mjs --only <changed ids>`; untouched golds keep their bytes; node ids in `tools/export-figma.mjs` only if a frame was replaced;
   - `node tools/check-figma.mjs`;
   - `tools/diff-gold.mjs`: the gold-conflict entries that existed only because of a stale frame (`chatRB` tail start, `themeConflicts` of `chatE` Dark and `cfgT` Light, and the `cfgT` sheet region) are removed; long-thread rules stay;
   - Results list every exported id and the `app gap` items handed to A61.

## Out of scope

- Compose (A61). Behavior or copy changes beyond ADR-048 and the live specifications. A new frame for copying text (platform selection, ADR-048 decision 2) or for the scrolling screenshot (system UI). Frames not listed, unless the sweep adds a correction of the same kind. `land`, `landM`, `priv` (site).

## Validation

1. The sweep and the discovery tables exist in Results before the first write.
2. Read-back: instances for every DS component, no raw hex outside the variables, no overlapping nodes; the moved action stacks are children of the thread, not of the footer.
3. Visual: one screenshot per changed frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
