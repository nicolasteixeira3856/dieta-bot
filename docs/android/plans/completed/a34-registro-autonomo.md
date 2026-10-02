# Plan — A34 Autonomous record, receipts with actions

- Status: Concluído (02/10/2026)
- Approval: 02/10/2026 (owner: "Aprovo o plano docs/android/plans/a34-registro-autonomo.md. Implemente o plano aprovado.")
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`core/network/ChatModels.kt`, `core/database/*` (Room v8), `core/memory/*`, `feature/chat/*`, `core/telemetry` events, tests, QA tools and captures)
- Prerequisites: **[S14](../../../server/plans/completed/s14-registro-autonomo.md) deployed on the dev server** and **[ST9](../../../stitch/plans/completed/st9-registro-autonomo.md) in `stitch/plans/completed/`** (golds `chatE`, `chatF`, `chatG` edited; `chatU`, `chatD` new). Executes [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md) decisions 1–9; accepts ADR-028 on completion.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a34-registro-autonomo.md`. Implemente o plano aprovado.

**First implementation step:** confirm `docs/stitch/plans/completed/st9-registro-autonomo.md`, the five golds in `docs/qa/stitch/{dark,light}/`, and S14 in `docs/server/plans/completed/` (or `pending_manual_validation/`) with the dev server answering `record`. Otherwise stop and tell the owner.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

A clear meal of today sent to the Chat is recorded by itself and leaves a receipt. The latest receipt of each slot carries Desfazer / Excluir / Trocar refeição / Editar, as ADR-028 decision 5. An unsure meal offers one Registrar button. A slot that already has a record asks inside the conversation before replacing it. "Pulei o café" skips by itself. Every action is measured in dev telemetry.

## Sources of truth

- Golds `chatE`, `chatF`, `chatG`, `chatU`, `chatD` (dark and light) from ST9; `chatT` (Trocar sheet), `chatQ`, `chatR`, `chatS`, `chatM` unchanged.
- [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-027](../../adrs/ADR-027-golds-divergentes.md), [chat](../../../produto/specifications/chat.md), [v1-chat](../../../server/specifications/v1-chat.md), [room-v2](../../specifications/room-v2.md).
- `AGENTS.md` tokens; skills `dieta-bot-android-ui`, `material3-expressive`, `room-ksp-coroutines`, `kotlin-clean`, `compose-stability`, `dieta-bot-android-visual`, `screenshot-testing`.

## Implementation scope

### 1. Wire

- `ChatIn.autoRecord: Boolean` sent as `true` on every request (no default, like `clarifyRounds` in A30, so it is always encoded).
- `ChatOut.record: String? = null` (`auto` | `ask` | `none`), `ChatOut.skipSlot: String? = null`, `intent` accepts `skip`.

### 2. Room v8 (one migration, `room-v2` spec updated)

`chat_message` gains, all nullable or defaulted:

- `recordMode` (assistant rows): `auto` | `ask` | `none`, stored from `ChatOut.record`. An answer from a server without `record` and with a recordable `log` estimate is stored as `ask`. Null = row from before A34: no actions at all (ADR-028 decision 5, last bullet).
- `recordState` (assistant rows): `recorded` | `pending_replace` | `not_recorded` | null.
- `receiptState` (receipt rows): `undone` | `deleted` | `moved` | `edited` | null (active).
- `undoData` (receipt rows): JSON with what the receipt changed, enough to reverse it: per touched slot the record before (text, kcal, P/C/G, source, or "none"/"skipped") and after; and the memory pre/post images of every fact the receipt changed (`{id, before: fact|null, after: fact|null}`).
- `recordSource` (receipt rows): `user` | `photo` | `plan` | `routine` (drives Editar).
- New receipt roles next to `logged`, `replaced`, `skipped`: `moved`, `restored`. Receipts still never go to the server.

Migration test v7 → v8 with existing rows (old receipts get no actions).

### 3. Automatic record after a turn

After an answer is stored (`ChatViewModel`), for `recordMode = auto`:

- `intent = log` with a released estimate: client guards — suggested slot is one of today's slots, no question-only state pending, the estimate is recordable. A guard failure downgrades to `ask`.
  - Slot empty → same path as today's Gravar (`addLog`, routine applied, `logged` receipt with `undoData`), `recordState = recorded`.
  - Slot with a record → `recordState = pending_replace`; the inline confirmation is shown (§ 5). Nothing changes yet.
  - Slot skipped → the record replaces the skip with no question (a skip has no numbers to lose); receipt `logged` whose `undoData` restores the skip.
- `intent = skip` with `skipSlot` of today: slot empty → skip + `skipped` receipt. Slot already recorded or skipped → nothing changes, `recordState = not_recorded`.
- One automatic action per answer. Retry of a failed send behaves as a new send.

`ask`: the actions slot shows the single **Registrar** pill (gold `chatE`) for the latest `ask` estimate of today. Tap → the same path as automatic (empty slot records; occupied slot opens the inline confirmation). Registrar disappears on the next send; that estimate gets `recordState = not_recorded`.

Plan (**Registrar assim**) and the routine card (**Registrar**) are unchanged, except that their receipts now carry `undoData` and the actions of § 4.

### 4. Receipt actions

- A receipt shows actions only when it is active (`receiptState` null) and it is the latest receipt of every slot it touched (pure function over today's and older rows, unit-tested). Any day.
- Layout as golds `chatG` (three actions), `chatF` (photo: no Editar) and `chatD` (restored): vertical stack, 8 dp gap, 44 dp buttons, radius 14, surface + 1 dp line, icon left, 15 sp semibold label; Excluir in `bad`.
- Set per receipt as the table in ADR-028 decision 5 (Desfazer on `replaced`, `moved` and `skipped`; Editar never on `photo`).
- **Excluir**: in one transaction, delete the slot's record, revert the receipt's memory (§ 6), set `receiptState = deleted`. Receipt shows the mark `Excluído`. No confirmation.
- **Desfazer**: in one transaction, restore every touched slot to its "before" from `undoData` (record back, record moved back, skip removed), revert memory, set `receiptState = undone`. If a slot ends with a record, insert a `restored` receipt (`Restaurado em {slot} · {hora}`, `{kcal} kcal`) with its own `undoData`, which becomes the active one. Mark `Desfeito`.
- **Trocar refeição**: opens the existing Trocar sheet (`chatT`) with the current slot marked "(atual)". Confirm on an empty slot → move the record (memory: revert this receipt's change, apply the estimate's pending routine to the new slot), receipt `moved` (`Movido para {slot} · {hora}`, `{kcal} kcal`), old receipt `moved`. Confirm on an occupied slot → the inline confirmation of § 5 below the receipt; Substituir moves and replaces in one transaction, `undoData` holds both slots.
- **Editar**: delete the record and revert memory as Excluir, mark `Removido para editar`, put the record's text in the composer (replacing any draft), focus, keyboard open, cursor at the end.
- Marks: muted label with icon at the end of the receipt title row, receipt at 50% opacity (gold `chatD`).

### 5. Inline replace confirmation (`chatU`)

- Shown below the AI bubble (or below the receipt, for Trocar refeição) while `recordState = pending_replace`: `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** (CTA tokens) | **Outra refeição** (outlined).
- Substituir → today's replace transaction (ADR-017), `replaced` receipt (`Atualizado em {slot} · {hora}`, chip `{antigo} → {novo} kcal`) with `undoData`, `recordState = recorded`.
- Outra refeição → Trocar sheet with nothing picked; the chosen empty slot records, an occupied one asks again.
- Expiry: the next send, the day change, or the slot changing by another path → `recordState = not_recorded`, card replaced by the mark `Não registrado`. The `chatP` dialog is no longer used by the Chat (Home keeps it).

