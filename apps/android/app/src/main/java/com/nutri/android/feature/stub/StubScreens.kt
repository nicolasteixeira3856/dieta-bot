package com.nutri.android.feature.stub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.NutriType

/** Config scaffold. A3 replaces the body (cfg, wipe). */
@Composable
fun ConfigStubScreen(onBack: () -> Unit) = StubScaffold("Configurações", "cfg", onBack)

@Composable
private fun StubScaffold(title: String, tag: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.phone).statusBarsPadding().testTag(tag)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(p.card)
                    .border(1.dp, p.line, CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("$tag-back"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = p.text, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, style = NutriType.headlineMd.copy(fontSize = 18.sp, fontWeight = FontWeight.W700), color = p.text)
        }
    }
}
