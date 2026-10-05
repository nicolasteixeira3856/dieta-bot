package com.nutri.android.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import com.nutri.android.core.designsystem.aero.AeroMacro
import com.nutri.android.core.designsystem.aero.AeroMacroTargetCard
import com.nutri.android.core.designsystem.aero.AeroMealSlotRow
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroNumberField
import com.nutri.android.core.designsystem.aero.AeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A46: an edit of an existing value starts with the cursor at its end. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class AeroEndCursorTest {
    @get:Rule val compose = createComposeRule()

    private fun selection(tag: String): TextRange =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]

    private fun text(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** The finger lands on the first digit (field padding 19 dp = 38 px): the cursor still goes to the end. */
    @Test fun tapOnTheFirstDigitPutsTheCursorAtTheEnd() {
        var value by mutableStateOf("200")
        compose.setContent { AeroTheme { AeroNumberField(value, { value = it }, "g", fieldModifier = Modifier.testTag("f")) } }
        compose.onNodeWithTag("f").performTouchInput { click(Offset(42f, centerY)) }
        compose.waitForIdle()
        assertEquals(TextRange(3), selection("f"))
        compose.onNodeWithTag("f").performTextInput("5")
        compose.waitForIdle()
        assertEquals("2005", value)
    }

    @Test fun requestFocusPutsTheCursorAtTheEnd() {
        val focus = FocusRequester()
        compose.setContent { AeroTheme { AeroNumberField("2560", {}, "kcal", fieldModifier = Modifier.focusRequester(focus).testTag("f")) } }
        compose.runOnIdle { focus.requestFocus() }
        compose.waitForIdle()
        assertEquals(TextRange(4), selection("f"))
    }

    @Test fun imeNextLandsAtTheEndOfTheNextField() {
        val next = FocusRequester()
        compose.setContent {
            AeroTheme {
                Column {
                    AeroNumberField(
                        "27", {}, "anos", fieldModifier = Modifier.testTag("age"),
                        imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { next.requestFocus() }),
                    )
                    AeroNumberField("164", {}, "cm", fieldModifier = Modifier.focusRequester(next).testTag("height"))
                }
            }
        }
        compose.onNodeWithTag("age").performClick()
        compose.onNodeWithTag("age").performImeAction()
        compose.waitForIdle()
        assertEquals(TextRange(3), selection("height"))
    }

    @Test fun adjustButtonOfAMacroPutsTheCursorAtTheEnd() {
        val focus = FocusRequester()
        var grams by mutableStateOf("200")
        compose.setContent {
            AeroTheme {
                AeroMacroTargetCard(
                    "Carboidrato", "4 kcal/g", AeroMacro.Carbs, grams, { grams = it },
                    onAdjust = { focus.requestFocus() },
                    fieldModifier = Modifier.focusRequester(focus).testTag("carb"),
                )
            }
        }
        compose.onNodeWithContentDescription("Ajustar Carboidrato").performClick()
        compose.waitForIdle()
        assertEquals(TextRange(3), selection("carb"))
        compose.onNodeWithTag("carb").performTextInput("5")
        compose.waitForIdle()
        assertEquals("2005", grams)
    }

    @Test fun externalChangeMovesTheCursorToTheEnd() {
        var value by mutableStateOf("1800")
        compose.setContent { AeroTheme { AeroNumberField(value, { value = it }, "kcal", fieldModifier = Modifier.testTag("f")) } }
        compose.onNodeWithTag("f").performClick()
        compose.runOnIdle { value = "21500" }
        compose.waitForIdle()
        assertEquals("21500", text("f"))
        assertEquals(TextRange(5), selection("f"))
    }

    @Test fun laterMovesInsideTheFocusedFieldAreKept() {
        var value by mutableStateOf("2000")
        compose.setContent { AeroTheme { AeroNumberField(value, { value = it }, "kcal", fieldModifier = Modifier.testTag("f")) } }
        compose.onNodeWithTag("f").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithTag("f").performTextInputSelection(TextRange(1))
        compose.waitForIdle()
        assertEquals(TextRange(1), selection("f"))
        compose.onNodeWithTag("f").performTextInput("5")
        compose.waitForIdle()
        assertEquals("25000", value)
    }

    @Test fun mealNameEditStartsAtTheEnd() {
        var name by mutableStateOf("Jantar")
        compose.setContent {
            AeroTheme {
                AeroMealSlotRow(
                    index = 0, name = name, time = "20:00", icon = AeroIconName.BowlFood,
                    suggestions = listOf("Jantar", "Ceia"), onName = { name = it }, onPickTime = {},
                )
            }
        }
        // Padding 15 dp + icon 20 dp + gap 10 dp: the name starts at 90 px; tap on its first letter.
        compose.onNodeWithTag("o3-name-0").performTouchInput { click(Offset(96f, centerY)) }
        compose.waitForIdle()
        assertEquals(TextRange(6), selection("o3-name-0"))
        compose.onNodeWithTag("o3-name-0").performTextInput("x")
        compose.waitForIdle()
        assertEquals("Jantarx", name)
    }
}
