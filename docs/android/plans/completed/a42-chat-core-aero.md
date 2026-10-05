# Plan — A42 Chat core on Aero

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/chat/` (screen, header, composer, estimate, question, loader, slot sheet, skip dialog), chat components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP` and `chatX` in `docs/qa/README.md`, captures.
- Prerequisites: [A41](a41-splash-onboarding-aero.md) and [D5](../../../design/plans/completed/d5-release1-chat-core.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a42-chat-core-aero.md. Implemente o plano aprovado.`

## Objective

Move the core Chat states onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the seven golds of this flow (`node tools/fake-chat-server.mjs` + `tools/capture-chat.sh`). Flow-specific work:

1. **`AeroLoader`** replaces the M3 `LoadingIndicator` in the estimate/fit loading.
2. **`AeroComposer`:** keep the 2000-character limit and `chatX` error (ADR-022), photo entry and send.
3. **Gold conflicts:** the conflicts listed in `docs/qa/README.md` (`GOLD_CONFLICTS`) for `chat0`, `chatL` and `chatE` end when these ids switch to Figma, because D5 draws them from one canonical component tree. The entries are removed in this delivery once the new golds pass.
4. **Spec:** [chat](../../../produto/specifications/chat.md) changes only its visual references.

## Out of scope

- `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM` and `chatS` ([A43](a43-chat-records-memory-aero.md)). The rest as A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with the chat captures and 14 images.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Chat frame on Aero** (`ChatScreen.kt`, route wrapped in `AeroTheme`): page gradient, `Chat/Header` (glass back button, title with the accent dot, `ASSISTENTE DE REFEIÇÕES`, the 44 dp space of the dropped tune button), the thread at 20 dp margins with 16 dp gaps, `Chip/Date`, the frame's edge bubbles, the footer 24 dp above the bottom without the old gradient layer.
2. **Bubbles:** user `Chat/Bubble` (tinted glass, max 288 dp, time and ticks inside), bot glass bubble (308 dp, radius 20 / 6, time inside), `Chat/BotLabel`, `Chat/Question` (accent bar, question icon, Body/Strong, time inside), `Chat/Estimate` (ENERGIA TOTAL, glass macro boxes in the semantic colours), the greeting bubble and `Chat/MetaCard`, the suggestion chips as `Chip/Log` Neutral, the failure bubble and the notice (no gold) in Aero.
3. **`AeroLoader`** (Loader/Aero: border/line track, 225° accent arc, turning) replaces the M3 `LoadingIndicator` in the estimate loading (`chatL`).
4. **Composer:** glass pill, 20 dp card past one line or with a photo, `TooLong` with the status/bad border, muted camera and send and "Texto muito longo" (ADR-022: the 2000-character limit, photo entry and send unchanged; the text-field sync, focus and keyboard behaviour are the same code).
5. **Actions slot:** `Chat/ActionBar` for Registrar (`chatE`), Forçar estimativa (`chatQ`) and Registrar assim (same component; `chatR` stays in A43's gold).
6. **`chatT`:** `AeroSheet` "Selecione a refeição" with `Sheet/SlotList` and `Row/SlotPick` (selected: tinted with the accent border, filled well and check; rows sit on the sheet's glass); primary `Confirmar refeição` with the arrow, secondary `Cancelar` pill.
7. **`chatP`:** the Home skip confirmation is `Dialog/Confirm` drawn in the screen over the blurred Home and `overlay/scrim` (`AeroConfirmDialog`); the Material `BasicAlertDialog` is gone from the Home.
8. **New Aero components** (`AeroChatComponents.kt`): `AeroBotLabel`, `AeroDateChip`, `AeroActionBar`, `AeroLoader`, `AeroSlotPickRow`, `AeroConfirmDialog`, `AeroSecondaryPill`; `AeroSheet` gained a primary icon; `AeroFieldNumber` and the slot rows take the glass fill alone on a glass parent.
9. **Gold sources:** `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP`, `chatX` are `figma`. Their Stitch exceptions are removed from `tools/diff-gold.mjs` and `StitchGoldTest` (`chat0`/`chatL`/`chatX` conflicts, the chatQ/chatX regions, the light chatQ/chatX report-only entries), together with the now-unused Stitch home/o3t entries.
10. **Migration window:** `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS` share the Chat screen but keep their Stitch golds until A43. `StitchGoldTest` measures and prints them without asserting (`AERO_PENDING`, emptied by A43); their emulator diffs are A43's gate.
11. **Tooling** (outside the plan's file list, needed for the gate): `tools/diff-gold.mjs` treats a Figma frame of the phone height (844 dp) as a phone screen (content from the top, the bottom stack from the bottom), with per-frame bottom zones for the Chat (the frame's bottom stack + the 48 dp the bars take); `chatL` and `chatQ` are reported and gated by region (the chatL scene sends a chip, since adb types no accents; the chatQ thread is taller than the phone's room); `chatP` is gated on the dialog box centred on the capture. `tools/capture-home.sh` captures `chatP` (tap the Lanche card of the empty day, then Cancelar).
12. **Docs:** [chat](../../../produto/specifications/chat.md) visual references (layout source, question bubble look) and Provenance; `docs/qa/README.md` gate notes.

### Figma MCP

4 read-only `use_figma` calls (budget 120): D5 chat components (with `Row/SlotPick`, `Sheet/SlotList`, `Dialog/Confirm`), the chat0 / chatQ / chatE frames, the chatL / chatX / chatT / chatP frames. Values not read from the PNG.

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 504 tests, 0 failures. `./gradlew.bat :app:verifyRoborazziDevDebug`: pass. Baselines re-recorded: `chatE`, `chatQ` (this flow), `homeW` (the field on the sheet's glass, item 8), and `chatA`, `chatD`, `chatF`, `chatG`, `chatM`, `chatR`, `chatS`, `chatU` (same Chat screen; their look is finished by A43).
2. JVM gold (`StitchGoldTest`, Figma frame geometry), dark / light: chat0 0.13 / 0.08 %, chatL 0.00 / 0.00 %, chatQ 0.02 / 0.02 %, chatE 0.02 / 0.03 %, chatT 0.00 / 0.00 %, chatP 1.61 / 1.66 %, chatX 0.10 / 0.08 %. Home unchanged in substance (0.00–0.15 %).
3. Emulator (API 36, 780 × 1688, density 320), fake chat server + `tools/capture-chat.sh dark|light` and `tools/capture-home.sh dark|light` (chatP), `node tools/diff-gold.mjs`, 3 iterations:
   - iteration 1 (light): chat0 0.67 % passed; chatL 8.18 %, chatQ 3.25 %, chatE 3.48 %, chatT 4.65 %, chatX 5.86 % failed. Causes, none in the layout: the gate aligned every Figma frame by the top only, so the bottom-anchored actions + composer were compared 48 dp off (the phone's bars); the chatL scene's message differs from the frame's; the clipped `PROTEÍNA` label in the estimate boxes (fixed: box padding 4, no clip);
   - iteration 2: phone-geometry rule and per-frame bottom zones in `diff-gold.mjs`, chatL/chatQ by region; the slot rows and the workout field take the glass fill alone on the sheet;
   - iteration 3: dark chat0 0.16 %, chatL loading region 0.00 % (screen 2.07 % reported), chatQ header 0.00 % + thread 0.05 % (screen 1.12 % reported), chatE 0.06 %, chatT 0.15 %, chatX 0.25 %, chatP dialog 0.00 %; light chat0 0.33 %, chatL region 0.03 % (screen 7.21 % reported), chatQ 0.00 % + 0.33 % (screen 2.54 % reported), chatE 0.27 %, chatT 0.55 %, chatX 0.35 %, chatP dialog 0.00 %. All 14 gates ≤ 2 %. Home re-checked: dark 0.83 / 1.03 / 0.82 %, homeW 0.09 + 0.22 %; light 0.62 / 0.83 / 0.43 %, homeW 1.14 + 0.32 %.
   - Diff list (none blocks): content 22–24 dp lower and the bottom stack 24 dp higher (status and navigation bars, absent from the frames); date and times from the device clock ("Hoje, 4 de outubro", 13:2x) instead of the frames' samples; chatL sends a chip ("Café com 2 ovos mexidos", one line); chatT marks `(atual)` on the scene's slot; the chatQ thread scrolls 12 dp on the phone; blur grain inside glass is not counted.
   - Interaction checks: every capture-chat and capture-home check passed in light; in dark, one A34 check ("Editar puts the text back in the composer", A43's scene) failed once (`composer not filled`) while the same step passed in light; re-checked with A43.
4. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,102,723 → 19,102,723 bytes (no measurable delta: page-aligned uncompressed dex).
5. `node tools/check-docs.mjs`: pass.

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](a46-input-cursor-keyboard.md).
