package app.fibrai.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.feature.workout.WorkoutEditorState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomePanelViewModel @Inject constructor(
    private val repository: DayRepository,
    private val clock: InstantClock,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    /** Field of the open "Treino de hoje" sheet. Null = closed. Nothing reaches Room before Salvar. */
    private val workoutDraft = MutableStateFlow<String?>(null)

    /** A60 part B: collapsed closure cards the user tapped open (until the screen goes). */
    private val expanded = MutableStateFlow<Set<String>>(emptySet())

    /** A72 (ADR-058 decision 4): the past day shown, with the day it was picked on; null = today. */
    private val selected = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    private val closures = repository.observeClosures(SaoPaulo.date(clock.now()).minusDays(STRIP_DAYS).toString())

    private val stripKcal = repository.observeDayKcal(
        SaoPaulo.date(clock.now()).minusDays(STRIP_DAYS).toString(),
        SaoPaulo.date(clock.now()).plusDays(1).toString(),
    ).map { rows -> rows.associate { it.date to it.kcal } }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val shownDay = combine(repository.observeToday(), selected) { today, pick -> today to pick }
        .flatMapLatest { (today, pick) ->
            // The day rollover returns to today: a pick of another day no longer holds.
            val day = pick?.takeIf { it.second.toString() == today.date }?.first
            if (day == null) flowOf(today to null) else repository.observeDay(day.toString()).map { it to day }
        }

    val uiState: StateFlow<HomePanelUiState> = combine(shownDay, workoutDraft, closures, expanded, stripKcal) { (day, shown), draft, rows, open, kcal ->
        HomePanelMapper.map(day, SaoPaulo.date(clock.now()), draft, rows, open, shown = shown, dayKcal = kcal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomePanelUiState())

    /** A72: a day of the strip; today's circle returns to today. */
    fun selectDay(iso: String) {
        val today = SaoPaulo.date(clock.now())
        val day = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return
        if (day == today) {
            selected.value = null
            return
        }
        if (day.isAfter(today)) return
        selected.value = day to today
        val offset = java.time.temporal.ChronoUnit.DAYS.between(day, today)
        telemetry.event(TelemetryEvents.HOME_DAY_SELECTED, mapOf("offset" to if (offset <= 1) "1" else if (offset <= 7) "2-7" else "8-30"))
        telemetry.event(TelemetryEvents.SCREEN_VIEW, mapOf("screen" to "home_past"))
    }

    /** A72: leaving the Home (Chat, Config) comes back on today: the Chat is today's and a wipe changes today. */
    fun backToToday() {
        selected.value = null
    }

    /** A collapsed closure card tapped open. */
    fun expandClosure(key: String) {
        val card = uiState.value.closures.firstOrNull { it.key == key && !it.expanded } ?: return
        expanded.value = expanded.value + key
        telemetry.event(TelemetryEvents.CLOSURE_OPENED, mapOf("period" to card.period, "from" to "card"))
    }

    /** Explicit user action from the timeline. Never automatic. */
    fun skip(slotId: Long) {
        val planned = uiState.value.timeline.any { it.slotId == slotId && it.state == SlotState.PLANNED }
        viewModelScope.launch {
            repository.addSkip(slotId)
            telemetry.event(TelemetryEvents.MEAL_SKIPPED, mapOf("from" to "home"))
            if (planned) telemetry.event(TelemetryEvents.PLAN_RESERVED, mapOf("action" to "cleared_by_skip"))
        }
    }

    /** Opens the sheet with the stored kcal of today. */
    fun openWorkout() {
        workoutDraft.value = uiState.value.workoutKcal?.toString().orEmpty()
    }

    fun setWorkout(value: String) {
        if (workoutDraft.value != null) workoutDraft.value = WorkoutEditorState.clean(value)
    }

    fun closeWorkout() {
        workoutDraft.value = null
    }

    private companion object {
        /** Closures and the strip: the last 30 days (A72), which hold the week and the day before it. */
        const val STRIP_DAYS = 31L
    }

    /** Same field as the Config workout editor. Empty = no workout = credit 0. */
    fun saveWorkout() {
        val draft = workoutDraft.value ?: return
        workoutDraft.value = null
        viewModelScope.launch { repository.setWorkout(draft.toIntOrNull()) }
    }
}
