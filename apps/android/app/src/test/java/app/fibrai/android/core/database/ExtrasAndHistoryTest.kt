package app.fibrai.android.core.database

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.fibrai.android.domain.Extras
import app.fibrai.android.domain.SlotChange
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import app.fibrai.android.feature.chat.ChatActions
import app.fibrai.android.feature.chat.PromptBuilder
import app.fibrai.android.feature.home.HomeFixtures
import app.fibrai.android.feature.home.HomePanelMapper
import app.fibrai.android.feature.home.DayCircleState
import app.fibrai.android.feature.home.SlotState as HomeSlotState
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A72 (ADR-058): Room v17, extras as slots of their own, a record in a past day, the merged timeline, the strip, `recent`. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ExtrasAndHistoryTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var repo: DayRepository
    private val now = Instant.parse("2026-10-03T15:40:00-03:00")

    @Before
    fun setUp() = runBlocking<Unit> {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "a72_${System.nanoTime()}.preferences_pb")
        repo = DayRepository(db, InstantClock { now }, PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file }))
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-19")
        repo.saveSlots(HomeFixtures.slots.map { it.copy(id = 0) })
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
    }

    @Test
    fun migrationV16V17_oldRowsAreMealRecords() {
        val name = "migration_v16_v17.db"
        helper.createDatabase(name, 16).apply {
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (2,'Jantar',1200,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (1,'2026-10-08','','frango',610,52,1,2,60,14,'user')")
            close()
        }
        helper.runMigrationsAndValidate(name, 17, true, MIGRATION_16_17).apply {
            query("SELECT kind, time, extraId, kcal FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("slot")
                assertThat(it.isNull(1)).isTrue()
                assertThat(it.isNull(2)).isTrue()
                assertThat(it.getInt(3)).isEqualTo(610)
            }
            close()
        }
    }

    @Test
    fun extra_isASlotOfItsOwn_writtenReadAndDeleted() = runBlocking<Unit> {
        val extra = Extras.slotId(repo.newExtraKey())
        assertThat(Extras.isExtra(extra)).isTrue()
        val record = SlotRecord("Energético, 1 lata (350 ml)", 160, 0, 40, 0, time = "15:40")
        assertThat(repo.commitRecord(listOf(SlotChange("2026-10-03", extra, SlotState.EMPTY, SlotState.of(record))))).isNotNull()
        val today = repo.observeToday().first()
        val log = today.logs.single()
        assertThat(log.extra).isTrue()
        assertThat(log.time).isEqualTo("15:40")
        assertThat(log.slotId).isNull()
        // A second extra never collides; the first one undoes exactly its own row.
        val other = Extras.slotId(repo.newExtraKey())
        assertThat(other).isNotEqualTo(extra)
        assertThat(repo.slotState("2026-10-03", extra).records.single().time).isEqualTo("15:40")
        assertThat(repo.commitRecord(listOf(SlotChange("2026-10-03", extra, SlotState.of(record), SlotState.EMPTY)))).isNotNull()
        assertThat(repo.observeToday().first().logs).isEmpty()
    }

    @Test
    fun pastDay_recordLandsOnThatDate_todayUntouched() = runBlocking<Unit> {
        val dinner = repo.observeToday().first().slots.single { it.name == "Jantar" }.id
        val record = SlotRecord("2 fatias de pizza", 560, 24, 60, 22)
        assertThat(repo.commitRecord(listOf(SlotChange("2026-10-02", dinner, SlotState.EMPTY, SlotState.of(record))))).isNotNull()
        assertThat(repo.observeToday().first().logs).isEmpty()
        val yesterday = repo.observeDay("2026-10-02").first()
        assertThat(yesterday.logs.single().slotId).isEqualTo(dinner)
        // A changed past slot is never overwritten: the guard compares the state first.
        assertThat(repo.commitRecord(listOf(SlotChange("2026-10-02", dinner, SlotState.EMPTY, SlotState.of(record))))).isNull()
        assertThat(repo.observeDayKcal("2026-09-03", "2026-10-03").first().associate { it.date to it.kcal }).containsExactly("2026-10-02", 560)
    }

    @Test
    fun timeline_mergedByTime_extraBetweenMeals_outrosWithoutExtras() {
        val ui = HomePanelMapper.map(HomeFixtures.home1Extra, LocalDate.parse("2026-10-03"))
        assertThat(ui.timeline.map { it.name }).containsExactly("Café da manhã", "Almoço", "Extra · 15:40", "Lanche", "Jantar").inOrder()
        assertThat(ui.timeline[2].state).isEqualTo(HomeSlotState.EXTRA)
        assertThat(ui.timeline.last().state).isEqualTo(HomeSlotState.NEXT)
        assertThat(ui.consumed).isEqualTo(1300)
        assertThat(ui.slotCount).isEqualTo(4)
    }

    @Test
    fun strip_thirtyDaysOrSinceTheFirstDay_monthWhereItChanges() {
        val today = LocalDate.parse("2026-10-03")
        val strip = HomePanelMapper.strip(HomeFixtures.home1Extra, today, today, HomeFixtures.stripKcal)
        assertThat(strip).hasSize(15)
        assertThat(strip.last().state).isEqualTo(DayCircleState.TODAY_SELECTED)
        assertThat(strip.first { it.date == LocalDate.parse("2026-09-28") }.state).isEqualTo(DayCircleState.NO_RECORD)
        assertThat(strip.filter { it.month != null }.map { it.month }).containsExactly("Set", "Out").inOrder()
        val long = HomePanelMapper.strip(HomeFixtures.home1Extra.copy(firstDay = "2026-01-01"), today, LocalDate.parse("2026-10-01"), HomeFixtures.stripKcal)
        assertThat(long).hasSize(30)
        assertThat(long.single { it.state == DayCircleState.PAST_SELECTED }.date).isEqualTo(LocalDate.parse("2026-10-01"))
        assertThat(long.last().state).isEqualTo(DayCircleState.TODAY)
        assertThat(long.last().label).isEqualTo("3 de outubro, 1800 kcal")
    }

    @Test
    fun pastDay_readOnlyWithItsClosure() {
        val ui = HomePanelMapper.map(HomeFixtures.homeH, LocalDate.parse("2026-10-03"), shown = LocalDate.parse("2026-10-01"), dayKcal = HomeFixtures.stripKcal)
        assertThat(ui.past).isTrue()
        assertThat(ui.dayLabel).isEqualTo("DIA 13")
        assertThat(ui.consumed).isEqualTo(1910)
        assertThat(ui.meta).isEqualTo(2175)
    }

    @Test
    fun recent_extrasGoWithTheirTime_andTheRequestCarriesTheCapabilities() {
        val logs = listOf(
            MealLogEntity(id = 1, date = "2026-10-02", text = "café", kcal = 300, slotId = 1),
            MealLogEntity(id = 2, date = "2026-10-02", text = "energético", kcal = 160, carbs = 40, kind = "extra", time = "15:40", extraId = 9),
        )
        val recent = PromptBuilder.recent(logs, HomeFixtures.slots, LocalDate.parse("2026-10-03"))
        val extra = recent.single { it.text == "energético" }
        assertThat(extra.slotId).isEqualTo("extra")
        assertThat(extra.time).isEqualTo("15:40")
        val body = PromptBuilder.build(HomeFixtures.home1Extra, emptyList(), emptyList(), "oi", now).body
        assertThat(body.extras).isTrue()
        assertThat(body.otherDay).isTrue()
        assertThat(body.firstDay).isEqualTo("2026-09-19")
        assertThat(body.day.eatenKcal).isEqualTo(1300)
    }

    @Test
    fun actionTarget_extraAndPastDay() {
        assertThat(ChatActions.target("""[{"id":"a1","type":"log","slot":"extra","time":"15:40","meal_day":"today"}]"""))
            .isEqualTo(ChatActions.Target(extra = true, time = "15:40", day = null))
        assertThat(ChatActions.target("""[{"id":"a1","type":"log","slot":"4","meal_day":"other","day":"2026-10-02"}]"""))
            .isEqualTo(ChatActions.Target(extra = false, time = null, day = "2026-10-02"))
        assertThat(ChatActions.target("""[{"id":"a1","type":"log","slot":"4","meal_day":"today"}]""")).isNull()
        assertThat(ChatActions.target("""[{"id":"a1","type":"plan","slot":"extra"}]""")).isNull()
    }
}
