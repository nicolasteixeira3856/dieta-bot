package com.nutri.android.feature.devtools

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DietaBotDatabase
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.memory.FakeMemoryFile
import com.nutri.android.core.memory.MemoryStore
import com.nutri.android.core.telemetry.FakeTelemetry
import com.nutri.android.feature.chat.PromptBuilder
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
class DevMemoryViewModelTest {
    private lateinit var db: DietaBotDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private lateinit var memoryFile: FakeMemoryFile
    private lateinit var memory: MemoryStore
    private lateinit var vm: DevMemoryViewModel
    private val telemetry = FakeTelemetry()

    // Friday 25/09/2026, 18:00 in São Paulo.
    private val clock = object : InstantClock {
        override fun now(): Instant = Instant.parse("2026-09-25T18:00:00-03:00")
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, DietaBotDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "dev_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "partial", 50, true, "2026-09-25")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
        repo.setWorkout(400)
        val cafe = repo.observeToday().first().slots.first().id
        repo.addLog("", "2 ovos", 380, 22, true, slotId = cafe)
        memoryFile = FakeMemoryFile("Café: 2 ovos (380 kcal)\nNão come glúten")
        memory = MemoryStore(memoryFile)
        vm = DevMemoryViewModel(repo, memory, clock, telemetry)
        awaitUi { it.loaded }
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun shows_theProfileAndMemoryOfTheNextPost() = runBlocking<Unit> {
        val body = PromptBuilder.build(repo.observeToday().first(), emptyList(), emptyList(), "x", clock.now(), memory = memory.read()).body
        val ui = vm.uiState.value
        assertThat(ui.memoryText).isEqualTo(body.memory)
        assertThat(ui.profileText).isEqualTo(ProfileText.format(body.profile))
        // 2000 base + 50% of 400.
        assertThat(ui.profileText).startsWith("teto_kcal=2200\n")
        assertThat(ui.dayText).contains("\"eaten_kcal\": 380")
    }

    @Test
    fun emptyMemory_isRefused_nothingSaved() = runBlocking<Unit> {
        vm.setMemory("  \n ")
        vm.save()
        assertThat(vm.uiState.value.error).isEqualTo("Memória vazia não é salva.")
        assertThat(vm.uiState.value.saved).isFalse()
        assertThat(memoryFile.writes).isEqualTo(0)
    }

    @Test
    fun removedKey_isRefused_nothingSaved() = runBlocking<Unit> {
        vm.setMemory("nova memória")
        vm.setProfile(vm.uiState.value.profileText.replace("gordura_g=67\n", "").replace("proteina_g=150", "proteina_g=170"))
        vm.save()
        assertThat(vm.uiState.value.error).isEqualTo("Falta gordura_g: remover não é permitido.")
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(repo.observeToday().first().proteinTargetG).isEqualTo(150)
    }

    @Test
    fun editWithoutCeiling_savesMemoryAndProfile_noWipe() = runBlocking<Unit> {
        vm.setMemory("Café: 2 ovos (380 kcal)\nNão come glúten nem lactose")
        vm.setProfile(
            vm.uiState.value.profileText
                .replace("proteina_g=150", "proteina_g=170")
                .replace("compensacao=partial 50%", "compensacao=full")
                .replace("Jantar 20:00", "Janta 20:30"),
        )
        vm.save()
        awaitUi { it.saved }

        assertThat(memory.read()).isEqualTo("Café: 2 ovos (380 kcal)\nNão come glúten nem lactose")
        val day = repo.observeToday().first()
        assertThat(day.proteinTargetG).isEqualTo(170)
        assertThat(day.eat).isEqualTo("full")
        assertThat(day.pct).isEqualTo(50)
        assertThat(day.slots.map { it.name to it.minutesFromMidnight }).containsExactly("Café da manhã" to 450, "Janta" to 1230).inOrder()
        assertThat(day.kcalSame).isEqualTo(2000)
        assertThat(day.logs).hasSize(1)
        val params = telemetry.params(DevMemoryViewModel.DEV_MEMORY_SAVED).single()
        assertThat(params).containsExactly("memory_len_bucket", "<1000", "profile_changed", true)
    }

    @Test
    fun changedCeiling_asksWipe_cancelStoresNothing() = runBlocking<Unit> {
        vm.setMemory("outra memória")
        vm.setProfile(vm.uiState.value.profileText.replace("teto_kcal=2200", "teto_kcal=2000"))
        vm.save()
        assertThat(vm.uiState.value.wipeConfirm).isTrue()

        vm.cancelWipe()
        assertThat(vm.uiState.value.wipeConfirm).isFalse()
        assertThat(vm.uiState.value.saved).isFalse()
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(repo.observeToday().first().kcalSame).isEqualTo(2000)
        assertThat(repo.observeToday().first().logs).hasSize(1)
    }

    @Test
    fun changedCeiling_confirmStoresBaseAndWipesToday() = runBlocking<Unit> {
        vm.setProfile(vm.uiState.value.profileText.replace("teto_kcal=2200", "teto_kcal=2000"))
        vm.save()
        vm.confirmWipe()
        awaitUi { it.saved }

        val day = repo.observeToday().first()
        // Effective 2000 - credit 200 = base 1800.
        assertThat(day.kcalSame).isEqualTo(1800)
        assertThat(day.logs).isEmpty()
        assertThat(telemetry.params(DevMemoryViewModel.DEV_MEMORY_SAVED).single()["profile_changed"]).isEqualTo(true)
    }

    @Test
    fun weekdayWeekend_changesOnlyTodaysCeiling() = runBlocking<Unit> {
        repo.changeCeiling("weekdayWeekend", 2000, 2100, 2500, List(7) { 2000 })
        vm = DevMemoryViewModel(repo, memory, clock, telemetry)
        awaitUi { it.loaded && it.profileText.startsWith("teto_kcal=2300") }
        vm.setProfile(vm.uiState.value.profileText.replace("teto_kcal=2300", "teto_kcal=2400"))
        vm.save()
        vm.confirmWipe()
        awaitUi { it.saved }

        val day = repo.observeToday().first()
        assertThat(day.kcalWeekday).isEqualTo(2200)
        assertThat(day.kcalWeekend).isEqualTo(2500)
    }

    private suspend fun awaitUi(predicate: (DevMemoryUiState) -> Boolean) {
        withTimeout(5_000) { vm.uiState.first(predicate) }
    }
}
