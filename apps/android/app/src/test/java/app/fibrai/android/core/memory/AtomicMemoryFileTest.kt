package app.fibrai.android.core.memory

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.domain.MemoryUpdate
import java.io.File
import java.io.IOException
import java.time.LocalDate
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
    fun factMemory_keepsCipherAndAtomicFile() = runBlocking<Unit> {
        val today = LocalDate.parse("2026-09-30")
        val memory = FactMemory(file())
        memory.apply(listOf(MemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado")), today)

        assertThat(FactMemory(file()).read(today).facts.single().text).isEqualTo("Leite semidesnatado")
        assertThat(String(bin.readBytes(), Charsets.UTF_8)).doesNotContain("semidesnatado")
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
    fun legacyA8File_isDeletedUnread_memoryStartsEmpty() = runBlocking<Unit> {
        legacy.present = true
        assertThat(file().read()).isNull()
        assertThat(legacy.exists()).isFalse()
        assertThat(FactMemory(file()).read(LocalDate.parse("2026-09-30")).facts).isEmpty()
    }

    @Test
    fun leftoverLegacy_doesNotTouchMemoryBin() {
        file().write("nova")
        legacy.present = true
        assertThat(file().read()).isEqualTo("nova")
        assertThat(legacy.exists()).isFalse()
    }

    private class FakeLegacy : LegacyMemory {
        var present = false

        override fun exists() = present

        override fun delete() {
            present = false
        }
    }
}
