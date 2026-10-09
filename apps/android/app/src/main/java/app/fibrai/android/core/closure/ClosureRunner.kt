package app.fibrai.android.core.closure

import app.fibrai.android.core.database.ClosureEntity
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.ceilingProfile
import app.fibrai.android.core.database.slotsOn
import app.fibrai.android.core.network.ChatSlot
import app.fibrai.android.core.network.CloseIn
import app.fibrai.android.core.network.CloseOut
import app.fibrai.android.core.network.CloseProfile
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.ClosureDay
import app.fibrai.android.domain.ClosureWeek
import app.fibrai.android.domain.Closures
import app.fibrai.android.domain.Macros
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SlotSuggestions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The only network call of the closures (A60 part B). */
fun interface CloseService {
    suspend fun close(body: CloseIn): CloseOut
}

/** Posts the closure notification (A60 part B); a no-op in JVM tests. */
fun interface ClosureNotifier {
    fun notify(period: String, content: String)
}

/**
 * Day and week closures (A60 part B, ADR-044): at 22:00 SP (Sunday also the week) the app computes the numbers from Room,
 * asks `POST /v1/close` for the text in the user's tone, stores `{numbers, text}` once per day or week and notifies. No
 * network: the numbers alone, `Sem o texto: sem rede.`, and one retry at the next app start. The closure records nothing.
 */
@Singleton
class ClosureRunner @Inject constructor(
    private val repository: DayRepository,
    private val service: CloseService,
    private val clock: InstantClock,
    private val notifier: ClosureNotifier,
    private val telemetry: Telemetry = NoopTelemetry,
) {
    private val mutex = Mutex()

    /** What the alarm at [now] closes; nothing before 22:00. Returns the closures produced. */
    suspend fun runDue(): List<ClosureEntity> {
        val now = clock.now()
        return Closures.duePeriods(now).mapNotNull { run(it) }
    }

    /**
     * Closes [period] now: the day of today, or the week (Monday to today) that today belongs to. Null when that
     * closure already exists (a closure is produced once) or before the onboarding.
     */
    suspend fun run(period: String): ClosureEntity? = mutex.withLock {
        val now = clock.now()
        val today = SaoPaulo.date(now)
        val snapshot = repository.observeToday().first()
        if (!snapshot.onboardingDone) return@withLock null
        val key = if (period == Closures.WEEK) Closures.weekKey(today) else Closures.dayKey(today)
        if (repository.closure(key) != null) return@withLock null
        val day = if (period == Closures.WEEK) null else dayNumbers(snapshot, today)
        val week = if (period == Closures.WEEK) weekNumbers(snapshot, Closures.monday(today), today) else null
        val numbers = day?.let { Closures.json.encodeToString(ClosureDay.serializer(), it) }
            ?: Closures.json.encodeToString(ClosureWeek.serializer(), week!!)
        val recorded = day?.recorded ?: week!!.days.any { it.recorded }
        val row = ClosureEntity(
            key = key,
            period = period,
            date = if (period == Closures.WEEK) Closures.monday(today).toString() else today.toString(),
            numbers = numbers,
            status = if (recorded) Closures.OFFLINE else Closures.EMPTY,
            createdAtEpochMs = now.toEpochMilli(),
        )
        if (!repository.insertClosure(row)) return@withLock null
        val done = if (recorded) fetchText(row, snapshot, retried = false) else row
        telemetry.event(TelemetryEvents.CLOSURE, mapOf("period" to period, "outcome" to done.status))
        notifier.notify(period, notificationText(done))
        done
    }

    /** At app start: a closure left without the text for lack of network asks for it once more. */
    suspend fun retryOffline() = mutex.withLock {
        val snapshot = repository.observeToday().first()
        repository.offlineClosures().forEach { fetchText(it, snapshot, retried = true) }
    }

    private suspend fun fetchText(row: ClosureEntity, snapshot: DaySnapshot, retried: Boolean): ClosureEntity {
        val date = LocalDate.parse(row.date)
        val body = CloseIn(
            period = row.period,
            tone = snapshot.tone.takeIf { it == "seco" || it == "duro" } ?: "seco",
            localTime = clock.now().atZone(SaoPaulo.zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            profile = profile(snapshot, if (row.period == Closures.WEEK) date.plusDays(6) else date),
            numbers = Closures.json.parseToJsonElement(row.numbers),
        )
        val text = try {
            service.close(body).text.trim().takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val status = when {
            text == null -> Closures.OFFLINE
            Closures.isFallback(text) -> Closures.FALLBACK
            else -> Closures.TEXT
        }
        repository.setClosureText(row.key, text, status, retried)
        return row.copy(text = text, status = status, retried = retried)
    }

    private fun profile(snapshot: DaySnapshot, date: LocalDate) = CloseProfile(
        ceilingKcal = snapshot.ceilingProfile().ceilingOn(date).coerceIn(1, KCAL_MAX),
        pTarget = snapshot.proteinTargetG.coerceIn(0, GRAMS_MAX),
        cTarget = snapshot.carbTargetG.coerceIn(0, GRAMS_MAX),
        gTarget = snapshot.fatTargetG.coerceIn(0, GRAMS_MAX),
        slots = snapshot.slotsOn(date).sortedBy { it.minutesFromMidnight }.take(SLOTS_MAX)
            .map { ChatSlot(it.id.toString(), it.name.take(NAME_MAX), SlotSuggestions.format(it.minutesFromMidnight)) },
    )

    /** The numbers of [date] from Room: its meals in slot order, every record (Outros included) and its ceiling. */
    suspend fun dayNumbers(snapshot: DaySnapshot, date: LocalDate): ClosureDay {
        val day = repository.dayTotals(snapshot, date)
        val meals = day.meals.take(SLOTS_MAX).map { it.copy(name = it.name.take(NAME_MAX)) }
        return Closures.day(date, meals, day.totals.clamped(), day.ceilingKcal.coerceIn(1, KCAL_MAX), day.workoutKcal?.coerceIn(0, KCAL_MAX))
    }

    suspend fun weekNumbers(snapshot: DaySnapshot, monday: LocalDate, until: LocalDate): ClosureWeek {
        val days = generateSequence(monday) { it.plusDays(1) }.takeWhile { !it.isAfter(until) && it.isBefore(monday.plusDays(7)) }.toList()
        return Closures.week(days.map { dayNumbers(snapshot, it) })
    }

    private fun Macros.clamped() = Macros(kcal.coerceIn(0, KCAL_MAX), p.coerceIn(0, GRAMS_MAX), c.coerceIn(0, GRAMS_MAX), g.coerceIn(0, GRAMS_MAX))

    /** The notification content: the first line of the text, else what the card shows without one. */
    private fun notificationText(row: ClosureEntity): String = when (row.status) {
        Closures.EMPTY -> NO_RECORD
        Closures.OFFLINE -> OFFLINE_TEXT
        else -> row.text?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: OFFLINE_TEXT
    }

    companion object {
        const val NO_RECORD = "Nenhum registro."
        const val OFFLINE_TEXT = "Sem o texto: sem rede."

        /** Server bounds of /v1/close. */
        private const val KCAL_MAX = 20_000
        private const val GRAMS_MAX = 5_000
        private const val SLOTS_MAX = 12
        private const val NAME_MAX = 40
    }
}
