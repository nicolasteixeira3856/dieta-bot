package app.fibrai.android.feature.chat

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.designsystem.aero.AeroTheme
import app.fibrai.android.domain.ReceiptAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A34: the record controls of the Chat reach the ViewModel with the right ids. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class ChatRecordUiTest {
    @get:Rule val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(ui: ChatUiState) = compose.setContent {
        AeroTheme(darkTheme = true) {
            ChatScreen(
                ui = ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {},
                onRegister = { calls += "register $it" },
                onReplaceConfirm = { calls += "replace $it" },
                onReplaceElsewhere = { calls += "elsewhere $it" },
                onReceiptAction = { id, action -> calls += "$action $id" },
                onMoveConfirm = { calls += "move-confirm" },
                onMoveElsewhere = { calls += "move-elsewhere" },
                onAdditionConfirm = { calls += "add $it" },
                onAdditionElsewhere = { calls += "add-elsewhere $it" },
                onRevisionConfirm = { calls += "revise $it" },
                onRevisionCancel = { calls += "revise-cancel $it" },
            )
        }
    }

    @Test fun registrar_onlyPillAboveTheComposer() {
        show(ChatFixtures.chatE)
        compose.onNodeWithTag("chat-register").assertIsDisplayed().performClick()
        compose.onNodeWithText("Gravar", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Pular").assertDoesNotExist()
        assertThat(calls).containsExactly("register 2")
    }

    @Test fun receiptButtons_carryTheReceiptId_inTheGoldOrder() {
        show(ChatFixtures.chatG)
        compose.onNodeWithText("Excluir").assertIsDisplayed()
        compose.onNodeWithTag("chat-receipt-delete").performClick()
        compose.onNodeWithTag("chat-receipt-move").performClick()
        compose.onNodeWithTag("chat-receipt-edit").performClick()
        compose.onNodeWithTag("chat-receipt-undo").assertDoesNotExist()
        assertThat(calls).containsExactly("${ReceiptAction.DELETE} 3", "${ReceiptAction.MOVE} 3", "${ReceiptAction.EDIT} 3").inOrder()
        val delete = compose.onNodeWithTag("chat-receipt-delete").fetchSemanticsNode().boundsInRoot
        val move = compose.onNodeWithTag("chat-receipt-move").fetchSemanticsNode().boundsInRoot
        val receipt = compose.onNodeWithTag("chat-receipt-3").fetchSemanticsNode().boundsInRoot
        assertThat(move.top).isGreaterThan(delete.bottom)
        assertThat(delete.height / compose.density.density).isWithin(0.5f).of(44f)
        assertThat(delete.width).isWithin(1f).of(receipt.width)
    }

    @Test fun photoReceipt_hasNoEditar() {
        show(ChatFixtures.chatF)
        compose.onNodeWithTag("chat-receipt-edit").assertDoesNotExist()
        compose.onNodeWithTag("chat-receipt-move").assertIsDisplayed()
    }

    @Test fun inlineReplace_substituirAndOutraRefeicao() {
        show(ChatFixtures.chatU)
        compose.onNodeWithText("Substituir Jantar?").assertIsDisplayed()
        compose.onNodeWithText("Jantar tem 380 kcal. Fica com 620 kcal.").assertIsDisplayed()
        compose.onNodeWithTag("chat-replace-confirm").performClick()
        compose.onNodeWithTag("chat-replace-elsewhere").performClick()
        assertThat(calls).containsExactly("replace 21", "elsewhere 21").inOrder()
    }

    @Test fun undoneReceipt_isMarked_restoredOneHasTheActions() {
        show(ChatFixtures.chatD)
        compose.onNodeWithText("Desfeito").assertIsDisplayed()
        compose.onNodeWithText("380 → 620 kcal").assertIsDisplayed()
        compose.onNodeWithText("Restaurado em ", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("chat-receipt-delete").performClick()
        assertThat(calls).containsExactly("${ReceiptAction.DELETE} 23")
    }

    @Test fun moveConfirm_belowTheReceipt() {
        val receipt = (ChatFixtures.chatG.items.last() as ChatItem.Receipt)
            .copy(moveConfirm = ReplaceConfirm(SlotRef(4, "Jantar", "20:00", 1200), oldKcal = 500, newKcal = 380))
        show(ChatFixtures.chatG.copy(items = ChatFixtures.chatG.items.dropLast(1) + receipt))
        compose.onNodeWithText("Jantar tem 500 kcal. Fica com 380 kcal.").assertIsDisplayed()
        compose.onNodeWithTag("chat-move-confirm").performClick()
        compose.onNodeWithTag("chat-move-elsewhere").performClick()
        assertThat(calls).containsExactly("move-confirm", "move-elsewhere").inOrder()
    }

    @Test fun notRecorded_markBelowTheAnswer() {
        val bot = (ChatFixtures.chatU.items[2] as ChatItem.Assistant).copy(notRecorded = true)
        show(ChatFixtures.chatU.copy(items = ChatFixtures.chatU.items.take(2) + bot))
        compose.onNodeWithTag("chat-not-recorded").assertIsDisplayed()
        compose.onNodeWithText("Não registrado").assertIsDisplayed()
    }

    /** A47 (chatI): labelled amounts, the + numbers of the added food, and both buttons with the answer id. */
    @Test fun addition_labelsAndButtons() {
        show(ChatFixtures.chatI)
        compose.onNodeWithText("Já registrado no Jantar").assertIsDisplayed()
        compose.onNodeWithText("Total do Jantar").assertIsDisplayed()
        compose.onNodeWithText("620 kcal").assertIsDisplayed()
        compose.onNodeWithText("+240").assertIsDisplayed()
        compose.onNodeWithText("+6g P").assertIsDisplayed()
        compose.onNodeWithText("Adicionar ao Jantar?").assertIsDisplayed()
        // No unlabeled total and no prose in the bubble.
        compose.onNodeWithText("~", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("chat-addition-confirm").performClick()
        compose.onNodeWithTag("chat-addition-elsewhere").performClick()
        assertThat(calls).containsExactly("add 31", "add-elsewhere 31").inOrder()
    }

    /** A47 (chatIC): Antes / Novo total, Atualizar | Cancelar, and no destination action. */
    @Test fun revision_buttons_noOtherMeal() {
        show(ChatFixtures.chatIC)
        compose.onNodeWithText("Atualizar Jantar?").assertIsDisplayed()
        compose.onNodeWithText("Antes").assertIsDisplayed()
        compose.onNodeWithText("Escolher outra refeição").assertDoesNotExist()
        compose.onNodeWithText("Outra refeição").assertDoesNotExist()
        compose.onNodeWithTag("chat-revision-confirm").performClick()
        compose.onNodeWithTag("chat-revision-cancel").performClick()
        assertThat(calls).containsExactly("revise 41", "revise-cancel 41").inOrder()
    }

    /** A47 (chatTI): the picker says it directs only the added food and keeps the source. */
    @Test fun additionPicker_namesTheAddedFoodAndTheSource() {
        show(ChatFixtures.chatTI)
        compose.onNodeWithText("Escolher outra refeição").assertIsDisplayed()
        compose.onNodeWithText("Só o acréscimo vai para a refeição escolhida. O Jantar fica como está.").assertIsDisplayed()
        compose.onNodeWithTag("chat-sheet-addition").assertIsDisplayed()
        compose.onNodeWithText("(atual)", substring = true).assertDoesNotExist()
    }
}
