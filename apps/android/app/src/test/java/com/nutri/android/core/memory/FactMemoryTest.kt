package com.nutri.android.core.memory

import com.google.common.truth.Truth.assertThat
import com.nutri.android.domain.MemoryUpdate
import com.nutri.android.domain.RecordedMeal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FactMemoryTest {
    private val today = LocalDate.parse("2026-09-30")
    private val leite = MemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado")

    @Test
    fun oldTextMemory_readsEmpty_andIsOverwrittenOnFirstWrite() = runBlocking<Unit> {
        val file = FakeMemoryFile("Café da manhã: 2 ovos (380 kcal)\nRespondeu \"pão?\": francês")
        val memory = FactMemory(file)
        assertThat(memory.read(today).facts).isEmpty()
        assertThat(file.writes).isEqualTo(0)

        memory.apply(listOf(leite), today)
        assertThat(file.text).startsWith("{\"v\":2,")
        assertThat(file.text).doesNotContain("Respondeu")
        assertThat(memory.read(today).facts.single().id).isEqualTo("P1")
    }

    @Test
    fun brokenJsonOrOtherVersion_readsEmpty() = runBlocking<Unit> {
        assertThat(FactMemory(FakeMemoryFile("{\"v\":2,\"facts\":[{")).read(today).facts).isEmpty()
        assertThat(FactMemory(FakeMemoryFile("{\"v\":3,\"facts\":[]}")).read(today).facts).isEmpty()
        assertThat(FactMemory(FakeMemoryFile(null)).read(today).facts).isEmpty()
    }

    @Test
    fun storedFormat_hasVersionNextAndEveryField() = runBlocking<Unit> {
        val file = FakeMemoryFile()
        FactMemory(file).apply(listOf(leite), today)
        assertThat(file.text).isEqualTo(
            "{\"v\":2,\"next\":{\"P\":2,\"D\":1},\"facts\":[{\"id\":\"P1\",\"kind\":\"permanent\",\"category\":\"preference\"," +
                "\"key\":\"leite\",\"text\":\"Leite semidesnatado\",\"slot\":null,\"source\":\"explicit\",\"days\":[\"2026-09-30\"]," +
                "\"created\":\"2026-09-30\",\"kcal\":null,\"p\":null,\"c\":null,\"g\":null}]}",
        )
    }

    @Test
    fun nothingApplied_nothingWritten() = runBlocking<Unit> {
        val file = FakeMemoryFile()
        val memory = FactMemory(file)
        val r = memory.apply(listOf(MemoryUpdate("remove", "P9", "permanent", "preference", "x", "x")), today)
        assertThat(r.changed).isFalse()
        assertThat(file.writes).isEqualTo(0)
    }

    @Test
    fun read_expiresWithoutWriting_nextApplyStoresIt() = runBlocking<Unit> {
        val file = FakeMemoryFile()
        val memory = FactMemory(file)
        memory.apply(listOf(MemoryUpdate("add", null, "dynamic", "portion", "arroz", "4 colheres de arroz")), today)
        val later = today.plusDays(21)
        assertThat(memory.read(later).facts).isEmpty()
        assertThat(file.writes).isEqualTo(1)

        val r = memory.apply(listOf(leite), later)
        assertThat(r.counts["expire"]).isEqualTo(1)
        assertThat(file.text).doesNotContain("arroz")
    }

    @Test
    fun reinforceRoutine_takesTheRecordedMeal() = runBlocking<Unit> {
        val memory = FactMemory(FakeMemoryFile())
        val meal = RecordedMeal("1", 440, 25, 38, 22)
        memory.apply(listOf(MemoryUpdate("add", null, "dynamic", "routine", "cafe", "2 ovos", "1")), today, meal)
        val next = today.plusDays(1)
        val r = memory.reinforceRoutine("D1", meal.copy(kcal = 460), next)
        val f = r.memory.facts.single()
        assertThat(f.days).containsExactly(today.toString(), next.toString()).inOrder()
        assertThat(f.kcal).isEqualTo(460)
        assertThat(f.text).isEqualTo("2 ovos")
    }

    private val routine = MemoryUpdate("add", null, "dynamic", "routine", "cafe", "2 ovos", "1")

    @Test
    fun apply_returnsTheImagesOfWhatItChanged() = runBlocking<Unit> {
        val memory = FactMemory(FakeMemoryFile())
        memory.apply(listOf(leite), today)
        val r = memory.apply(listOf(routine), today, RecordedMeal("1", 440, 25, 38, 22))
        val image = r.images.single()
        assertThat(image.id).isEqualTo("D1")
        assertThat(image.before).isNull()
        assertThat(image.after!!.kcal).isEqualTo(440)
    }

    @Test
    fun revert_unchangedFactGoesBack_addedLeaves_changedSinceIsKept() = runBlocking<Unit> {
        val memory = FactMemory(FakeMemoryFile())
        memory.apply(listOf(routine), today, RecordedMeal("1", 440, 25, 38, 22))
        val next = today.plusDays(1)
        val reinforce = memory.reinforceRoutine("D1", RecordedMeal("1", 460, 25, 38, 22), next)
        val added = memory.apply(listOf(MemoryUpdate("add", null, "dynamic", "portion", "arroz", "4 colheres")), next)

        // Reverting the reinforce: D1 still equals its post image, so it gets its old days and kcal back.
        val undo = memory.revertAndApply(reinforce.images, emptyList(), next)
        assertThat(undo.reverted).isEqualTo(1)
        assertThat(undo.kept).isEqualTo(0)
        val d1 = memory.read(next).facts.single { it.id == "D1" }
        assertThat(d1.days).containsExactly(today.toString())
        assertThat(d1.kcal).isEqualTo(440)

        // A fact changed since the receipt is left as it is.
        memory.apply(listOf(MemoryUpdate("replace", "D2", "dynamic", "portion", "arroz", "5 colheres")), next)
        val kept = memory.revertAndApply(added.images, emptyList(), next)
        assertThat(kept.reverted).isEqualTo(0)
        assertThat(kept.kept).isEqualTo(1)
        assertThat(memory.read(next).facts.single { it.id == "D2" }.text).isEqualTo("5 colheres")
    }

    @Test
    fun revert_ofAnAdd_removesIt_ofARemove_bringsItBack() = runBlocking<Unit> {
        val memory = FactMemory(FakeMemoryFile())
        val add = memory.apply(listOf(leite), today)
        assertThat(memory.revertAndApply(add.images, emptyList(), today).reverted).isEqualTo(1)
        assertThat(memory.read(today).facts).isEmpty()

        memory.apply(listOf(leite), today)
        val remove = memory.apply(listOf(MemoryUpdate("remove", "P2", "permanent", "preference", "leite", "")), today)
        assertThat(memory.read(today).facts).isEmpty()
        memory.revertAndApply(remove.images, emptyList(), today)
        assertThat(memory.read(today).facts.single().id).isEqualTo("P2")
    }
}
