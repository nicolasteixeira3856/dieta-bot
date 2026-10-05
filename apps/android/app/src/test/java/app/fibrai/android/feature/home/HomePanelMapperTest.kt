package app.fibrai.android.feature.home

import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.MealLog
import app.fibrai.android.core.database.MealSlot
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

    @Test
    fun `workout row - no workout reads Informar, credit 0`() {
        val ui = HomePanelMapper.map(HomeFixtures.day().copy(eat = "partial", pct = 50), today)
        assertThat(ui.workoutKcal).isNull()
        assertThat(ui.workoutCredit).isEqualTo(0)
        assertThat(ui.meta).isEqualTo(2000)
        assertThat(ui.workoutEditor).isNull()
    }

    @Test
    fun `workout row - credit follows the eat-back policy 0, 50 and 100 percent`() {
        val day = HomeFixtures.day().copy(workoutKcal = 350)
        val zero = HomePanelMapper.map(day.copy(eat = "zero"), today)
        assertThat(zero.workoutKcal).isEqualTo(350)
        assertThat(zero.workoutCredit).isEqualTo(0)
        assertThat(zero.meta).isEqualTo(2000)
        val half = HomePanelMapper.map(day.copy(eat = "partial", pct = 50), today)
        assertThat(half.workoutCredit).isEqualTo(175)
        assertThat(half.meta).isEqualTo(2175)
        val full = HomePanelMapper.map(day.copy(eat = "full"), today)
        assertThat(full.workoutCredit).isEqualTo(350)
        assertThat(full.meta).isEqualTo(2350)
    }

    @Test
    fun `open sheet carries the draft and a live credit line`() {
        val day = HomeFixtures.day().copy(eat = "partial", pct = 50, workoutKcal = 200)
        val ui = HomePanelMapper.map(day, today, workoutDraft = "350")
        assertThat(ui.workoutEditor?.creditLine).isEqualTo("+175 kcal na meta de hoje (compensação 50%)")
        // The stored value drives the row until Salvar.
        assertThat(ui.workoutCredit).isEqualTo(100)
    }
    @Test fun saturdayTimelineContainsOnlyWeekendAndPreservesOtherLogs() {
        val day = HomeFixtures.day().copy(
            slotMode = "split",
            slots = listOf(MealSlot(1, "Útil", 450, 31), MealSlot(2, "Sábado", 570, 96)),
            logs = listOf(MealLog("", "ovo", 380, 22, true, slotId = 1)),
        )
        val ui = HomePanelMapper.map(day, LocalDate.parse("2026-09-26"))
        assertThat(ui.slotCount).isEqualTo(1)
        assertThat(ui.timeline.map { it.name }).containsExactly("Sábado", "Outros").inOrder()
        assertThat(ui.consumed).isEqualTo(380)
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

    /** A22 golds: "350 kcal · +175 na meta" and "200 kcal · +100 na meta" with the pill still at 2000. */
    val home1Workout = home1.copy(kcalSame = 1825, eat = "partial", pct = 50, workoutKcal = 350)
    val homeXWorkout = homeX.copy(kcalSame = 1900, eat = "partial", pct = 50, workoutKcal = 200)

}
