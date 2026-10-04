package com.nutri.android.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroFieldNumber
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroSheet

private val FieldShape = RoundedCornerShape(DietaBotMeasure.cardDp.dp)

/**
 * homeW field: 28pt number + muted "kcal", gold flame, gold border while focused, then the live
 * credit line. Used as is by the Config workout sheet.
 */
@Composable
fun WorkoutField(
    state: WorkoutEditorState,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "workout",
    autoFocus: Boolean = false,
) {
    val p = LocalPalette.current
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // Cursor at the end of the stored value when the sheet opens.
    var text by remember { mutableStateOf(TextFieldValue(state.input, TextRange(state.input.length))) }
    if (text.text != state.input) text = TextFieldValue(state.input, TextRange(state.input.length))
    val number = DietaBotType.displayLg.copy(fontSize = DietaBotMeasure.fieldPt.sp, lineHeight = 34.sp, fontWeight = FontWeight.W700, letterSpacing = (-1).sp)
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                // Stitch: 56 dp dark, 59 dp light.
                .height(57.dp)
                .clip(FieldShape)
                .background(p.surf)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) p.gold else p.line, FieldShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    runCatching { focus.requestFocus() }
                }
                .padding(start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { v ->
                    val clean = WorkoutEditorState.clean(v.text)
                    text = if (clean == v.text) v else TextFieldValue(clean, TextRange(clean.length))
                    if (clean != state.input) onChange(clean)
                },
                textStyle = number.copy(color = p.text),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused }
                    .testTag("$tag-field"),
                decorationBox = { inner ->
                    Box {
                        if (state.input.isEmpty()) {
                            Text("0", style = number, color = p.dim)
                        }
                        inner()
                    }
                },
            )
            Text(
                "kcal",
                style = DietaBotType.bodyLg.copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
                color = p.muted,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = p.gold, modifier = Modifier.size(22.dp))
        }
        Text(
            state.creditLine,
            style = DietaBotType.bodyMd.copy(fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            color = p.muted,
            modifier = Modifier.padding(top = 10.dp).testTag("$tag-credit"),
        )
    }
}

/**
 * homeW: "Treino de hoje" Aero sheet (Sheet/Bottom + Field/Number) over the blurred Home, under overlay/scrim.
 * Salvar stores day.workoutKcal (empty = no workout).
 */
@Composable
fun BoxScope.WorkoutSheet(
    state: WorkoutEditorState,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    Box(
        Modifier
            .fillMaxSize()
            .background(Aero.colors.overlayScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
    )
    val focus = remember { FocusRequester() }
    var text by remember { mutableStateOf(TextFieldValue(state.input, TextRange(state.input.length))) }
    if (text.text != state.input) text = TextFieldValue(state.input, TextRange(state.input.length))
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AeroSheet(
        title = "Treino de hoje",
        primary = "Salvar",
        onPrimary = onSave,
        secondary = "Cancelar",
        onSecondary = onCancel,
        primaryTag = "home-workout-save",
        secondaryTag = "home-workout-cancel",
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .testTag("home-workout-sheet"),
        bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    ) {
        AeroFieldNumber(
            value = text,
            onValueChange = { v ->
                val clean = WorkoutEditorState.clean(v.text)
                text = if (clean == v.text) v else TextFieldValue(clean, TextRange(clean.length))
                if (clean != state.input) onChange(clean)
            },
            unit = "kcal",
            helper = state.creditLine,
            icon = AeroIconName.Barbell,
            focusRequester = focus,
            fieldTag = "home-workout-field",
            helperTag = "home-workout-credit",
        )
    }
}
