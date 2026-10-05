package app.fibrai.android.domain

import kotlin.math.roundToInt

data class MacroTargets(val proteinG: Int, val carbG: Int, val fatG: Int)

object MacroSplit {
    /** 30/40/30 of kcal. P and C at 4 kcal/g, G at 9 kcal/g. */
    fun of(kcal: Int): MacroTargets {
        val total = kcal.coerceAtLeast(0).toDouble()
        return MacroTargets(
            proteinG = (total * 0.30 / 4.0).roundToInt(),
            carbG = (total * 0.40 / 4.0).roundToInt(),
            fatG = (total * 0.30 / 9.0).roundToInt(),
        )
    }
}
