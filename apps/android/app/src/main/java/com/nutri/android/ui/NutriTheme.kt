package com.nutri.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF0B0D10)
val Panel = Color(0xFF12151A)
val Phone = Color(0xFF0E1114)
val Surf = Color(0xFF171B20)
val Surf2 = Color(0xFF1E242B)
val Line = Color(0xFF2A3139)
val TextMain = Color(0xFFF3F5F7)
val Muted = Color(0xFF8B939C)
val Dim = Color(0xFF5C6570)
val Gold = Color(0xFFE8B86D)
val Good = Color(0xFF7DDA9A)
val Bad = Color(0xFFE07A6A)
val CtaBg = Color(0xFFF3F5F7)
val CtaText = Color(0xFF111111)
val Handle = Color(0xFF3A424C)

private val scheme = darkColorScheme(
    background = Bg,
    surface = Surf,
    surfaceVariant = Surf2,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Panel,
    surfaceContainer = Surf,
    surfaceContainerHigh = Surf2,
    surfaceContainerHighest = Surf2,
    surfaceBright = Surf2,
    surfaceDim = Bg,
    primary = CtaBg,
    onPrimary = CtaText,
    primaryContainer = Surf,
    onPrimaryContainer = TextMain,
    secondary = Gold,
    onSecondary = CtaText,
    secondaryContainer = Color(0x33E8B86D),
    onSecondaryContainer = Gold,
    tertiary = Good,
    onTertiary = Bg,
    tertiaryContainer = Surf,
    onTertiaryContainer = Good,
    onBackground = TextMain,
    onSurface = TextMain,
    onSurfaceVariant = Handle,
    outline = Line,
    outlineVariant = Line,
    error = Bad,
    onError = TextMain,
    errorContainer = Surf,
    onErrorContainer = Bad,
    surfaceTint = Color.Transparent,
    inverseSurface = TextMain,
    inverseOnSurface = Bg,
    inversePrimary = Gold,
    scrim = Bg,
)

private val formas = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp),
    largeIncreased = RoundedCornerShape(14.dp),
    extraLargeIncreased = RoundedCornerShape(22.dp),
    extraExtraLarge = RoundedCornerShape(22.dp),
)

private val tipo = Typography(
    headlineLarge = TextStyle(color = TextMain, fontSize = 34.sp, fontWeight = FontWeight(590), letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(color = TextMain, fontSize = 28.sp, fontWeight = FontWeight(560)),
    titleLarge = TextStyle(color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.W600),
    bodyLarge = TextStyle(color = TextMain, fontSize = 16.sp),
    bodyMedium = TextStyle(color = Muted, fontSize = 13.sp),
    labelSmall = TextStyle(color = Gold, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 1.sp),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NutriTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = formas,
        typography = tipo,
        content = content,
    )
}

@Composable
fun GrupoEscolha(
    opcoes: List<Pair<String, Boolean>>,
    onEscolha: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        opcoes.forEachIndexed { index, (rotulo, marcado) ->
            val toque = MutableInteractionSource()
            val item = Modifier.weight(1f).animateWidth(toque).heightIn(min = 48.dp)
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = marcado,
                        onCheckedChange = { ligado -> if (ligado) onEscolha(index) },
                        modifier = item,
                        interactionSource = toque,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = Surf2,
                            contentColor = TextMain,
                            checkedContainerColor = Gold,
                            checkedContentColor = CtaText,
                        ),
                        border = if (marcado) null else BorderStroke(1.dp, Line),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Text(
                            rotulo,
                            color = if (marcado) CtaText else TextMain,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                        )
                    }
                },
                menuContent = { estado: ButtonGroupMenuState ->
                    DropdownMenuItem(
                        text = { Text(rotulo) },
                        onClick = {
                            onEscolha(index)
                            estado.dismiss()
                        },
                    )
                },
            )
        }
    }
}

@Composable
fun NutriCta(texto: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CtaBg, contentColor = CtaText),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(texto, color = CtaText, fontWeight = FontWeight.W600)
    }
}

fun Modifier.cardBorda(): Modifier = this
    .background(Surf, RoundedCornerShape(14.dp))
    .border(1.dp, Line, RoundedCornerShape(14.dp))
