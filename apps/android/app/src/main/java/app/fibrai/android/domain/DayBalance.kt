package app.fibrai.android.domain

/**
 * A64: the day balance the Chat shows under a record (the receipt) and under an estimate still waiting for Registrar
 * (`Projeção:`), computed by the app from Room; the model is never asked for it.
 */
object DayBalance {
    /** `1.240 de 2.000 kcal · faltam 62 g de proteína`; protein at or over its target: `meta de proteína atingida`. */
    fun line(eaten: Macros, ceilingKcal: Int, proteinTargetG: Int): String {
        val left = proteinTargetG - eaten.p
        val protein = if (left > 0) "faltam $left g de proteína" else "meta de proteína atingida"
        return "${Closures.thousands(eaten.kcal)} de ${Closures.thousands(ceilingKcal)} kcal · $protein"
    }

    /** The day as it would be with [estimate] recorded on top of [eaten]. */
    fun projection(eaten: Macros, estimate: Macros, ceilingKcal: Int, proteinTargetG: Int): String =
        "Projeção: " + line(Macros(eaten.kcal + estimate.kcal, eaten.p + estimate.p, eaten.c + estimate.c, eaten.g + estimate.g), ceilingKcal, proteinTargetG)
}
