# Specification — Room persistence (v10)

## Ownership

Android owns the database. Schema version 10. The file is `fibrai.db` ([ADR-036](../adrs/ADR-036-fibrai-technical-identity.md)). Exported schemas 1–10 live in `apps/android/app/schemas/app.fibrai.android.core.database.FibraiDatabase/`, byte-identical to their history. The filename keeps its historical name for incoming links.

`FibraiDatabase` contains `profile`, `day`, `meal_log`, `meal_slot`, `slot_skip`, `chat_message` and `day_digest`. Structured daily state stays in Room; DataStore is only a legacy import path. No destructive migration fallback.

## Functional rules

1. `profile` holds ceiling configuration, eat-back, onboarding/first-day markers, body fields and daily macro targets. v2 added sex (default empty), age/height/weight (default 0), protein/carbohydrate/fat targets (150/200/67). v4 adds `slotMode TEXT NOT NULL DEFAULT 'same'`: `same`, `split` or `each`, independent of ceiling mode.
2. `meal_slot` holds an auto-generated ID, name, minutes since midnight (0–1439) and sort order. v4 adds `days INTEGER NOT NULL DEFAULT 127`. Monday=1 through Sunday=64; same=127, split=31/96, each=one bit. Each group has 2–6 named meals. Names need not be unique.
3. `meal_log` holds the date, text, kcal/P/C/G, stability/source and nullable `slotId` foreign key (`ON DELETE SET NULL`). Legacy window remains. Repository `addLog` inserts; Chat follows [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md): recording an occupied slot asks for confirmation, then replaces that day's slot logs in one transaction. Older duplicate rows continue to sum.
4. `slot_skip` has primary key `(date, slotId)`. Skip is always explicit. Recording clears the slot's skip; skipping removes that day's logs for the slot.
5. `chat_message` stores date, role, text, timestamp, photo path and nullable estimates (kcal/P/C/G, confidence, suggested slot, question, item names). v3 added `estimateSlotId`, `estimateQuestion` and `estimateItems`. v5 adds nullable `estimateMealText` (server `meal_text`, the text a record writes) and `intent` (`log` | `plan` | `question`; null = old row or old server, handled as `log`). v6 adds nullable `pendingMemory` (JSON of the routine `add`/`reinforce` proposals of an answer, applied only when that estimate is recorded), nullable `memoryUsedKinds` (`permanent`, `dynamic` or `permanent,dynamic`) and `memoryUpdated INTEGER NOT NULL DEFAULT 0` (a memory change was applied with the answer or with the receipt). UI retains 60 days, read newest first in pages of 20 (`ChatMessageDao.observeLatest(fromDate, limit)`, v7 index `(createdAtEpochMs, id)`); logic that must see a whole day reads `getByDate`, and Registrar/Substituir read the estimate by `getById`. Receipt roles (`logged`, `replaced`, `skipped`, and since v8 `moved`, `restored`) are UI-only; `wiped` is neither rendered nor sent.
6. v8 adds five nullable `chat_message` columns. Assistant rows: `recordMode` (`auto` | `ask` | `none`, from the server `record`; a recordable `log` estimate from a server without `record` is stored as `ask`; null = row from before v8, no record actions at all) and `recordState` (`recorded` | `pending_replace` | `not_recorded` | null). Receipt rows: `receiptState` (`undone` | `deleted` | `moved` | `edited` | null = active) and `recordSource` (`user` | `photo` | `plan` | `routine`, drives Editar). `undoData` holds, on a receipt, the `UndoData` JSON (per touched slot: date, slot id, the state before and after — the logs in id order with text, kcal, P/C/G, source, window, stable, and the skip — plus the memory pre/post image of every fact it changed and the routine updates of the record); on an assistant row with `pending_replace`, the slot state the inline confirmation asked about. Every record action of the Chat is one `DayRepository.commitRecord` transaction: it checks that each touched slot still holds the expected state, then writes the new states, the receipt marks, the record states and the new receipt(s); a mismatch writes nothing.
7. v10 adds nullable `chat_message.mealChange`: on an assistant row, the versioned `MealProposal` JSON of a meal addition or revision ([ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md)): version, operation (`new` | `add` | `revise` | `invalid` with an enum reason), request day, latest `wiped` id of that day, source slot id and state, target slot id and state, the added food (text, kcal/P/C/G, items) and a destination picked later with its state. It is stored with the answer, before any record attempt; null = older row or a server without the capability, which keeps the A34 flow. `recordState` adds `pending_add` (Adicionar asks) and `pending_revise` (Atualizar asks). A proposal's record passes `commitRecord` a `RecordGuard`, checked in the same transaction and never written: today is the request day, the latest `wiped` id is unchanged, every read-only slot (the source of a rerouted addition) still holds its captured state, and the answer is still undecided (`recordState` null or pending). Expiring an answer (`closeOpenRecord`) changes only an undecided row. Nothing rebuilds a proposal from older rows.
8. `day_digest` has primary key `(date, seq)`, at most two rows per day. New blocks replace the oldest after both slots are occupied. v9 adds nullable `coversUntilId`: the id of the newest `chat_message` the digest summarised; the prompt history starts after it. Null (digests from before v9) = cut by `createdAtEpochMs`. `upsertDigest` takes the day and the latest `wiped` id captured before the compact call and, in its transaction, writes nothing when either changed.
9. `day.workoutKcal` is nullable and date-keyed in America/Sao_Paulo; missing means zero workout credit. Profile survives rollover. Mifflin-St Jeor suggests a ceiling; initial macros use 30/40/30.
10. `observeToday` switches its date-keyed queries when the Sao Paulo calendar changes, checked every 30 seconds while collected. Configuration retains the entire week in `DaySnapshot.slots`; daily consumers use `slotsOn(date)` / `SlotsOfDay`. Home, Chat profile/snapshot/current-slot/actions and push use the same day selection.
11. `saveSlots(slots, slotMode)` updates kept IDs, creates new rows, deletes removed slots and saves the mode atomically. It never deletes meal logs, skips, Chat, digests or encrypted memory. Removed slots orphan their logs; logs of slots outside today's group appear under “Outros”. Copying a group retains destination IDs and does not reuse source IDs.
12. `wipeToday` deletes only today's logs/skips/digests, preserves profile/slots/workout/Chat/memory, and inserts a `wiped` Chat marker. `changeCeiling` saves the ceiling and wipes in one transaction. The prompt restarts after the marker.
13. Prompt totals include all today's logs, including those outside today's meal group. Its slot lists include only today's slots, with eaten/skipped/empty status.
14. `MealLogDao.getBetween(from, to)` (inclusive ISO dates) feeds the Chat `recent`: the 7 days before today in America/Sao_Paulo (`DayRepository.recentLogs`).

