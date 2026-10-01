package com.nutri.android.core.network

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class InstallationIdTest {
    private val dir: File = Files.createTempDirectory("installation").toFile()
    private val file get() = File(dir, "installation_id")

    @Test
    fun `first use creates one canonical UUID v4 and persists it`() {
        val id = FileInstallationId(dir).get()

        assertThat(FileInstallationId.isCanonical(id)).isTrue()
        assertThat(file.readText()).isEqualTo(id)
        assertThat(File(dir, "installation_id.new").exists()).isFalse()
    }

    @Test
    fun `concurrent first requests share one value`() {
        val created = AtomicInteger()
        val provider = FileInstallationId(dir) { created.incrementAndGet(); java.util.UUID.randomUUID().toString() }
        val pool = Executors.newFixedThreadPool(16)
        val start = CountDownLatch(1)
        val seen = Collections.synchronizedSet(mutableSetOf<String>())
        repeat(64) { pool.execute { start.await(); seen += provider.get() } }
        start.countDown()
        pool.shutdown()
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

        assertThat(seen).hasSize(1)
        assertThat(created.get()).isEqualTo(1)
        assertThat(file.readText()).isEqualTo(seen.single())
    }

    @Test
    fun `a recreated provider (restart, update) reads the same id`() {
        val first = FileInstallationId(dir).get()
        val again = FileInstallationId(dir) { error("must not create") }.get()

        assertThat(again).isEqualTo(first)
    }

    @Test
    fun `missing file after clear-data creates a different id`() {
        val first = FileInstallationId(dir).get()
        file.delete()

        val second = FileInstallationId(dir).get()

        assertThat(second).isNotEqualTo(first)
        assertThat(FileInstallationId.isCanonical(second)).isTrue()
    }

    @Test
    fun `corrupt file is replaced by a fresh canonical id`() {
        listOf("", "garbage", "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA", "00000000-0000-1000-8000-000000000000").forEach { bad ->
            file.writeText(bad)
            val id = FileInstallationId(dir).get()
            assertThat(FileInstallationId.isCanonical(id)).isTrue()
            assertThat(id).isNotEqualTo(bad)
            assertThat(file.readText()).isEqualTo(id)
        }
    }

    @Test
    fun `failed write keeps a random id for the process, never a device identifier`() {
        val blocked = File(dir, "not-a-dir").apply { writeText("x") }
        val provider = FileInstallationId(blocked) { "11111111-1111-4111-8111-111111111111" }

        assertThat(provider.get()).isEqualTo("11111111-1111-4111-8111-111111111111")
        assertThat(provider.get()).isEqualTo("11111111-1111-4111-8111-111111111111")
    }

    @Test
    fun `separate app-private dirs (dev vs prod) never share an id`() {
        val dev = FileInstallationId(Files.createTempDirectory("dev").toFile()).get()
        val prod = FileInstallationId(Files.createTempDirectory("prod").toFile()).get()

        assertThat(dev).isNotEqualTo(prod)
    }

    @Test
    fun `lives in noBackupFilesDir, outside the wiped Room rows and filesDir, with backup off`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val id = FileInstallationId(context).get()

        assertThat(File(context.noBackupFilesDir, "installation_id").readText()).isEqualTo(id)
        assertThat(File(context.filesDir, "installation_id").exists()).isFalse()
        // Daily wipe deletes Room rows only (DayRepository.wipeToday); a new provider still reads the id.
        assertThat(FileInstallationId(context).get()).isEqualTo(id)
    }

    @Test
    fun `manifest keeps allowBackup false`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertThat(manifest).contains("android:allowBackup=\"false\"")
    }
}
