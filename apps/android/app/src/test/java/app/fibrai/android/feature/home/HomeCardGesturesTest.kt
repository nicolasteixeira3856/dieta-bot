package app.fibrai.android.feature.home

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.fibrai.android.core.designsystem.aero.AeroTheme
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A52 (ADR-040): an empty card opens the Chat on tap and asks to skip on long press. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class HomeCardGesturesTest {
    @get:Rule val compose = createComposeRule()

    private val skips = mutableListOf<Long>()
    private var chats = 0

    private fun show(day: app.fibrai.android.core.database.DaySnapshot) = compose.setContent {
        AeroTheme(darkTheme = true) {
            HomePanelScreen(
                HomePanelMapper.map(day, LocalDate.parse("2026-09-25")),
                onSkip = { skips += it },
                onConfig = {},
                onChat = { chats++ },
            )
        }
    }

    private fun emptyCard() = compose.onNodeWithTag("home-slot-1").onChildren().filterToOne(
        SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick),
    )

    @Test fun tap_opensChat_writesNoSkip() {
        show(HomeFixtures.home0)
        emptyCard().performClick()
        assertThat(chats).isEqualTo(1)
        assertThat(skips).isEmpty()
        compose.onNodeWithTag("home-skip-dialog").assertDoesNotExist()
    }

    @Test fun longPress_asksToSkip_confirmWritesOnce() {
        show(HomeFixtures.home0)
        emptyCard().performTouchInput { longClick() }
        compose.onNodeWithText("Pular Café da manhã?").assertExists()
        assertThat(chats).isEqualTo(0)
        compose.onNodeWithTag("home-skip-confirm").performClick()
        assertThat(skips).containsExactly(1L)
        compose.onNodeWithTag("home-skip-dialog").assertDoesNotExist()
    }

    @Test fun longPress_cancel_writesNothing() {
        show(HomeFixtures.home0)
        emptyCard().performTouchInput { longClick() }
        compose.onNodeWithTag("home-skip-cancel").performClick()
        assertThat(skips).isEmpty()
        compose.onNodeWithTag("home-skip-dialog").assertDoesNotExist()
    }

    @Test fun emptyCard_hasAccessibilityLabels() {
        show(HomeFixtures.home0)
        emptyCard()
            .assert(SemanticsMatcher("click label Registrar") { it.config[SemanticsActions.OnClick].label == "Registrar" })
            .assert(SemanticsMatcher("long click label Pular") { it.config[SemanticsActions.OnLongClick].label == "Pular" })
    }

    @Test fun loggedAndSkippedCards_haveNoGesture() {
        show(HomeFixtures.home1Workout)
        val gestures = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).fetchSemanticsNodes()
        // home1: only the next empty slot (Jantar) is actionable; logged and skipped cards are not.
        assertThat(gestures).hasSize(1)
        assertThat(compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).onFirst().fetchSemanticsNode().config[SemanticsActions.OnLongClick].label).isEqualTo("Pular")
    }
}
