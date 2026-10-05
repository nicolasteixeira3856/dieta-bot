package com.nutri.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Meal-description bounds of the HTTP contract (S18): Unicode code points, a supplementary character counts once. */
object MealText {
    const val NORMAL_MAX = 500
    const val COMPOSED_MAX = 2000

    /** Joins an addition to the recorded text, as the server composes it from DAY. */
    const val SEPARATOR = "; "

    fun length(text: String): Int = text.codePointCount(0, text.length)

    fun fits(text: String, max: Int): Boolean = text.isNotBlank() && length(text) <= max
}

/** Only the newly eaten food of an addition (S18 `addition`), each amount rounded once. */
@Serializable
data class MealAddition(
    val mealText: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val items: List<AdditionItem>,
)

@Serializable
data class AdditionItem(val name: String, val g: Double, val kcal: Int)

/**
 * The structured proposal of one assistant answer (A47, ADR-032), stored with it before any record attempt.
 * [source]/[target] are the slot states the request was built from; [chosen] is a destination picked later for an
 * addition (Escolher outra refeição), with the state it had when picked. Never rebuilt from prose.
 */
@Serializable
data class MealProposal(
    val version: Int = VERSION,
    val operation: String,
    /** Request day (ISO, America/Sao_Paulo) and the latest wipe marker of that day when it left. */
    val date: String,
    val wipeId: Long? = null,
    /** The eaten meal the change is based on (server base_slot); null for a new meal or an addition without one. */
    val sourceSlotId: Long? = null,
    val source: SlotState? = null,
    /** The suggested destination and its state; null = unknown (the user picks it). */
    val targetSlotId: Long? = null,
    val target: SlotState? = null,
    val addition: MealAddition? = null,
    val chosenSlotId: Long? = null,
    val chosen: SlotState? = null,
    /** [INVALID] only: malformed | contradicts | overflow. Enum, never text from the answer. */
    val reason: String? = null,
) {
    val isAddition: Boolean get() = operation == ADD
    val isRevision: Boolean get() = operation == REVISE
    val actionable: Boolean get() = operation != INVALID

    val destinationSlotId: Long? get() = chosenSlotId ?: targetSlotId
    val destination: SlotState? get() = if (chosenSlotId != null) chosen else target

    /**
     * Day, wipe and every captured state this proposal depends on still hold: the source and the current
     * destination. [state] reads the slot now (today).
     */
    fun matches(date: String, wipeId: Long?, state: (Long) -> SlotState): Boolean {
        if (!sourceHolds(date, wipeId, state)) return false
        val destination = destinationSlotId ?: return true
        return state(destination) == this.destination
    }

    /** Day, wipe and the source still hold: what a new destination for an addition depends on. */
    fun sourceHolds(date: String, wipeId: Long?, state: (Long) -> SlotState): Boolean =
        this.date == date && this.wipeId == wipeId && (sourceSlotId == null || state(sourceSlotId) == source)

    /** The addition's destination is now [slotId] holding [state]; the original target clears the choice. */
    fun choose(slotId: Long, state: SlotState): MealProposal =
        if (slotId == targetSlotId) copy(chosenSlotId = null, chosen = null) else copy(chosenSlotId = slotId, chosen = state)

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val VERSION = 1
        const val NEW = "new"
        const val ADD = "add"
        const val REVISE = "revise"
        const val INVALID = "invalid"

        const val MALFORMED = "malformed"
        const val CONTRADICTS = "contradicts"
        const val OVERFLOW = "overflow"

        private val json = Json { ignoreUnknownKeys = true }

        /** Null for a row without a proposal, an unreadable payload or a version this app does not know. */
        fun decode(text: String?): MealProposal? = text
            ?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
            ?.takeIf { it.version == VERSION }
    }
}

/** What the answer's estimate says, in the server's numbers. */
data class EstimateNumbers(
    val kcal: Double,
    val p: Double,
    val c: Double,
    val g: Double,
    val mealText: String?,
    val suggestedSlot: String?,
)

/**
 * A47 (ADR-032, S18): reads `meal_change`, checks it against the captured slot states and composes additions.
 * Pure: unit-tested without Room. Nothing here reads `reply`, food names or button copy.
 */
