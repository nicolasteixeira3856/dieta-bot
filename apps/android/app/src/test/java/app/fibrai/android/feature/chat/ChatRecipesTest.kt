package app.fibrai.android.feature.chat

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
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatAction
import app.fibrai.android.core.network.ChatMemoryUpdate
import app.fibrai.android.core.memory.FakeMemoryFile as MemFile
import app.fibrai.android.core.network.ChatWorkout
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ItemOut
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SlotState
import com.google.common.truth.Truth.assertThat
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

/** A68 (ADR-052): Salvar receita, the recipe index and the named recipe in the prompt, a record by recipe keeps its version, delete. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatRecipesTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { recipePlan() }
    private val service = ChatService { requests += it; answer() }
    private val telemetry = FakeTelemetry()
    private var jantar = 0L
    private var almoco = 0L
    private var cafe = 0L
    private var lanche = 0L

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "recipes_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-09-22")
        repo.saveSlots(listOf(MealSlot(name = "Café", minutesFromMidnight = 450), MealSlot(name = "Almoço", minutesFromMidnight = 750), MealSlot(name = "Lanche", minutesFromMidnight = 960), MealSlot(name = "Jantar", minutesFromMidnight = 1200)))
        repo.observeToday().first().slots.forEach { when (it.name) { "Jantar" -> jantar = it.id; "Almoço" -> almoco = it.id; "Café" -> cafe = it.id; else -> lanche = it.id } }
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, FactMemory(FakeMemoryFile()), FakePhotoFiles(), telemetry)

    private fun recipePlan() = ChatOut(
        reply = "Frango com brócolis e arroz:\n- 120 g de peito de frango\n1. Grelhe o frango.",
        actions = listOf(
            ChatAction(
                id = "a1", type = "plan", slot = jantar.toString(), record = "none",
                estimate = ChatEstimate(kcal = 520.0, p = 46.0, c = 41.0, g = 17.0, confidence = "high", items = listOf(ItemOut("frango", 120.0, 200.0)),
                    suggestedSlot = jantar.toString(), mealText = "Frango com brócolis e arroz"),
                recipe = kotlinx.serialization.json.Json.parseToJsonElement(
                    """{"name":"Frango com brócolis e arroz","ingredients":[{"name":"Peito de frango","g":120,"kcal":200},{"name":"Arroz cozido","g":120,"kcal":160},""" +
                        """{"name":"Brócolis","g":100,"kcal":35}],"steps":["Grelhe o frango.","Refogue o brócolis.","Misture com o arroz."]}""",
                ),
            ),
        ),
        model = "gpt-6-luna",
    )

    private fun logByRecipe(id: String) = ChatOut(
        reply = "Jantar: frango com brócolis e arroz, 520 kcal.",
        actions = listOf(
            ChatAction(
                id = "a1", type = "log", slot = jantar.toString(), record = "auto", recipeId = id,
                estimate = ChatEstimate(kcal = 520.0, p = 46.0, c = 41.0, g = 17.0, confidence = "high", suggestedSlot = jantar.toString(), mealText = "Frango com brócolis e arroz"),
                mealChange = kotlinx.serialization.json.Json.parseToJsonElement("""{"operation":"new","base_slot":null,"addition":null}"""),
            ),
        ),
        model = "gpt-6-luna",
    )

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { while (answers() <= before) delay(10) }
        vm.await { !it.sending }
    }

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    @Test
    fun salvarReceita_savesVersion1Once_receipt_thenTheIndexAndTheNamedRecipeGoToTheServer() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "me passa uma receita de frango com brócolis")
        val actions = vm.await { it.actions?.saveRecipe == true }.actions!!
        vm.saveRecipe(actions.estimateId)
        vm.saveRecipe(actions.estimateId)
        vm.await { s -> s.actions?.saveRecipe == false && s.items.any { it is ChatItem.Receipt && it.kind == ReceiptKind.RECIPE_SAVED } }
        val saved = repo.savedRecipes().single()
        assertThat(saved.recipe.name).isEqualTo("Frango com brócolis e arroz")
        assertThat(saved.version.version).isEqualTo(1)
        assertThat(listOf(saved.version.kcal, saved.version.p, saved.version.c, saved.version.g)).containsExactly(520, 46, 41, 17).inOrder()
        // Saving records nothing.
        assertThat(repo.slotState("2026-09-25", jantar).records).isEmpty()
        answer = { ChatOut(reply = "Ok.", intent = "question", record = "none", model = "gpt-6-luna") }
        send(vm, "obrigado")
        assertThat(requests.last().recipes.single().id).isEqualTo("R${saved.recipe.id}")
        assertThat(requests.last().recipes.single().keyFoods).containsExactly("Peito de frango", "Arroz cozido", "Brócolis").inOrder()
        assertThat(requests.last().recipeFull).isNull()
        send(vm, "lembra a receita do frango com brocolis e arroz?")
        val full = requests.last().recipeFull!!
        assertThat(full.ingredients.map { it.name }).containsExactly("Peito de frango", "Arroz cozido", "Brócolis").inOrder()
        assertThat(full.steps).hasSize(3)
        // A log by recipe keeps the version it used.
        answer = { logByRecipe("R${saved.recipe.id}") }
        send(vm, "jantei o frango com brocolis")
        withTimeout(5_000) { while (repo.slotState("2026-09-25", jantar).kcal != 520) delay(10) }
        assertThat(repo.slotState("2026-09-25", jantar).records.single().recipeVersionId).isEqualTo(saved.version.id)
    }

    @Test
    fun configList_detail_andDelete() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "me passa uma receita de frango com brócolis")
        vm.saveRecipe(vm.await { it.actions?.saveRecipe == true }.actions!!.estimateId)
        withTimeout(5_000) { while (repo.savedRecipes().isEmpty()) delay(10) }
        val id = repo.savedRecipes().single().recipe.id
        val recipes = app.fibrai.android.feature.recipes.RecipesViewModel(repo, telemetry)
        val list = withTimeout(5_000) { recipes.uiState.first { it.rows.isNotEmpty() } }
        assertThat(list.rows.single().name).isEqualTo("Frango com brócolis e arroz")
        recipes.open(id)
        val detail = withTimeout(5_000) { recipes.uiState.first { it.detail != null } }.detail!!
        assertThat(detail.versionLine).isEqualTo("Versão 1 · salva em 25 de setembro")
        assertThat(detail.ingredients.first()).isEqualTo("Peito de frango" to "120 g")
        recipes.askDelete()
        recipes.confirmDelete()
        withTimeout(5_000) { recipes.uiState.first { it.deleted && it.rows.isEmpty() } }
        assertThat(repo.savedRecipes()).isEmpty()
    }
}
