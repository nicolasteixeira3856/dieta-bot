package com.nutri.android.core.memory

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MemoryStoreTest {
    @Test
    fun fiftyAppends_stayUnder4000_oldestLeaveFirst() = runBlocking<Unit> {
        val file = FakeMemoryFile()
        val store = MemoryStore(file)
        repeat(50) { i -> store.append("Almoço: prato feito número $i com frango, arroz, feijão e salada verde (780 kcal)") }

        val text = store.read()
        assertThat(text.length).isAtMost(MemoryStore.MAX_CHARS)
        val lines = text.lines()
        assertThat(lines.last()).contains("número 49 ")
        assertThat(text).doesNotContain("número 0 ")
        assertThat(lines.map { it.substringAfter("número ").substringBefore(" ").toInt() }).isInOrder()
    }

    @Test
    fun oneLinePerAppend_trimmedAndCapped() = runBlocking<Unit> {
        val store = MemoryStore(FakeMemoryFile())
        store.append("  Café:\n2 ovos  \t e pão  ")
        store.append("   ")
        store.append("x".repeat(1000))

        val lines = store.read().lines()
        assertThat(lines).hasSize(2)
        assertThat(lines[0]).isEqualTo("Café: 2 ovos e pão")
        assertThat(lines[1]).hasLength(MemoryStore.MAX_LINE_CHARS)
    }

    @Test
    fun emptyOrUnreadableFile_readsEmpty() = runBlocking<Unit> {
        assertThat(MemoryStore(FakeMemoryFile(null)).read()).isEmpty()
        val file = FakeMemoryFile("linha antiga\n\n")
        val store = MemoryStore(file)
        store.append("nova")
        assertThat(file.text).isEqualTo("linha antiga\nnova")
    }
}
