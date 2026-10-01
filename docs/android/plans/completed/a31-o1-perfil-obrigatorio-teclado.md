# Plan — A31 O1: required profile and keyboard flow

- Status: Concluído
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`feature/onboarding/*`, tests and captures)
- Prerequisites: **[ST8](../../../stitch/plans/completed/st8-teto-sem-perfil.md) in `stitch/plans/completed/`** (new gold `o1e`). Without it the implementation does not start.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a31-o1-perfil-obrigatorio-teclado.md`. Implemente o plano aprovado.

**First implementation step:** confirm `docs/stitch/plans/completed/st8-teto-sem-perfil.md` and the golds `docs/qa/stitch/{dark,light}/o1e.png`. Otherwise stop and tell the owner.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

O1 only continues with a full profile: sex, age, height and weight. Until the three body fields are filled, the ceiling mode and the ceiling fields are disabled (gold `o1e`). The keyboard walks the form: Next from age to height to weight, Done on weight closes the keyboard.

## Owner decisions (01/10/2026)

1. Age, height and weight are required to continue. A user who does not want to give them cannot set the ceiling by hand.
2. Until they are filled, "Modo do teto" and the ceiling fields are disabled.
3. Keyboard: the IME action moves to the next field; the last body field shows Done and closes the keyboard.
4. The disabled state gets its own gold through a Stitch gate (ST8).

## Sources of truth

- Golds `o1` (filled) and `o1e` (before the profile), dark and light.
- [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) rule 1 and "Estados e falhas".
- `AGENTS.md` tokens; skills `dieta-bot-android-ui`, `material3-expressive`, `dieta-bot-android-visual`, `compose-stability`, `screenshot-testing`.

## Implementation scope

### 1. Rule (`OnboardingUiState`)

- `profileValid`: `sex` set, `ageField`, `heightField` > 0 as integers and `weightField` > 0 as decimal. Same bounds as `TmbCalculator` (> 0); no new plausibility ranges.
- `o1Valid = profileValid && ceilingFields().all { > 0 }`.
- Ceiling defaults: `sameField`, `weekdayField`, `weekendField` and `dayFields` start blank instead of `"2000"`. They are filled by the TMB prefill as soon as the profile becomes valid (current `ceilingPrefill` path, unchanged while `ceilingEdited` is false). `day1Ceiling` keeps its 2000 fallback for O4 only.
- Clearing a body field after the ceiling was edited: the controls disable again and keep the typed values; Continuar turns off.
- A stored profile read back (`loaded`, back from O2) is valid and shows `o1`, as today.

### 2. UI (`OnboardingScreens.O1`)

Layout, tokens and measures from gold `o1e`:

- `ModeGroup` with `enabled = ui.profileValid`: 38 % opacity, no click, selected radio as a `dim` ring without gold fill.
- `KcalField` (all three modes) with `enabled = ui.profileValid`: 38 % opacity, not focusable; blank value shows `—` in `dim`.
- Without a valid profile, the "Sugerido …" caption under the ceiling field is hidden and the hint `Preencha idade, altura e peso para ver a meta sugerida.` shows right below the body fields, above "MODO DO TETO", in `muted`, info outline icon in `muted` (`testTag("o1-profile-hint")`). The `o1e` gold is taller than one phone screen (ST8 prompt 8.2): the content scrolls, and `StitchGoldTest` renders `o1e` as a full-page capture, like O3.
- Continuar uses the existing disabled `PillCta` (38 %), no change.
- Body fields and the sex toggle stay enabled.

### 3. Keyboard

- `UnitField` and `KcalField` take `imeAction` and `keyboardActions`.
- Age `Next` → height; height `Next` → weight; weight `Done` → `focusManager.clearFocus()` (keyboard closes). Moves use `FocusRequester`s, not `moveFocus`, so the order does not depend on the layout direction.
- Ceiling fields (once enabled): single field `Done`; weekday → weekend `Next`, weekend `Done`; seven days Seg → … → Dom with `Next`, Dom `Done`. Done clears focus.
- Keyboard types unchanged (`Number`, weight `Decimal`).

### 4. Spec

- [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md): rule 1 (age, height and weight required; ceiling controls disabled until then; prefill on valid profile; IME order) and "Estados e falhas" (`o1e`).

## Affected files

- `feature/onboarding/OnboardingUiState.kt`, `OnboardingViewModel.kt`, `OnboardingScreens.kt`.
- Tests: `OnboardingViewModelTest`, onboarding fixtures, `StitchGoldTest` (`o1e_dark`, `o1e_light`), Roborazzi baseline for `o1e`.
- `tools/capture-*.sh` (capture `o1e`), `tools/diff-gold.mjs` (`o1e` in the O1 set), `docs/produto/specifications/perfil-onboarding.md`.

## Planned validation

1. `testDevDebugUnitTest`:
   - Only age → `o1Valid` false; age + height → false; all three → true with the TMB prefill in every ceiling field.
   - Valid profile, ceiling edited, weight cleared → `profileValid` false, edited value kept, `o1Valid` false.
   - Stored profile loaded → valid.
2. `verifyRoborazziDevDebug`; `StitchGoldTest` `o1e` dark/light ≤ 2 % against the gold; `o1` without regression.
3. Emulator (dev): fresh install → `o1e` state; type age, Next, height, Next, weight, Done → keyboard closed, mode and ceiling enabled with the suggested value, Continuar on. Seven-day mode: Next walks Seg → Dom, Done closes. Captures `docs/qa/android/current/{dark,light}/o1e.png` and `o1.png` + `node tools/diff-gold.mjs dark/o1e light/o1e dark/o1 light/o1` + written diff list.

## Out of scope

- Plausibility ranges for age, height or weight.
- Config (`cfg`) body and ceiling fields.
- O2–O4.

## Risks and controls

- **Existing tests assume the 2000 default:** update fixtures; `day1Ceiling` keeps 2000 so O4 tests do not move.
- **Focus jumping into a disabled field:** disabled `KcalField`s are not focusable; weight ends with Done, never Next.

## Acceptance criteria

- Continuar only with sex, age, height, weight and ceiling filled.
- Mode and ceiling disabled until the profile is filled, matching `o1e`.
- Keyboard walks age → height → weight and closes on Done.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`, with the git delivery (§ 6).

