package com.nutri.android.core.memory

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Raw storage of the memory text. Production: [EncryptedMemoryFile]. */
interface MemoryFile {
    /** Null when there is no file (or it cannot be decrypted). */
    fun read(): String?

    fun write(text: String)
}

/**
 * Short memory sent as `memory` in every POST /v1/chat (spec memoria-push). One line per fact,
 * oldest first. Grows through [append]: after Gravar or an answered assumption. The dev editor
 * (A23) rewrites it with [replace]. There is no clear: wipeToday keeps it, uninstall removes it
 * (filesDir, allowBackup=false).
 */
@Singleton
class MemoryStore @Inject constructor(private val file: MemoryFile) {
    private val mutex = Mutex()

    suspend fun read(): String = mutex.withLock { load().joinToString("\n") }

    /** Adds one line; the oldest lines leave while the text is over [MAX_CHARS]. */
    suspend fun append(line: String) {
        val clean = clean(line)
        if (clean.isEmpty()) return
        mutex.withLock { store(load() + clean) }
    }

    /**
     * A23 dev editor: the whole text becomes [text], same line rules and [MAX_CHARS] cut as
     * [append]. Never a delete: a text with no line is refused (false) and nothing is written.
     */
    suspend fun replace(text: String): Boolean {
        val lines = text.lines().map(::clean).filter { it.isNotEmpty() }
        if (lines.isEmpty()) return false
        mutex.withLock { store(lines) }
        return true
    }

    private fun clean(line: String) = line.replace(Regex("\\s+"), " ").trim().take(MAX_LINE_CHARS)

    private suspend fun store(all: List<String>) {
        val lines = all.toMutableList()
        while (lines.size > 1 && lines.joinToString("\n").length > MAX_CHARS) lines.removeAt(0)
        withContext(Dispatchers.IO) { file.write(lines.joinToString("\n")) }
    }

    private suspend fun load(): List<String> = withContext(Dispatchers.IO) {
        file.read().orEmpty().lines().filter { it.isNotBlank() }
    }

    companion object {
        /** ~1k tokens (chars / 4): with the profile it stays under the ~1.3k tok budget. */
        const val MAX_CHARS = 4000
        const val MAX_LINE_CHARS = 240
    }
}
