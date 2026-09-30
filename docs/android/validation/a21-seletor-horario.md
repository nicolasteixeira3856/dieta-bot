# A21 — Meal time wheel validation

Date: 2026-09-29 (America/Sao_Paulo).
Plan: [A21](../plans/completed/a21-seletor-horario.md).
Design prerequisite: [completed ST3](../../stitch/plans/completed/st3-seletor-horario.md).

## Implementation

`TimeWheelDialog` is shared by onboarding O3 and the Config meal editor. It uses
two snapped lazy lists with one million virtual indices, initialized near the
middle at the supplied hour/minute. Values repeat every 24/60 indices; the step
is one minute. OK returns the currently centered time in minutes since midnight;
Cancel and outside/back dismissal leave the caller's draft unchanged.

The title uses the meal name, falling back to `Refeição {n}` for an empty name.
Each wheel exposes an adjustable range and the pt-BR state description, supports
arrow/volume keys while focused, and sends a light `TextHandleMove` haptic when
its centered index changes. The platform respects the system touch-vibration
setting. No clock picker, keyboard toggle or typing mode remains.

## Automated checks

- `:app:testDevDebugUnitTest --tests '*TimeWheel*'`: 7/7 passed. Covers every
  starting hour/minute, many virtual cycles, both wrap directions, a slow touch
  drag of exactly three minute rows followed by OK (07:30 → 07:33), accessibility
  adjustment across midnight, cancellation, announced values, arrow and volume
  input, and confirmation of 21:45.
- `:app:verifyRoborazziDevDebug --tests '*RoborazziSmokeTest*'`: 16/16 passed,
  including new dark/light `o3t` baselines. Existing baselines were verified
  before recording only the two new screenshots.
- `:app:testDevDebugUnitTest :app:assembleDevRelease --continue`: 228 tests,
  226 passed. Only the two existing `StitchGoldTest.o3_*` failures remain, with
  the identical baseline scores documented below. `assembleDevRelease` completed
  successfully, including release compilation and vital lint. The combined
  Gradle invocation exits nonzero because of those two unit-test failures.
- `node tools/check-stitch.mjs`: all 48 design PNGs passed inventory checks.
- Centered-dialog gate fixtures: identical golds pass and blank captures fail in
  both themes. Original emulator images were restored after each fixture.
- Bash/Node syntax and all relative links in changed documents passed checks.
- Plan-state directories: `completed/` and `pending_manual_validation/` both
  contain plans; no empty state directory required removal.
- `tools/capture-onboarding.sh dark|light`: both completed successfully. The
  script taps the first O3 time field, cancels, completes onboarding and verifies
  persistence on restart. It seeds the Unicode gold name in the emulator's test
  Room profile, reopens the existing O3 capture entry and captures the same real
  time-field interaction. It returns to Home afterwards.
- Config emulator journey: opened the first meal time from the slot editor;
  scrolling then Cancel preserved 07:30. Reopened, scrolled three rows to 07:33,
  confirmed OK and saved the sheet. Config showed 07:33 again after a complete
  process restart. The shared dialog used the same measured action bounds.

## Visual comparison

Device: `emulator-5554` (Medium_Phone), 780×1688 px, density 320. Fresh captures:
[dark](../../qa/android/current/dark/o3t.png) and
[light](../../qa/android/current/light/o3t.png). Golds were not modified.

ST3 exports are 780×2206 px. The complete dialog is centered in both viewports,
so its expected vertical shift is (1688 − 2206) / 2 = −259 px. The comparison tool
gates the full dialog with the existing 2% blurred-pixel threshold and 0.8–1.25
content-presence ratio. The underlying O3 remains a separate test; its ST4
changes are outside A21 and belong to A24.

| Theme | Full dialog difference | Content ratio | Result |
| --- | ---: | ---: | --- |
| Dark | 0.97% | 1.03 | Pass |
| Light | 1.80% | 1.04 | Pass |

Written diff after iterations:

- Layout: centered dialog, 24 dp exterior margins, title/subtitle centered,
  five visible values per wheel, one uninterrupted selection band and a colon.
  The whole dialog is approximately `[48,452][732,1236]` dark and
  `[48,464][732,1224]` light. The background is blurred and dimmed as in ST3.
- Tokens: theme surface/text/muted/line roles; neutral selection band; the ST3
  contrast pair for OK. Gold stays confined to the unchanged background accents.
- Type: title 18 sp semibold, subtitle 13 sp; center 40 sp, adjacent 28 sp at
  50% opacity, outer 22 sp at 25%. Theme-specific neighbor weight/spacing follows
  the exported images. Buttons both use 16 sp semibold.
- Radius: dialog 28 dp, selection band 12 dp, both actions pill-shaped. The
  existing Config sheet keeps its 22 dp top corners.
- ButtonGroup: the existing O3 meal-count control is unchanged. The dialog has
  two equal-width actions, each 48 dp tall, with a 12 dp gap.
- CTA: Cancel outlined with primary text; OK filled with the ST3 contrast pair.
  No gold-colored text-only confirmation remains.
- Timeline and semantic macros: unaffected; this dialog edits only a meal time.

Measured action bounds from `android layout --flat --full`:

| Theme | Cancel | OK | Height |
| --- | --- | --- | --- |
| Dark | `[96,1092][378,1188]` | `[402,1092][684,1188]` | 96 px = 48 dp |
| Light | `[100,1080][378,1176]` | `[402,1080][680,1176]` | 96 px = 48 dp |

The hour/minute controls are each 400 px tall; their shared center is y=844.
Only system clock, battery/navigation and font raster differences are ignored.

## Existing O3 gold mismatch

The `master` baseline at `8f9eb55` was tested separately with
`:app:testDevDebugUnitTest --tests '*StitchGoldTest.o3_*'`. Both tests fail with
the same existing ST4 mismatch: dark 6.35% / ink 0.66, light 5.75% / ink 0.70.
The baseline contains no A21 changes. No threshold was relaxed and no failing
test was disabled. The wheel dialog has its own passing ST3 comparison.

## Pending owner validation

Choose 07:30 and 21:45 comfortably on the physical device. This subjective check
has not been performed by the agent. The owner approved the plan on 30/09/2026; it is `Concluído`.
