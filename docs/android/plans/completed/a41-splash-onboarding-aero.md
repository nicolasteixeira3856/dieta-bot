# Plan — A41 Splash and onboarding on Aero

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - splash, `feature/onboarding/`, onboarding components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s` and `o4` in `docs/qa/README.md`, captures.
- Prerequisites: [A40](a40-home-aero.md) and [D4](../../../design/plans/completed/d4-release1-splash-onboarding.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a41-splash-onboarding-aero.md. Implemente o plano aprovado.`

## Objective

Move the splash and O1–O4, with their states, onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the eight golds of this flow (`tools/capture-onboarding.sh`). Flow-specific work:

1. **`AeroSegmented`** replaces the M3 `ButtonGroup` / `ExpressiveButtonGroup` in O1 and O2: same three eat-back modes, typed % default 50, no cap (AGENTS "Product").
2. **`AeroTimeWheelDialog`** replaces `TimeWheelDialog`'s Material look (`o3t`); the measured `o3t` regression (docs/qa § Regressão) is re-done against the Figma gold.
3. **Splash** keeps its timing (cold start ≤ 2 s, not a freeze) and the current mark.
4. **Spec:** [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) changes only its visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with `capture-onboarding.sh` and 16 images.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Splash** on Aero: page gradient, the current brand mark in its 120 dp box, the wordmark in Hero/Number, the 48 × 4 accent bar, the disclaimer (Body, 280 dp, centred) and the three edge bubbles. Timing unchanged (`SplashBoot.DELAY_MS`, capture mode stays up).
2. **O1–O4** rebuilt from the Figma frames in `feature/onboarding/OnboardingAeroScreens.kt` (same public signatures; O3 is `OnboardingSlotsScreen`): stepper / intake / brand top bars, Intro (eyebrow, Title, Body), labelled groups, the CTA (`AeroButtonPrimary` + arrow) fixed 32 dp above the bottom, 24 dp between blocks (o1e 20), edge bubbles moving with the scroll.
   - O1: `AeroSegmented` (Homem/Mulher), three compact `AeroNumberField`s, three `AeroOptionCard` modes, the Large meta field with the bolt, the sparkle caption; o1e disables modes, meta and CTA at 38 % and shows the profile hint. The split and per-day meta fields (no gold) use compact fields with a day caption.
   - O2: three `AeroOptionCard`s (badge `Padrão`), the typed-% compact field inside the partial card, the note card. Eat-back modes, typed % default 50, no cap: unchanged.
   - O3/o3s: `AeroTabs` for the weekday modes, group step and `Copiar de …` pill, `AeroSegmented` 2–6, `AeroMealSlotRow` per slot (band icons coffee / cookie / fork-knife / bowl-food / moon), the bell note.
   - o3t: `AeroTimeWheelDialog` (glass dialog, Hero/Title/Body wheel rows, selection band, Cancelar + OK) over the blurred O3 and `overlay/scrim`; same wheel logic as `TimeWheelDialog` (loop, snap, haptics, keys, accessibility). The `ButtonGroup`/`ExpressiveButtonGroup` is no longer used by the onboarding.
   - O4: brand bar with help, split card, three `AeroMacroTargetCard`s (adjust button focuses the grams field), note card with the `KCAL TOTAL ESTIMADA` footer.
