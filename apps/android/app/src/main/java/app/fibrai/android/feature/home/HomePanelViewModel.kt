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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    private val closures = repository.observeClosures(SaoPaulo.date(clock.now()).minusDays(CLOSURE_DAYS).toString())

    val uiState: StateFlow<HomePanelUiState> = combine(repository.observeToday(), workoutDraft, closures, expanded) { day, draft, rows, open ->
        HomePanelMapper.map(day, SaoPaulo.date(clock.now()), draft, rows, open)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomePanelUiState())

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
        /** Closures read for the cards: a week back and the day before it. */
        const val CLOSURE_DAYS = 8L
    }

    /** Same field as the Config workout editor. Empty = no workout = credit 0. */
    fun saveWorkout() {
        val draft = workoutDraft.value ?: return
        workoutDraft.value = null
        viewModelScope.launch { repository.setWorkout(draft.toIntOrNull()) }
    }
}
