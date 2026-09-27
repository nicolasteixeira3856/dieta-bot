package com.nutri.android.core.database

import com.nutri.android.domain.BudgetCalculator
import com.nutri.android.domain.BudgetInput
import com.nutri.android.domain.CeilingProfile
import com.nutri.android.domain.CreditPolicy
import com.nutri.android.domain.SameEveryDayCeiling
import com.nutri.android.domain.SevenDayCeiling
import com.nutri.android.domain.WeekdayWeekendCeiling
import java.time.LocalDate

/** Effective ceiling of [date]: base ceiling for that weekday + workout credit. reservedUpcoming = 0. */
fun DaySnapshot.metaOn(date: LocalDate): Int = BudgetCalculator().calculate(
    BudgetInput(
        date = date,
        profile = ceilingProfile(),
        policy = when (eat) {
            "partial" -> CreditPolicy.PARTIAL
            "full" -> CreditPolicy.FULL
            else -> CreditPolicy.ZERO
        },
        percent = pct,
        workoutKcal = workoutKcal,
        reservedUpcoming = 0,
    ),
).effectiveCeiling

fun DaySnapshot.ceilingProfile(): CeilingProfile = when (ceilingMode) {
    "weekdayWeekend" -> WeekdayWeekendCeiling(kcalWeekday, kcalWeekend)
    "seven" -> kcalDays.let { if (it.size == 7) it else List(7) { 2000 } }
        .let { d -> SevenDayCeiling(d[0], d[1], d[2], d[3], d[4], d[5], d[6]) }
    else -> SameEveryDayCeiling(kcalSame)
}
