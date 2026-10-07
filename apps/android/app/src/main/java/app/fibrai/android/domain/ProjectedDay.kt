package app.fibrai.android.domain

/** kcal and grams of P/C/G: what was eaten, what a plan adds, or the day targets. */
data class Macros(val kcal: Int, val p: Int, val c: Int, val g: Int)

/**
 * The day after a plan (A29, ADR-023 decision 3): computed by the app, never by the AI.
 * "Dia: {eatenKcal} → {projected.kcal} de {ceilingKcal} kcal" and "P {projected.p}/{targets.p} · …".
 */
data class ProjectedDay(val eatenKcal: Int, val projected: Macros, val ceilingKcal: Int, val targets: Macros) {
    /** Above the effective ceiling: the kcal line goes `bad`. */
    val over: Boolean get() = projected.kcal > ceilingKcal

    companion object {
        /**
         * [budget] carries today's profile, eat-back policy and workout; its `eaten` is ignored for [eaten]. [reserved]: the
         * plans reserved for the other meals of today (A60 part D, ADR-046), counted in the projected day, never eaten.
         */
        fun of(budget: BudgetInput, eaten: Macros, plan: Macros, targets: Macros, reserved: Macros = Macros(0, 0, 0, 0)): ProjectedDay {
            val ceiling = BudgetCalculator().calculate(budget.copy(eaten = eaten.kcal, reservedUpcoming = reserved.kcal)).effectiveCeiling
            val projected = Macros(
                eaten.kcal + plan.kcal + reserved.kcal, eaten.p + plan.p + reserved.p, eaten.c + plan.c + reserved.c, eaten.g + plan.g + reserved.g,
            )
            return ProjectedDay(eaten.kcal, projected, ceiling, targets)
        }
    }
}
