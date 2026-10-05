# Plan — A40 Home on Aero

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/home/`, Home-specific components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `home0`, `home1`, `homeX` and `homeW` in `docs/qa/README.md`, captures in `docs/qa/android/current/{dark,light}/`.
- Prerequisites: [A39](a39-aero-foundation.md) and [D3](../../../design/plans/completed/d3-release1-home.md) `Concluído` (Figma golds in `docs/qa/figma/`).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a40-home-aero.md. Implemente o plano aprovado.`

## Objective

Move the Home screen and its four states onto Aero, matching the Figma golds, with no behavior change.

## Scope

1. **Build:**
   - the Home is wrapped in `AeroTheme`, using the A39 components plus the D3 additions (`AeroRingDay`, `AeroSheet`, `AeroFieldNumber`, `AeroHeaderDay`);
   - the workout sheet (`homeW`) uses `AeroSheet`;
   - testTags and the capture flow (`tools/capture-home.sh`) are unchanged.
2. **Values:** exact values (color, radius, spacing, type) come from the Figma variables and component properties read through the MCP, not measured from the PNG.
3. **Visual loop** ([docs/qa/README.md](../../../qa/README.md) § Loop, against `docs/qa/figma/`):
   - capture → `diff-gold` → diff list in Results → fix, until each of the 8 images passes the gate (≤ 2 %);
   - glass blur is rasterized differently by Figma and Android: when a difference is only blur grain inside a glass surface, it is listed as such and not counted.
4. **Switch:** in the same delivery, the source of the four Home ids becomes `figma` in the inventory.
5. **Spec:** [home-timeline](../../../produto/specifications/home-timeline.md) changes only its visual references (gold source, tokens); behavior text stays. A40 joins its Provenance.

## Out of scope

- Other flows; Material removal ([A45](a45-remove-material3.md)); behavior or copy.

## Validation

1. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` (baselines re-recorded for Home only), `assembleDevRelease`.
2. `tools/capture-home.sh dark|light` + `node tools/diff-gold.mjs dark/home1 …`: every Home gold passes.
3. Manual, on the owner's phone, as one consolidated check for A39–A45 (owner decision, 2026-10-04), on the dev APK that `tools/distribute-dev.ps1` ships after A45:
   - glass blur (A39);
   - scroll smoothness of the timeline with blur (no visible jank);
   - number legibility outdoors in both themes;
   - the same two checks on every migrated flow: Home, splash and onboarding, Chat, Config and push.

   Until the owner's OK, the plan waits in `pending_manual_validation/` and the next plan of the chain may start. After the OK, the agent closes every plan waiting on this check in one delivery.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Home on Aero.** `MainActivity` wraps the Home route in `AeroTheme`; `HomePanelScreen` is rebuilt from the Figma frames (`Release 1` → `Home · D3`): `AeroPage` (the page gradient spans the scrolled page, as the frame fill does), `AeroHeaderDay`, a glass hero card with `AeroRingDay` (+ the `Meta excedida` Bad chip in homeX), the macro card (`AeroMacroRow` ×3), the workout row (barbell well, `Informar` in the accent or `{kcal} kcal · +{n} na meta`), the timeline (`AeroTimelineNode` + `AeroMealCard`, guide from node centre to node centre), the centred disclaimer, the three edge bubbles of the frame and the `Chat` FAB (`AeroButtonPrimary` with `chat-circle`). Margins 20, top 24, 20 between blocks; the disclaimer ends 20 dp above the FAB at the end of the scroll.
2. **homeW.** `WorkoutSheet` is an `AeroSheet` with `AeroFieldNumber` (focused border, barbell, live credit line) over the Home blurred 8 dp and `overlay/scrim`. The Config workout sheet keeps the old `WorkoutField` until A44. The skip confirmation (no gold) is an Aero glass card with the same action pair, still in the Material dialog window (A45 scope).
3. **New Aero components** (`core/designsystem/aero/AeroHomeComponents.kt`): `AeroHeaderDay`, `AeroRingDay` (Normal/Exceeded/Empty, 196 dp, inner radius 88 %), `AeroFieldNumber`, `AeroSheet`, `AeroBubble`. A39 components extended: `AeroMealCard` takes several log lines (`AeroMealLine`), `AeroButtonPrimary` can wrap its content (FAB), `aeroGlass(backdropBlurred)` for a sheet over a blurred screen, `AeroPage(scroll)`.
4. **testTags and `tools/capture-home.sh`** unchanged; every interaction check of the script passes in both themes.
5. **Gold sources.** `home0`, `home1`, `homeX`, `homeW` are `figma` in the inventory. `StitchGoldTest` renders a `figma` id as the bare frame (frame height, no status or nav inset) and gates the whole page; Stitch ids keep their old geometry and exceptions.
6. **`tools/diff-gold.mjs`** (outside the plan's file list, needed for the gate): the Stitch exception lists (conflicts, regions) apply to `stitch` ids only; `figma` ids get their own rules. `homeW` Figma: screen reported, gate on the blurred Home at the top (gold rows 40–450 dp) and on the bottom-anchored sheet. Stitch results unchanged (`node tools/diff-gold.mjs` default set: same numbers as before).
7. **Docs:** [home-timeline](../../../produto/specifications/home-timeline.md) visual references (gold source, Aero tokens, 20 dp respiro, `accent/default`, `Field/Number`) and Provenance; `docs/qa/README.md` gate note for Figma golds.

### Figma MCP

5 calls (budget 120): 4 read-only `use_figma` (2 of them failed on a script error and were retried) and 1 more read for the workout row, homeX hero, homeW layers and bubble paint. Values not read from the PNG.

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 498 tests, 0 failures. `./gradlew.bat :app:verifyRoborazziDevDebug`: pass; baselines re-recorded only for `homeW` (dark, light).
2. JVM gold (`StitchGoldTest`, Figma frame geometry, blurred diff): home0 0.01 % / 0.02 %, home1 0.12 % / 0.12 %, homeX 0.03 % / 0.06 %, homeW 0.09 % / 0.15 % (dark / light), ink 0.89–1.07.
3. Emulator (API 36, `wm size 780x1688`, density 320), `tools/capture-home.sh dark|light` + `node tools/diff-gold.mjs`, 2 iterations:
   - iteration 1: dark passed; light failed (home0 9.75 %, home1 13.28 %, homeX 10.05 %) because the page gradient was fixed to the 844 dp screen while the Figma frame spreads it over the whole page. Fix: the gradient spans the scrolled page and moves with it;
   - iteration 2: dark home0 0.85 %, home1 1.06 %, homeX 0.85 %, homeW top 0.10 % and sheet 0.22 %; light home0 0.64 %, home1 0.85 %, homeX 0.45 %, homeW top 1.61 % and sheet 0.39 %. All gates ≤ 2 %, ink 1.00–1.04. homeW whole screen reported (20.8 % / 39.2 %): the 844 dp phone puts the sheet over the Home that the 1414 dp frame shows.
   - Remaining diff list (none blocks): content 24 dp lower (status bar; the frame has none); date and meta come from the seeded day ("4 de outubro", `Meta 2000 kcal`) instead of the frame's sample ("25 de setembro", 2175); homeW sheet glass shows a little more of the blurred Home than the frame; blur grain inside the glass surfaces is not counted (Figma and Android rasterize it differently). Layout, tokens, type sizes, radii, CTA, timeline guide and nodes, semantic macro colors and the consolidated meal chip match.
4. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,069,363 → 19,069,363 bytes (no measurable delta: the dex files are stored uncompressed and page-aligned).
5. `node tools/check-docs.mjs`: pass.

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](a46-input-cursor-keyboard.md).