Only an explicit owner statement cancelling this plan allows `Cancelado` and the `plans/cancelled/` folder.

## Results (01/10/2026)

Approved by the owner on 01/10/2026 with the sentence of the authorization gate. Prerequisite checked first: `st8-teto-sem-perfil.md` in `stitch/plans/completed/` and golds `docs/qa/stitch/{dark,light}/o1e.png` present.

Implemented:

- `OnboardingUiState`: `profileValid` (sex set, age and height > 0 as integers, weight > 0 as decimal); `o1Valid = profileValid && ceilingFields() > 0`. `sameField`, `weekdayField`, `weekendField` and `dayFields` start blank. `day1Ceiling` keeps its 2000 fallback.
- `OnboardingViewModel`: the stored ceiling is read back only when the stored body profile is complete; otherwise the fields stay blank until the TMB prefill (`DaySnapshot` defaults are 2000/2300 even without a profile row). The prefill path is unchanged.
- `OnboardingScreens.CeilingScreen`: `ModeGroup(enabled)` and `KcalField(enabled)` dim to 38 % through one save layer (`Modifier.disabledAlpha`), no click, the text field is `enabled = false` (not focusable), a blank value shows `—` in `dim`, the `kcal` chip turns `dim` with a `line` border; `GoldRadio(enabled = false)` draws a `dim` ring and dot. Without a valid profile the "Sugerido" caption is hidden and `o1-profile-hint` (info icon + copy, `muted`) sits under the body fields. `UnitField`/`KcalField` take `imeAction`/`keyboardActions`; moves use `FocusRequester`s; Done calls `clearFocus()`. New tags `o1-weekday`, `o1-weekend`, `o1-day-<i>`. `ModeGroup`/`KcalField` keep their defaults for `cfg` (out of scope, unchanged). The disabled `PillCta` uses the same `DISABLED_ALPHA` constant (same 0.38).
- Spacing: the `o1e` gold is tighter than `o1` (sex toggle 9–13 dp higher, toggle → body label 6–10 dp less, mode group → "META DIÁRIA" 5–8 dp less; the dark and light `o1e` golds also disagree by up to 16 dp between them). The disabled state takes its own spacers (14 / 21 / 25 / 23 dp, the average of both golds); `o1` keeps 25 / 29 / 30 / 30 dp. The hint already reflows the screen when the profile completes, so the extra shift happens at the same moment.
- `modifier.alpha()` (graphics layer) was not drawn by the Robolectric capture; the save layer renders the same on device and in the JVM test.
- Tools: `capture-onboarding.sh` captures `o1e` after Homem, then walks age → Enter → height → Enter → weight → Enter and fails if the keyboard is still open. `diff-gold.mjs` default set includes `o1e`.

