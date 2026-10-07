package app.fibrai.android.feature.onboarding

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.FibraiDatabase
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class OnboardingViewModelTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = InstantClock { Instant.parse("2026-03-16T12:00:00-03:00") }
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "test_onboarding_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(
            scope = storeScope,
            produceFile = { file },
        )
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun o1_tmbPrefillsCeilingUntilEdited() = runBlocking<Unit> {
        val vm = loadedVm()
        assertThat(vm.uiState.value.suggestedCeiling).isNull()
        assertThat(vm.uiState.value.o1Valid).isFalse()

        vm.setSex("male")
        vm.setAge("27")
        vm.setHeight("180")
        vm.setWeight("116")
        assertThat(vm.uiState.value.suggestedCeiling).isEqualTo(2160)
        assertThat(vm.uiState.value.sameField).isEqualTo("2160")
        assertThat(vm.uiState.value.o1Valid).isTrue()

        vm.setSameField("2000")
        vm.setWeight("100")
        assertThat(vm.uiState.value.suggestedCeiling).isEqualTo(2000) // 10*100+1125-135+5 = 1995 -> 2000
        assertThat(vm.uiState.value.sameField).isEqualTo("2000")

        vm.setAge("")
        assertThat(vm.uiState.value.suggestedCeiling).isNull()
        vm.setSameField("0")
        assertThat(vm.uiState.value.o1Valid).isFalse()
    }

    @Test
    fun o1_requiresFullProfile_prefillsEveryCeilingField() = runBlocking<Unit> {
        val vm = loadedVm()
        assertThat(vm.uiState.value.sameField).isEmpty()
        assertThat(vm.uiState.value.dayFields.all { it.isEmpty() }).isTrue()

        vm.setSex("male")
        vm.setAge("27")
        assertThat(vm.uiState.value.profileValid).isFalse()
        assertThat(vm.uiState.value.o1Valid).isFalse()
        vm.setHeight("180")
        assertThat(vm.uiState.value.o1Valid).isFalse()
        vm.setWeight("116")
        assertThat(vm.uiState.value.profileValid).isTrue()
        assertThat(vm.uiState.value.o1Valid).isTrue()
        val s = vm.uiState.value
        assertThat(listOf(s.sameField, s.weekdayField, s.weekendField) + s.dayFields).containsExactlyElementsIn(List(10) { "2160" })
    }

    @Test
    fun o1_clearingBodyFieldAfterEditKeepsCeilingAndBlocks() = runBlocking<Unit> {
        val vm = loadedVm()
        vm.setSex("female")
        vm.setAge("30")
        vm.setHeight("165")
        vm.setWeight("60")
        vm.setSameField("1800")
        assertThat(vm.uiState.value.o1Valid).isTrue()

        vm.setWeight("")
        assertThat(vm.uiState.value.profileValid).isFalse()
        assertThat(vm.uiState.value.sameField).isEqualTo("1800")
        assertThat(vm.uiState.value.o1Valid).isFalse()
    }

    @Test
    fun o1_storedProfileIsValidOnLoad() = runBlocking<Unit> {
        val repo = DayRepository(db, clock, store)
        val vm = loadedVm(repo)
        vm.setSex("male")
        vm.setAge("27")
        vm.setHeight("180")
        vm.setWeight("116")
        vm.setSlotCount(2)
        vm.setSlotName(0, "Café")
        vm.setSlotName(1, "Janta")
        vm.enterMacros()
        vm.completeOnboarding {}
        repo.observeToday().first { it.onboardingDone }

        val again = loadedVm(DayRepository(db, clock, store))
        assertThat(again.uiState.value.profileValid).isTrue()
        assertThat(again.uiState.value.o1Valid).isTrue()
        assertThat(again.uiState.value.sameField).isEqualTo("2160")
    }

    @Test
    fun o3_countKeepsNamesAndRequiresAllNames()= runBlocking<Unit> {
        val vm = loadedVm()
        assertThat(vm.uiState.value.slots.map { it.minutes }).containsExactly(450, 750, 960, 1200).inOrder()
        assertThat(vm.uiState.value.slots.all { it.name.isEmpty() }).isTrue()
        assertThat(vm.uiState.value.o3Valid).isFalse()

        vm.setSlotName(0, "Café da manhã")
        vm.setSlotCount(6)
        assertThat(vm.uiState.value.slots).hasSize(6)
        assertThat(vm.uiState.value.slots[0].name).isEqualTo("Café da manhã")
        assertThat(vm.uiState.value.slots[5].minutes).isEqualTo(22 * 60 + 30)

        vm.setSlotCount(9)
        assertThat(vm.uiState.value.slots).hasSize(6)
        vm.setSlotCount(2)
        assertThat(vm.uiState.value.slots).hasSize(2)
        // Row 0 was named (kept); row 1 was untouched and takes the 2-meal default (20:00).
        assertThat(vm.uiState.value.slots.map { it.minutes }).containsExactly(450, 1200).inOrder()
        vm.setSlotName(1, "  ")
        assertThat(vm.uiState.value.o3Valid).isFalse()
        vm.setSlotName(1, "Janta")
        vm.setSlotTime(1, 20 * 60 + 15)
        assertThat(vm.uiState.value.o3Valid).isTrue()
        assertThat(vm.uiState.value.slots[1].minutes).isEqualTo(1215)
    }

    @Test
    fun o4_prefillsSplitOfDay1CeilingUnlessEdited() = runBlocking<Unit> {
        val vm = loadedVm()
        vm.setCeilingMode("weekdayWeekend")
        vm.setWeekdayField("1800")
        vm.setWeekendField("2400")
        vm.enterMacros() // 2026-03-16 is a Monday -> weekday
        assertThat(vm.uiState.value.day1Ceiling).isEqualTo(1800)
        assertThat(listOf(vm.uiState.value.proteinField, vm.uiState.value.carbField, vm.uiState.value.fatField))
            .containsExactly("135", "180", "60").inOrder()

        vm.setProtein("170")
        vm.setWeekdayField("2000")
        vm.enterMacros()
        assertThat(vm.uiState.value.day1Ceiling).isEqualTo(2000)
        assertThat(vm.uiState.value.proteinField).isEqualTo("170")
        assertThat(vm.uiState.value.carbField).isEqualTo("180")
    }

    @Test
    fun complete_persistsProfileAndSlots_andSurvivesRestart() = runBlocking<Unit> {
        val repo = DayRepository(db, clock, store)
        val vm = loadedVm(repo)
        vm.setSex("male")
        vm.setAge("27")
        vm.setHeight("180")
        vm.setWeight("116")
        vm.setEat("partial")
        vm.setPct("40")
        vm.setSlotCount(3)
        vm.setSlotName(0, "Café")
        vm.setSlotName(1, "Almoço")
        vm.setSlotName(2, "Janta")
        vm.setSlotTime(0, 21 * 60) // stored sorted by time
        vm.enterMacros()

        var completed = false
        vm.completeOnboarding { completed = true }
        val snap = repo.observeToday().first { it.onboardingDone }
        assertThat(snap.sex).isEqualTo("male")
        assertThat(snap.ageYears).isEqualTo(27)
        assertThat(snap.heightCm).isEqualTo(180)
        assertThat(snap.weightKg).isEqualTo(116.0)
        assertThat(snap.kcalSame).isEqualTo(2160)
        assertThat(snap.eat).isEqualTo("partial")
        assertThat(snap.pct).isEqualTo(40)
        assertThat(listOf(snap.proteinTargetG, snap.carbTargetG, snap.fatTargetG)).containsExactly(162, 216, 72).inOrder()
        assertThat(snap.firstDay).isEqualTo("2026-03-16")
        assertThat(snap.slots.map { it.name to it.minutesFromMidnight })
            .containsExactly("Almoço" to 750, "Janta" to 1200, "Café" to 1260).inOrder()
        vm.uiState.first { it.isComplete }
        assertThat(completed).isTrue()

        // Kill + relaunch: a fresh repository and ViewModel read the same rows back.
        val again = loadedVm(DayRepository(db, clock, store))
        assertThat(again.uiState.value.sex).isEqualTo("male")
        assertThat(again.uiState.value.weightField).isEqualTo("116")
        assertThat(again.uiState.value.slots.map { it.name }).containsExactly("Almoço", "Janta", "Café").inOrder()
        assertThat(again.uiState.value.proteinField).isEqualTo("162")
        assertThat(again.uiState.value.tone).isEqualTo("seco")
    }

    /** A60 part B (O5): seco is preselected; the tone picked on O5 is stored with the onboarding, onboardingDone last. */
    @Test
    fun o5_toneDefaultsToSeco_andDuroIsStoredOnComplete() = runBlocking<Unit> {
        val repo = DayRepository(db, clock, store)
        val vm = loadedVm(repo)
        assertThat(vm.uiState.value.tone).isEqualTo("seco")
        vm.setTone("bravo")
        assertThat(vm.uiState.value.tone).isEqualTo("seco")
        vm.setTone("duro")
        vm.setSex("female")
        vm.setAge("30")
        vm.setHeight("165")
        vm.setWeight("60")
        vm.setSlotCount(3)
        listOf("Café", "Almoço", "Jantar").forEachIndexed { i, n -> vm.setSlotName(i, n) }
        vm.enterMacros()
        assertThat(repo.observeToday().first().onboardingDone).isFalse()
        vm.completeOnboarding {}
        val snap = repo.observeToday().first { it.onboardingDone }
        assertThat(snap.tone).isEqualTo("duro")
    }

    @Test
    fun complete_isBlockedWhileInvalid_andWritesNoEmptySlots() = runBlocking<Unit> {
        val repo = DayRepository(db, clock, store)
        val vm = loadedVm(repo)
        vm.setSex("female")
        vm.completeOnboarding { error("must not complete") }
        val snap = repo.observeToday().first()
        assertThat(snap.onboardingDone).isFalse()
        assertThat(snap.slots).isEmpty()
        assertThat(vm.uiState.value.isComplete).isFalse()
    }

    private suspend fun loadedVm(repo: DayRepository = DayRepository(db, clock, store)): OnboardingViewModel {
        val vm = OnboardingViewModel(repo, clock)
        withTimeout(5_000) { vm.uiState.first { it.loaded } }
        return vm
    }

    @Test fun splitGroupsAdvanceBackCopyAndFinishWithoutDuplicatedIds() = runBlocking<Unit> {
        val vm = loadedVm()
        vm.setSlotMode("split")
        vm.setSlotCount(2)
        vm.setSlotName(0, "Café")
        vm.setSlotName(1, "Jantar")
        var next = false
        vm.nextSlotGroup { next = true }
        assertThat(next).isFalse()
        assertThat(vm.uiState.value.slotSchedule.index).isEqualTo(1)
        vm.copyPreviousSlots()
        assertThat(vm.uiState.value.slots.map { it.name }).containsExactly("Café", "Jantar").inOrder()
        vm.setSlotTime(0, 570)
        vm.previousSlotGroup {}
        assertThat(vm.uiState.value.slots[0].minutes).isNotEqualTo(570)
        vm.nextSlotGroup {}
        assertThat(vm.uiState.value.slots[0].minutes).isEqualTo(570)
        vm.nextSlotGroup { next = true }
        assertThat(next).isTrue()
        assertThat(vm.uiState.value.slotSchedule.valid).isTrue()
    }

}
