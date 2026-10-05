package app.fibrai.android.feature.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlotScheduleDraftTest {
    @Test fun modeChangeRequiresConfirmationAndCancelPreservesDraft() {
        val stored = SlotScheduleDraft().withSlots(listOf(SlotDraft(9, "Café", 450), SlotDraft(10, "Jantar", 1200)))
        val requested = stored.requestMode("split")
        assertThat(requested.mode).isEqualTo("same")
        assertThat(requested.discardedGroups.map { it.days }).containsExactly(127)
        assertThat(requested.copy(pendingMode = null)).isEqualTo(stored)
        assertThat(requested.confirmMode().drafts).isEmpty()
    }

    @Test fun unchangedModeKeepsExistingGroupsAndMovingBackKeepsEdits() {
        val first = listOf(SlotDraft(1, "Café", 450), SlotDraft(2, "Jantar", 1200))
        val second = listOf(SlotDraft(3, "Almoço", 810), SlotDraft(4, "Ceia", 1260))
        val schedule = SlotScheduleDraft(mode = "split").withSlots(first).copy(index = 1).withSlots(second)
        assertThat(schedule.requestMode("split")).isEqualTo(schedule)
        assertThat(schedule.copy(index = 0).slots).isEqualTo(first)
        assertThat(schedule.copy(index = 0).copy(index = 1).slots).isEqualTo(second)
        assertThat(schedule.meals().map { it.days }).containsExactly(31, 31, 96, 96).inOrder()
    }

    @Test fun copyUsesPreviousValuesAndDestinationIds() {
        val schedule = SlotScheduleDraft(mode = "split")
            .withSlots(listOf(SlotDraft(1, "Café", 450), SlotDraft(2, "Jantar", 1200)))
            .copy(index = 1).withSlots(listOf(SlotDraft(3, "Almoço", 810), SlotDraft(4, "Ceia", 1260)))
        assertThat(schedule.copyPrevious().slots).containsExactly(SlotDraft(3, "Café", 450), SlotDraft(4, "Jantar", 1200)).inOrder()
        assertThat(schedule.copyPrevious().valid).isTrue()
    }

    @Test fun untouchedModeSwitchDoesNotAskAndUnfilledGroupsCannotSave() {
        val split = SlotScheduleDraft().withSlots(SlotScheduleDraft.defaults()).requestMode("split")
        assertThat(split.mode).isEqualTo("split")
        assertThat(split.pendingMode).isNull()
        assertThat(split.valid).isFalse()
    }
}
