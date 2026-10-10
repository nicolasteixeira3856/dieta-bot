package app.fibrai.android.domain

import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One memory fact (ADR-023 decision 4). [days]: distinct America/Sao_Paulo dates the fact showed up,
 * only those of the last [MemoryRules.DYNAMIC_TTL_DAYS]. kcal/p/c/g: routine only, from the last record.
 * A temp fact (A38, ADR-029) lives [MemoryRules.TEMP_TTL_DAYS] days from [created]; its days are never counted.
 */
@Serializable
data class Fact(
    val id: String,
    val kind: String,
    val category: String,
    val key: String,
    val text: String,
    val slot: String? = null,
    val source: String,
    val days: List<String> = emptyList(),
    val created: String,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
    /** A67 (ADR-051): a routine the user declared (discovery), with no recorded day; it lives from [created]. */
    val declared: Boolean = false,
) {
    val permanent: Boolean get() = kind == MemoryRules.PERMANENT
    val dynamic: Boolean get() = kind == MemoryRules.DYNAMIC
    val temp: Boolean get() = kind == MemoryRules.TEMP
    val lastSeen: String? get() = days.maxOrNull()
}

/** Next number of each id prefix: ids are never reused. A file without T reads as 1 (A38). */
@Serializable
data class NextIds(@SerialName("P") val p: Int = 1, @SerialName("D") val d: Int = 1, @SerialName("T") val t: Int = 1)

/** A69 (ADR-053 § 1): the key of a fact the user deleted and the day it was deleted. */
@Serializable
data class Tombstone(val key: String, val deleted: String)

/** [tombstones] (A69): keys an `add` may not bring back until the next compaction. */
@Serializable
data class Memory(val next: NextIds = NextIds(), val facts: List<Fact> = emptyList(), val tombstones: List<Tombstone> = emptyList())

/** A proposal of the AI (server `memory_updates`). id is null only on add. A67: numbers of a routine or liked dish; [declared]. */
data class MemoryUpdate(
    val op: String,
    val id: String?,
    val kind: String,
    val category: String,
    val key: String,
    val text: String,
    val slot: String? = null,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
    val declared: Boolean = false,
)

/** [revert] outcome: the memory and how many facts went back or were left as they are. */
data class RevertResult(val memory: Memory, val reverted: Int, val kept: Int)

/** The meal that was just recorded: a routine takes its slot and macros. */
data class RecordedMeal(val slot: String, val kcal: Int, val p: Int, val c: Int, val g: Int)

/**
 * Operations effectively applied, by name ([MemoryRules.OPS]). [images]: pre and post image of every fact
 * the operation changed (A34), filled by [app.fibrai.android.core.memory.FactMemory]; expiry is not a change.
 */
data class MemoryResult(val memory: Memory, val counts: Map<String, Int>, val images: List<FactImage> = emptyList()) {
    /** At least one update of the AI was applied (expire alone does not count). Temp ops count under their op too. */
    val changed: Boolean get() = UPDATE_OPS.any { (counts[it] ?: 0) > 0 }

    private companion object {
        val UPDATE_OPS = listOf(MemoryRules.ADD, MemoryRules.REINFORCE, MemoryRules.REPLACE, MemoryRules.REMOVE, MemoryRules.PROMOTE)
    }
}

/**
 * The fixed rules the app applies to what the AI proposes (A28). Pure Kotlin: the server only
 * proposes, the app decides. An update that cannot apply (no room, unknown id) is ignored.
 */
object MemoryRules {
    const val PERMANENT_MAX = 50
    const val DYNAMIC_MAX = 40
    const val DYNAMIC_TTL_DAYS = 21L
    const val PROMOTE_DAYS = 5
    const val STRONG_ROUTINE_DAYS = 3
    const val TEMP_MAX = 5
    const val TEMP_TTL_DAYS = 3L
    const val TEXT_MAX = 160
    const val KEY_MAX = 40

    const val PERMANENT = "permanent"
    const val DYNAMIC = "dynamic"
    const val TEMP = "temp"
    const val ROUTINE = "routine"

