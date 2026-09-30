package com.nutri.android.core.memory

import com.nutri.android.domain.Fact
import com.nutri.android.domain.Memory
import com.nutri.android.domain.MemoryResult
import com.nutri.android.domain.MemoryRules
import com.nutri.android.domain.MemoryUpdate
import com.nutri.android.domain.NextIds
import com.nutri.android.domain.RecordedMeal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Raw storage of the memory bytes as text. Production: [AtomicMemoryFile]. */
interface MemoryFile {
    /** Null when there is no file (or it cannot be decrypted). */
    fun read(): String?

    fun write(text: String)
}

/**
 * Memory v2 (A28, ADR-023): a short list of [Fact]s, JSON inside the same encrypted memory.bin.
 * Anything that is not `v: 2` (the A8 text memory) reads as empty and is overwritten by the next
 * write: no conversion. The rules live in [MemoryRules]; this class only loads, expires and stores.
 * wipeToday keeps it, uninstall removes it (filesDir, allowBackup=false).
 */
@Singleton
class FactMemory @Inject constructor(private val file: MemoryFile) {
    private val mutex = Mutex()

    /** Facts after expiration: what the next POST sends. */
    suspend fun read(today: LocalDate): Memory = mutex.withLock { MemoryRules.expire(load(), today).memory }

    /** Applies the AI updates; [recorded] is the meal of a Gravar/Substituir (routine only). */
    suspend fun apply(updates: List<MemoryUpdate>, today: LocalDate, recorded: RecordedMeal? = null): MemoryResult =
        mutex.withLock {
            val before = load()
            val result = MemoryRules.apply(before, updates, today, recorded)
            if (result.memory != before) store(result.memory)
            result
        }

    /** A29 "Registrar assim": the suggested routine was recorded as is. */
    suspend fun reinforceRoutine(factId: String, meal: RecordedMeal, today: LocalDate): MemoryResult {
        val update = MemoryUpdate(MemoryRules.REINFORCE, factId, MemoryRules.DYNAMIC, MemoryRules.ROUTINE, "", "", meal.slot)
        return apply(listOf(update), today, meal)
    }

    /** A23 dev editor: the whole memory, already validated by the editor. */
    suspend fun replaceAll(memory: Memory) = mutex.withLock { store(memory) }

    private suspend fun store(memory: Memory) = withContext(Dispatchers.IO) {
        file.write(json.encodeToString(Stored.serializer(), Stored(VERSION, memory.next, memory.facts)))
    }

    private suspend fun load(): Memory = withContext(Dispatchers.IO) {
        val text = file.read() ?: return@withContext Memory()
        runCatching { json.decodeFromString(Stored.serializer(), text) }.getOrNull()
            ?.takeIf { it.v == VERSION }
            ?.let { Memory(it.next, it.facts) }
            ?: Memory()
    }

    @Serializable
    private data class Stored(val v: Int, val next: NextIds = NextIds(), val facts: List<Fact> = emptyList())

    companion object {
        const val VERSION = 2
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
