# Plan — A51 Logo-only splash

- Status: Concluído
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (`feature/splash/SplashScreen.kt`, `core/designsystem/FibraiTokens.kt` `SplashBoot`, splash Roborazzi baselines, splash tests) and the splash emulator capture in `docs/qa/android/current/{dark,light}/`.
- Related documentation: [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) rules 7 and 9, [android README § Marca](../../README.md#marca), the plan indexes.
- Prerequisites: [D13](../../../design/plans/completed/d13-oat-seed-logo.md) `Concluído` with the `splash` golds re-exported; no parallel Android plan touching `feature/splash/`.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approved on 06/10/2026 (`Aprovo o plano docs/android/plans/a51-logo-only-splash.md. Implemente o plano aprovado.`).

## Objective

Make the Compose splash match the D13 gold: only the logo, centered and large. Remove the wordmark, the divider and the disclaimer from the splash. Timing is unchanged (`SplashBoot.MAX_MS`, `DELAY_MS`; cold start <= 2 s).

## Scope

1. `SplashScreen`: keep the Aero background; draw `logo_mark` alone, centered, at the size the gold measures (D13 proposes 160 dp); drop the wordmark, divider and disclaimer composables. Keep `testTag("splash-logo")`.
2. `SplashBoot`: remove `COPY` and `WORDMARK` if nothing else uses them; replace `LOGO_BOX_DP` / `LOGO_DP` by the single measured size. The Home keeps its own disclaimer copy.
3. Tests: `GoldTest.splash_*` against the new golds within the approved threshold; re-record `src/test/snapshots/{dark,light}/splash.png` with `recordRoborazziDevDebug` limited to the splash tests; `verifyRoborazziDevDebug` green.
4. Emulator capture of the splash in both themes to `docs/qa/android/current/`, diff list against the golds per AGENTS § Visual QA.

## Intended documentation changes

At Completion: [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) rule 7 (disclaimer only on the Home) and rule 9 (the splash shows the logo, not the wordmark); [android README § Marca](../../README.md#marca) splash line; the plan indexes.

## Out of scope

Onboarding screens, Home, the system splash (Android 12+ keeps the launcher foreground), the icon background color, the launcher layers and favicon already regenerated with the brand sources.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` green.
2. Splash gold diff recorded for both themes; emulator captures in place; diff list written.
3. SDD git delivery.

## Results

Implemented on 06/10/2026, same delivery as D13 and the brand assets (PR #139).

### Code

- `SplashScreen`: Aero page, the three bubbles and one `Image` of `logo_mark` centered at `SplashBoot.LOGO_DP` (160 dp) with `testTag("splash-logo")` and the content description `Fibrai`. Wordmark, accent bar, disclaimer and the `splash-copy` tag removed. Timing unchanged.
- `SplashBoot`: `WORDMARK`, `LOGO_BOX_DP` and the 60 dp `LOGO_DP` removed; `LOGO_DP = 160`; `LOGO_DESCRIPTION = "Fibrai"`; `COPY` kept for the Home disclaimer. `TokensTest` updated.

### Visual QA

| Theme | JVM `GoldTest` (blurred) | Emulator `diff-gold` | Content offset |
|---|---|---|---|
| dark | 0.00 % | 0.08 % (ink 1.03) | 1 dp |
| light | 0.00 % | 0.06 % (ink 1.00) | 0 dp |

Emulator: `Aero_API30` at 780 × 1688 / 320 dpi, devDebug APK, `am start -e fibrai_tela splash`, `screencap` to `docs/qa/android/current/{dark,light}/splash.png`.

Diff list against `docs/qa/figma/{dark,light}/splash.png`: layout, the seed is 91 × 160 dp centered on both axes in both; tokens, page gradient and bubble fills unchanged; type, none on the frame; radius, none; ButtonGroup and CTA, none; timeline, none; macros, none. Remaining differences are the status and navigation bars and the emulator font raster (ignored by rule). No logo size or offset diff.

### Checks

`:app:testDevDebugUnitTest` and `:app:verifyRoborazziDevDebug` green after re-recording `src/test/snapshots/{dark,light}/splash.png` (see the PR for the final run). `node tools/check-docs.mjs` passes.
