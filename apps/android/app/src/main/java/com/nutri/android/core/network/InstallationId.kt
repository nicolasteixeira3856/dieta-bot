package com.nutri.android.core.network

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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Pseudonymous installation signal sent as X-Client-Instance-Id (CP4, ADR-025). Not authentication. */
interface InstallationId {
    /** Blocking on the first call (file read/write): call off the UI thread. */
    fun get(): String
}

/**
 * noBackupFilesDir/installation_id: one random UUID v4 per installation. Survives restarts, updates
 * and the daily wipe (Room only); clear-data or uninstall resets it; dev and prod have separate
 * app-private dirs. Atomic write (.new + fsync + ATOMIC_MOVE). A missing or corrupt file creates a
 * fresh id; a failed write keeps a random id for this process only. Never a device identifier.
 */
class FileInstallationId(
    dir: File,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : InstallationId {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.noBackupFilesDir)

    private val base = File(dir, FILE_NAME)
    private val pending = File(dir, "$FILE_NAME.new")

    @Volatile
    private var cached: String? = null

    override fun get(): String {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val id = read() ?: create()
            cached = id
            return id
        }
    }

    private fun read(): String? {
        if (!base.exists()) return null
        val text = runCatching { base.readText(Charsets.US_ASCII).trim() }.getOrNull()
        if (text != null && isCanonical(text)) return text
        Log.w(TAG, CODE_CORRUPT)
        return null
    }

    private fun create(): String {
        val id = newId()
        try {
            base.parentFile?.mkdirs()
            FileOutputStream(pending).use {
                it.write(id.toByteArray(Charsets.US_ASCII))
                it.fd.sync()
            }
            Files.move(pending.toPath(), base.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            pending.delete()
            Log.w(TAG, CODE_WRITE_FAILED)
        }
        return id
    }

    companion object {
        private const val FILE_NAME = "installation_id"
        private const val TAG = "DietaBotInstallation"
        const val CODE_CORRUPT = "installation_id_corrupt"
        const val CODE_WRITE_FAILED = "installation_id_write_failed"

        private val CANONICAL = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

        /** The server's accepted shape (api-contract): lowercase UUID v4, 36 characters. */
        fun isCanonical(value: String): Boolean = CANONICAL.matches(value)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallationIdModule {
    @Binds
    @Singleton
    abstract fun installationId(impl: FileInstallationId): InstallationId
}
