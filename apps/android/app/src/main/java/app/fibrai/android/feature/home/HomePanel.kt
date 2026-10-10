package app.fibrai.android.feature.home

import androidx.compose.runtime.Immutable
import app.fibrai.android.core.database.ClosureEntity
import app.fibrai.android.core.database.slotsOn
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.MealLog
import app.fibrai.android.core.database.metaOn
import app.fibrai.android.domain.ClosureDay
import app.fibrai.android.domain.ClosureMeal
import app.fibrai.android.domain.ClosureWeek
import app.fibrai.android.domain.Closures
import app.fibrai.android.domain.Macros
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.feature.workout.WorkoutEditorState
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

    /** A60 part D (homeP): a plan reserved for the meal; nothing eaten, not in the ring. */
    PLANNED,

    /** A72 (ADR-058 decision 2): something eaten outside the meals, a node of its own at its time. */
    EXTRA,
}

/** A72 (D28 `Home/DayCircle`): the five states of a day of the strip. */
enum class DayCircleState { TODAY_SELECTED, TODAY, PAST_SELECTED, PAST, NO_RECORD }

/** One day of the strip: its number, the month above it where it changes, its state and the accessibility label. */
@Immutable
data class StripDay(val date: LocalDate, val day: String, val month: String?, val state: DayCircleState, val label: String)

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
    /** A72: an extra's text (Card/Extra), its [time] in the title. */
    val extraText: String? = null,
) {
    /** Consolidated meal line, e.g. "520 kcal · 28P · 52C · 22G". */
    val summary: String get() = "$kcal kcal · ${p}P · ${c}C · ${g}G"
}

/**
 * A closure card (A60 part B, homeC / homeK): [expanded] shows the title, the numbers, the detail lines and the server
 * text; collapsed, only [line]. Tap on a collapsed card expands it.
 */
@Immutable
data class ClosureCard(
    val key: String,
    val period: String,
    val expanded: Boolean,
    val title: String,
    /** "1300 de 2175 kcal" (day) or "13.420 kcal · média 1.917 kcal/dia" (week). */
    val kcalLine: String,
    /** Day only: P/C/G eaten over their targets, in the macro colours. */
    val macros: List<MacroLine>? = null,
    /** "Pulado: Lanche · Sem registro: Jantar" (day) or "Proteína: média 118 g/dia · Dias sem registro: 1" (week). */
    val detail: String? = null,
    /** "Treino: 350 kcal" (day) or "Jantar passou da janela em 4 dias" (week). */
    val extra: String? = null,
    /** The server text, or what stands in for it (no network, no record). */
    val text: String,
    /** Collapsed: "Ontem: 1300 de 2175 kcal". */
    val line: String,
)

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
    /** A60 part B: the week card above the day card, between the workout row and the timeline. */
    val closures: List<ClosureCard> = emptyList(),
    /** A72 (ADR-058 decision 3): the last 30 days (or since the first day), oldest first, today last. */
    val strip: List<StripDay> = emptyList(),
    /** A72 (homeH): a past day is shown: read only, no gestures, `Treino do dia`. */
    val past: Boolean = false,
) {
    val over: Int get() = (consumed - meta).coerceAtLeast(0)
    val ringFraction: Float get() = if (meta <= 0) 1f else (consumed.toFloat() / meta).coerceIn(0f, 1f)
}

object HomePanelMapper {
    private val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

    /**
     * [workoutDraft] = field of the open workout sheet, null when closed. [closures]: the stored closures of the last
     * days; [expanded]: the keys of collapsed cards the user tapped open.
     */
    fun map(
        day: DaySnapshot,
        today: LocalDate,
        workoutDraft: String? = null,
        closures: List<ClosureEntity> = emptyList(),
        expanded: Set<String> = emptySet(),
        /** A72: the day shown (a past day of the strip); null = today. */
        shown: LocalDate? = null,
        /** A72: kcal of each day with records, for the strip; null = no strip (older renders). */
        dayKcal: Map<String, Int>? = null,
    ): HomePanelUiState {
        if (shown != null && shown != today) return mapPast(day, today, shown, closures, dayKcal.orEmpty())
        return mapToday(day, today, workoutDraft, closures, expanded).copy(strip = dayKcal?.let { strip(day, today, today, it) }.orEmpty())
    }

