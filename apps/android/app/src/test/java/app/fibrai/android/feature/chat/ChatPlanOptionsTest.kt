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

/** A67 (ADR-051): plan options with their own Registrar and Reservar, declared routines, equipment and liked facts, discovery. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatPlanOptionsTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { options() }
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
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "options_${System.nanoTime()}.preferences_pb") })
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

    private val memoryFile = MemFile()
    private val memory = FactMemory(memoryFile)

    private fun vm() = ChatViewModel(repo, service, clock, memory, FakePhotoFiles(), telemetry)

    private fun estimate(kcal: Int, slot: Long, text: String, p: Int = 20) = ChatEstimate(
        kcal = kcal.toDouble(), p = p.toDouble(), c = 30.0, g = 10.0, confidence = "high",
        items = listOf(ItemOut("frango desfiado", 100.0, 160.0), ItemOut("1 pão sírio", 60.0, 160.0)), suggestedSlot = slot.toString(), mealText = text,
    )

    private fun options() = ChatOut(
        reply = "Duas opções para o jantar:\n**Opção 1: Pizza de pão sírio**\n- 1 pão sírio (60 g)\n**Opção 2: Omelete de forno**\n- 3 ovos",
        actions = listOf(
            ChatAction(
                id = "a1", type = "plan", slot = jantar.toString(), estimate = estimate(420, jantar, "Pizza de pão sírio", p = 40), record = "none",
                options = kotlinx.serialization.json.Json.parseToJsonElement(
                    """[{"id":"o1","name":"Pizza de pão sírio","estimate":{"kcal":420,"p":40,"c":38,"g":12,"items":[{"name":"1 pão sírio","g":60,"kcal":160}],"meal_text":"Pizza de pão sírio"}},""" +
                        """{"id":"o2","name":"Omelete de forno","estimate":{"kcal":360,"p":28,"c":14,"g":20,"items":[{"name":"ricota","g":50,"kcal":80}],"meal_text":"Omelete de forno: 3 ovos e ricota"}}]""",
                ),
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

    private fun ChatUiState.planBubble() = items.filterIsInstance<ChatItem.Assistant>().lastOrNull() ?: ChatItem.Assistant(-1, "", "")

    @Test
    fun twoOptions_blocksWithActions_noBarUnderTheBubble_leadLineOnly() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        val ui = vm.await { it.planBubble().options.size == 2 }
        val bubble = ui.planBubble()
        assertThat(bubble.text).isEqualTo("Duas opções para o jantar:")
        assertThat(bubble.options.map { it.title }).containsExactly("Opção 1: Pizza de pão sírio", "Opção 2: Omelete de forno").inOrder()
        assertThat(bubble.options[1].items).containsExactly("50 g de ricota")
        assertThat(bubble.options[0].items).containsExactly("1 pão sírio (60 g)")
        assertThat(bubble.options.all { it.canRecord && it.canReserve }).isTrue()
        assertThat(ui.actions).isNull()
        // The option ids stay with the message.
        assertThat(repo.observeMessages().first().last { it.role == "assistant" }.actions).contains("\"o2\"")
    }

    @Test
    fun registrarOnOption2_recordsItsNumbers() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        val id = vm.await { it.planBubble().options.size == 2 }.planBubble().id
        vm.recordOption(id, "o2")
        withTimeout(5_000) { while (repo.slotState("2026-09-25", jantar).kcal != 360) delay(10) }
        assertThat(repo.slotState("2026-09-25", jantar).records.single().text).isEqualTo("Omelete de forno: 3 ovos e ricota")
        // Recorded: the options lose their actions.
        vm.await { s -> s.planBubble().options.none { it.canRecord } }
    }

    @Test
    fun reservarOnOption2_reservesItsNumbers() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        val id = vm.await { it.planBubble().options.size == 2 }.planBubble().id
        vm.reserveOption(id, "o2")
        withTimeout(5_000) { while (repo.observeToday().first().planned[jantar]?.kcal != 360) delay(10) }
        vm.await { it.planBubble().reservedFor == "Jantar" }
    }

    @Test
    fun discovery_onTheFirstTwoTurnsWithAnEmptyMemory_declaredRoutineEquipmentAndLiked() = runBlocking<Unit> {
        answer = { ChatOut(reply = "Oi! Me conta:\n- seu café de sempre?", intent = "question", record = "none", model = "gpt-6-luna") }
        val vm = vm()
        send(vm, "oi")
        assertThat(requests.last().discovery).isTrue()
        answer = {
            ChatOut(
                reply = "Vou lembrar: café de 2 ovos e pão; air fryer.",
                intent = "question",
                record = "none",
                memoryUpdates = listOf(
                    ChatMemoryUpdate("add", null, "dynamic", "routine", "cafe", "2 ovos mexidos e 1 pão francês", almoco.toString(), 320, 17, 29, 16, declared = true),
                    ChatMemoryUpdate("add", null, "permanent", "equipment", "air fryer", "Tem air fryer"),
                ),
                model = "gpt-6-luna",
            )
        }
        send(vm, "cafe 2 ovos e pao, tenho air fryer")
        assertThat(requests.last().discovery).isTrue()
        val facts = memory.read(java.time.LocalDate.parse("2026-09-25")).facts
        val routine = facts.single { it.category == "routine" }
        assertThat(routine.declared).isTrue()
        assertThat(routine.days).isEmpty()
        assertThat(listOf(routine.kcal, routine.p, routine.c, routine.g)).containsExactly(320, 17, 29, 16).inOrder()
        assertThat(facts.single { it.category == "equipment" }.permanent).isTrue()
        // The memory is no longer empty: no more discovery; the routine goes with its numbers.
        answer = { ChatOut(reply = "Ok.", intent = "question", record = "none", model = "gpt-6-luna") }
        send(vm, "valeu")
        assertThat(requests.last().discovery).isFalse()
        assertThat(requests.last().facts!!.single { it.category == "routine" }.kcal).isEqualTo(320)
        // A declared routine lives 21 days from its creation without a recorded day.
        assertThat(memory.read(java.time.LocalDate.parse("2026-10-15")).facts.any { it.category == "routine" }).isTrue()
        assertThat(memory.read(java.time.LocalDate.parse("2026-10-16")).facts.any { it.category == "routine" }).isFalse()
    }

    @Test
    fun aLikedDishIsStoredWithItsSlotAndNumbers() = runBlocking<Unit> {
        answer = {
            ChatOut(
                reply = "Anotado que a omelete funcionou.", intent = "question", record = "none", model = "gpt-6-luna",
                memoryUpdates = listOf(ChatMemoryUpdate("add", null, "dynamic", "liked", "omelete", "Omelete de forno", jantar.toString(), 360, 28, 14, 20)),
            )
        }
        val vm = vm()
        send(vm, "a omelete de ontem ficou otima")
        val liked = memory.read(java.time.LocalDate.parse("2026-09-25")).facts.single { it.category == "liked" }
        assertThat(liked.slot).isEqualTo(jantar.toString())
        assertThat(liked.kcal).isEqualTo(360)
        assertThat(liked.days).containsExactly("2026-09-25")
    }
}
