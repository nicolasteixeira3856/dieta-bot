package com.nutri.android.core.memory

import android.content.Context
import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * filesDir/memory.bin, sealed by [MemoryCipher]. Atomic write: the new bytes go to memory.bin.new,
 * are fsynced, then moved over memory.bin with ATOMIC_MOVE (rename(2) on Android). A crash
 * mid-write leaves the previous memory intact; a stale .new is never read. First read migrates the
 * A8 file ([LegacyMemory]).
 */
class AtomicMemoryFile(
    dir: File,
    private val cipher: MemoryCipher,
    private val legacy: LegacyMemory,
) : MemoryFile {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        cipher: MemoryCipher,
        legacy: LegacyMemory,
    ) : this(context.filesDir, cipher, legacy)

    private val base = File(dir, FILE_NAME)
    private val pending = File(dir, "$FILE_NAME.new")

    override fun read(): String? {
        if (!base.exists()) {
            if (legacy.exists()) return migrate()
            return null
        }
        // Crash after writing memory.bin but before removing the A8 file: memory.bin wins.
        if (legacy.exists()) legacy.delete()
        return runCatching { cipher.decrypt(base.readBytes()).toString(Charsets.UTF_8) }
            .onFailure {
                // Key gone or bytes altered: the memory is lost, the chat keeps working without it.
                Log.w(TAG, "memory unreadable: ${it.javaClass.simpleName}")
                base.delete()
            }
            .getOrNull()
    }

    override fun write(text: String) {
        val sealed = cipher.encrypt(text.toByteArray(Charsets.UTF_8))
        try {
            FileOutputStream(pending).use {
                it.write(sealed)
                it.fd.sync()
            }
            Files.move(pending.toPath(), base.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            pending.delete()
            throw e
        }
    }

    /** A8 memory.txt → memory.bin. The old file goes only after the new one is safely written. */
    private fun migrate(): String? {
        val text = runCatching { legacy.read() }.getOrNull()
        if (text == null) {
            Log.w(TAG, "legacy memory unreadable")
            legacy.delete()
            return null
        }
        // A failed write keeps memory.txt: the next read tries again. This session still gets the text.
        runCatching { write(text) }
            .onSuccess { legacy.delete() }
            .onFailure { Log.w(TAG, "migration write failed: ${it.javaClass.simpleName}") }
        return text
    }

    private companion object {
        const val FILE_NAME = "memory.bin"
        const val TAG = "NutriMemory"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoryModule {
    @Binds
    @Singleton
    abstract fun memoryFile(impl: AtomicMemoryFile): MemoryFile

    @Binds
    abstract fun memoryCipher(impl: KeystoreMemoryCipher): MemoryCipher

    @Binds
    abstract fun legacyMemory(impl: EncryptedFileLegacyMemory): LegacyMemory
}
