package com.nutri.android.feature.home

import androidx.compose.runtime.Immutable
import com.nutri.android.core.network.DishOut
import com.nutri.android.core.network.EstimateOut
import com.nutri.android.core.network.FitOut
import com.nutri.android.domain.Chip

enum class HomeSheetKind { T1, T3 }

@Immutable
data class HomeLogLine(
    val window: String,
    val title: String,
    val kcal: Int,
    val p: Int,
    val text: String = "",
) {
    val line: String get() = "$title · $kcal kcal · $p g P"
}

@Immutable
data class HomeUiState(
    val ready: Boolean = true,
    val shortDate: String = "",
    val remaining: Int = 2000,
    val remainingLabel: String = "",
    val nextTitle: String = "",
    val nextDetail: String = "",
    val bar: Float = 0f,
    val appDay: Int = 1,
    val chips: List<Chip> = emptyList(),
    val chipLabel: String? = null,
    val chipNote: String? = null,
    val window: String = "dinner",
    val logs: List<HomeLogLine> = emptyList(),
    val eaten: Int = 0,
    val protein: Int = 0,
    val effectiveCeiling: Int = 2000,
    val windowBudget: Int = 2000,
    val weekend: Boolean = false,
    val sheet: HomeSheetKind? = null,
    val logText: String = "",
    val photoB64: String? = null,
    val loading: Boolean = false,
    val fitMode: String = "want",
    val fitText: String = "",
    val fit: FitOut? = null,
    val fitDishes: List<DishOut> = emptyList(),
    val selectedFitIndex: Int? = null,
    val t3Headline: String? = null,
    val t3Line: String? = null,
    val t3Sub: String? = null,
    val t3Cta: String = "Encaixar",
)
