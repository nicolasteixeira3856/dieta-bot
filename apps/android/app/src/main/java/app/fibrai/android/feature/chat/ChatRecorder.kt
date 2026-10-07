package app.fibrai.android.feature.chat

import app.fibrai.android.core.database.ChatMessageEntity
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.RecordGuard
import app.fibrai.android.core.database.SkipMove
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.FactImage
import app.fibrai.android.domain.MemoryResult
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.ReceiptRules
import app.fibrai.android.domain.RecordedMeal
import app.fibrai.android.domain.RoutineUpdate
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SkipOutcomes
import app.fibrai.android.domain.SlotChange
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import app.fibrai.android.domain.UndoData

/** A record about to be written by the Chat (A34): the meal_log row and what its receipt keeps. */
internal data class NewRecord(
    val record: SlotRecord,
    /** user | photo | plan | routine: drives Editar. */
    val source: String,
    /** Routine add/reinforce that count only with this record (ADR-023). */
    val routine: List<RoutineUpdate>,
    /** The assistant row recorded, or null (routine card). Its recordState becomes "recorded". */
    val estimateId: Long?,
)

/** Outcome of a record or a move into a slot. */
internal sealed interface RecordOutcome {
    data class Recorded(val before: SlotState, val receiptId: Long) : RecordOutcome

    /** The slot already has a record: nothing changed, the Chat asks first (chatU). */
    data class Taken(val state: SlotState) : RecordOutcome

    /** The slot moved under the action (another path, a race): nothing written. */
    data object Stale : RecordOutcome
}

/**
 * Every Chat write of a record, a skip and the receipt actions (A34, ADR-028). Each one is one Room
 * transaction that first checks the slot still holds what the action expects; the memory change follows
 * and is stored on the receipt so Excluir, Desfazer, Editar and Trocar refeição can reverse it.
 */
