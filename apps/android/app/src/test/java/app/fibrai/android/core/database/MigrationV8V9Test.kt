package app.fibrai.android.core.database

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.feature.chat.PromptBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A v8 file (A34) with a digest: coversUntilId arrives null, so the old digest keeps its time cut (A38). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV8V9Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun digestKeepsTextAndTime_coversUntilIdNull_timeCutKept() {
        val name = "migration_v8_v9.db"
        helper.createDatabase(name, 8).apply {
            execSQL("INSERT INTO day_digest (date,seq,text,createdAtEpochMs) VALUES ('2026-10-03',1,'resumo',1003)")
            close()
        }
        helper.runMigrationsAndValidate(name, 9, true, MIGRATION_8_9).apply {
            query("SELECT date,seq,text,createdAtEpochMs,coversUntilId FROM day_digest").use {
                assertThat(it.count).isEqualTo(1)
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("2026-10-03")
                assertThat(it.getInt(1)).isEqualTo(1)
                assertThat(it.getString(2)).isEqualTo("resumo")
                assertThat(it.getLong(3)).isEqualTo(1003)
                assertThat(it.isNull(4)).isTrue()
            }
            close()
        }
        val digest = DayDigestEntity("2026-10-03", 1, "resumo", createdAtEpochMs = 1003)
        val messages = (1..6L).map { ChatMessageEntity(id = it, date = "2026-10-03", role = if (it % 2 == 1L) "user" else "assistant", createdAtEpochMs = 1000 + it) }
        assertThat(PromptBuilder.rawSinceDigest(messages, listOf(digest)).map { it.id }).containsExactly(4L, 5L, 6L).inOrder()
    }
}
