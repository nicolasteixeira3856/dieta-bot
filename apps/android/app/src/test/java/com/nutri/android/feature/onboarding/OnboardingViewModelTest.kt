package com.nutri.android.feature.onboarding

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.NutriDatabase
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
class OnboardingViewModelTest {
    private lateinit var context: Context
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private val clock = InstantClock { Instant.parse("2026-03-16T12:00:00-03:00") }
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
    fun defaultsAndMutations() = runBlocking {
        val repo = DayRepository(db, clock, store)
        val vm = OnboardingViewModel(repo, clock)

        assertThat(vm.uiState.value.ceilingMode).isEqualTo("same")
        assertThat(vm.uiState.value.sameField).isEqualTo("2000")

        vm.setCeilingMode("weekdayWeekend")
        vm.setWeekdayField("1900")
        vm.setWeekendField("2200")
        vm.setEat("partial")
        vm.setPct("40")

        assertThat(vm.uiState.value.ceilingMode).isEqualTo("weekdayWeekend")
        assertThat(vm.uiState.value.weekdayField).isEqualTo("1900")
        assertThat(vm.uiState.value.weekendField).isEqualTo("2200")
        assertThat(vm.uiState.value.eat).isEqualTo("partial")
        assertThat(vm.uiState.value.pct).isEqualTo("40")

        var completed = false
        vm.completeOnboarding { completed = true }

        val snap = repo.observeToday().first { it.onboardingDone }
        assertThat(completed).isTrue()
        assertThat(vm.uiState.value.isComplete).isTrue()
        assertThat(snap.onboardingDone).isTrue()
        assertThat(snap.ceilingMode).isEqualTo("weekdayWeekend")
        assertThat(snap.kcalWeekday).isEqualTo(1900)
        assertThat(snap.kcalWeekend).isEqualTo(2200)
        assertThat(snap.eat).isEqualTo("partial")
        assertThat(snap.pct).isEqualTo(40)
    }
}
