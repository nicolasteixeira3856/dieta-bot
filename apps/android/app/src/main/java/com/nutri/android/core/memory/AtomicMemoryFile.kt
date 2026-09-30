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
 * mid-write leaves the previous memory intact; a stale .new is never read. The A8 file
 * ([LegacyMemory]) is deleted unread: memory v2 starts empty (A28).
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
        if (legacy.exists()) legacy.delete()
        if (!base.exists()) return null
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

    private companion object {
        const val FILE_NAME = "memory.bin"
        const val TAG = "DietaBotMemory"
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
    abstract fun legacyMemory(impl: FileLegacyMemory): LegacyMemory
}
