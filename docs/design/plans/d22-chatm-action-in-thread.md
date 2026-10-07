# Plan — D22 Chat: `chatM` with Registrar in the thread

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat · D6", frames `chatM` Light (`72:3034`) and Dark (`72:3333`) only; no new component. Repository: `docs/qa/figma/{dark,light}/chatM.png`; `tools/export-figma.mjs` only if a node id changes.
- Prerequisites: D20 `Concluído` ([history](completed/)); [A61](../../android/plans/pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) merged (0.0.19-dev: the app the frame is compared with).
- Figma MCP budget: ≤ 12 calls (at most 120 a day, ADR-031 § 6).

One frame, two themes. It was found during A61: `chatM` shows **Registrar** (an `ask` estimate), but it was not in the D20 list. The gold still draws Registrar in the slot pinned above the composer, while the app has drawn it in the thread since A61 ([ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md) decision 1). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d22-chatm-action-in-thread.md. Implemente o plano aprovado.`

## Objective

Bring `chatM` in line with ADR-048 in both themes. **Registrar** moves from the footer into the thread, right under its answer, as D20 drew it for `chatE`. The composer stays alone at the bottom. Nothing else in the frame changes.

## Sources (feature parity, ADR-031 § 4)

| Gold | What is stale | Behavior source |
|---|---|---|
| `chatM` | **Registrar** (`Chat/ActionBar`, check-circle icon) is drawn in the footer above `Chat/Composer` | ADR-048 decision 1; [Chat](../../produto/specifications/chat.md) rules 6, 11 and 17; `chatE` of D20 for the layout |

- Code: `apps/android/app/src/main/java/app/fibrai/android/feature/chat/ChatScreen.kt` (`threadRows`, `AnswerActions`), fixture `ChatFixtures.chatM`.
- Latest app captures: the `GoldTest` render `build/outputs/gold/render/{theme}/chatM.png` (A61), and an emulator capture taken during discovery.

## Scope

1. **Discovery (read only):**
   - **Sweep:** every inventory gold that shows an answer action is compared against the app (A61) for an action still pinned above the composer. Known: `chatM`. To confirm: `chatT`, where the Registrar of `chatE` is behind the blurred sheet. Anything else of the same kind is added here; any other difference is listed for the owner and stays out.
   - **Per frame:** every element of `chatM` is classified `app`, `gold-only` or `copy`, as in the template.
   - Both tables go into Results before any write.
2. **Frame** (`chatM`, Light, then the Dark clone in mode Dark):
   - **Answer group:** as D20 did for `chatE`, the last thread element (the `Bot` group: bubble, memory chips, time) moves into an `Answer` frame. It is followed by an `Actions` frame (vertical, thread width) holding the `Chat/ActionBar` **Registrar** instance taken from the `Footer`.
   - **Gaps:** 12 dp from the time under the chips to Registrar (the D20 under-bubble gap), then the thread's bottom margin.
   - **Footer:** keeps only `Chat/Composer`, with the frame's existing bottom margin.
   - **Frame height:** follows the content (hug), so the whole thread shows.
   - **Dark text paints:** text ranges bound to colour variables in the Dark clone carry the Dark resolved value (the D20 placeholder-paint gotcha).
3. **Owner review** (Figma review gate): one screenshot per changed frame, made free with `node tools/export-figma.mjs --only chatM --dry-run`. The plan goes to `pending_manual_validation/` until the owner's OK in Figma; fixes follow with one screenshot each.
4. **Export** after the OK:
   - `node tools/export-figma.mjs --only chatM` (both themes); untouched golds keep their bytes;
   - `node tools/check-figma.mjs`;
   - `tools/diff-gold.mjs`: `chatM` keeps its long-thread rules (header box and tail); a rule that existed only for the pinned slot is removed;
   - Results list the exported files and the hand-over to the client follow-up.

## Out of scope

- Compose. The app already draws Registrar in the thread (A61). The client follow-up is written after this plan's OK. It removes the `GoldTest` region exception `CHAT_M_BOXES` / `CHAT_M_ACTION`, so `chatM` is gated whole, and adds an emulator capture of `chatM`.
- Behavior or copy changes. Other frames, unless the sweep finds the same stale pinned action. The Light `text/dim` contrast ([D21](d21-light-text-dim-contrast.md)).

## Validation

1. The sweep and discovery tables exist in Results before the first write.
2. Read-back (`use_figma`): the `Actions` stack is a child of the thread; the `Footer` holds only `Chat/Composer`; DS instances everywhere; no visible solid paint without a variable; Dark in mode Dark.
3. Visual: one screenshot per changed frame; manual owner OK.
4. After the export, the `GoldTest` render of `chatM` (A61 code) compared with the new gold: the blurred diff of the whole frame is ≤ 2 % in both themes. This is printed in Results; the gate itself moves in the client follow-up.
5. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

Planning only.
