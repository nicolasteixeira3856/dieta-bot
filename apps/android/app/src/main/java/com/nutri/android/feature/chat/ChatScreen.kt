package com.nutri.android.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.Inter
import com.nutri.android.core.designsystem.Jakarta
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.designsystem.formatRemaining
import com.nutri.android.domain.SlotBand
import com.nutri.android.domain.SlotSuggestions
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Every chat bubble has the same shape: 16 dp on all four corners (ST1). */
internal val BubbleShape = RoundedCornerShape(16.dp)
private val UserShape = BubbleShape
private val BotShape = BubbleShape

/** Suggestion chips of an empty day (chat0). The photo one waits for A6. */
private val SUGGESTIONS = listOf("☕" to "Café com 2 ovos mexidos", "🥗" to "Salada Caesar")

@Composable
fun ChatScreen(
    ui: ChatUiState,
    onBack: () -> Unit,
    onComposer: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onRecord: (estimateId: Long, slotId: Long) -> Unit,
    onSwap: (estimateId: Long) -> Unit,
    onSheetSelect: (Long) -> Unit,
    onSheetConfirm: () -> Unit,
    onSheetClose: () -> Unit,
    onAskSkip: (SlotRef) -> Unit,
    onSkipConfirm: () -> Unit,
    onSkipCancel: () -> Unit,
    /** ADR-017: Gravar on a taken slot → Substituir | Outra refeição. */
    onReplaceConfirm: () -> Unit = {},
    onReplaceElsewhere: () -> Unit = {},
    onReplaceCancel: () -> Unit = {},
    /** A6: composer camera → chooser; chip → camera; chooser rows → launchers. */
    onPhoto: () -> Unit = {},
    onCamera: () -> Unit = {},
    onGallery: () -> Unit = {},
    onPhotoSheetClose: () -> Unit = {},
    onNoticeShown: () -> Unit = {},
    /** chatA: ✕ on the composer thumbnail. */
    onRemoveAttachment: () -> Unit = {},
    /** chatR: Registrar assim on a plan. */
    onRecordPlan: (estimateId: Long) -> Unit = {},
    /** chatS: Registrar | Quase igual on the routine card. */
    onRoutineRecord: () -> Unit = {},
    onRoutineEdit: () -> Unit = {},
    /** chatQ: Forçar estimativa from the second question. */
    onForceEstimate: () -> Unit = {},
) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(p.phone)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = 0.10f), Color.Transparent), Offset(0f, 0f), 160.dp.toPx()), 160.dp.toPx(), Offset(0f, 0f))
            }
            .testTag("chat"),
    ) {
        val overlay = ui.sheetFor != null || ui.skipConfirm != null || ui.replaceConfirm != null || ui.photoSheet
        // Stitch: backdrop-blur behind the sheet and the skip dialog.
        Column(Modifier.fillMaxSize().then(if (overlay) Modifier.blur(8.dp) else Modifier).statusBarsPadding().imePadding()) {
            Header(onBack)
            Thread(ui, onRetry, onRoutineRecord, onRoutineEdit, Modifier.weight(1f))
            ui.notice?.let { Notice(it, onNoticeShown) }
            // chatA: with a photo attached the chips go away (they would compete with it).
            if (ui.emptyDay && ui.attachment == null) SuggestionRow(onComposer, onCamera)
            Footer(ui, onComposer, onSend, onRecord, onSwap, onAskSkip, onPhoto, onRemoveAttachment, onRecordPlan, onForceEstimate)
        }
        if (ui.sheetFor != null) SlotSheet(ui, onSheetSelect, onSheetConfirm, onSheetClose)
        ui.skipConfirm?.let { SkipDialog(it, onSkipConfirm, onSkipCancel) }
        ui.replaceConfirm?.let { ReplaceDialog(it, onReplaceConfirm, onReplaceElsewhere, onReplaceCancel) }
        if (ui.photoSheet) PhotoSheet(onCamera, onGallery, onPhotoSheetClose)
    }
}

// ----------------------------------------------------------------------------- header

@Composable
private fun Header(onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(p.card)
                .border(1.dp, p.line, CircleShape)
                .dietaClick(onClick = onBack)
                .testTag("chat-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Voltar", tint = p.text, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chat Dieta Bot", style = DietaBotType.headlineMd.copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.025).em), color = p.text)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(p.gold))
            }
            Text(
                "ASSISTENTE DE REFEIÇÕES",
                style = DietaBotType.labelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.em),
                color = p.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // Stitch draws a "tune" button here; there is no such feature, so only its space stays.
        Spacer(Modifier.size(38.dp))
    }
}

