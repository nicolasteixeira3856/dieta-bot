package app.fibrai.android.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroButtonPrimary
import app.fibrai.android.core.designsystem.aero.AeroChatBubble
import app.fibrai.android.core.designsystem.aero.AeroChipLog
import app.fibrai.android.core.designsystem.aero.AeroChipTone
import app.fibrai.android.core.designsystem.aero.AeroComposer
import app.fibrai.android.core.designsystem.aero.AeroIconButton
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroMacro
import app.fibrai.android.core.designsystem.aero.AeroMacroRow
import app.fibrai.android.core.designsystem.aero.AeroMealCard
import app.fibrai.android.core.designsystem.aero.AeroMealState
import app.fibrai.android.core.designsystem.aero.AeroNodeState
import app.fibrai.android.core.designsystem.aero.AeroOptionCard
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroProgressBar
import app.fibrai.android.core.designsystem.aero.AeroTheme
import app.fibrai.android.core.designsystem.aero.AeroTimelineNode
import app.fibrai.android.core.designsystem.aero.aeroGlass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A39: Roborazzi baselines of every Aero component, Light and Dark, on the page gradient. They are compared by eye
 * with the Figma `Componentes` page (same tokens); they are regression baselines, not golds.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class AeroComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            AeroTheme(darkTheme = dark) {
                AeroPage(Modifier.width(390.dp).testTag("aero-shot")) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
                }
            }
        }
        compose.onNodeWithTag("aero-shot").captureRoboImage(
            filePath = "src/test/snapshots/${if (dark) "dark" else "light"}/aero/$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)),
        )
    }

    @Test fun buttonPrimary_light() = shot("ButtonPrimary", false) { ButtonPrimary() }
    @Test fun buttonPrimary_dark() = shot("ButtonPrimary", true) { ButtonPrimary() }

    @Composable private fun ButtonPrimary() {
        AeroButtonPrimary("Continuar", {}, icon = AeroIconName.ArrowRight)
        AeroButtonPrimary("Continuar", {}, enabled = false)
    }

    @Test fun iconButton_light() = shot("IconButton", false) { IconButtons() }
    @Test fun iconButton_dark() = shot("IconButton", true) { IconButtons() }

    @Composable private fun IconButtons() {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AeroIconButton(AeroIconName.Gear, {}, "Config")
            AeroIconButton(AeroIconName.Camera, {}, "Foto")
            AeroIconButton(AeroIconName.Barbell, {}, "Treino")
            AeroIconButton(AeroIconName.Info, {}, "Info")
        }
    }

    @Test fun chipLog_light() = shot("ChipLog", false) { Chips() }
    @Test fun chipLog_dark() = shot("ChipLog", true) { Chips() }

    @Composable private fun Chips() {
        AeroChipLog("520 kcal · 28P · 52C · 22G")
        AeroChipLog("Meta 2.000 kcal", tone = AeroChipTone.Neutral)
        AeroChipLog("Meta excedida (+120 kcal)", tone = AeroChipTone.Bad)
    }

    @Test fun progressBar_light() = shot("ProgressBar", false) { AeroProgressBar(0.5f) }
    @Test fun progressBar_dark() = shot("ProgressBar", true) { AeroProgressBar(0.5f) }

    @Test fun macroRow_light() = shot("MacroRow", false) { Macros() }
    @Test fun macroRow_dark() = shot("MacroRow", true) { Macros() }

    @Composable private fun Macros() {
        AeroMacroRow(AeroMacro.Protein, "P Proteína", "76", "150 g", 76 / 150f, over = false)
        AeroMacroRow(AeroMacro.Protein, "P Proteína", "168", "150 g", 1f, over = true)
        AeroMacroRow(AeroMacro.Carbs, "C Carboidratos", "134", "200 g", 134 / 200f, over = false)
        AeroMacroRow(AeroMacro.Fat, "G Gorduras", "40", "67 g", 40 / 67f, over = false)
        AeroMacroRow(AeroMacro.Fat, "G Gorduras", "0", "67 g", 0f, over = false)
    }

    @Test fun timelineNode_light() = shot("TimelineNode", false) { Nodes() }
    @Test fun timelineNode_dark() = shot("TimelineNode", true) { Nodes() }

    @Composable private fun Nodes() {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.padding(8.dp)) {
            AeroNodeState.entries.forEach { AeroTimelineNode(it) }
        }
    }

    @Test fun mealCard_light() = shot("MealCard", false) { Meals() }
    @Test fun mealCard_dark() = shot("MealCard", true) { Meals() }

    @Composable private fun Meals() {
        val desc = "2 pães franceses, 2 ovos mexidos e café com leite"
        AeroMealCard(AeroMealState.Logged, "Café da manhã", "07:30", desc, kcal = "520 kcal", log = "520 kcal · 28P · 52C · 22G")
        AeroMealCard(AeroMealState.Over, "Café da manhã", "07:30", desc, kcal = "520 kcal", log = "520 kcal · 28P · 52C · 22G")
        AeroMealCard(AeroMealState.Skipped, "Café da manhã", "07:30", "Refeição pulada")
        AeroMealCard(AeroMealState.Pending, "Café da manhã", "07:30", "Nenhum registro · Toque para registrar, segura para pular", onClick = {})
        AeroMealCard(AeroMealState.Empty, "Café da manhã", "07:30", "Nenhum registro · Toque para registrar, segura para pular", onClick = {})
    }

    @Test fun optionCard_light() = shot("OptionCard", false) { Options() }
    @Test fun optionCard_dark() = shot("OptionCard", true) { Options() }

    @Composable private fun Options() {
        val desc = "O gasto do treino não altera sua meta diária de calorias."
        AeroOptionCard("0% (Não compensar)", desc, selected = true, onClick = {}, badge = "Padrão")
        AeroOptionCard("0% (Não compensar)", desc, selected = false, onClick = {}, badge = "Padrão")
    }

    @Test fun chatBubble_light() = shot("ChatBubble", false) { Bubbles() }
    @Test fun chatBubble_dark() = shot("ChatBubble", true) { Bubbles() }

    @Composable private fun Bubbles() {
        val text = "2 pães franceses com 2 ovos mexidos no café da manhã"
        Box(Modifier.fillMaxWidth()) { AeroChatBubble(text, "20:15", fromUser = true, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)) }
        AeroChatBubble(text, "20:15", fromUser = false)
    }

    @Test fun composer_light() = shot("Composer", false) { Composers() }
    @Test fun composer_dark() = shot("Composer", true) { Composers() }

    @Composable private fun Composers() {
        AeroComposer("", {}, "Descreva sua refeição ou envie foto...", {}, {}, sendEnabled = true)
        AeroComposer("almoço de hoje, comi tudo", {}, "", {}, {})
        AeroComposer(
            "Hoje no almoço comi arroz branco, feijão carioca, bife acebolado, salada de alface e tomate e um copo de suco",
            {}, "", {}, {}, error = "Texto muito longo",
        )
    }

    /** Glass over the gradient: blur on API 31+ (this test), opaque fallback below (glassFallback). */
    @Test fun glass_light() = shot("Glass", false) { GlassDemo() }
    @Test fun glass_dark() = shot("Glass", true) { GlassDemo() }

    @Test @Config(sdk = [30]) fun glassFallback_light() = shot("GlassApi30", false) { GlassDemo() }
    @Test @Config(sdk = [30]) fun glassFallback_dark() = shot("GlassApi30", true) { GlassDemo() }

    @Composable private fun GlassDemo() {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            Box(Modifier.size(90.dp).padding(10.dp).aeroGlass(CircleShape))
            Box(
                Modifier.padding(start = 40.dp, top = 40.dp).fillMaxWidth().height(100.dp).aeroGlass(Aero.shapes.card),
            ) { BasicText("1.240 kcal", Modifier.padding(16.dp), style = Aero.type.heroNumber.copy(color = Aero.colors.textPrimary)) }
        }
    }

    /** ADR-030 § 3: number styles use tabular figures; "1111" and "0000" must measure the same width. */
    @Test
    fun numberStylesAreTabular() {
        val widths = mutableMapOf<String, Pair<Int, Int>>()
        compose.setContent {
            AeroTheme {
                val m = rememberTextMeasurer()
                val t = Aero.type
                listOf("heroNumber" to t.heroNumber, "fieldNumber" to t.fieldNumber, "captionStrong" to t.captionStrong).forEach { (n, s) ->
                    widths[n] = m.measure("1111", s).size.width to m.measure("0000", s).size.width
                }
            }
        }
        compose.waitForIdle()
        assertThat(widths).hasSize(3)
        widths.forEach { (name, w) -> assertWithName(name, w.first, w.second) }
    }

    private fun assertWithName(name: String, ones: Int, zeros: Int) {
        println("TNUM $name 1111=$ones 0000=$zeros")
        assertThat(ones).isEqualTo(zeros)
    }
}
