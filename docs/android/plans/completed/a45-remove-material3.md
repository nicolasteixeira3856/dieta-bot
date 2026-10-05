# Plan — A45 Remove Material 3 Expressive

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code:
  - `apps/android/`: `core/designsystem/` (old `DietaBotTheme`, `ExpressiveButtonGroup`, M3 wrappers), every remaining `androidx.compose.material3` import in feature code, `gradle/libs.versions.toml`, `StitchGoldTest` renamed to `GoldTest`, tests;
  - docs updated with the delivery: `AGENTS.md` (Live stack, Tokens), `docs/tokens.md` (Material section removed), [ADR-004](../../../decisions/004-m3-expressive.md) status line, `docs/android/README.md` scope line.
- Prerequisites: [A44](a44-config-push-aero.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a45-remove-material3.md. Implemente o plano aprovado.`

## Objective

Finish [ADR-030](../../../design/adrs/ADR-030-own-design-system-aero.md): no Material look and no feature-level Material import left.

## Scope

1. **Remove the old theme:**
   - delete `MaterialExpressiveTheme`, `MotionScheme.expressive()`, the old `DietaBotTheme` colors/shapes and `ExpressiveButtonGroup`;
   - `AeroTheme` becomes the root theme in `MainActivity`.
2. **Feature imports:** `grep androidx.compose.material3` outside `core/designsystem/` returns nothing. Inside it, Material may remain only as a hidden behavior provider (sheet drag, dialog window), each use listed in Results.
3. **Dependencies:**
   - if no `material3` API is used anymore, remove the `material3 1.5.0-alpha29` override and its `compileSdk 37` reason;
   - otherwise keep the stable BOM version and record why;
   - Material icons (`material-icons-*`) are removed.
4. **Gold test:** `StitchGoldTest` becomes `GoldTest` and reads only `docs/qa/figma/`.
5. **Docs** (Completion):
   - `AGENTS.md` Live stack: Aero lines replace the MaterialExpressiveTheme/material3 lines, and Tokens point to Figma `Design`;
   - `docs/tokens.md`: drop the Material section;
   - ADR-004 status: `Superseded by ADR-030`;
   - the engineering skill `material3-expressive` is replaced by an `aero-compose` skill in the four trees, with `node tools/check-skills.mjs`.

## Out of scope

- Visual changes: every screen already matches its Figma gold (A40–A44).

## Validation

1. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` (no baseline change expected), `assembleDevRelease`; APK size delta recorded.
2. Every capture script still passes `diff-gold` against `docs/qa/figma/`.
3. `node tools/check-docs.mjs` and `node tools/check-skills.mjs` pass.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Old theme removed:** `DietaBotTheme.kt` (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, the old palette, shapes and typography, `DietaBotCta`), `DietaBotComponents.kt` (`WaitIndicator` on `LoadingIndicator`, `SheetActions`, `NumberField`, `TextBox`), `ExpressiveButtonGroup.kt`, `DietaBotType.kt` with the Plus Jakarta Sans and Inter fonts, the legacy `TimeWheelDialog` (its loop logic stays in `TimeWheelLoop.kt` for `AeroTimeWheelDialog`) and the old `DietaBotMeasure` / `DietaBotShapes` / `DietaBotHex` constants. `AeroTheme` is the root theme in `MainActivity`; each route still opens its own `AeroTheme`, so every screen keeps its own backdrop-blur state through navigation transitions.
2. **Last Material uses replaced:** the O4 help (`AlertDialog`) is `AeroNoticeDialog` and the meal-mode discard confirmation is `Dialog/Confirm` Tone=Danger, both drawn in the screen over the blurred page; the older-messages loader of the Chat is `AeroLoader`; the press feedback of `dietaClick` is an Aero press veil (`aeroPressIndication`, text/primary at 12 % while pressed) instead of the Material ripple; the dev memory tool (ADR-019, no gold) is on Aero tokens and components.
3. **Material inside `core/designsystem`:** none. `grep -rn "androidx.compose.material" apps/android/app/src` returns nothing (no `material3`, no `material-icons`). Left outside Compose: the platform window theme `android:Theme.Material.NoActionBar` of `res/values*/themes.xml` (the Android window, not a Compose dependency).
4. **Dependencies:** `material3 1.5.0-alpha29`, `material3-android` and `material-icons-extended` removed from `libs.versions.toml` and `app/build.gradle.kts`; the release runtime classpath has no `androidx.compose.material*` artifact (the debug-only `ui-tooling` still brings `material3` 1.4.0 for the preview tooling). `compileSdk` stays 37 with a new reason: with 36, `checkDevDebugAarMetadata` fails on Compose 1.12.1 of the stable BOM `2026.09.00` (`animation-core-android`), not on Material.
5. **Gold test:** `StitchGoldTest` is `GoldTest`: it asserts each id's source is `figma`, reads only `docs/qa/figma/`, renders the bare frame (no status or navigation inset) and writes to `build/outputs/gold/`. The Stitch geometry (bands, status rows, footers, `fullPage` / `navDp` / `footerDp`) is gone; the gate numbers are unchanged.
6. **Tests:** `ExpressiveButtonGroupTest` and the legacy `TimeWheelDialogTest` removed with their components (`AeroTimeWheelDialogTest` keeps the same four wheel checks); `TokensTest` checks the Aero tokens (WCAG AA 4.5:1 of every text, accent, status and macro colour on bg/page in both themes; accent never a macro colour) besides the formatters and the splash.
7. **Docs:** `AGENTS.md` Live stack (Aero lines replace MaterialExpressiveTheme / material3) and Tokens (values from the Figma `Design` variables through `docs/design/tokens.json`); `docs/tokens.md` without the Material section; [ADR-004](../../../decisions/004-m3-expressive.md) `Superseded by ADR-030`; `docs/android/README.md` scope and Visual QA lines; `docs/qa/README.md` names `GoldTest`.
8. **Skills:** `material3-expressive` replaced by `aero-compose` in `.agents`, `.claude`, `.grok` and `.hermes` (listed in AGENTS, the old name retired); `dieta-bot-android-ui` and `screenshot-testing` point to Aero and `GoldTest`.

### Figma MCP

0 calls (no layout change).

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 494 tests, 0 failures (504 before: −2 `ExpressiveButtonGroupTest`, −4 `TimeWheelDialogTest`, −4 legacy token checks). `./gradlew.bat :app:verifyRoborazziDevDebug`: pass, no baseline changed. `GoldTest`: every id passes as in A40–A44.
2. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,053,691 → 11,803,036 bytes (−7,250,655: Material icons, Material 3 and the two old fonts gone).
3. Emulator (API 36, 780 × 1688, density 320), every capture script in both themes (`capture-onboarding`, `capture-home`, `capture-config`, `capture-push` with the dev APK; `capture-photo`, `capture-replace`, `capture-chat` with the fake server), `node tools/diff-gold.mjs` against `docs/qa/figma/`, dark / light:
   - splash 0.00 / 0.00 %, o1 1.34 / 0.89 %, o1e 0.85 / 0.00 %, o2 1.01 / 0.69 %, o3 1.08 / 0.73 %, o3t dialog 1.04 / 0.84 %, o3s 1.14 / 0.74 %, o4 1.26 / 0.97 %;
   - home0 0.83 / 0.62 %, home1 1.03 / 0.83 %, homeX 0.82 / 0.43 %, homeW 0.09 + 0.22 / 1.14 + 0.32 %, chatP dialog 0.00 / 0.00 %;
   - chat0 0.16 / 0.33 %, chatL 0.00 / 0.00 %, chatQ 0.00 + 0.35 / 0.00 + 1.30 %, chatE 0.06 / 0.27 %, chatT 0.15 / 0.55 %, chatX 0.16 / 0.35 %;
   - chatA 0.80 / 0.97 %, chatF 0.00 + 0.00 / 0.01 + 0.30 %, chatG 0.00 + 0.46 / 0.00 + 0.59 %, chatU 0.00 + 0.65 / 0.00 + 1.06 %, chatD 0.00 + 0.00 / 0.00 + 0.23 %, chatR 0.00 + 0.03 / 0.00 + 0.15 %, chatM 0.00 + 0.02 / 0.00 + 0.22 %, chatS 0.00 + 0.05 / 0.00 + 0.07 %;
   - cfg 0.00 / 0.00 %, cfgS 0.00 / 0.00 %, wipe dialog 0.19 / 0.04 %; push reported (system lock screen).
   - All gates ≤ 2 %, the same numbers as A40–A44 (no visual change). Interaction checks: every check of every script passed in dark; in light, two A34 checks of `capture-chat.sh` (the inline `Substituir` card below the answer, the composer filled by Editar) failed once while the following steps of the same scenes passed (the card was confirmed and stored 620 kcal), and the `SCENES=a34` rerun in light passed all 40 checks. The same composer check flaked once in A42; both look up the screen before the thread settles.
4. `node tools/check-docs.mjs`: pass. `node tools/check-skills.mjs`: pass (27 skills, 137 files in each tree).

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](../pending_manual_validation/a46-input-cursor-keyboard.md).
