package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A64 and A70 (ADR-056 § 8): the day lines of the Chat. Pure. */
class DayBalanceTest {
    private val eaten = Macros(1640, 86, 120, 40)

    @Test fun optionDay_isEatenPlusTheOption_againstTheCeilingAndTheProteinTarget() {
        assertThat(DayBalance.optionDay(eaten, Macros(420, 40, 38, 12), 2200, 167)).isEqualTo("Dia: ~2.060 de 2.200 kcal · P 126 de 167")
        assertThat(DayBalance.optionDay(eaten, Macros(360, 28, 14, 20), 2200, 167)).isEqualTo("Dia: ~2.000 de 2.200 kcal · P 114 de 167")
    }

    @Test fun optionDay_matchesTheProjectionNumbers() {
        val option = Macros(680, 39, 62, 29)
        assertThat(DayBalance.projection(eaten, option, 2200, 167)).startsWith("Projeção: 2.320 de 2.200 kcal")
        assertThat(DayBalance.optionDay(eaten, option, 2200, 167)).startsWith("Dia: ~2.320 de 2.200 kcal")
    }

    @Test fun optionFit_fromOverKcal() {
        assertThat(DayBalance.optionFit(0, "Jantar")).isEqualTo("Cabe na janela do Jantar")
        assertThat(DayBalance.optionFit(130, "Jantar")).isEqualTo("Passa 130 kcal da janela do Jantar")
    }
}
