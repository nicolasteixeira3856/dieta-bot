package com.nutri.android.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: DayRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = repository.observeToday().first()
            _uiState.value = _uiState.value.copy(
                ready = true,
                onboardingDone = snapshot.onboardingDone,
            )
        }
    }

    fun setCapture(capture: Boolean) {
        _uiState.value = _uiState.value.copy(capture = capture)
    }
}