3. **New Aero components:** `AeroStepper`, `AeroSegmented`, `AeroTabs`, `AeroNoteCard`, `AeroNumberField` (Large/Compact, String value), `AeroMealSlotRow`, `AeroMacroTargetCard`, `AeroTimeWheelDialog`, `AeroPageBubbles`, `AeroScrim`, `aeroAccentSheen`; Phosphor `sliders-horizontal` added (37 icons).
4. **`AeroText`** (`aeroLineBox`): Compose kept the font's natural height when a style's line height is smaller (Hero/Number 47 dp for 40, Field/Number 39 for 36, Title 30 for 28), pushing every block under a title down. `AeroText` clamps the box to lines × line height with the glyphs centred, as Figma does; every Aero screen and component uses it. The Home benefits too (JVM home golds 0.00–0.11 %); its `homeW` Roborazzi baseline was re-recorded with this plan for that reason.
5. **Tap catcher:** sheet and dialog bodies consumed taps with `clickable {}`, which merged their children's semantics (the wheels were unreachable by tag and TalkBack). `aeroSwallowTaps()` consumes taps without semantics; used by `AeroSheet` (homeW) and the time dialog. Found by the new `AeroTimeWheelDialogTest`.
6. **Config untouched:** the Config screens still use the legacy onboarding controls (`SlotsScreen`, `ModeGroup`, `KcalField`, `EatCard`, `PctField`, `MacroCard`, `TimeWheelDialog`), kept in `OnboardingScreens.kt` until A44.
7. **Gold sources:** `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4` are `figma` in the inventory; `StitchGoldTest` renders them at the frame heights (844, 979, 937, 844, 1268, 1270, 865).
8. **Tooling** (outside the plan's file list, needed for the gate): `tools/diff-gold.mjs` gates the Figma `o3t` dialog aligned on the capture centre (the frame draws it 300 dp down a 1268 dp page); `tools/capture-onboarding.sh` scrolls before tapping the second card's chip (the fixed CTA covers it at the top of the scroll and uiautomator skips covered nodes). The flow and its checks are otherwise unchanged.
9. **Docs:** [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) visual references and Provenance; `docs/qa/README.md` § Regressão for the Figma `o3t`.

### Figma MCP

5 read-only `use_figma` calls (budget 120): D4 components, splash + o1 frames with effect colours and icon tints, o1e + o2 + o4 frames, o3 + o3s + o3t frames with `Dialog/TimeWheel`. Values not read from the PNG.

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 502 tests, 0 failures (new: `AeroTimeWheelDialogTest`, the four `TimeWheelDialogTest` cases on the Aero dialog). `./gradlew.bat :app:verifyRoborazziDevDebug`: pass; baselines re-recorded: `splash`, `o1e`, `o3t` (this flow) and `homeW` (item 4).
2. JVM gold (`StitchGoldTest`, Figma frame geometry, blurred diff), 2 iterations:
   - iteration 1: 11 of 14 passed; o3s 2.53 % / 4.06 % and light o4 2.55 % failed, from the text line boxes (item 4);
   - iteration 2: splash 0.00 / 0.00 %, o1 0.00 / 0.00 %, o1e 0.00 / 0.00 %, o2 0.07 / 0.04 %, o3 0.01 / 0.00 %, o3s 0.04 / 0.00 %, o4 0.10 / 0.07 % (dark / light).
3. Emulator (API 36, 780 × 1688, density 320), `tools/capture-onboarding.sh dark|light` + `node tools/diff-gold.mjs`: dark splash 0.00 %, o1 1.34 %, o1e 0.85 %, o2 1.14 %, o3 1.08 %, o3t dialog 1.04 %, o3s 1.14 %, o4 1.26 %; light splash 0.00 %, o1 0.89 %, o1e 0.00 %, o2 0.78 %, o3 0.73 %, o3t dialog 0.84 %, o3s 0.74 %, o4 0.97 %. All 16 ≤ 2 %, ink 0.96–1.16. Every interaction check of the script passed (IME walk, relaunch lands on Home, o3t from the real time field).
   - Diff list (none blocks): content 23–24 dp lower (status bar; the frames have none); the o3t dialog is centred on the phone instead of 300 dp down the frame; the disabled CTA (38 %) shows the cards under it while scrolling, as the frame's layer opacity does; blur grain inside glass is not counted. Layout, tokens, type sizes, radii, segmented controls, CTA and semantic macro colours match.
4. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,069,363 → 19,102,723 bytes (+33,360).
5. `node tools/check-docs.mjs`: pass.

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](../pending_manual_validation/a46-input-cursor-keyboard.md).
