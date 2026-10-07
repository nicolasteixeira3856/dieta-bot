# Plan — D22 Chat: `chatM` with Registrar in the thread

- Status: Concluído (07/10/2026)
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat · D6", frames `chatM` Light (`72:3034`) and Dark (`72:3333`) only; no new component. Repository: `docs/qa/figma/{dark,light}/chatM.png`; `tools/export-figma.mjs` only if a node id changes.
- Prerequisites: D20 `Concluído` ([history](../completed/)); [A61](../../../android/plans/pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) merged (0.0.19-dev: the app the frame is compared with).
- Figma MCP budget: ≤ 12 calls (at most 120 a day, ADR-031 § 6).

One frame, two themes. It was found during A61: `chatM` shows **Registrar** (an `ask` estimate), but it was not in the D20 list. The gold still draws Registrar in the slot pinned above the composer, while the app has drawn it in the thread since A61 ([ADR-048](../../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md) decision 1). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d22-chatm-action-in-thread.md. Implemente o plano aprovado.`

## Objective

Bring `chatM` in line with ADR-048 in both themes. **Registrar** moves from the footer into the thread, right under its answer, as D20 drew it for `chatE`. The composer stays alone at the bottom. Nothing else in the frame changes.

## Sources (feature parity, ADR-031 § 4)

| Gold | What is stale | Behavior source |
|---|---|---|
| `chatM` | **Registrar** (`Chat/ActionBar`, check-circle icon) is drawn in the footer above `Chat/Composer` | ADR-048 decision 1; [Chat](../../../produto/specifications/chat.md) rules 6, 11 and 17; `chatE` of D20 for the layout |

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
- Behavior or copy changes. Other frames, unless the sweep finds the same stale pinned action. The Light `text/dim` contrast ([D21](../d21-light-text-dim-contrast.md)).

## Validation

1. The sweep and discovery tables exist in Results before the first write.
2. Read-back (`use_figma`): the `Actions` stack is a child of the thread; the `Footer` holds only `Chat/Composer`; DS instances everywhere; no visible solid paint without a variable; Dark in mode Dark.
3. Visual: one screenshot per changed frame; manual owner OK.
4. After the export, the `GoldTest` render of `chatM` (A61 code) compared with the new gold: the blurred diff of the whole frame is ≤ 2 % in both themes. This is printed in Results; the gate itself moves in the client follow-up.
5. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

**Sweep** (07/10/2026, before any write): the bottom of every Chat gold of the inventory (Light), read side by side with the A61 code (`threadRows`, `AnswerActions`) and its `GoldTest` fixture, plus a read-only `use_figma` of the frames whose action could be hidden.

| Gold | Figma | App (A61) | Class |
|---|---|---|---|
| `chatM` | `Chat/ActionBar` **Registrar** in the `Footer`, above `Chat/Composer` | Registrar in the thread, 12 dp under the time | known: stale gold (ADR-048 decision 1) |
| `chatT` | the Chat page under the sheet keeps **Registrar** in the `Footer` (`63:1929`), behind the sheet; the D20 move skipped it | `ChatFixtures.chatT` is `estimated` with `actions`: Registrar in the thread under the answer, behind the sheet | stale gold of the same kind: added to this plan (moved the same way; the change is under the blurred sheet) |
| `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`, `chatCP`, `chatCC` | actions inside `Thread` (D20) | same | no change |
| `chatTI` | `Footer` holds only `Chat/Composer` | same | no change |
| `chat0`, `chatL`, `chatP`, `chatX`, `chatF`, `chatG`, `chatA`, `chatS`, `chatU`, `chatD`, `chatSK`, `chatSD`, `chatI`, `chatIC` | no answer action above the composer (suggestion chips, attachment, receipt actions and inline buttons are in the thread or the composer) | same | no change |

No other difference was found.

**Discovery** (`chatM`, both themes; the copy is the `ChatFixtures.chatM` text):

| Element | Class | Source |
|---|---|---|
| `Chat/Header` (back, Tali, `ASSISTENTE DE REFEIÇÕES`) | app | `ChatScreen.kt` header |
| `Chip/Date` `Hoje, 25 de setembro` | app | date separator |
| User bubble `Café da manhã igual ao de sempre, mas hoje com pão integral`, `20:15` | app | `ChatItem.User` |
| `Chat/BotLabel`, bubble prose, `Chat/Estimate` (~430 kcal · 26g P · 36g C · 20g G), `Deseja registrar essa refeição no Café da manhã?` | app | `ChatItem.Assistant` + `EstimateView` |
| `Chip/Memory` Memória atualizada · Memória permanente · Memória dinâmica, time `20:15` | app | `MemoryNotice` (A29) |
| `Chat/ActionBar` **Registrar** | app (moves into the thread) | `AnswerActions`, ADR-048 decision 1 |
| `Chat/Composer` | app | footer |
| Page bubbles (ellipses) | app | page background |

No `gold-only` element and no copy change.

### Build (07/10/2026)

In each of the four frames (same node ids), the last thread element (the `Bot` group: label, bubble, memory chips, time) moved into a new `Answer` frame (vertical, 12 dp, fills the thread width). The `Answer` frame ends with an `Actions` frame (vertical, 10 dp, thread width) holding the `Chat/ActionBar` **Registrar** instance taken from the `Footer`. The `Footer` keeps only `Chat/Composer`. This is the D20 structure of `chatE`.

| Gold | Light | Dark | `Answer` / `Actions` | Height |
|---|---|---|---|---|
| `chatM` | `72:3034` | `72:3333` | `181:6161`/`181:6162`, `181:6163`/`181:6164` | 925 → 927 px (hug) |
| `chatT` (page under the sheet) | `63:1911` | `63:2176` | `181:6165`/`181:6166`, `181:6167`/`181:6168` | 844 (fixed; the `Spacer` absorbs the difference) |

Registrar sits 12 px under the time line in all four frames and keeps its 350 × 48 size. No node id in `tools/export-figma.mjs` changes.

### Validation (before the owner review)

1. Discovery tables above, written before the first write.
2. Read-back (in the write call):
   - every `Actions` stack is inside `Thread`, and every `Footer` holds only `Chat/Composer`;
   - instances: 22 in each `chatM` frame, 29 in each `chatT` frame;
   - zero visible solid paints without a variable or style;
   - Light frames are in mode Light (`2:0`) and Dark frames in mode Dark (`2:1`);
   - no text was created or edited, so the Dark placeholder-paint gotcha does not apply.
3. Review images from `node tools/export-figma.mjs --only chatM,chatT --dry-run` (no MCP calls):
   - `chatM` is 780 × 1854 in both themes. Registrar sits under the answer and the composer stays alone, as in the `GoldTest` render of the A61 code.
   - `chatT` shows no pixel above the noise threshold against the current gold in either theme: the change is hidden by the sheet. The export noise filter will keep its bytes; only the Figma structure changes.

Figma MCP budget: 5 of 12 calls (whoami, 2 skill reads, 1 read, 1 write).

### Owner review

**OK (07/10/2026).** The owner approved the frames in Figma ("Aprovado no Figma") on the first round, with no fixes.

### Export (07/10/2026)

- `node tools/export-figma.mjs --only chatM,chatT`:
  - `chatM` is 780 × 1850 → 780 × 1854 in both themes, with fresh bytes kept;
  - `chatT` changed 0.000 % of its pixels in both themes, so the noise filter restored its bytes from git. Only the Figma structure changed.
  - No node id changed in `tools/export-figma.mjs`.
- `node tools/check-figma.mjs`: 100 golds verified (50 dark + 50 light).
- `tools/diff-gold.mjs`: no change. `chatM` keeps its long-thread rules (`HEADER_BOX`, tail 0). It never had a rule for the pinned slot.
- Gold check (the A61 code against the new gold; `GoldTest`, run with `--rerun` because the gold PNG is not a Gradle input):

  | Measure | Dark | Light |
  |---|---|---|
  | Blurred diff of the whole frame | 0.44 % | 1.51 % |
  | Header and thread region | 0.04 % | 0.11 % |
  | Old action zone | 0.00 % | 0.00 % |

  `chatT` scores 0.00 % in both themes.
- **Hand-over:** [A62](../../../android/plans/a62-chatm-gold-gate.md) removes the `GoldTest` exception (`CHAT_M_BOXES`, `CHAT_M_ACTION`, `reportOnly`), moves the qualifier to 927 dp and adds the emulator capture of `chatM`.

Figma MCP budget: 5 of 12 calls in total.
