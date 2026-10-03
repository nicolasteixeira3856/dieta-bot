# Plan — A38 Temp facts on the device, suggested slot in the history, compaction that keeps the open tail

- Status: Pendente aprovação manual
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`core/network/ChatModels.kt`, `core/database/*` (Room v9), `core/memory/FactMemory.kt`, `domain/MemoryRules.kt`, `feature/chat/PromptBuilder.kt`, `feature/chat/ChatViewModel.kt`, `src/dev/.../devtools/{FactText,DevMemoryViewModel,DevMemoryScreen}.kt`, `core/telemetry` counts, tests)
- Prerequisites: [ADR-029](../../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md) accepted by the owner; **[S16](../../../server/plans/completed/s16-dia-da-refeicao-fatos-temporarios.md) deployed on the dev server** (it accepts `T` facts and `temp_facts`, and returns `question_slot`). Executes ADR-029 decisions 1 (client part), 2 and the slot marker of decision 5. No Stitch gate: no gold changes.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a38-fatos-temporarios-compactacao.md`. Implemente o plano aprovado.

**First implementation step:** confirm S16 in `docs/server/plans/completed/` (or `pending_manual_validation/`) and that the dev server accepts a request with a `T1` temp fact and `temp_facts: true` (HTTP 200, not 422). Otherwise stop and tell the owner: a server without S16 rejects `T` facts, so this APK must not ship before it.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

Label data read one day is used on the following days and leaves memory by itself. Asking to record lands in the slot the AI already suggested. An answer given right after a compaction still has its question.

## Background

Owner session of 03/10/2026 (ADR-029 § Context):

- The compaction at 13:59:33 summarised all 12 raw messages, including the cheesecake photo and the assistant question asked 17 s earlier. The next turn ("100 gramas") went with `DIGESTS` and an empty `HISTORY` (`PromptBuilder.rawSinceDigest` cuts at the digest creation time).
- "É para registrar na refeição de hoje" got "Em qual refeição?": `PromptBuilder.turnText` sends text only, so the `estimateSlotId` stored on each answer never reaches the model; a question-only row stores no slot at all (the server held the estimate).
- The label numbers of 02/10 had nowhere to live: today's history only, `recent` only holds recorded meals, memory had only permanent/dynamic kinds.

## Implementation scope

### 1. Wire (`ChatModels.kt`, `ChatViewModel`)

- `ChatIn.tempFacts: Boolean` (`temp_facts`) sent as `true` on every request, always encoded (like `autoRecord`). This makes the app a v5 client.
- `ChatFact` for a temp fact: `id` `T{n}`, `kind` `temp`, `days_seen` 1, `last_seen` = creation date.
- `ChatOut.questionSlot: String? = null` (`question_slot`). A question-only answer stores it in `estimateSlotId` when it is one of today's slots (same check as `suggestedSlot`); otherwise null. No new column.

### 2. Suggested slot in `HISTORY` (`PromptBuilder.turnText`)

- An assistant row with `estimateSlotId` (estimate or question-only) ends with a line `[refeição sugerida: {slot name}]`, the slot name as in today's profile; a slot no longer in today's slots gets no line.
- The text is clipped first (`ChatText.clip`) and the line appended after, so the marker always survives.
- Receipts, user rows and rows without a slot are unchanged.

### 3. Memory (`domain/MemoryRules.kt`, `core/memory/FactMemory.kt`)

- `Fact.kind` accepts `temp`; `NextIds` gains `T` (default 1; a v2 file without it reads as 1). File stays `v: 2`; an older APK reading a file with `T` facts is not supported (dev testers update forward only; said in the release notes).
- Constants: `TEMP_MAX = 5`, `TEMP_TTL_DAYS = 3`.
- `add` with `kind: temp`: same key among **temp** facts → replace its text (keeps id); else new `T{n}`; above 5, the oldest by `created` leaves. Never merges with a permanent or dynamic fact of the same key, and an `add` of another kind never merges with a temp fact. `category` `routine` or a slot → ignored.
- `replace` / `remove` of a `T` id: as for other kinds. `reinforce` of a `T` id: ignored. No promotion from or to temp.
- Expiry at every read: a temp fact whose `created` is 3 or more São Paulo days before today leaves (created 02/10 → gone on 05/10). Expiry is not a change (`Memória atualizada` is not shown for it).
- Applied at answer time (like preference and portion). Record, Registrar, Registrar assim, Substituir, Trocar refeição, Excluir, Editar and Desfazer never add, remove or revert a temp fact: temp facts never enter a receipt's memory images.
- `memoryUsedKinds` ignores `T` ids (no new chip).

### 4. Dev editor (`FactText.kt`, `DevMemoryViewModel.kt`, `DevMemoryScreen.kt`, dev flavor only)

- Format: temp facts after the others, one read-only line each: `T1 | temp | portion | lasanha | {text} · criado dd/MM`. Counter line gains `Temporária n/5`.
- Parse: `kind` is read explicitly (`permanent`, `dynamic`, `temp`); nothing is "dynamic because not permanent". Lines with a `T` id are skipped by the parser and the temp facts of the current memory are kept as they are on Salvar; a changed or removed temp line is ignored, not an error.
- Key uniqueness is checked inside each group (permanent + dynamic together, as today; temp apart), so `lasanha` may exist as temp and as dynamic.

### 5. Compaction keeps the open tail (`PromptBuilder`, `ChatViewModel`, `DayRepository`)

- Room v9: `day_digest.coversUntilId` (nullable `Long`): the id of the newest `chat_message` the digest summarised. Null (rows before v9) = old behaviour, cut by `createdAtEpochMs`.
- `rawSinceDigest`: raw messages after the newest digest's `coversUntilId` (or after its creation time when null), after the latest wipe, as today. It returns all of them; the prompt still takes the newest 12.
- Compaction selection, computed on the full raw list (never on the truncated 12):
  1. `keepFrom` = the newest 4 raw (`KEEP_RAW = 4`); when `clarifyRounds(todayMessages) > 0`, moved back to the user message right before the first question-only row of the open sequence, if older.
  2. `block` = the oldest contiguous raw messages before `keepFrom`, at most 12. Empty → no compaction.
  3. The compact request carries exactly `block`; the digest stores `coversUntilId` = `block.last().id`.
  4. While the raw left is still more than 12 (after earlier failures), one more block is compacted in the same send; at most 2 compactions per send. What still exceeds 12 waits for the next send (the prompt sends the newest 12, as today).
  5. An open sequence of 12 or more: no compaction; the newest 12 go raw.
- Day and wipe are captured before the compact call (São Paulo date of the send and id of the latest `wiped` row). `upsertDigest` takes both and, in its transaction, writes nothing when today's date or the latest wipe id differs. The turn is rebuilt after a successful compaction, as today.
- Failure path unchanged: a failed compaction stores nothing; the turn goes with the newest 12 raw and the next send tries again.

### 6. Room v9 (one migration, `room-v2` spec updated at Completion)

- `day_digest.coversUntilId INTEGER` nullable. Migration test v8 → v9 with existing digests (time cut kept).

### 7. Telemetry (dev)

- The memory event counts add `temp` totals and the ops on temp facts (`add`, `replace`, `remove`, `expire`). Numbers only.
- The compaction adds `kept` (raw kept) and `blocks` (compactions in the send). Numbers only.

## Intended spec changes (written at Completion, not now)

- [memoria-push](../../../produto/specifications/memoria-push.md) memory rules 1–5, 7, 8: third kind `temp`, `T` ids, cap 5, 3-day expiry, not touched by records or receipts, editor, badge; Provenance A38.
- [chat](../../../produto/specifications/chat.md) rules 8 and 9: prompt carries temp facts and the `[refeição sugerida: …]` line; compaction summarises the oldest block of ≤ 12, the newest 4 raw (or the open question's meal) stay raw, day and wipe checked; Provenance A38.
- [room-v2](../../specifications/room-v2.md): v9 column, title version.
- [api-contract](../../../api-contract.md) is written by S16.

## Out of scope

- Server (S16): prompt rules, schema, shaping, `question_slot`, evaluator.
- A visible temp-memory chip or any new UI copy (would need a Stitch gate).
- Removing a temp fact when a meal is recorded (rejected in ADR-029, option A of 03/10/2026); promotion of temp facts; editing temp facts in the dev editor.
- Sending yesterday's chat or digest.
- Retroactive records ([A35](../out_of_scope/a35-registro-retroativo.md), `Fora de escopo`).

## Planned validation

1. `./gradlew testDevDebugUnitTest` green, including new tests:
   - `MemoryRulesTest`: temp add/replace/remove; key isolation both ways with dynamic; cap 5 drops the oldest; expiry on day 3 and not on day 2; reinforce and routine temp ignored; a record's routine update leaves temp facts untouched.
   - `FactMemoryTest`: `NextIds` without `T` reads as 1; `T` ids never reused.
   - Record paths: the same `T1` used by two recorded meals stays; Trocar refeição, Excluir, Editar and Desfazer leave temp facts as they are.
   - `FactTextTest`: temp lines formatted and counted; Salvar of an edited permanent fact keeps every temp fact; same key as temp and dynamic accepted; a changed temp line ignored.
   - `PromptBuilderTest`: the marker on estimate and question-only rows, after clipping, slot name from today's profile, absent for a slot not in today's profile; `temp_facts: true` always encoded; temp facts sent with `last_seen` = creation date.
   - `ChatCompactTest`: 12 raw → 8 summarised, 4 kept, `coversUntilId` set; open question 6 back keeps 6; open sequence of 12 → no compaction; 18 raw after failures → one block of 12, 6 left; 30 raw → two blocks; three compactions in a day (third replaces the oldest digest, nothing summarised twice); a v8 digest without `coversUntilId` keeps the time cut; controlled clock crossing midnight during the compact call → nothing stored; a wipe during the compact call → nothing stored; `question_slot` stored on a question-only row.
   - Migration v8 → v9.
2. `./gradlew verifyRoborazziDevDebug` green (no visual change expected; any diff is a stop).
3. `./gradlew assembleDevRelease` builds.
4. Manual on the dev APK (owner, through `tools/distribute-dev.ps1` when the owner asks for a test build), checked against the dev conversation log:
   - Day 1: send a label (typed or photo) for a food not eaten now → `Memória atualizada`; the dev editor shows `T1`.
   - Day 2: "comi 300 g da lasanha" → estimate uses the label numbers (log `memory_used` has `T1`) and is recorded; the editor still shows `T1`. On that receipt: Trocar refeição to another empty slot, then Excluir → `T1` still there.
   - Day 2: record a second meal over an already recorded slot (Substituir) and tap Desfazer on that receipt → the previous record comes back with `Restaurado`; `T1` unchanged.
   - Day 3: "comi o resto da lasanha, 300 g" → uses `T1` again. Day 4 (3 days after creation): the editor no longer shows `T1`.
   - Suggested slot: send food without naming a meal, get an estimate suggesting a slot without recording (e.g. a question round), then "registra na refeição de hoje" → recorded in that slot without "Em qual refeição?".
   - Clarify after compaction: with 11 or more raw messages today, send a food photo that gets a question, answer it → the answer is understood (log `HISTORY` of the answer turn has the question).
5. Docs: `node tools/check-docs.mjs` passes.

## Risks and controls

- **A wrong temp fact used for 3 days:** "esquece X" removes it; the dev editor shows it; cap 5.
- **The marker confuses the model** (repeats it in the reply): S16 instruction and evaluator; the marker is plain pt-BR and short.
- **Tail logic keeps too much and the prompt grows:** hard cap 12 raw, as today.
- **Old digests without `coversUntilId`:** time cut, as today; covered by a test.
- **Memory file read by an older APK:** dev testers only update forward; release notes say so.

## Acceptance criteria

- A label given one day is used by estimates on the following days, for every portion, until it is 3 days old; records and receipt actions never remove or restore it.
- There are never more than 5 temp facts.
- "Registra na refeição de hoje" after an estimate or question that suggested a slot records in that slot.
- After a compaction, the open question and its meal are still in `HISTORY`; no message is summarised twice; nothing from yesterday or from before a wipe is stored as today's digest.
- No visual diff against the current Roborazzi baselines.

## Results

Owner approval received on 2026-10-03 with the sentence of the authorization gate. Code and automated validation done on 2026-10-03; the owner's manual validation (step 4) is pending, so the plan is `Pendente aprovação manual`.

### First implementation step

S16 is in `docs/server/plans/completed/`. A live `POST /v1/chat` to the dev server with `facts: [T1, kind temp]`, `temp_facts: true`, `clarify_rounds: 0`, `auto_record: true` (request id `a38-gate-check`) returned **HTTP 200** with `memory_used: ["T1"]`.

### Implementation

- Wire: `ChatIn.tempFacts` (`temp_facts`, no default, always encoded); `ChatOut.questionSlot`. A question-only answer stores `question_slot` in `estimateSlotId` when it is one of today's slots; the record mode still uses only the estimate's slot, and a question-only row is not recordable, so no action or visual change.
- History marker: `PromptBuilder.chatTurn` appends `\n[refeição sugerida: {slot}]` to assistant rows whose slot is in today's profile. The body is clipped to `2000 − marker length` code points first, so the turn stays within the server's 2000 limit (a clip to 2000 followed by the marker would exceed it and get a 422).
- Memory: `Fact.dynamic` / `Fact.temp`, `NextIds.t`, `TEMP_MAX = 5`, `TEMP_TTL_DAYS = 3`; temp add/replace/remove/expiry as specified; cap, promotion and expiry of dynamic facts now test `kind == dynamic` explicitly. `MemoryRules.images` and `revert` skip temp facts; `FactMemory.revertAndApply` (every record and receipt path) drops temp updates and temp images, while the answer path (`apply`) keeps them.
- Compaction: `PromptBuilder.compactBlocks` (keep 4, back to the open question's meal, ≤ 12 per block, ≤ 2 blocks, open tail ≥ 12 → none); `Turn.blocks` / `Turn.kept`; `compact(turn, block)` sends exactly the block. `rawSinceDigest` cuts by the newest digest's `coversUntilId`, or by its creation time when null. `ChatViewModel.post` captures the send date and the latest wipe id, compacts block by block (stops at the first failure) and rebuilds the turn after each stored digest. `DayRepository.upsertDigest(text, coversUntilId, sentOn, wipeId)` returns false and writes nothing when today or the latest wipe changed.
- Room v9: `day_digest.coversUntilId`, `MIGRATION_8_9`, schema `9.json`. Digest ordering ties are broken by `coversUntilId`, so two digests stored in the same millisecond keep their order.
- Dev editor: temp lines last and read-only, `Temporária n/5`, `T` lines skipped by the parser and current temp facts kept on Salvar; the `visto` list shows only permanent and dynamic facts (a temp line already carries `criado`). `DevMemoryScreen.kt` needed no change.
- Telemetry (dev): `memory_changed` adds `temp`, `temp_add`, `temp_replace`, `temp_remove`, `temp_expire`; a temp op is also counted under its plain op, so `Memória atualizada` follows the existing rule. New event `chat_compact` with `blocks`, `kept`, `summarised` (numbers only), fired when at least one digest was stored.

### Automated validation

- `./gradlew :app:testDevDebugUnitTest`: **471 tests, 0 failures** (final run). New: `MemoryRulesTest` (9), `FactMemoryTest` (3), `PromptBuilderTest` (8, 2 updated), `ChatCompactTest` (6, 3 updated), `ChatViewModelTest` (2: temp fact through two records, Trocar refeição, Excluir, Editar, Substituir + Desfazer; `question_slot` stored and sent back as the marker), `FactTextTest` (2), `MigrationV8V9Test` (1). Existing expectations updated for v9 and the new telemetry keys.
- One run failed once in `ChatCompactTest.compactFailure_turnStillAnswers_nothingStored_retriesNextSend`: it read `uiState.value` right after Room had the answer, before the render. The test now waits for the rendered answer; the next two full runs were green.
- `./gradlew :app:verifyRoborazziDevDebug`: **BUILD SUCCESSFUL**, no new compare images (no visual diff).
- `./gradlew :app:assembleDevRelease`: **BUILD SUCCESSFUL**.
- `node tools/check-docs.mjs`: passes. Its C4 rule flags `pending` inside a `plans/pending_manual_validation/` link target as a Provenance status marker, so the four spec Provenance entries for A38 use reference-style links (`[A38][a38]`); they become inline links when the plan closes. A separate checker fix was flagged to the owner.

### Pending

- Manual validation step 4 on a dev APK (owner, through `tools/distribute-dev.ps1` when a test build is asked): temp label across days 1–4, receipt actions leaving `T1`, suggested slot without "Em qual refeição?", clarify answer after a compaction. Release notes must say that an older APK cannot read a memory file with temp facts (update forward only).

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../../sdd/README.md#fora-de-escopo).