### 6. Memory revert (`core/memory`)

- `memory.apply` / `reinforceRoutine` return the pre and post image of each fact they touched; the receipt stores them in `undoData`.
- `memory.revert(images)`: for each fact, restore `before` only if the current fact equals `after` (an added fact is removed, a removed fact comes back, a changed fact gets its old text/counters). A fact changed since is left as is. Atomic write as today (A8b). The reverting receipt does not show `Memória atualizada` again; telemetry counts reverted and skipped facts.

### 7. Removed from the Chat

- The Gravar | Trocar | Pular action bar and the Chat skip dialog. Old estimates without `recordMode` show no actions.
- `chat` spec rules 4–7 behaviour that depends on them (replaced by § 3–5).

### 8. Dev telemetry (enums and numbers only, ADR-015)

- `meal_auto_recorded {kind: log|skip, slot_state: empty|skipped, source: user|photo, rounds}`.
- `record_ask {action: shown|tapped|expired}`.
- `replace_confirm {action: shown|confirmed|elsewhere|expired, from: answer|move}`.
- `receipt_action {action: undo|delete|move|edit, receipt: logged|replaced|skipped|moved|restored, source: user|photo|plan|routine, age_s, same_day}`.
- `memory_reverted {reverted, kept}` (counts).
- `record_guard {reason: slot_not_today|no_slot|question_pending|not_recordable}` when `auto` is downgraded.
- `chat_result` gains `record` (`auto` | `ask` | `none` | `missing`).
- Firebase Analytics registration in the dev flavor only; prod stays `NoopTelemetry` (ADR-014).

