# Plan — A39 Aero design system foundation in Compose

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `app/src/main/java/com/nutri/android/core/designsystem/aero/` (new);
  - `app/src/main/res/font/` (Nunito Sans) and `app/src/main/res/drawable/ph_*.xml` (Phosphor);
  - `third_party/` licenses;
  - the Gradle token task, the gold test source resolution and new Roborazzi component tests.
- Prerequisites: [ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md) and [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) accepted; [D1](../../design/plans/pending_manual_validation/d1-figma-file-foundation.md) and [D2](../../design/plans/d2-figma-tooling-stitch-deprecation.md) `Concluído` (`docs/design/tokens.json` exists).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a39-aero-foundation.md. Implemente o plano aprovado.`

## Objective

Build the Aero layer next to the current Material 3 Expressive theme, without changing any screen. Each flow plan (A40–A44) then moves its screens onto it.

## Scope

1. **Tokens:**
   - a Gradle task reads `docs/design/tokens.json` and generates `AeroTokens.kt` into `build/generated` (colors per mode, shape, spacing, sheen and gradient stops, motion);
   - no hand-written token values in Kotlin;
   - the build fails when the JSON is missing or invalid.
2. **Theme:**
   - `AeroTheme { }` provides CompositionLocals (`AeroColors` resolved for `isSystemInDarkTheme()`, `AeroType`, `AeroShapes`, `AeroMotion`);
   - no dynamic color;
   - `DietaBotTheme` and `MaterialExpressiveTheme` stay untouched for the screens not yet migrated.
3. **Typography:**
   - Nunito Sans TTFs (OFL) in `res/font`, with `third_party/nunito-sans/OFL.txt`;
   - `AeroType` mirrors the Figma text styles;
   - number styles set `fontFeatureSettings = "tnum"`;
   - a JVM test renders "1111" and "0000" and asserts equal width; if it fails, apply the fallback recorded by D1 and write it in Results.
4. **Icons:**
   - the Phosphor SVGs used in Figma become VectorDrawables (`ph_<name>_<weight>.xml`), produced from the official `@phosphor-icons/core` package by a one-off script in the agent scratchpad;
   - `third_party/phosphor/LICENSE` (MIT);
   - duotone keeps its 20 % layer;
   - `AeroIcon(name, tint)`.
5. **Surfaces and effects:**
   - `Modifier.aeroPage()`: the page gradient from the bound stops;
   - `Modifier.aeroGlass(shape)`:
     - API 31+: backdrop blur 16 dp + glass fill + top sheen + hairline border + shadow;
     - API 26–30: no blur, glass alpha raised (light white 85 %, dark `#0B2747` 88 %);
   - `Modifier.aeroGloss()`: the button gloss overlay.

   **New dependency, named for approval:** [Haze](https://github.com/chrisbanes/haze) (Apache-2.0), at the latest stable version compatible with the current Compose BOM, for the backdrop blur. If Haze is not approved, A39 ships glass without blur on every API, and that is recorded in Results.
6. **Components** (Compose versions of the D1 components, same names):
   - `AeroButtonPrimary`, `AeroIconButton`, `AeroChipLog`, `AeroProgressBar`;
   - `AeroMacroRow`, `AeroTimelineNode`, `AeroMealCard`, `AeroOptionCard`;
   - `AeroChatBubble`, `AeroComposer`.

   Each takes state and lambdas only (stable params, `compose-stability` skill). Flow-specific components come with their flow plan.
7. **Gold test source:**
   - `StitchGoldTest` reads each id's source (`stitch`/`figma`) from the inventory table in `docs/qa/README.md` and loads the gold from the matching folder;
   - behavior is unchanged while every id is `stitch`.
8. **AGENTS.md** (Completion): add to Live stack the line "Aero design system layer (ADR-030): `core/designsystem/aero`, flows migrate per `docs/design/plans/README.md`". Material lines stay until [A45](a45-remove-material3.md).

## Out of scope

- Any screen change (A40–A44). Removing Material (A45). iOS.

## Validation

1. `./gradlew.bat :app:testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass. New Roborazzi baselines: every Aero component in Light and Dark.
2. Visual: the component baselines are compared by eye with the Figma `Componentes` screenshots (same tokens); differences are listed in Results.
3. Blur check on a device with API 31+ and on an API 26–30 emulator (fallback), screenshot of `AeroGlass` over a gradient in both.
4. `:app:assembleDevRelease` succeeds; the APK size delta is recorded.
5. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
