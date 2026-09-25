package com.nutri.android.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Day ceiling and window budget. Pure data, no Android, no network. */
object SaoPaulo {
    val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun date(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()
}

sealed interface CeilingProfile {
    fun ceilingOn(date: LocalDate): Int
}

data class SameEveryDayCeiling(val kcal: Int) : CeilingProfile {
    override fun ceilingOn(date: LocalDate): Int = kcal
}

data class WeekdayWeekendCeiling(val weekday: Int, val weekend: Int) : CeilingProfile {
    override fun ceilingOn(date: LocalDate): Int {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        return if (isWeekend) weekend else weekday
    }
}

data class SevenDayCeiling(
    val monday: Int,
    val tuesday: Int,
    val wednesday: Int,
    val thursday: Int,
    val friday: Int,
    val saturday: Int,
    val sunday: Int,
) : CeilingProfile {
    override fun ceilingOn(date: LocalDate): Int = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> monday
        DayOfWeek.TUESDAY -> tuesday
        DayOfWeek.WEDNESDAY -> wednesday
        DayOfWeek.THURSDAY -> thursday
        DayOfWeek.FRIDAY -> friday
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }
}

enum class CreditPolicy { ZERO, PARTIAL, FULL }

data class BudgetInput(
    val date: LocalDate,
    val profile: CeilingProfile,
    val policy: CreditPolicy,
    val percent: Int? = null,
    val workoutKcal: Int? = null,
    val eaten: Int = 0,
    val reservedUpcoming: Int = 0,
)

data class BudgetResult(
    val baseCeiling: Int,
    val credit: Int,
    val effectiveCeiling: Int,
    val windowBudget: Int,
)

class BudgetCalculator {
    fun calculate(input: BudgetInput): BudgetResult {
        val baseCeiling = input.profile.ceilingOn(input.date)
        val credit = workoutCredit(input.policy, input.percent, input.workoutKcal)
        val effectiveCeiling = baseCeiling + credit
        val raw = effectiveCeiling - input.eaten - input.reservedUpcoming
        return BudgetResult(
            baseCeiling = baseCeiling,
            credit = credit,
            effectiveCeiling = effectiveCeiling,
            windowBudget = maxOf(0, raw),
        )
    }
}

/** Policy 0, or workout kcal for the day missing, zeros credit. No cap. */
fun workoutCredit(policy: CreditPolicy, percent: Int?, workoutKcal: Int?): Int {
    if (policy == CreditPolicy.ZERO || workoutKcal == null) return 0
    if (policy == CreditPolicy.FULL) return workoutKcal
    val pct = percent ?: error("percent")
    return workoutKcal * pct / 100
}
