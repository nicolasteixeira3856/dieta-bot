package com.nutri.android.core.memory

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric has no AndroidKeyStore: same AES-GCM envelope, software key. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AtomicMemoryFileTest {
    private lateinit var dir: File
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesGcmMemoryCipher { key }
    private val legacy = FakeLegacy()

    @Before
    fun setUp() {
        dir = File(ApplicationProvider.getApplicationContext<Application>().cacheDir, "mem_${System.nanoTime()}").apply { mkdirs() }
    }

    private fun file(c: MemoryCipher = cipher) = AtomicMemoryFile(dir, c, legacy)
    private val bin get() = File(dir, "memory.bin")

    @Test
    fun roundTrip_andNotPlaintextOnDisk() {
        file().write("Café da manhã: 2 ovos (380 kcal)")
        assertThat(file().read()).isEqualTo("Café da manhã: 2 ovos (380 kcal)")
        val raw = bin.readBytes()
        assertThat(String(raw, Charsets.ISO_8859_1)).doesNotContain("380 kcal")
        assertThat(String(raw, Charsets.UTF_8)).doesNotContain("ovos")
        // Randomized IV: same text, different bytes.
        file().write("Café da manhã: 2 ovos (380 kcal)")
        assertThat(bin.readBytes()).isNotEqualTo(raw)
    }

    @Test
    fun storeReplace_keepsCipherAndAtomicFile() = runBlocking<Unit> {
        val store = MemoryStore(file())
        store.append("Almoço: frango (600 kcal)")
        assertThat(store.replace("Jantar: sopa de legumes (350 kcal)")).isTrue()

        assertThat(file().read()).isEqualTo("Jantar: sopa de legumes (350 kcal)")
        assertThat(String(bin.readBytes(), Charsets.UTF_8)).doesNotContain("sopa")
        assertThat(File(dir, "memory.bin.new").exists()).isFalse()
    }

    @Test
    fun missing_readsNull() {
        assertThat(file().read()).isNull()
    }

    @Test
    fun crashMidWrite_keepsPreviousMemory() {
        file().write("linha 1")
        // Process dies after writing part of the new bytes, before the move.
        File(dir, "memory.bin.new").writeBytes(cipher.encrypt("linha 1\nlinha 2".toByteArray()).copyOf(10))

        assertThat(file().read()).isEqualTo("linha 1")
        file().write("linha 1\nlinha 3")
        assertThat(file().read()).isEqualTo("linha 1\nlinha 3")
    }

    @Test
    fun failedWrite_keepsPreviousMemory() {
        file().write("linha 1")
        val broken = object : MemoryCipher by cipher {
            override fun encrypt(plain: ByteArray): ByteArray = throw IOException("keystore")
        }
        runCatching { file(broken).write("linha 2") }
        assertThat(file().read()).isEqualTo("linha 1")
    }

    @Test
    fun alteredOrForeignKey_readsNullAndDeletes() {
        file().write("segredo")
        val bytes = bin.readBytes()
        bytes[bytes.size - 1] = (bytes.last() + 1).toByte()
        bin.writeBytes(bytes)
        assertThat(file().read()).isNull()
        assertThat(bin.exists()).isFalse()

        file().write("segredo")
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertThat(file(AesGcmMemoryCipher { otherKey }).read()).isNull()
    }

    @Test
    fun migration_movesLegacyThenDeletesIt() {
        legacy.text = "Almoço: PF (780 kcal)"
        assertThat(file().read()).isEqualTo("Almoço: PF (780 kcal)")
        assertThat(legacy.exists()).isFalse()
        assertThat(bin.exists()).isTrue()
        assertThat(file().read()).isEqualTo("Almoço: PF (780 kcal)")
    }

    @Test
    fun migration_failedWriteKeepsLegacy_unreadableLegacyIsDropped() {
        legacy.text = "Almoço: PF (780 kcal)"
        val broken = object : MemoryCipher by cipher {
            override fun encrypt(plain: ByteArray): ByteArray = throw IOException("keystore")
        }
        assertThat(file(broken).read()).isEqualTo("Almoço: PF (780 kcal)")
        assertThat(legacy.exists()).isTrue()
        assertThat(bin.exists()).isFalse()

        legacy.failRead = true
        assertThat(file().read()).isNull()
        assertThat(legacy.exists()).isFalse()
    }

    @Test
    fun leftoverLegacyAfterMigrationCrash_newFileWins() {
        file().write("nova")
        legacy.text = "velha"
        assertThat(file().read()).isEqualTo("nova")
        assertThat(legacy.exists()).isFalse()
    }

    @Test
    fun memoryStore_onAtomicFile_endToEnd() = runBlocking<Unit> {
        val store = MemoryStore(file())
        repeat(60) { store.append("Jantar: sopa de legumes número $it (320 kcal)") }
        val text = MemoryStore(file()).read()
        assertThat(text.length).isAtMost(MemoryStore.MAX_CHARS)
        assertThat(text.lines().last()).contains("número 59 ")
    }

    private class FakeLegacy : LegacyMemory {
        var text: String? = null
        var failRead = false

        override fun exists() = text != null

        override fun read(): String = if (failRead) throw IOException("tink") else text!!

        override fun delete() {
            text = null
        }
    }
}
