package com.nutri.android.core.memory

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** A8 memory file. Memory v2 (A28) starts empty: it is deleted, never read (ADR-023). */
interface LegacyMemory {
    fun exists(): Boolean

    fun delete()
}

/** A8 format: filesDir/memory.txt (EncryptedFile). Only its presence matters now. */
class FileLegacyMemory @Inject constructor(@ApplicationContext context: Context) : LegacyMemory {
    private val target = File(context.filesDir, FILE_NAME)

    override fun exists(): Boolean = target.exists()

    override fun delete() {
        target.delete()
    }

    private companion object {
        const val FILE_NAME = "memory.txt"
    }
}
