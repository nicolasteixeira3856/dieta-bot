package com.nutri.android.domain

import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One memory fact (ADR-023 decision 4). [days]: distinct America/Sao_Paulo dates the fact showed up,
 * only those of the last [MemoryRules.DYNAMIC_TTL_DAYS]. kcal/p/c/g: routine only, from the last record.
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
) {
    val permanent: Boolean get() = kind == MemoryRules.PERMANENT
    val lastSeen: String? get() = days.maxOrNull()
}

/** Next number of each id prefix: ids are never reused. */
@Serializable
data class NextIds(@SerialName("P") val p: Int = 1, @SerialName("D") val d: Int = 1)

@Serializable
data class Memory(val next: NextIds = NextIds(), val facts: List<Fact> = emptyList())

/** A proposal of the AI (server `memory_updates`). id is null only on add. */
data class MemoryUpdate(
    val op: String,
    val id: String?,
    val kind: String,
    val category: String,
    val key: String,
    val text: String,
    val slot: String? = null,
)

/** [revert] outcome: the memory and how many facts went back or were left as they are. */
data class RevertResult(val memory: Memory, val reverted: Int, val kept: Int)

/** The meal that was just recorded: a routine takes its slot and macros. */
data class RecordedMeal(val slot: String, val kcal: Int, val p: Int, val c: Int, val g: Int)

/**
 * Operations effectively applied, by name ([MemoryRules.OPS]). [images]: pre and post image of every fact
 * the operation changed (A34), filled by [com.nutri.android.core.memory.FactMemory]; expiry is not a change.
 */
data class MemoryResult(val memory: Memory, val counts: Map<String, Int>, val images: List<FactImage> = emptyList()) {
    /** At least one update of the AI was applied (expire alone does not count). */
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
    const val PERMANENT_MAX = 30
    const val DYNAMIC_MAX = 40
    const val DYNAMIC_TTL_DAYS = 21L
    const val PROMOTE_DAYS = 5
    const val STRONG_ROUTINE_DAYS = 3
    const val TEXT_MAX = 160
    const val KEY_MAX = 40

    const val PERMANENT = "permanent"
    const val DYNAMIC = "dynamic"
    const val ROUTINE = "routine"
    val CATEGORIES = setOf("preference", "portion", ROUTINE)

    const val EXPLICIT = "explicit"
    const val PROMOTED = "promoted"
    const val OBSERVED = "observed"

    const val ADD = "add"
    const val REINFORCE = "reinforce"
    const val REPLACE = "replace"
    const val REMOVE = "remove"
    const val PROMOTE = "promote"
    const val EXPIRE = "expire"
    val OPS = listOf(ADD, REINFORCE, REPLACE, REMOVE, PROMOTE, EXPIRE)

    /**
     * Days older than the 21-day window leave; a dynamic fact with no day left leaves (a fact seen on
     * day 1 and never again is gone on day 22). Permanent never expires.
     */
    fun expire(memory: Memory, today: LocalDate): MemoryResult {
        val from = today.minusDays(DYNAMIC_TTL_DAYS - 1).toString()
        var expired = 0
        val facts = memory.facts.mapNotNull { fact ->
            val days = fact.days.filter { it >= from }
            if (!fact.permanent && days.isEmpty()) {
                expired++
                null
            } else {
                fact.copy(days = days)
            }
        }
        return MemoryResult(memory.copy(facts = facts), mapOf(EXPIRE to expired))
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

    /** Every fact that differs between [before] and [after], by id (A34). */
    fun images(before: Memory, after: Memory): List<FactImage> {
        val old = before.facts.associateBy { it.id }
        val new = after.facts.associateBy { it.id }
        return (old.keys + new.keys).filter { old[it] != new[it] }.map { FactImage(it, old[it], new[it]) }
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
        for (image in images) {
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

    private class State(memory: Memory, val today: String) {
        val facts = memory.facts.toMutableList()
        var nextP = memory.next.p
        var nextD = memory.next.d
        val counts = OPS.filter { it != EXPIRE }.associateWith { 0 }.toMutableMap()

        fun memory() = Memory(NextIds(nextP, nextD), facts.toList())

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
                ADD -> if (key.isNotEmpty() && text.isNotEmpty()) add(update, key, text, recorded)
                REINFORCE -> reinforce(indexOf(update.id), text, recorded)
                REPLACE -> replace(update, text)
                REMOVE -> {
                    val i = indexOf(update.id)
                    if (i >= 0) {
                        facts.removeAt(i)
                        count(REMOVE)
                    }
                }
            }
        }

        private fun indexOf(id: String?) = if (id == null) -1 else facts.indexOfFirst { it.id == id }

        private fun add(update: MemoryUpdate, key: String, text: String, recorded: RecordedMeal?) {
            val same = facts.indexOfFirst { sameKey(it.key, key) }
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
            val promote = update.kind == PERMANENT && !old.permanent && permanentRoom()
            facts[i] = if (promote) {
                old.copy(id = newP(), kind = PERMANENT, source = EXPLICIT, text = text)
            } else {
                old.copy(text = text)
            }
            count(REPLACE)
        }

        private fun newFact(id: String, kind: String, update: MemoryUpdate, key: String, text: String, source: String) = Fact(
            id = id,
            kind = kind,
            category = update.category,
            key = key,
            text = text,
            slot = update.slot.takeIf { update.category == ROUTINE },
            source = source,
            days = listOf(today),
            created = today,
        )

        /** A routine takes the slot and macros of the record it came with. */
        private fun withMeal(fact: Fact, meal: RecordedMeal?): Fact {
            if (fact.category != ROUTINE || meal == null) return fact
            return fact.copy(slot = meal.slot, kcal = meal.kcal, p = meal.p, c = meal.c, g = meal.g)
        }

        private fun seen(days: List<String>) = if (today in days) days else (days + today).sorted()

        /** Above the limit, the dynamic fact seen longest ago leaves (oldest id on a tie). */
        private fun cap() {
            while (facts.count { !it.permanent } > DYNAMIC_MAX) {
                val oldest = facts.filter { !it.permanent }
                    .minWith(compareBy<Fact>({ it.lastSeen ?: "" }, { it.id.drop(1).toIntOrNull() ?: 0 }))
                facts.remove(oldest)
            }
        }

        /** Dynamic seen on ≥ 5 days of the window becomes permanent, when there is room; else it waits. */
        fun promote() {
            for (i in facts.indices) {
                val fact = facts[i]
                if (fact.permanent || fact.days.size < PROMOTE_DAYS || !permanentRoom()) continue
                facts[i] = fact.copy(id = newP(), kind = PERMANENT, source = PROMOTED)
                count(PROMOTE)
            }
        }
    }
}
