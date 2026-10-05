package app.fibrai.android.feature.chat

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.designsystem.aero.AeroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A29 chatM: memory chips under the bubble in a fixed order; no memory, no row. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class ChatMemoryChipsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(ui: ChatUiState) = compose.setContent {
        AeroTheme(darkTheme = true) { ChatScreen(ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {}) }
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().top

    @Test
    fun chips_updatedThenPermanentThenDynamic_belowTheBubble() {
        show(ChatFixtures.chatM)
        val bubble = compose.onNodeWithTag("chat-bot-10").getUnclippedBoundsInRoot().bottom
        val order = listOf("updated", "permanent", "dynamic").map { top("chat-memory-$it") }
        assertThat(order.first()).isGreaterThan(bubble)
        assertThat(order).isInStrictOrder()
    }

    @Test
    fun onlyTheUsedKind_isDrawn() {
        val bot = ChatFixtures.chatM.items.filterIsInstance<ChatItem.Assistant>().single()
        show(ChatFixtures.chatM.copy(items = ChatFixtures.chatM.items.map { if (it == bot) bot.copy(memory = MemoryNotice(dynamic = true)) else it }))
        compose.onAllNodesWithTag("chat-memory-updated").assertCountEquals(0)
        compose.onAllNodesWithTag("chat-memory-permanent").assertCountEquals(0)
        compose.onAllNodesWithTag("chat-memory-dynamic").assertCountEquals(1)
    }

    @Test
    fun noMemory_noRow() {
        show(ChatFixtures.chatE)
        compose.onAllNodesWithTag("chat-memory").assertCountEquals(0)
    }

    @Test
    fun receiptWithRoutineApplied_showsMemoriaAtualizada() {
        val receipt = ChatFixtures.chatG.items.filterIsInstance<ChatItem.Receipt>().single()
        show(ChatFixtures.chatG.copy(items = ChatFixtures.chatG.items.map { if (it == receipt) receipt.copy(memoryUpdated = true) else it }))
        compose.onAllNodesWithTag("chat-memory-updated").assertCountEquals(1)
        assertThat(top("chat-memory-updated")).isGreaterThan(compose.onNodeWithTag("chat-receipt-3").getUnclippedBoundsInRoot().bottom)
    }
}