### 9. Docs in the same delivery

- [chat](../../../produto/specifications/chat.md): rewrite "Contexto e objetivo", rules 4–7, 12, 16–18 (receipts and actions), "Fora de escopo", states list (`chatU`, `chatD`), acceptance criteria; remove the ADR-028 proposal overlay.
- [room-v2](../../specifications/room-v2.md) v8; `AGENTS.md` Chat screen list gains `chatU`, `chatD`; ADR-028 status → Accepted; produto and android READMEs and matrix.

## Affected files and areas

- `apps/android/app/src/main/java/com/nutri/android/core/network/ChatModels.kt`
- `apps/android/app/src/main/java/com/nutri/android/core/database/` (`ChatMessageEntity.kt`, `ChatMessageDao.kt`, `DayRepository.kt`, `MealLogDao.kt`, `SlotSkipDao.kt`, `DietaBotDatabase.kt`, `Migrations.kt`) and the exported schema `8.json`.
- `apps/android/app/src/main/java/com/nutri/android/core/memory/` (`FactMemory.kt`), `domain/MemoryRules.kt`.
- `apps/android/app/src/main/java/com/nutri/android/feature/chat/` (`ChatViewModel.kt`, `ChatUiState.kt`, `ChatScreen.kt`, `ChatV2Components.kt`, `PromptBuilder.kt`).
- `apps/android/app/src/main/java/com/nutri/android/core/telemetry/` event constants.
- Unit tests, Roborazzi baselines, `StitchGoldTest`, `tools/fake-chat-server.mjs` (`record`, `skip_slot`), `tools/capture-chat.sh` (`SCENES=a34`), `docs/qa/android/current/{dark,light}/` for `chatE`, `chatF`, `chatG`, `chatU`, `chatD`.

## Planned validation

1. `./gradlew.bat :app:testDevDebugUnitTest`: wire encoding (`"auto_record":true`), Room v7 → v8 migration, auto record on an empty slot, `ask` → Registrar → record, guards downgrading to `ask`, pending replace → Substituir / Outra refeição / expiry on send and on day change, skip by text (empty, recorded, skipped), active-receipt function (latest per slot, moves touching two slots, other days), each action per receipt kind with its `undoData` round trip, Desfazer of a replacement producing a `restored` receipt, Editar filling the composer and absent on photo, memory revert (fact unchanged → restored; changed since → kept; added → removed), old rows without actions, telemetry maps.
2. `./gradlew.bat :app:verifyRoborazziDevDebug` with new/updated baselines for the five golds.
3. Emulator captures `chatE`, `chatF`, `chatG`, `chatU`, `chatD` in both themes vs the ST9 golds; written diff list; iterate until they match (AGENTS Visual QA, ADR-027).
4. `./gradlew.bat :app:assembleDevRelease`.
5. Manual on the dev server with the owner's phrases: "Na janta comi…", "Lanche da tarde: 200g de…", "Registra aí, comi…" → recorded with receipt; "pudim de leite com calda" → Registrar; "também comi um pudim" with dinner recorded → inline confirmation; Desfazer → Restaurado; Excluir; Trocar refeição to an empty and to an occupied slot; Editar; photo with no Editar; "pulei o café"; "ontem jantei pizza" → nothing recorded. Telemetry events seen in Firebase DebugView.

## Out of scope

