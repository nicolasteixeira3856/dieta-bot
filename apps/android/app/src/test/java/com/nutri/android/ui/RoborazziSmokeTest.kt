package com.nutri.android.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.nutri.android.core.designsystem.DietaBotTheme
import com.nutri.android.feature.chat.ChatFixtures
import com.nutri.android.feature.chat.ChatScreen
import com.nutri.android.feature.splash.SplashScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: the real one starts PushSync on a Room flow that outlives each test (see StitchGoldTest).
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class RoborazziSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splash_dark() {
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = true) {
                SplashScreen(capture = true, onDone = {})
            }
        }
        val target = File("src/test/snapshots/dark/splash.png")
        composeTestRule.onRoot().captureRoboImage(
            filePath = target.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }

    @Test
    fun splash_light() {
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = false) {
                SplashScreen(capture = true, onDone = {})
            }
        }
        val target = File("src/test/snapshots/light/splash.png")
        composeTestRule.onRoot().captureRoboImage(
            filePath = target.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }

    /** A18: replace confirmation (chatP layout, ADR-017 copy). */
    @Test
    fun chatReplace_dark() = chatReplace(dark = true)

    @Test
    fun chatReplace_light() = chatReplace(dark = false)

    private fun chatReplace(dark: Boolean) {
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                ChatScreen(ChatFixtures.chatReplace, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {})
            }
        }
        val target = File("src/test/snapshots/${if (dark) "dark" else "light"}/chatReplace.png")
        composeTestRule.onRoot().captureRoboImage(
            filePath = target.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }
}
