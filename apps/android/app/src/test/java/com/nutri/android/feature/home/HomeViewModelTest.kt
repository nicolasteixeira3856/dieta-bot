package com.nutri.android.feature.home

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.NutriDatabase
import com.nutri.android.core.network.DishOut
import com.nutri.android.core.network.EstimateGate
import com.nutri.android.core.network.EstimateOut
import com.nutri.android.core.network.FitOut
import com.nutri.android.core.network.NutriApi
import com.nutri.android.core.network.PhotoCompressor
import io.mockk.coEvery
import io.mockk.mockk
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class HomeViewModelTest {
    private lateinit var context: Context
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = InstantClock { Instant.parse("2026-03-16T19:00:00-03:00") }
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NutriDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "test_home_${System.nanoTime()}.preferences_pb")
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
    fun initialState_calculatesBudgetCorrectly() = runBlocking {
        val repo = DayRepository(db, clock, store)
        repo.saveProfile(
            ceilingMode = "same",
            kcalSame = 2000,
            kcalWeekday = 2000,
            kcalWeekend = 2300,
            kcalDays = List(7) { 2000 },
            eat = "zero",
            pct = 50,
            onboardingDone = true,
            firstDay = "2026-03-16",
        )
        val api = mockk<NutriApi>()
        val vm = HomeViewModel(
            repository = repo,
            gate = EstimateGate(api),
            photos = mockk<PhotoCompressor>(relaxed = true),
            clock = clock,
        )

        val ui = vm.uiState.first { it.shortDate.isNotBlank() }
        assertThat(ui.remaining).isEqualTo(2000)
        assertThat(ui.effectiveCeiling).isEqualTo(2000)
        assertThat(ui.window).isEqualTo("dinner")
        assertThat(ui.logs).isEmpty()
    }

    @Test
    fun openLogAndSubmit_emitsNavigateToT2() = runBlocking {
        val repo = DayRepository(db, clock, store)
        val api = mockk<NutriApi>()
        val estimateOut = EstimateOut(kcal = 450.0, p = 30.0, confidence = "high")
        coEvery { api.estimate(any()) } returns estimateOut

        val vm = HomeViewModel(
            repository = repo,
            gate = EstimateGate(api),
            photos = mockk<PhotoCompressor>(relaxed = true),
            clock = clock,
        )

        vm.navEvents.test {
            vm.openLog()
            assertThat(vm.uiState.value.sheet).isEqualTo(HomeSheetKind.T1)

            vm.setLogText("frango e salada")
            vm.submitLog()

            val navEvent = awaitItem()
            assertThat(navEvent).isInstanceOf(HomeNavEvent.NavigateToT2::class.java)
            val t2Event = navEvent as HomeNavEvent.NavigateToT2
            assertThat(t2Event.mealText).isEqualTo("frango e salada")
            assertThat(t2Event.estimate.kcal).isEqualTo(450.0)
            assertThat(vm.uiState.value.sheet).isNull()
        }
    }

    @Test
    fun commitFit_addsLogToRepository() = runBlocking {
        val repo = DayRepository(db, clock, store)
        val api = mockk<NutriApi>()
        val dish = DishOut(name = "omelete", kcal = 350.0, p = 25.0, fits = true)
        coEvery { api.fit(any()) } returns FitOut(fits = true, dish = dish)

        val vm = HomeViewModel(
            repository = repo,
            gate = EstimateGate(api),
            photos = mockk<PhotoCompressor>(relaxed = true),
            clock = clock,
        )

        vm.openFit()
        assertThat(vm.uiState.value.sheet).isEqualTo(HomeSheetKind.T3)

        vm.primaryFitAction() // requests fit
        vm.uiState.first { it.fit != null }
        vm.selectFitDish(0)
        vm.primaryFitAction() // commits fit

        val ui = vm.uiState.first { it.sheet == null && it.logs.size == 1 }
        assertThat(ui.sheet).isNull()
        val snap = repo.observeToday().first { it.logs.isNotEmpty() }
        assertThat(snap.logs).hasSize(1)
        assertThat(snap.logs[0].text).isEqualTo("omelete")
        assertThat(snap.logs[0].kcal).isEqualTo(350)
    }
}
