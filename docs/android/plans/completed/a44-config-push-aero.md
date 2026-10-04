# Plan — A44 Config and push on Aero

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - the Config screens, the wipe dialog, the push notification builder (small icon, accent color, actions), tests;
  - docs updated with the delivery: the source of `cfg`, `cfgS`, `wipe` and `push` in `docs/qa/README.md`, captures.
- Prerequisites: [A43](a43-chat-records-memory-aero.md) and [D7](../../../design/plans/completed/d7-release1-config-push.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a44-config-push-aero.md. Implemente o plano aprovado.`

## Objective

Move Config, the wipe dialog and the meal reminder onto Aero, matching the Figma golds, with no behavior change. After this plan every gold has source `figma`.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the four golds of this flow (`tools/capture-config.sh`, `tools/capture-push.sh`). Flow-specific work:

1. **Push:** only the fields the app controls change (small icon, accent color, text and actions). `push` stays a lock-screen gold measured and reported, not gated ([docs/qa/README.md](../../../qa/README.md) § Gate), unless D7 makes it comparable.
2. **Dev tools:** `Memória da IA (dev)` may adopt Aero components, but it has no gold ([ADR-019](../../../produto/adrs/ADR-019-ferramentas-dev.md)) and keeps `debug.nutri.hide_dev_tools=1` for the cfg capture.
3. **Spec:** [memoria-push](../../../produto/specifications/memoria-push.md) changes only its visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with 8 images.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Config on Aero** (`ConfigScreen.kt`, route wrapped in `AeroTheme`): page gradient and edge bubbles, `Header/Page` (glass back button with caret-left, `Title`, 12 dp apart), blocks 24 dp apart with `Label/Section` in text/muted and the glass group 12 dp below (the mode Caption on the right in `cfgS`), `Row/Setting` (Body title, Caption detail, value in Body text/muted or Body/Strong accent, muted caret-right, 16 dp padding, 8 dp gaps) inside the group's 1 dp border with border/line dividers, and `Card/Note`.
2. **Edit sheets** (no gold): `Sheet/Bottom` over the blurred list and overlay/scrim, with the subtitle and the editor scrolled past half the screen: ceiling modes as `Card/Option` + `Field/Number`, eat-back as `Card/Option` with the percent field, macros as `Card/MacroTarget`, workout as the homeW `Field/Number` with the credit line. Tags and behaviour unchanged.
3. **Meal editor:** the Config reuses the Aero O3 screen (`OnboardingSlotsScreen` gained a header bar, CTA label, CTA tag and tag prefix) with `Header/Page` and no eyebrow; same step-by-step, copy and discard confirmation.
4. **`wipe`:** `Dialog/Confirm` Tone=Danger (`AeroConfirmDialog` gained `body` and `dangerIcon`): 48 dp status/bad-tint badge with arrow-counter-clockwise, Title, Body in text/muted, 56 dp status/bad primary with the icon and the label in accent/on, the secondary pill. The dev memory tool (ADR-019) uses the same dialog; its route is wrapped in `AeroTheme`, the tool itself keeps its look.
5. **Push:** small icon = Phosphor fork-knife (monochrome), accent = `accent/default` of the system theme (`setColor`). Copy, actions and scheduling unchanged.
6. **Legacy removed:** `OnboardingScreens.kt` (the Config's legacy controls and `SlotsScreen`), `OnboardingChrome.kt`, `SlotScheduleControls()` and the legacy `WorkoutField`; only `SlotModeConfirmation` stays (Material dialog, A45). `Card/Option`, `Field/Number` (compact) and `Card/MacroTarget` take `onGlass` (the glass fill alone on a sheet).
7. **Gold sources:** `cfg`, `cfgS`, `wipe`, `push` are `figma`; every gold of the inventory is now `figma`. `push` is a Figma conflict (the frame draws a lock screen; SystemUI draws the real one): measured and reported, not gated. The Stitch `GOLD_CONFLICTS` set is empty.
8. **Specs:** [memoria-push](../../../produto/specifications/memoria-push.md) visual references (context, sheet actions, the push icon and accent) and Provenance; `docs/qa/README.md` inventory and gate note.

### Figma MCP

6 read-only `get_metadata` calls (budget 120): the cfg frame, the `Row/Setting` instance, the `Header/Page` instance, the wipe frame, its `Dialog` instance, the cfgS frame. Colours from `docs/design/tokens.json`.

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 504 tests, 0 failures. `./gradlew.bat :app:verifyRoborazziDevDebug`: pass. Baselines re-recorded only for this flow: `cfgSlots`, `cfgWorkout` (both themes; the smoke test now wraps Config in `AeroTheme`).
2. JVM gold (`StitchGoldTest`, Figma frame heights cfg 917, cfgS 845, wipe 844 dp), 2 iterations: iteration 1 cfg 1.46 / 2.34 %, cfgS 2.49 / 4.87 % (the groups lacked the 1 dp border inset; the cfgS label row is 18 dp with the Caption), wipe 0.08 / 0.05 %; iteration 2, dark / light: cfg 0.00 / 0.00 %, cfgS 0.00 / 0.00 %, wipe 0.08 / 0.05 %.
3. Emulator (API 36, 780 × 1688, density 320), `tools/capture-config.sh` and `tools/capture-push.sh` in both themes, `node tools/diff-gold.mjs`, 2 iterations:
   - iteration 1: cfg 0.00 / 0.00 %, cfgS 0.00 / 0.00 % passed; wipe failed whole-screen (3.39 % light) because the dialog is centred on the phone screen, which has the status and navigation bars the frame lacks;
   - iteration 2: wipe gated on the dialog box centred on the capture, as `chatP`; the dialog ink background became the median of the box (one sampled pixel landed on the blurred page behind the glass, which sits 23 dp lower on the phone). Final, dark / light: cfg 0.00 / 0.00 %, cfgS 0.00 / 0.00 %, wipe dialog 0.19 / 0.04 % (ink 1.01 / 1.03). `o3t` and `chatP` re-checked under the new ink rule: 1.04 / 0.84 % and 0.00 / 0.00 %. `push` reported: 25.54 / 79.71 % (lock screen wallpaper and clock are the system's).
   - Diff list (none blocks): content 23 dp lower (status bar, absent from the frames); the page behind the wipe dialog blurred at a different scroll offset; push: system lock screen, clock, date, the second notification and the system card, the app's icon (fork-knife on the accent), title and actions as in the frame.
   - Interaction checks: every capture-config check (rename without wipe, workout 400 / empty, wipe dialog Cancelar / Confirmar and the Room checks, grouped editor) and every capture-push check passed in both themes. The push check "Home asks for notification permission" had failed in every run since A21, because `capture-onboarding.sh` grants the permission up front; `capture-push.sh` now revokes it before that step.
4. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,102,723 → 19,053,691 bytes (−49,032: the legacy onboarding controls are gone).
5. `node tools/check-docs.mjs`: pass.

### Deviations

- Tooling outside the plan's file list, needed for the gate: `tools/diff-gold.mjs` (wipe centre box, push as a Figma conflict, median background for the dialog ink) and `tools/capture-push.sh` (permission reset).
- The D8 plan's link to this plan was updated to its new folder (link only, no content change), as for A40.

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](../a46-input-cursor-keyboard.md).
