package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A34: which receipt carries actions, and which ones (ADR-028 decision 5). */
class ReceiptRulesTest {
    private fun r(id: Long, vararg slots: Pair<String, Long>, active: Boolean = true) =
        ReceiptRules.Receipt(id, createdAt = id * 1000, slots = slots.toSet(), active = active)

    private val today = "2026-10-02"
    private val yesterday = "2026-10-01"

    @Test
    fun latestOfEachSlot_only() {
        val receipts = listOf(r(1, today to 1L), r(2, today to 2L), r(3, today to 1L))
        assertThat(ReceiptRules.latest(receipts)).containsExactly(2L, 3L)
    }

    @Test
    fun aMoveTouchesTwoSlots_aLaterReceiptOnEitherTakesItsActions() {
        val move = r(2, today to 1L, today to 3L)
        assertThat(ReceiptRules.latest(listOf(r(1, today to 1L), move))).containsExactly(2L)
        assertThat(ReceiptRules.latest(listOf(r(1, today to 1L), move, r(3, today to 3L)))).containsExactly(3L)
    }

    @Test
    fun otherDays_areOtherSlots() {
        val receipts = listOf(r(1, yesterday to 1L), r(2, today to 1L))
        assertThat(ReceiptRules.latest(receipts)).containsExactly(1L, 2L)
    }

    @Test
    fun inactiveOrOld_neverListed_butStillBlockTheOlderOnes() {
        // An old receipt (no undo data) or a marked one has no actions, and the receipt before it neither.
        val receipts = listOf(r(1, today to 1L), r(2, today to 1L, active = false))
        assertThat(ReceiptRules.latest(receipts)).isEmpty()
        assertThat(ReceiptRules.latest(listOf(r(1, active = true)))).isEmpty()
    }

    @Test
    fun sameTime_orderedById() {
        val a = ReceiptRules.Receipt(5, 1000, setOf(today to 1L), true)
        val b = ReceiptRules.Receipt(6, 1000, setOf(today to 1L), true)
        assertThat(ReceiptRules.latest(listOf(b, a))).containsExactly(6L)
    }

    private val record = SlotRecord("arroz", 380, 20, 40, 10)
    private fun undo(before: SlotState) = UndoData(listOf(SlotChange(today, 1, before, SlotState.of(record))))

    @Test
    fun actionTable() {
        val first = undo(SlotState.EMPTY)
        val replaced = undo(SlotState.of(record.copy(kcal = 200)))
        assertThat(ReceiptRules.actions(ReceiptRules.LOGGED, "user", first))
            .containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.LOGGED, "photo", first))
            .containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.LOGGED, "plan", first))
            .containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.LOGGED, "routine", first))
            .containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.REPLACED, "user", replaced))
            .containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.REPLACED, "photo", replaced))
            .containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.MOVED, "user", first))
            .containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        // A restore has no Desfazer even though its slot had something before it.
        assertThat(ReceiptRules.actions(ReceiptRules.RESTORED, "user", replaced))
            .containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ReceiptRules.actions(ReceiptRules.SKIPPED, null, undo(SlotState.EMPTY))).containsExactly(ReceiptAction.UNDO)
        // A record over a skip: Desfazer brings the skip back.
        assertThat(ReceiptRules.actions(ReceiptRules.LOGGED, "user", undo(SlotState.SKIPPED)))
            .containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
    }

    @Test
    fun undoData_roundTrip() {
        val fact = Fact("D1", MemoryRules.DYNAMIC, MemoryRules.ROUTINE, "cafe", "pão com ovo", "1", MemoryRules.OBSERVED, listOf(today), today, 380, 20, 40, 10)
        val data = UndoData(
            slots = listOf(
                SlotChange(today, 1, SlotState.of(record), SlotState.EMPTY),
                SlotChange(today, 3, SlotState.SKIPPED, SlotState.of(record)),
            ),
            facts = listOf(FactImage("D1", before = null, after = fact)),
            routine = listOf(RoutineUpdate(MemoryRules.ADD, null, MemoryRules.DYNAMIC, MemoryRules.ROUTINE, "cafe", "pão com ovo", "1")),
        )
        val back = UndoData.decode(data.encode())
        assertThat(back).isEqualTo(data)
        assertThat(back!!.recordSlot!!.slotId).isEqualTo(3)
        assertThat(UndoData.decode("not json")).isNull()
        assertThat(UndoData.decode(null)).isNull()
    }
}