// ----------------------------------------------------------------------------- thread

@Composable
private fun Thread(ui: ChatUiState, onRetry: () -> Unit, onRoutineRecord: () -> Unit, onRoutineEdit: () -> Unit, modifier: Modifier) {
    val list = rememberLazyListState()
    LaunchedEffect(ui.items.size) { if (ui.items.isNotEmpty()) list.animateScrollToItem(ui.items.lastIndex) }
    LazyColumn(
        modifier.fillMaxWidth().testTag("chat-thread"),
        state = list,
        // Bottom 2 dp: with the footer 8 dp, the last time sits 10 dp above the action bar (chatR).
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 2.dp),
    ) {
        itemsIndexed(ui.items, key = { _, it -> it.key }) { i, item ->
            // 16 dp between items; the question hugs its estimate (chatE); the routine card sits
            // 12 dp under the meta card (chatS).
            val gap = when {
                i == 0 -> 0.dp
                item is ChatItem.Question && !item.standalone -> 5.dp
                item is ChatItem.Routine -> 12.dp
                else -> 16.dp
            }
            Box(Modifier.padding(top = gap)) {
                ThreadItem(item, ui, onRetry, onRoutineRecord, onRoutineEdit)
            }
        }
    }
}

@Composable
private fun ThreadItem(item: ChatItem, ui: ChatUiState, onRetry: () -> Unit, onRoutineRecord: () -> Unit, onRoutineEdit: () -> Unit) {
    when (item) {
        is ChatItem.DateSeparator -> DatePill(item.label)
        is ChatItem.User -> item.photoPath?.let { PhotoBubble(item, it) } ?: UserBubble(item)
        is ChatItem.Assistant -> AssistantBubble(item)
        is ChatItem.Question -> QuestionBubble(item)
        is ChatItem.Receipt -> ReceiptCard(item)
        is ChatItem.Greeting -> Greeting(item, ui)
        ChatItem.Loading -> LoadingBubble()
        ChatItem.Failed -> FailedBubble(onRetry)
        is ChatItem.Routine -> RoutineSuggestionCard(item.suggestion, onRoutineRecord, onRoutineEdit)
    }
}

@Composable
private fun DatePill(label: String) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = DietaBotType.labelCaps.copy(letterSpacing = 0.05.em),
            color = p.muted,
            modifier = Modifier
                .clip(CircleShape)
                .background(p.card.copy(alpha = 0.8f))
                .border(1.dp, p.line, CircleShape)
                .padding(horizontal = 14.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UserBubble(item: ChatItem.User) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Text(
            item.text,
            style = DietaBotType.bodyMd.copy(fontSize = 13.5.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            color = p.text,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .wrapContentWidth(Alignment.End)
                .clip(UserShape)
                .background(p.surf2)
                .border(1.dp, p.text.copy(alpha = 0.04f), UserShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("chat-user-${item.id}"),
        )
        Row(Modifier.padding(top = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.time, style = DietaBotType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.dim)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.DoneAll, contentDescription = null, tint = p.gold, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AiLabel() {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 9.dp)) {
        Box(
            Modifier.size(16.dp).clip(CircleShape).background(p.gold.copy(alpha = 0.2f)).border(1.dp, p.gold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = p.gold, modifier = Modifier.size(10.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text("Dieta Bot AI", style = DietaBotType.headlineMd.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.025).em), color = p.text)
    }
}

@Composable
private fun AssistantBubble(item: ChatItem.Assistant) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(0.88f), horizontalAlignment = Alignment.Start) {
        AiLabel()
        Column(
            Modifier
                .clip(BotShape)
                .background(p.card)
                .border(1.dp, p.line, BotShape)
                .padding(16.dp)
                .testTag("chat-bot-${item.id}"),
        ) {
            if (item.plan != null) {
                PlanText(item.text)
                PlanPanel(item.plan)
            } else {
                Text(highlighted(item.text, item.highlights, p.gold), style = DietaBotType.bodyMd.copy(fontSize = 13.5.sp, lineHeight = 21.sp, letterSpacing = 0.sp), color = p.text)
            }
            item.estimate?.let { e ->
                EstimateCard(e)
                e.slotQuestion?.let { q ->
                    Text(
                        slotQuestion(q, p.text),
                        style = DietaBotType.bodyMd.copy(fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.sp),
                        color = p.muted,
                    )
                }
            }
        }
        MemoryChips(item.memory)
        // With a follow-up question the time moves under the question bubble (chatE).
        if (item.estimate?.question.isNullOrBlank()) {
            Text(item.time, style = DietaBotType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.dim, modifier = Modifier.padding(top = 4.dp, start = 8.dp))
        }
    }
}

/**
 * The clarifying question in its own bubble. Gold bar on the left, help icon, text in primary colour.
 * Standalone (chatQ): a question before the estimate, with the "Dieta Bot AI" label above.
 */
@Composable
private fun QuestionBubble(item: ChatItem.Question) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(0.88f), horizontalAlignment = Alignment.Start) {
        if (item.standalone) AiLabel()
        Row(
            Modifier
                .fillMaxWidth()
                .clip(BubbleShape)
                .drawBehind { questionFrame(p.card, p.line, p.gold) }
                .padding(start = 19.dp, end = 16.dp, top = 16.dp, bottom = 19.dp)
                .testTag("chat-question"),
            // Top: a question of several lines keeps the icon on its first line. Text 18 sp (ST7 gold).
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = p.gold, modifier = Modifier.padding(top = 3.25.dp).size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(item.text, style = DietaBotType.bodyLg.copy(fontSize = 18.sp, lineHeight = 24.5.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.text)
        }
        MemoryChips(item.memory)
        Text(item.time, style = DietaBotType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.dim, modifier = Modifier.padding(top = 4.dp, start = 8.dp))
    }
}

