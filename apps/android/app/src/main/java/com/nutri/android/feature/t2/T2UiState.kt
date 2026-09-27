package com.nutri.android.feature.t2

import androidx.compose.runtime.Immutable
import com.nutri.android.core.network.EstimateOut

@Immutable
data class T2UiState(
    val title: String = "confirmação",
    val name: String = "",
    val estimate: EstimateOut? = null,
    val window: String = "dinner",
    val windowBudget: Int = 2000,
    val answer: Int = 0,
    val confirmEnabled: Boolean = true,
    val question: String? = null,
    val range: String? = null,
)
