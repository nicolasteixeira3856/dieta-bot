package app.fibrai.android.ui

import android.app.Application
import android.graphics.Point
import android.graphics.Rect
import android.view.ScrollCaptureTarget
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroTheme
import app.fibrai.android.feature.chat.ChatFixtures
import app.fibrai.android.feature.chat.ChatScreen
import app.fibrai.android.feature.config.ConfigActions
import app.fibrai.android.feature.config.ConfigMapper
import app.fibrai.android.feature.config.ConfigScreen
import app.fibrai.android.feature.home.HomeFixtures
import app.fibrai.android.feature.home.HomePanelMapper
import app.fibrai.android.feature.home.HomePanelScreen
import app.fibrai.android.feature.onboarding.CeilingScreen
import app.fibrai.android.feature.onboarding.EatScreen
import app.fibrai.android.feature.onboarding.MacrosScreen
import app.fibrai.android.feature.onboarding.OnboardingSlotsScreen
import com.google.common.truth.Truth.assertWithMessage
import java.time.LocalDate
import java.util.function.Consumer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A61 part C (ADR-048 decision 3): every scrollable product screen offers the system scrolling screenshot. The
 * framework search (`View.dispatchScrollCaptureSearch`, the call SystemUI makes for "Capturar mais") must find one
 * target per screen, inside the window, whose content scrolls. Window shorter than the content: 390 x 640 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h640dp-xhdpi")
class ScrollCaptureTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun home() = check("home") { HomePanelScreen(HomePanelMapper.map(HomeFixtures.home1Workout, DAY), {}, {}, {}) }

    @Test fun chat() = check("chat") { Chat(ChatFixtures.chatR) }

    @Test fun config() = check("cfg") { ConfigScreen(ConfigMapper.map(GoldTest.CFG_DAY, DAY), ConfigActions()) }

    @Test fun onboardingCeiling() = check("o1") { CeilingScreen(GoldTest.GOLD_STATE, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}) }

    @Test fun onboardingEat() = check("o2") { EatScreen(GoldTest.GOLD_STATE, {}, {}, {}, {}) }

    @Test fun onboardingSlots() = check("o3") { OnboardingSlotsScreen(GoldTest.GOLD_STATE, {}, { _, _ -> }, { _, _ -> }, {}, {}) }

    @Test fun onboardingMacros() = check("o4") { MacrosScreen(GoldTest.GOLD_STATE, {}, {}, {}, {}, {}) }

    @Composable
    private fun Chat(ui: app.fibrai.android.feature.chat.ChatUiState) =
        ChatScreen(ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {})

    private fun check(name: String, screen: @Composable () -> Unit) {
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            AeroTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(Aero.colors.bgPage)) { screen() }
            }
        }
        compose.waitForIdle()
        val root = view.rootView
        val window = Rect(0, 0, root.width, root.height)
        val targets = mutableListOf<ScrollCaptureTarget>()
        compose.runOnIdle { root.dispatchScrollCaptureSearch(window, Point(0, 0), Consumer { targets += it }) }
        println("SCROLL_CAPTURE $name targets=${targets.size} " + targets.joinToString { "${it.localVisibleRect} hint=${it.hint}" })
        assertWithMessage("$name: one scroll-capture target").that(targets).hasSize(1)
        val bounds = targets.single().localVisibleRect
        assertWithMessage("$name: target inside the window").that(window.contains(bounds)).isTrue()
        assertWithMessage("$name: target is not empty").that(bounds.height()).isGreaterThan(0)
    }

    private companion object {
        val DAY: LocalDate = LocalDate.parse("2026-09-25")
    }
}
