# Plan — A63 Light `textDim` in the app

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (one line in `ConfigScreen.kt`, `TokensTest`, the Roborazzi baselines in `app/src/test/snapshots/light/`), the Light captures in `docs/qa/android/current/light/`, and the run classes of `tools/contrast-gold.mjs`. No server, no Room, no token value (the values are D21's).
- Related documentation: [D21](../../design/plans/completed/d21-light-text-dim-contrast.md), [tokens](../../tokens.md), [QA](../../qa/README.md).
- Prerequisites:
  - [D21](../../design/plans/completed/d21-light-text-dim-contrast.md) `Concluído`: Light `text/dim` `#435463` in Figma, `docs/design/tokens.json` and the Light golds;
  - [A61](pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a63-light-text-dim-client.md. Implemente o plano aprovado.`

## Objective

The app already draws Light `textDim` as `#435463`: `generateAeroTokens` writes `AeroColors.textDim` from `docs/design/tokens.json` on every build. This plan proves it on the device, makes the tests hold the contrast rule D21 measured, fixes the one Light text the sweep found in the wrong token, and refreshes the evidence. Every Light `textDim` text in the touched flows must reach ≥ 4.5:1 on the pixels behind it.

## Findings (07/10/2026, before any change)

Measured with `tools/contrast-gold.mjs` on `master` after D21:

| What | Result |
|---|---|
| Light golds, `#435463` | 249 text runs in 35 PNGs; worst 4.75 (`homeP`); 0 under 4.5 |
| Light app captures, still the old build (`--hex #546a7d`) | 148 text runs; worst 3.32 (`homeX`); 21 under 4.5 |
| Same captures scored as `#435463` (`--hex #546a7d --as #435463`) | worst 4.62 (`homeX`); 0 under 4.5 |
| `verifyRoborazziDevDebug` with `#435463` built in | passes (exit 0). The comparison tolerance absorbs the change: the Light baselines still show `#546a7d` without failing, so a later move back to the old value would not fail either |
| `TokensTest.wcagContrastOnPage` | passes; it checks `bg/page` only |

Use of `textDim` against the token each gold binds (the 29 `textDim` lines in `src/main` against the 249 gold runs):

- **`cfgS`, the mode label `Seg–Sex · Sáb–Dom`:** the gold paints it in `text/dim` (`#435463`, 4.95 on the gradient). The app paints the `Block` trailing text in `textMuted` (`ConfigScreen.kt:193`, `#486781`), which measures 3.77 there. This is the only mismatch found. It is fixed here, following the gold.
- The `CaptionedKcal` captions (`Dias úteis`, `Fim de semana`; `ConfigScreen.kt:342`, `OnboardingAeroScreens.kt:374`) are in `textDim`, but no inventory gold draws them. No change.
- The rest match: the O1 placeholders (`o1e`), the O3 time placeholders (`o3`), the bubble times, `MICRO & MACRONUTRIENTES` (`chatL`, `ChatScreen.kt:780`), and `{n} Refeições` (`HomePanelScreen.kt:261`).

Tool: on the Dark captures, `contrast-gold.mjs` classed the skip icon's minus (an ink box of 20 × 2 px, `chatSK`) as text. Run boxes ≤ 4 px in one dimension are lines, not glyphs.

## Scope

1. **`ConfigScreen.kt`:** in `Block`, the trailing text uses `c.textDim` instead of `c.textMuted`, as the `cfgS` gold does. It stays Caption, one line. Nothing else in Config changes.
2. **`TokensTest`:** a new test `textDimContrastOnRealBackgrounds`. Composited colours are computed as `fg.compositeOver(bg)`. It asserts `textDim` ≥ 4.5 on:
   - both themes: `bgPage`, `surface2`, `bgMid`;
   - Light: `surfaceTint` and `surfaceGlass` composited over `bgTop`, the worst bubble backgrounds D21 measured (5.06 and 5.93).

   Dark is not asserted over `bgTop`: it measures 4.35 there (see Out of scope).
3. **Roborazzi:** `recordRoborazziDevDebug`, keeping only the Light baselines whose pixels moved, which are the ones that draw `textDim`. Dark baselines and unchanged files go back to their committed bytes. Then `verifyRoborazziDevDebug`.
4. **`tools/contrast-gold.mjs`:** a `line` class for run boxes ≤ 4 px wide or tall. It is reported but not gated, like `marker`.
5. **Emulator** (partial validation: the Light flows that show `textDim`; Dark does not change):
   - AVD at the gold geometry (`adb shell wm size 780x1688 && adb shell wm density 320`), devDebug APK;
   - `tools/capture-home.sh light` (`home0`, `home1`, `homeX`, `homeW`);
   - `tools/capture-config.sh light` (`cfg`, `cfgS`, `wipe`, `cfgR`);
   - `tools/capture-onboarding.sh light` (`o1e`, `o3`, `o3s` among its captures);
   - `node tools/fake-chat-server.mjs`, the APK with `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`, then `SCENES=v2 tools/capture-chat.sh light` (`chatS`, `chatR`, `chatM`) and `SCENES=a30 tools/capture-chat.sh light` (`chatQ`, `chatE`);
   - the captures of those runs replace the old ones in `docs/qa/android/current/light/`; captures of other scenes go back to their committed bytes;
   - `node tools/diff-gold.mjs` on the recaptured ids, with the existing gate and long-thread rules;
   - a diff list per flow (layout, tokens, type size, CTA, semantic macros), as in the QA loop.

## Out of scope

- **Dark `text/dim` near the top of the Chat (finding).** The Dark app captures put the user-bubble time at 4.43–4.49 when the bubble sits right under the header (`chatR`, `chatM`, `chatCP`, `chatG`, `chatRL`, `chatF`). The app's gradient spans the 844 dp screen, while the golds are taller frames, so they never show it (Dark gold worst 4.67). `surfaceTint` over Dark `bgTop` is 4.35. A token value is a design decision: a later design plan, like D21, would set it. A first sizing gives Dark `#87a3bb` (4.64–4.71 on those backgrounds, still dimmer than `text/muted`). Until then the `TokensTest` Dark assertions stop at `bgMid`.
- Light `text/muted` on the gradient ([D21 finding](../../design/plans/completed/d21-light-text-dim-contrast.md)).
- Any other screen, copy or layout change.

## Validation

1. `testDevDebugUnitTest --rerun` passes, with `TokensTest.textDimContrastOnRealBackgrounds` and `GoldTest` (the golds are not Gradle inputs, hence `--rerun`).
2. `verifyRoborazziDevDebug` passes after the re-record. `git diff --stat apps/android/app/src/test/snapshots/` lists only Light files.
3. `node tools/contrast-gold.mjs --theme light --dir docs/qa/android/current/light --ids <the recaptured ids>`: every text run ≥ 4.5, `cfgS` included. With `--hex #546a7d`, 0 text runs left in those ids.
4. `node tools/diff-gold.mjs` passes on every recaptured id that has a gate. Report-only screens are listed as such.
5. `node tools/check-docs.mjs` passes.
6. Test build only if the owner asks for one ([A16](completed/a16-app-distribution.md), `tools/distribute-dev.ps1`, notes under Ajustes).

## Results

<Filled at Completion: test results, re-recorded baselines, captures and contrast table, diff list.>
