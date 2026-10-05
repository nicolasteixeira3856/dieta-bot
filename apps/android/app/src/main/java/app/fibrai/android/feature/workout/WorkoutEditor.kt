package app.fibrai.android.feature.workout

import androidx.compose.runtime.Immutable
import app.fibrai.android.domain.CreditPolicy
import app.fibrai.android.domain.workoutCredit

/**
 * A22: the "Treino de hoje" editor, shared by the Home sheet (homeW) and the Config sheet.
 * Same field rules, same live credit line. Empty field = no workout today = credit 0.
 */
@Immutable
data class WorkoutEditorState(
    val input: String = "",
    val policy: CreditPolicy = CreditPolicy.ZERO,
    val pct: Int = 50,
) {
    val kcal: Int? get() = input.toIntOrNull()
    val credit: Int get() = workoutCredit(policy, pct, kcal)

    val creditLine: String
        get() = when (policy) {
            CreditPolicy.ZERO -> "Compensação desativada na Config"
            CreditPolicy.PARTIAL -> "+$credit kcal na meta de hoje (compensação $pct%)"
            CreditPolicy.FULL -> "+$credit kcal na meta de hoje (compensação 100%)"
        }

    companion object {
        private const val MAX_DIGITS = 5

        /** Digits only, at most 5. */
        fun clean(value: String): String = value.filter { it.isDigit() }.take(MAX_DIGITS)

        fun policyOf(eat: String) = when (eat) {
            "partial" -> CreditPolicy.PARTIAL
            "full" -> CreditPolicy.FULL
            else -> CreditPolicy.ZERO
        }
    }
}
