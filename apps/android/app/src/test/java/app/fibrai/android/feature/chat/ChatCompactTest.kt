package app.fibrai.android.feature.chat

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import java.io.File
import java.io.IOException
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
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A5b: compact=true is live. The clock ticks as on a phone. A38: the oldest block is summarised, the open tail stays raw. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatCompactTest {
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memoryFile = FakeMemoryFile()
    private val memory = FactMemory(memoryFile)
    private val photos = FakePhotoFiles()
    private val clock = TickingClock(Instant.parse("2026-09-25T12:00:00-03:00"))
    private val requests = mutableListOf<ChatIn>()
    private var compactAnswer: suspend () -> ChatOut = { ChatOut(digest = "resumo ${requests.count { it.compact }}", model = "gpt-6-luna") }

    /** True: every turn answers a question only (A30), so the open clarify sequence grows. */
    private var clarify = false

    private val service = ChatService { body ->
        requests += body
        when {
            body.compact -> compactAnswer()
            clarify -> ChatOut(reply = "", intent = "log", question = "Quantos gramas?", model = "gpt-6-luna")
            else -> ChatOut(reply = "ok ${body.text}", model = "gpt-6-luna")
        }
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "compact_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(listOf(MealSlot(name = "Almoço", minutesFromMidnight = 750)))
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    /** One send that completes (user + assistant stored). */
    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = userCount()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { repo.observeMessages().first { m -> m.count { it.role == "user" } == before + 1 } }
    }

    private suspend fun userCount() = repo.observeMessages().first().count { it.role == "user" }

    /** [n] sends: 2n raw messages. */
    private suspend fun exchanges(vm: ChatViewModel, n: Int, from: Int = 0) = repeat(n) { send(vm, "m${from + it}") }

    @Test
    fun under12Raw_noCompact() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6) // the 6th send sees 10 raw
        assertThat(requests.none { it.compact }).isTrue()
        assertThat(repo.digestsToday()).isEmpty()
    }

    @Test
    fun twelveRaw_compactThenTurnWithDigest() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6)
        requests.clear()

        send(vm, "jantar")

        assertThat(requests).hasSize(2)
        val (compact, turn) = requests
        assertThat(compact.compact).isTrue()
        // A38: 12 raw → the oldest 8 are summarised, the newest 4 stay raw.
        assertThat(compact.messages.map { it.text }).containsExactly("m0", "ok m0", "m1", "ok m1", "m2", "ok m2", "m3", "ok m3").inOrder()
        assertThat(turn.compact).isFalse()
        assertThat(turn.text).isEqualTo("jantar")
        assertThat(turn.digests).containsExactly("resumo 1")
        assertThat(turn.messages.map { it.text }).containsExactly("m4", "ok m4", "m5", "ok m5").inOrder()
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo 1")
        val raw = repo.observeMessages().first().filter { it.role in setOf("user", "assistant") }
        assertThat(repo.digestsToday().single().coversUntilId).isEqualTo(raw[7].id)
        // The digest never becomes a bubble or a chat_message.
        assertThat(repo.observeMessages().first().map { it.text }).doesNotContain("resumo 1")
        val ui = vm.uiState.value
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().map { it.text }).doesNotContain("resumo 1")
        assertThat(ui.items.none { it is ChatItem.Failed }).isTrue()
    }

    @Test
    fun threeCompactions_keepTwoDigests_oldestReplaced_nothingSummarisedTwice() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        // Compact on the 7th, 11th and 15th send (12 raw since the last digest, 4 of them kept each time).
        exchanges(vm, 15)

        val compacts = requests.filter { it.compact }
        assertThat(compacts).hasSize(3)
        val summarised = compacts.flatMap { it.messages.map { m -> m.text } }
        assertThat(summarised).containsNoDuplicates()
        assertThat(summarised).hasSize(24)
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo 2", "resumo 3")
        assertThat(requests.last().digests).containsExactly("resumo 2", "resumo 3").inOrder()
        assertThat(requests.last().messages.map { it.text }).containsExactly("m12", "ok m12", "m13", "ok m13").inOrder()
    }

    @Test
    fun openQuestion_sixBack_keepsTheMealAndItsQuestionsRaw() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 3)
        compactAnswer = { ChatOut(digest = "resumo", model = "gpt-6-luna") }
        // The meal, then 2 question-only rounds and their answers: 6 open rows, 12 raw in all on the next send.
        clarify = true
        send(vm, "foto de cheesecake")
        send(vm, "100 gramas")
        send(vm, "sem calda")
        clarify = false
        requests.clear()

        send(vm, "isso")

        val (compact, turn) = requests
        assertThat(compact.compact).isTrue()
        assertThat(compact.messages.map { it.text }).containsExactly("m0", "ok m0", "m1", "ok m1", "m2", "ok m2").inOrder()
        assertThat(turn.messages.first().text).isEqualTo("foto de cheesecake")
        assertThat(turn.messages).hasSize(6)
    }

    @Test
    fun openSequenceOf12_noCompaction() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        clarify = true
        exchanges(vm, 6)
        requests.clear()

        send(vm, "mais uma")

        assertThat(requests.single().compact).isFalse()
        assertThat(requests.single().messages).hasSize(12)
        assertThat(repo.digestsToday()).isEmpty()
    }

    @Test
    fun after18Raw_oneBlockOf12_then30Raw_twoBlocks() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        compactAnswer = { throw IOException("down") }
        exchanges(vm, 9) // 18 raw, every compact failed
        compactAnswer = { ChatOut(digest = "resumo ${requests.count { it.compact }}", model = "gpt-6-luna") }
        requests.clear()

        send(vm, "jantar")

        assertThat(requests.filter { it.compact }.map { it.messages.size }).containsExactly(12)
        assertThat(requests.last().messages).hasSize(6)

        compactAnswer = { throw IOException("down") }
        exchanges(vm, 11, from = 100) // 6 + 2 + 22 = 30 raw after the last digest
        compactAnswer = { ChatOut(digest = "resumo ${requests.count { it.compact }}", model = "gpt-6-luna") }
        requests.clear()
        val telemetry = FakeTelemetry()
        val vm2 = ChatViewModel(repo, service, clock, memory, photos, telemetry)

        send(vm2, "ceia")

        val compacts = requests.filter { it.compact }
        assertThat(compacts.map { it.messages.size }).containsExactly(12, 12).inOrder()
        // The second compact already sees the first digest.
        assertThat(compacts[1].digests).hasSize(2)
        assertThat(requests.last().messages).hasSize(6)
        assertThat(telemetry.params(TelemetryEvents.CHAT_COMPACT).single())
            .isEqualTo(mapOf("blocks" to 2, "kept" to 6, "summarised" to 24))
    }

    @Test
    fun v8DigestWithoutCoversUntilId_keepsTheTimeCut() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 2)
        repo.upsertDigest("resumo antigo") // v8 shape: no coversUntilId
        requests.clear()

        send(vm, "jantar")

        assertThat(requests.single().messages).isEmpty()
        assertThat(requests.single().digests).containsExactly("resumo antigo")
    }

    @Test
    fun midnightDuringTheCompactCall_nothingStored() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6)
        compactAnswer = {
            clock.jumpTo(Instant.parse("2026-09-26T00:00:05-03:00"))
            ChatOut(digest = "resumo de ontem", model = "gpt-6-luna")
        }
        requests.clear()

        send(vm, "ceia")

        assertThat(requests.map { it.compact }).containsExactly(true, false).inOrder()
        assertThat(db.dayDigestDao().getByDate("2026-09-25")).isEmpty()
        assertThat(db.dayDigestDao().getByDate("2026-09-26")).isEmpty()
        assertThat(requests.last().messages).hasSize(12)
    }

    @Test
    fun wipeDuringTheCompactCall_nothingStored() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6)
        compactAnswer = {
            repo.wipeToday()
            ChatOut(digest = "resumo de antes do wipe", model = "gpt-6-luna")
        }
        requests.clear()

        send(vm, "ceia")

        assertThat(requests.map { it.compact }).containsExactly(true, false).inOrder()
        assertThat(repo.digestsToday()).isEmpty()
    }

    @Test
    fun compactFailure_turnStillAnswers_nothingStored_retriesNextSend() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6)
        for (failure in listOf<suspend () -> ChatOut>({ throw IOException("timeout") }, { ChatOut(digest = null) }, { ChatOut(digest = " ") })) {
            compactAnswer = failure
            requests.clear()
            send(vm, "jantar")

            assertThat(requests.map { it.compact }).containsExactly(true, false).inOrder()
            assertThat(requests.last().messages).hasSize(12)
            assertThat(requests.last().digests).isEmpty()
            assertThat(repo.digestsToday()).isEmpty()
            // Room holds the answer before the UI renders it: wait for the render, then check it.
            val ui = withTimeout(5_000) { vm.uiState.first { it.items.filterIsInstance<ChatItem.Assistant>().lastOrNull()?.text == "ok jantar" } }
            assertThat(ui.items.none { it is ChatItem.Failed }).isTrue()
        }

        compactAnswer = { ChatOut(digest = "resumo ok", model = "gpt-6-luna") }
        requests.clear()
        send(vm, "ceia")
        assertThat(requests.first().compact).isTrue()
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo ok")
    }

    @Test
    fun wipe_restartsTheRawCount() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        exchanges(vm, 6)
        repo.wipeToday()
        requests.clear()

        send(vm, "depois do wipe")

        assertThat(requests.single().compact).isFalse()
        assertThat(requests.single().messages).isEmpty()
    }

    private class TickingClock(private var instant: Instant) : InstantClock {
        override fun now(): Instant = instant.also { instant = instant.plusSeconds(1) }

        fun jumpTo(to: Instant) {
            instant = to
        }
    }
}
