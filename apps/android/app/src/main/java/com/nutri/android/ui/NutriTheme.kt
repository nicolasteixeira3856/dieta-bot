package com.nutri.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

private val scheme = darkColorScheme(
    background = Bg,
    surface = Surf,
    surfaceContainer = Surf2,
    surfaceContainerHigh = Surf2,
    primary = CtaBg,
    onPrimary = CtaText,
    secondary = Gold,
    onSecondary = CtaText,
    onBackground = TextMain,
    onSurface = TextMain,
    outline = Line,
    error = Bad,
    onError = TextMain,
)

private val tipo = Typography(
    headlineLarge = TextStyle(color = TextMain, fontSize = 34.sp, fontWeight = FontWeight(590), letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(color = TextMain, fontSize = 28.sp, fontWeight = FontWeight(560)),
    titleLarge = TextStyle(color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.W600),
    bodyLarge = TextStyle(color = TextMain, fontSize = 16.sp),
    bodyMedium = TextStyle(color = Muted, fontSize = 13.sp),
    labelSmall = TextStyle(color = Gold, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 1.sp),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = tipo, content = content)
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
