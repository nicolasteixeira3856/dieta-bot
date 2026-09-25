package com.nutri.android.ui

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.data.DayRepository
import com.nutri.android.data.DishOut
import com.nutri.android.data.EstimateGate
import com.nutri.android.data.EstimateOut
import com.nutri.android.data.FitOut
import com.nutri.android.data.InstantClock
import com.nutri.android.data.NutriApi
import com.nutri.android.data.NutriDatabase
import com.nutri.android.data.PhotoCompressor
import com.nutri.android.domain.windowTitle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
class DayActionsTest {
    private lateinit var context: Context
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = MutableClock(DINNER_MONDAY)
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
        Dispatchers.resetMain()
    }

    @Test
    fun highConfirm_writesOneMealLogAndLowersRemaining() = runBlocking {
        val api = apiWithEstimate(HIGH)
        val (repo, vm) = startHome(api)
        reachT2(vm)
        val remainingBefore = vm.ui.value.remaining

        vm.confirm()

        val ui = awaitUi(vm) { it.stage == Stage.HOME && it.logs.size == 1 }
        val logs = repo.observeToday().first().logs
        assertThat(logs).hasSize(1)
        assertThat(logs[0].kcal).isEqualTo(420)
        assertThat(logs[0].p).isEqualTo(22)
        assertThat(logs[0].stable).isTrue()
        assertThat(logs[0].text).isEqualTo(MEAL_TEXT)
        assertThat(ui.remaining).isLessThan(remainingBefore)
        assertThat(ui.logs[0].line).isEqualTo(
            "${windowTitle(logs[0].window)} · 420 kcal · 22 g P",
        )
        assertThat(ui.chipLabel).isNull()
        assertThat(ui.appDay).isEqualTo(1)
    }

    @Test
    fun lowSim_writesOneMealLog() = runBlocking {
        val api = apiWithEstimate(LOW)
        val (repo, vm) = startHome(api)
        reachT2(vm)

        vm.t2Yes()

        awaitUi(vm) { it.stage == Stage.HOME && it.logs.size == 1 }
        val logs = repo.observeToday().first().logs
        assertThat(logs).hasSize(1)
        assertThat(logs[0].kcal).isEqualTo(380)
        assertThat(logs[0].p).isEqualTo(18)
        assertThat(vm.ui.value.logs[0].line).contains(windowTitle(logs[0].window))
        assertThat(vm.ui.value.logs[0].line).contains("380 kcal")
        assertThat(vm.ui.value.logs[0].line).contains("18 g P")
    }

    @Test
    fun lowRevise_zeroLogsAndOpensT1() = runBlocking {
        val api = apiWithEstimate(LOW)
        val (repo, vm) = startHome(api)
        reachT2(vm)

        vm.t2Revise()

        val ui = awaitUi(vm) { it.stage == Stage.HOME && it.sheet == SheetKind.T1 }
        settle()
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(ui.text).isEqualTo(MEAL_TEXT)
        assertThat(ui.estimate).isNull()
    }

    @Test
    fun lowDiscard_zeroLogsAndHome() = runBlocking {
        val api = apiWithEstimate(LOW)
        val (repo, vm) = startHome(api)
        reachT2(vm)

        vm.t2Discard()

        val ui = awaitUi(vm) { it.stage == Stage.HOME && it.sheet == null }
        settle()
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(ui.logs).isEmpty()
    }

    @Test
    fun kcalZeroLow_confirmDoesNotInsert() = runBlocking {
        val api = apiWithEstimate(LOW_ZERO)
        val (repo, vm) = startHome(api)
        reachT2(vm)
        assertThat(vm.ui.value.t2ConfirmEnabled).isFalse()
        assertThat(vm.ui.value.estimate!!.kcal.toInt()).isEqualTo(0)

        vm.confirm()
        settle()

        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(vm.ui.value.stage).isEqualTo(Stage.T2)
        assertThat(vm.ui.value.logs).isEmpty()
    }

    @Test
    fun t3CommitFittingDish_writesOneMealLogWithThatKcal() = runBlocking {
        val dish = DishOut(name = "omelete 3 ovos", kcal = 420.0, p = 30.0, fits = true)
        val api = apiWithFit(FitOut(fits = true, dish = dish))
        val (repo, vm) = startHome(api)
        vm.openFit()
        awaitUi(vm) { it.sheet == SheetKind.T3 && it.fit == null }
        assertThat(vm.ui.value.t3Cta).isEqualTo("Encaixar")
        val remainingBefore = vm.ui.value.remaining

        vm.t3Primary()
        awaitUi(vm) { it.fit != null && it.t3Cta == "Vou nesse" }
        assertThat(vm.ui.value.fitDishes.any { it.fits && it.kcal.toInt() == 420 }).isTrue()
        vm.selectFitDish(0)
        vm.t3Primary()

        val ui = awaitUi(vm) { it.sheet == null && it.logs.size == 1 }
        val logs = repo.observeToday().first().logs
        assertThat(logs).hasSize(1)
        assertThat(logs[0].kcal).isEqualTo(420)
        assertThat(logs[0].p).isEqualTo(30)
        assertThat(logs[0].text).isEqualTo("omelete 3 ovos")
        assertThat(logs[0].stable).isTrue()
        assertThat(ui.remaining).isLessThan(remainingBefore)
        assertThat(ui.logs[0].line).isEqualTo(
            "${windowTitle(logs[0].window)} · 420 kcal · 30 g P",
        )
        coVerify(exactly = 1) { api.fit(any()) }
    }

    @Test
    fun t3AlreadyAte_zeroLogsAndEmptyT1() = runBlocking {
        val dish = DishOut(name = "sopa + pão", kcal = 380.0, p = 18.0, fits = true)
        val api = apiWithFit(FitOut(fits = true, dish = dish))
        val (repo, vm) = startHome(api)
        vm.openFit()
        vm.t3Primary()
        awaitUi(vm) { it.fit != null }

        vm.alreadyAte()

        val ui = awaitUi(vm) { it.sheet == SheetKind.T1 }
        settle()
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(ui.text).isEmpty()
        assertThat(ui.fit).isNull()
        assertThat(ui.logs).isEmpty()
    }

    @Test
    fun t2StaysFullScreenRoute() {
        assertThat(SheetKind.entries.map { it.name }).containsExactly("T1", "T3").inOrder()
        assertThat(Stage.T2).isEqualTo(Stage.T2)
        assertThat(captureState("t2").stage).isEqualTo(Stage.T2)
        assertThat(captureState("t2").sheet).isNull()
    }

    private fun repository() = DayRepository(db, clock, store)

    private fun viewModel(repo: DayRepository, api: NutriApi): DayViewModel {
        return DayViewModel(
            days = repo,
            gate = EstimateGate(api),
            photos = mockk<PhotoCompressor>(relaxed = true),
            clock = clock,
        )
    }

    private fun apiWithEstimate(out: EstimateOut): NutriApi {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } returns out
        coEvery { api.fit(any()) } returns FitOut()
        return api
    }

    private fun apiWithFit(out: FitOut): NutriApi {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } returns HIGH
        coEvery { api.fit(any()) } returns out
        return api
    }

    private suspend fun startHome(api: NutriApi): Pair<DayRepository, DayViewModel> {
        val repo = repository()
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
        val vm = viewModel(repo, api)
        awaitUi(vm) { it.shortDate.isNotBlank() }
        vm.leaveSplash()
        awaitUi(vm) { it.stage == Stage.HOME }
        return repo to vm
    }

    private suspend fun reachT2(vm: DayViewModel) {
        vm.text(MEAL_TEXT)
        vm.submitLog()
        awaitUi(vm) { it.stage == Stage.T2 && it.estimate != null }
    }

    private suspend fun awaitUi(vm: DayViewModel, predicate: (DayUi) -> Boolean): DayUi {
        return withTimeout(5_000) { vm.ui.first(predicate) }
    }

    private suspend fun settle() {
        delay(400)
    }

    private class MutableClock(var instant: Instant) : InstantClock {
        override fun now(): Instant = instant
    }

    companion object {
        private val DINNER_MONDAY: Instant = Instant.parse("2026-03-16T19:00:00-03:00")
        private const val MEAL_TEXT = "2 pães, ovo, café"
        private val HIGH = EstimateOut(kcal = 420.0, p = 22.0, confidence = "high")
        private val LOW = EstimateOut(
            kcal = 380.0,
            p = 18.0,
            confidence = "low",
            question = "Pão era francês?",
        )
        private val LOW_ZERO = EstimateOut(
            kcal = 0.0,
            p = 0.0,
            confidence = "low",
            question = "descreve em 1 linha",
        )
    }
}
