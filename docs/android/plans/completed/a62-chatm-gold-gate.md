# Plan — A62 `chatM` gated whole

- Status: Concluído (aprovado e implementado 07/10/2026)
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/app/src/test/` (`GoldTest`) and the capture `docs/qa/android/current/{dark,light}/chatM.png`. No production code, no server, no Room. `tools/capture-chat.sh` and `tools/diff-gold.mjs` only if the capture shows a rule that no longer fits.
- Related documentation: [ADR-048](../../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md), [product Chat](../../../produto/specifications/chat.md), [QA](../../../qa/README.md).
- Prerequisites:
  - [D22](../../../design/plans/completed/d22-chatm-action-in-thread.md) `Concluído`: the `chatM` gold draws Registrar in the thread (780 × 1854, 927 dp);
  - [A61](../completed/a61-chat-copy-scroll-capture-inline-actions.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a62-chatm-gold-gate.md. Implemente o plano aprovado.`

## Objective

Since A61, `chatM` was checked only on the header and the thread, and its action zone was only reported. The gold was stale: it drew Registrar above the composer. D22 redrew it with Registrar in the thread. This plan removes that exception, so `chatM` is gated as a whole frame like the other Chat golds, and refreshes its emulator capture.

## Scope

1. **`GoldTest`:**
   - `chatM_dark` and `chatM_light` drop `reportOnly`, `CHAT_M_BOXES` and `CHAT_M_ACTION`, and the comment about D20;
   - the qualifier follows the new frame height, `h925dp` → `h927dp`;
   - the gate is the default one: a blurred diff of the whole frame ≤ 2 %. After the D22 export the render measured Dark 0.44 % and Light 1.51 %.
2. **Emulator** (partial validation, `chatM` only, both themes):
   - capture through the existing scene: `node tools/fake-chat-server.mjs`, the devDebug APK with `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`, then `SCENES=v2 tools/capture-chat.sh dark|light`;
   - `node tools/diff-gold.mjs` keeps the long-thread rules of `chatM` (header box and tail);
   - write a diff list with the remaining differences.

## Out of scope

- Any Compose change: A61 already draws the frame.
- Other golds, and the Light `text/dim` contrast ([D21](../../../design/plans/d21-light-text-dim-contrast.md)).
- A dev build: the APK does not change.

## Validation

1. `testDevDebugUnitTest` passes, with `GoldTest.chatM_*` gated (not report-only). Run it with `--rerun`, because the gold PNG is not a Gradle input.
2. `verifyRoborazziDevDebug` passes.
3. The `chatM` emulator captures exist in both themes, and `tools/diff-gold.mjs` passes on them.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented on 07/10/2026.

**`GoldTest`:**
- `chatM_dark` and `chatM_light` now run the default check, so the whole frame is gated: `reportOnly`, `CHAT_M_BOXES`, `CHAT_M_ACTION` and their comment are gone;
- the qualifier is `w390dp-h927dp-xhdpi`, the height of the D22 frame.

**Validation:**
1. `testDevDebugUnitTest --rerun` passes (exit 0). The `chatM` render against the D22 gold measured a blurred diff of 0.44 % (Dark) and 1.51 % (Light) when D22 was exported (≤ 2 %).
2. `verifyRoborazziDevDebug` passes on the second run. On the first run, `ChatCopyTest.longPressSelects_tapsToggle_andTheLastRemovalEnds` (A61) timed out once.
   - It is a race in the A61 production code, outside this plan's boundary: `ChatViewModel.toggleSelected` builds the next selection from the rendered `uiState`, not from the local state, so two taps in a row before a render can lose the first one.
   - It was handed to its own task for a separate plan.
3. Emulator (Medium_Phone API 36, 780 × 1688, density 320; devDebug APK against `tools/fake-chat-server.mjs`):
   - `SCENES=v2 tools/capture-chat.sh dark|light` passes every check in both themes, including `chips in order`;
   - only `chatM.png` is kept from the run; the other captures of the scene went back to their committed bytes;
   - `node tools/diff-gold.mjs dark/chatM light/chatM`:

     | Gate | Dark | Light |
     |---|---|---|
     | Header region | 0.00 % | 0.00 % |
     | Thread tail down to the composer | 0.02 % | 0.23 % |

     The whole screen is reported only, as the long-thread rule says (13.01 % Dark, 32.41 % Light). The phone shows 844 dp of a 927 dp frame, so the thread is cropped at the top.
   - Diff list against the gold:
     - layout: Registrar sits in the thread under the time, with the composer alone at the bottom, as in the gold;
     - tokens, type size, radius, CTA and semantic macro colours: same;
     - ignored: the status bar, the gesture bar, the clock (the seeded time is the capture time, so `20:13`/`20:07` instead of `20:15`) and the top of the long thread that the phone does not show.
4. `node tools/check-docs.mjs` passes.

No production code changed, and no dev build was needed.
