# A24 validation — meals by weekday

Plan: [A24](../plans/completed/a24-refeicoes-por-dia.md). Date: 2026-09-29. Status: automated implementation verified; owner + tester update pending.

## Automated checks

From `apps/android`: `./gradlew.bat :app:testDevDebugUnitTest :app:verifyRoborazziDevDebug :app:assembleDevRelease` — PASS, 245 tests, zero failures. Release packaging and lint vital passed. No dependency, version or server changes.

Coverage includes all seven weekday bits and Sao Paulo date boundaries, Saturday prompt filtering, Home daily timeline and orphan logs, Friday-to-Saturday alarm cancellation/rearm, staged Onboarding/Config edits, previous-group copy without duplicated IDs, discard/cancel, and history preservation on save. MigrationTestHelper creates a real SQLite v3 file from exported schema 3, applies the additive 3→4 migration and validates schema 4, defaults, foreign keys and retained values in all seven tables. Existing v1 migrations, Chat replacement, wipe, memory-file and photo tests also passed.

`node tools/check-stitch.mjs` — PASS, all 48 golds present. ST4 is completed. Imported golds remain unchanged. New grouped O3/Config gold renders pass the unchanged 2% gate. Only the two app-owned `cfgSlots` Roborazzi baselines were recorded after inspecting the approved replacement of the old sheet with a full-screen editor.

## In-place emulator upgrade

Device: `emulator-5554`, 780×1688 px, density 320. Installed the new devDebug APK with `adb -s emulator-5554 install -r` over the existing v3 devDebug 0.0.2 app. Before upgrade, the actual database contained four meals, a 380 kcal log, one skip, Chat and digest fixtures.

After upgrade: `user_version=4`; every pre-existing column and row of profile/day/meal_slot/meal_log/slot_skip/chat_message/day_digest is identical. All old meals have `days=127`, profile has `slotMode=same`. Before/after Home PNGs were visually identical except system indicators. The existing resync alarm retained epoch `1790737500000`. At the capture time all meal times had already passed, so no future meal alarm was expected. Ciphered long-term memory was not populated in this device upgrade fixture; memory-file tests passed, and the populated-memory App Tester update remains manual.

The v3/v4 SQLite files, old APK, upgrade PNGs, alarm dumps and Android CLI layout JSON stay in the task scratchpad, outside committed product data.

## Visual comparison

Fresh captures: `docs/qa/android/current/{dark,light}/{o3,o3s,cfg,cfgS}.png`. Real onboarding and Config journeys use explicit `ANDROID_SERIAL=emulator-5554`. The adb binary screenshot fallback is used by the existing scripts; Android CLI measures the layouts separately.

Gold exports are taller than the device. The existing comparator aligns the top content and handles the fixed CTA independently; lower cards scroll behind that CTA, as the screen and gold require. No threshold or report-only exception was added.

| Screen | Dark emulator diff | Light emulator diff |
| --- | --- | --- |
| o3 | 0.96% | 0.69% |
| o3s | 1.96% | 1.35% |
| cfg | 0.40% | 0.37% |
| cfgS | 0.65% | 0.39% |

Written diff list:

- Layout: 24 dp horizontal inset, two-row mode chips, same O3 header, group label/stage segments and copy action; Config has one count/time-range row per group. Full-screen Config editing reuses O3 with the Config header. Full-page export offsets and lower-card clipping are recorded, not confused with identical image heights.
- Tokens: system dark/light palette, no dynamic color; selected mode uses the gold CTA contrast, gold remains an accent on clocks/progress/copy.
- Typography: gold-specific mode sizes (dark 14 sp, light 12 sp), 20 sp group label, 13 sp stage; existing meal fields and titles retained. Remaining stays 34 sp, unaffected.
- Radius: 14 dp mode/card/copy controls; existing sheet 22 dp top corners remain on unchanged sheets.
- ButtonGroup: existing 2–6 meal quantity control retained, correct selected 4 for o3 and 3 for o3s.
- CTA: bottom-fixed Continuar / final Config Salvar with theme-specific contrast. Back and staged advancement use the shared ViewModel draft.
- Timeline: active-day meals only; logs outside those meals remain in Outros and calorie totals. No visual timeline redesign.
- Semantic macros: protein/carbs/fat palette and consolidated log text retained; upgrade Home retained `380 kcal · 22P · 4C · 16G`.

