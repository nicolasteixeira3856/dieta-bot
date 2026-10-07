# Plan — A62 `chatM` gated whole

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/app/src/test/` (`GoldTest`) and the capture `docs/qa/android/current/{dark,light}/chatM.png`. No production code, no server, no Room. `tools/capture-chat.sh` and `tools/diff-gold.mjs` only if the capture shows a rule that no longer fits.
- Related documentation: [ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md), [product Chat](../../produto/specifications/chat.md), [QA](../../qa/README.md).
- Prerequisites:
  - [D22](../../design/plans/completed/d22-chatm-action-in-thread.md) `Concluído`: the `chatM` gold draws Registrar in the thread (780 × 1854, 927 dp);
  - [A61](pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a62-chatm-gold-gate.md. Implemente o plano aprovado.`

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
- Other golds, and the Light `text/dim` contrast ([D21](../../design/plans/d21-light-text-dim-contrast.md)).
- A dev build: the APK does not change.

## Validation

1. `testDevDebugUnitTest` passes, with `GoldTest.chatM_*` gated (not report-only). Run it with `--rerun`, because the gold PNG is not a Gradle input.
2. `verifyRoborazziDevDebug` passes.
3. The `chatM` emulator captures exist in both themes, and `tools/diff-gold.mjs` passes on them.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
