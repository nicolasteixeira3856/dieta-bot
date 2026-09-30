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
import com.nutri.android.core.memory.FactMemory
import com.nutri.android.core.telemetry.FakeTelemetry
import com.nutri.android.domain.MemoryUpdate
import com.nutri.android.domain.RecordedMeal
import com.nutri.android.feature.chat.PromptBuilder
import java.io.File
import java.time.Instant
import java.time.LocalDate
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
    private lateinit var memory: FactMemory
    private val today = LocalDate.parse("2026-09-25")
    private var cafe = 0L
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
        cafe = repo.observeToday().first().slots.first().id
        repo.addLog("", "2 ovos", 380, 22, true, slotId = cafe)
        memoryFile = FakeMemoryFile()
        memory = FactMemory(memoryFile)
        memory.apply(listOf(MemoryUpdate("add", null, "permanent", "preference", "gluten", "Não come glúten")), today.minusDays(2))
        memory.apply(
            listOf(MemoryUpdate("add", null, "dynamic", "routine", "cafe", "2 ovos mexidos", cafe.toString())),
            today,
            RecordedMeal(cafe.toString(), 380, 22, 4, 16),
        )
        memoryFile.writes = 0
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
        val facts = memory.read(today).facts
        val body = PromptBuilder.build(repo.observeToday().first(), emptyList(), emptyList(), "x", clock.now(), facts = facts).body
        val ui = vm.uiState.value
        assertThat(ui.memoryText).isEqualTo(
            "P1 | preference | gluten | Não come glúten\n" +
                "D1 | routine | cafe | slot=$cafe | 2 ovos mexidos | 380 kcal 22P 4C 16G",
        )
        assertThat(body.facts!!.map { it.id }).containsExactly("P1", "D1").inOrder()
        assertThat(ui.memorySummary).isEqualTo("Permanente 1/30 · Dinâmica 1/40")
        assertThat(ui.memorySeen).isEqualTo("P1 · visto 1 dia · último 23/09\nD1 · visto 1 dia · último 25/09")
        assertThat(ui.profileText).isEqualTo(ProfileText.format(body.profile))
        // 2000 base + 50% of 400.
        assertThat(ui.profileText).startsWith("teto_kcal=2200\n")
        assertThat(ui.dayText).contains("\"eaten_kcal\": 380")
    }

    @Test
    fun removedFact_isRefusedWithItsLine_nothingSaved() = runBlocking<Unit> {
        vm.setMemory(vm.uiState.value.memoryText.lines().first())
        vm.save()
        assertThat(vm.uiState.value.error).isEqualTo("Linha 2: remover não é permitido (D1). Esquecer é pelo Chat.")
        assertThat(vm.uiState.value.saved).isFalse()
        assertThat(memoryFile.writes).isEqualTo(0)

        vm.setMemory("")
        vm.save()
        assertThat(vm.uiState.value.error).isEqualTo("Linha 1: remover não é permitido (P1). Esquecer é pelo Chat.")
        assertThat(memoryFile.writes).isEqualTo(0)
    }

    @Test
    fun oldTextMemory_showsEmpty_profileStillSaves() = runBlocking<Unit> {
        memoryFile = FakeMemoryFile("Café: 2 ovos (380 kcal)\nNão come glúten")
        memory = FactMemory(memoryFile)
        vm = DevMemoryViewModel(repo, memory, clock, telemetry)
        awaitUi { it.loaded }
        assertThat(vm.uiState.value.memoryText).isEmpty()
        assertThat(vm.uiState.value.memorySummary).isEqualTo("Permanente 0/30 · Dinâmica 0/40")

        vm.setProfile(vm.uiState.value.profileText.replace("proteina_g=150", "proteina_g=160"))
        vm.save()
        awaitUi { it.saved }
        assertThat(repo.observeToday().first().proteinTargetG).isEqualTo(160)
        assertThat(memoryFile.writes).isEqualTo(0)
    }

    @Test
    fun removedKey_isRefused_nothingSaved() = runBlocking<Unit> {
        vm.setMemory(vm.uiState.value.memoryText.replace("Não come glúten", "Não come glúten nem lactose"))
        vm.setProfile(vm.uiState.value.profileText.replace("gordura_g=67\n", "").replace("proteina_g=150", "proteina_g=170"))
        vm.save()
        assertThat(vm.uiState.value.error).isEqualTo("Falta gordura_g: remover não é permitido.")
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(repo.observeToday().first().proteinTargetG).isEqualTo(150)
    }

    @Test
    fun editWithoutCeiling_savesMemoryAndProfile_noWipe() = runBlocking<Unit> {
        vm.setMemory(
            vm.uiState.value.memoryText
                .replace("preference | gluten | Não come glúten", "preference | gluten | Não come glúten nem lactose")
                .replace("| 380 kcal 22P 4C 16G", "| 400 kcal 22P 8C 16G") + "\nnovo | preference | queijo | Queijo minas",
        )
        vm.setProfile(
            vm.uiState.value.profileText
                .replace("proteina_g=150", "proteina_g=170")
                .replace("compensacao=partial 50%", "compensacao=full")
                .replace("Jantar 20:00", "Janta 20:30"),
        )
        vm.save()
        awaitUi { it.saved }

        val facts = memory.read(today).facts
        assertThat(facts.map { it.id to it.text }).containsExactly(
            "P1" to "Não come glúten nem lactose",
            "D1" to "2 ovos mexidos",
            "P2" to "Queijo minas",
        ).inOrder()
        assertThat(facts[1].kcal).isEqualTo(400)
        assertThat(facts[1].days).containsExactly("2026-09-25")
        assertThat(facts[2].source).isEqualTo("explicit")
        assertThat(facts[2].days).containsExactly("2026-09-25")
        val day = repo.observeToday().first()
        assertThat(day.proteinTargetG).isEqualTo(170)
        assertThat(day.eat).isEqualTo("full")
        assertThat(day.pct).isEqualTo(50)
        assertThat(day.slots.map { it.name to it.minutesFromMidnight }).containsExactly("Café da manhã" to 450, "Janta" to 1230).inOrder()
        assertThat(day.kcalSame).isEqualTo(2000)
        assertThat(day.logs).hasSize(1)
        val params = telemetry.params(DevMemoryViewModel.DEV_MEMORY_SAVED).single()
        assertThat(params).containsExactly("permanent", 2, "dynamic", 1, "memory_changed", true, "profile_changed", true)
    }

    @Test
    fun changedCeiling_asksWipe_cancelStoresNothing() = runBlocking<Unit> {
        vm.setMemory(vm.uiState.value.memoryText + "\nnovo | portion | arroz | 4 colheres")
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

    @Test
    fun slotsByWeekday_editTodays_keepTheRestOfTheWeek() = runBlocking<Unit> {
        // A24: Mon-Fri (31) and Sat-Sun (96) groups. Today is a Friday.
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450, days = 31),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200, days = 31),
                MealSlot(name = "Brunch", minutesFromMidnight = 630, days = 96),
            ),
            slotMode = "split",
        )
        vm = DevMemoryViewModel(repo, memory, clock, telemetry)
        awaitUi { it.loaded && it.profileText.contains("refeicao.2=Jantar 20:00") }
        assertThat(vm.uiState.value.profileText).doesNotContain("Brunch")

        vm.setProfile(vm.uiState.value.profileText.replace("Jantar 20:00", "Janta 20:30"))
        vm.save()
        awaitUi { it.saved }

        val day = repo.observeToday().first()
        assertThat(day.slotMode).isEqualTo("split")
        assertThat(day.slots.map { Triple(it.name, it.minutesFromMidnight, it.days) }).containsExactly(
            Triple("Café da manhã", 450, 31),
            Triple("Brunch", 630, 96),
            Triple("Janta", 1230, 31),
        ).inOrder()
    }

    private suspend fun awaitUi(predicate: (DevMemoryUiState) -> Boolean) {
        withTimeout(5_000) { vm.uiState.first(predicate) }
    }
}