/**
 * Gold's CSS frame: 1 dp line, 3 dp gold on the left that follows the 16 dp corners. Drawn inside a
 * box clipped to [BubbleShape]: line and gold first, the card on top inset by the border widths.
 */
private fun DrawScope.questionFrame(card: Color, line: Color, gold: Color) {
    val edge = 1.dp.toPx()
    val bar = 3.dp.toPx()
    val radius = 16.dp.toPx()
    drawRect(line)
    // Where the corner turns from gold to line, as the browser splits the border.
    drawRect(gold, size = Size(bar + (radius - bar) / 2, size.height))
    val inner = RoundRect(
        left = bar,
        top = edge,
        right = size.width - edge,
        bottom = size.height - edge,
        topLeftCornerRadius = CornerRadius(radius - bar, radius - edge),
        topRightCornerRadius = CornerRadius(radius - edge),
        bottomRightCornerRadius = CornerRadius(radius - edge),
        bottomLeftCornerRadius = CornerRadius(radius - bar, radius - edge),
    )
    drawPath(Path().apply { addRoundRect(inner) }, card)
}

/** "Deseja registrar essa refeição no {slot}?" with the slot in text colour. */
private fun slotQuestion(q: String, emphasis: Color): AnnotatedString = buildAnnotatedString {
    val start = q.indexOf(" no ").takeIf { it >= 0 }?.plus(4)
    if (start == null || !q.endsWith("?")) {
        append(q)
        return@buildAnnotatedString
    }
    append(q.substring(0, start))
    withStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.W500)) { append(q.substring(start, q.length - 1)) }
    append("?")
}

private fun highlighted(text: String, names: List<String>, gold: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    names.filter { it.length >= 3 }.forEach { name ->
        var from = 0
        while (true) {
            val at = text.indexOf(name, from, ignoreCase = true)
            if (at < 0) break
            addStyle(SpanStyle(color = gold, fontWeight = FontWeight.W500), at, at + name.length)
            from = at + name.length
        }
    }
}

