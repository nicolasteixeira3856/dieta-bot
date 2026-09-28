package com.nutri.android.core.memory

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** A8 memory file, read once to migrate. */
interface LegacyMemory {
    fun exists(): Boolean

    /** Throws when it cannot be decrypted. */
    fun read(): String

    fun delete()
}

/**
 * A8 format: filesDir/memory.txt through Jetpack Security EncryptedFile. Only here, only for the
 * migration: EncryptedFile/MasterKey are deprecated in security-crypto 1.1.0. Drop this class and
 * the dependency once no device still has memory.txt (A8b).
 */
@Suppress("DEPRECATION")
class EncryptedFileLegacyMemory @Inject constructor(@ApplicationContext private val context: Context) : LegacyMemory {
    private val target = File(context.filesDir, FILE_NAME)

    override fun exists(): Boolean = target.exists()

    override fun read(): String {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val encrypted = EncryptedFile.Builder(context, target, key, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        return encrypted.openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }
    }

    override fun delete() {
        target.delete()
    }

    private companion object {
        const val FILE_NAME = "memory.txt"
    }
}
