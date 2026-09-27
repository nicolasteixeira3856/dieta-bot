package com.nutri.android.feature.onboarding

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(
    val ceilingMode: String = "same",
    val sameField: String = "2000",
    val weekdayField: String = "2000",
    val weekendField: String = "2300",
    val dayFields: List<String> = List(7) { "2000" },
    val eat: String = "zero",
    val pct: String = "50",
    val isComplete: Boolean = false,
)