internal class ChatRecorder(
    private val repository: DayRepository,
    private val memory: FactMemory,
    private val telemetry: Telemetry,
    private val clock: InstantClock,
    /** After every memory write: telemetry of the change and the routine card facts. */
    private val onMemory: suspend (MemoryResult) -> Unit,
) {
    private fun today() = SaoPaulo.date(clock.now())

    /**
     * [new] into [slot] of today. Empty or skipped slot: recorded (a skip has no numbers to lose). A slot with
     * a record: [RecordOutcome.Taken] unless [expected] says the user already confirmed that exact state.
     * [guard] (A47): the day, wipe, untouched source slots and the open answer a proposal also requires.
     */
    suspend fun record(new: NewRecord, slot: SlotRef, expected: SlotState? = null, guard: RecordGuard? = null): RecordOutcome {
        val date = today().toString()
        val before = repository.slotState(date, slot.id)
        if (expected == null && before.records.isNotEmpty()) return RecordOutcome.Taken(before)
        if (expected != null && before != expected) return RecordOutcome.Stale
        val change = SlotChange(date, slot.id, before, SlotState.of(new.record))
        val role = if (before.records.isNotEmpty()) ReceiptRules.REPLACED else ReceiptRules.LOGGED
        val receipt = receipt(role, slot.name, slot.id, new.record.kcal, new.source, UndoData(listOf(change), routine = new.routine))
        val id = repository.commitRecord(
            listOf(change),
            receipts = listOf(receipt),
            recordStates = new.estimateId?.let { mapOf(it to RECORDED) }.orEmpty(),
            guard = guard,
        )?.single() ?: return RecordOutcome.Stale
        val result = applyRoutine(emptyList(), new.routine, slot.id, new.record)
        repository.setReceiptUndo(id, UndoData(listOf(change), result?.images.orEmpty(), new.routine).encode(), result?.changed == true)
        return RecordOutcome.Recorded(before, id)
    }

    /** "Pulei o café" (ADR-028 decision 4): only an empty slot of today is skipped. */
    suspend fun skip(estimateId: Long, slot: SlotRef): Boolean {
        val date = today().toString()
        val change = SlotChange(date, slot.id, SlotState.EMPTY, SlotState.SKIPPED)
        val receipt = receipt(ReceiptRules.SKIPPED, slot.name, slot.id, null, null, UndoData(listOf(change)))
        return repository.commitRecord(listOf(change), receipts = listOf(receipt), recordStates = mapOf(estimateId to RECORDED)) != null
    }

    /**
     * A59 (ADR-047): a slot listed in `skip_slots` that is empty now is skipped with its own receipt, and the answer's
     * skip moves from pending to skipped in the same transaction. False when the slot or the skip moved meanwhile.
     */
    suspend fun skipListed(answerId: Long, slot: SlotRef): Boolean {
        val date = today().toString()
        val change = SlotChange(date, slot.id, SlotState.EMPTY, SlotState.SKIPPED)
        val receipt = receipt(ReceiptRules.SKIPPED, slot.name, slot.id, null, null, UndoData(listOf(change)))
        return repository.commitRecord(
            listOf(change),
            receipts = listOf(receipt),
            skip = SkipMove(answerId, slot.id, from = null, to = SkipOutcomes.SKIPPED, state = SlotState.EMPTY),
        ) != null
    }

    /**
     * Excluir e pular (A59, chatSD): the slot's records leave as Excluir does and the slot is skipped, in one transaction
     * that rechecks the day, the wipe, the slot against [state] and the open proposal. [recordReceipt], the active
     * receipt of those records, is marked Excluído and its memory change reverted; the skip receipt keeps that revert, so
     * Desfazer brings the records and the memory back. Null when anything moved.
     */
    suspend fun deleteAndSkip(answerId: Long, slot: SlotRef, state: SlotState, recordReceipt: ChatMessageEntity?, guard: RecordGuard): Long? {
        val change = SlotChange(guard.date, slot.id, state, SlotState.SKIPPED)
        val receipt = receipt(ReceiptRules.SKIPPED, slot.name, slot.id, null, null, UndoData(listOf(change)))
        val id = repository.commitRecord(
            listOf(change),
            receipts = listOf(receipt),
            receiptMarks = recordReceipt?.let { mapOf(it.id to DELETED) }.orEmpty(),
            guard = guard,
            skip = SkipMove(answerId, slot.id, from = SkipOutcomes.PENDING_DELETE, to = SkipOutcomes.DELETED),
        )?.single() ?: return null
        val images = revert(recordReceipt?.let { UndoData.decode(it.undoData)?.facts }.orEmpty())
        if (images.isNotEmpty()) repository.setReceiptUndo(id, UndoData(listOf(change), images).encode(), false)
        return id
    }

    /**
     * Excluir (or Editar with [ReceiptMark.EDITED]): the record's slot becomes empty and the memory change of
     * the receipt is reverted. Returns the records removed, or null when the slot no longer matches.
     */
    suspend fun delete(receipt: ChatMessageEntity, mark: String): List<SlotRecord>? {
        val undo = UndoData.decode(receipt.undoData) ?: return null
        val slot = undo.recordSlot ?: return null
        val change = SlotChange(slot.date, slot.slotId, slot.after, SlotState.EMPTY)
        repository.commitRecord(listOf(change), receiptMarks = mapOf(receipt.id to mark)) ?: return null
        revert(undo.facts)
        return slot.after.records
    }

    /**
     * Desfazer: every touched slot goes back to its "before", the memory change is reverted. A slot that ends
     * with a record gets a `restored` receipt, which takes the actions (chatD).
     */
    suspend fun undo(receipt: ChatMessageEntity, slotName: suspend (Long) -> String): Boolean {
        val undo = UndoData.decode(receipt.undoData) ?: return false
        val changes = undo.slots.reversed().map { SlotChange(it.date, it.slotId, it.after, it.before) }
        val restored = changes.filter { it.after.records.isNotEmpty() }.map {
            receipt(ReceiptRules.RESTORED, slotName(it.slotId), it.slotId, it.after.kcal, it.after.records.first().source.asReceiptSource(), UndoData(listOf(it)))
        }
        repository.commitRecord(changes, receipts = restored, receiptMarks = mapOf(receipt.id to UNDONE)) ?: return false
        revert(undo.facts)
        return true
    }

    /**
     * Trocar refeição: the record leaves its slot for [target] on the same day. A target with a record is
     * [RecordOutcome.Taken] unless [expected] confirms it (Substituir below the receipt). The memory change of the
     * receipt is reverted and its routine applied again to the new slot.
     */
    suspend fun move(receipt: ChatMessageEntity, target: SlotRef, expected: SlotState? = null): RecordOutcome {
        val undo = UndoData.decode(receipt.undoData) ?: return RecordOutcome.Stale
        val from = undo.recordSlot ?: return RecordOutcome.Stale
        if (target.id == from.slotId) return RecordOutcome.Stale
        val targetState = repository.slotState(from.date, target.id)
        if (expected == null && targetState.records.isNotEmpty()) return RecordOutcome.Taken(targetState)
        if (expected != null && targetState != expected) return RecordOutcome.Stale
        val changes = listOf(
            SlotChange(from.date, from.slotId, from.after, SlotState.EMPTY),
            SlotChange(from.date, target.id, targetState, from.after),
        )
        val moved = receipt(ReceiptRules.MOVED, target.name, target.id, from.after.kcal, receipt.recordSource, UndoData(changes, routine = undo.routine))
        val id = repository.commitRecord(changes, receipts = listOf(moved), receiptMarks = mapOf(receipt.id to MOVED))?.single()
            ?: return RecordOutcome.Stale
        val record = from.after.records.reduce { a, b -> a.copy(kcal = a.kcal + b.kcal, p = a.p + b.p, c = a.c + b.c, g = a.g + b.g) }
        val result = applyRoutine(undo.facts, undo.routine, target.id, record)
        repository.setReceiptUndo(id, UndoData(changes, result?.images.orEmpty(), undo.routine).encode(), result?.changed == true)
        return RecordOutcome.Recorded(targetState, id)
    }

    /** `receipt_action` (ADR-028 decision 9): enums and numbers only. */
    fun receiptEvent(action: ReceiptAction, receipt: ChatMessageEntity) {
        val now = clock.now()
        telemetry.event(
            TelemetryEvents.RECEIPT_ACTION,
            mapOf(
                "action" to action.name.lowercase(),
                "receipt" to receipt.role,
                "source" to (receipt.recordSource ?: "user"),
                "age_s" to ((now.toEpochMilli() - receipt.createdAtEpochMs) / 1000).coerceAtLeast(0),
                "same_day" to (receipt.date == SaoPaulo.date(now).toString()),
            ),
        )
    }

    /** Reverts [revert] and applies [routine] with the meal now in [slotId]; null when the memory could not be written. */
    private suspend fun applyRoutine(revert: List<FactImage>, routine: List<RoutineUpdate>, slotId: Long, record: SlotRecord): MemoryResult? {
        if (revert.isEmpty() && routine.isEmpty()) return null
        val meal = RecordedMeal(slotId.toString(), record.kcal, record.p, record.c, record.g)
        val edit = runCatching { memory.revertAndApply(revert, routine.map { it.toDomain() }, today(), meal) }.getOrNull() ?: return null
        if (revert.isNotEmpty()) reverted(edit.reverted, edit.kept)
        onMemory(edit.result)
        return edit.result
    }

    /** Reverts [images]; returns the images of that revert (reverting them puts the facts back), empty when nothing ran. */
    private suspend fun revert(images: List<FactImage>): List<FactImage> {
        if (images.isEmpty()) return emptyList()
        val edit = runCatching { memory.revertAndApply(images, emptyList(), today()) }.getOrNull() ?: return emptyList()
        reverted(edit.reverted, edit.kept)
        onMemory(edit.result)
        return edit.result.images
    }

    private fun reverted(reverted: Int, kept: Int) =
        telemetry.event(TelemetryEvents.MEMORY_REVERTED, mapOf("reverted" to reverted, "kept" to kept))

    private fun receipt(role: String, slotName: String, slotId: Long, kcal: Int?, source: String?, undo: UndoData) = ChatMessageEntity(
        role = role,
        text = slotName,
        estimateKcal = kcal,
        estimateSlotId = slotId,
        recordSource = source,
        undoData = undo.encode(),
    )

    /** meal_log.source to the receipt source: user | photo | routine (a plan is recorded as user). */
    private fun String.asReceiptSource() = takeIf { it in setOf("user", "photo", "routine") } ?: "user"

    companion object {
        const val RECORDED = "recorded"
        const val PENDING_REPLACE = "pending_replace"

        /** A47: Adicionar ao {slot}? below the answer (chatI); Atualizar {slot}? (chatIC). */
        const val PENDING_ADD = "pending_add"
        const val PENDING_REVISE = "pending_revise"
        const val NOT_RECORDED = "not_recorded"

        const val UNDONE = "undone"
        const val DELETED = "deleted"
        const val MOVED = "moved"
        const val EDITED = "edited"
    }
}
