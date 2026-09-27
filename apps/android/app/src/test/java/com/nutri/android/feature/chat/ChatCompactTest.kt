package com.nutri.android.feature.chat

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.NutriDatabase
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatOut
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

/** A5b: compact=true is live. The clock ticks so digests cut the raw block by time, as on a phone. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatCompactTest {
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val clock = TickingClock(Instant.parse("2026-09-25T12:00:00-03:00"))
    private val requests = mutableListOf<ChatIn>()
    private var compactAnswer: () -> ChatOut = { ChatOut(digest = "resumo ${requests.count { it.compact }}", model = "gpt-6-luna") }

    private val service = ChatService { body ->
        requests += body
        if (body.compact) compactAnswer() else ChatOut(reply = "ok ${body.text}", model = "gpt-6-luna")
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, NutriDatabase::class.java).allowMainThreadQueries().build()
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
        val vm = ChatViewModel(repo, service, clock)
        exchanges(vm, 6) // the 6th send sees 10 raw
        assertThat(requests.none { it.compact }).isTrue()
        assertThat(repo.digestsToday()).isEmpty()
    }

    @Test
    fun twelveRaw_compactThenTurnWithDigest() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock)
        exchanges(vm, 6)
        requests.clear()

        send(vm, "jantar")

        assertThat(requests).hasSize(2)
        val (compact, turn) = requests
        assertThat(compact.compact).isTrue()
        assertThat(compact.messages).hasSize(12)
        assertThat(turn.compact).isFalse()
        assertThat(turn.text).isEqualTo("jantar")
        assertThat(turn.digests).containsExactly("resumo 1")
        assertThat(turn.messages).isEmpty()
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo 1")
        // The digest never becomes a bubble or a chat_message.
        assertThat(repo.observeMessages().first().map { it.text }).doesNotContain("resumo 1")
        val ui = vm.uiState.value
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().map { it.text }).doesNotContain("resumo 1")
        assertThat(ui.items.none { it is ChatItem.Failed }).isTrue()
    }

    @Test
    fun threeCompactions_keepTwoDigests_oldestReplaced() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock)
        // Compact on the 7th, 13th and 19th send (12 raw since the last digest each time).
        exchanges(vm, 19)

        assertThat(requests.count { it.compact }).isEqualTo(3)
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo 2", "resumo 3")
        assertThat(requests.last().digests).containsExactly("resumo 2", "resumo 3").inOrder()
    }

    @Test
    fun compactFailure_turnStillAnswers_nothingStored_retriesNextSend() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock)
        exchanges(vm, 6)
        for (failure in listOf<() -> ChatOut>({ throw IOException("timeout") }, { ChatOut(digest = null) }, { ChatOut(digest = " ") })) {
            compactAnswer = failure
            requests.clear()
            send(vm, "jantar")

            assertThat(requests.map { it.compact }).containsExactly(true, false).inOrder()
            assertThat(requests.last().messages).hasSize(12)
            assertThat(requests.last().digests).isEmpty()
            assertThat(repo.digestsToday()).isEmpty()
            assertThat(vm.uiState.value.items.none { it is ChatItem.Failed }).isTrue()
            assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().last().text).isEqualTo("ok jantar")
        }

        compactAnswer = { ChatOut(digest = "resumo ok", model = "gpt-6-luna") }
        requests.clear()
        send(vm, "ceia")
        assertThat(requests.first().compact).isTrue()
        assertThat(repo.digestsToday().map { it.text }).containsExactly("resumo ok")
    }

    @Test
    fun wipe_restartsTheRawCount() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock)
        exchanges(vm, 6)
        repo.wipeToday()
        requests.clear()

        send(vm, "depois do wipe")

        assertThat(requests.single().compact).isFalse()
        assertThat(requests.single().messages).isEmpty()
    }

    private class TickingClock(private var instant: Instant) : InstantClock {
        override fun now(): Instant = instant.also { instant = instant.plusSeconds(1) }
    }
}
