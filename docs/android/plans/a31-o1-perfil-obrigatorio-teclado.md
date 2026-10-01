# Plan — A31 O1: required profile and keyboard flow

- Status: Aguardando aprovação
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`feature/onboarding/*`, tests and captures)
- Prerequisites: **[ST8](../../stitch/plans/completed/st8-teto-sem-perfil.md) in `stitch/plans/completed/`** (new gold `o1e`). Without it the implementation does not start.

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
- [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) rule 1 and "Estados e falhas".
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

- [perfil-onboarding](../../produto/specifications/perfil-onboarding.md): rule 1 (age, height and weight required; ceiling controls disabled until then; prefill on valid profile; IME order) and "Estados e falhas" (`o1e`).

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
