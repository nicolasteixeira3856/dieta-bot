package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class PushPlanTest {
    private val slots = listOf(
        PushSlot(1, "Café da manhã", 7 * 60 + 30),
        PushSlot(2, "Almoço", 12 * 60 + 30),
        PushSlot(3, "Lanche", 16 * 60),
        PushSlot(4, "Jantar", 20 * 60),
    )
    private val day = LocalDate.parse("2026-09-25")
    private val sixAm = Instant.parse("2026-09-25T06:00:00-03:00")

    @Test
    fun fourEmptySlots_fourAlarms_atTheirTimeInSaoPaulo() {
        val alarms = PushPlan.alarms(slots, emptySet(), emptySet(), day, sixAm)
        assertThat(alarms.map { it.slotId }).containsExactly(1L, 2L, 3L, 4L).inOrder()
        assertThat(alarms.last().at).isEqualTo(Instant.parse("2026-09-25T20:00:00-03:00"))
    }

    @Test
    fun loggedOrSkippedSlot_hasNoAlarm_pastSlotNeither() {
        val alarms = PushPlan.alarms(slots, loggedSlotIds = setOf(2), skippedSlotIds = setOf(3), dayDate = day, now = Instant.parse("2026-09-25T08:00:00-03:00"))
        assertThat(alarms.map { it.slotId }).containsExactly(4L)
    }

    @Test
    fun staleSnapshot_afterMidnight_countsAsEmptyDay() {
        val next = Instant.parse("2026-09-26T00:05:00-03:00")
        val alarms = PushPlan.alarms(slots, setOf(1, 2, 3, 4), setOf(1), dayDate = day, now = next)
        assertThat(alarms).hasSize(4)
        assertThat(alarms.first().at).isEqualTo(Instant.parse("2026-09-26T07:30:00-03:00"))
    }

    @Test
    fun weekend_usesTheRegisteredSlots_noLunchDinnerOnlyRule() {
        val saturday = Instant.parse("2026-09-26T06:00:00-03:00")
        assertThat(PushPlan.alarms(slots, emptySet(), emptySet(), LocalDate.parse("2026-09-26"), saturday)).hasSize(4)
    }

    @Test
    fun resyncAt0005SaoPaulo() {
        assertThat(PushPlan.nextResync(sixAm)).isEqualTo(Instant.parse("2026-09-26T00:05:00-03:00"))
        assertThat(PushPlan.nextResync(Instant.parse("2026-09-25T00:01:00-03:00"))).isEqualTo(Instant.parse("2026-09-25T00:05:00-03:00"))
    }

    @Test
    fun copyAndFireTimeCheck() {
        assertThat(PushPlan.copy("Jantar")).isEqualTo("Jantar. Ainda não registrou.")
        assertThat(PushPlan.shouldNotify(4, emptySet(), emptySet())).isTrue()
        assertThat(PushPlan.shouldNotify(4, setOf(4), emptySet())).isFalse()
        assertThat(PushPlan.shouldNotify(4, emptySet(), setOf(4))).isFalse()
    }
}
