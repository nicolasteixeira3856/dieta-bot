package com.nutri.android.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import com.nutri.android.core.designsystem.TimeWheelDialog
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.nutri.android.core.designsystem.DietaBotTheme
import com.nutri.android.feature.chat.ChatFixtures
import com.nutri.android.feature.chat.ChatScreen
import com.nutri.android.feature.config.ConfigActions
import com.nutri.android.feature.config.ConfigDraft
import com.nutri.android.feature.config.ConfigEditor
import com.nutri.android.feature.config.ConfigScreen
import com.nutri.android.feature.config.ConfigSlotRow
import com.nutri.android.feature.config.ConfigUiState
import com.nutri.android.feature.home.HomeFixtures
import com.nutri.android.feature.home.HomePanelMapper
import com.nutri.android.feature.home.HomePanelScreen
import com.nutri.android.feature.onboarding.SlotDraft
import com.nutri.android.feature.splash.SplashScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: the real one starts PushSync on a Room flow that outlives each test (see StitchGoldTest).
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class RoborazziSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test fun o3t_dark() = timeWheel(dark = true)
    @Test fun o3t_light() = timeWheel(dark = false)

    private fun timeWheel(dark: Boolean) {
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) { TimeWheelDialog("Café da manhã", 450, {}, {}) }
        }
        composeTestRule.onNodeWithTag("time-wheel-dialog").captureRoboImage(
            filePath = "src/test/snapshots/${if (dark) "dark" else "light"}/o3t.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)),
        )
    }

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

    /** A19: follow-up question in its own bubble (chatE). */
    @Test
    fun chatQuestion_dark() = chat(dark = true, ChatFixtures.chatE, "chatQuestion")

    @Test
    fun chatQuestion_light() = chat(dark = false, ChatFixtures.chatE, "chatQuestion")

    /** A19: photo attached in the composer, not sent (chatA). */
    @Test
    fun chatA_dark() = chat(dark = true, ChatFixtures.chatA, "chatA")

    @Test
    fun chatA_light() = chat(dark = false, ChatFixtures.chatA, "chatA")

    private fun chat(dark: Boolean, ui: com.nutri.android.feature.chat.ChatUiState, name: String) {
        com.nutri.android.feature.chat.PhotoPreviews.load(ChatFixtures.CHAT_A_PHOTO)
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                ChatScreen(ui, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {})
            }
        }
        val target = File("src/test/snapshots/${if (dark) "dark" else "light"}/$name.png")
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

    /** A20: Salvar/Cancelar as one SheetActions pair; Treino de hoje has no subtitle. */
    @Test
    fun cfgWorkout_dark() = cfgSheet(dark = true, ConfigEditor.WORKOUT, "cfgWorkout")

    @Test
    fun cfgWorkout_light() = cfgSheet(dark = false, ConfigEditor.WORKOUT, "cfgWorkout")

    /** A22 homeW: Treino de hoje sheet over the Home, live credit line. */
    @Test
    fun homeW_dark() = homeW(dark = true)

    @Test
    fun homeW_light() = homeW(dark = false)

    private fun homeW(dark: Boolean) {
        val ui = HomePanelMapper.map(HomeFixtures.home1Workout, LocalDate.parse("2026-09-25"), workoutDraft = "350")
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                HomePanelScreen(ui, {}, {}, {})
            }
        }
        val target = File("src/test/snapshots/${if (dark) "dark" else "light"}/homeW.png")
        composeTestRule.onRoot().captureRoboImage(
            filePath = target.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }

    /** A20: slots sheet copy. */
    @Test
    fun cfgSlots_dark() = cfgSheet(dark = true, ConfigEditor.SLOTS, "cfgSlots")

    @Test
    fun cfgSlots_light() = cfgSheet(dark = false, ConfigEditor.SLOTS, "cfgSlots")

    private fun cfgSheet(dark: Boolean, editor: ConfigEditor, name: String) {
        val slots = listOf(SlotDraft(1, "Café da manhã", 450), SlotDraft(2, "Almoço", 750), SlotDraft(3, "Jantar", 1200))
        val ui = ConfigUiState(
            loaded = true,
            ceilingValue = "2000 kcal",
            ceilingDetail = "Mesmo valor todos os dias",
            eatBackValue = "0% (desativado)",
            macrosValue = "150g · 200g · 67g",
            slots = slots.map { ConfigSlotRow(it.id, it.name, "%02d:%02d".format(it.minutes / 60, it.minutes % 60)) },
            workoutValue = "Nenhum informado",
            editor = editor,
            draft = ConfigDraft(slots = slots, workoutField = "350"),
        )
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                ConfigScreen(ui, ConfigActions())
            }
        }
        val target = File("src/test/snapshots/${if (dark) "dark" else "light"}/$name.png")
        composeTestRule.onRoot().captureRoboImage(
            filePath = target.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }
}
