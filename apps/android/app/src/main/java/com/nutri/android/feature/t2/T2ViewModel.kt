package com.nutri.android.feature.t2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.network.EstimateOut
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class T2ViewModel @Inject constructor(
    private val repository: DayRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(T2UiState())
    val uiState: StateFlow<T2UiState> = _uiState.asStateFlow()

    fun setInitialData(
        estimate: EstimateOut,
        mealText: String,
        window: String,
        windowBudget: Int,
    ) {
        val low = estimate.confidence != "high"
        val kcal = estimate.kcal.toInt()
        _uiState.value = T2UiState(
            title = if (low) "confiança baixa · 1 pergunta" else "confirmação",
            name = mealText.replace(",", " +").ifBlank { "refeição" },
            estimate = estimate,
            window = window,
            windowBudget = windowBudget,
            answer = 0,
            confirmEnabled = !(low && kcal <= 0),
            question = if (low) estimate.question else null,
            range = null,
        )
    }

    fun onYes(onComplete: () -> Unit) {
        _uiState.update { it.copy(answer = 0) }
        confirm(onComplete)
    }

    fun onRevise(onRevise: () -> Unit) {
        _uiState.update { it.copy(answer = 1) }
        onRevise()
    }

    fun onDiscard(onDiscard: () -> Unit) {
        _uiState.update { it.copy(answer = 2) }
        onDiscard()
    }

    fun confirm(onComplete: () -> Unit) {
        val current = _uiState.value
        val est = current.estimate ?: return
        val kcal = est.kcal.toInt()
        if (kcal <= 0) return
        viewModelScope.launch {
            repository.addLog(
                window = current.window,
                text = current.name,
                kcal = kcal,
                p = est.p.toInt(),
                stable = true,
            )
            onComplete()
        }
    }
}
