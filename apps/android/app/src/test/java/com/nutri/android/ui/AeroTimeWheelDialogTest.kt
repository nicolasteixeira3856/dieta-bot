package com.nutri.android.ui

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.swipe
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.nutri.android.core.designsystem.aero.AeroTheme
import com.nutri.android.core.designsystem.aero.AeroTimeWheelDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A41: the Aero time dialog keeps the TimeWheelDialog behavior (same cases). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class AeroTimeWheelDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollThreeMinutesAndConfirm() {
        var confirmed: Int? = null
        compose.setContent { AeroTheme { Box(Modifier.fillMaxSize()) { AeroTimeWheelDialog("Café da manhã", 450, {}, { confirmed = it }) } } }
        compose.onNodeWithTag("time-wheel-minutes").performTouchInput {
            // 40 dp per row at xhdpi. A slow drag ends at +3 without a velocity fling.
            swipe(Offset(center.x, center.y + 120f), Offset(center.x, center.y - 120f), 1500)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("time-wheel-ok").performClick()
        assertEquals(453, confirmed)
    }

    @Test fun adjustableWheelsWrapAndReturnMidnight() {
        var confirmed: Int? = null
        compose.setContent { AeroTheme { Box(Modifier.fillMaxSize()) { AeroTimeWheelDialog("Jantar", 1439, {}, { confirmed = it }) } } }
        compose.onNodeWithTag("time-wheel-hours").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.onNodeWithTag("time-wheel-minutes").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitForIdle()
        compose.onNodeWithTag("time-wheel-ok").performClick()
        assertEquals(0, confirmed)
    }

    @Test fun announcesTheValueAndSupportsArrowAndVolumeKeys() {
        var confirmed: Int? = null
        compose.setContent { AeroTheme { Box(Modifier.fillMaxSize()) { AeroTimeWheelDialog("Jantar", 21 * 60 + 45, {}, { confirmed = it }) } } }
        val hours = compose.onNodeWithTag("time-wheel-hours")
        val minutes = compose.onNodeWithTag("time-wheel-minutes")
        hours.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "21 horas"))
        minutes.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "45 minutos"))
        minutes.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        minutes.performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        compose.waitForIdle()
        minutes.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "46 minutos"))
        minutes.performKeyInput { keyDown(Key.VolumeUp); keyUp(Key.VolumeUp) }
        compose.waitForIdle()
        minutes.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "45 minutos"))
        compose.onNodeWithTag("time-wheel-ok").performClick()
        assertEquals(21 * 60 + 45, confirmed)
    }

    @Test fun cancelDoesNotConfirmOrChangeTheOriginalTime() {
        var confirmed: Int? = null
        var cancelled = false
        compose.setContent { AeroTheme { Box(Modifier.fillMaxSize()) { AeroTimeWheelDialog("Refeição 1", 450, { cancelled = true }, { confirmed = it }) } } }
        compose.onNodeWithText("Refeição 1").assertIsDisplayed()
        compose.onNodeWithTag("time-wheel-cancel").performClick()
        assertEquals(true, cancelled)
        assertNull(confirmed)
    }
}
