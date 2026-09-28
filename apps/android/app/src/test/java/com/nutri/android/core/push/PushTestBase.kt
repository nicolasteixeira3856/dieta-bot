package com.nutri.android.core.push

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.NutriDatabase
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before

/** In-memory Room with the gold profile: 4 slots (07:30, 12:30, 16:00, 20:00), onboarding done. */
abstract class PushTestBase {
    protected val context: Application = ApplicationProvider.getApplicationContext()
    protected lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    protected lateinit var repo: DayRepository
    protected var now: Instant = Instant.parse("2026-09-25T06:00:00-03:00")
    protected val clock = InstantClock { now }
    protected lateinit var slotIds: List<Long>

    @Before
    fun setUpRoom() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(context, NutriDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "push_${System.nanoTime()}.preferences_pb")
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450),
                MealSlot(name = "Almoço", minutesFromMidnight = 750),
                MealSlot(name = "Lanche", minutesFromMidnight = 960),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
        slotIds = repo.observeToday().first().slots.map { it.id }
    }

    @After
    fun tearDownRoom() {
        db.close()
        storeScope.cancel()
    }
}