## Migrations and validation

- `MIGRATION_1_2` adds body/macros and meal-slot/skip/Chat/digest tables, rebuilding `meal_log` for its foreign key.
- `MIGRATION_2_3` adds the three Chat estimate metadata columns.
- `MIGRATION_3_4` executes only two `ALTER TABLE ... ADD COLUMN` statements. Existing IDs, meals, logs, skips, Chat and digests stay intact; old slots apply every day and old profiles use `same`.
- `MIGRATION_4_5` executes only two `ALTER TABLE `chat_message` ADD COLUMN` statements (`estimateMealText`, `intent`). Old rows stay null and keep working as before.
- `MIGRATION_5_6` executes only three `ALTER TABLE `chat_message` ADD COLUMN` statements. Old rows: nothing pending, nothing used, not updated.
- `MIGRATION_6_7` executes only `CREATE INDEX IF NOT EXISTS index_chat_message_createdAtEpochMs_id`. No data change.
- `MIGRATION_8_9` executes only `ALTER TABLE `day_digest` ADD COLUMN `coversUntilId` INTEGER`. Old digests stay null and keep the time cut.
- `MIGRATION_9_10` executes only `ALTER TABLE `chat_message` ADD COLUMN `mealChange` TEXT`. Old rows stay null: no proposal is guessed, records, receipts, undo data, photos and memory are untouched (`MigrationV9V10Test` undoes a migrated receipt).
- `MIGRATION_7_8` executes only five `ALTER TABLE `chat_message` ADD COLUMN` statements (`recordMode`, `recordState`, `receiptState`, `undoData`, `recordSource`). Old rows stay null: old estimates and receipts show no actions.
- Schemas are exported by KSP. Debug assets include these schemas for `MigrationTestHelper`; release does not package them.
- Tests validate old migrations, the real v3 and v4 fixtures with `MigrationTestHelper`, weekly filtering/rollover, slot-save history preservation and Chat replacement/wipe behavior. Device evidence: [A24 validation](../validation/a24-refeicoes-por-dia.md).

## Related decisions

- [Room decision 010](../../decisions/010-room.md)
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md), [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md)

## Provenance

- [A1](../plans/completed/a1-room-v2.md) — Room v2
- [A24](../plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [A27](../plans/completed/a27-chat-v2-texto-intencao.md) — Chat v2: texto da refeição, intenção e histórico de 7 dias
- [A28](../plans/completed/a28-memoria-v2.md) — Memória v2: fatos permanentes e dinâmicos
- [A32](../plans/completed/a32-chat-rolagem-paginacao.md) — Chat: opens at the bottom, reverse paging, keyboard off for the photo
- [A34](../plans/completed/a34-registro-autonomo.md) — Autonomous record, receipts with actions
- [A38](../plans/completed/a38-fatos-temporarios-compactacao.md) — Temp facts on the device, suggested slot in the history, compaction that keeps the open tail
- [A47](../plans/completed/a47-chat-meal-updates.md) — Chat meal updates
