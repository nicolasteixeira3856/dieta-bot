package com.nutri.android.feature.chat

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.nutri.android.core.designsystem.DietaBotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A32: the thread opens at the bottom, asks for older pages at the top, and the photo closes the keyboard. */
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

    private fun show(ui: ChatUiState, keyboard: Keyboard = Keyboard(), onPhoto: () -> Unit = {}, onCamera: () -> Unit = {}, onLoadOlder: () -> Unit = {}) {
        compose.setContent {
            DietaBotTheme {
                CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                    ChatScreen(
                        ui = ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onRecord = { _, _ -> },
                        onSwap = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {}, onAskSkip = {},
                        onSkipConfirm = {}, onSkipCancel = {}, onPhoto = onPhoto, onCamera = onCamera, onLoadOlder = onLoadOlder,
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
}
