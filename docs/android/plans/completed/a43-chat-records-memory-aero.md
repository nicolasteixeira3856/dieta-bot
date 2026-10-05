# Plan — A43 Chat records, photo and memory on Aero

- Status: Concluído
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/chat/` (receipt, photo bubble, attachment, photo source sheet, memory chips, routine card, meal plan), components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM` and `chatS` in `docs/qa/README.md`, captures.
- Prerequisites: [A42](a42-chat-core-aero.md) and [D6](../../../design/plans/completed/d6-release1-chat-records-memory.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a43-chat-records-memory-aero.md. Implemente o plano aprovado.`

## Objective

Move the remaining Chat states onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the eight golds of this flow. Captures:

- `tools/capture-photo.sh`;
- `tools/capture-replace.sh`;
- `tools/capture-chat.sh` with `SCENES=v2` and `SCENES=a34`.

Flow-specific work:

1. **Receipts** keep every action and rule of ADR-028 (Desfazer, Excluir, Trocar refeição, Editar, Registrar on doubt).
2. **Photo** keeps ADR-018 (JPEG q85, 2048 px, EXIF stripped, 16 MB guard).
3. **Gold conflicts:** the regional gates for `chatF` and `chatA` and the `GOLD_CONFLICTS` entries for `chatG`, `chatF` and `chatA` are removed once the new golds pass.
4. **Spec:** [chat](../../../produto/specifications/chat.md) and [memoria-push](../../../produto/specifications/memoria-push.md) change only their visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with 16 images.

## Results

Implemented 2026-10-04 after the owner's named approval. The consolidated phone check was approved by the owner on 04/10/2026.

### Delivered

1. **Receipts on Aero** (`ChatRecordComponents.kt`): `Chat/Receipt` on a surface/2 card 12 dp in from the thread edges (status/good well and kcal chip at 15 %, the restore chip on the title row, the skip in text/dim), the mark at the end of the title row, a marked receipt at 50 % through one save layer (ADR-027 rule 4); `Chat/ReceiptAction` as 44 dp glass rows (22 dp icon, `Button` label, Excluir in status/bad) stacked 16 dp under the latest receipt. Every ADR-028 action and rule is the same code (Desfazer, Excluir, Trocar refeição, Editar, Registrar on doubt).
2. **`chatU`:** `Chat/Receipt` State=ReplacePending, `Substituir {slot}?` as a full-width glass card with `Button/Primary` Substituir and the outlined Outra refeição pill.
3. **Photo** (`ChatPhoto.kt`): `Chat/Photo` tinted glass bubble (265 dp) with the photo, the glass `Visão Computacional` tag, caption, time and ticks; `chatA` thumbnail 64 dp with the ✕ on its corner. The photo source chooser (no gold) is `Sheet/Bottom` with `Sheet/PhotoSource` rows and Cancelar (`AeroSheet` takes `primary = null`). ADR-018 (JPEG q85, 2048 px, EXIF stripped, 16 MB guard) untouched.
4. **Plan, memory, routine** (`ChatV2Components.kt`): the plan text as one Body text with the macros in their colours and `Card/MealPlan` (status/bad above the ceiling); `Chip/Memory` (28 dp pill, accent check only on `Memória atualizada`, Phosphor icons), with the bubble time moved under the chips (`chatM`); `Card/Routine` glass card with `Registrar` + `Quase igual` (166 / 142 dp).
5. **Thread rhythm:** 16 dp between every item (8 dp after a question), thread bottom padding 32 dp, as the Figma frames.
6. **Gold sources:** `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS` are `figma`; the inventory has no Chat id left on Stitch. The Stitch exceptions are removed: `GOLD_CONFLICTS` keeps only `push`; the `chatA`/`chatS`/`chatM` regions, the receipt and photo regions and the report-only sets are gone from `tools/diff-gold.mjs` and `StitchGoldTest`; `AERO_PENDING` is removed.
7. **Specs:** [chat](../../../produto/specifications/chat.md) rules 17 and 19 (component names instead of raw sizes) and Provenance; [memoria-push](../../../produto/specifications/memoria-push.md) visual reference and Provenance; `docs/qa/README.md` gate notes.

### Figma MCP

3 read-only `use_figma` calls (budget 120): D6 chat record components (`Chat/Receipt`, `Chat/ReceiptAction`, `Chat/Photo`, `Chip/Memory`, `Card/Routine`, `Card/MealPlan`, `Sheet/PhotoSource`), the chatF / chatA / chatG / chatU / chatD frames, the chatR / chatM / chatS frames.

### Validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: 504 tests, 0 failures. One run under emulator load failed `ChatViewModelTest.skipByText_emptySlot_skipsWithReceipt_recordedSlotChangesNothing` (`not_recorded` read before the turn was stored, a timing flake; ViewModel not touched by this plan); the class passed 3 reruns in isolation and the full suite passed twice afterwards.
2. `./gradlew.bat :app:verifyRoborazziDevDebug`: pass. Baselines re-recorded only for this flow: `chatA`, `chatD`, `chatF`, `chatG`, `chatM`, `chatR`, `chatS`, `chatU` (both themes).
3. JVM gold (`StitchGoldTest`, Figma frame geometry: chatF 961 dp, chatD 942, chatG 902, chatM 925, chatR 882, chatA / chatS / chatU 844), dark / light: chatA 0.67 / 0.66 %, chatD 0.00 / 0.00 %, chatG 0.02 / 0.02 %, chatM 0.03 / 0.01 %, chatR 0.03 / 0.03 %, chatS 0.14 / 0.10 %, chatU 0.03 / 0.01 %; chatF by region (the photo box left out: the frame crops its sample photo differently from the app's centre crop) 0.00 + 1.15 / 0.00 + 1.29 % (whole screen 4.92 / 5.02 % reported).
4. Emulator (API 36, 780 × 1688, density 320), fake chat server + `tools/capture-photo.sh`, `tools/capture-replace.sh` and `tools/capture-chat.sh` (all scenes, v2 and a34 included) in both themes, `node tools/diff-gold.mjs`, 4 iterations:
   - iteration 1 (JVM): chatF, chatS, chatU above 2 %: receipt and routine gaps (22 / 24 dp) and the 88 % replace card;
   - iteration 2: gaps 16 dp, full-width replace card, time under the memory chips; JVM all pass;
   - iteration 3 (emulator light): the long threads (chatF, chatG, chatD, chatR, chatM, chatU) fail whole-screen (8–40 %) because the phone shows the newest part of a thread the frame draws whole; chatS seeds another day (meta 2.000, 100 %);
   - iteration 4: `diff-gold.mjs` long-thread rule (header gated from the top, the thread tail down to the composer from the bottom, screen reported), chatS gated on header + routine card, chatA bottom zone; thread bottom padding 32 dp.
   - Final, dark: chatA 0.76 %, chatF header 0.00 % + tail 0.00 %, chatG 0.00 + 0.46 %, chatD 0.00 + 0.00 %, chatR 0.00 + 0.03 %, chatM 0.00 + 0.02 %, chatU 0.00 + 0.65 % (screen 0.91 %), chatS header 0.00 % + card 0.05 %. Light: chatA 1.00 %, chatF 0.01 + 0.30 %, chatG 0.00 + 0.59 %, chatD 0.00 + 0.23 %, chatR 0.00 + 0.15 %, chatM 0.00 + 0.22 %, chatU 0.00 + 1.06 %, chatS 0.00 + 0.07 %. All 16 images ≤ 2 % on their gates. A42 ids re-checked on the same build: dark chat0 0.16 %, chatE 0.06 %, chatT 0.15 %, chatX 0.28 %, chatQ 0.00 + 0.35 %, chatL 0.00 %; light chat0 0.33 %, chatE 0.27 %, chatT 0.55 %, chatX 0.56 %, chatQ 0.00 + 1.30 %, chatL 0.03 %.
   - Diff list (none blocks): long threads scrolled to their newest part (whole screen 10–13 % reported, the frames draw the full thread); the chatF sample photo differs from the seeded one (excluded); chatS seeds another day's numbers (header and card gated); content 22–24 dp lower and the bottom stack 24 dp higher (status and navigation bars, absent from the frames); date and times from the device clock; blur grain inside glass not counted.
   - Interaction checks: every capture-photo, capture-replace and capture-chat check passed in both themes (A34 "Editar puts the text back in the composer" included). One light photo run failed after an interrupted capture left the app mid-flow; the rerun passed.
5. `./gradlew.bat :app:assembleDevRelease`: success; `app-dev-release.apk` 19,102,723 → 19,102,723 bytes (no measurable delta: page-aligned uncompressed dex).
6. `node tools/check-docs.mjs`: pass.

### Deviations

- Thread bottom padding 32 dp (was the A42 value), read from the frames.
- Plan § Scope 3 asked to remove the `chatF` / `chatA` regional gates: the Stitch ones are removed; the Figma `chatF` keeps a region gate (photo box out, both JVM and emulator), and the long threads use the new header + tail rule, for the reasons in the diff list.

### Manual validation

- Consolidated phone check of A39–A45 approved by the owner on 04/10/2026. The two input defects found in it (keyboard over the focused field, cursor at the start of an edited value) go to [A46](../pending_manual_validation/a46-input-cursor-keyboard.md).
