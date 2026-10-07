# Plan — A53 Config: app reset

- Status: Concluído (07/10/2026, owner device check declared done)
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Config screen and ViewModel, a reset use case over Room, memory file, photo store and push, navigation, telemetry, tests) plus `tools/capture-config.sh`.
- Related documentation: [memoria-push](../../../produto/specifications/memoria-push.md) § Config, [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md), [Room](../../specifications/room-v2.md) (no schema change), Android validation and plan indexes.
- Prerequisites: [D15](../../../design/plans/completed/d15-config-app-reset.md) `Concluído` with `cfg` and `cfgR` exported; ADR-040 accepted. Independent of [A52](a52-home-card-gestures.md); not run in parallel with another Android plan.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a53-config-app-reset.md. Implemente o plano aprovado.`

## Objective

Config offers `Resetar app`; confirming erases all local user data and reopens O1 ([ADR-040](../../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) decision 2).

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

Delivered 06/10/2026.

- Code: `core/reset/AppReset.kt` (`AppReset` + `ResetSteps`, production `DeviceResetSteps`, Hilt `ResetModule`). Steps in order: `DayRepository.markOnboardingPending()`, `SlotAlarmScheduler.cancelAll()` (slot alarms, resync, `fibrai_push` preferences) + all shown notifications, delete `memory.bin`, `memory.bin.new`, `memory.txt` and `files/photos`, `DayRepository.clearAll()` (`clearAllTables`). Installation id untouched. `finishInterrupted()` runs only when something is left (`leftover`: files or any Room row). Telemetry `app_reset` (`outcome`, failed `step`) and a non-fatal on failure.
- UI: Config block `Dados` / `Resetar app` (`cfg-reset`) and `ResetDialog` (`cfg-reset-dialog`, `-confirm`, `-cancel`) with the D15 copy. `ConfigViewModel` `openReset` / `cancelReset` / `confirmReset` (one run per dialog; failure closes the dialog and stays). `MainActivity` opens `RouteOnboarding` popping the whole graph on `resetDone`. `SplashViewModel` calls `finishInterrupted()` when onboarding is not done.
- Deviation from the plan text: no separate cold-start test file; the interrupted path is covered by `DeviceResetStepsTest.interrupted_afterMark_isFinishedOnColdStart` and `ConfigViewModelTest.reset_failure_staysOnConfig_rerunCompletes`.
- Tests: `DeviceResetStepsTest` (3: everything erased with the installation id kept and push prefs empty; re-run no-op and nothing left on a fresh state; interrupted after the mark finished on cold start), `ConfigViewModelTest` +3 (cancel changes nothing; confirm runs mark → push → files → database once on a double tap and empties Room; failure at files stays on Config, onboarding pending, a re-run completes). `GoldTest` cfg at 1047 dp and new `cfgR` (dark, light) pass. Roborazzi `cfgWorkout` (dark, light) re-recorded: the blurred background now holds the Dados block, no visible change. `verifyRoborazziDevDebug` (all unit tests), `assembleDevRelease` and `assembleDevDebug` pass.
- Emulator (`Medium_Phone`, API 36, 780x1688 @ 320 dpi, dev debug), `tools/capture-config.sh dark|light`: every Config and reset check green: dialog up and captured, Cancelar keeps the profile, Apagar tudo opens O1, the 7 Room tables empty, memory and photos deleted, installation id kept, only the daily resync alarm left (3 alarms before), back from O1 leaves the app, relaunch opens O1. The A46 keyboard checks (`… above the keyboard`, in onboarding and Config) fail on this emulator with the keyboard already closed at measurement; A53 changes no onboarding, field or keyboard code; the run tolerated the onboarding step only for this evidence.
- Gold comparison (`node tools/diff-gold.mjs`, `cfgR` added to its centred-dialog regions): `cfg` 0.00% / 0.00%, `cfgR` dialog 0.00% / 0.10%, `wipe` 0.19% / 0.03%, `cfgS` 0.00% / 0.00% (dark / light, max 2%). Light `cfgS` ink 1.27 comes from the status bar (clock, icons), ignored by AGENTS; content identical. Diff list: layout, tokens, type size, radius, danger dialog and CTA match the D15 golds. Fresh captures `cfg`, `cfgR`, `cfgS`, `wipe` in `docs/qa/android/current/{dark,light}/`; onboarding captures not kept.
- Specs: [memoria-push](../../../produto/specifications/memoria-push.md) Config rule 11, acceptance and Provenance; [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) states and Provenance; QA README Config capture line.

Pending: owner check on a device (dev build): Config → Resetar app → Apagar tudo lands on an empty O1; no reminder fires afterwards; a new onboarding works.

### Owner validation

On 07/10/2026 the owner declared this plan's manual validation done and asked to conclude it (chat message: "Pode concluir todos os planos que estão pendentes de validação manual"). The agent did not run the device check itself; this records the owner's statement. Plan moved to `completed/`.