    /** A67 (ADR-051): an appliance the user declared (permanent, no slot) and a dish the user approved (dynamic, its slot). */
    const val EQUIPMENT = "equipment"
    const val LIKED = "liked"
    val CATEGORIES = setOf("preference", "portion", ROUTINE, EQUIPMENT, LIKED)

    /** A67: the categories that keep a slot and numbers. */
    val MEAL_CATEGORIES = setOf(ROUTINE, LIKED)

    const val EXPLICIT = "explicit"

    /** A67: the source of a routine the user declared in the discovery turn. */
    const val DECLARED = "declared"
    const val PROMOTED = "promoted"
    const val OBSERVED = "observed"

    const val ADD = "add"
    const val REINFORCE = "reinforce"
    const val REPLACE = "replace"
    const val REMOVE = "remove"
    const val PROMOTE = "promote"
    const val EXPIRE = "expire"
    val OPS = listOf(ADD, REINFORCE, REPLACE, REMOVE, PROMOTE, EXPIRE)

    /** A38: the ops on temp facts, counted again under these names (telemetry). */
    const val TEMP_ADD = "temp_add"
    const val TEMP_REPLACE = "temp_replace"
    const val TEMP_REMOVE = "temp_remove"
    const val TEMP_EXPIRE = "temp_expire"
    val TEMP_OPS = listOf(TEMP_ADD, TEMP_REPLACE, TEMP_REMOVE, TEMP_EXPIRE)

    /**
     * Days older than the 21-day window leave; a dynamic fact with no day left leaves (a fact seen on
     * day 1 and never again is gone on day 22). Permanent never expires. A temp fact leaves
     * [TEMP_TTL_DAYS] São Paulo days after it was created (created 02/10 → gone on 05/10).
     */
    fun expire(memory: Memory, today: LocalDate): MemoryResult {
        val from = today.minusDays(DYNAMIC_TTL_DAYS - 1).toString()
        val tempFrom = today.minusDays(TEMP_TTL_DAYS - 1).toString()
        var expired = 0
        var tempExpired = 0
        val facts = memory.facts.mapNotNull { fact ->
            val days = fact.days.filter { it >= from }
            when {
                fact.temp && fact.created < tempFrom -> {
                    expired++
                    tempExpired++
                    null
                }
                // A67: a declared routine has no recorded day; it lives the same window from its creation.
                fact.dynamic && days.isEmpty() && !(fact.declared && fact.created >= from) -> {
                    expired++
                    null
                }
                fact.temp -> fact
                else -> fact.copy(days = days)
            }
        }
        return MemoryResult(memory.copy(facts = facts), mapOf(EXPIRE to expired, TEMP_EXPIRE to tempExpired))
    }

    /** Expire, apply every update in order, cap the dynamic list, promote. */
    fun apply(memory: Memory, updates: List<MemoryUpdate>, today: LocalDate, recorded: RecordedMeal? = null): MemoryResult {
        val expired = expire(memory, today)
        val state = State(expired.memory, today.toString())
        updates.forEach { state.apply(it, recorded) }
        state.promote()
        return MemoryResult(state.memory(), expired.counts + state.counts)
    }

    /** Routine the A29 suggestion can use: permanent, or dynamic seen on ≥ 3 days, with slot and kcal. */
    fun isStrongRoutine(fact: Fact): Boolean =
        fact.category == ROUTINE && fact.slot != null && fact.kcal != null &&
            (fact.permanent || fact.days.size >= STRONG_ROUTINE_DAYS)

    fun clean(value: String, max: Int): String {
        val v = value.replace(Regex("\\s+"), " ").trim()
        if (v.codePointCount(0, v.length) <= max) return v
        return v.substring(0, v.offsetByCodePoints(0, max))
    }

    fun sameKey(a: String, b: String) = clean(a, KEY_MAX).lowercase() == clean(b, KEY_MAX).lowercase()