@Composable
private fun EstimateCard(e: EstimateView) {
    val p = LocalPalette.current
    Column(
        Modifier
            .padding(top = 12.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.phone.copy(alpha = 0.9f))
            .border(1.dp, p.line, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("chat-estimate"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawLine(p.line.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ENERGIA TOTAL", style = DietaBotType.labelCaps.copy(letterSpacing = 0.sp), color = p.muted, modifier = Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = p.gold, fontFamily = Jakarta, fontSize = 18.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.025).em)) { append("~${e.kcal}") }
                    withStyle(SpanStyle(color = p.muted, fontFamily = Inter, fontSize = 12.sp)) { append(" kcal") }
                },
            )
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroBox("PROTEÍNA", "${e.p}g P", p.protein, Modifier.weight(1f))
            MacroBox("CARBO", "${e.c}g C", p.carbs, Modifier.weight(1f))
            MacroBox("GORDURA", "${e.g}g G", p.fat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroBox(label: String, value: String, color: Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = DietaBotType.labelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 0.1.em), color = p.muted)
        Text(value, style = DietaBotType.headlineMd.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = color)
    }
}

@Composable
private fun ReceiptCard(item: ChatItem.Receipt) {
    val p = LocalPalette.current
    val tone = if (item.skipped) p.dim else p.good
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("chat-receipt-${item.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(tone.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(if (item.skipped) Icons.Outlined.Remove else Icons.Filled.DoneAll, contentDescription = null, tint = tone, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    buildAnnotatedString {
                        append(
                            when {
                                item.skipped -> "Pulado "
                                item.replaced -> "Atualizado em "
                                else -> "Registrado em "
                            },
                        )
                        withStyle(SpanStyle(fontWeight = FontWeight.W600)) { append(item.slotName) }
                        item.slotTime?.let { withStyle(SpanStyle(color = p.dim)) { append("  ·  $it") } }
                    },
                    style = DietaBotType.bodyMd.copy(fontSize = 13.sp, letterSpacing = 0.sp),
                    color = p.text,
                )
                item.kcal?.takeIf { !item.skipped }?.let {
                    Text(
                        if (item.replaced) "$it kcal" else "+$it kcal",
                        style = DietaBotType.labelMd.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                        color = p.good,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(p.good.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        MemoryChips(MemoryNotice(updated = item.memoryUpdated), horizontal = Alignment.CenterHorizontally)
    }
}

@Composable
private fun Greeting(item: ChatItem.Greeting, ui: ChatUiState) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                "Olá! Descreva o que você comeu ou envie uma foto do prato para estimarmos as calorias e macros.",
                style = DietaBotType.bodyMd.copy(lineHeight = 22.75.sp, letterSpacing = 0.sp),
                color = p.text,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(BotShape)
                    .background(p.card)
                    .border(1.dp, p.line.copy(alpha = 0.3f), BotShape)
                    .padding(14.dp)
                    .testTag("chat-greeting"),
            )
            Text(item.time, style = DietaBotType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.dim, modifier = Modifier.padding(top = 6.dp, start = 8.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(p.phone.copy(alpha = 0.9f))
                .border(1.dp, p.line.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp)
                .testTag("chat-meta"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(p.card).border(1.dp, p.gold.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = p.gold, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Meta calórica de hoje", style = DietaBotType.bodyMd.copy(fontSize = 13.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.text)
                Text(
                    "${formatRemaining(ui.metaRemaining)} / ${formatRemaining(ui.metaTotal)} kcal restantes",
                    style = DietaBotType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
                    color = p.muted,
                )
            }
            val pct = if (ui.metaTotal > 0) (ui.metaRemaining * 100f / ui.metaTotal).roundToInt() else 0
            Text("$pct%", style = DietaBotType.labelMd.copy(fontWeight = FontWeight.W600, letterSpacing = 0.sp), color = p.gold)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingBubble() {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            Modifier
                .widthIn(max = 320.dp)
                .clip(BotShape)
                .background(p.card)
                .border(1.dp, p.line.copy(alpha = 0.6f), BotShape)
                .padding(14.dp)
                .testTag("chat-loading"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingIndicator(color = p.gold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Analisando e calculando estimativa...", style = DietaBotType.bodyMd.copy(fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.muted)
                Text("MICRO & MACRONUTRIENTES", style = DietaBotType.labelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.W400, letterSpacing = 0.05.em), color = p.dim, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text("Dieta Bot AI", style = DietaBotType.labelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
private fun FailedBubble(onRetry: () -> Unit) {
    val p = LocalPalette.current
    Text(
        "Não deu. Toque para tentar de novo.",
        style = DietaBotType.bodyMd.copy(letterSpacing = 0.sp),
        color = p.bad,
        modifier = Modifier
            .clip(BotShape)
            .background(p.card)
            .border(1.dp, p.bad.copy(alpha = 0.4f), BotShape)
            .dietaClick(onClick = onRetry)
            .padding(14.dp)
            .testTag("chat-failed"),
    )
}

// ----------------------------------------------------------------------------- footer

@Composable
private fun SuggestionRow(onPick: (String) -> Unit, onCamera: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("📸", "Tirar foto do prato", tag = "chat-suggestion-photo", onClick = onCamera)
        SUGGESTIONS.forEachIndexed { i, (emoji, text) -> Chip(emoji, text, tag = "chat-suggestion-$i") { onPick(text) } }
    }
}

@Composable
private fun Chip(emoji: String, text: String, enabled: Boolean = true, tag: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(p.card)
            .border(1.dp, p.line.copy(alpha = 0.7f), CircleShape)
            .dietaClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(text, style = DietaBotType.labelMd.copy(fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.muted)
    }
}

@Composable
private fun Footer(
    ui: ChatUiState,
    onComposer: (String) -> Unit,
    onSend: () -> Unit,
    onRecord: (Long, Long) -> Unit,
    onSwap: (Long) -> Unit,
    onAskSkip: (SlotRef) -> Unit,
    onPhoto: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onRecordPlan: (Long) -> Unit,
    onForceEstimate: () -> Unit,
) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, p.phone.copy(alpha = 0.95f), p.phone)))
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (ui.forceEstimate) {
            ForceBar(onForceEstimate)
        } else {
            ui.actions?.let { if (it.plan) PlanBar(it) { onRecordPlan(it.estimateId) } else ActionBar(it, onRecord, onSwap, onAskSkip) }
        }
        Composer(ui, onComposer, onSend, onPhoto, onRemoveAttachment)
    }
}

@Composable
private fun ActionBar(actions: EstimateActions, onRecord: (Long, Long) -> Unit, onSwap: (Long) -> Unit, onAskSkip: (SlotRef) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .testTag("chat-actions"),
    ) {
        val segments = buildList<@Composable BoxScope.() -> Unit> {
            actions.record?.let { slot -> add { Action(Icons.Outlined.CheckCircle, "Gravar ${shortName(slot.name)}", "chat-record", Haptic.Confirm) { onRecord(actions.estimateId, slot.id) } } }
            add { Action(Icons.Outlined.SwapHoriz, "Trocar", "chat-swap") { onSwap(actions.estimateId) } }
            actions.skip?.let { slot -> add { Action(Icons.Outlined.Close, "Pular", "chat-skip") { onAskSkip(slot) } } }
        }
        segments.forEachIndexed { i, segment ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(p.line))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center, content = segment)
        }
    }
}