## Device behavior

PASS: the device clock was set to Saturday 2026-10-03 at 12:00 America/Sao_Paulo using Android's alarm service. Home showed three meals with the weekend times. Push preferences contained only the future weekend IDs (13:30 and 20:30); 09:30 had already passed. A stored assistant estimate suggested the weekend breakfast, Gravar used it, and Trocar exposed exactly the three weekend IDs/times with 09:30 marked current. The device epoch and automatic-time setting were restored in a finally block, followed by app restart/rearm. A legacy notification from the separately installed `com.nutri.android` package was identified and excluded from dev push evidence.

Android CLI `layout --device=emulator-5554 --flat --full` measured these screen-pixel rectangles in both themes. Chip/copy touch semantics expand to the 48 dp minimum hit target; their visible fills remain the 40/44 dp gold sizes. O3 capture content aligns at −16 dp; grouped O3 at −15 dp. Config offsets: dark cfg −18 dp / cfgS −17 dp; light cfg −16 dp / cfgS +5 dp (footer +8 dp). These are the comparator's existing system/full-export offsets, not additional tolerance. Later cards are scrollable and remain behind the bottom-fixed CTA.

| Element | Dark bounds | Light bounds |
| --- | --- | --- |
| o3 / o3-mode-same | `[48,596][288,692]` | `[48,596][282,692]` |
| o3 / o3-mode-split | `[304,596][628,692]` | `[298,596][604,692]` |
| o3 / o3-mode-each | `[48,692][218,788]` | `[48,692][218,788]` |
| o3 / o3-time-0 | `[478,1054][702,1150]` | `[478,1054][702,1150]` |
| o3 / o3-continue | `[48,1472][732,1584]` | `[48,1472][732,1584]` |
| o3s / o3-group | `[48,826][255,876]` | `[48,830][255,880]` |
| o3s / o3-copy | `[48,920][732,1016]` | `[48,924][732,1020]` |
| o3s / o3-time-0 | `[480,1284][704,1380]` | `[480,1288][704,1384]` |
| o3s / o3-continue | `[48,1472][732,1584]` | `[48,1472][732,1584]` |
| cfgS / cfg-group-0 | `[50,787][730,927]` | `[50,787][730,927]` |
| cfgS / cfg-group-1 | `[50,929][730,1069]` | `[50,929][730,1069]` |
| cfg-editor / cfg-group | `[48,764][255,814]` | `[48,768][255,818]` |
| cfg-editor / cfg-copy | `[48,858][732,954]` | `[48,862][732,958]` |
| cfg-editor / cfg-save | `[48,1472][732,1584]` | `[48,1472][732,1584]` |

The Config journey also checks rename without wipe, workout 400/empty persistence, ceiling wipe cancel/confirm, preserved Chat and a weekend full-screen editor save. An initial capture attempt was invalidated by an additional device; subsequent runs explicitly selected emulator-5554. A dark rerun reached every functional check but a script edit during execution invalidated its exit status; the stable light run passed every check and exited 0. All eight inspected PNGs pass their gold gate. All committed captures come from that device.

## Manual validation pending

Owner + one tester must update the signed dev release through Firebase App Tester without losing their existing meals, logs, skips, Chat or populated memory, then enable Seg–Sex · Sáb–Dom and confirm weekend behavior. No App Distribution upload or version bump was requested in this implementation turn. Keep the plan in `pending_manual_validation` until that result is recorded.
