package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class BudgetCalculatorTest {
    private val calc = BudgetCalculator()
    private val monday = LocalDate.of(2026, 9, 21)
    private val friday = LocalDate.of(2026, 9, 25)
    private val saturday = LocalDate.of(2026, 9, 26)
    private val sunday = LocalDate.of(2026, 9, 27)
    private val wednesday = LocalDate.of(2026, 9, 23)

    @Test
    fun `policy 0 with workout 1000 credits 0`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.ZERO,
                workoutKcal = 1000,
            ),
        )
        assertThat(r.credit).isEqualTo(0)
        assertThat(r.effectiveCeiling).isEqualTo(2000)
    }

    @Test
    fun `missing workout with policy 100 credits 0`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.FULL,
                workoutKcal = null,
            ),
        )
        assertThat(r.credit).isEqualTo(0)
        assertThat(r.effectiveCeiling).isEqualTo(2000)
    }

    @Test
    fun `policy 50 with workout 480 credits 240`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.PARTIAL,
                percent = 50,
                workoutKcal = 480,
            ),
        )
        assertThat(r.credit).isEqualTo(240)
        assertThat(r.effectiveCeiling).isEqualTo(2240)
    }

    @Test
    fun `policy 100 with workout 480 credits 480`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.FULL,
                workoutKcal = 480,
            ),
        )
        assertThat(r.credit).isEqualTo(480)
        assertThat(r.effectiveCeiling).isEqualTo(2480)
    }

    @Test
    fun `no cap workout 2000 at 100 percent adds 2000 to ceiling`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.FULL,
                workoutKcal = 2000,
            ),
        )
        assertThat(r.credit).isEqualTo(2000)
        assertThat(r.effectiveCeiling).isEqualTo(4000)
    }

    @Test
    fun `window budget never negative`() {
        val r = calc.calculate(
            BudgetInput(
                date = wednesday,
                profile = SameEveryDayCeiling(2000),
                policy = CreditPolicy.ZERO,
                workoutKcal = 1000,
                eaten = 1500,
                reservedUpcoming = 700,
            ),
        )
        assertThat(r.windowBudget).isEqualTo(0)
        assertThat(r.windowBudget).isAtLeast(0)
    }

    @Test
    fun `base ceiling differs across same every day weekday weekend and 7 days`() {
        assertThat(monday.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(saturday.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(sunday.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)

        val same = SameEveryDayCeiling(2000)
        assertThat(same.ceilingOn(wednesday)).isEqualTo(2000)
        assertThat(same.ceilingOn(sunday)).isEqualTo(2000)

        val weekdayWeekend = WeekdayWeekendCeiling(weekday = 2000, weekend = 2300)
        assertThat(weekdayWeekend.ceilingOn(monday)).isEqualTo(2000)
        assertThat(weekdayWeekend.ceilingOn(friday)).isEqualTo(2000)
        assertThat(weekdayWeekend.ceilingOn(saturday)).isEqualTo(2300)
        assertThat(weekdayWeekend.ceilingOn(sunday)).isEqualTo(2300)
        assertThat(weekdayWeekend.ceilingOn(monday)).isNotEqualTo(weekdayWeekend.ceilingOn(saturday))

        val seven = SevenDayCeiling(1900, 2100, 1800, 2400, 2000, 2600, 1700)
        val days = listOf(
            LocalDate.of(2026, 9, 21),
            LocalDate.of(2026, 9, 22),
            LocalDate.of(2026, 9, 23),
            LocalDate.of(2026, 9, 24),
            LocalDate.of(2026, 9, 25),
            LocalDate.of(2026, 9, 26),
            LocalDate.of(2026, 9, 27),
        )
        val ceilings = days.map { seven.ceilingOn(it) }
        assertThat(ceilings).containsExactly(1900, 2100, 1800, 2400, 2000, 2600, 1700).inOrder()
        assertThat(ceilings.toSet()).hasSize(7)
    }

    @Test
    fun `America Sao Paulo date shifts weekday near UTC midnight`() {
        val instant = Instant.parse("2026-09-27T02:30:00Z")
        val date = SaoPaulo.date(instant)
        assertThat(date).isEqualTo(LocalDate.of(2026, 9, 26))
        val ceiling = WeekdayWeekendCeiling(weekday = 2000, weekend = 2300).ceilingOn(date)
        assertThat(ceiling).isEqualTo(2300)
    }
}