Validation:

1. `:app:testDevDebugUnitTest`: **391** tests, 0 failures (one intermediate run hit the known `Dispatchers.Main is used concurrently` flake in `ChatViewModelTest`/`ConfigViewModelTest`; the rerun was green). New in `OnboardingViewModelTest`: only age → `o1Valid` false; age + height → false; all three → true with 2160 in all 10 ceiling fields; ceiling edited to 1800, weight cleared → `profileValid` false, 1800 kept, `o1Valid` false; stored profile read by a fresh ViewModel → valid, `sameField` 2160. Existing O1/O4/complete tests unchanged and green. ✅
2. `:app:verifyRoborazziDevDebug` green; new baselines `snapshots/{dark,light}/o1e.png` (925 dp). `StitchGoldTest`: `o1e` dark **1.10 %**, light **1.56 %** (full page, 925 dp); `o1` dark 1.07 %, light 0.78 % (unchanged). ✅
3. `:app:assembleDevRelease` ✅
4. Emulator (devDebug, Medium_Phone API 36, 780×1688 @ 320 dpi): `capture-onboarding.sh` dark and light pass end to end (keyboard closed after Done, relaunch skips onboarding). Manual walk: before the profile, tapping "Personalizado por dia" keeps the single-field mode and tapping the ceiling opens no keyboard; age Next → height, Next → weight, Done → keyboard closed, ceiling 2160, Continuar enabled; seven-day mode Seg Next → Ter → … → Dom, Done → keyboard closed. ✅
5. `node tools/diff-gold.mjs dark/o1e light/o1e dark/o1 light/o1`: o1e dark 1.27 %, light 1.35 %; o1 dark 1.16 %, light 0.82 % (max 2 %). ✅ Captures in `docs/qa/android/current/{dark,light}/o1e.png` and `o1.png`.

Diff list (`o1e` vs gold, both themes):

- Layout: same order (header, sex toggle, body fields, hint, MODO DO TETO, mode group, META DIÁRIA, kcal field, CTA). Spacing fitted between the two golds (see above); residual ±4–8 dp versus each theme.
- Tokens: hint and icon `muted`; placeholders `dim`; disabled radio `dim` ring and dot, no gold. Light: mode titles at 38 % of `text` match the gold (162 vs 160). Dark: the gold draws the disabled titles dimmer than its own 38 % spec (≈ `dim` at 38 %); the app follows the plan's 38 % of the normal colours. Disabled `kcal` chip: `dim` text and icon, `line` border, no gold fill (gold: same; ST8 had flagged a gold chip as a defect).
- Type size: hint 14 sp / 19 sp line; wraps after "meta" in dark, as in the dark gold (the light gold wraps one word earlier, font raster).
- Radius: fields and group 16 dp, unchanged.
- CTA: existing disabled `PillCta` (38 %), unchanged.
- Timeline, semantic macros, ButtonGroup: not on this screen.

No pending manual validation.
