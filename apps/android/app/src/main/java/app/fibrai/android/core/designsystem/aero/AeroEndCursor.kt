package app.fibrai.android.core.designsystem.aero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay

/**
 * A46: editing an existing value starts at its end. The text field keeps a [TextFieldValue] whose cursor goes to the
 * end when the field gains focus (tap, IME Next, an adjust button, requestFocus) and when the value changes from
 * outside (prefill, suggestion chip, copy). The tap that gives the focus does not move it back; later taps inside the
 * focused field place the cursor where the user taps.
 *
 * Unfocused, the selection rests at the start: a single-line field as wide as its text would otherwise scroll the text
 * sideways by the width of a cursor parked at the end (the goldens are drawn without it).
 */
@Stable
internal class EndCursorField(initial: String) {
    var field by mutableStateOf(TextFieldValue(initial, TextRange.Zero))
    internal var external = initial
    internal var focused = false
    internal var justFocused = false
    internal var focusCount by mutableIntStateOf(0)

    fun onValueChange(next: TextFieldValue, onValueChange: (String) -> Unit) {
        val previous = field.text
        if (justFocused && next.text == previous) {
            // The tap that focused the field only placed the cursor: keep it at the end.
            justFocused = false
            field = next.copy(selection = TextRange(next.text.length))
            return
        }
        justFocused = false
        field = next
        if (next.text != previous) onValueChange(next.text)
    }
}

/** The cursor state of a text field showing [value]. */
@Composable
internal fun rememberEndCursorField(value: String): EndCursorField {
    val state = remember { EndCursorField(value) }
    if (value != state.external) {
        state.external = value
        if (value != state.field.text) state.field = TextFieldValue(value, if (state.focused) TextRange(value.length) else TextRange.Zero)
    }
    // The focusing tap arrives in the same gesture; after that, taps inside the field move the cursor normally.
    LaunchedEffect(state.focusCount) {
        if (state.focusCount > 0) {
            delay(FOCUS_TAP_WINDOW_MS)
            state.justFocused = false
        }
    }
    return state
}

/** Moves the cursor of [state] to the end when the field gains focus. */
internal fun Modifier.endCursorOnFocus(state: EndCursorField): Modifier = onFocusChanged {
    if (it.isFocused && !state.focused) {
        state.field = state.field.copy(selection = TextRange(state.field.text.length))
        state.justFocused = true
        state.focusCount++
    }
    if (!it.isFocused && state.focused) state.field = state.field.copy(selection = TextRange.Zero)
    state.focused = it.isFocused
}

private const val FOCUS_TAP_WINDOW_MS = 300L
