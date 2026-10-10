package app.fibrai.android.core.memory

import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.FactImage
import app.fibrai.android.domain.Memory
import app.fibrai.android.domain.MemoryResult
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.MemoryUpdate
import app.fibrai.android.domain.NextIds
import app.fibrai.android.domain.RecordedMeal
import app.fibrai.android.domain.Tombstone
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** [FactMemory.revertAndApply]: the apply result and the revert counts. */
data class MemoryEdit(val result: MemoryResult, val reverted: Int, val kept: Int)

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

    /**
     * Applies the AI updates; [recorded] is the meal of a record (routine only). The result carries the
     * images of the facts it changed (A34), measured after expiry so an expired fact never comes back.
     */
    suspend fun apply(updates: List<MemoryUpdate>, today: LocalDate, recorded: RecordedMeal? = null): MemoryResult =
        edit(emptyList(), updates, today, recorded).result

    /**
     * A34: reverts [images] (Excluir, Desfazer, Editar), then applies [updates] with [recorded] (Trocar
     * refeição), in one locked write. The images of the result cover both steps. A record never adds,
     * removes or reverts a temp fact (A38): temp updates and images are dropped here.
     */
    suspend fun revertAndApply(
        images: List<FactImage>,
        updates: List<MemoryUpdate>,
        today: LocalDate,
        recorded: RecordedMeal? = null,
    ): MemoryEdit = edit(
        images.filterNot { it.temp },
        updates.filterNot { it.kind == MemoryRules.TEMP || it.id?.startsWith("T") == true },
        today,
        recorded,
    )

    private suspend fun edit(
        images: List<FactImage>,
        updates: List<MemoryUpdate>,
        today: LocalDate,
        recorded: RecordedMeal?,
    ): MemoryEdit = mutex.withLock {
        val stored = load()
        val expired = MemoryRules.expire(stored, today)
        val base = expired.memory
        val undone = MemoryRules.revert(base, images)
        val result = MemoryRules.apply(undone.memory, updates, today, recorded)
        if (result.memory != stored) store(result.memory)
        val counts = result.counts + expired.counts
        MemoryEdit(result.copy(counts = counts, images = MemoryRules.images(base, result.memory)), undone.reverted, undone.kept)
    }

    /** A29 "Registrar assim": the suggested routine was recorded as is. */
    suspend fun reinforceRoutine(factId: String, meal: RecordedMeal, today: LocalDate): MemoryResult {
        val update = MemoryUpdate(MemoryRules.REINFORCE, factId, MemoryRules.DYNAMIC, MemoryRules.ROUTINE, "", "", meal.slot)
        return apply(listOf(update), today, meal)
    }

    /**
     * A71 (ADR-057 decisions 5, 8): the facts of the profile build, stored as declared ([MemoryRules.declare]). A fact whose
     * key is already kept takes the new text, so a repeated build never duplicates. Returns how many were stored.
     */
    suspend fun declare(facts: List<MemoryUpdate>, today: LocalDate): Int = mutex.withLock {
        val stored = load()
        val base = MemoryRules.expire(stored, today).memory
        val next = MemoryRules.declare(base, facts, today)
        if (next != stored) store(next)
        next.facts.count { it.source == MemoryRules.DECLARED && it.created == today.toString() }
    }

    /** The whole memory as given (tests seed it). */
    suspend fun replaceAll(memory: Memory) = mutex.withLock { store(memory) }

    /** A69 (memL): the user deletes a fact; its key is kept as a tombstone. Returns the memory after expiry. */
    suspend fun delete(id: String, today: LocalDate): Memory = change(today) { MemoryRules.delete(it, id, today) }

    /** A69 (memL): the user corrects a fact's text. Returns the memory after expiry. */
    suspend fun correct(id: String, text: String, today: LocalDate): Memory = change(today) { MemoryRules.correct(it, id, text) }

    /** A69: a compaction was stored; the tombstones end. */
    suspend fun clearTombstones(today: LocalDate) {
        change(today) { MemoryRules.clearTombstones(it) }
    }

    private suspend fun change(today: LocalDate, transform: (Memory) -> Memory): Memory = mutex.withLock {
        val stored = load()
        val next = transform(MemoryRules.expire(stored, today).memory)
        if (next != stored) store(next)
        next
    }

    private suspend fun store(memory: Memory) = withContext(Dispatchers.IO) {
        file.write(json.encodeToString(Stored.serializer(), Stored(VERSION, memory.next, memory.facts, memory.tombstones)))
    }

    private suspend fun load(): Memory = withContext(Dispatchers.IO) {
        val text = file.read() ?: return@withContext Memory()
        runCatching { json.decodeFromString(Stored.serializer(), text) }.getOrNull()
            ?.takeIf { it.v == VERSION }
            ?.let { Memory(it.next, it.facts, it.tombstones) }
            ?: Memory()
    }

    @Serializable
    private data class Stored(val v: Int, val next: NextIds = NextIds(), val facts: List<Fact> = emptyList(), val tombstones: List<Tombstone> = emptyList())

    companion object {
        const val VERSION = 2
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
