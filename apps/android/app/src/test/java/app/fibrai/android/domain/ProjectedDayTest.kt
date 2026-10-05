package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ProjectedDayTest {
    private val day = LocalDate.parse("2026-09-25")
    private val targets = Macros(0, 167, 223, 74)
    private val eaten = Macros(1640, 86, 152, 46)
    private val pizza = Macros(420, 40, 38, 12)

    private fun budget(policy: CreditPolicy, percent: Int? = null, workout: Int? = null) =
        BudgetInput(date = day, profile = SameEveryDayCeiling(2200), policy = policy, percent = percent, workoutKcal = workout)

    @Test
    fun sumsEatenAndPlan_onTheGoldNumbers() {
        val d = ProjectedDay.of(budget(CreditPolicy.ZERO), eaten, pizza, targets)
        assertThat(d.eatenKcal).isEqualTo(1640)
        assertThat(d.projected).isEqualTo(Macros(2060, 126, 190, 58))
        assertThat(d.ceilingKcal).isEqualTo(2200)
        assertThat(d.targets).isEqualTo(targets)
        assertThat(d.over).isFalse()
    }

    @Test
    fun eatBackZero_ignoresTheWorkout() {
        assertThat(ProjectedDay.of(budget(CreditPolicy.ZERO, workout = 400), eaten, pizza, targets).ceilingKcal).isEqualTo(2200)
    }

    @Test
    fun eatBackPercent_addsThatShareOfTheWorkout() {
        assertThat(ProjectedDay.of(budget(CreditPolicy.PARTIAL, percent = 50, workout = 400), eaten, pizza, targets).ceilingKcal).isEqualTo(2400)
    }

    @Test
    fun eatBackFull_addsTheWholeWorkout_noCap() {
        assertThat(ProjectedDay.of(budget(CreditPolicy.FULL, workout = 900), eaten, pizza, targets).ceilingKcal).isEqualTo(3100)
    }

    @Test
    fun workoutMissing_creditZero() {
        assertThat(ProjectedDay.of(budget(CreditPolicy.FULL, workout = null), eaten, pizza, targets).ceilingKcal).isEqualTo(2200)
    }

    @Test
    fun aboveTheEffectiveCeiling_isOver_exactlyAtItIsNot() {
        assertThat(ProjectedDay.of(budget(CreditPolicy.ZERO), eaten, Macros(561, 0, 0, 0), targets).over).isTrue()
        assertThat(ProjectedDay.of(budget(CreditPolicy.ZERO), eaten, Macros(560, 0, 0, 0), targets).over).isFalse()
    }
}
