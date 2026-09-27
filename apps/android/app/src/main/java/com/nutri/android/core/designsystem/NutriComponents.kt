package com.nutri.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    lines: Int = 2,
) {
    val p = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        textStyle = TextStyle(color = p.text, fontSize = 16.sp, lineHeight = 22.sp),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((lines * 28).dp.coerceAtLeast(48.dp))
                    .background(p.surf, RoundedCornerShape(16.dp))
                    .border(1.dp, p.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = p.muted, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaitIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier.size(28.dp), color = LocalPalette.current.gold)
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    compact: Boolean = false,
) {
    val p = LocalPalette.current
    val size = when {
        compact -> 12.sp
        large -> NutriMeasure.fieldPt.sp
        else -> 16.sp
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) Text(label, color = p.muted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                color = p.text,
                fontSize = size,
                fontWeight = FontWeight(560),
                letterSpacing = (-0.5).sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("number-field"),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(p.surf, RoundedCornerShape(16.dp))
                        .border(1.dp, p.line, RoundedCornerShape(16.dp))
                        .padding(horizontal = if (compact) 4.dp else 14.dp, vertical = if (compact) 10.dp else 14.dp),
                    contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}
