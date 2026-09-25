package com.nutri.android.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class DayRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = MutableClock(DAY_D)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NutriDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "nutri_day_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(
            scope = storeScope,
            produceFile = { file },
        )
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
    }

    @Test
    fun addLogOnD_observeTodayContainsIt() = runBlocking {
        val repo = repository()
        repo.addLog("lunch", "rice", 500, 20, true)
        val snap = repo.observeToday().first()
        assertThat(snap.date).isEqualTo("2026-03-15")
        assertThat(snap.logs).hasSize(1)
        assertThat(snap.logs[0].window).isEqualTo("lunch")
        assertThat(snap.logs[0].text).isEqualTo("rice")
        assertThat(snap.logs[0].kcal).isEqualTo(500)
        assertThat(snap.logs[0].p).isEqualTo(20)
        assertThat(snap.logs[0].stable).isTrue()
    }

    @Test
    fun addLogOnD_observeDPlus1_logsEmptyProfileRemains() = runBlocking {
        val repo = repository()
        repo.saveProfile(
            ceilingMode = "same",
            kcalSame = 1850,
            kcalWeekday = 1900,
            kcalWeekend = 2200,
            kcalDays = List(7) { 1850 },
            eat = "partial",
            pct = 40,
            onboardingDone = true,
            firstDay = "2026-03-15",
        )
        repo.addLog("dinner", "beans", 640, 28, true)
        clock.instant = DAY_D_PLUS_1
        val snap = repo.observeToday().first()
        assertThat(snap.date).isEqualTo("2026-03-16")
        assertThat(snap.logs).isEmpty()
        assertThat(snap.kcalSame).isEqualTo(1850)
        assertThat(snap.eat).isEqualTo("partial")
        assertThat(snap.pct).isEqualTo(40)
        assertThat(snap.onboardingDone).isTrue()
        assertThat(snap.firstDay).isEqualTo("2026-03-15")
    }

    @Test
    fun saveProfile_survivesRepositoryRecreate() = runBlocking {
        val repo = repository()
        repo.saveProfile(
            ceilingMode = "weekdayWeekend",
            kcalSame = 2000,
            kcalWeekday = 2100,
            kcalWeekend = 2400,
            kcalDays = listOf(1, 2, 3, 4, 5, 6, 7),
            eat = "full",
            pct = 100,
            onboardingDone = true,
            firstDay = "2026-03-15",
        )
        val again = DayRepository(db, clock, store)
        val snap = again.observeToday().first()
        assertThat(snap.ceilingMode).isEqualTo("weekdayWeekend")
        assertThat(snap.kcalWeekday).isEqualTo(2100)
        assertThat(snap.kcalWeekend).isEqualTo(2400)
        assertThat(snap.kcalDays).isEqualTo(listOf(1, 2, 3, 4, 5, 6, 7))
        assertThat(snap.eat).isEqualTo("full")
        assertThat(snap.pct).isEqualTo(100)
        assertThat(snap.onboardingDone).isTrue()
        assertThat(snap.firstDay).isEqualTo("2026-03-15")
    }

    @Test
    fun dataStoreJsonImport_keepsKcalSumAndClearsKey() = runBlocking {
        val saved = SavedDay(
            ceilingMode = "same",
            kcalSame = 2100,
            firstDay = "2026-03-15",
            logs = listOf(
                SavedLog(window = "lunch", text = "a", kcal = 400, p = 20, stable = true),
                SavedLog(window = "dinner", text = "b", kcal = 250, p = 30, stable = true),
            ),
        )
        val key = stringPreferencesKey(DAY_PREF_KEY)
        store.edit { it[key] = Json.encodeToString(saved) }
        val repo = repository()
        val snap = repo.observeToday().first()
        assertThat(snap.logs.sumOf { it.kcal }).isEqualTo(650)
        assertThat(db.mealLogDao().getByDate("2026-03-15").sumOf { it.kcal }).isEqualTo(650)
        assertThat(snap.kcalSame).isEqualTo(2100)
        val prefs = store.data.first()
        assertThat(prefs.contains(key)).isFalse()
    }

    private fun repository() = DayRepository(db, clock, store)

    private class MutableClock(var instant: Instant) : InstantClock {
        override fun now(): Instant = instant
    }

    companion object {
        private val DAY_D: Instant = Instant.parse("2026-03-15T15:00:00-03:00")
        private val DAY_D_PLUS_1: Instant = Instant.parse("2026-03-16T15:00:00-03:00")
    }
}
