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
}
