package com.nutri.android.feature.devtools

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nutri.android.core.designsystem.DietaBotShapes
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.designsystem.aero.AeroTheme
import com.nutri.android.feature.config.Group
import com.nutri.android.feature.config.SettingRow
import com.nutri.android.feature.config.WipeDialog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable data object RouteDevMemory

fun NavGraphBuilder.devMemoryDestination(nav: NavController) {
    composable<RouteDevMemory> {
        val vm: DevMemoryViewModel = hiltViewModel()
        val ui by vm.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        LaunchedEffect(ui.saved) {
            if (ui.saved) {
                Toast.makeText(context, "Salvo.", Toast.LENGTH_SHORT).show()
                nav.popBackStack()
            }
        }
        // The shared wipe dialog is Aero (A44); the tool itself keeps its own look (ADR-019, no gold).
        AeroTheme {
            DevMemoryScreen(
                ui = ui,
                onBack = { nav.popBackStack() },
                onProfile = vm::setProfile,
                onMemory = vm::setMemory,
                onSave = vm::save,
                onConfirmWipe = vm::confirmWipe,
                onCancelWipe = vm::cancelWipe,
            )
        }
    }
}

/**
 * Last Config row in dev. Hidden while `debug.nutri.hide_dev_tools` = 1, so the cfg capture
 * (tools/capture-config.sh) still matches the gold (ADR-019).
 */
@Composable
fun ColumnScope.DevMemoryConfigRow(nav: NavController) {
    val hidden by produceState<Boolean?>(null) { value = withContext(Dispatchers.IO) { hideDevTools() } }
    if (hidden != false) return
    // The Config column puts 24 dp between blocks.
    Group {
        SettingRow("Memória da IA (dev)", "", "cfg-dev-memory") { nav.navigate(RouteDevMemory) }
    }
}

private fun hideDevTools(): Boolean = runCatching {
    val process = ProcessBuilder("getprop", HIDE_PROP).redirectErrorStream(true).start()
    val out = process.inputStream.bufferedReader().use { it.readText() }.trim()
    process.waitFor(1, TimeUnit.SECONDS)
    out == "1"
}.getOrDefault(false)

private const val HIDE_PROP = "debug.nutri.hide_dev_tools"

/** A23 tool screen (ADR-019): app theme and tokens, no gold, wraps text instead of scrolling sideways. */
@Composable
fun DevMemoryScreen(
    ui: DevMemoryUiState,
    onBack: () -> Unit,
    onProfile: (String) -> Unit,
    onMemory: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmWipe: () -> Unit,
    onCancelWipe: () -> Unit,
) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.phone).testTag("dev-memory")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp, top = 13.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(37.dp)
                        .clip(CircleShape)
                        .background(p.surf2)
                        .border(1.dp, p.line, CircleShape)
                        .dietaClick(onClick = onBack)
                        .testTag("dev-memory-back"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Voltar", tint = p.text, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(17.dp))
                Text("Memória da IA", style = DietaBotType.headlineMd.copy(fontSize = 22.sp, lineHeight = 28.sp), color = p.text, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (ui.loaded) p.ctaBg else p.surf2)
                        .dietaClick(onClick = { if (ui.loaded) onSave() })
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                        .testTag("dev-memory-save"),
                ) {
                    Text("Salvar", style = DietaBotType.labelLg, color = if (ui.loaded) p.ctaText else p.dim)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 25.dp, end = 25.dp, bottom = 32.dp),
            ) {
                ui.error?.let {
                    Text(it, style = DietaBotType.bodyMd, color = p.bad, modifier = Modifier.padding(top = 8.dp).testTag("dev-memory-error"))
                }
                Label("Perfil")
                Field(ui.profileText, onProfile, "dev-profile-field", minHeight = 180, loaded = ui.loaded)
                Label("Memória")
                Text(ui.memorySummary, style = DietaBotType.labelMd, color = p.muted, modifier = Modifier.testTag("dev-memory-count"))
                if (ui.memorySeen.isNotEmpty()) {
                    Text(
                        ui.memorySeen,
                        style = mono.copy(color = p.muted),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("dev-memory-seen"),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Field(ui.memoryText, onMemory, "dev-memory-field", minHeight = 260, loaded = ui.loaded)
                Label("Dia (próximo turno)")
                Text(ui.dayText, style = mono.copy(color = p.muted), modifier = Modifier.fillMaxWidth().testTag("dev-day"))
            }
        }
        if (ui.wipeConfirm) WipeDialog(onConfirmWipe, onCancelWipe)
    }
}

private val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = DietaBotType.labelCaps.copy(fontWeight = FontWeight.W600),
        color = LocalPalette.current.muted,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/**
 * The text lives here, synchronously, and is pushed to the ViewModel on every change; [loaded]
 * resets it once with the loaded value. The A23 field read its value back from the StateFlow one
 * frame late, a known way for the IME to re-apply its buffer over the text: probable cause of the
 * memory shown twice, glued at the last line, seen on 30/09 (A28).
 */
@Composable
private fun Field(value: String, onChange: (String) -> Unit, tag: String, minHeight: Int, loaded: Boolean) {
    val p = LocalPalette.current
    var field by remember(loaded) { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    BasicTextField(
        value = field,
        onValueChange = {
            val changed = it.text != field.text
            field = it
            if (changed) onChange(it.text)
        },
        textStyle = mono.copy(color = p.text),
        cursorBrush = SolidColor(p.gold),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(DietaBotShapes.cardRadius)
            .background(p.surf)
            .border(1.dp, p.line, DietaBotShapes.cardRadius)
            .padding(12.dp)
            .testTag(tag),
    )
}