    private fun mapToday(
        day: DaySnapshot,
        today: LocalDate,
        workoutDraft: String?,
        closures: List<ClosureEntity>,
        expanded: Set<String>,
    ): HomePanelUiState {
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
            timeline = timeline(day, meta, today),
            slotCount = day.slotsOn(today).size,
            workoutKcal = day.workoutKcal,
            workoutCredit = stored.credit,
            workoutEditor = workoutDraft?.let { stored.copy(input = it) },
            closures = closureCards(day, today, closures, expanded),
        )
    }

    /**
     * A72 (homeH): a past day from Room with that day's ceiling and credit, its closure card (expanded, when it exists) and
     * its timeline in that day's group, read only.
     */
    private fun mapPast(day: DaySnapshot, today: LocalDate, shown: LocalDate, closures: List<ClosureEntity>, dayKcal: Map<String, Int>): HomePanelUiState {
        val base = mapToday(day, shown, null, emptyList(), emptySet())
        val card = closures.firstOrNull { it.period == Closures.DAY && it.date == shown.toString() }?.let { row ->
            decodeDay(row.numbers)?.let { n ->
                dayCard(row, n, shown, today, Closures.CardState.EXPANDED, emptySet(), Triple(day.proteinTargetG, day.carbTargetG, day.fatTargetG))
            }
        }
        return base.copy(
            timeline = base.timeline.map { if (it.state == SlotState.NEXT) it.copy(state = SlotState.EMPTY) else it },
            closures = listOfNotNull(card),
            strip = strip(day, today, shown, dayKcal),
            past = true,
        )
    }

    private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-BR"))
    private val fullFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

    /** A72 (ADR-058 decision 3): the last 30 days, never before the first day of the app; the month where it changes. */
    fun strip(day: DaySnapshot, today: LocalDate, selected: LocalDate, dayKcal: Map<String, Int>): List<StripDay> {
        val first = day.firstDay.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: today
        val from = maxOf(today.minusDays(STRIP_DAYS - 1), minOf(first, today))
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.mapIndexed { i, date ->
            val kcal = dayKcal[date.toString()]
            val state = when {
                date == today && selected == today -> DayCircleState.TODAY_SELECTED
                date == today -> DayCircleState.TODAY
                date == selected -> DayCircleState.PAST_SELECTED
                kcal == null -> DayCircleState.NO_RECORD
                else -> DayCircleState.PAST
            }
            StripDay(
                date = date,
                day = date.dayOfMonth.toString(),
                month = if (i == 0 || date.dayOfMonth == 1) date.format(monthFormat).trimEnd('.').replaceFirstChar { it.uppercase() } else null,
                state = state,
                label = "${date.format(fullFormat)}, ${kcal ?: 0} kcal",
            )
        }.toList()
    }

    // ------------------------------------------------------------------ closures (A60 part B, ADR-044)

    private fun closureCards(day: DaySnapshot, today: LocalDate, rows: List<ClosureEntity>, expanded: Set<String>): List<ClosureCard> {
        val week = rows.filter { it.period == Closures.WEEK }.maxByOrNull { it.date }?.let { row ->
            val state = Closures.weekCard(LocalDate.parse(row.date), today)
            weekCard(row, state, expanded)
        }
        // Today's closure replaces yesterday's; yesterday's stays until the first record of today, then one line.
        val days = rows.filter { it.period == Closures.DAY }
        val dayRow = days.firstOrNull { it.date == today.toString() } ?: days.firstOrNull { it.date == today.minusDays(1).toString() }
        val dayCard = dayRow?.let { row ->
            val closed = LocalDate.parse(row.date)
            val state = Closures.dayCard(closed, today, todayHasRecord = day.logs.isNotEmpty())
            // A later record of today updates the numbers of today's card from Room, not the text.
            val numbers = if (closed == today) liveDay(day, today) else decodeDay(row.numbers)
            numbers?.let { dayCard(row, it, closed, today, state, expanded, Triple(day.proteinTargetG, day.carbTargetG, day.fatTargetG)) }
        }
        return listOfNotNull(week, dayCard)
    }

    private fun dayCard(
        row: ClosureEntity,
        n: ClosureDay,
        closed: LocalDate,
        today: LocalDate,
        state: Closures.CardState,
        expanded: Set<String>,
        targets: Triple<Int, Int, Int>,
    ): ClosureCard? {
        if (state == Closures.CardState.HIDDEN) return null
        val kcalLine = "${n.kcal} de ${n.ceilingKcal} kcal"
        return ClosureCard(
            key = row.key,
            period = row.period,
            expanded = state == Closures.CardState.EXPANDED || row.key in expanded,
            title = Closures.dayTitle(closed),
            kcalLine = kcalLine,
            macros = listOf(MacroLine(n.p, targets.first), MacroLine(n.c, targets.second), MacroLine(n.g, targets.third)),
            detail = Closures.missingLine(n),
            extra = n.workoutKcal?.let { "Treino: $it kcal" },
            text = cardText(row),
            line = (if (closed == today) "Hoje: " else "Ontem: ") + kcalLine,
        )
    }

    private fun weekCard(row: ClosureEntity, state: Closures.CardState, expanded: Set<String>): ClosureCard? {
        if (state == Closures.CardState.HIDDEN) return null
        val n = runCatching { Closures.json.decodeFromString(ClosureWeek.serializer(), row.numbers) }.getOrNull() ?: return null
        val monday = LocalDate.parse(row.date)
        val kcalLine = "${Closures.thousands(n.total)} kcal · média ${Closures.thousands(n.meanKcal)} kcal/dia"
        return ClosureCard(
            key = row.key,
            period = row.period,
            expanded = state == Closures.CardState.EXPANDED || row.key in expanded,
            title = Closures.weekTitle(monday, monday.plusDays(6)),
            kcalLine = kcalLine,
            detail = "Proteína: média ${n.meanP} g/dia · Dias sem registro: ${n.unrecorded}",
            extra = n.overSlot?.let { "${it.name} passou da janela em ${it.days} ${if (it.days == 1) "dia" else "dias"}" },
            text = cardText(row),
            line = "Semana: $kcalLine",
        )
    }

    private fun cardText(row: ClosureEntity): String = when (row.status) {
        Closures.EMPTY -> "Nenhum registro."
        Closures.OFFLINE -> "Sem o texto: sem rede."
        else -> row.text ?: "Sem o texto: sem rede."
    }

    private fun decodeDay(numbers: String): ClosureDay? =
        runCatching { Closures.json.decodeFromString(ClosureDay.serializer(), numbers) }.getOrNull()

    /** Today's numbers as the closure computes them, from the snapshot. */
    fun liveDay(day: DaySnapshot, today: LocalDate): ClosureDay {
        val meals = day.slotsOn(today).sortedBy { it.minutesFromMidnight }.map { slot ->
            val logs = day.logs.filter { it.slotId == slot.id }
            ClosureMeal(slot.id, slot.name, logs.sumOf { it.kcal }, slot.id in day.skippedSlotIds, day.planned[slot.id]?.kcal, logs.isNotEmpty())
        }
        val totals = Macros(day.logs.sumOf { it.kcal }, day.logs.sumOf { it.p }, day.logs.sumOf { it.carbs }, day.logs.sumOf { it.fat })
        return Closures.day(today, meals, totals, day.metaOn(today), day.workoutKcal)
    }

    /**
     * A72 (ADR-058 decision 2): the day's slots and extras merged by time (a slot by its profile time, an extra by its own;
     * on a tie the meal first); the over-the-meta rule walks the merged list. `Outros` keeps the logs of removed or foreign slots.
     */
    private fun timeline(day: DaySnapshot, meta: Int, today: LocalDate): List<TimelineSlot> {
        val known = day.slotsOn(today).map { it.id }.toSet()
        val slots = day.slotsOn(today).sortedBy { it.minutesFromMidnight }
        val lastFilled = slots.indexOfLast { s -> s.id in day.skippedSlotIds || day.logs.any { it.slotId == s.id } }
        val extras = day.logs.filter { it.extra }
        val entries = slots.mapIndexed { i, slot -> Triple(slot.minutesFromMidnight, 0, i) } +
            extras.mapIndexed { i, log -> Triple(minutesOf(log.time), 1, i) }
        var running = 0
        val out = entries.sortedWith(compareBy({ it.first }, { it.second })).map { (_, kind, i) ->
            if (kind == 1) {
                val log = extras[i]
                running += log.kcal
                return@map TimelineSlot(
                    slotId = null,
                    name = "Extra · ${log.time.orEmpty()}".removeSuffix(" · "),
                    time = log.time,
                    state = SlotState.EXTRA,
                    kcal = log.kcal,
                    p = log.p,
                    c = log.carbs,
                    g = log.fat,
                    extraText = log.text,
                )
            }
            val slot = slots[i]
            val logs = day.logs.filter { it.slotId == slot.id }
            val time = SlotSuggestions.format(slot.minutesFromMidnight)
            when {
                logs.isNotEmpty() -> {
                    running += logs.sumOf { it.kcal }
                    filled(slot.id, slot.name, time, logs.map { LogLine(it.text, it.kcal) }, logs, running > meta)
                }
                slot.id in day.skippedSlotIds -> TimelineSlot(slot.id, slot.name, time, SlotState.SKIPPED)
                day.planned[slot.id] != null -> day.planned.getValue(slot.id).let { plan ->
                    TimelineSlot(slot.id, slot.name, time, SlotState.PLANNED, lines = listOf(LogLine(plan.text, plan.kcal)), kcal = plan.kcal)
                }
                else -> TimelineSlot(slot.id, slot.name, time, if (lastFilled >= 0 && i == lastFilled + 1) SlotState.NEXT else SlotState.EMPTY)
            }
        }
        val orphans = day.logs.filter { !it.extra && (it.slotId == null || it.slotId !in known) }
        if (orphans.isEmpty()) return out
        running += orphans.sumOf { it.kcal }
        return out + filled(null, "Outros", null, orphans.map { LogLine(it.text, it.kcal) }, orphans, running > meta)
    }

    /** `HH:mm` to minutes; an extra without a time goes last. */
    private fun minutesOf(time: String?): Int =
        time?.split(':')?.takeIf { it.size == 2 }?.let { (h, m) -> (h.toIntOrNull() ?: return@let null)?.times(60)?.plus(m.toIntOrNull() ?: 0) } ?: Int.MAX_VALUE

    /** A72: the strip shows 30 days. */
    const val STRIP_DAYS = 30L

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