object MealChanges {
    /** `meal_change` as it came. */
    sealed interface Wire {
        /** The field is absent: a server without the capability, legacy flow. */
        data object Absent : Wire

        /** Explicit null: the capable server proposes no change. */
        data object None : Wire

        data class Change(val operation: String, val baseSlot: String?, val addition: MealAddition?) : Wire

        data object Malformed : Wire
    }

    private val KEYS = setOf("operation", "base_slot", "addition")
    private val ADDITION_KEYS = setOf("meal_text", "kcal", "p", "c", "g", "items")
    private val ITEM_KEYS = setOf("name", "g", "kcal")
    private const val MAX_ITEMS = 100

    fun parse(element: JsonElement?): Wire {
        if (element == null) return Wire.Absent
        if (element is JsonNull) return Wire.None
        return runCatching { change(element) }.getOrNull() ?: Wire.Malformed
    }

    private fun change(element: JsonElement): Wire.Change? {
        val o = element as? JsonObject ?: return null
        if (o.keys != KEYS) return null
        val operation = o.string("operation") ?: return null
        val base = when (val b = o["base_slot"]) {
            is JsonNull -> null
            else -> (b as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        }
        val addition = when (val a = o["addition"]) {
            is JsonNull -> null
            else -> addition(a ?: return null) ?: return null
        }
        if (operation !in setOf(MealProposal.NEW, MealProposal.ADD, MealProposal.REVISE)) return null
        if ((operation == MealProposal.ADD) != (addition != null)) return null
        return Wire.Change(operation, base, addition)
    }

    /** Round once, ties upward; rounded item kcal sum to the rounded kcal; a caloric input never rounds to zero. */
    private fun addition(element: JsonElement): MealAddition? {
        val o = element as? JsonObject ?: return null
        if (o.keys != ADDITION_KEYS) return null
        val text = o.string("meal_text")?.takeIf { MealText.fits(it, MealText.NORMAL_MAX) } ?: return null
        val raw = listOf("kcal", "p", "c", "g").map { o.number(it) ?: return null }
        val list = o["items"] as? kotlinx.serialization.json.JsonArray ?: return null
        if (list.isEmpty() || list.size > MAX_ITEMS) return null
        val items = list.map { item(it) ?: return null }
        val (kcal, p, c, g) = raw.map { rounded(it) ?: return null }
        if (items.sumOf { it.kcal } != kcal) return null
        if (kcal == 0 && raw.any { it.signum() > 0 }) return null
        return MealAddition(text, kcal, p, c, g, items)
    }

    private fun item(element: JsonElement): AdditionItem? {
        val o = element as? JsonObject ?: return null
        if (o.keys != ITEM_KEYS) return null
        val name = o.string("name")?.takeIf { MealText.fits(it, MealText.NORMAL_MAX) } ?: return null
        val grams = o.number("g")?.takeIf { it.signum() > 0 } ?: return null
        val kcal = rounded(o.number("kcal") ?: return null) ?: return null
        return AdditionItem(name, grams.toDouble().takeIf { it.isFinite() } ?: return null, kcal)
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** A finite, nonnegative JSON number; strings, booleans and null are not numbers. */
    private fun JsonObject.number(key: String): BigDecimal? {
        val p = this[key] as? JsonPrimitive ?: return null
        if (p.isString || p is JsonNull || p.content == "true" || p.content == "false") return null
        return runCatching { BigDecimal(p.content) }.getOrNull()?.takeIf { it.signum() >= 0 }
    }

    private fun rounded(value: BigDecimal): Int? = runCatching { value.setScale(0, RoundingMode.HALF_UP).intValueExact() }.getOrNull()

    /** The text DAY sends for a slot: its logs in id order. */
    fun baseText(state: SlotState): String = state.records.joinToString(MealText.SEPARATOR) { it.text }

    /**
     * The destination after an addition (ADR-032 decision 1): an empty or skipped slot holds only the addition; a slot
     * with a record keeps its text and numbers and gains the addition, as one consolidated record. Null when the
     * composed text does not fit the contract bound: never cut.
     */
    fun compose(destination: SlotState, addition: MealAddition, source: String): SlotRecord? {
        if (destination.records.isEmpty()) return SlotRecord(addition.mealText, addition.kcal, addition.p, addition.c, addition.g, source)
        val text = baseText(destination) + MealText.SEPARATOR + addition.mealText
        if (!MealText.fits(text, MealText.COMPOSED_MAX)) return null
        val r = destination.records
        return SlotRecord(
            text = text,
            kcal = r.sumOf { it.kcal } + addition.kcal,
            p = r.sumOf { it.p } + addition.p,
            c = r.sumOf { it.c } + addition.c,
            g = r.sumOf { it.g } + addition.g,
            source = source,
        )
    }

    /**
     * The proposal of an answer of a capable request, or null when there is none (legacy server, or no change on a
     * turn that records nothing). [states]: every slot of today as the request sent it. A released log estimate
     * whose metadata is missing, malformed or contradicts the captured base and total is [MealProposal.INVALID]:
     * shown, never recorded.
     */
    fun proposal(
        wire: Wire,
        estimate: EstimateNumbers?,
        recordable: Boolean,
        states: Map<Long, SlotState>,
        date: String,
        wipeId: Long?,
    ): MealProposal? {
        val invalid = { reason: String -> MealProposal(operation = MealProposal.INVALID, date = date, wipeId = wipeId, reason = reason) }
        return when (wire) {
            Wire.Absent -> null
            Wire.None -> if (estimate != null && recordable) invalid(MealProposal.MALFORMED) else null
            Wire.Malformed -> if (estimate != null) invalid(MealProposal.MALFORMED) else null
            is Wire.Change -> {
                if (estimate == null || !recordable) return null
                check(wire, estimate, states, date, wipeId) ?: invalid(
                    if (wire.operation == MealProposal.ADD && wire.baseSlot != null && composedOverflows(wire, states)) {
                        MealProposal.OVERFLOW
                    } else {
                        MealProposal.CONTRADICTS
                    },
                )
            }
        }
    }

    private fun composedOverflows(wire: Wire.Change, states: Map<Long, SlotState>): Boolean {
        val base = wire.baseSlot?.toLongOrNull()?.let(states::get) ?: return false
        return base.records.isNotEmpty() && compose(base, wire.addition ?: return false, "user") == null
    }

    private fun check(wire: Wire.Change, e: EstimateNumbers, states: Map<Long, SlotState>, date: String, wipeId: Long?): MealProposal? {
        val targetId = e.suggestedSlot?.let { it.toLongOrNull() ?: return null }
        if (targetId != null && targetId !in states) return null
        val target = targetId?.let { states.getValue(it) }
        val occupied = target?.records?.isNotEmpty() == true
        val baseId = wire.baseSlot?.let { it.toLongOrNull() ?: return null }
        if (baseId != null && (baseId != targetId || !occupied)) return null
        val numbers = listOf(e.kcal, e.p, e.c, e.g).map { whole(it) ?: return null }
        val base = MealProposal(
            operation = wire.operation,
            date = date,
            wipeId = wipeId,
            sourceSlotId = baseId,
            source = baseId?.let { states.getValue(it) },
            targetSlotId = targetId,
            target = target,
            addition = wire.addition,
        )
        return when (wire.operation) {
            MealProposal.ADD -> {
                val addition = wire.addition ?: return null
                if (occupied != (baseId != null)) return null
                val expected = compose(target ?: SlotState.EMPTY, addition, "user") ?: return null
                val same = numbers == listOf(expected.kcal, expected.p, expected.c, expected.g) && e.mealText == expected.text
                base.takeIf { same }
            }
            MealProposal.REVISE -> base.takeIf { baseId != null && wire.addition == null && e.mealText.fitsNormal() }
            else -> base.takeIf { baseId == null && !occupied && wire.addition == null && e.mealText.fitsNormal() }
        }
    }

    private fun String?.fitsNormal() = this != null && MealText.fits(this, MealText.NORMAL_MAX)

    /** A server total is a whole, nonnegative number (the server rounds once; DAY sends whole numbers). */
    private fun whole(value: Double): Int? =
        value.takeIf { it.isFinite() && it >= 0 && it == Math.floor(it) && it <= Int.MAX_VALUE }?.toInt()
}