/** chatR: one Registrar assim in the place of Gravar | Trocar | Pular, same bar. */
@Composable
private fun PlanBar(actions: EstimateActions, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .dietaClick(Haptic.Confirm, onClick = onClick)
            .testTag("chat-record-plan"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Registrar assim", style = DietaBotType.labelMd.copy(fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp), color = p.text, maxLines = 1)
    }
}

/** chatQ: one Forçar estimativa in the place of Gravar | Trocar | Pular, same bar. */
@Composable
private fun ForceBar(onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .dietaClick(Haptic.Light, onClick = onClick)
            .testTag("chat-force-estimate"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.FastForward, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Forçar estimativa", style = DietaBotType.labelMd.copy(fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp), color = p.text, maxLines = 1)
    }
}

/** "Café da manhã" -> "café" (gold: "Gravar café"). */
private fun shortName(name: String) = name.substringBefore(' ').lowercase()

@Composable
private fun BoxScope.Action(icon: ImageVector, label: String, tag: String, haptic: Haptic = Haptic.Light, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxSize().dietaClick(haptic, onClick = onClick).testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = p.muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = DietaBotType.labelMd.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.text, maxLines = 1)
    }
}

@Composable
private fun Composer(ui: ChatUiState, onComposer: (String) -> Unit, onSend: () -> Unit, onPhoto: () -> Unit, onRemoveAttachment: () -> Unit) {
    val p = LocalPalette.current
    // chatA: with an attachment the pill grows into a rounded box, thumbnail on top.
    // chatX: past one line the pill becomes a 24 dp box, buttons at the bottom.
    val attached = ui.attachment != null
    var multiLine by remember { mutableStateOf(false) }
    val metrics = when {
        attached -> ComposerMetrics(ComposerAttachedShape, PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 11.dp), buttonDp = 40, textStartDp = 16, bottomAligned = false)
        multiLine -> ComposerMetrics(ComposerTallShape, PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 13.dp), buttonDp = 40, textStartDp = 12, bottomAligned = true, textEndDp = 4)
        else -> ComposerMetrics(CircleShape, PaddingValues(horizontal = 8.dp, vertical = 6.dp), buttonDp = 36, textStartDp = 12, bottomAligned = false)
    }
    val border = if (ui.composerTooLong) BorderStroke(1.5.dp, p.bad) else BorderStroke(1.dp, p.line)
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(metrics.shape)
                .background(p.card)
                .border(border, metrics.shape)
                .padding(metrics.padding),
        ) {
            ui.attachment?.let { AttachmentThumb(it, onRemoveAttachment) }
            ComposerRow(ui, onComposer, onSend, onPhoto, metrics) { lines -> multiLine = lines > 1 }
        }
        if (ui.composerTooLong) {
            // Start of the text field (chatX gold): container padding + camera.
            val textStart = metrics.padding.calculateStartPadding(LocalLayoutDirection.current) + metrics.buttonDp.dp
            Text(
                "Texto muito longo",
                style = DietaBotType.labelMd.copy(letterSpacing = 0.sp),
                color = p.bad,
                maxLines = 1,
                modifier = Modifier.padding(start = textStart, top = 4.dp).testTag("chat-too-long"),
            )
        }
    }
}

