package com.nutri.android.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.nutri.android.core.designsystem.aero.aeroPressIndication

/**
 * Touch vibration (A20). Two strengths only. The platform call honours the system
 * "vibrate on touch" setting, so turning it off silences the app too.
 */
enum class Haptic {
    /** Gravar, Substituir, Salvar, Continuar, Pular refeição. */
    Confirm,

    /** Chips, Config rows, tabs, Trocar, Cancelar, FAB. */
    Light,

    None,
}

/** For controls that handle their own click: call it inside onClick. */
@Composable
fun rememberHaptic(): (Haptic) -> Unit {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) {
        { haptic ->
            when (haptic) {
                Haptic.Confirm -> feedback.performHapticFeedback(HapticFeedbackType.Confirm)
                Haptic.Light -> feedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                Haptic.None -> Unit
            }
        }
    }
}

/**
 * Every tappable control: the Aero press veil + haptic. Clip the shape before it so the veil follows it.
 * `indication = null` stays only on scrims and sheet backgrounds that swallow taps.
 */
@Composable
fun Modifier.dietaClick(
    haptic: Haptic = Haptic.Light,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val perform = rememberHaptic()
    return clickable(interactionSource = null, indication = aeroPressIndication(), enabled = enabled, role = role) {
        perform(haptic)
        onClick()
    }
}
