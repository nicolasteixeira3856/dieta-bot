package app.fibrai.android.feature.memory

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.Memory
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.MemoryUpdate
import app.fibrai.android.domain.NextIds
import com.google.common.truth.Truth.assertThat
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

/** A69 (ADR-053): what Tali knows, grouped with its origin; delete keeps a tombstone until the next compaction; correct. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MemoryViewModelTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private val now = Instant.parse("2026-10-08T18:00:00-03:00")
    private val today = LocalDate.parse("2026-10-08")
    private val memory = FactMemory(FakeMemoryFile())
    private val telemetry = FakeTelemetry()
    private var cafe = 0L

    private fun fact(id: String, kind: String, category: String, key: String, text: String, days: List<String> = emptyList(), source: String = "observed",
                     created: String = "2026-09-25", slot: String? = null, kcal: Int? = null) =
        Fact(id, kind, category, key, text, slot, source, days, created, kcal, kcal?.let { 26 }, kcal?.let { 36 }, kcal?.let { 20 })

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "memory_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, InstantClock { now }, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(listOf(MealSlot(name = "Café da manhã", minutesFromMidnight = 450)))
        cafe = repo.observeToday().first().slots.single().id
        memory.replaceAll(
            Memory(
                NextIds(3, 3, 2),
                listOf(
                    fact("P1", "permanent", "preference", "leite", "Usa leite semidesnatado", listOf("2026-09-25"), "explicit"),
                    fact("D1", "dynamic", "routine", "cafe", "Pão francês com ovo mexido", (3..7).map { "2026-10-0$it" }, slot = cafe.toString(), kcal = 430),
                    fact("T1", "temp", "preference", "viagem", "Viagem até domingo", created = "2026-10-08"),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = MemoryViewModel(memory, repo, InstantClock { now }, telemetry)

    @Test
    fun groupsWithChipsMacrosAndOrigins() = runBlocking<Unit> {
        val ui = withTimeout(5_000) { vm().uiState.first { it.loaded && it.groups.isNotEmpty() } }
        assertThat(ui.groups.map { it.label }).containsExactly("Fixas", "Rotinas", "Temporárias").inOrder()
        assertThat(ui.groups[0].facts.single().origin).isEqualTo("Declarado · 25/09")
        val routine = ui.groups[1].facts.single()
        assertThat(routine.chip).isEqualTo("Rotina · Café da manhã")
        assertThat(routine.macros).containsExactly(430, 26, 36, 20).inOrder()
        assertThat(routine.origin).isEqualTo("Registrado 5 dias · último 07/10")
        assertThat(ui.groups[2].facts.single().origin).isEqualTo("Até 11/10 · criado 08/10")
    }

    @Test
    fun deleteKeepsATombstone_anAddWithTheSameKeyIsRefused_untilTheNextCompaction() = runBlocking<Unit> {
        val vm = vm()
        withTimeout(5_000) { vm.uiState.first { it.loaded && it.groups.isNotEmpty() } }
        vm.askDelete("D1")
        vm.confirmDelete()
        withTimeout(5_000) { vm.uiState.first { s -> s.groups.none { g -> g.facts.any { it.id == "D1" } } } }
        val add = MemoryUpdate("add", null, "dynamic", "routine", "cafe", "Pão francês com ovo mexido", cafe.toString())
        assertThat(memory.apply(listOf(add), today).memory.facts.none { it.key == "cafe" }).isTrue()
        // A temp fact is never blocked; another key goes in.
        assertThat(memory.apply(listOf(MemoryUpdate("add", null, "permanent", "preference", "acucar", "Sem açúcar")), today).memory.facts.any { it.key == "acucar" }).isTrue()
        memory.clearTombstones(today)
        assertThat(memory.apply(listOf(add), today).memory.facts.any { it.key == "cafe" }).isTrue()
        assertThat(telemetry.events.map { it.first }).contains(app.fibrai.android.core.telemetry.TelemetryEvents.MEMORY_FACT_DELETED)
    }

    @Test
    fun correctChangesOnlyTheText_blankChangesNothing() = runBlocking<Unit> {
        val vm = vm()
        withTimeout(5_000) { vm.uiState.first { it.loaded && it.groups.isNotEmpty() } }
        vm.edit("P1")
        assertThat(vm.uiState.value.draft).isEqualTo("Usa leite semidesnatado")
        vm.setDraft("  Usa leite desnatado ")
        vm.saveEdit()
        withTimeout(5_000) { vm.uiState.first { s -> s.groups[0].facts.single().text == "Usa leite desnatado" } }
        val fact = memory.read(today).facts.single { it.id == "P1" }
        assertThat(listOf(fact.kind, fact.category, fact.slot)).containsExactly("permanent", "preference", null).inOrder()
        vm.edit("P1")
        vm.setDraft("   ")
        vm.saveEdit()
        assertThat(memory.read(today).facts.single { it.id == "P1" }.text).isEqualTo("Usa leite desnatado")
    }

    @Test
    fun theRulesAlone() {
        val m = Memory(facts = listOf(fact("D1", "dynamic", "routine", "cafe", "x", listOf("2026-10-07")), fact("T1", "temp", "preference", "viagem", "y", created = "2026-10-08")))
        val deleted = MemoryRules.delete(MemoryRules.delete(m, "D1", today), "T1", today)
        assertThat(deleted.facts).isEmpty()
        // A temp fact leaves no tombstone.
        assertThat(deleted.tombstones.map { it.key }).containsExactly("cafe")
        assertThat(MemoryRules.clearTombstones(deleted).tombstones).isEmpty()
    }
}