    /** Every fact that differs between [before] and [after], by id (A34). Temp facts never enter an image (A38). */
    fun images(before: Memory, after: Memory): List<FactImage> {
        val old = before.facts.associateBy { it.id }
        val new = after.facts.associateBy { it.id }
        return (old.keys + new.keys).filter { old[it] != new[it] }.map { FactImage(it, old[it], new[it]) }
            .filterNot { it.temp }
    }

    /**
     * Undo of [images] (A34, ADR-028 decision 6): a fact goes back to `before` only if it still equals `after`
     * (an added fact leaves, a removed one returns, a changed one gets its old text and days). A fact changed
     * since is kept. A returning fact keeps its place when its id is free.
     */
    fun revert(memory: Memory, images: List<FactImage>): RevertResult {
        val facts = memory.facts.toMutableList()
        var reverted = 0
        var kept = 0
        for (image in images.filterNot { it.temp }) {
            val i = facts.indexOfFirst { it.id == image.id }
            val current = facts.getOrNull(i)
            if (current != image.after) {
                kept++
                continue
            }
            when {
                image.before == null -> facts.removeAt(i)
                i >= 0 -> facts[i] = image.before
                else -> facts += image.before
            }
            reverted++
        }
        return RevertResult(memory.copy(facts = facts), reverted, kept)
    }

    /**
     * A69 (ADR-053 § 1): the user deletes [id]: the fact leaves and, unless it is temporary, its key stays as a tombstone
     * dated [today], so an `add` with the same key is refused until the next compaction. Unknown id: unchanged.
     */
    fun delete(memory: Memory, id: String, today: LocalDate): Memory {
        val fact = memory.facts.firstOrNull { it.id == id } ?: return memory
        val tombstones = if (fact.temp) memory.tombstones else memory.tombstones.filterNot { sameKey(it.key, fact.key) } + Tombstone(fact.key, today.toString())
        return memory.copy(facts = memory.facts - fact, tombstones = tombstones)
    }

    /** A69: the user corrects the text of [id]; category, slot, kind and days stay. A blank or unchanged text: unchanged. */
    fun correct(memory: Memory, id: String, text: String): Memory {
        val clean = clean(text, TEXT_MAX).takeIf { it.isNotEmpty() } ?: return memory
        return memory.copy(facts = memory.facts.map { if (it.id == id && it.text != clean) it.copy(text = clean) else it })
    }

    /** A69: a compaction was stored: the tombstones end (ADR-053 § 1, "until the next compaction"). */
    /** At most this many facts come from the onboarding (ADR-057 decision 8). */
    const val ONBOARDING_MAX = 30

    /**
     * A71 (ADR-057): the facts of the profile build, in the server's priority order, at most [ONBOARDING_MAX]. A routine is
     * dynamic, declared, with its slot and numbers and no recorded day; every other fact is permanent, `declared`, no day.
     * A key already kept (not temp) takes the new text in place; no room drops the rest; a tombstoned key is skipped.
     */
    fun declare(memory: Memory, facts: List<MemoryUpdate>, today: LocalDate): Memory {
        var next = memory.next
        val out = memory.facts.toMutableList()
        val day = today.toString()
        for (update in facts.take(ONBOARDING_MAX)) {
            val key = clean(update.key, KEY_MAX)
            val text = clean(update.text, TEXT_MAX)
            if (key.isEmpty() || text.isEmpty()) continue
            if (memory.tombstones.any { sameKey(it.key, key) }) continue
            val routine = update.category == ROUTINE
            if (routine && update.slot == null) continue
            val same = out.indexOfFirst { !it.temp && sameKey(it.key, key) }
            if (same >= 0) {
                out[same] = out[same].copy(text = text)
                continue
            }
            val kind = if (routine) DYNAMIC else PERMANENT
            if (kind == PERMANENT && out.count { it.permanent } >= PERMANENT_MAX) continue
            if (kind == DYNAMIC && out.count { it.dynamic } >= DYNAMIC_MAX) continue
            val id = if (kind == PERMANENT) "P${next.p}".also { next = next.copy(p = next.p + 1) } else "D${next.d}".also { next = next.copy(d = next.d + 1) }
            out += Fact(
                id = id,
                kind = kind,
                category = update.category,
                key = key,
                text = text,
                slot = update.slot.takeIf { routine },
                source = DECLARED,
                days = emptyList(),
                created = day,
                kcal = update.kcal.takeIf { routine },
                p = update.p.takeIf { routine },
                c = update.c.takeIf { routine },
                g = update.g.takeIf { routine },
                declared = routine,
            )
        }
        return memory.copy(next = next, facts = out)
    }

