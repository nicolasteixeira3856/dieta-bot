package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExtraRulesTest {
    @Test
    fun `day 1 shows no chip even with two stable logs`() {
        val chip = chipForWindow(
            appDay = 1,
            currentWindow = "breakfast",
            logs = listOf(StableLog("breakfast", true), StableLog("breakfast", true)),
            removed = emptySet(),
            asked = emptySet(),
        )
        assertThat(chip).isNull()
    }

    @Test
    fun `second stable log of same window asks once and can be removed`() {
        val logs = listOf(StableLog("breakfast", true), StableLog("breakfast", true), StableLog("lunch", true))
        val asking = chipForWindow(2, "breakfast", logs, emptySet(), emptySet())
        assertThat(asking).isEqualTo(Chip("breakfast", question = true))

        val alreadyAsked = chipForWindow(2, "breakfast", logs, emptySet(), setOf("breakfast"))
        assertThat(alreadyAsked).isEqualTo(Chip("breakfast", question = false))

        val removed = chipForWindow(2, "breakfast", logs, setOf("breakfast"), setOf("breakfast"))
        assertThat(removed).isNull()
    }

    @Test
    fun `longest photo side is 1280 and jpeg is 70`() {
        assertThat(PhotoScale.JPEG_QUALITY).isEqualTo(70)
        assertThat(PhotoScale.dimensions(2000, 1000)).isEqualTo(1280 to 640)
        assertThat(PhotoScale.dimensions(800, 600)).isEqualTo(800 to 600)
        assertThat(PhotoScale.dimensions(1000, 2000)).isEqualTo(640 to 1280)
    }

    @Test
    fun `does not offer a dish that blows the ceiling`() {
        val offers = dishesThatFit(
            listOf(
                DishOffer("hamburguer", 900.0),
                DishOffer("ovos", 380.0),
                DishOffer("", 100.0),
            ),
            budgetKcal = 455.0,
        )
        assertThat(offers.map { it.name }).containsExactly("ovos")
    }
}
