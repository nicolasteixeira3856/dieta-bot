package com.nutri.android.feature.chat

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.nutri.android.core.designsystem.aero.AeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A32: the thread opens at the bottom, asks for older pages at the top, and the photo closes the keyboard. A37: send closes it too. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class ChatThreadTest {
    @get:Rule val compose = createComposeRule()

    private val long = ChatUiState(
        items = listOf(ChatItem.DateSeparator("Hoje, 1 de outubro")) +
            (1L..40L).map { ChatItem.User(it, "msg$it", "12:00") },
        emptyDay = false,
        hasOlder = true,
    )

    private class Keyboard : SoftwareKeyboardController {
        val calls = mutableListOf<String>()
        override fun show() { calls += "show" }
        override fun hide() { calls += "hide" }
    }

    private fun show(ui: ChatUiState, keyboard: Keyboard = Keyboard(), onPhoto: () -> Unit = {}, onCamera: () -> Unit = {}, onLoadOlder: () -> Unit = {}, onSend: () -> Unit = {}) {
        compose.setContent {
            AeroTheme {
                CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                    ChatScreen(
                        ui = ui, onBack = {}, onComposer = {}, onSend = onSend, onRetry = {},
                        onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {},
                        onPhoto = onPhoto, onCamera = onCamera, onLoadOlder = onLoadOlder,
                    )
                }
            }
        }
    }

    @Test fun opensAtTheNewestMessage() {
        var older = 0
        show(long, onLoadOlder = { older++ })
        compose.onNodeWithTag("chat-user-40").assertIsDisplayed()
        compose.onNodeWithTag("chat-user-1").assertDoesNotExist()
        assertEquals(0, older)
    }

    @Test fun reachingTheOldestItemsAsksForAnOlderPage() {
        var older = 0
        show(long, onLoadOlder = { older++ })
        compose.onNodeWithTag("chat-thread").performScrollToIndex(long.items.lastIndex)
        compose.waitForIdle()
        assertEquals(1, older)
    }

    @Test fun loadingOlderShowsTheIndicatorAtTheTop_andAsksNothingMore() {
        var older = 0
        show(long.copy(loadingOlder = true), onLoadOlder = { older++ })
        compose.onNodeWithTag("chat-thread").performScrollToIndex(long.items.size)
        compose.onNodeWithTag("chat-loading-older").assertIsDisplayed()
        assertEquals(0, older)
    }

    /** A34: an answer and its receipt (or Substituir) land together: the thread still follows from the bottom. */
    @Test fun twoNewItemsAtOnce_followFromTheBottom() {
        var ui by mutableStateOf(long)
        compose.setContent {
            AeroTheme {
                ChatScreen(ui = ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {})
            }
        }
        compose.onNodeWithTag("chat-user-40").assertIsDisplayed()
        ui = long.copy(
            items = long.items + ChatItem.Assistant(41, "Juntei ao jantar.", "12:01") +
                ChatItem.ReplacePrompt(41, ReplaceConfirm(SlotRef(4, "Jantar", "20:00", 1200), oldKcal = 380, newKcal = 620)),
        )
        compose.waitForIdle()
        compose.onNodeWithTag("chat-replace-card").assertIsDisplayed()
    }

    @Test fun cameraButtonHidesTheKeyboardBeforeThePhotoSheet() {
        val keyboard = Keyboard()
        show(long, keyboard, onPhoto = { keyboard.calls += "photo" })
        compose.onNodeWithTag("chat-photo").performClick()
        assertEquals(listOf("hide", "photo"), keyboard.calls)
    }

    @Test fun photoChipHidesTheKeyboardBeforeTheCamera() {
        val keyboard = Keyboard()
        show(ChatFixtures.chat0, keyboard, onCamera = { keyboard.calls += "camera" })
        compose.onNodeWithTag("chat-suggestion-photo").performClick()
        assertEquals(listOf("hide", "camera"), keyboard.calls)
    }

    @Test fun sendHidesTheKeyboardBeforeSending() {
        val keyboard = Keyboard()
        show(ChatFixtures.chat0.copy(composer = "arroz e feijão"), keyboard, onSend = { keyboard.calls += "send" })
        compose.onNodeWithTag("chat-send").performClick()
        assertEquals(listOf("hide", "send"), keyboard.calls)
    }
}