private val ComposerAttachedShape = RoundedCornerShape(28.dp)
private val ComposerTallShape = RoundedCornerShape(24.dp)

private class ComposerMetrics(
    val shape: Shape,
    val padding: PaddingValues,
    val buttonDp: Int,
    val textStartDp: Int,
    val bottomAligned: Boolean,
    val textEndDp: Int = 8,
)

@Composable
private fun ComposerRow(
    ui: ChatUiState,
    onComposer: (String) -> Unit,
    onSend: () -> Unit,
    onPhoto: () -> Unit,
    metrics: ComposerMetrics,
    onLines: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val buttonDp = metrics.buttonDp
    val blocked = ui.composerTooLong
    // Text set from outside (chip, Quase igual) lands with the cursor at its end.
    // Only a change of ui.composer itself counts: while a keystroke travels through the VM, the
    // field is ahead of it and must not be reset.
    var field by remember { mutableStateOf(TextFieldValue(ui.composer)) }
    val external = remember { arrayOf(ui.composer) }
    if (ui.composer != external[0]) {
        external[0] = ui.composer
        if (ui.composer != field.text) field = TextFieldValue(ui.composer, TextRange(ui.composer.length))
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(ui.focusComposer) {
        if (ui.focusComposer > 0) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = if (metrics.bottomAligned) Alignment.Bottom else Alignment.CenterVertically) {
        Box(
            Modifier
                .size(buttonDp.dp)
                .clip(CircleShape)
                .background(p.surf2)
                .border(1.dp, p.text.copy(alpha = 0.05f), CircleShape)
                .dietaClick(enabled = ui.canAttach, onClick = onPhoto)
                .testTag("chat-photo"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Enviar foto", tint = if (blocked) p.dim else p.muted, modifier = Modifier.size(19.dp))
        }
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                if (it.text != ui.composer) onComposer(it.text)
            },
            // Enter inserts a line break; only the arrow sends (A18).
            singleLine = false,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
            textStyle = DietaBotType.bodyMd.copy(letterSpacing = 0.sp, color = p.text),
            cursorBrush = SolidColor(p.gold),
            onTextLayout = { onLines(it.lineCount) },
            modifier = Modifier.weight(1f).padding(start = metrics.textStartDp.dp, end = metrics.textEndDp.dp).focusRequester(focus).testTag("chat-input"),
            decorationBox = { inner ->
                Box {
                    if (ui.composer.isEmpty()) {
                        Text("Descreva sua refeição ou envie foto...", style = DietaBotType.bodyMd.copy(letterSpacing = 0.sp), color = p.dim, maxLines = 1)
                    }
                    inner()
                }
            },
        )
        Box(
            Modifier
                .size(buttonDp.dp)
                .clip(CircleShape)
                .background(if (blocked) p.surf2 else p.gold)
                .dietaClick(enabled = ui.canSend, onClick = onSend)
                .testTag("chat-send"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Enviar", tint = if (blocked) p.dim else p.onGold, modifier = Modifier.size(20.dp))
        }
    }
}

// ----------------------------------------------------------------------------- overlays

@Composable
private fun Scrim(scrimColor: Color, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(scrimColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
}

@Composable
private fun BoxScope.SlotSheet(ui: ChatUiState, onSelect: (Long) -> Unit, onConfirm: () -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    BackHandler(onBack = onClose)
    Scrim(Color.Black.copy(alpha = if (p.isDark) 0.6f else 0.35f), onClose)
    val shape = RoundedCornerShape(topStart = DietaBotMeasure.sheetTopDp.dp, topEnd = DietaBotMeasure.sheetTopDp.dp)
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(shape)
            // Stitch: #14171d dark (panel), #faf9f6 light (phone).
            .background(if (p.isDark) p.panel else p.phone)
            .border(1.dp, p.line.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            // Stitch: ~48 dp under "Cancelar" (pb-8 + home pill). Gesture nav is 24 dp; never less.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets(bottom = 24.dp)))
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
            .testTag("chat-sheet"),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(44.dp).height(5.dp).clip(CircleShape).background(p.dim))
        Text("Selecione a refeição", style = DietaBotType.headlineMd.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.text, modifier = Modifier.padding(top = 16.dp))
        Text("Escolha o momento do dia para salvar este registro:", style = DietaBotType.bodyMd.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ui.slots.forEach { slot -> SheetRow(slot, slot.id == ui.sheetSelection, slot.id == ui.currentSlotId) { onSelect(slot.id) } }
        }
        Row(
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(p.gold.copy(alpha = if (ui.sheetSelection != null) 1f else 0.5f))
                .dietaClick(Haptic.Confirm, enabled = ui.sheetSelection != null, onClick = onConfirm)
                .testTag("chat-sheet-confirm"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Confirmar refeição", style = DietaBotType.labelLg.copy(fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.onGold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = p.onGold, modifier = Modifier.size(18.dp))
        }
        Text(
            "Cancelar",
            style = DietaBotType.labelMd.copy(letterSpacing = 0.05.em),
            color = p.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).clip(CircleShape).dietaClick(onClick = onClose).padding(horizontal = 12.dp, vertical = 2.dp).testTag("chat-sheet-cancel"),
        )
    }
}

@Composable
private fun SheetRow(slot: SlotRef, selected: Boolean, current: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (selected) p.gold.copy(alpha = 0.16f) else p.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) p.gold else p.line.copy(alpha = 0.6f), shape)
            .dietaClick(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag("chat-sheet-slot-${slot.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) p.gold else p.panel)
                .border(1.dp, if (selected) p.gold else p.line.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sheetIcon(slot.minutes), contentDescription = null, tint = if (selected) p.onGold else p.muted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(slot.name, style = DietaBotType.labelLg.copy(fontWeight = if (selected) FontWeight.W600 else FontWeight.W500), color = p.text)
            Text(
                if (current) "${slot.time} (atual)" else slot.time,
                style = DietaBotType.labelCaps.copy(fontWeight = if (selected) FontWeight.W500 else FontWeight.W400, letterSpacing = 0.sp),
                color = if (selected) p.gold else p.muted,
            )
        }
        if (selected) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(p.gold), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = p.onGold, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(Modifier.size(20.dp).border(1.dp, p.line, CircleShape))
        }
    }
}

