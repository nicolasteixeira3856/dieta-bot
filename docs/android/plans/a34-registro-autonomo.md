# Plan — A34 Autonomous record, receipts with actions

- Status: Aguardando aprovação
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`core/network/ChatModels.kt`, `core/database/*` (Room v8), `core/memory/*`, `feature/chat/*`, `core/telemetry` events, tests, QA tools and captures)
- Prerequisites: **[S14](../../server/plans/s14-registro-autonomo.md) deployed on the dev server** and **[ST9](../../stitch/plans/st9-registro-autonomo.md) in `stitch/plans/completed/`** (golds `chatE`, `chatF`, `chatG` edited; `chatU`, `chatD` new). Executes [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) decisions 1–9; accepts ADR-028 on completion.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a34-registro-autonomo.md`. Implemente o plano aprovado.

**First implementation step:** confirm `docs/stitch/plans/completed/st9-registro-autonomo.md`, the five golds in `docs/qa/stitch/{dark,light}/`, and S14 in `docs/server/plans/completed/` (or `pending_manual_validation/`) with the dev server answering `record`. Otherwise stop and tell the owner.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

A clear meal of today sent to the Chat is recorded by itself and leaves a receipt. The latest receipt of each slot carries Desfazer / Excluir / Trocar refeição / Editar, as ADR-028 decision 5. An unsure meal offers one Registrar button. A slot that already has a record asks inside the conversation before replacing it. "Pulei o café" skips by itself. Every action is measured in dev telemetry.

## Sources of truth

- Golds `chatE`, `chatF`, `chatG`, `chatU`, `chatD` (dark and light) from ST9; `chatT` (Trocar sheet), `chatQ`, `chatR`, `chatS`, `chatM` unchanged.
- [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-027](../adrs/ADR-027-golds-divergentes.md), [chat](../../produto/specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md), [room-v2](../specifications/room-v2.md).
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

- [chat](../../produto/specifications/chat.md): rewrite "Contexto e objetivo", rules 4–7, 12, 16–18 (receipts and actions), "Fora de escopo", states list (`chatU`, `chatD`), acceptance criteria; remove the ADR-028 proposal overlay.
- [room-v2](../specifications/room-v2.md) v8; `AGENTS.md` Chat screen list gains `chatU`, `chatD`; ADR-028 status → Accepted; produto and android READMEs and matrix.

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
- Retroactive recording ([A35](out_of_scope/a35-registro-retroativo.md)).
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

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../sdd/README.md#fora-de-escopo).
