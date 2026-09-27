package com.nutri.android.feature.home

import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.MealLog
import com.nutri.android.core.database.MealSlot
import java.time.LocalDate
import org.junit.Test

class HomePanelMapperTest {
    private val today = LocalDate.parse("2026-09-25")

    @Test
    fun `day 1 empty - zero consumed, all slots empty, none highlighted`() {
        val ui = HomePanelMapper.map(HomeFixtures.day(), today)
        assertThat(ui.dayLabel).isEqualTo("DIA 1")
        assertThat(ui.dateLabel).isEqualTo("25 de setembro")
        assertThat(ui.consumed).isEqualTo(0)
        assertThat(ui.meta).isEqualTo(2000)
        assertThat(ui.slotCount).isEqualTo(4)
        assertThat(ui.timeline.map { it.state }).containsExactly(SlotState.EMPTY, SlotState.EMPTY, SlotState.EMPTY, SlotState.EMPTY)
    }

    @Test
    fun `logs group by slot, second log stacks, skip shows, next empty is highlighted`() {
        val day = HomeFixtures.day(
            logs = listOf(
                MealLog("breakfast", "pão", 300, 10, true, slotId = 1, carbs = 40, fat = 8),
                MealLog("breakfast", "café com leite", 220, 18, true, slotId = 1, carbs = 12, fat = 14),
                MealLog("lunch", "prato feito", 780, 48, true, slotId = 2, carbs = 82, fat = 18, source = "photo"),
            ),
            skipped = setOf(3L),
        )
        val ui = HomePanelMapper.map(day, today)
        assertThat(ui.consumed).isEqualTo(1300)
        val (cafe, almoco, lanche, jantar) = ui.timeline
        assertThat(cafe.state).isEqualTo(SlotState.LOGGED)
        assertThat(cafe.lines.map { it.text }).containsExactly("pão", "café com leite").inOrder()
        assertThat(cafe.summary).isEqualTo("520 kcal · 28P · 52C · 22G")
        assertThat(almoco.fromPhoto).isTrue()
        assertThat(lanche.state).isEqualTo(SlotState.SKIPPED)
        assertThat(jantar.state).isEqualTo(SlotState.NEXT)
        assertThat(ui.protein).isEqualTo(MacroLine(76, 150))
    }

    @Test
    fun `slot that crosses the meta is OVER, macros over flag`() {
        val day = HomeFixtures.day(
            logs = listOf(
                MealLog("b", "a", 380, 22, true, slotId = 1, carbs = 48, fat = 12),
                MealLog("l", "b", 780, 64, true, slotId = 2, carbs = 88, fat = 16),
                MealLog("d", "c", 1120, 82, true, slotId = 4, carbs = 104, fat = 54),
            ),
            skipped = setOf(3L),
        )
        val ui = HomePanelMapper.map(day, today)
        assertThat(ui.over).isEqualTo(280)
        assertThat(ui.timeline.map { it.state })
            .containsExactly(SlotState.LOGGED, SlotState.LOGGED, SlotState.SKIPPED, SlotState.OVER).inOrder()
        assertThat(ui.protein.over).isTrue()
        assertThat(ui.fat.over).isTrue()
        assertThat(ui.ringFraction).isEqualTo(1f)
    }

    @Test
    fun `orphan logs go to Outros, meta includes workout credit, day counter`() {
        val day = HomeFixtures.day(
            logs = listOf(MealLog("dinner", "v1 log", 400, 20, true)),
        ).copy(eat = "partial", pct = 50, workoutKcal = 300, firstDay = "2026-09-23")
        val ui = HomePanelMapper.map(day, today)
        assertThat(ui.meta).isEqualTo(2150)
        assertThat(ui.dayLabel).isEqualTo("DIA 3")
        assertThat(ui.timeline.last().name).isEqualTo("Outros")
        assertThat(ui.timeline.last().slotId).isNull()
        assertThat(ui.timeline.last().time).isNull()
        // Nothing filled among real slots: no highlight.
        assertThat(ui.timeline.dropLast(1).map { it.state }.toSet()).containsExactly(SlotState.EMPTY)
    }
}

object HomeFixtures {
    val slots = listOf(
        MealSlot(1, "Café da manhã", 7 * 60 + 30),
        MealSlot(2, "Almoço", 12 * 60 + 30),
        MealSlot(3, "Lanche", 16 * 60),
        MealSlot(4, "Jantar", 20 * 60),
    )

    fun day(logs: List<MealLog> = emptyList(), skipped: Set<Long> = emptySet()) = DaySnapshot(
        date = "2026-09-25",
        kcalSame = 2000,
        onboardingDone = true,
        firstDay = "2026-09-25",
        logs = logs,
        slots = slots,
        skippedSlotIds = skipped,
    )

    /** States drawn in the Stitch golds home0 / home1 / homeX. */
    val home0 = day()
    val home1 = day(
        logs = listOf(
            MealLog("breakfast", "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, true, slotId = 1, carbs = 52, fat = 22),
            MealLog("lunch", "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, true, slotId = 2, carbs = 82, fat = 18, source = "photo"),
        ),
        skipped = setOf(3L),
    )
    val homeX = day(
        logs = listOf(
            MealLog("breakfast", "2 pães franceses, 2 ovos", 380, 22, true, slotId = 1, carbs = 48, fat = 12),
            MealLog("lunch", "PF de frango grelhado, arroz e feijão", 780, 64, true, slotId = 2, carbs = 88, fat = 16),
            MealLog("dinner", "Pizza brotinho e refrigerante", 1120, 82, true, slotId = 4, carbs = 104, fat = 54),
        ),
        skipped = setOf(3L),
    )
}
