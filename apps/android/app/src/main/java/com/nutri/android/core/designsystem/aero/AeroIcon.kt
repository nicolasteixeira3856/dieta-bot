package com.nutri.android.core.designsystem.aero

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nutri.android.R

/**
 * Phosphor icons used in the Figma file `Design` (MIT, third_party/phosphor/LICENSE), one VectorDrawable per
 * name and weight (`ph_<name>_<weight>.xml`). Duotone keeps its 20 % layer under the tint.
 */
enum class AeroIconName(@param:DrawableRes val res: Int) {
    ArrowLeft(R.drawable.ph_arrow_left_regular),
    ArrowRight(R.drawable.ph_arrow_right_bold),
    ArrowUp(R.drawable.ph_arrow_up_bold),
    Barbell(R.drawable.ph_barbell_duotone),
    Camera(R.drawable.ph_camera_regular),
    CaretLeft(R.drawable.ph_caret_left_regular),
    CaretRight(R.drawable.ph_caret_right_regular),
    ChatCircle(R.drawable.ph_chat_circle_fill),
    Check(R.drawable.ph_check_bold),
    CheckCircle(R.drawable.ph_check_circle_bold),
    Checks(R.drawable.ph_checks_regular),
    Gear(R.drawable.ph_gear_regular),
    Info(R.drawable.ph_info_duotone),
    Lightning(R.drawable.ph_lightning_duotone),
    Minus(R.drawable.ph_minus_regular),
    ExclamationMark(R.drawable.ph_exclamation_mark_bold),
    Question(R.drawable.ph_question_regular),
    Clock(R.drawable.ph_clock_regular),
    Copy(R.drawable.ph_copy_regular),
    BellRinging(R.drawable.ph_bell_ringing_regular),
    Coffee(R.drawable.ph_coffee_regular),
    Cookie(R.drawable.ph_cookie_regular),
    ForkKnife(R.drawable.ph_fork_knife_regular),
    BowlFood(R.drawable.ph_bowl_food_regular),
    Moon(R.drawable.ph_moon_regular),
    Sparkle(R.drawable.ph_sparkle_fill),
    FastForward(R.drawable.ph_fast_forward_regular),
    ArrowCounterClockwise(R.drawable.ph_arrow_counter_clockwise_regular),
    ArrowsClockwise(R.drawable.ph_arrows_clockwise_regular),
    ArrowsLeftRight(R.drawable.ph_arrows_left_right_regular),
    ClockCounterClockwise(R.drawable.ph_clock_counter_clockwise_regular),
    Image(R.drawable.ph_image_regular),
    PencilSimple(R.drawable.ph_pencil_simple_regular),
    PushPin(R.drawable.ph_push_pin_regular),
    Trash(R.drawable.ph_trash_regular),
    X(R.drawable.ph_x_regular),
}

/** A Phosphor icon in a token color. The gloss comes from the container, never from the icon (ADR-030 § 4). */
@Composable
fun AeroIcon(
    name: AeroIconName,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(name.res),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
