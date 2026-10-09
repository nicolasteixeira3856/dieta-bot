package app.fibrai.android.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One meal_log row as a receipt remembers it (A34). Enough to write it back. */
@Serializable
data class SlotRecord(
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val source: String = "user",
    val window: String = "",
    val stable: Boolean = true,
    /** A68: the recipe version a record by recipe used (meal_log.recipeVersionId); null otherwise. */
    val recipeVersionId: Long? = null,
)

/** A plan reserved for a meal (A60 part D, ADR-046): its dish and numbers, nothing eaten. */
@Serializable
data class PlannedSlot(
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    /** The plan bubble it came from (Reservado para o {slot}); null when unknown. */
    val sourceMessageId: Long? = null,
)

/**
 * What a slot of a day holds: its logs in id order, whether it is skipped and the plan reserved for it (A60 part D).
 * Empty = none of them. A record or a skip replaces the reservation; Desfazer brings it back with the slot's before.
 */
@Serializable
data class SlotState(val records: List<SlotRecord> = emptyList(), val skipped: Boolean = false, val planned: PlannedSlot? = null) {
    val empty: Boolean get() = records.isEmpty() && !skipped && planned == null

    /** Nothing eaten and not skipped: a record or a skip goes in without asking (a reservation is not a record). */
    val open: Boolean get() = records.isEmpty() && !skipped
    val kcal: Int get() = records.sumOf { it.kcal }

    companion object {
        val EMPTY = SlotState()
        val SKIPPED = SlotState(skipped = true)
        fun of(record: SlotRecord) = SlotState(listOf(record))
    }
}

/** A47: a slot a record depends on but does not change (the source of a rerouted addition): it must still hold [state]. */
@Serializable
data class SlotCheck(val date: String, val slotId: Long, val state: SlotState)

/** One slot of one day, from [before] to [after]. */
@Serializable
data class SlotChange(val date: String, val slotId: Long, val before: SlotState, val after: SlotState)

/** A65 (ADR-049): the workout energy of [date] from [before] to [after]; null = no number that day. */
@Serializable
data class WorkoutChange(val date: String, val before: Int?, val after: Int?) {
    fun reversed() = WorkoutChange(date, after, before)
}

/** Pre and post image of one memory fact (A34). Null = the fact did not exist. */
@Serializable
data class FactImage(val id: String, val before: Fact? = null, val after: Fact? = null) {
    /** A38: a temp fact is never recorded, reverted or restored by a receipt. */
    val temp: Boolean get() = before?.temp == true || after?.temp == true || id.startsWith("T")
}

/**
 * What a receipt changed (A34, ADR-028 decision 5): every touched slot before/after, the memory images,
 * and the routine updates that came with the record (applied again to the new slot by Trocar refeição).
 */
@Serializable
data class UndoData(
    val slots: List<SlotChange>,
    val facts: List<FactImage> = emptyList(),
    val routine: List<RoutineUpdate> = emptyList(),
    /** A65: the day's workout number a workout receipt changed; null on every other receipt. */
    val workout: WorkoutChange? = null,
    /** A66 (ADR-050): the first answer row of the typed-actions answer that wrote this receipt by itself; null otherwise. */
    val batch: Long? = null,
) {
    /** The slot that holds the record after this receipt: the one whose "after" has records. */
    val recordSlot: SlotChange? get() = slots.lastOrNull { it.after.records.isNotEmpty() }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): UndoData? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        fun decodeChange(text: String?): SlotChange? =
            text?.let { runCatching { json.decodeFromString(SlotChange.serializer(), it) }.getOrNull() }

        fun encodeChange(change: SlotChange): String = json.encodeToString(SlotChange.serializer(), change)
    }
}

/** A routine add/reinforce kept with a record ([MemoryUpdate] as stored). */
@Serializable
data class RoutineUpdate(
    val op: String,
    val id: String? = null,
    val kind: String,
    val category: String,
    val key: String = "",
    val text: String = "",
    val slot: String? = null,
) {
    fun toDomain() = MemoryUpdate(op, id, kind, category, key, text, slot)
}

/** Receipt actions (ADR-028 decision 5), in the order the buttons stack. */
enum class ReceiptAction { UNDO, DELETE, MOVE, EDIT }

/**
 * Which receipt rows still carry actions (A34). Pure: unit-tested without Room.
 */
object ReceiptRules {
    const val LOGGED = "logged"
    const val REPLACED = "replaced"
    const val SKIPPED = "skipped"
    const val MOVED = "moved"
    const val RESTORED = "restored"

    /** A65: the day's workout energy written from the Chat; text = its mode ("replace" | "add"), kcal = the stated number. */
    const val WORKOUT = "workout"
    val ROLES = setOf(LOGGED, REPLACED, SKIPPED, MOVED, RESTORED, WORKOUT)

    /** A65: the slot id a workout receipt touches in [latest]: one per day, never a real slot. */
    const val WORKOUT_SLOT = -1L

    /** One receipt as the rule sees it: [slots] = (date, slotId) it touched. */
    data class Receipt(val id: Long, val createdAt: Long, val slots: Set<Pair<String, Long>>, val active: Boolean)

    /**
     * Ids of the receipts that are active ([Receipt.active]: undo data and no mark) and the latest receipt
     * of every slot they touched. Order is (createdAt, id). Any day.
     */
    fun latest(receipts: List<Receipt>): Set<Long> {
        val newest = mutableMapOf<Pair<String, Long>, Receipt>()
        for (r in receipts.sortedWith(compareBy({ it.createdAt }, { it.id }))) r.slots.forEach { newest[it] = r }
        return receipts.filter { r -> r.active && r.slots.isNotEmpty() && r.slots.all { newest[it]?.id == r.id } }.map { it.id }.toSet()
    }

    /**
     * The buttons of an active receipt (ADR-028 table). Desfazer on a replacement, a move, a skip and a record
     * over a skip (it brings the skip back); never on a restore. Editar never for a photo nor a skip.
     */
    fun actions(role: String, source: String?, undo: UndoData): List<ReceiptAction> {
        if (role == SKIPPED || role == WORKOUT) return listOf(ReceiptAction.UNDO)
        return buildList {
            val overSkip = role == LOGGED && undo.slots.any { !it.before.empty }
            if (role == REPLACED || role == MOVED || overSkip) add(ReceiptAction.UNDO)
            add(ReceiptAction.DELETE)
            add(ReceiptAction.MOVE)
            if (source != SOURCE_PHOTO) add(ReceiptAction.EDIT)
        }
    }

    const val SOURCE_PHOTO = "photo"
}
