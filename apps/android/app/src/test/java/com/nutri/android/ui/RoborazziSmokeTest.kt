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
import com.nutri.android.feature.onboarding.CeilingScreen
import com.nutri.android.feature.onboarding.OnboardingUiState
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

    /** A31: O1 before the profile, mode and ceiling disabled (gold o1e). */
    @Test @Config(qualifiers = "w390dp-h925dp-xhdpi") fun o1e_dark() = o1e(dark = true)
    @Test @Config(qualifiers = "w390dp-h925dp-xhdpi") fun o1e_light() = o1e(dark = false)

    private fun o1e(dark: Boolean) {
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                CeilingScreen(OnboardingUiState(sex = "male"), {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/${if (dark) "dark" else "light"}/o1e.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)),
        )
    }

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

    /** A30 (ST7): the estimate with no question bubble, time under it (chatE). */
    @Test
    fun chatE_dark() = chat(dark = true, ChatFixtures.chatE, "chatE")

    @Test
    fun chatE_light() = chat(dark = false, ChatFixtures.chatE, "chatE")

    /** A19: photo attached in the composer, not sent (chatA). */
    @Test
    fun chatA_dark() = chat(dark = true, ChatFixtures.chatA, "chatA")

    @Test
    fun chatA_light() = chat(dark = false, ChatFixtures.chatA, "chatA")

    /** A29: plan with the projected day and Registrar assim (chatR). */
    @Test
    fun chatR_dark() = chat(dark = true, ChatFixtures.chatR, "chatR")

    @Test
    fun chatR_light() = chat(dark = false, ChatFixtures.chatR, "chatR")

    /** A29: Memória atualizada + origin chips (chatM). */
    @Test
    fun chatM_dark() = chat(dark = true, ChatFixtures.chatM, "chatM")

    @Test
    fun chatM_light() = chat(dark = false, ChatFixtures.chatM, "chatM")

    /** A29: routine suggestion card on an empty day (chatS). */
    @Test
    fun chatS_dark() = chat(dark = true, ChatFixtures.chatS, "chatS")

    @Test
    fun chatS_light() = chat(dark = false, ChatFixtures.chatS, "chatS")

    /** A30: second question before the estimate, Forçar estimativa (chatQ). */
    @Test
    fun chatQ_dark() = chat(dark = true, ChatFixtures.chatQ, "chatQ")

    @Test
    fun chatQ_light() = chat(dark = false, ChatFixtures.chatQ, "chatQ")

    /** A34 (ST9): automatic record receipts, Substituir inline, undone replacement. */
    @Test
    fun chatG_dark() = chat(dark = true, ChatFixtures.chatG, "chatG")

    @Test
    fun chatG_light() = chat(dark = false, ChatFixtures.chatG, "chatG")

    @Test
    fun chatF_dark() = chat(dark = true, ChatFixtures.chatF, "chatF")

    @Test
    fun chatF_light() = chat(dark = false, ChatFixtures.chatF, "chatF")

    @Test
    fun chatU_dark() = chat(dark = true, ChatFixtures.chatU, "chatU")

    @Test
    fun chatU_light() = chat(dark = false, ChatFixtures.chatU, "chatU")

    @Test
    fun chatD_dark() = chat(dark = true, ChatFixtures.chatD, "chatD")

    @Test
    fun chatD_light() = chat(dark = false, ChatFixtures.chatD, "chatD")

    private fun chat(dark: Boolean, ui: com.nutri.android.feature.chat.ChatUiState, name: String) {
        com.nutri.android.feature.chat.PhotoPreviews.load(ChatFixtures.CHAT_A_PHOTO)
        com.nutri.android.feature.chat.PhotoPreviews.load(ChatFixtures.CHAT_F_PHOTO)
        composeTestRule.setContent {
            DietaBotTheme(darkTheme = dark) {
                ChatScreen(ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {})
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

    /** A24: full-screen meal editor replacing the former slots sheet. */
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
