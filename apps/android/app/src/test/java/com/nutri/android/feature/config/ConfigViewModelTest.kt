package com.nutri.android.feature.config

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.DietaBotDatabase
import com.nutri.android.feature.home.HomePanelUiState
import com.nutri.android.feature.home.HomePanelViewModel
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ConfigViewModelTest {
    private lateinit var db: DietaBotDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private lateinit var vm: ConfigViewModel
    private val clock = MutableClock(Instant.parse("2026-09-25T18:00:00-03:00"))

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, DietaBotDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "cfg_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "partial", 50, true, "2026-09-25")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450),
                MealSlot(name = "Almoço", minutesFromMidnight = 750),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
        val cafe = repo.observeToday().first().slots.first().id
        repo.addLog("", "2 ovos", 380, 22, true, slotId = cafe)
        repo.insertMessage("user", "2 ovos")
        vm = ConfigViewModel(repo, clock)
        awaitUi { it.loaded }
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun rows_showStoredProfile() {
        val ui = vm.uiState.value
        assertThat(ui.ceilingValue).isEqualTo("2000 kcal")
        assertThat(ui.ceilingDetail).isEqualTo("Mesmo valor todos os dias")
        assertThat(ui.eatBackValue).isEqualTo("50%")
        assertThat(ui.macrosValue).isEqualTo("150g · 200g · 67g")
        assertThat(ui.slots.map { it.name to it.time })
            .containsExactly("Café da manhã" to "07:30", "Almoço" to "12:30", "Jantar" to "20:00").inOrder()
        assertThat(ui.workoutValue).isEqualTo(ConfigMapper.NO_WORKOUT)
        assertThat(ui.creditKcal).isEqualTo(0)
    }

    @Test
    fun ceilingChange_asksFirst_confirmWipesTodayKeepsChatAndProfile() = runBlocking<Unit> {
        vm.open(ConfigEditor.CEILING)
        vm.setSame("1800")
        vm.save()
        assertThat(vm.uiState.value.wipeConfirm).isTrue()
        assertThat(vm.uiState.value.editor).isNull()
        // Nothing stored until confirmed.
        assertThat(repo.observeToday().first().kcalSame).isEqualTo(2000)
        assertThat(repo.observeToday().first().logs).hasSize(1)

        vm.confirmWipe()
        awaitUi { it.ceilingValue == "1800 kcal" && !it.wipeConfirm }

        val day = repo.observeToday().first()
        assertThat(day.kcalSame).isEqualTo(1800)
        assertThat(day.logs).isEmpty()
        assertThat(day.slots).hasSize(3)
        assertThat(day.eat).isEqualTo("partial")
        assertThat(repo.observeMessages().first().filter { it.role == "user" }.map { it.text }).containsExactly("2 ovos")
    }

    @Test
    fun ceilingChange_cancelDoesNotSaveCeiling() = runBlocking<Unit> {
        vm.open(ConfigEditor.CEILING)
        vm.setCeilingMode("weekdayWeekend")
        vm.setWeekday("1700")
        vm.save()
        assertThat(vm.uiState.value.wipeConfirm).isTrue()

        vm.cancelWipe()

        assertThat(vm.uiState.value.wipeConfirm).isFalse()
        val day = repo.observeToday().first()
        assertThat(day.ceilingMode).isEqualTo("same")
        assertThat(day.kcalWeekday).isEqualTo(2000)
        assertThat(day.logs).hasSize(1)
        assertThat(repo.observeMessages().first().map { it.role }).doesNotContain(DayRepository.ROLE_WIPED)
    }

    @Test
    fun sameCeiling_savesWithoutDialog() = runBlocking<Unit> {
        vm.open(ConfigEditor.CEILING)
        vm.save()
        assertThat(vm.uiState.value.wipeConfirm).isFalse()
        assertThat(vm.uiState.value.editor).isNull()
        assertThat(repo.observeToday().first().logs).hasSize(1)
    }

    @Test
    fun slotTimeAndName_relabelWithoutWipe() = runBlocking<Unit> {
        val before = repo.observeToday().first().slots.map { it.id }
        vm.open(ConfigEditor.SLOTS)
        vm.setSlotTime(0, 8 * 60)
        vm.setSlotName(0, "Desjejum")
        vm.save()
        assertThat(vm.uiState.value.wipeConfirm).isFalse()
        awaitUi { it.slots.first().time == "08:00" }

        val day = repo.observeToday().first()
        assertThat(day.slots.map { it.id }).isEqualTo(before)
        assertThat(day.slots.first().name).isEqualTo("Desjejum")
        assertThat(day.logs.single().slotId).isEqualTo(before.first())
        assertThat(repo.observeMessages().first().map { it.role }).doesNotContain(DayRepository.ROLE_WIPED)
    }

    @Test
    fun slotsNeedANameEach() {
        vm.open(ConfigEditor.SLOTS)
        vm.setSlotCount(4)
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.setSlotName(3, "Ceia")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun workout_emptyIsNullAndCreditZero_numberGivesCredit() = runBlocking<Unit> {
        vm.open(ConfigEditor.WORKOUT)
        vm.setWorkout("400")
        vm.save()
        awaitUi { it.workoutValue == "400 kcal" }
        assertThat(vm.uiState.value.creditKcal).isEqualTo(200)
        assertThat(repo.observeToday().first().workoutKcal).isEqualTo(400)

        vm.open(ConfigEditor.WORKOUT)
        assertThat(vm.uiState.value.draft.workoutField).isEqualTo("400")
        vm.setWorkout("")
        vm.save()
        awaitUi { it.workoutValue == ConfigMapper.NO_WORKOUT }
        assertThat(vm.uiState.value.creditKcal).isEqualTo(0)
        assertThat(repo.observeToday().first().workoutKcal).isNull()
    }

    @Test
    fun workout_rolloverSaoPauloStartsEmpty() = runBlocking<Unit> {
        repo.setWorkout(500)
        clock.instant = Instant.parse("2026-09-26T00:05:00-03:00")
        val next = repo.observeToday().first()
        assertThat(next.workoutKcal).isNull()
    }

    @Test
    fun eatBackAndMacros_saveWithoutWipe() = runBlocking<Unit> {
        vm.open(ConfigEditor.EAT_BACK)
        vm.setEat("full")
        vm.save()
        vm.open(ConfigEditor.MACROS)
        vm.setProtein("160")
        vm.save()
        awaitUi { it.eatBackValue == "100%" && it.macrosValue.startsWith("160g") }
        assertThat(repo.observeToday().first().logs).hasSize(1)
    }

    @Test
    fun workout_savedFromHome_raisesHomeMetaAndShowsInConfig() = runBlocking<Unit> {
        val home = HomePanelViewModel(repo, clock)
        val collector = launch(Dispatchers.Main) { home.uiState.collect {} }
        awaitHome(home) { it.meta == 2000 && it.workoutKcal == null }

        home.openWorkout()
        awaitHome(home) { it.workoutEditor?.input == "" }
        home.setWorkout("350")
        awaitHome(home) { it.workoutEditor?.creditLine == "+175 kcal na meta de hoje (compensação 50%)" }
        home.saveWorkout()

        awaitHome(home) { it.meta == 2175 && it.workoutKcal == 350 && it.workoutCredit == 175 && it.workoutEditor == null }
        awaitUi { it.workoutValue == "350 kcal" && it.creditKcal == 175 }
        vm.open(ConfigEditor.WORKOUT)
        assertThat(vm.uiState.value.draft.workoutField).isEqualTo("350")
        assertThat(vm.uiState.value.draft.workoutEditor.credit).isEqualTo(175)
        vm.close()

        // Empty + Salvar = no workout = credit 0.
        home.openWorkout()
        awaitHome(home) { it.workoutEditor?.input == "350" }
        home.setWorkout("")
        home.saveWorkout()
        awaitHome(home) { it.meta == 2000 && it.workoutKcal == null && it.workoutCredit == 0 }
        awaitUi { it.workoutValue == ConfigMapper.NO_WORKOUT && it.creditKcal == 0 }
        collector.cancel()
    }

    @Test
    fun workout_cancelFromHome_storesNothing() = runBlocking<Unit> {
        val home = HomePanelViewModel(repo, clock)
        val collector = launch(Dispatchers.Main) { home.uiState.collect {} }
        awaitHome(home) { it.meta == 2000 }
        home.openWorkout()
        home.setWorkout("500")
        awaitHome(home) { it.workoutEditor?.input == "500" }
        home.closeWorkout()
        awaitHome(home) { it.workoutEditor == null }
        assertThat(repo.observeToday().first().workoutKcal).isNull()
        collector.cancel()
    }

    @Test fun modeChangeIsStagedUntilLastGroupAndKeepsTodayHistory() = runBlocking<Unit> {
        vm.open(ConfigEditor.SLOTS)
        vm.setSlotMode("split")
        assertThat(vm.uiState.value.draft.slotSchedule.pendingMode).isEqualTo("split")
        vm.cancelSlotMode()
        assertThat(vm.uiState.value.draft.slotSchedule.mode).isEqualTo("same")
        vm.setSlotMode("split")
        vm.confirmSlotMode()
        vm.setSlotCount(2)
        vm.setSlotName(0, "Café")
        vm.setSlotName(1, "Jantar")
        vm.save()
        assertThat(vm.uiState.value.draft.slotSchedule.index).isEqualTo(1)
        assertThat(repo.observeToday().first().slotMode).isEqualTo("same")
        vm.copyPreviousSlots()
        vm.setSlotTime(0, 570)
        vm.save()
        awaitUi { it.slotMode == "split" && it.editor == null }
        val stored = repo.observeToday().first()
        assertThat(stored.slots.map { it.days }).containsExactly(31, 31, 96, 96)
        assertThat(stored.logs).hasSize(1)
        assertThat(repo.observeMessages().first()).hasSize(1)
        assertThat(stored.slots.single { it.days == 96 && it.name == "Café" }.minutesFromMidnight).isEqualTo(570)
    }

    private suspend fun awaitHome(home: HomePanelViewModel, predicate: (HomePanelUiState) -> Boolean) {
        withTimeout(5_000) { home.uiState.first(predicate) }
    }

    private suspend fun awaitUi(predicate: (ConfigUiState) -> Boolean) {
        withTimeout(5_000) { vm.uiState.first(predicate) }
    }

    private class MutableClock(var instant: Instant) : InstantClock {
        override fun now(): Instant = instant
    }
}
