package com.nutri.android.feature.splash

import androidx.compose.runtime.Immutable

@Immutable
data class SplashUiState(
    val capture: Boolean = false,
    val ready: Boolean = false,
    val onboardingDone: Boolean = false,
)
