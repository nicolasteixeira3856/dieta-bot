package com.nutri.android.core.database

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Builds real v1/v2 files from the exported schemas, then opens them with the current Room + migrations. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV1V2Test {
    private lateinit var context: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath("migration_${System.nanoTime()}.db")
        dbFile.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbFile.name)
    }

    @Test
    fun v1Data_survivesMigration_withV2Defaults() = runBlocking {
        createV1 { v1 ->
            v1.execSQL(
                "INSERT INTO profile (id, ceilingMode, kcalSame, kcalWeekday, kcalWeekend, kcalDays, eat, pct, " +
                    "onboardingDone, firstDay) VALUES (1, 'same', 1850, 2000, 2300, '[1850,1850,1850,1850,1850,1850,1850]', " +
                    "'partial', 40, 1, '2026-03-15')",
            )
            v1.execSQL("INSERT INTO day (date, workoutKcal, removedWindows, askedWindows) VALUES ('2026-03-15', 320, '[]', '[]')")
            v1.execSQL(
                "INSERT INTO meal_log (id, date, window, text, kcal, p, stable) VALUES " +
                    "(7, '2026-03-15', 'lunch', 'arroz', 500, 20, 1), (8, '2026-03-15', 'dinner', 'sopa', 300, 15, 1)",
            )
        }

        val db = Room.databaseBuilder(context, DietaBotDatabase::class.java, dbFile.absolutePath)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        try {
            assertThat(db.openHelper.readableDatabase.version).isEqualTo(10)

            val profile = db.profileDao().get()!!
            assertThat(profile.kcalSame).isEqualTo(1850)
            assertThat(profile.eat).isEqualTo("partial")
            assertThat(profile.kcalDays).isEqualTo(List(7) { 1850 })
            assertThat(profile.sex).isEqualTo("")
            assertThat(profile.ageYears).isEqualTo(0)
            assertThat(profile.weightKg).isEqualTo(0.0)
            assertThat(profile.proteinTargetG).isEqualTo(150)
            assertThat(profile.carbTargetG).isEqualTo(200)
            assertThat(profile.fatTargetG).isEqualTo(67)

            assertThat(db.dayDao().get("2026-03-15")!!.workoutKcal).isEqualTo(320)

            val logs = db.mealLogDao().getByDate("2026-03-15")
            assertThat(logs.map { it.id }).containsExactly(7L, 8L).inOrder()
            assertThat(logs.sumOf { it.kcal }).isEqualTo(800)
            assertThat(logs.all { it.slotId == null && it.carbs == 0 && it.fat == 0 && it.source == "user" }).isTrue()

            // New ids continue after the copied ones.
            db.mealLogDao().insert(MealLogEntity(date = "2026-03-15", window = "dinner", text = "pao", kcal = 100))
            assertThat(db.mealLogDao().getByDate("2026-03-15").last().id).isEqualTo(9L)

            // FK is live: deleting a slot orphans its logs.
            val slotId = db.mealSlotDao().upsert(MealSlotEntity(name = "Janta", minutesFromMidnight = 1200))
            db.mealLogDao().insert(MealLogEntity(date = "2026-03-15", window = "dinner", text = "x", kcal = 1, slotId = slotId))
            db.mealSlotDao().deleteAllExcept(emptyList())
            assertThat(db.mealLogDao().getByDate("2026-03-15").last().slotId).isNull()

            assertThat(db.chatMessageDao().getByDate("2026-03-15")).isEmpty()
            assertThat(db.dayDigestDao().getByDate("2026-03-15")).isEmpty()
            assertThat(db.slotSkipDao().getByDate("2026-03-15")).isEmpty()
        } finally {
            db.close()
        }
    }

    @Test
    fun emptyV1_migrates() {
        createV1 { }
        val db = Room.databaseBuilder(context, DietaBotDatabase::class.java, dbFile.absolutePath)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        try {
            assertThat(db.openHelper.readableDatabase.version).isEqualTo(10)
        } finally {
            db.close()
        }
    }

    @Test
    fun v2ChatMessages_gainEstimateColumns() = runBlocking {
        createFromSchema(SCHEMA_V2, version = 2) { v2 ->
            v2.execSQL(
                "INSERT INTO chat_message (id, date, role, text, createdAtEpochMs, estimateKcal) " +
                    "VALUES (1, '2026-09-25', 'assistant', 'ok', 1000, 380)",
            )
        }
        val db = Room.databaseBuilder(context, DietaBotDatabase::class.java, dbFile.absolutePath)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        try {
            val row = db.chatMessageDao().getByDate("2026-09-25").single()
            assertThat(row.estimateKcal).isEqualTo(380)
            assertThat(row.estimateSlotId).isNull()
            assertThat(row.itemNames).isEmpty()
            db.chatMessageDao().insert(
                ChatMessageEntity(date = "2026-09-25", role = "assistant", estimateSlotId = 4, estimateItems = "pao${ITEM_SEPARATOR}ovo"),
            )
            assertThat(db.chatMessageDao().getByDate("2026-09-25").first { it.id != 1L }.itemNames).containsExactly("pao", "ovo").inOrder()
        } finally {
            db.close()
        }
    }

    private fun createV1(seed: (SQLiteDatabase) -> Unit) = createFromSchema(SCHEMA_V1, version = 1, seed)

    private fun createFromSchema(path: String, version: Int, seed: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File(path).readText()).getJSONObject("database")
        val v1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                v1.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    v1.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) {
                v1.execSQL(setup.getString(i))
            }
            seed(v1)
            v1.version = version
        } finally {
            v1.close()
        }
    }

    private companion object {
        const val SCHEMA_V1 = "schemas/com.nutri.android.core.database.DietaBotDatabase/1.json"
        const val SCHEMA_V2 = "schemas/com.nutri.android.core.database.DietaBotDatabase/2.json"
    }
}
