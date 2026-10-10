package app.fibrai.android.domain

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One meal of a closed day as `/v1/close` reads it: status empty | eaten | skipped | planned, kcal when it has any. */
@Serializable
data class ClosureSlot(val name: String, val status: String, val kcal: Int? = null)

/** The totals of one day (A60 part B, ADR-044): eaten, the effective ceiling of that day and its workout. */
@Serializable
data class ClosureDay(
    val date: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("workout_kcal") val workoutKcal: Int? = null,
    val slots: List<ClosureSlot> = emptyList(),
) {
    val recorded: Boolean get() = slots.any { it.status == "eaten" } || kcal > 0
}

@Serializable
data class ClosureWeekDay(
    val date: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("workout_kcal") val workoutKcal: Int? = null,
    val recorded: Boolean,
)

@Serializable
data class ClosureOverSlot(val name: String, val days: Int)

@Serializable
data class ClosureWeek(
    val days: List<ClosureWeekDay>,
    @SerialName("over_slot") val overSlot: ClosureOverSlot? = null,
) {
    private val recordedDays get() = days.filter { it.recorded }
    val total: Int get() = recordedDays.sumOf { it.kcal }
    val meanKcal: Int get() = recordedDays.takeIf { it.isNotEmpty() }?.let { (total.toDouble() / it.size).roundToInt() } ?: 0
    val meanP: Int get() = recordedDays.takeIf { it.isNotEmpty() }?.let { (it.sumOf { d -> d.p }.toDouble() / it.size).roundToInt() } ?: 0
    val unrecorded: Int get() = days.count { !it.recorded }
}

/** A meal of a day as the closure reads it from Room: its slot, its records and the slot's state. */
data class ClosureMeal(val slotId: Long, val name: String, val kcal: Int, val skipped: Boolean, val plannedKcal: Int?, val eaten: Boolean)

/**
 * Day and week closures (A60 part B, ADR-044): the numbers are the app's, computed from Room; the server writes prose over
 * them. Pure: no Android, no network.
 */
object Closures {
    const val DAY = "day"
    const val WEEK = "week"

    /** 22:00 America/Sao_Paulo by default, every day; the week closes on Sunday at the same time. */
    val AT: LocalTime = LocalTime.of(22, 0)

    /** A71 (ADR-057 decision 10): the profile's closure time `HH:mm`; anything else is [AT]. */
    fun time(hhmm: String?): LocalTime = runCatching { LocalTime.parse(hhmm) }.getOrNull() ?: AT

    /** Text statuses of a stored closure. */
    const val TEXT = "text"
    const val FALLBACK = "fallback"
    const val OFFLINE = "offline"
    const val EMPTY = "empty"

    fun dayKey(date: LocalDate) = "day:$date"

    /** The week of [date]: Monday to Sunday (ISO), keyed by its Monday. */
    fun weekKey(date: LocalDate) = "week:${monday(date)}"