- Server changes (S14), Stitch (ST9).
- Retroactive recording ([A35](../out_of_scope/a35-registro-retroativo.md)).
- Home timeline edit/delete actions; the Home skip dialog (`chatP`) stays.
- A settings toggle for automatic recording.
- Redo after Desfazer/Excluir.

## Risks and controls

- **Wrong automatic record:** visible receipt with one-tap actions; `ask` default when unsure; undo rate in telemetry.
- **Undo restores a stale state:** actions only on the latest receipt of every touched slot; Desfazer reverses only what `undoData` says and the transaction checks that the slot still matches "after" (otherwise it aborts and hides the actions).
- **Memory revert erases a newer fact:** revert only when the fact still equals the receipt's post image.
- **Server not deployed:** `record` missing → stored as `ask` for a recordable `log`, so nothing is recorded without a tap; the prerequisite check prevents shipping ahead.
- **Migration:** additive nullable columns; migration test with real v7 rows.

## Acceptance criteria

- A clear meal of today is recorded without a tap and shows a receipt matching `chatG` (`chatF` for a photo).
- An unsure meal shows only Registrar, matching `chatE`.
- A meal for a slot already recorded shows the inline confirmation matching `chatU`; nothing changes until Substituir.
- Desfazer on a replacement restores the previous record and shows the receipts of `chatD`.
- Excluir, Trocar refeição and Editar act only from the latest receipt of the slot and revert the memory change of that record.
- "pulei o café" skips the slot with a receipt; another day is never recorded.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../../sdd/README.md#fora-de-escopo).

## Results (02/10/2026)

Approval: 02/10/2026 (owner: "Aprovo o plano docs/android/plans/a34-registro-autonomo.md. Implemente o plano aprovado.").

Prerequisites confirmed before any code: ST9 in `stitch/plans/completed/`; the five golds (`chatE`, `chatF`, `chatG`, `chatU`, `chatD`) in `docs/qa/stitch/{dark,light}/`; S14 `Concluído`; a real request to the dev server answered `intent: skip`, `record: auto`, `skip_slot: "1"`.

Implemented:

- Wire: `ChatIn.autoRecord` has no default (always encoded, like `clarify_rounds`). `ChatOut` gains `record` and `skip_slot`, and `intent` accepts `skip`.
- Room v8 (`MIGRATION_7_8`, schema `8.json`): `recordMode`, `recordState`, `receiptState`, `undoData`, `recordSource`, plus the new receipt roles `moved` and `restored`. `DayRepository.commitRecord` is the one transaction behind every record action. It checks each touched slot against its expected state, then writes the slots, the receipt marks, the record states and the new receipts. On a mismatch it writes nothing.
- Domain (`domain/RecordUndo.kt`): `SlotRecord`, `SlotState`, `SlotChange`, `FactImage`, `UndoData` and `RoutineUpdate`. `ReceiptRules.latest` (latest receipt per touched `(date, slot)`) and `ReceiptRules.actions` (the ADR-028 table) are pure.
- Memory: `FactMemory.revertAndApply` reverts images, then applies updates in one locked write. Images are measured after expiry, so an expired fact never comes back. `MemoryRules.revert` restores a fact only while it still equals its post image.
- `ChatRecorder` (new) owns record, skip, delete/edit, undo and move. `ChatViewModel` handles:
  - the record mode and its guards, with `record_guard` telemetry;
  - one automatic action per answer;
  - Registrar;
  - the inline Substituir / Outra refeição;
  - expiry on the next send, on the day change, and when the slot changes by another path (re-checked in Room before expiring);
  - the receipt actions and their telemetry.

  The Gravar | Trocar | Pular bar, the Chat skip dialog and the Chat `chatP` dialog are gone.
