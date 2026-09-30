package com.nutri.android.core.database

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class RoomV2Test {
    private lateinit var context: Context
    private lateinit var db: DietaBotDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = MutableClock(DAY_D)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DietaBotDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "nutri_day_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
    }

    @Test
    fun databaseIsVersion4() {
        assertThat(db.openHelper.readableDatabase.version).isEqualTo(4)
    }

    @Test
    fun addLog_twiceOnSameSlot_sumsTwoRows() = runBlocking<Unit> {
        val repo = repository()
        repo.saveSlots(listOf(MealSlot(name = "Almoco", minutesFromMidnight = 12 * 60)))
        val lunch = repo.observeToday().first().slots.single().id
        repo.addLog("lunch", "arroz", 400, 20, true, slotId = lunch, carbs = 60, fat = 8)
        repo.addLog("lunch", "feijao", 200, 12, true, slotId = lunch, carbs = 30, fat = 2)
        val snap = repo.observeToday().first()
        val rows = snap.logs.filter { it.slotId == lunch }
        assertThat(rows).hasSize(2)
        assertThat(rows.sumOf { it.kcal }).isEqualTo(600)
        assertThat(rows.sumOf { it.carbs }).isEqualTo(90)
        assertThat(rows.sumOf { it.fat }).isEqualTo(10)
        assertThat(rows.map { it.source }.toSet()).containsExactly("user")
    }

    @Test
    fun addSkip_removesLogs_andAddLog_removesSkip() = runBlocking<Unit> {
        val repo = repository()
        repo.saveSlots(
            listOf(
                MealSlot(name = "Cafe", minutesFromMidnight = 8 * 60),
                MealSlot(name = "Janta", minutesFromMidnight = 20 * 60),
            ),
        )
        val (cafe, janta) = repo.observeToday().first().slots.map { it.id }
        repo.addLog("breakfast", "pao", 300, 10, true, slotId = cafe)
        repo.addLog("dinner", "sopa", 350, 15, true, slotId = janta)

        repo.addSkip(cafe)
        var snap = repo.observeToday().first()
        assertThat(snap.skippedSlotIds).containsExactly(cafe)
        assertThat(snap.logs.map { it.slotId }).containsExactly(janta)

        repo.addLog("breakfast", "ovo", 150, 12, true, slotId = cafe)
        snap = repo.observeToday().first()
        assertThat(snap.skippedSlotIds).isEmpty()
        assertThat(snap.logs.map { it.slotId }).containsExactly(janta, cafe)
    }

    @Test
    fun wipeToday_preservesChatProfileSlotsAndWorkout() = runBlocking<Unit> {
        val repo = repository()
        repo.saveProfile("same", 1900, 2000, 2300, List(7) { 1900 }, "partial", 50, true, "2026-03-15", sex = "male")
        repo.saveSlots(listOf(MealSlot(name = "Janta", minutesFromMidnight = 20 * 60)))
        val janta = repo.observeToday().first().slots.single().id
        repo.setWorkout(300)
        repo.addLog("dinner", "sopa", 350, 15, true, slotId = janta)
        repo.addSkip(janta)
        repo.addLog("dinner", "pao", 200, 8, true)
        repo.upsertDigest("resumo")
        repo.insertMessage("user", "2 ovos")

        repo.wipeToday()

        val snap = repo.observeToday().first()
        assertThat(snap.logs).isEmpty()
        assertThat(snap.skippedSlotIds).isEmpty()
        assertThat(repo.digestsToday()).isEmpty()
        val messages = repo.observeMessages().first()
        assertThat(messages.filter { it.role == "user" }.map { it.text }).containsExactly("2 ovos")
        assertThat(messages.map { it.role }).containsExactly("user", DayRepository.ROLE_WIPED).inOrder()
        assertThat(snap.kcalSame).isEqualTo(1900)
        assertThat(snap.sex).isEqualTo("male")
        assertThat(snap.slots.map { it.name }).containsExactly("Janta")
        assertThat(snap.workoutKcal).isEqualTo(300)
    }

    @Test
    fun saveSlots_replacesAll_keepsIdsOfKeptSlots_orphansDeletedOnes() = runBlocking<Unit> {
        val repo = repository()
        repo.saveSlots(
            listOf(
                MealSlot(name = "Cafe", minutesFromMidnight = 8 * 60),
                MealSlot(name = "Lanche", minutesFromMidnight = 16 * 60),
            ),
        )
        val (cafe, lanche) = repo.observeToday().first().slots
        repo.addLog("breakfast", "pao", 300, 10, true, slotId = cafe.id)
        repo.addLog("afternoonSnack", "fruta", 90, 1, true, slotId = lanche.id)

        repo.saveSlots(
            listOf(
                MealSlot(name = "Janta", minutesFromMidnight = 20 * 60),
                cafe.copy(name = "Cafe da manha"),
            ),
        )

        val snap = repo.observeToday().first()
        assertThat(snap.slots.map { it.name }).containsExactly("Janta", "Cafe da manha").inOrder()
        assertThat(snap.slots.last().id).isEqualTo(cafe.id)
        assertThat(snap.logs.first { it.text == "pao" }.slotId).isEqualTo(cafe.id)
        assertThat(snap.logs.first { it.text == "fruta" }.slotId).isNull()
    }

    @Test
    fun saveProfile_v1Signature_keepsBodyAndMacros() = runBlocking<Unit> {
        val repo = repository()
        repo.saveProfile(
            "same", 2160, 2000, 2300, List(7) { 2160 }, "zero", 50, true, "2026-03-15",
            sex = "male", ageYears = 27, heightCm = 180, weightKg = 116.0,
            proteinTargetG = 162, carbTargetG = 216, fatTargetG = 72,
        )
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "full", 100, true, "2026-03-15")
        val snap = repo.observeToday().first()
        assertThat(snap.kcalSame).isEqualTo(2000)
        assertThat(snap.eat).isEqualTo("full")
        assertThat(snap.ageYears).isEqualTo(27)
        assertThat(snap.weightKg).isEqualTo(116.0)
        assertThat(snap.proteinTargetG).isEqualTo(162)
        assertThat(snap.fatTargetG).isEqualTo(72)
    }

    @Test
    fun upsertDigest_thirdOverwritesSeq1() = runBlocking<Unit> {
        val repo = repository()
        repo.upsertDigest("a")
        clock.instant = clock.instant.plusSeconds(60)
        repo.upsertDigest("b")
        clock.instant = clock.instant.plusSeconds(60)
        repo.upsertDigest("c")
        val digests = repo.digestsToday()
        assertThat(digests.map { it.seq to it.text }).containsExactly(1 to "c", 2 to "b").inOrder()
    }

    @Test
    fun changeCeiling_storesCeilingAndWipesOnlyToday() = runBlocking<Unit> {
        val repo = repository()
        repo.saveProfile("same", 1900, 2000, 2300, List(7) { 1900 }, "full", 50, true, "2026-03-14", proteinTargetG = 140)
        clock.instant = DAY_D.minusSeconds(86_400)
        repo.addLog("", "ontem", 700, 30, true)
        clock.instant = DAY_D
        repo.addLog("", "hoje", 500, 20, true)
        repo.setWorkout(250)
        repo.insertMessage("user", "2 ovos")

        repo.changeCeiling("weekdayWeekend", 1900, 1800, 2200, List(7) { 1900 })

        val snap = repo.observeToday().first()
        assertThat(snap.ceilingMode).isEqualTo("weekdayWeekend")
        assertThat(snap.kcalWeekday).isEqualTo(1800)
        assertThat(snap.kcalWeekend).isEqualTo(2200)
        assertThat(snap.logs).isEmpty()
        assertThat(snap.eat).isEqualTo("full")
        assertThat(snap.proteinTargetG).isEqualTo(140)
        assertThat(snap.onboardingDone).isTrue()
        assertThat(snap.workoutKcal).isEqualTo(250)
        assertThat(db.mealLogDao().getByDate("2026-03-14").map { it.text }).containsExactly("ontem")
        assertThat(repo.observeMessages().first().map { it.role }).containsExactly("user", DayRepository.ROLE_WIPED).inOrder()
    }

    @Test
    fun observeMessages_keeps60Days() = runBlocking<Unit> {
        val repo = repository()
        clock.instant = DAY_D.minusSeconds(60L * 86_400)
        repo.insertMessage("user", "velha")
        clock.instant = DAY_D.minusSeconds(59L * 86_400)
        repo.insertMessage("assistant", "limite", estimateKcal = 300, estimateConfidence = "high")
        clock.instant = DAY_D
        repo.insertMessage("user", "hoje", photoPath = "/tmp/x.jpg")
        val texts = repo.observeMessages().first().map { it.text }
        assertThat(texts).containsExactly("limite", "hoje").inOrder()
    }

    private fun repository() = DayRepository(db, clock, store)

    private class MutableClock(var instant: Instant) : InstantClock {
        override fun now(): Instant = instant
    }

    companion object {
        private val DAY_D: Instant = Instant.parse("2026-03-15T15:00:00-03:00")
    }
}
