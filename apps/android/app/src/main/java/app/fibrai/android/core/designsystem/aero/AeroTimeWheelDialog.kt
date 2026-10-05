package app.fibrai.android.core.designsystem.aero

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.TimeWheelLoop
import app.fibrai.android.core.designsystem.centerIndex
import app.fibrai.android.core.designsystem.dietaClick
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dialog/TimeWheel (Figma `Design`, D4): glass dialog with the hour and minute wheels (Hero/Number at the centre,
 * Title at ±1 at 55 %, Body at ±2 at 30 %), the surface/2 selection band and the Cancelar / OK pair.
 * Drawn in the screen over overlay/scrim (the screen blurs itself behind it), centred; back dismisses.
 * Same wheel behavior as TimeWheelDialog: looping, snapping, haptics, arrow and volume keys, accessibility.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.AeroTimeWheelDialog(title: String, minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    val hours = rememberLazyListState(TimeWheelLoop.initialIndex(minutes / 60, 24) - 2)
    val minute = rememberLazyListState(TimeWheelLoop.initialIndex(minutes % 60, 60) - 2)
    BackHandler(onBack = onDismiss)
    AeroScrim(onDismiss)
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .aeroGlass(RoundedCornerShape(AeroDimens.radiusSheet), backdropBlurred = true)
            .aeroSwallowTaps()
            .padding(25.dp)
            .testTag("time-wheel-dialog")
            .semantics { testTagsAsResourceId = true },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AeroText(title, style = type.title.copy(color = c.textPrimary))
        AeroText("Horário da refeição", style = type.caption.copy(color = c.textMuted))
        Box(Modifier.padding(vertical = 20.dp).fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(AeroDimens.radiusCard))
                    .background(c.surface2)
                    .border(1.dp, c.borderLine, RoundedCornerShape(AeroDimens.radiusCard)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AeroWheel(hours, 24, "Horas", "horas", "time-wheel-hours", Modifier.weight(1f))
                AeroText(":", style = type.heroNumber.copy(color = c.textPrimary, textAlign = TextAlign.Center), modifier = Modifier.width(9.dp).clearAndSetSemantics {})
                AeroWheel(minute, 60, "Minutos", "minutos", "time-wheel-minutes", Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val pill = RoundedCornerShape(percent = 50)
            Box(
                Modifier
                    .height(52.dp)
                    .clip(pill)
                    .background(c.surfaceTint)
                    .border(1.dp, c.borderLine, pill)
                    .dietaClick(Haptic.Light, onClick = onDismiss)
                    .testTag("time-wheel-cancel")
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                AeroText("Cancelar", style = type.button.copy(color = c.textPrimary))
            }
            AeroButtonPrimary(
                "OK",
                {
                    onConfirm(TimeWheelLoop.value(hours.centerIndex(), 24) * 60 + TimeWheelLoop.value(minute.centerIndex(), 60))
                },
                modifier = Modifier.weight(1f).testTag("time-wheel-ok"),
            )
        }
    }
}

@Composable
private fun AeroWheel(state: LazyListState, period: Int, label: String, unit: String, tag: String, modifier: Modifier) {
    val c = Aero.colors
    val type = Aero.type
    val scope = rememberCoroutineScope()
    val feedback = LocalHapticFeedback.current
    val selected by remember(state) { derivedStateOf { state.centerIndex() } }
    fun moveTo(index: Int) { scope.launch { state.animateScrollToItem(index.coerceIn(2, TimeWheelLoop.COUNT - 3) - 2) } }
    LaunchedEffect(state, feedback) {
        snapshotFlow { state.centerIndex() }.distinctUntilChanged().drop(1).collect {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        modifier = modifier.height(200.dp).testTag(tag).onKeyEvent {
            val step = when (it.key) {
                Key.DirectionUp, Key.VolumeUp -> -1
                Key.DirectionDown, Key.VolumeDown -> 1
                else -> return@onKeyEvent false
            }
            if (it.type == KeyEventType.KeyDown) moveTo(selected + step)
            true
        }.focusable().clearAndSetSemantics {
            contentDescription = label
            stateDescription = "%02d %s".format(TimeWheelLoop.value(selected, period), unit)
            progressBarRangeInfo = ProgressBarRangeInfo(TimeWheelLoop.value(selected, period).toFloat(), 0f..(period - 1).toFloat(), period - 2)
            setProgress { value ->
                if (!value.isFinite()) false else {
                    moveTo(TimeWheelLoop.nearestIndex(selected, value.roundToInt().coerceIn(0, period - 1), period))
                    true
                }
            }
        },
    ) {
        items(TimeWheelLoop.COUNT) { index ->
            val distance = abs(index - selected)
            val style = when (distance) {
                0 -> type.heroNumber
                1 -> type.title
                else -> type.body
            }
            val alpha = when (distance) { 0 -> 1f; 1 -> 0.55f; 2 -> 0.30f; else -> 0f }
            Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                AeroText(
                    "%02d".format(TimeWheelLoop.value(index, period)),
                    style = style.copy(color = c.textPrimary.copy(alpha = c.textPrimary.alpha * alpha)),
                )
            }
        }
    }
}
