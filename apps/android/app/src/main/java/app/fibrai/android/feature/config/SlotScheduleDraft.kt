package app.fibrai.android.feature.config

import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.domain.SlotModes
import app.fibrai.android.domain.SlotSuggestions

/** Shared O3/Config editing state. Stored schedules change only on the final save. */
data class SlotScheduleDraft(
    val mode: String = "same",
    val index: Int = 0,
    val drafts: Map<Int, List<SlotDraft>> = emptyMap(),
    val pendingMode: String? = null,
) {
    val groups get() = SlotModes.groups(mode)
    val group get() = groups[index]
    val slots get() = drafts[group.days] ?: defaults()
    val last get() = index == groups.lastIndex
    val valid get() = groups.all { validSlots(drafts[it.days].orEmpty()) }
    val discardedGroups get() = groups.filter { old ->
        pendingMode != null && SlotModes.groups(pendingMode).none { it.days == old.days } &&
            drafts[old.days]?.let { rows -> rows.any { it.name.isNotBlank() || it.id != 0L } || rows != defaults() } == true
    }

    fun withSlots(rows: List<SlotDraft>) = copy(drafts = drafts + (group.days to rows.toList()))
    fun requestMode(value: String): SlotScheduleDraft {
        if (value == mode || SlotModes.labels.none { it.first == value }) return this
        val pending = copy(pendingMode = value)
        return if (pending.discardedGroups.isEmpty()) pending.confirmMode() else pending
    }
    fun confirmMode(): SlotScheduleDraft {
        val next = pendingMode ?: return this
        val kept = SlotModes.groups(next).map { it.days }.toSet()
        return copy(mode = next, index = 0, drafts = drafts.filterKeys { it in kept }, pendingMode = null)
    }
    fun copyPrevious(): SlotScheduleDraft {
        if (index == 0) return this
        val previous = drafts[groups[index - 1].days] ?: return this
        // Copy values, retain only the destination IDs so existing logs stay attached there.
        return withSlots(previous.mapIndexed { i, row -> row.copy(id = slots.getOrNull(i)?.id ?: 0) })
    }
    fun meals(): List<MealSlot> = groups.flatMap { group ->
        drafts[group.days].orEmpty().sortedBy { it.minutes }.map {
            MealSlot(it.id, it.name.trim(), it.minutes, group.days)
        }
    }

    companion object {
        fun defaults() = SlotSuggestions.defaultTimes(4).map { SlotDraft(minutes = it) }
        fun validSlots(rows: List<SlotDraft>) = rows.size in SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS && rows.all { it.name.isNotBlank() }
        fun stored(mode: String, slots: List<MealSlot>) = SlotScheduleDraft(
            mode = mode,
            drafts = slots.groupBy { it.days }.mapValues { (_, rows) ->
                rows.sortedBy { it.minutesFromMidnight }.map { SlotDraft(it.id, it.name, it.minutesFromMidnight) }
            },
        )
    }
}

@androidx.compose.runtime.Immutable
data class SlotDraft(
    val id: Long = 0,
    val name: String = "",
    val minutes: Int,
)
