package com.nutri.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.telemetry.NoopTelemetry
import com.nutri.android.core.telemetry.Telemetry
import com.nutri.android.core.telemetry.TelemetryEvents
import com.nutri.android.domain.SaoPaulo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomePanelViewModel @Inject constructor(
    private val repository: DayRepository,
    private val clock: InstantClock,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    val uiState: StateFlow<HomePanelUiState> = repository.observeToday()
        .map { HomePanelMapper.map(it, SaoPaulo.date(clock.now())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomePanelUiState())

    /** Explicit user action from the timeline. Never automatic. */
    fun skip(slotId: Long) {
        viewModelScope.launch {
            repository.addSkip(slotId)
            telemetry.event(TelemetryEvents.MEAL_SKIPPED, mapOf("from" to "home"))
        }
    }
}
