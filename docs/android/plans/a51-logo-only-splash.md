# Plan — A51 Logo-only splash

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (`feature/splash/SplashScreen.kt`, `core/designsystem/FibraiTokens.kt` `SplashBoot`, splash Roborazzi baselines, splash tests) and the splash emulator capture in `docs/qa/android/current/{dark,light}/`.
- Related documentation: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) rules 7 and 9, [android README § Marca](../README.md#marca), the plan indexes.
- Prerequisites: [D13](../../design/plans/d13-oat-seed-logo.md) `Concluído` with the `splash` golds re-exported; no parallel Android plan touching `feature/splash/`.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a51-logo-only-splash.md. Implemente o plano aprovado.`

## Objective

Make the Compose splash match the D13 gold: only the logo, centered and large. Remove the wordmark, the divider and the disclaimer from the splash. Timing is unchanged (`SplashBoot.MAX_MS`, `DELAY_MS`; cold start <= 2 s).

## Scope

1. `SplashScreen`: keep the Aero background; draw `logo_mark` alone, centered, at the size the gold measures (D13 proposes 160 dp); drop the wordmark, divider and disclaimer composables. Keep `testTag("splash-logo")`.
2. `SplashBoot`: remove `COPY` and `WORDMARK` if nothing else uses them; replace `LOGO_BOX_DP` / `LOGO_DP` by the single measured size. The Home keeps its own disclaimer copy.
3. Tests: `GoldTest.splash_*` against the new golds within the approved threshold; re-record `src/test/snapshots/{dark,light}/splash.png` with `recordRoborazziDevDebug` limited to the splash tests; `verifyRoborazziDevDebug` green.
4. Emulator capture of the splash in both themes to `docs/qa/android/current/`, diff list against the golds per AGENTS § Visual QA.

## Intended documentation changes

At Completion: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) rule 7 (disclaimer only on the Home) and rule 9 (the splash shows the logo, not the wordmark); [android README § Marca](../README.md#marca) splash line; the plan indexes.

## Out of scope

Onboarding screens, Home, the system splash (Android 12+ keeps the launcher foreground), the icon background color, the launcher layers and favicon already regenerated with the brand sources.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` green.
2. Splash gold diff recorded for both themes; emulator captures in place; diff list written.
3. SDD git delivery.

## Results

Planning only.