- UI (`ChatRecordComponents.kt`, new): the receipt with its mark and stacked actions, the inline `ReplaceCard` (`chatU`), `Não registrado` and the Registrar pill (`chatE`). The estimate card drops its bottom gap when there is no slot question.
- Thread fix found on the emulator: an answer can now land together with its receipt or its Substituir. The thread treated "at the bottom" as `firstVisibleItemIndex <= 1` and stopped following. It now counts the items added in front of the previous newest one (`ChatThreadTest.twoNewItemsAtOnce_followFromTheBottom` fails without the fix).
- Telemetry (dev only, enums and numbers): `meal_auto_recorded`, `record_ask`, `replace_confirm`, `receipt_action`, `memory_reverted`, `record_guard`, and `record` on `chat_result`. `FirebaseTelemetry` logs any event name, so nothing extra had to be registered.
- QA tools:
  - `fake-chat-server.mjs`: `{"record"}`, `{"skip"}`, `{"reply"}`, and `autoRecord` in `/__calls`.
  - `capture-chat.sh`: old scenes moved to Registrar, plus a new `SCENES=a34`.
  - `capture-photo.sh`: the chatF seed is the ST9 state.
  - `capture-replace.sh`: the inline flow, no capture.
  - `diff-gold.mjs` and `StitchGoldTest`: per-gold receipt and actions boxes, the chatM thread region, the chatF photo bubble.
  - The stale `chatP` / `chatReplace` captures and the `chatReplace` baselines were removed.

Validation:

1. `:app:testDevDebugUnitTest`: **439** tests, 0 failures. ✅ New or rewritten coverage:
   - wire `"auto_record":true`, and `record` / `skip_slot` read;
   - migration v7 → v8 with real rows (all new columns null);
   - `commitRecord` all-or-nothing;
   - `ReceiptRules` (latest per slot, a move touching two slots, other days, the action table, `UndoData` round trip);
   - memory images and revert (unchanged fact restored, fact changed since kept, add removed, remove brought back).
   - Chat ViewModel:
     - auto record into an empty slot, and into a skipped slot (Desfazer brings the skip back);
     - auto into a taken slot asks inline;
     - guards downgrade to `ask`; a server without `record` gives `ask`;
     - Registrar records, and the next send expires the one left;
     - pending replace: Substituir, Outra refeição (to an empty slot, to a taken slot asks again), expiry on the next send, on another path and on the day change;
     - skip by text (empty slot; recorded slot gets `Não registrado`);
     - another day is never recorded.
   - Receipt actions:
     - Excluir;
     - Desfazer of a replacement gives `Restaurado`;
     - Trocar refeição to an empty slot, and to a taken slot with Substituir; Desfazer of both;
     - Editar fills the composer and focuses;
     - a photo record has no Editar;
     - actions only on the latest receipt and hidden after another path; yesterday's receipt keeps its actions;
     - old rows have no actions;
     - Excluir reverts the routine; the routine card's record has actions.
   - Compose: the record controls (`ChatRecordUiTest`) and thread follow (`ChatThreadTest`).
   - Telemetry maps.
