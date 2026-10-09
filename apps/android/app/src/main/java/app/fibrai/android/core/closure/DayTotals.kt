package app.fibrai.android.core.closure

import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.budgetOn
import app.fibrai.android.core.database.slotsOn
import app.fibrai.android.domain.BudgetCalculator
import app.fibrai.android.domain.ClosureMeal
import app.fibrai.android.domain.Macros
import java.time.LocalDate

/**
 * A day of Room as the closures (A60 part B) and the Chat `recent_days` (A64) read it: its meals in slot order, the totals
 * of every record (Outros included) and the effective ceiling with that day's own workout credit.
 */
data class DayTotals(val date: LocalDate, val meals: List<ClosureMeal>, val totals: Macros, val ceilingKcal: Int, val workoutKcal: Int?)

suspend fun DayRepository.dayTotals(snapshot: DaySnapshot, date: LocalDate): DayTotals {
    val record = dayRecord(date.toString())
    val meals = snapshot.slotsOn(date).sortedBy { it.minutesFromMidnight }.map { slot ->
        val logs = record.logs.filter { it.second == slot.id }.map { it.first }
        ClosureMeal(slot.id, slot.name, logs.sumOf { it.kcal }, slot.id in record.skipped, record.planned[slot.id]?.kcal, logs.isNotEmpty())
    }
    val all = record.logs.map { it.first }
    val totals = Macros(all.sumOf { it.kcal }, all.sumOf { it.p }, all.sumOf { it.c }, all.sumOf { it.g })
    val ceiling = BudgetCalculator().calculate(snapshot.budgetOn(date).copy(workoutKcal = record.workoutKcal)).effectiveCeiling
    return DayTotals(date, meals, totals, ceiling, record.workoutKcal)
}
