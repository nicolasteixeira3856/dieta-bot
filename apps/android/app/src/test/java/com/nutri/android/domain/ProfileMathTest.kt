package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class ProfileMathTest {
    @Test
    fun `tmb male 27y 116kg 180cm is 2155 and prefills 2160`() {
        assertThat(TmbCalculator.tmb(Sex.MALE, 27, 180, 116.0)).isEqualTo(2155)
        assertThat(TmbCalculator.ceilingPrefill(Sex.MALE, 27, 180, 116.0)).isEqualTo(2160)
    }

    @Test
    fun `tmb female uses minus 161`() {
        assertThat(TmbCalculator.tmb(Sex.FEMALE, 27, 180, 116.0)).isEqualTo(1989)
    }

    @Test
    fun `tmb is null when a body field is missing`() {
        assertThat(TmbCalculator.tmb(null, 27, 180, 116.0)).isNull()
        assertThat(TmbCalculator.tmb(Sex.MALE, 0, 180, 116.0)).isNull()
        assertThat(TmbCalculator.tmb(Sex.MALE, 27, 0, 116.0)).isNull()
        assertThat(TmbCalculator.tmb(Sex.MALE, 27, 180, 0.0)).isNull()
    }

    @Test
    fun `macro split 2000 is 150 200 67`() {
        assertThat(MacroSplit.of(2000)).isEqualTo(MacroTargets(proteinG = 150, carbG = 200, fatG = 67))
    }

    @Test
    fun `minutes from midnight uses Sao Paulo`() {
        assertThat(SlotClock.minutesFromMidnight(Instant.parse("2026-03-15T21:10:00-03:00"))).isEqualTo(21 * 60 + 10)
        assertThat(SlotClock.minutesFromMidnight(Instant.parse("2026-03-16T01:30:00Z"))).isEqualTo(22 * 60 + 30)
    }

    @Test
    fun `current slot is last with minutes lte now, else first`() {
        val slots = listOf(12 * 60, 8 * 60, 20 * 60)
        assertThat(SlotClock.current(slots, 5 * 60) { it }).isEqualTo(8 * 60)
        assertThat(SlotClock.current(slots, 8 * 60) { it }).isEqualTo(8 * 60)
        assertThat(SlotClock.current(slots, 15 * 60) { it }).isEqualTo(12 * 60)
        assertThat(SlotClock.current(slots, 23 * 60) { it }).isEqualTo(20 * 60)
        assertThat(SlotClock.current(emptyList<Int>(), 600) { it }).isNull()
    }

    @Test
    fun `slot suggestions follow the time band`() {
        assertThat(SlotSuggestions.namesFor(7 * 60 + 30)).containsExactly("Café", "Desjejum").inOrder()
        assertThat(SlotSuggestions.namesFor(12 * 60 + 30)).containsExactly("Almoço", "Prato feito").inOrder()
        assertThat(SlotSuggestions.namesFor(16 * 60)).containsExactly("Lanche", "Café da tarde").inOrder()
        assertThat(SlotSuggestions.namesFor(20 * 60)).containsExactly("Jantar", "Ceia").inOrder()
        assertThat(SlotSuggestions.bandOf(10 * 60 + 30)).isEqualTo(SlotBand.MORNING_SNACK)
        assertThat(SlotSuggestions.bandOf(23 * 60)).isEqualTo(SlotBand.NIGHT)
        assertThat(SlotSuggestions.bandOf(4 * 60 + 59)).isEqualTo(SlotBand.NIGHT)
        assertThat(SlotSuggestions.bandOf(5 * 60)).isEqualTo(SlotBand.BREAKFAST)
    }

    @Test
    fun `default times cover 2 to 6 slots in order`() {
        (2..6).forEach { n ->
            val t = SlotSuggestions.defaultTimes(n)
            assertThat(t).hasSize(n)
            assertThat(t).isInStrictOrder()
        }
        assertThat(SlotSuggestions.defaultTimes(1)).hasSize(2)
        assertThat(SlotSuggestions.defaultTimes(9)).hasSize(6)
        assertThat(SlotSuggestions.format(7 * 60 + 5)).isEqualTo("07:05")
    }
}