2. `:app:verifyRoborazziDevDebug` green. New baselines: `chatG`, `chatF`, `chatU`, `chatD`. Updated: `chatE`, `chatM` (Registrar instead of the bar). `chatReplace` removed. ✅
3. Gold comparison (blurred, 2 % gate). ✅

   JVM `StitchGoldTest`:
   - chatU 1.77 % / 1.84 %; chatE 1.84 % / 1.60 %; chatT 1.27 % / 1.74 %.
   - Receipt / actions: chatG dark 0.94 / 0.16 %, chatD dark 0.27 / 0.16 %; light actions chatG 0.15 %, chatD 0.14 %, chatF 0.86 %.
   - chatF light photo bubble 1.59 %; chatM thread region 1.41 % / 1.43 %.

   Emulator, `capture-chat.sh` dark and light (all scenes, `SCENES=a34` included), `capture-photo.sh`, `capture-replace.sh`: every flow check ✓. Two light failures were rerun:
   - The A32 follow check now looks at the new reply (a taller answer plus Registrar push the user's line just above the view).
   - adb dropped two characters from the typed text ("copode"); the rerun passed.

   `node tools/diff-gold.mjs`:
   - chatU 1.24 % / 1.63 %; chatE 1.69 % / 1.34 %; chatT 1.39 % / 1.82 %; chatR 1.72 % / 1.97 %; chatQ 1.21 %.
   - Receipt / actions: chatG dark 0.91 / 0.18 %, chatD dark 0.28 / 0.17 %; light actions chatG 0.18 %, chatD 0.14 %, chatF 0.87 %.
   - chatM thread 1.60 % / 1.47 %.
   - `dark/chatX` composer region 4.56 % is pre-existing: master's capture scores 4.77 %. It is not touched by A34.
4. `:app:assembleDevRelease` ✅
5. Dev server (gpt-6-luna), dev release APK on the emulator, the owner's phrases:
   - "Na janta comi 4 colheres de arroz branco, 1 concha de feijão carioca e 150 g de peito de frango grelhado sem óleo" → recorded with no tap: `Registrado em Jantar · 20:00 +470 kcal`, Excluir · Trocar refeição · Editar.
   - "também comi um pudim de leite no jantar" → `Substituir Jantar?` / `Jantar tem 470 kcal. Fica com 770 kcal.` → Substituir → `Atualizado 470 → 770 kcal` → Desfazer → `Desfeito` + `Restaurado em Jantar · 20:00 470 kcal` with the actions.
   - Trocar refeição → `Movido para Lanche · 16:00` → Editar → `Removido para editar`, Home at 0 kcal.
   - "pudim de leite com calda" → Registrar only, then `Não registrado` after the next send.
   - "pulei o café hoje" → `Pulado Café · 07:30` with Desfazer.
   - "ontem jantei pizza, 3 fatias" → a question first, nothing recorded (the S14 note).
   - A shorter dinner phrase got a question first (ADR-026), as expected.

   ✅ Owner, 02/10/2026: tested dev 0.0.8 (Firebase App Tester) on their phone. Owner: "Já testei aqui e está funcional, pode completar o plano." Plan → `Concluído`.

Diff list (ST9 golds vs app, written per AGENTS Visual QA):

- `chatG`, `chatD`, `chatF`: receipt 335 dp wide (thread content minus 12 dp per side) and 65 dp tall (46 dp for a restore, chip on the title row).
  - Plus Jakarta Sans as in the golds: title 13.5 sp W500 with the slot W700, time `muted` after a `dim` dot, chip 11.5 sp W600 on `good` 15 %.
  - Icon circle 24 dp, then 10 dp to the text.
  - Buttons 44 dp, radius 14, `surf` + 1 dp line, gap 8 dp, icon 22 dp (filled delete and pencil as in the golds), label Jakarta 15 sp semibold (plan value; the gold renders it near 17 sp, ADR-027 rule 3); Excluir in `bad`.
  - Receipt fill `surf2` above the `surf` buttons, as the dark golds.
  - Mark at the end of the title row, 50 % save layer (ADR-027 rule 4).
- `chatU`: card at bubble width, radius 14, `surf` + line, title 16 sp W700, body 14 sp `muted`, two 44 dp pills (CTA tokens | outlined), both 14 sp bold (gold sizes; the plan sets none). Inner spacing is the average of the dark and light golds (ADR-027 rule 2).
- `chatE`: one full-width 44 dp Registrar pill, `card` + line, check-circle and label in `text`. The dark gold tints the icon `good`, light uses `text`: one colour kept, gold stays an accent.
- Gold conflicts, reported and not gated:
  - Receipt → actions gap: 16.5 dp (chatD), 24 dp (chatF) and 28 dp (chatG). The app uses 22 dp; receipt and actions are gated as separate boxes.
  - Light receipts are green-tinted (no token); dark and the app use `surf2`.
  - The `chatF` dark gold draws receipt and buttons 314 dp wide (335 in the other five), re-crops the photo and bolds the caption.
  - `chatM` (ST6) still draws Gravar | Trocar | Pular: only its thread is gated.
  - The header "tune" button and the composer mic, as on every chat gold.
- `chatP` is no longer drawn by the Chat (ADR-028). Its gold stays as the reference of the Home skip, which has its own sheet.

Decisions taken within the plan, recorded here:

- A restored receipt carries no routine. Trocar refeição on it moves the record without memory changes, and Excluir on it reverts nothing (the replaced receipt's own revert already ran on Desfazer).
- A moved receipt's memory images cover the whole move (revert of the old change plus the routine in the new slot), so Desfazer of a move returns the memory to just before it.
- Undo of a move that replaced a record restores both slots, each with its own `Restaurado` receipt.
- A skip announced for a slot that is not of today, or with no slot, keeps the turn `Não registrado` and logs `record_guard`.
- The memory change lands on the receipt right after the record: it is stored with the undo data once the slot transaction has committed.
