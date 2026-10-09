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
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.domain.DayBalance
import app.fibrai.android.domain.Macros
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A70 (ADR-056 § 8): the S39 comparison fixture (`fixtures/s39-comparacao-hamburguer.json`, synthetic request and response
 * of the server run) replayed through the real mapper, and the bubble printed as the app shows it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatOptionsRenderTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private val now = Instant.parse("2026-10-15T16:40:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private lateinit var answer: () -> ChatOut
    private val service = ChatService { requests += it; answer() }
    private val ids = mutableMapOf<String, Long>()
    private val json = Json { ignoreUnknownKeys = true }

    private val fixture: JsonObject by lazy {
        val text = javaClass.classLoader!!.getResource("fixtures/s39-comparacao-hamburguer.json")!!.readText(Charsets.UTF_8)
        json.parseToJsonElement(text).jsonObject
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "render_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        // The fixture's day: ceiling 2200, P 150, five slots, café and almoço eaten (1250 kcal · P 85).
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-10-01")
        repo.saveMacroTargets(150, 240, 70)
        val slots = listOf("c" to ("Café" to 420), "a" to ("Almoço" to 750), "l" to ("Lanche" to 960), "j" to ("Jantar" to 1170), "n" to ("Ceia" to 1320))
        repo.saveSlots(slots.map { (_, s) -> MealSlot(name = s.first, minutesFromMidnight = s.second) })
        val byName = repo.observeToday().first().slots.associate { it.name to it.id }
        slots.forEach { (key, s) -> ids[key] = byName.getValue(s.first) }
        repo.addLog(window = "", text = "pão com ovo e café com leite", kcal = 450, p = 28, carbs = 48, fat = 15, stable = true, slotId = ids["c"])
        repo.addLog(window = "", text = "arroz, feijão, frango e salada", kcal = 800, p = 57, carbs = 95, fat = 22, stable = true, slotId = ids["a"])
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, FactMemory(FakeMemoryFile()), FakePhotoFiles(), FakeTelemetry())

    /** The fixture's response with the profile slot keys of the server run mapped to this device's slot ids. */
    private fun response(edit: (JsonElement) -> JsonElement = { it }): ChatOut =
        json.decodeFromJsonElement(ChatOut.serializer(), edit(remap(fixture.getValue("response"))))

    private fun remap(e: JsonElement, key: String? = null): JsonElement = when (e) {
        is JsonObject -> JsonObject(e.mapValues { (k, v) -> remap(v, k) })
        is JsonArray -> JsonArray(e.map { remap(it, key) })
        is JsonPrimitive -> if (key in SLOT_KEYS && e.isString && e.content in ids) JsonPrimitive(ids.getValue(e.content).toString()) else e
    }

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun ask(vm: ChatViewModel): ChatUiState {
        vm.setComposer(fixture.getValue("request").jsonObject.getValue("text").let { (it as JsonPrimitive).content })
        vm.send()
        withTimeout(5_000) { while (repo.observeMessages().first().none { it.role == "assistant" }) delay(10) }
        return vm.await { !it.sending && it.bubble()?.options?.size == 2 }
    }

    private fun ChatUiState.bubble() = items.filterIsInstance<ChatItem.Assistant>().lastOrNull { it.options.isNotEmpty() }

    /** The bubble as the app draws it (ChatScreen.AssistantBubble), top to bottom. */
    private fun render(ui: ChatUiState): String {
        val b = ui.bubble()!!
        val out = mutableListOf<String>()
        out += b.text.lines().filter { it.isNotBlank() }
        b.options.forEach { o ->
            out += ""
            out += "  │ ${o.title}"
            o.items.forEach { out += "  │ • $it" }
            out += "  │ ~${o.kcal} kcal · ${o.p}P · ${o.c}C · ${o.g}G"
            o.fit?.let { out += "  │ $it" }
            o.day?.let { out += "  │ $it" }
            out += "  │ " + listOfNotNull("[Registrar]".takeIf { o.canRecord }, "[Reservar]".takeIf { o.canReserve }).joinToString(" ")
        }
        b.trailing.lines().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { out += ""; out += it }
        b.plan?.let { out += ""; out += "[Dia: ${it.eatenKcal} → ${it.projected.kcal} de ${it.ceilingKcal} kcal · P ${it.projected.p}/${it.targets.p}]" }
        b.budget?.let { n -> out += ""; out += "Passa ${n.overKcal} kcal do que sobra."; n.reserved.forEach { (k, l) -> out += "Reservei $k kcal para $l." } }
        ui.actions?.choice?.let { out += "[Pode passar] [Ajustar para caber]" }
        return out.joinToString("\n")
    }

    @Test
    fun comparison_printsTheBubble_decisionFirst_fitAndDayPerOption_noNoteForAChosenOptionThatFits() = runBlocking<Unit> {
        answer = { response() }
        val ui = ask(vm())
        val printed = render(ui)
        println("---- A70 · s39-comparacao-hamburguer as the app shows it ----\n$printed\n----")
        val b = ui.bubble()!!

        assertThat(b.text.lines().first()).startsWith("Vai de Hambúrguer com batata rústica: ~680 kcal · P 39 g, cabe na janela do Jantar.")
        assertThat(b.text).doesNotContain("Opção")
        val (o1, o2) = b.options
        assertThat(o1.title).isEqualTo("Opção 1: Hambúrguer duplo caprichado")
        assertThat(o1.fit).isEqualTo("Passa 130 kcal da janela do Jantar")
        assertThat(o2.fit).isEqualTo("Cabe na janela do Jantar")
        val eaten = Macros(1250, 85, 143, 37)
        assertThat(o1.day).isEqualTo(DayBalance.optionDay(eaten, Macros(o1.kcal, o1.p, o1.c, o1.g), 2200, 150))
        assertThat(o2.day).isEqualTo(DayBalance.optionDay(eaten, Macros(o2.kcal, o2.p, o2.c, o2.g), 2200, 150))
        assertThat(o1.day).isEqualTo("Dia: ~2.110 de 2.200 kcal · P 153 de 150")
        assertThat(o2.day).isEqualTo("Dia: ~1.930 de 2.200 kcal · P 124 de 150")
        // The rest of the reply below the blocks; the option paragraphs went to the blocks.
        assertThat(b.trailing.lines().first()).isEqualTo("1. Asse a batata a 220 °C por 25–30 min, virando na metade.")
        assertThat(b.trailing).contains("Ceia: iogurte natural com whey ~270 kcal · P 26")
        assertThat(b.trailing).doesNotContain("Total:")
        // The day panel and the budget follow the chosen option (o2, the plan's estimate): it fits, no note, no choice.
        assertThat(b.plan!!.projected.kcal).isEqualTo(1250 + 680)
        assertThat(b.budget).isNull()
        assertThat(ui.actions).isNull()
    }

    @Test
    fun chosenOptionOverTheWindow_showsTheNoteAndTheChoice() = runBlocking<Unit> {
        // Both pass: the server's plan_budget of the chosen option is over its window.
        answer = {
            response { r ->
                val o = r.jsonObject
                val actions = (o.getValue("actions") as JsonArray).map { a ->
                    val ao = a.jsonObject
                    if ((ao["type"] as JsonPrimitive).content != "plan") a else JsonObject(
                        ao + ("plan_budget" to JsonObject(mapOf("limit_kcal" to JsonPrimitive(600), "over_kcal" to JsonPrimitive(80), "reserved" to JsonArray(emptyList())))),
                    )
                }
                JsonObject(o + ("actions" to JsonArray(actions)))
            }
        }
        val ui = ask(vm())
        println("---- A70 · chosen option over the window ----\n${render(ui)}\n----")
        assertThat(ui.bubble()!!.budget?.overKcal).isEqualTo(80)
        assertThat(ui.actions?.choice?.overKcal).isEqualTo(80)
    }

    @Test
    fun olderServer_withoutOverKcal_noFitLine_dayLineStays() = runBlocking<Unit> {
        answer = { response { r -> strip(r, "over_kcal") } }
        val b = ask(vm()).bubble()!!
        assertThat(b.options.map { it.fit }).containsExactly(null, null)
        assertThat(b.options.all { it.day != null }).isTrue()
    }

    private fun strip(e: JsonElement, key: String): JsonElement = when (e) {
        is JsonObject -> JsonObject(e.filterKeys { it != key }.mapValues { strip(it.value, key) })
        is JsonArray -> JsonArray(e.map { strip(it, key) })
        else -> e
    }

    private companion object {
        val SLOT_KEYS = setOf("slot", "suggested_slot", "skip_slots", "skip_slot", "question_slot")
    }
}
