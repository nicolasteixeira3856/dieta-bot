package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class SlotsOfDayTest {
    @Test fun everyDayAndAllSevenBits() {
        val monday = LocalDate.parse("2026-09-28")
        repeat(7) { index ->
            val date = monday.plusDays(index.toLong())
            assertThat(SlotsOfDay.select(listOf(127), date) { it }).containsExactly(127)
            assertThat(SlotsOfDay.select((0..6).map { 1 shl it }, date) { it }).containsExactly(1 shl index)
            assertThat(SlotsOfDay.select(listOf(31, 96), date) { it }).containsExactly(if (index < 5) 31 else 96)
        }
    }

    @Test fun saturdayStartsAtSaoPauloMidnight() {
        assertThat(SlotsOfDay.select(listOf(31, 96), Instant.parse("2026-09-26T02:59:59Z")) { it }).containsExactly(31)
        assertThat(SlotsOfDay.select(listOf(31, 96), Instant.parse("2026-09-26T03:00:00Z")) { it }).containsExactly(96)
    }
}
