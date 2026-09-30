# Specification — Room persistence (v6)

## Status and ownership

Android owns the database. Room v4 extends the shipped v3 schema through [A24](../plans/completed/a24-refeicoes-por-dia.md) and [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md). The historical filename remains stable for incoming links. Room v5 adds the Chat meal text and intent through [A27](../plans/completed/a27-chat-v2-texto-intencao.md) ([ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md)). Room v6 adds per-message memory v2 metadata through [A28](../plans/completed/a28-memoria-v2.md). Exported schemas 1–6 live in `apps/android/app/schemas/`.

`DietaBotDatabase` contains `profile`, `day`, `meal_log`, `meal_slot`, `slot_skip`, `chat_message` and `day_digest`. Structured daily state stays in Room; DataStore is only a legacy import path. No destructive migration fallback.

## Functional rules

1. `profile` holds ceiling configuration, eat-back, onboarding/first-day markers, body fields and daily macro targets. v2 added sex (default empty), age/height/weight (default 0), protein/carbohydrate/fat targets (150/200/67). v4 adds `slotMode TEXT NOT NULL DEFAULT 'same'`: `same`, `split` or `each`, independent of ceiling mode.
2. `meal_slot` holds an auto-generated ID, name, minutes since midnight (0–1439) and sort order. v4 adds `days INTEGER NOT NULL DEFAULT 127`. Monday=1 through Sunday=64; same=127, split=31/96, each=one bit. Each group has 2–6 named meals. Names need not be unique.
3. `meal_log` holds the date, text, kcal/P/C/G, stability/source and nullable `slotId` foreign key (`ON DELETE SET NULL`). Legacy window remains. Repository `addLog` inserts; Chat follows [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md): recording an occupied slot asks for confirmation, then replaces that day's slot logs in one transaction. Older duplicate rows continue to sum.
4. `slot_skip` has primary key `(date, slotId)`. Skip is always explicit. Recording clears the slot's skip; skipping removes that day's logs for the slot.
5. `chat_message` stores date, role, text, timestamp, photo path and nullable estimates (kcal/P/C/G, confidence, suggested slot, question, item names). v3 added `estimateSlotId`, `estimateQuestion` and `estimateItems`. v5 adds nullable `estimateMealText` (server `meal_text`, the text Gravar records) and `intent` (`log` | `plan` | `question`; null = old row or old server, handled as `log`). v6 adds nullable `pendingMemory` (JSON of the routine `add`/`reinforce` proposals of an answer, applied only when that estimate is recorded), nullable `memoryUsedKinds` (`permanent`, `dynamic` or `permanent,dynamic`) and `memoryUpdated INTEGER NOT NULL DEFAULT 0` (a memory change was applied with the answer or with the receipt). UI retains 60 days. Receipt roles (`logged`, `replaced`, `skipped`) are UI-only; `wiped` is neither rendered nor sent.
6. `day_digest` has primary key `(date, seq)`, at most two rows per day. New blocks replace the oldest after both slots are occupied.
7. `day.workoutKcal` is nullable and date-keyed in America/Sao_Paulo; missing means zero workout credit. Profile survives rollover. Mifflin-St Jeor suggests a ceiling; initial macros use 30/40/30.
8. `observeToday` switches its date-keyed queries when the Sao Paulo calendar changes, checked every 30 seconds while collected. Configuration retains the entire week in `DaySnapshot.slots`; daily consumers use `slotsOn(date)` / `SlotsOfDay`. Home, Chat profile/snapshot/current-slot/actions and push use the same day selection.
9. `saveSlots(slots, slotMode)` updates kept IDs, creates new rows, deletes removed slots and saves the mode atomically. It never deletes meal logs, skips, Chat, digests or encrypted memory. Removed slots orphan their logs; logs of slots outside today's group appear under “Outros”. Copying a group retains destination IDs and does not reuse source IDs.
10. `wipeToday` deletes only today's logs/skips/digests, preserves profile/slots/workout/Chat/memory, and inserts a `wiped` Chat marker. `changeCeiling` saves the ceiling and wipes in one transaction. The prompt restarts after the marker.
11. Prompt totals include all today's logs, including those outside today's meal group. Its slot lists include only today's slots, with eaten/skipped/empty status.
12. `MealLogDao.getBetween(from, to)` (inclusive ISO dates) feeds the Chat `recent`: the 7 days before today in America/Sao_Paulo (`DayRepository.recentLogs`).

## Migrations and validation

- `MIGRATION_1_2` adds body/macros and meal-slot/skip/Chat/digest tables, rebuilding `meal_log` for its foreign key.
- `MIGRATION_2_3` adds the three Chat estimate metadata columns.
- `MIGRATION_3_4` executes only two `ALTER TABLE ... ADD COLUMN` statements. Existing IDs, meals, logs, skips, Chat and digests stay intact; old slots apply every day and old profiles use `same`.
- `MIGRATION_4_5` executes only two `ALTER TABLE `chat_message` ADD COLUMN` statements (`estimateMealText`, `intent`). Old rows stay null and keep working as before.
- `MIGRATION_5_6` executes only three `ALTER TABLE `chat_message` ADD COLUMN` statements. Old rows: nothing pending, nothing used, not updated.
- Schemas are exported by KSP. Debug assets include these schemas for `MigrationTestHelper`; release does not package them.
- Tests validate old migrations, the real v3 and v4 fixtures with `MigrationTestHelper`, weekly filtering/rollover, slot-save history preservation and Chat replacement/wipe behavior. Device evidence and pending owner/tester update validation are recorded in [A24 validation](../validation/a24-refeicoes-por-dia.md).

## Related decisions and plans

- [Room decision 010](../../decisions/010-room.md)
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md)
- [A1](../plans/completed/a1-room-v2.md), [A24](../plans/completed/a24-refeicoes-por-dia.md), [A27](../plans/completed/a27-chat-v2-texto-intencao.md), [A28](../plans/completed/a28-memoria-v2.md)
