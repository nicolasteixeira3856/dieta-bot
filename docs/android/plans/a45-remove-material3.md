# Plan — A45 Remove Material 3 Expressive

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code:
  - `apps/android/`: `core/designsystem/` (old `DietaBotTheme`, `ExpressiveButtonGroup`, M3 wrappers), every remaining `androidx.compose.material3` import in feature code, `gradle/libs.versions.toml`, `StitchGoldTest` renamed to `GoldTest`, tests;
  - docs updated with the delivery: `AGENTS.md` (Live stack, Tokens), `docs/tokens.md` (Material section removed), [ADR-004](../../decisions/004-m3-expressive.md) status line, `docs/android/README.md` scope line.
- Prerequisites: [A44](pending_manual_validation/a44-config-push-aero.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](pending_manual_validation/a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a45-remove-material3.md. Implemente o plano aprovado.`

## Objective

Finish [ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md): no Material look and no feature-level Material import left.

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

<Filled at Completion.>
