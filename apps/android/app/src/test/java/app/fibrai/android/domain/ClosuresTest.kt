package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

/** A60 part B (ADR-044): closure alarm times, keys, the week numbers and the card rules. Pure. */
class ClosuresTest {
    private fun at(iso: String) = Instant.parse(iso)

    @Test fun nextAlarm_is22hSaoPaulo_nowWhenMissed_tomorrowWhenDone() {
        assertThat(Closures.nextAlarm(at("2026-10-07T18:00:00-03:00"), dayDone = false)).isEqualTo(at("2026-10-07T22:00:00-03:00"))
        val late = at("2026-10-07T23:10:00-03:00")
        assertThat(Closures.nextAlarm(late, dayDone = false)).isEqualTo(late)
        assertThat(Closures.nextAlarm(late, dayDone = true)).isEqualTo(at("2026-10-08T22:00:00-03:00"))
        // 23:30 SP is 02:30 UTC of the next day: the SP calendar decides.
        assertThat(Closures.nextAlarm(at("2026-10-08T02:30:00Z"), dayDone = true)).isEqualTo(at("2026-10-08T22:00:00-03:00"))
        // After midnight SP (the 00:05 resync): today's 22:00.
        assertThat(Closures.nextAlarm(at("2026-10-08T00:05:00-03:00"), dayDone = false)).isEqualTo(at("2026-10-08T22:00:00-03:00"))
    }

    @Test fun duePeriods_dayFrom22h_weekOnSunday() {
        assertThat(Closures.duePeriods(at("2026-10-07T21:59:00-03:00"))).isEmpty()
        assertThat(Closures.duePeriods(at("2026-10-07T22:00:00-03:00"))).containsExactly("day")
        assertThat(Closures.duePeriods(at("2026-10-11T22:00:00-03:00"))).containsExactly("day", "week").inOrder()
    }

    @Test fun keys_dayAndIsoWeek() {
        assertThat(Closures.dayKey(LocalDate.parse("2026-10-07"))).isEqualTo("day:2026-10-07")
        assertThat(Closures.weekKey(LocalDate.parse("2026-10-11"))).isEqualTo("week:2026-10-05")
        assertThat(Closures.weekKey(LocalDate.parse("2026-10-05"))).isEqualTo("week:2026-10-05")
    }

    private fun day(date: String, vararg meals: Pair<String, Int?>, ceiling: Int = 2000): ClosureDay {
        val list = meals.mapIndexed { i, (name, kcal) -> ClosureMeal(i.toLong(), name, kcal ?: 0, skipped = false, plannedKcal = null, eaten = kcal != null) }
        val total = list.sumOf { it.kcal }
        return Closures.day(LocalDate.parse(date), list, Macros(total, total / 20, total / 8, total / 30), ceiling, null)
    }

    @Test fun day_statusesInSlotOrder_andRecorded() {
        val meals = listOf(
            ClosureMeal(1, "Café", 400, skipped = false, plannedKcal = null, eaten = true),
            ClosureMeal(2, "Lanche", 0, skipped = true, plannedKcal = null, eaten = false),
            ClosureMeal(3, "Jantar", 0, skipped = false, plannedKcal = 550, eaten = false),
            ClosureMeal(4, "Ceia", 0, skipped = false, plannedKcal = null, eaten = false),
        )
        val d = Closures.day(LocalDate.parse("2026-10-07"), meals, Macros(400, 20, 40, 15), 2175, 350)
        assertThat(d.slots.map { it.status }).containsExactly("eaten", "skipped", "planned", "empty").inOrder()
        assertThat(d.slots[2].kcal).isEqualTo(550)
        assertThat(d.recorded).isTrue()
        assertThat(Closures.missingLine(d)).isEqualTo("Pulado: Lanche · Sem registro: Jantar, Ceia")
        assertThat(day("2026-10-07", "Café" to null).recorded).isFalse()
    }

    @Test fun week_countsTheMealOverItsShare_andMeansOverRecordedDays() {
        val days = listOf(
            day("2026-10-05", "Almoço" to 600, "Jantar" to 1100),
            day("2026-10-06", "Almoço" to 1050, "Jantar" to 1200),
            day("2026-10-07", "Almoço" to null, "Jantar" to null),
            day("2026-10-08", "Almoço" to 500, "Jantar" to 1001),
        )
        val week = Closures.week(days)
        assertThat(week.overSlot).isEqualTo(ClosureOverSlot("Jantar", 3))
        assertThat(week.total).isEqualTo(1700 + 2250 + 1501)
        assertThat(week.meanKcal).isEqualTo(1817)
        assertThat(week.unrecorded).isEqualTo(1)
        assertThat(Closures.week(listOf(day("2026-10-05", "Almoço" to 500))).overSlot).isNull()
    }

    @Test fun cards_dayAndWeekCollapseRules() {
        val today = LocalDate.parse("2026-10-08")
        assertThat(Closures.dayCard(today, today, todayHasRecord = true)).isEqualTo(Closures.CardState.EXPANDED)
        assertThat(Closures.dayCard(today.minusDays(1), today, todayHasRecord = false)).isEqualTo(Closures.CardState.EXPANDED)
        assertThat(Closures.dayCard(today.minusDays(1), today, todayHasRecord = true)).isEqualTo(Closures.CardState.COLLAPSED)
        assertThat(Closures.dayCard(today.minusDays(2), today, todayHasRecord = false)).isEqualTo(Closures.CardState.HIDDEN)
        val monday = LocalDate.parse("2026-10-05")
        assertThat(Closures.weekCard(monday, LocalDate.parse("2026-10-10"))).isEqualTo(Closures.CardState.HIDDEN)
        assertThat(Closures.weekCard(monday, LocalDate.parse("2026-10-11"))).isEqualTo(Closures.CardState.EXPANDED)
        assertThat(Closures.weekCard(monday, LocalDate.parse("2026-10-12"))).isEqualTo(Closures.CardState.EXPANDED)
        assertThat(Closures.weekCard(monday, LocalDate.parse("2026-10-13"))).isEqualTo(Closures.CardState.COLLAPSED)
        assertThat(Closures.weekCard(monday, LocalDate.parse("2026-10-18"))).isEqualTo(Closures.CardState.HIDDEN)
    }

    @Test fun copy() {
        assertThat(Closures.dayTitle(LocalDate.parse("2026-09-25"))).isEqualTo("Fechamento de 25 de setembro")
        assertThat(Closures.weekTitle(LocalDate.parse("2026-09-28"), LocalDate.parse("2026-10-04"))).isEqualTo("Semana de 28 de setembro a 4 de outubro")
        assertThat(Closures.thousands(13420)).isEqualTo("13.420")
        assertThat(Closures.isFallback("Dia fechado. 2230 de 2000 kcal.")).isTrue()
        assertThat(Closures.isFallback("Passou 230 kcal.")).isFalse()
    }
}
