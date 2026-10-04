package com.nutri.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Virtual indices leave years of scrolling on either side without allocating a backing list. */
internal object TimeWheelLoop {
    const val COUNT = 1_000_000
    fun value(index: Int, period: Int): Int = Math.floorMod(index, period)
    fun initialIndex(value: Int, period: Int): Int = COUNT / 2 - (COUNT / 2 % period) + value
    fun nearestIndex(index: Int, value: Int, period: Int): Int {
        val delta = Math.floorMod(value - value(index, period) + period / 2, period) - period / 2
        return (index + delta).coerceIn(2, COUNT - 3)
    }
}

/** Five visible rows; the third row is snapped to the centre, including during confirmation. */
internal fun LazyListState.centerIndex(): Int {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        ?: (firstVisibleItemIndex + 2)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimeWheelDialog(title: String, minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val p = LocalPalette.current
    val hours = rememberLazyListState(TimeWheelLoop.initialIndex(minutes / 60, 24) - 2)
    val minute = rememberLazyListState(TimeWheelLoop.initialIndex(minutes % 60, 60) - 2)
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(if (p.isDark) 0.6f else 0.4f) }
        Column(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(shape)
                .background(p.surf).border(1.dp, p.line, shape)
                .padding(horizontal = if (p.isDark) 24.dp else 26.dp, vertical = 24.dp)
                .testTag("time-wheel-dialog").semantics { testTagsAsResourceId = true },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = p.text, style = TextStyle(fontFamily = FontFamily.Default, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.W600))
            Text("Horário da refeição", color = p.muted, style = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 20.sp), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(if (p.isDark) 28.dp else 22.dp))
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                val band = RoundedCornerShape(12.dp)
                Box(Modifier.padding(horizontal = if (p.isDark) 9.dp else 0.dp).fillMaxWidth().height(if (p.isDark) 52.dp else 58.dp).clip(band)
                    .background(if (p.isDark) p.surf2 else p.bg)
                    .border(1.dp, p.line.copy(alpha = if (p.isDark) 0.4f else 0.6f), band))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeWheel(hours, 24, "Horas", "horas", "time-wheel-hours", Modifier.weight(1f))
                    Text(":", color = p.text, style = TextStyle(fontFamily = FontFamily.Default, fontSize = 40.sp, fontWeight = FontWeight.W600, textAlign = TextAlign.Center), modifier = Modifier.width(32.dp).clearAndSetSemantics {})
                    TimeWheel(minute, 60, "Minutos", "minutos", "time-wheel-minutes", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(if (p.isDark) 24.dp else 18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeWheelAction("Cancelar", "time-wheel-cancel", false, Modifier.weight(1f), onDismiss)
                TimeWheelAction("OK", "time-wheel-ok", true, Modifier.weight(1f)) {
                    onConfirm(TimeWheelLoop.value(hours.centerIndex(), 24) * 60 + TimeWheelLoop.value(minute.centerIndex(), 60))
                }
            }
        }
    }
}

@Composable
private fun TimeWheel(state: LazyListState, period: Int, label: String, unit: String, tag: String, modifier: Modifier) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val feedback = LocalHapticFeedback.current
    val selected by remember(state) { derivedStateOf { state.centerIndex() } }
    fun moveTo(index: Int) { scope.launch { state.animateScrollToItem(index.coerceIn(2, TimeWheelLoop.COUNT - 3) - 2) } }
    LaunchedEffect(state, feedback) {
        snapshotFlow { state.centerIndex() }.distinctUntilChanged().drop(1).collect {
            // The platform honours the system touch-vibration setting, as in A20.
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
            val size = when (distance) { 0 -> 40; 1 -> 28; else -> 22 }
            val opacity = when (distance) { 0 -> 1f; 1 -> 0.5f; 2 -> 0.25f; else -> 0f }
            Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                Text("%02d".format(TimeWheelLoop.value(index, period)),
                    color = p.text.copy(alpha = opacity),
                    style = TextStyle(fontFamily = FontFamily.Default, fontSize = size.sp, fontWeight = when {
                        distance == 0 -> if (p.isDark) FontWeight.W600 else FontWeight.W700
                        distance == 1 && p.isDark -> FontWeight.W600
                        else -> FontWeight.W400
                    }),
                    modifier = Modifier.graphicsLayer {
                        if (distance == 0 && !p.isDark) scaleX = 0.95f
                        translationY = when (index - selected) {
                            -2 -> (if (p.isDark) -4.dp else 3.dp).toPx()
                            -1 -> (if (p.isDark) -6.dp else -4.dp).toPx()
                            0 -> -3.dp.toPx()
                            1 -> (if (p.isDark) 6.dp else 3.dp).toPx()
                            2 -> (if (p.isDark) 4.dp else -4.dp).toPx()
                            else -> 0f
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeWheelAction(text: String, tag: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    // ST3 uses the same contrast pair as the O3 Continuar button.
    val fill = if (p.isDark) Color(0xFFE2E2E9) else Color(0xFF111111)
    Box(modifier.height(48.dp).clip(CircleShape).background(if (primary) fill else Color.Transparent)
        .then(if (primary) Modifier else Modifier.border(1.dp, p.line, CircleShape))
        .dietaClick(if (primary) Haptic.Confirm else Haptic.Light, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) p.surf else p.text,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.W600),
            modifier = Modifier.graphicsLayer { scaleX = 0.96f })
    }
}