    fun clearTombstones(memory: Memory): Memory = if (memory.tombstones.isEmpty()) memory else memory.copy(tombstones = emptyList())

    private class State(memory: Memory, val today: String) {
        val facts = memory.facts.toMutableList()
        val tombstones = memory.tombstones
        var nextP = memory.next.p
        var nextD = memory.next.d
        var nextT = memory.next.t
        val counts = (OPS + TEMP_OPS).filter { it != EXPIRE && it != TEMP_EXPIRE }.associateWith { 0 }.toMutableMap()

        fun memory() = Memory(NextIds(nextP, nextD, nextT), facts.toList(), tombstones)

        private fun count(op: String) {
            counts[op] = counts.getValue(op) + 1
        }

        private fun permanentRoom() = facts.count { it.permanent } < PERMANENT_MAX

        private fun newP() = "P${nextP++}"

        fun apply(update: MemoryUpdate, recorded: RecordedMeal?) {
            if (update.category !in CATEGORIES) return
            val key = clean(update.key, KEY_MAX)
            val text = clean(update.text, TEXT_MAX)
            when (update.op) {
                // A69: a key the user deleted is not added back until the next compaction.
                ADD -> if (key.isNotEmpty() && text.isNotEmpty() && (update.kind == TEMP || tombstones.none { sameKey(it.key, key) })) {
                    if (update.kind == TEMP) addTemp(update, key, text) else add(update, key, text, recorded)
                }
                // A temp fact is never reinforced: it lives from its creation (A38).
                REINFORCE -> indexOf(update.id).let { i -> if (i < 0 || !facts[i].temp) reinforce(i, text, recorded) }
                REPLACE -> replace(update, text)
                REMOVE -> {
                    val i = indexOf(update.id)
                    if (i >= 0) {
                        val removed = facts.removeAt(i)
                        count(REMOVE)
                        if (removed.temp) count(TEMP_REMOVE)
                    }
                }
            }
        }

        private fun indexOf(id: String?) = if (id == null) -1 else facts.indexOfFirst { it.id == id }

        /**
         * A38: a temp fact only meets other temp facts. Same key → its text changes (id and creation kept);
         * else a new T id; above [TEMP_MAX] the oldest created leaves. Never a routine, never a slot.
         */
        private fun addTemp(update: MemoryUpdate, key: String, text: String) {
            if (update.category == ROUTINE || update.slot != null) return
            val same = facts.indexOfFirst { it.temp && sameKey(it.key, key) }
            if (same >= 0) {
                if (facts[same].text == text) return
                facts[same] = facts[same].copy(text = text)
                count(REPLACE)
                count(TEMP_REPLACE)
                return
            }
            facts += newFact("T${nextT++}", TEMP, update, key, text, OBSERVED)
            count(ADD)
            count(TEMP_ADD)
            while (facts.count { it.temp } > TEMP_MAX) {
                facts.remove(facts.filter { it.temp }.minWith(compareBy<Fact>({ it.created }, { it.id.drop(1).toIntOrNull() ?: 0 })))
            }
        }

        private fun add(update: MemoryUpdate, key: String, text: String, recorded: RecordedMeal?) {
            val same = facts.indexOfFirst { !it.temp && sameKey(it.key, key) }
            if (update.kind == PERMANENT) {
                if (same >= 0) return contradiction(same, update, text, recorded)
                if (!permanentRoom()) return
                facts += withMeal(newFact(newP(), PERMANENT, update, key, text, EXPLICIT), recorded)
                count(ADD)
                return
            }
            if (same >= 0) return reinforce(same, text, recorded)
            facts += withMeal(newFact("D${nextD++}", DYNAMIC, update, key, text, OBSERVED), recorded)
            count(ADD)
            cap()
        }

