package com.nutri.android.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.domain.SaoPaulo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: DayRepository,
    private val clock: InstantClock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val day = repository.observeToday().first()
            _uiState.update { current ->
                current.copy(
                    ceilingMode = day.ceilingMode,
                    sameField = day.kcalSame.toString(),
                    weekdayField = day.kcalWeekday.toString(),
                    weekendField = day.kcalWeekend.toString(),
                    dayFields = day.kcalDays.map { it.toString() }.let { if (it.size == 7) it else List(7) { "2000" } },
                    eat = day.eat,
                    pct = day.pct.toString(),
                )
            }
        }
    }

    fun setCeilingMode(mode: String) = _uiState.update { it.copy(ceilingMode = mode) }
    fun setSameField(v: String) = _uiState.update { it.copy(sameField = v.filter { c -> c.isDigit() }) }
    fun setWeekdayField(v: String) = _uiState.update { it.copy(weekdayField = v.filter { c -> c.isDigit() }) }
    fun setWeekendField(v: String) = _uiState.update { it.copy(weekendField = v.filter { c -> c.isDigit() }) }
    fun setDayField(index: Int, v: String) = _uiState.update {
        val list = it.dayFields.toMutableList()
        if (index in list.indices) {
            list[index] = v.filter { c -> c.isDigit() }
        }
        it.copy(dayFields = list)
    }

    fun setEat(mode: String) = _uiState.update { it.copy(eat = mode) }
    fun setPct(v: String) = _uiState.update { it.copy(pct = v.filter { c -> c.isDigit() }.take(3).ifBlank { "50" }) }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val today = SaoPaulo.date(clock.now()).toString()
            repository.saveProfile(
                ceilingMode = state.ceilingMode,
                kcalSame = state.sameField.toIntOrNull() ?: 2000,
                kcalWeekday = state.weekdayField.toIntOrNull() ?: 2000,
                kcalWeekend = state.weekendField.toIntOrNull() ?: 2300,
                kcalDays = state.dayFields.map { it.toIntOrNull() ?: 2000 },
                eat = state.eat,
                pct = state.pct.toIntOrNull() ?: 50,
                onboardingDone = true,
                firstDay = today,
            )
            _uiState.update { it.copy(isComplete = true) }
            onSuccess()
        }
    }
}