private fun sheetIcon(minutes: Int): ImageVector = when (SlotSuggestions.bandOf(minutes)) {
    SlotBand.BREAKFAST -> Icons.Outlined.Coffee
    SlotBand.MORNING_SNACK, SlotBand.AFTERNOON_SNACK -> Icons.Outlined.BakeryDining
    SlotBand.LUNCH -> Icons.Outlined.LunchDining
    SlotBand.DINNER -> Icons.Outlined.DinnerDining
    SlotBand.NIGHT -> Icons.Outlined.Bedtime
}

@Composable
private fun BoxScope.SkipDialog(slot: SlotRef, onConfirm: () -> Unit, onCancel: () -> Unit) = ConfirmDialog(
    time = slot.time,
    title = "Deseja pular o ${slot.name}?",
    body = "Nenhuma caloria será somada hoje. Se você mudar de ideia, ainda poderá registrar alimentos nessa refeição mais tarde.",
    primaryIcon = Icons.Outlined.FastForward,
    primaryLabel = "Pular refeição",
    secondaryLabel = "Cancelar",
    tag = "chat-skip",
    onPrimary = onConfirm,
    onSecondary = onCancel,
    onDismiss = onCancel,
)

/** ADR-017, same chatP dialog. Outra refeição opens Trocar; back and scrim only close. */
@Composable
private fun BoxScope.ReplaceDialog(confirm: ReplaceConfirm, onReplace: () -> Unit, onElsewhere: () -> Unit, onCancel: () -> Unit) = ConfirmDialog(
    time = confirm.slot.time,
    title = "Substituir ${confirm.slot.name}?",
    body = "${confirm.slot.name} tem ${confirm.oldKcal} kcal. Fica com ${confirm.newKcal} kcal.",
    primaryIcon = Icons.Outlined.SwapHoriz,
    primaryLabel = "Substituir",
    secondaryLabel = "Outra refeição",
    tag = "chat-replace",
    onPrimary = onReplace,
    onSecondary = onElsewhere,
    onDismiss = onCancel,
)

