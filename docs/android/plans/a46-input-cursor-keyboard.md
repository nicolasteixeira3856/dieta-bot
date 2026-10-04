# Plan — A46 Text input: cursor at the end, focused field above the keyboard

- Status: Aguardando aprovação
- Date: 04/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `core/designsystem/aero/` (`AeroNumberField`, `AeroMealSlotRow`, `AeroMacroTargetCard`, `AeroSheet`), `feature/onboarding/OnboardingAeroScreens.kt` (onboarding frame), `feature/config/ConfigScreen.kt` (edit sheets), tests;
  - `tools/capture-onboarding.sh` and `tools/capture-config.sh` (new interaction checks);
  - docs updated with the delivery: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) and [memoria-push](../../produto/specifications/memoria-push.md) rules and Provenance.
- Prerequisites: [A41](completed/a41-splash-onboarding-aero.md) and [A44](completed/a44-config-push-aero.md) `Concluído`.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a46-input-cursor-keyboard.md. Implemente o plano aprovado.`

## Objective

Fix the two input defects the owner found in the consolidated phone check of A39–A45 (04/10/2026), in onboarding and in Config:

1. **Keyboard over the field:** in O1 with `Metas separadas`, focusing `Fim de semana` opens the keyboard over half the screen and hides the field being typed.
2. **Cursor at the start:** in O4, tapping to edit a macro computed by the app (e.g. `200` g of carbs) puts the cursor before `200` instead of after it.

Rule from the owner: every edit of an existing value starts with the cursor at the end, and the field being typed is always visible above the keyboard.

## Diagnosis (current code, 04/10/2026)

1. **Keyboard.** `MainActivity` calls `enableEdgeToEdge()`. Under edge-to-edge, the manifest's `adjustResize` no longer shrinks the window, and the IME inset must be consumed by the layout. The onboarding frame (`AeroOnboardingFrame`) pads only the status and navigation bars, so its scroll viewport stays the full screen behind the keyboard. A field in the lower half (O1 weekday / weekend and per-day fields, O1 body fields on short screens, the O2 percent field, the O3 meal names, the O4 grams) cannot scroll above the keyboard. The legacy frame had the same gap; it is not an A41 regression.
   - **Config, same defect:** the meal editor reuses `OnboardingSlotsScreen` and that frame. The meal names near the bottom of `Metas`, `Seg a Sex` or `Cada dia` stay under the keyboard.
   - **Config, at risk:** the edit sheets (`AeroSheet` + `imePadding`) rise with the keyboard. But their editor area is capped at half the screen, plus title and the 58 + 12 + 58 dp action pair. With the keyboard open, the ceiling sheet in `Personalizado por dia` (and `Metas separadas` on short screens) can be taller than the room left, and its top or the focused field leaves the screen. To be confirmed on the emulator in step 1 of the implementation.
2. **Cursor.** `AeroNumberField` (O1 age, height, weight and ceiling fields, O2 percent, O4 and Config grams through `AeroMacroTargetCard`, Config ceiling and percent) and the meal-name field of `AeroMealSlotRow` use the `String` overload of `BasicTextField`. That overload keeps its own selection, which starts at 0. Focus given by code (the adjust button of `Card/MacroTarget`, `Próximo` on the keyboard) leaves the cursor at the start, and a tap leaves it wherever the finger lands. **Config has the same defect:** its macros sheet uses `AeroMacroTargetCard` and its ceiling sheet uses `AeroNumberField`. The fields that already do it right are the Home and Config workout field (`AeroFieldNumber`, `TextFieldValue` with the cursor at the end), the Chat composer and the dev memory tool.

## Scope

1. **Cursor at the end on focus.** `AeroNumberField` and the `AeroMealSlotRow` name field hold a `TextFieldValue`. When the field gains focus (tap, `Próximo`, adjust button, `requestFocus`), the selection becomes the end of the text. While focused, taps and the user's own cursor moves are respected (they can still edit in the middle). An external value change (prefill, a suggestion chip, `Copiar de`) also puts the cursor at the end. The public API stays `String` in / `String` out, so callers and ViewModels do not change.
2. **Focused field above the keyboard, onboarding and the Config meal editor.** The scroll column of `AeroOnboardingFrame` consumes the IME inset, so the viewport ends at the top of the keyboard and Compose brings the focused field into view.
   - Decision proposed for approval: the fixed CTA (`Continuar` / `Salvar` / `Concluir e começar`) stays at the bottom, behind the keyboard, and returns when the keyboard closes. It does not ride above the keyboard, so the room left for the field is as large as possible. Keyboard `Concluído` closes it as today (A31).
3. **Config edit sheets.** The sheet never grows past the room between the status bar and the top of the keyboard. Its editor area shrinks and scrolls, title and actions stay visible, and the focused field is brought into view inside it. Applies to the ceiling (all three modes), eat-back percent and macros sheets. The workout sheet of the Home (`homeW`) uses the same `AeroSheet` and gets the same cap.
4. **No visual change with the keyboard closed:** every gold, Roborazzi baseline and diff-gold number stays as in A40–A45.
5. **Spec (Completion):**
   - [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) gains a rule for O1–O4: a field that receives focus shows its cursor at the end of the value; the focused field stays visible above the keyboard; the CTA stays behind the keyboard while it is open.
   - [memoria-push](../../produto/specifications/memoria-push.md) Config rules gain the same rule for the edit sheets and the meal editor.

## Out of scope

- The Chat composer and the workout field (already put the cursor at the end; only the sheet cap of item 3 touches `homeW`).
- Layout, copy or behaviour changes with the keyboard closed; new screens; Figma frames (the open keyboard has no gold).
- `server/`.

## Validation

1. Implementation step 1, before any change: on the emulator, reproduce both defects in O1 (`Metas separadas` → `Fim de semana`), O4 (adjust `Carboidrato`), the Config meal editor (last meal name), the Config macros sheet and the Config ceiling sheet in `Personalizado por dia` (`Dom`). Record which of them reproduce in Results.
2. JVM (Robolectric Compose tests): for `AeroNumberField`, the macro card adjust button and the meal-name field, focus by `requestFocus`, by tap and by `Próximo` leaves `selection == TextRange(text.length)`; typing `5` into `200` gives `2005`; an external value change moves the cursor to the end; a tap inside a focused field keeps its position.
3. Emulator, both themes, with new checks in the capture scripts:
   - `capture-onboarding.sh`: the weekend field, `Dom` (per-day mode), the last O3 meal name and an O4 grams field are inside the visible window above the IME (uiautomator bounds against the IME top from `dumpsys input_method` / window insets) after focus;
   - `capture-config.sh`: the same for the meal editor's last name and the ceiling sheet in `Personalizado por dia`, with title and `Salvar` visible;
   - both scripts: adjust + type `5` on a computed macro, and tap + type `5` on an existing value, read back `…5` at the end through uiautomator.
4. `./gradlew.bat :app:testDevDebugUnitTest`, `./gradlew.bat :app:verifyRoborazziDevDebug` (no baseline change expected), `./gradlew.bat :app:assembleDevRelease` (APK size delta recorded).
5. Every capture script still passes `node tools/diff-gold.mjs` against `docs/qa/figma/` in both themes (keyboard closed: same numbers as A45).
6. `node tools/check-docs.mjs` passes.
7. Manual (owner, phone): the two scenarios of the Objective, plus Config → `Horários das refeições` (last meal name) and Config → `Macronutrientes` (edit a value), in both themes.

## Results

<Filled at Completion.>
