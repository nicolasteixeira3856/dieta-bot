# Plan — A53 Config: app reset

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Config screen and ViewModel, a reset use case over Room, memory file, photo store and push, navigation, telemetry, tests) plus `tools/capture-config.sh`.
- Related documentation: [memoria-push](../../produto/specifications/memoria-push.md) § Config, [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [Room](../specifications/room-v2.md) (no schema change), Android validation and plan indexes.
- Prerequisites: [D15](../../design/plans/completed/d15-config-app-reset.md) `Concluído` with `cfg` and `cfgR` exported; ADR-040 accepted. Independent of [A52](a52-home-card-gestures.md); not run in parallel with another Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a53-config-app-reset.md. Implemente o plano aprovado.`

## Objective

Config offers `Resetar app`; confirming erases all local user data and reopens O1 ([ADR-040](../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) decision 2).

## Scope

1. **UI.** The `Dados` block and `Resetar app` row on Config, and the `cfgR` dialog (`AeroConfirmDialog` danger, like `WipeDialog`), copy taken from the D15 gold. Test tags `cfg-reset`, `cfg-reset-dialog`, `cfg-reset-confirm`, `cfg-reset-cancel`. Confirm vibrates like Salvar; while the reset runs the dialog's buttons are disabled.
2. **Reset use case** (`domain`/`data`, injected), in this order:
   1. mark the profile `onboardingDone = 0` (one row write) so an interrupted reset lands on onboarding;
   2. cancel every slot alarm and clear shown notifications; clear the `fibrai_push` and scheduler preferences;
   3. delete the memory file (`memory.bin`, `.new`, legacy `memory.txt`) and the Chat photo directory;
   4. `clearAllTables()` on the Room database.
   Kept: the installation id (`noBackupFilesDir`) and dev-flavor settings. Every step is idempotent.
3. **Interrupted reset.** At cold start, `onboardingDone = 0` with leftover rows or files runs the same use case before O1. A fresh install runs it as a no-op.
4. **Navigation.** After success, navigate to the onboarding route clearing the whole back stack; back from O1 leaves the app. A failure keeps the user on Config with the dialog closed and logs the step (no data in the event).
5. **Telemetry.** One event `app_reset` with the outcome enum (`done` | `failed`) and the failed step enum. No user text.
6. **QA tooling.** `tools/capture-config.sh` gains: open `cfgR`, cancel (Room unchanged), confirm, assert O1 empty, Room tables empty, `memory.bin` and photos gone, no alarm in `dumpsys alarm` for the package, installation id unchanged.

### Intended specification changes

At Completion: memoria-push § Config gains the reset rule (row, `cfgR`, what is erased and kept, O1 after); perfil-onboarding "Estados e falhas" gains "Reset na Config: como reinstalação, onboarding de novo"; Provenance gains A53. The inventory gains `cfgR` through D15, not here.

## Out of scope

Server data, installation id rotation, export/backup before reset, partial resets, Home, dev tools.

## Validation

1. Unit tests of the use case with fakes: every step called once in order; re-running after a failure at each step completes; installation id untouched.
2. Room test: seeded profile, slots, days, logs, skips, chat rows and digests are all empty after reset; schema unchanged.
3. ViewModel tests: cancel changes nothing; confirm navigates once to onboarding; double tap runs one reset; failure stays on Config.
4. Cold-start test: `onboardingDone = 0` with leftover memory file → cleaned before O1.
5. Emulator, dev flavor: fresh `cfg` and `cfgR` captures compared with the D15 golds, written diff list; then the full reset path above, followed by a complete onboarding and a Home with day 1 zeroes. Partial validation: Config and onboarding flows only.
6. `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.
7. Manual: owner resets a device with real data (dev build) and redoes the onboarding.

## Results

Planning only.
