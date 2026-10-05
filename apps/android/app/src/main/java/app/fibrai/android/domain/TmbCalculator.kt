package app.fibrai.android.domain

import kotlin.math.roundToInt

enum class Sex { MALE, FEMALE }

object TmbCalculator {
    /** Mifflin-St Jeor. Null when any body field is missing. */
    fun tmb(sex: Sex?, ageYears: Int, heightCm: Int, weightKg: Double): Int? {
        if (sex == null || ageYears <= 0 || heightCm <= 0 || weightKg <= 0.0) return null
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        val offset = if (sex == Sex.MALE) 5.0 else -161.0
        return (base + offset).roundToInt()
    }

    /** Ceiling prefill: TMB rounded to 10 kcal. */
    fun ceilingPrefill(sex: Sex?, ageYears: Int, heightCm: Int, weightKg: Double): Int? =
        tmb(sex, ageYears, heightCm, weightKg)?.let { (it / 10.0).roundToInt() * 10 }
}
