package com.nutri.android.core.designsystem

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ExpressiveButtonGroupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun horizontalGroup_rendersAndSelectsOption() {
        val options = listOf("0%", "%", "100%")
        var selectedIndex = 0

        composeTestRule.setContent {
            NutriTheme {
                ExpressiveButtonGroup(
                    options = options,
                    selected = selectedIndex,
                    onSelect = { selectedIndex = it },
                    stacked = false,
                )
            }
        }

        composeTestRule.onNodeWithText("0%").assertExists()
        composeTestRule.onNodeWithText("%").assertExists()
        composeTestRule.onNodeWithText("100%").assertExists()

        composeTestRule.onNodeWithText("100%").performClick()
        assertThat(selectedIndex).isEqualTo(2)
    }

    @Test
    fun stackedGroup_rendersAndSelectsOption() {
        val options = listOf("Mesmo todos os dias", "Útil / fds", "7 dias")
        var selectedIndex = 0

        composeTestRule.setContent {
            NutriTheme {
                ExpressiveButtonGroup(
                    options = options,
                    selected = selectedIndex,
                    onSelect = { selectedIndex = it },
                    stacked = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Mesmo todos os dias").assertExists()
        composeTestRule.onNodeWithText("Útil / fds").assertExists()
        composeTestRule.onNodeWithText("7 dias").assertExists()

        composeTestRule.onNodeWithText("Útil / fds").performClick()
        assertThat(selectedIndex).isEqualTo(1)
    }
}
