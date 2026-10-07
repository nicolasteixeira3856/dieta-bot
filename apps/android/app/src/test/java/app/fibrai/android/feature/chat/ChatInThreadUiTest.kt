package app.fibrai.android.feature.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import app.fibrai.android.core.designsystem.aero.AeroTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A61 parts A and B (ADR-048): the actions scroll with their message; selecting and copying from the screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h640dp-xhdpi")
class ChatInThreadUiTest {
    @get:Rule val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(initial: ChatUiState, onState: ((ChatUiState) -> Unit) -> Unit = {}) = compose.setContent {
        var ui by androidx.compose.runtime.remember { mutableStateOf(initial) }
        onState { ui = it }
        AeroTheme(darkTheme = false) {
            ChatScreen(
                ui = ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {},
                onRecordPlan = { calls += "plan $it" },
                onReserve = { calls += "reserve $it" },
                onLongPress = { calls += "long $it"; ui = ui.copy(selected = ui.selected + it) },
                onSelectTap = { calls += "tap $it" },
                onSelectionClose = { calls += "close"; ui = ui.copy(selected = emptySet()) },
                onCopy = { calls += "copy" },
            )
        }
    }

    @Test fun theActionsSitUnderTheAnswer_andScrollWithIt_theComposerStays() {
        show(ChatFixtures.chatR)
        val bubble = compose.onNodeWithTag("chat-bot-8").fetchSemanticsNode().boundsInRoot
        val plan = compose.onNodeWithTag("chat-record-plan").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val reserve = compose.onNodeWithTag("chat-reserve").fetchSemanticsNode().boundsInRoot
        val composer = compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot
        val dp = compose.density.density
        assertThat((plan.top - bubble.bottom) / dp).isWithin(1f).of(12f)
        assertThat((reserve.top - plan.bottom) / dp).isWithin(1f).of(10f)

        // A short drag (the thread scrolls 50 dp at most here): the stack moves with the answer.
        compose.onNodeWithTag("chat-thread").performTouchInput { swipeDown(startY = top + 40f, endY = top + 200f) }
        compose.waitForIdle()

        val planAfter = compose.onNodeWithTag("chat-record-plan").fetchSemanticsNode().boundsInRoot
        assertThat(planAfter.top).isGreaterThan(plan.top)
        assertThat(compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot).isEqualTo(composer)
    }

    @Test fun forceEstimate_isUnderTheLatestQuestion() {
        show(ChatFixtures.chatQ)
        val question = compose.onAllNodesWithTag("chat-question").fetchSemanticsNodes().maxBy { it.boundsInRoot.bottom }.boundsInRoot
        val force = compose.onNodeWithTag("chat-force-estimate").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        // The question's tag sits inside its 15 dp bottom padding: 12 dp from the bubble.
        assertThat((force.top - question.bottom) / compose.density.density).isWithin(1f).of(12f + 15f)
        assertThat(force.bottom).isLessThan(compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot.top)
    }

    @Test fun longPressShowsTheSelectionBar_backEndsIt_copyReachesTheViewModel() {
        // chatG with its receipt cut, so the user row is on screen in the short window.
        show(ChatFixtures.chatG.copy(items = ChatFixtures.chatG.items.dropLast(1)))
        compose.onNodeWithTag("chat-selection").assertDoesNotExist()
        compose.onNodeWithTag("chat-row-u-1").performTouchInput { longClick() }
        compose.onNodeWithTag("chat-selection").assertIsDisplayed()
        compose.onNodeWithTag("chat-selection-count").assertIsDisplayed()
        compose.onNodeWithTag("chat-copy").performClick()
        compose.onNodeWithTag("chat-selection-close").performClick()
        compose.onNodeWithTag("chat-selection").assertDoesNotExist()
        assertThat(calls).containsExactly("long u-1", "copy", "close").inOrder()
    }

    @Test fun aReceiptIgnoresTheLongPress() {
        show(ChatFixtures.chatG)
        compose.onNodeWithTag("chat-row-r-3").assertDoesNotExist()
        compose.onNodeWithTag("chat-receipt-3").performTouchInput { longClick() }
        assertThat(calls).isEmpty()
    }

    @Test fun anActionTapEndsTheSelectionFirst() {
        show(ChatFixtures.chatR.copy(selected = setOf("u-7")))
        compose.onNodeWithTag("chat-record-plan").performClick()
        assertThat(calls).containsExactly("close", "plan 8").inOrder()
    }

    @Test fun theCopyConfirmation_singularAndPlural() {
        var set: (ChatUiState) -> Unit = {}
        show(ChatFixtures.chatCC) { set = it }
        compose.onNodeWithTag("chat-copied").assertIsDisplayed()
        compose.onNodeWithText("Mensagem copiada").assertIsDisplayed()
        set(ChatFixtures.chatG.copy(copied = 2))
        compose.onNodeWithText("2 mensagens copiadas").assertIsDisplayed()
    }
}
