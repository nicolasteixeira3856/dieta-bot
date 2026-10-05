package app.fibrai.android.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroFieldNumber
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroSheet

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
            .statusBarsPadding()
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
            onGlass = true,
        )
    }
}
