# Plan — A39 Aero design system foundation in Compose

- Status: Pendente aprovação manual
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `app/src/main/java/com/nutri/android/core/designsystem/aero/` (new);
  - `app/src/main/res/font/` (Nunito Sans) and `app/src/main/res/drawable/ph_*.xml` (Phosphor);
  - `third_party/` licenses;
  - the Gradle token task, the gold test source resolution and new Roborazzi component tests.
- Prerequisites: [ADR-030](../../../design/adrs/ADR-030-own-design-system-aero.md) and [ADR-031](../../../design/adrs/ADR-031-figma-source-of-truth.md) accepted; [D1](../../../design/plans/completed/d1-figma-file-foundation.md) and [D2](../../../design/plans/completed/d2-figma-tooling-stitch-deprecation.md) `Concluído` (`docs/design/tokens.json` exists).

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

   **New dependency, named for approval:** [Haze](https://github.com/chrisbanes/haze) (Apache-2.0), at the latest stable version compatible with the current Compose BOM, for the backdrop blur. Approved by the owner on 2026-10-04.
6. **Components** (Compose versions of the D1 components, same names):
   - `AeroButtonPrimary`, `AeroIconButton`, `AeroChipLog`, `AeroProgressBar`;
   - `AeroMacroRow`, `AeroTimelineNode`, `AeroMealCard`, `AeroOptionCard`;
   - `AeroChatBubble`, `AeroComposer`.

   Each takes state and lambdas only (stable params, `compose-stability` skill). Flow-specific components come with their flow plan.
7. **Gold test source:**
   - `StitchGoldTest` reads each id's source (`stitch`/`figma`) from the inventory table in `docs/qa/README.md` and loads the gold from the matching folder;
   - behavior is unchanged while every id is `stitch`.
8. **AGENTS.md** (Completion): add to Live stack the line "Aero design system layer (ADR-030): `core/designsystem/aero`, flows migrate per `docs/design/plans/README.md`". Material lines stay until [A45](../a45-remove-material3.md).

## Out of scope

- Any screen change (A40–A44). Removing Material (A45). iOS.

## Validation

1. `./gradlew.bat :app:testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass. New Roborazzi baselines: every Aero component in Light and Dark.
2. Visual: the component baselines are compared by eye with the Figma `Componentes` screenshots (same tokens); differences are listed in Results.
3. Blur check on an API 31+ emulator and on an API 26–30 emulator (fallback), screenshot of `AeroGlass` over a gradient in both. The blur on the owner's phone is part of the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3).
4. `:app:assembleDevRelease` succeeds; the APK size delta is recorded.
5. `node tools/check-docs.mjs` passes.

## Results

Implemented 2026-10-04 after the owner's named approval. Waits in `pending_manual_validation/` only for the consolidated phone check of A39–A45 ([A40 § Validation](a40-home-aero.md#validation) step 3: glass blur on the owner's phone).

### Delivered

1. **Tokens.** Gradle task `generateAeroTokens` (`app/build.gradle.kts`) reads `docs/design/tokens.json` and writes `build/generated/aero/kotlin/.../AeroTokens.kt`, wired as a generated source of every variant: `AeroColors` + `AeroLightColors`/`AeroDarkColors` (25 colors), `AeroDimens` (11), `AeroMotion` (3 durations, 3 easings), `AeroTextTokens` (9), `AeroPaints` (page, sheen and gloss stops), `AeroEffects` (glass blur and shadow, accent glow). A missing file, invalid JSON, unknown variable, bad color or unsupported effect fails the build. No token value is written by hand in Kotlin.
   - `docs/design/tokens.json` gained the Figma paint styles (`Background/Page`, `Surface/Glass`, `Gloss/Button`), the effect styles (`Glass`, `Glow/Accent`) and the `textCase` of `Label/Section` (`UPPER`), read through the MCP: the gloss stops, the glass shadow and the text case were missing from the mirror. `node tools/gen-tokens.mjs --check` still passes (the Aero table renders variables and text styles only).
2. **Theme.** `AeroTheme { }` provides `LocalAeroColors` (by `isSystemInDarkTheme()`), `LocalAeroType`, `LocalAeroShapes` (card 20, sheet 28 top, pill) and `LocalAeroHaze`; access through `Aero.colors/type/shapes/motion`. `DietaBotTheme` and `MaterialExpressiveTheme` are untouched.
3. **Typography.** Nunito Sans variable fonts (roman and italic, google/fonts `ofl/nunitosans`; axis defaults wdth 100, opsz 12, YTLC 500, the values Figma uses) in `res/font`, license `third_party/nunito-sans/OFL.txt`. `AeroType` mirrors the 9 Figma text styles; `heroNumber`, `fieldNumber` and `captionStrong` set `fontFeatureSettings = "tnum"`.
   - tnum check (`AeroComponentsTest.numberStylesAreTabular`): "1111" = "0000" in every number style (Hero 164 px = 82 dp, the D1 Figma value; Field 136 px; Caption/Strong 64 px). The font has no `tnum` GSUB feature: its default figures are already tabular, so no fallback is needed.
4. **Icons.** 36 Phosphor icons (the set used in Figma minus the unused `microphone` and `sliders-horizontal`), converted from `@phosphor-icons/core` 2.1.1 by a one-off script in the scratchpad into `res/drawable/ph_<name>_<weight>.xml` (viewport 256, 24 dp); duotone keeps `fillAlpha 0.2`. `AeroIcon(name, tint)` tints with `SrcIn`, so the 20 % layer stays 20 %. License `third_party/phosphor/LICENSE` (MIT).
5. **Surfaces.** `Modifier.aeroPage()` (page gradient and Haze source; `AeroPage { }` puts it on a layer behind the content), `Modifier.aeroGlass(shape)` (API 31+: Haze backdrop blur 16 dp + glass tint; API 26–30: no blur, glass fill at 85 % light / 88 % dark; then the top sheen, the hairline border and the `Glass` shadow, drawn outside the shape only as Figma does under a translucent fill), `Modifier.aeroGloss(shape)`, `Modifier.aeroShadow`.
   - **Haze version:** 1.7.2. Haze 2.0.1 needs Kotlin stdlib 2.4.20, and Haze 1.7.3 ships Kotlin 2.3 metadata, which the Hilt 2.56.2 kapt processor rejects ("maximum supported version is 2.2.0"). 1.7.2 (Kotlin 2.2.21, Compose UI 1.12) is the latest stable that builds with the current toolchain (Kotlin 2.2.20, BOM 2026.09.00 = Compose UI 1.12.1).
6. **Components** (`core/designsystem/aero/AeroComponents.kt`, state and lambdas only): `AeroButtonPrimary`, `AeroIconButton`, `AeroChipLog` (Accent/Neutral/Bad), `AeroProgressBar`, `AeroMacroRow`, `AeroTimelineNode` (Done/Photo/Skipped/Active/Over/Empty), `AeroMealCard` (Logged/Over/Skipped/Pending/Empty), `AeroOptionCard`, `AeroChatBubble`, `AeroComposer` (Default/TooLong/Attached). Sizes, paddings and icon tints come from the Figma component properties; Figma strokes are inside and counted in the layout, so paddings include the border.
7. **Gold test source.** `GoldInventory` (test) parses `docs/qa/README.md` § Golds like `parseGoldInventory`; `StitchGoldTest` loads `docs/qa/<source>/{theme}/<id>.png`. Every id is still `stitch`, so behavior is unchanged (`GoldInventoryTest`).
8. **AGENTS.md:** Live stack line for the Aero layer.
9. **Validation harness** (outside the plan's file list, debug build type only, never in a release APK): `app/src/debug/.../AeroGlassCheckActivity`, glass over the page gradient and colored shapes, for the emulator blur check.

### Figma MCP

7 calls (budget 120): 1 skill read (`figma-use`) and 6 read-only `use_figma` (styles and component tree, component list, A39 component details, icon tints, macro/button/stroke details, text style case). Component previews for the visual comparison came from `node tools/export-figma.mjs --only <node ids> --dry-run` (REST, no MCP call).

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 498 tests, 0 failures. One `verifyRoborazziDevDebug` run had 2 failures in the `ChatViewModelTest`/`ConfigViewModelTest` teardown ("Dispatchers.Main is used concurrently with setting it") while two emulators loaded the machine; the rerun passed. No UI test failed.
2. `./gradlew.bat :app:verifyRoborazziDevDebug`: pass. New baselines only in `src/test/snapshots/{dark,light}/aero/` (ButtonPrimary, IconButton, ChipLog, ProgressBar, MacroRow, TimelineNode, MealCard, OptionCard, ChatBubble, Composer, Glass, GlassApi30); no existing baseline changed.
3. Visual, baselines × Figma `Componentes` (REST previews at 2x). Diff list:
   - fixed in the loop: `Label/Section` is upper case (badge "PADRÃO") and the Option/Card title fills its row so the badge sits at the right; the `Over` meal card chip has no dot; the Skipped card's 60 % layer no longer cuts its shadow;
   - remaining, not defects: the sample cards are 350 dp wide in the baselines and 310 dp in Figma, so the meal description wraps one word later; the Figma REST previews draw the shadow on a transparent canvas (a dark halo there, the 15 % `shadow/glass` over the gradient in the app); the JVM render of the blur over a smooth gradient looks like the plain tint;
   - type, radii, strokes, colors and icon tints match the bound variables.
4. Blur on emulators (`AeroGlassCheckActivity`, both themes): API 36 (`Medium_Phone`) shows the circles and the green band blurred behind every glass surface ("blur on"); API 30 (`Aero_API30`, system image `android-30;default;x86_64`, installed for this check) shows no blur and the opaque fallback fill ("blur fallback"), text readable in both themes. The blur on the owner's phone stays in the consolidated phone check.
5. `./gradlew.bat :app:assembleDevRelease`: success. `app-dev-release.apk` 18,345,872 → 19,069,363 bytes (+723,491: Nunito Sans roman and italic, Haze, 36 vector icons).
6. `node tools/check-docs.mjs`: pass.

### Pending (manual)

- Consolidated phone check, [A40 § Validation](a40-home-aero.md#validation) step 3.
