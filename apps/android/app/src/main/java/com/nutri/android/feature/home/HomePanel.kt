package com.nutri.android.feature.home

import androidx.compose.runtime.Immutable
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.MealLog
import com.nutri.android.core.database.metaOn
import com.nutri.android.domain.SlotSuggestions
import com.nutri.android.feature.workout.WorkoutEditorState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Immutable
data class MacroLine(val consumed: Int, val target: Int) {
    val over: Boolean get() = consumed > target
    val fraction: Float get() = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f)
}

@Immutable
data class LogLine(val text: String, val kcal: Int)

enum class SlotState {
    /** Has logs, day still under the meta. */
    LOGGED,

    /** Has logs and this slot pushed the day over the meta (or came after it did). */
    OVER,
    SKIPPED,

    /** No logs, no skip: the next one to fill (after the last filled slot). */
    NEXT,
    EMPTY,
}

@Immutable
data class TimelineSlot(
    /** Null = "Outros" (logs without a slot). */
    val slotId: Long?,
    val name: String,
    val time: String?,
    val state: SlotState,
    val lines: List<LogLine> = emptyList(),
    val kcal: Int = 0,
    val p: Int = 0,
    val c: Int = 0,
    val g: Int = 0,
    val fromPhoto: Boolean = false,
) {
    /** Consolidated meal line, e.g. "520 kcal · 28P · 52C · 22G". */
    val summary: String get() = "$kcal kcal · ${p}P · ${c}C · ${g}G"
}

@Immutable
data class HomePanelUiState(
    val dayLabel: String = "DIA 1",
    val dateLabel: String = "",
    val consumed: Int = 0,
    val meta: Int = 2000,
    val protein: MacroLine = MacroLine(0, 150),
    val carbs: MacroLine = MacroLine(0, 200),
    val fat: MacroLine = MacroLine(0, 67),
    val timeline: List<TimelineSlot> = emptyList(),
    val slotCount: Int = 0,
    /** Null = no workout today: the row reads "Informar". */
    val workoutKcal: Int? = null,
    val workoutCredit: Int = 0,
    /** A22: "Treino de hoje" sheet (homeW). Null = closed. */
    val workoutEditor: WorkoutEditorState? = null,
) {
    val over: Int get() = (consumed - meta).coerceAtLeast(0)
    val ringFraction: Float get() = if (meta <= 0) 1f else (consumed.toFloat() / meta).coerceIn(0f, 1f)
}

object HomePanelMapper {
    private val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

    /** [workoutDraft] = field of the open workout sheet, null when closed. */
    fun map(day: DaySnapshot, today: LocalDate, workoutDraft: String? = null): HomePanelUiState {
        val first = day.firstDay.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: today
        val appDay = (ChronoUnit.DAYS.between(first, today) + 1).coerceAtLeast(1)
        val meta = day.metaOn(today)

        val consumed = day.logs.sumOf { it.kcal }
        val policy = WorkoutEditorState.policyOf(day.eat)
        val stored = WorkoutEditorState(day.workoutKcal?.toString().orEmpty(), policy, day.pct)
        return HomePanelUiState(
            dayLabel = "DIA $appDay",
            dateLabel = today.format(dateFormat),
            consumed = consumed,
            meta = meta,
            protein = MacroLine(day.logs.sumOf { it.p }, day.proteinTargetG),
            carbs = MacroLine(day.logs.sumOf { it.carbs }, day.carbTargetG),
            fat = MacroLine(day.logs.sumOf { it.fat }, day.fatTargetG),
            timeline = timeline(day, meta),
            slotCount = day.slots.size,
            workoutKcal = day.workoutKcal,
            workoutCredit = stored.credit,
            workoutEditor = workoutDraft?.let { stored.copy(input = it) },
        )
    }

    private fun timeline(day: DaySnapshot, meta: Int): List<TimelineSlot> {
        val known = day.slots.map { it.id }.toSet()
        val slots = day.slots.sortedBy { it.minutesFromMidnight }
        val lastFilled = slots.indexOfLast { s -> s.id in day.skippedSlotIds || day.logs.any { it.slotId == s.id } }
        var running = 0
        val out = slots.mapIndexed { i, slot ->
            val logs = day.logs.filter { it.slotId == slot.id }
            val time = SlotSuggestions.format(slot.minutesFromMidnight)
            when {
                logs.isNotEmpty() -> {
                    running += logs.sumOf { it.kcal }
                    filled(slot.id, slot.name, time, logs.map { LogLine(it.text, it.kcal) }, logs, running > meta)
                }
                slot.id in day.skippedSlotIds -> TimelineSlot(slot.id, slot.name, time, SlotState.SKIPPED)
                else -> TimelineSlot(slot.id, slot.name, time, if (lastFilled >= 0 && i == lastFilled + 1) SlotState.NEXT else SlotState.EMPTY)
            }
        }
        val orphans = day.logs.filter { it.slotId == null || it.slotId !in known }
        if (orphans.isEmpty()) return out
        running += orphans.sumOf { it.kcal }
        return out + filled(null, "Outros", null, orphans.map { LogLine(it.text, it.kcal) }, orphans, running > meta)
    }

    private fun filled(
        id: Long?,
        name: String,
        time: String?,
        lines: List<LogLine>,
        logs: List<MealLog>,
        over: Boolean,
    ) = TimelineSlot(
        slotId = id,
        name = name,
        time = time,
        state = if (over) SlotState.OVER else SlotState.LOGGED,
        lines = lines,
        kcal = logs.sumOf { it.kcal },
        p = logs.sumOf { it.p },
        c = logs.sumOf { it.carbs },
        g = logs.sumOf { it.fat },
        fromPhoto = logs.any { it.source == "photo" },
    )
}