    fun monday(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * When the next closure alarm fires: 22:00 today, or now when it passed and the day's closure is still missing
     * ([dayDone] false), else 22:00 tomorrow.
     */
    fun nextAlarm(now: Instant, dayDone: Boolean, closureAt: LocalTime = AT): Instant {
        val today = SaoPaulo.date(now)
        val at = today.atTime(closureAt).atZone(SaoPaulo.zone).toInstant()
        return when {
            now.isBefore(at) -> at
            !dayDone -> now
            else -> today.plusDays(1).atTime(closureAt).atZone(SaoPaulo.zone).toInstant()
        }
    }

    /** The periods an alarm at [now] closes: the day from the closure time on, and the week on Sunday. */
    fun duePeriods(now: Instant, closureAt: LocalTime = AT): List<String> {
        val local = now.atZone(SaoPaulo.zone)
        if (local.toLocalTime().isBefore(closureAt)) return emptyList()
        return if (local.dayOfWeek == DayOfWeek.SUNDAY) listOf(DAY, WEEK) else listOf(DAY)
    }

    /** One day from its meals in slot order and its totals. [ceilingKcal]: the effective ceiling of that day. */
    fun day(date: LocalDate, meals: List<ClosureMeal>, totals: Macros, ceilingKcal: Int, workoutKcal: Int?): ClosureDay = ClosureDay(
        date = date.toString(),
        kcal = totals.kcal,
        p = totals.p,
        c = totals.c,
        g = totals.g,
        ceilingKcal = ceilingKcal,
        workoutKcal = workoutKcal?.takeIf { it > 0 },
        slots = meals.map { m ->
            when {
                m.eaten -> ClosureSlot(m.name, "eaten", m.kcal)
                m.skipped -> ClosureSlot(m.name, "skipped")
                m.plannedKcal != null -> ClosureSlot(m.name, "planned", m.plannedKcal)
                else -> ClosureSlot(m.name, "empty")
            }
        },
    )

    /**
     * The week of [days] (Monday first, at most 7). The meal that most often went over its share: kcal above the day's
     * ceiling divided by its meals; null when none did.
     */
    fun week(days: List<ClosureDay>): ClosureWeek {
        val over = mutableMapOf<String, Int>()
        for (d in days) {
            val share = if (d.slots.isEmpty()) 0 else d.ceilingKcal / d.slots.size
            d.slots.filter { it.status == "eaten" && (it.kcal ?: 0) > share }.forEach { over[it.name] = (over[it.name] ?: 0) + 1 }
        }
        val top = over.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).firstOrNull()
        return ClosureWeek(
            days = days.map { ClosureWeekDay(it.date, it.kcal, it.p, it.c, it.g, it.ceilingKcal, it.workoutKcal, it.recorded) },
            overSlot = top?.let { ClosureOverSlot(it.key, it.value) },
        )
    }

    /** A server text that is the fixed neutral line of a refusal or a failure (server closure.fallback). */
    fun isFallback(text: String): Boolean = text.startsWith("Dia fechado.") || text.startsWith("Semana fechada.")

    // ------------------------------------------------------------------ cards (homeC, homeK)

    /**
     * How a day card shows on [today] (ADR-044 decision 2): its own day, expanded; yesterday's, expanded until the first
     * record of today, then one line; anything older, not at all.
     */
    fun dayCard(closed: LocalDate, today: LocalDate, todayHasRecord: Boolean): CardState = when (closed) {
        today -> CardState.EXPANDED
        today.minusDays(1) -> if (todayHasRecord) CardState.COLLAPSED else CardState.EXPANDED
        else -> CardState.HIDDEN
    }

    /** A week card (closed on Sunday): expanded until Tuesday 00:00, then one line until the next Sunday. */
    fun weekCard(monday: LocalDate, today: LocalDate): CardState {
        val sunday = monday.plusDays(6)
        return when {
            today.isBefore(sunday) -> CardState.HIDDEN
            !today.isAfter(sunday.plusDays(1)) -> CardState.EXPANDED
            today.isBefore(sunday.plusDays(7)) -> CardState.COLLAPSED
            else -> CardState.HIDDEN
        }
    }

    enum class CardState { EXPANDED, COLLAPSED, HIDDEN }

    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d 'de' MMMM", ptBr)

    fun dayTitle(date: LocalDate) = "Fechamento de ${date.format(DAY_MONTH)}"

    /** "Semana de 28 de setembro a 4 de outubro". */
    fun weekTitle(monday: LocalDate, sunday: LocalDate) = "Semana de ${monday.format(DAY_MONTH)} a ${sunday.format(DAY_MONTH)}"

    /** Thousands with a dot ("13.420"), as the week card. */
    fun thousands(n: Int): String = NumberFormat.getIntegerInstance(ptBr).format(n)

    /** "Pulado: Lanche · Sem registro: Jantar"; null when every meal has a record. A planned meal is missing too. */
    fun missingLine(day: ClosureDay): String? {
        val skipped = day.slots.filter { it.status == "skipped" }.map { it.name }
        val missing = day.slots.filter { it.status == "empty" || it.status == "planned" }.map { it.name }
        return listOfNotNull(
            skipped.takeIf { it.isNotEmpty() }?.let { "Pulado: ${it.joinToString(", ")}" },
            missing.takeIf { it.isNotEmpty() }?.let { "Sem registro: ${it.joinToString(", ")}" },
        ).joinToString(" · ").ifEmpty { null }
    }

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
}