/** Stitch chatP layout: icon + slot time, title, body, CTA pill, secondary pill. Tags {tag}-dialog/-confirm/-cancel. */
@Composable
private fun BoxScope.ConfirmDialog(
    time: String,
    title: String,
    body: String,
    primaryIcon: ImageVector,
    primaryLabel: String,
    secondaryLabel: String,
    tag: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    BackHandler(onBack = onDismiss)
    // Stitch chatP: #07090d/80 dark, text colour/40 light.
    Scrim(if (p.isDark) Color(0xFF07090D).copy(alpha = 0.8f) else p.text.copy(alpha = 0.4f), onDismiss)
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (p.isDark) p.card else p.surf)
            .border(1.dp, p.line, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp)
            .testTag("$tag-dialog"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(p.surf2).border(1.dp, p.line, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Fastfood, contentDescription = null, tint = if (p.isDark) p.gold else p.muted, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(CircleShape).background(p.surf2).border(1.dp, p.line, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = p.muted, modifier = Modifier.size(14.dp))
                Text(time, style = DietaBotType.labelCaps.copy(letterSpacing = 0.05.em), color = p.muted)
            }
        }
        Text(
            title,
            style = DietaBotType.bodyLg.copy(fontSize = 18.sp, lineHeight = 24.75.sp, fontWeight = if (p.isDark) FontWeight.W600 else FontWeight.W700, letterSpacing = (-0.025).em),
            color = p.text,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            body,
            style = DietaBotType.bodyMd.copy(lineHeight = 22.75.sp, letterSpacing = 0.sp),
            color = p.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(p.ctaBg)
                .dietaClick(Haptic.Confirm, onClick = onPrimary)
                .testTag("$tag-confirm"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(primaryIcon, contentDescription = null, tint = p.ctaText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(primaryLabel, style = DietaBotType.labelLg.copy(fontSize = 15.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.ctaText)
        }
        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(p.surf2)
                .border(1.dp, p.line, CircleShape)
                .dietaClick(onClick = onSecondary)
                .testTag("$tag-cancel"),
            contentAlignment = Alignment.Center,
        ) {
            Text(secondaryLabel, style = DietaBotType.labelLg.copy(fontSize = 15.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.text)
        }
    }
}

// ----------------------------------------------------------------------------- photo (A6)

@Composable
private fun BoxScope.PhotoSheet(onCamera: () -> Unit, onGallery: () -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    BackHandler(onBack = onClose)
    Scrim(Color.Black.copy(alpha = if (p.isDark) 0.6f else 0.35f), onClose)
    val shape = RoundedCornerShape(topStart = DietaBotMeasure.sheetTopDp.dp, topEnd = DietaBotMeasure.sheetTopDp.dp)
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(shape)
            .background(if (p.isDark) p.panel else p.phone)
            .border(1.dp, p.line.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets(bottom = 24.dp)))
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
            .testTag("chat-photo-sheet"),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(44.dp).height(5.dp).clip(CircleShape).background(p.dim))
        Text("Enviar foto do prato", style = DietaBotType.headlineMd.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.text, modifier = Modifier.padding(top = 16.dp))
        Text("O texto digitado vai junto como legenda. Até 16 MB.", style = DietaBotType.bodyMd.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoSourceRow(Icons.Outlined.PhotoCamera, "Tirar foto", "chat-photo-camera", onCamera)
            PhotoSourceRow(Icons.Outlined.PhotoLibrary, "Escolher da galeria", "chat-photo-gallery", onGallery)
        }
        Text(
            "Cancelar",
            style = DietaBotType.labelMd.copy(letterSpacing = 0.05.em),
            color = p.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clip(CircleShape).dietaClick(onClick = onClose).padding(horizontal = 12.dp, vertical = 2.dp).testTag("chat-photo-cancel"),
        )
    }
}

@Composable
private fun PhotoSourceRow(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.line.copy(alpha = 0.6f), shape)
            .dietaClick(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(p.panel).border(1.dp, p.line.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(label, style = DietaBotType.labelLg.copy(fontWeight = FontWeight.W500), color = p.text)
    }
}

/** One-shot message above the composer ("Foto grande demais."). */
@Composable
private fun Notice(text: String, onShown: () -> Unit) {
    val p = LocalPalette.current
    LaunchedEffect(text) {
        delay(NOTICE_MS)
        onShown()
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = DietaBotType.labelLg.copy(fontWeight = FontWeight.W500, letterSpacing = 0.sp),
            color = p.bad,
            modifier = Modifier
                .clip(CircleShape)
                .background(p.bad.copy(alpha = 0.12f))
                .border(1.dp, p.bad.copy(alpha = 0.3f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("chat-notice"),
        )
    }
}

private const val NOTICE_MS = 3_000L