        /** Explicit sentence over a fact of the same key: it becomes that fact, permanent, new text. */
        private fun contradiction(i: Int, update: MemoryUpdate, text: String, recorded: RecordedMeal?) {
            val old = facts[i]
            val promote = !old.permanent && permanentRoom()
            val routine = update.category == ROUTINE
            val fact = old.copy(
                id = if (promote) newP() else old.id,
                kind = if (old.permanent || promote) PERMANENT else DYNAMIC,
                category = update.category,
                text = text,
                source = if (old.permanent || promote) EXPLICIT else old.source,
                days = seen(old.days),
                slot = if (routine) old.slot ?: update.slot else null,
                kcal = old.kcal.takeIf { routine },
                p = old.p.takeIf { routine },
                c = old.c.takeIf { routine },
                g = old.g.takeIf { routine },
            )
            facts[i] = withMeal(fact, recorded)
            count(REPLACE)
        }

        private fun reinforce(i: Int, text: String, recorded: RecordedMeal?) {
            if (i < 0) return
            val old = facts[i]
            // A permanent text changes only through replace.
            val newText = if (!old.permanent && text.isNotEmpty()) text else old.text
            facts[i] = withMeal(old.copy(text = newText, days = seen(old.days)), recorded)
            count(REINFORCE)
        }

        private fun replace(update: MemoryUpdate, text: String) {
            val i = indexOf(update.id)
            if (i < 0 || text.isEmpty()) return
            val old = facts[i]
            // No promotion from or to temp (A38).
            val promote = update.kind == PERMANENT && old.dynamic && permanentRoom()
            facts[i] = if (promote) {
                old.copy(id = newP(), kind = PERMANENT, source = EXPLICIT, text = text)
            } else {
                old.copy(text = text)
            }
            count(REPLACE)
            if (old.temp) count(TEMP_REPLACE)
        }

        private fun newFact(id: String, kind: String, update: MemoryUpdate, key: String, text: String, source: String): Fact {
            val meal = update.category in MEAL_CATEGORIES
            // A67: a declared routine counts no day; a routine or liked dish keeps the numbers the model estimated.
            val declared = update.declared && update.category == ROUTINE && kind == DYNAMIC
            return Fact(
                id = id,
                kind = kind,
                category = update.category,
                key = key,
                text = text,
                slot = update.slot.takeIf { meal },
                source = if (declared) DECLARED else source,
                days = if (declared) emptyList() else listOf(today),
                created = today,
                kcal = update.kcal.takeIf { meal },
                p = update.p.takeIf { meal },
                c = update.c.takeIf { meal },
                g = update.g.takeIf { meal },
                declared = declared,
            )
        }

        /** A routine takes the slot and macros of the record it came with. */
        private fun withMeal(fact: Fact, meal: RecordedMeal?): Fact {
            if (fact.category != ROUTINE || meal == null) return fact
            return fact.copy(slot = meal.slot, kcal = meal.kcal, p = meal.p, c = meal.c, g = meal.g)
        }

        private fun seen(days: List<String>) = if (today in days) days else (days + today).sorted()

        /** Above the limit, the dynamic fact seen longest ago leaves (oldest id on a tie). */
        private fun cap() {
            while (facts.count { it.dynamic } > DYNAMIC_MAX) {
                val oldest = facts.filter { it.dynamic }
                    .minWith(compareBy<Fact>({ it.lastSeen ?: "" }, { it.id.drop(1).toIntOrNull() ?: 0 }))
                facts.remove(oldest)
            }
        }

        /** Dynamic seen on ≥ 5 days of the window becomes permanent, when there is room; else it waits. */
        fun promote() {
            for (i in facts.indices) {
                val fact = facts[i]
                if (!fact.dynamic || fact.days.size < PROMOTE_DAYS || !permanentRoom()) continue
                facts[i] = fact.copy(id = newP(), kind = PERMANENT, source = PROMOTED)
                count(PROMOTE)
            }
        }
    }
}
