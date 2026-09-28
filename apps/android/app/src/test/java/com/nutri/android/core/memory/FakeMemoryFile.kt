package com.nutri.android.core.memory

/** In-memory [MemoryFile] for JVM tests (Robolectric has no AndroidKeyStore for EncryptedFile). */
class FakeMemoryFile(var text: String? = null) : MemoryFile {
    var writes = 0

    override fun read(): String? = text

    override fun write(text: String) {
        writes++
        this.text = text
    }
}
