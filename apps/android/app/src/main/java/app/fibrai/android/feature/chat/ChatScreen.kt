package app.fibrai.android.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.fibrai.android.core.designsystem.aero.AeroCopyToast
import app.fibrai.android.core.designsystem.aero.AeroSelectionBar
import app.fibrai.android.core.designsystem.aero.aeroSelectedBubble
import app.fibrai.android.core.designsystem.aero.aeroSelectedRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.fibrai.android.domain.ReplyMarkup
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.dietaClick
import app.fibrai.android.core.designsystem.formatRemaining
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroActionBar
import app.fibrai.android.core.designsystem.aero.AeroChoiceBar
import app.fibrai.android.core.designsystem.aero.AeroReplyBlocks
import app.fibrai.android.core.designsystem.aero.AeroBotLabel
import app.fibrai.android.core.designsystem.aero.AeroBubbleSpec
import app.fibrai.android.core.designsystem.aero.AeroChatBubble
import app.fibrai.android.core.designsystem.aero.AeroChipLog
import app.fibrai.android.core.designsystem.aero.AeroChipTone
import app.fibrai.android.core.designsystem.aero.AeroDateChip
import app.fibrai.android.core.designsystem.aero.AeroDimens
import app.fibrai.android.core.designsystem.aero.AeroIcon
import app.fibrai.android.core.designsystem.aero.AeroIconButton
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroLoader
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroPageBubbles
import app.fibrai.android.core.designsystem.aero.AeroScrim
import app.fibrai.android.core.designsystem.aero.AeroSheet
import app.fibrai.android.core.designsystem.aero.AeroSlotPickRow
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.TaliAvatar
import app.fibrai.android.core.designsystem.aero.TaliAvatarSize
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.aeroGloss
import app.fibrai.android.core.designsystem.aero.cased
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SlotBand
import app.fibrai.android.domain.SlotSuggestions
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
    onSheetSelect: (Long) -> Unit,
    onSheetConfirm: () -> Unit,
    onSheetClose: () -> Unit,
    /** chatE: Registrar on an `ask` estimate (A34). */
    onRegister: (estimateId: Long) -> Unit = {},
    /** chatU: Substituir | Outra refeição below an answer whose slot already has a record. */
    onReplaceConfirm: (estimateId: Long) -> Unit = {},
    onReplaceElsewhere: (estimateId: Long) -> Unit = {},
    /** chatG / chatF / chatD: Desfazer · Excluir · Trocar refeição · Editar on a receipt. */
    onReceiptAction: (receiptId: Long, action: ReceiptAction) -> Unit = { _, _ -> },
    /** chatU below a receipt: Trocar refeição into a slot with a record. */
    onMoveConfirm: () -> Unit = {},
    onMoveElsewhere: () -> Unit = {},
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
    /** A32: the thread reached its oldest drawn items. */
    onLoadOlder: () -> Unit = {},
    /** chatI (A47): Adicionar | Escolher outra refeição below an addition to a meal with a record. */
    onAdditionConfirm: (estimateId: Long) -> Unit = {},
    onAdditionElsewhere: (estimateId: Long) -> Unit = {},
    /** chatIC (A47): Atualizar | Cancelar below a revision. */
    onRevisionConfirm: (estimateId: Long) -> Unit = {},
    onRevisionCancel: (estimateId: Long) -> Unit = {},
    /** chatSD (A59): Excluir e pular | Manter registro below an answer that skipped a meal with a record. */
    onSkipDelete: (answerId: Long, slotId: Long) -> Unit = { _, _ -> },
    onSkipKeep: (answerId: Long, slotId: Long) -> Unit = { _, _ -> },
    /** chatRB (A60 part A): Pode passar | Ajustar para caber on an over-budget plan. */
    onBudgetOverOk: (estimateId: Long) -> Unit = {},
    onBudgetFit: (estimateId: Long) -> Unit = {},
    /** chatR (A60 part D): Reservar para o {slot}. */
    onReserve: (estimateId: Long) -> Unit = {},
    /** chatRK (A68): Salvar receita under a cooking plan. */
    onSaveRecipe: (estimateId: Long) -> Unit = {},
    /** chatO (A67): Registrar | Reservar inside an option of a plan. */
    onOptionRecord: (estimateId: Long, optionId: String) -> Unit = { _, _ -> },
    onOptionReserve: (estimateId: Long, optionId: String) -> Unit = { _, _ -> },
    /** chatCP (A61 part B): long press on a text bubble, a tap on one while selecting, ✕ / back, Copiar. */
    onLongPress: (key: String) -> Unit = {},
    onSelectTap: (key: String) -> Unit = {},
    onSelectionClose: () -> Unit = {},
    onCopy: () -> Unit = {},
    /** chatCC (A61 part B): the app's own copy confirmation went away. */
    onCopiedShown: () -> Unit = {},
) {
    // A32: camera button and photo chip close the keyboard first, so the photo sheet shows whole.
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val photo = {
        keyboard?.hide()
        focusManager.clearFocus()
        onPhoto()
    }
    val camera = {
        keyboard?.hide()
        focusManager.clearFocus()
        onCamera()
    }
    // A37: send closes the keyboard, so the answer and the receipt show whole.
    val send = {
        keyboard?.hide()
        focusManager.clearFocus()
        onSend()
    }
    // A42: the Chat on Aero (Figma `Design`, Chat · D5): page gradient, 20 dp margins, 24 dp top and bottom, 16 dp
    // between header, thread, chips and footer, the frame's edge bubbles.
    AeroPage(Modifier.fillMaxSize().testTag("chat")) {
        val overlay = ui.sheetFor != null || ui.photoSheet
        // Figma: layer blur 8 behind the sheets.
        Column(
            Modifier
                .fillMaxSize()
                .then(if (overlay) Modifier.blur(8.dp) else Modifier)
                .statusBarsPadding()
                .imePadding(),
        ) {
            // chatCP: while messages are selected, the selection bar takes the header's place.
            if (ui.selected.isEmpty()) Header(onBack) else AeroSelectionBar(ui.selected.size, onSelectionClose, onCopy, Modifier.testTag("chat-selection"))
            // A tap on an action ends the selection first (ADR-048).
            val end = { if (ui.selected.isNotEmpty()) onSelectionClose() }
            val record = RecordCallbacks(
                { end(); onReplaceConfirm(it) }, { end(); onReplaceElsewhere(it) }, { id, a -> end(); onReceiptAction(id, a) },
                { end(); onMoveConfirm() }, { end(); onMoveElsewhere() }, { end(); onAdditionConfirm(it) }, { end(); onAdditionElsewhere(it) },
                { end(); onRevisionConfirm(it) }, { end(); onRevisionCancel(it) }, { a, s -> end(); onSkipDelete(a, s) }, { a, s -> end(); onSkipKeep(a, s) },
                { e, o -> end(); onOptionRecord(e, o) }, { e, o -> end(); onOptionReserve(e, o) },
            )
            val answer = AnswerCallbacks(
                { end(); onRegister(it) }, { end(); onRecordPlan(it) }, { end(); onForceEstimate() },
                { end(); onBudgetOverOk(it) }, { end(); onBudgetFit(it) }, { end(); onReserve(it) }, { end(); onSaveRecipe(it) },
            )
            val select = SelectCallbacks(onLongPress, onSelectTap)
            if (ui.loaded) Thread(ui, onRetry, onRoutineRecord, onRoutineEdit, onLoadOlder, record, answer, select, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            ui.notice?.let { Notice(it, onNoticeShown) }
            ui.copied?.let { CopyToast(it, onCopiedShown) }
            // chatA: with a photo attached the chips go away (they would compete with it).
            if (ui.emptyDay && ui.attachment == null) SuggestionRow(onComposer, camera)
            Footer(ui, onComposer, send, photo, onRemoveAttachment)
        }
        AeroPageBubbles(ChatBubbles, null, Modifier.statusBarsPadding())
        // The sheets register their back after this one, so an open sheet closes first.
        BackHandler(enabled = ui.selected.isNotEmpty(), onBack = onSelectionClose)
        if (ui.sheetFor != null) SlotSheet(ui, onSheetSelect, onSheetConfirm, onSheetClose)
        if (ui.photoSheet) PhotoSheet(onCamera, onGallery, onPhotoSheetClose)
    }
}

/** Edge bubbles of the Chat frames. */
private val ChatBubbles = listOf(
    AeroBubbleSpec(200.dp, (-50).dp, 80.dp),
    AeroBubbleSpec(372.dp, 560.dp, 36.dp),
    AeroBubbleSpec((-18).dp, 420.dp, 36.dp),
)

/** The record taps of thread items (A34), one object instead of five parameters per level. */
private class RecordCallbacks(
    val onReplaceConfirm: (Long) -> Unit,
    val onReplaceElsewhere: (Long) -> Unit,
    val onReceiptAction: (Long, ReceiptAction) -> Unit,
    val onMoveConfirm: () -> Unit,
    val onMoveElsewhere: () -> Unit,
    val onAdditionConfirm: (Long) -> Unit,
    val onAdditionElsewhere: (Long) -> Unit,
    val onRevisionConfirm: (Long) -> Unit,
    val onRevisionCancel: (Long) -> Unit,
    val onSkipDelete: (Long, Long) -> Unit,
    val onSkipKeep: (Long, Long) -> Unit,
    val onOptionRecord: (Long, String) -> Unit = { _, _ -> },
    val onOptionReserve: (Long, String) -> Unit = { _, _ -> },
)

/** The action taps of the latest answer (A61: drawn in the thread, under their message). */
private class AnswerCallbacks(
    val onRegister: (Long) -> Unit,
    val onRecordPlan: (Long) -> Unit,
    val onForceEstimate: () -> Unit,
    val onBudgetOverOk: (Long) -> Unit,
    val onBudgetFit: (Long) -> Unit,
    val onReserve: (Long) -> Unit,
    val onSaveRecipe: (Long) -> Unit = {},
)

/** A61 part B: the long press and the selection taps of the text bubbles. */
private class SelectCallbacks(val onLongPress: (String) -> Unit, val onTap: (String) -> Unit)

// ----------------------------------------------------------------------------- header

@Composable
private fun Header(onBack: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    // Chat/Header: back, title with the accent dot and subtitle, and the 44 dp space of the dropped tune button.
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp).height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIconButton(AeroIconName.CaretLeft, onBack, contentDescription = "Voltar", modifier = Modifier.testTag("chat-back"))
        // D10: the Header avatar next to "Tali" + the accent dot over the subtitle, left-aligned to it, the group centred.
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            TaliAvatar(TaliAvatarSize.Header)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AeroText("Tali", style = type.bodyStrong.copy(color = c.textPrimary))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.accentDefault))
                }
                AeroText(AeroTextTokens.labelSection.cased("Assistente de refeições"), style = type.labelSection.copy(color = c.textMuted), maxLines = 1)
            }
        }
        Spacer(Modifier.size(44.dp))
    }
}

// ----------------------------------------------------------------------------- thread

/**
 * A32: reversed list over the newest-first items, so the first frame is already the bottom of the
 * conversation. Older pages are added at the far end and do not move the visible items.
 */
@Composable
private fun Thread(
    ui: ChatUiState,
    onRetry: () -> Unit,
    onRoutineRecord: () -> Unit,
    onRoutineEdit: () -> Unit,
    onLoadOlder: () -> Unit,
    record: RecordCallbacks,
    answer: AnswerCallbacks,
    select: SelectCallbacks,
    modifier: Modifier,
) {
    val list = rememberLazyListState()
    val items = remember(ui.newestFirst, ui.actions, ui.forceEstimate) { threadRows(ui) }
    val newest = items.firstOrNull()
    val seen = remember { arrayOf(newest?.key) }
    LaunchedEffect(newest?.key) {
        if (newest == null || newest.key == seen[0]) return@LaunchedEffect
        // A34: one answer can add several items (the answer and its receipt or Substituir): the list keeps
        // the old newest item in place by key, so "at the bottom" counts the items added in front of it.
        val added = items.indexOfFirst { it.key == seen[0] }.coerceAtLeast(0)
        seen[0] = newest.key
        // Follows only from the bottom or for the user's own send; scrolled up, nothing jumps.
        val own = (newest as? ThreadRow.Item)?.item.let { it is ChatItem.Loading || it is ChatItem.Thinking || it is ChatItem.User && it.pending }
        if (own || list.firstVisibleItemIndex <= added + 1) list.animateScrollToItem(0)
    }
    val nearOldest by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - 1 - OLDER_THRESHOLD
        }
    }
    LaunchedEffect(nearOldest, ui.hasOlder, ui.loadingOlder) {
        if (nearOldest && ui.hasOlder && !ui.loadingOlder) onLoadOlder()
    }
    LazyColumn(
        modifier.fillMaxWidth().testTag("chat-thread"),
        state = list,
        reverseLayout = true,
        // A short thread still starts at the top, as in the golds.
        verticalArrangement = Arrangement.Top,
        // Bottom 32 dp: the frame's two 16 dp gaps around its empty spacer when the thread fills the screen.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.key }) { i, row ->
            // 16 dp between items; the question hugs its estimate (chatE); the actions sit 12 dp under their
            // message (D20). The oldest drawn item has none.
            val item = (row as? ThreadRow.Item)?.item
            val gap = when {
                i == items.lastIndex -> 0.dp
                row is ThreadRow.Actions -> 12.dp
                item is ChatItem.Question && !item.standalone -> 8.dp
                else -> 16.dp
            }
            Box(Modifier.padding(top = gap)) {
                when {
                    item == null -> AnswerActions(ui, answer)
                    item.copyText != null -> SelectableRow(item.key, ui.selected, select) { selected ->
                        ThreadItem(item, ui, onRetry, onRoutineRecord, onRoutineEdit, record, selected)
                    }
                    else -> ThreadItem(item, ui, onRetry, onRoutineRecord, onRoutineEdit, record)
                }
            }
        }
        if (ui.loadingOlder) {
            item(key = "loading-older") {
                Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                    AeroLoader(Modifier.testTag("chat-loading-older"), size = 28.dp)
                }
            }
        }
    }
}

/** Older page requested when the oldest drawn item is this close to the end of the list. */
private const val OLDER_THRESHOLD = 5

/**
 * A61 part B (chatCP): a text bubble row. A long press selects it (platform long-press haptic); while selecting, a tap
 * adds or removes it. Selected: the surface/selected band across the screen behind the row.
 */
@Composable
private fun SelectableRow(key: String, selection: Set<String>, select: SelectCallbacks, content: @Composable (selected: Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val selected = key in selection
    val selecting = selection.isNotEmpty()
    val onLong by rememberUpdatedState {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        select.onLongPress(key)
    }
    val onTap by rememberUpdatedState { if (selecting) select.onTap(key) }
    // Plain gestures, not a clickable: the bubble's own nodes (tags, chips, links) keep their semantics unmerged, and the
    // buttons inside the row consume their taps first.
    Box(
        Modifier
            .fillMaxWidth()
            .aeroSelectedRow(selected, Aero.colors.surfaceSelected)
            .pointerInput(key) { detectTapGestures(onLongPress = { onLong() }, onTap = { onTap() }) }
            .semantics {
                this.selected = selected
                onLongClick("Selecionar mensagem") { onLong(); true }
                if (selecting) onClick { onTap(); true }
            }
            .testTag("chat-row-$key"),
    ) { content(selected) }
}

/** A row of the thread: a [ChatItem], or the action stack of the latest answer (A61, ADR-048 decision 1). */
private sealed interface ThreadRow {
    val key: String

    class Item(val item: ChatItem) : ThreadRow {
        override val key get() = item.key
    }

    class Actions(override val key: String) : ThreadRow
}

/**
 * [ChatUiState.newestFirst] with the action stack right under its message: Forçar estimativa under the latest
 * question, an estimate's actions under its answer (and its follow-up question, chatE). Its own key, so it scrolls
 * and recomposes with the thread.
 */
private fun threadRows(ui: ChatUiState): List<ThreadRow> {
    val items = ui.newestFirst
    val rows = items.mapTo(ArrayList<ThreadRow>(items.size + 1)) { ThreadRow.Item(it) }
    val actions = ui.actions
    val (key, at) = when {
        ui.forceEstimate -> {
            val q = items.indexOfFirst { it is ChatItem.Question && it.standalone }
            "force-${(items.getOrNull(q) as? ChatItem.Question)?.messageId}" to q
        }
        actions != null -> {
            val own = setOf("a-${actions.estimateId}", "q-${actions.estimateId}")
            "actions-${actions.estimateId}" to items.indexOfFirst { it.key in own }
        }
        else -> return rows
    }
    rows.add(at.coerceAtLeast(0), ThreadRow.Actions(key))
    return rows
}

/** The stack of the latest answer: Forçar estimativa, Registrar, Registrar assim + Reservar, or the two pills. */
@Composable
private fun AnswerActions(ui: ChatUiState, on: AnswerCallbacks) {
    Column(Modifier.fillMaxWidth().testTag("chat-answer-actions"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val actions = ui.actions
        when {
            ui.forceEstimate -> ForceBar(on.onForceEstimate)
            actions == null -> Unit
            actions.choice != null -> AeroChoiceBar(
                "Pode passar", "Ajustar para caber", { on.onBudgetOverOk(actions.estimateId) }, { on.onBudgetFit(actions.estimateId) },
                leftTag = "chat-budget-over-ok", rightTag = "chat-budget-fit",
            )
            actions.plan -> {
                PlanBar(actions) { on.onRecordPlan(actions.estimateId) }
                actions.reserve?.let { slot ->
                    AeroActionBar("Reservar para o ${slot.name}", AeroIconName.CalendarCheck, { on.onReserve(actions.estimateId) }, Modifier.testTag("chat-reserve"))
                }
                // A68 (chatRK): a cooking plan not saved yet.
                if (actions.saveRecipe) {
                    AeroActionBar("Salvar receita", AeroIconName.BookmarkSimple, { on.onSaveRecipe(actions.estimateId) }, Modifier.testTag("chat-save-recipe"))
                }
            }
            else -> RegisterBar { on.onRegister(actions.estimateId) }
        }
    }
}

@Composable
private fun ThreadItem(
    item: ChatItem,
    ui: ChatUiState,
    onRetry: () -> Unit,
    onRoutineRecord: () -> Unit,
    onRoutineEdit: () -> Unit,
    record: RecordCallbacks,
    selected: Boolean = false,
) {
    when (item) {
        is ChatItem.DateSeparator -> DatePill(item.label)
        is ChatItem.User -> item.photoPath?.let { PhotoBubble(item, it, selected) } ?: UserBubble(item, selected)
        is ChatItem.Assistant -> AssistantBubble(item, selected, record)
        is ChatItem.Question -> QuestionBubble(item, selected)
        is ChatItem.Receipt -> ReceiptCard(item, { record.onReceiptAction(item.id, it) }, record.onMoveConfirm, record.onMoveElsewhere)
        is ChatItem.ReplacePrompt -> ReplaceCard(
            item.confirm,
            tag = "chat-replace",
            onReplace = { record.onReplaceConfirm(item.estimateId) },
            onElsewhere = { record.onReplaceElsewhere(item.estimateId) },
        )
        is ChatItem.AdditionPrompt -> AdditionCard(
            item.confirm,
            onAdd = { record.onAdditionConfirm(item.estimateId) },
            onElsewhere = { record.onAdditionElsewhere(item.estimateId) },
        )
        is ChatItem.RevisionPrompt -> RevisionCard(
            item.confirm,
            onUpdate = { record.onRevisionConfirm(item.estimateId) },
            onCancel = { record.onRevisionCancel(item.estimateId) },
        )
        is ChatItem.SkipDeletePrompt -> SkipDeleteCard(
            item.confirm,
            onDelete = { record.onSkipDelete(item.answerId, item.confirm.slot.id) },
            onKeep = { record.onSkipKeep(item.answerId, item.confirm.slot.id) },
        )
        is ChatItem.SkipMark -> SkipMarkLabel(item.kept, Modifier.padding(start = 8.dp))
        is ChatItem.Greeting -> Greeting(item, ui)
        ChatItem.Loading -> LoadingBubble()
        ChatItem.Thinking -> LoadingBubble(thinking = true)
        ChatItem.Failed -> FailedBubble(onRetry)
        is ChatItem.Routine -> RoutineSuggestionCard(item.suggestion, onRoutineRecord, onRoutineEdit)
    }
}

@Composable
private fun DatePill(label: String) = AeroDateChip(label)

/** Chat/Bubble Sender=User: tinted glass at the right, time and read ticks inside. */
@Composable
private fun UserBubble(item: ChatItem.User, selected: Boolean = false) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        AeroChatBubble(
            item.text, item.time, fromUser = true, maxWidth = UserBubbleMax,
            modifier = Modifier.aeroSelectedBubble(selected, UserTailShape).testTag("chat-user-${item.id}"),
        )
    }
}

/** Chat/Bubble Sender=User: radius 20, 6 at the bottom right (the selection ring follows it). */
private val UserTailShape = RoundedCornerShape(topStart = AeroDimens.radiusCard, topEnd = AeroDimens.radiusCard, bottomEnd = 6.dp, bottomStart = AeroDimens.radiusCard)

/** Glass bot bubble and question: radius 20, 6 at the bottom left. */
private val BotTailShape = RoundedCornerShape(topStart = AeroDimens.radiusCard, topEnd = AeroDimens.radiusCard, bottomEnd = AeroDimens.radiusCard, bottomStart = 6.dp)

/** User bubbles take at most 288 of the 350 dp thread; bot bubbles are 308 dp. */
private val UserBubbleMax = 288.dp
private const val BOT_FRACTION = 0.88f

@Composable
private fun AiLabel() = AeroBotLabel(Modifier.padding(bottom = 8.dp))

/** Glass bot bubble (radius 20, 6 at the bottom left), 18 x 14 dp padding inside the 1 dp border. */
@Composable
private fun BotBubble(modifier: Modifier = Modifier, gap: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    val r = AeroDimens.radiusCard
    Column(
        modifier
            .fillMaxWidth()
            .aeroGlass(RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = 6.dp))
            .padding(horizontal = 19.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
private fun BubbleTime(time: String, modifier: Modifier = Modifier) {
    AeroText(time, modifier, style = Aero.type.caption.copy(color = Aero.colors.textDim))
}

@Composable
private fun AssistantBubble(item: ChatItem.Assistant, selected: Boolean = false, record: RecordCallbacks? = null) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth(BOT_FRACTION), horizontalAlignment = Alignment.Start) {
        AiLabel()
        BotBubble(Modifier.aeroSelectedBubble(selected, BotTailShape).testTag("chat-bot-${item.id}")) {
            val blocks = item.blocks
            if (item.options.isNotEmpty()) {
                // A70 (chatO, ADR-056 § 8): the decision line, one block per option with its own actions, the rest of the
                // reply, then the day with the chosen option.
                OptionReplyText(item.text)
                item.options.forEach { option ->
                    PlanOptionCard(option, { record?.onOptionRecord?.invoke(item.id, option.id) }, { record?.onOptionReserve?.invoke(item.id, option.id) })
                }
                OptionReplyText(item.trailing)
                item.plan?.let { PlanPanel(it) }
            } else if (item.plan != null) {
                if (blocks != null) AeroReplyBlocks(blocks, macroDecor()) else if (item.text.isNotBlank()) PlanText(item.text)
                PlanPanel(item.plan)
            } else if (item.prose) {
                // A formatted reply carries its own emphasis (D17): no accent highlight of the item names.
                if (blocks != null) {
                    AeroReplyBlocks(blocks)
                } else {
                    AeroText(highlighted(item.text, item.highlights, c.accentDefault), style = type.body.copy(color = c.textPrimary))
                }
            }
            item.estimate?.let { e ->
                EstimateCard(e)
                e.slotQuestion?.let { q -> AeroText(slotQuestion(q, c.textPrimary), style = type.body.copy(color = c.textMuted)) }
            }
            // With a follow-up question the time moves under the question bubble (chatE); with memory chips, under them (chatM).
            if (item.estimate?.question.isNullOrBlank() && !item.memory.any) BubbleTime(item.time, Modifier.align(Alignment.End))
        }
        MemoryChips(item.memory)
        if (item.memory.any && item.estimate?.question.isNullOrBlank()) BubbleTime(item.time, Modifier.padding(top = 8.dp, start = 4.dp))
        item.noted.forEach { NotedLabel(it, Modifier.padding(top = 6.dp, start = 8.dp)) }
        if (item.notRecorded) NotRecordedLabel(Modifier.padding(top = 6.dp, start = 8.dp))
        item.budget?.let { BudgetLines(it, Modifier.padding(top = 8.dp)) }
        item.reservedFor?.let { ReservedLabel(it, Modifier.padding(top = 8.dp, start = 2.dp)) }
        item.projection?.let { ProjectionLabel(it, Modifier.padding(top = 6.dp, start = 8.dp)) }
    }
}

/**
 * Chat/Question: the clarifying question in its own glass bubble, accent bar on the left, question icon, Body/Strong
 * text and the time inside. Standalone (chatQ): a question before the estimate, with the bot label above.
 */
@Composable
private fun QuestionBubble(item: ChatItem.Question, selected: Boolean = false) {
    val c = Aero.colors
    val type = Aero.type
    val r = AeroDimens.radiusCard
    val shape = RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = 6.dp)
    Column(Modifier.fillMaxWidth(BOT_FRACTION), horizontalAlignment = Alignment.Start) {
        if (item.standalone) AiLabel()
        Column(
            Modifier
                .fillMaxWidth()
                .aeroSelectedBubble(selected, shape)
                .aeroGlass(shape)
                .drawBehind { drawRect(c.accentDefault, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = 20.dp, end = 17.dp, top = 17.dp, bottom = 15.dp)
                .testTag("chat-question"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Top: a question of several lines keeps the icon on its first line.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                AeroIcon(AeroIconName.Question, c.accentDefault, size = 20.dp, modifier = Modifier.padding(top = 2.dp))
                AeroText(item.text, style = type.bodyStrong.copy(color = c.textPrimary))
            }
            BubbleTime(item.time)
        }
        MemoryChips(item.memory)
    }
}

/** "Deseja registrar essa refeição no {slot}?" with the slot in Body/Strong primary. */
private fun slotQuestion(q: String, emphasis: Color): AnnotatedString = buildAnnotatedString {
    val start = q.indexOf(" no ").takeIf { it >= 0 }?.plus(4)
    if (start == null || !q.endsWith("?")) {
        append(q)
        return@buildAnnotatedString
    }
    append(q.substring(0, start))
    withStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.W600)) { append(q.substring(start, q.length - 1)) }
    append("?")
}

/** The item names of the estimate in the accent, Body/Strong. */
private fun highlighted(text: String, names: List<String>, accent: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    names.filter { it.length >= 3 }.forEach { name ->
        var from = 0
        while (true) {
            val at = text.indexOf(name, from, ignoreCase = true)
            if (at < 0) break
            addStyle(SpanStyle(color = accent, fontWeight = FontWeight.W600), at, at + name.length)
            from = at + name.length
        }
    }
}

/**
 * Chat/Estimate: ENERGIA TOTAL ~kcal, a line, and three glass macro boxes in the semantic colours. A47: Kind=Addition
 * names the added food with +kcal and + macros; Kind=Revision shows the revised meal over its NOVO TOTAL.
 */
@Composable
private fun EstimateCard(e: EstimateView) {
    val c = Aero.colors
    val type = Aero.type
    val shape = Aero.shapes.card
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.borderLine, shape)
            .padding(13.dp)
            .testTag("chat-estimate"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val sign = if (e.kind == EstimateKind.ADDITION) "+" else ""
        if (e.kind == EstimateKind.ADDITION) {
            // The food wraps; its +kcal stays on the first line.
            Row(Modifier.fillMaxWidth().testTag("chat-estimate-addition"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroText(e.label.orEmpty(), Modifier.weight(1f).alignByBaseline(), style = type.body.copy(color = c.textPrimary))
                AeroText("+${e.kcal}", Modifier.alignByBaseline(), style = type.title.copy(color = c.textPrimary, fontFeatureSettings = "tnum"))
                AeroText("kcal", Modifier.alignByBaseline(), style = type.caption.copy(color = c.textMuted))
            }
        } else {
            if (e.kind == EstimateKind.REVISION) e.label?.let { AeroText(it, style = type.body.copy(color = c.textPrimary)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                val label = if (e.kind == EstimateKind.REVISION) "Novo total" else "Energia total"
                AeroText(AeroTextTokens.labelSection.cased(label), Modifier.weight(1f).padding(bottom = 3.dp), style = type.labelSection.copy(color = c.textMuted))
                // The revised total is a number to confirm, not an estimate range: no ~ (D9).
                val kcal = if (e.kind == EstimateKind.REVISION) "${e.kcal}" else "~${e.kcal}"
                AeroText(kcal, style = type.title.copy(color = c.textPrimary, fontFeatureSettings = "tnum"))
                AeroText("kcal", Modifier.padding(bottom = 2.dp), style = type.caption.copy(color = c.textMuted))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderLine))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroBox("Proteína", "${sign}${e.p}g P", c.macroProtein, Modifier.weight(1f))
            MacroBox("Carbo", "${sign}${e.c}g C", c.macroCarbs, Modifier.weight(1f))
            MacroBox("Gordura", "${sign}${e.g}g G", c.macroFat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroBox(label: String, value: String, color: Color, modifier: Modifier) {
    val type = Aero.type
    Column(
        modifier.aeroGlass(Aero.shapes.card).padding(horizontal = 4.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AeroText(AeroTextTokens.labelSection.cased(label), style = type.labelSection.copy(color = Aero.colors.textMuted), maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        AeroText(value, style = type.bodyStrong.copy(color = color), maxLines = 1)
    }
}

/** chat0: greeting bubble (time inside) and Chat/MetaCard. */
@Composable
private fun Greeting(item: ChatItem.Greeting, ui: ChatUiState) {
    val c = Aero.colors
    val type = Aero.type
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BotBubble(Modifier.fillMaxWidth(BOT_FRACTION).testTag("chat-greeting"), gap = 6.dp) {
            AeroText(
                "Olá! Descreva o que você comeu ou envie uma foto do prato para estimarmos as calorias e macros.",
                style = type.body.copy(color = c.textPrimary),
            )
            BubbleTime(item.time, Modifier.align(Alignment.End))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .aeroGlass(Aero.shapes.card)
                .padding(15.dp)
                .testTag("chat-meta"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceTint), contentAlignment = Alignment.Center) {
                AeroIcon(AeroIconName.ForkKnife, c.accentDefault, size = 22.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AeroText("Meta calórica de hoje", style = type.bodyStrong.copy(color = c.textPrimary))
                AeroText(
                    "${formatRemaining(ui.metaRemaining)} / ${formatRemaining(ui.metaTotal)} kcal restantes",
                    style = type.caption.copy(color = c.textMuted),
                )
            }
            val pct = if (ui.metaTotal > 0) (ui.metaRemaining * 100f / ui.metaTotal).roundToInt() else 0
            AeroText("$pct%", style = type.bodyStrong.copy(color = c.accentDefault))
        }
    }
}

/**
 * chatL: Loader/Aero, the status in two lines and the bot label under the bubble. [thinking] (A64, ADR-054 § 3): after
 * 4 s without an answer the first line says `Tali está pensando…`.
 */
@Composable
private fun LoadingBubble(thinking: Boolean = false) {
    val c = Aero.colors
    val type = Aero.type
    val r = AeroDimens.radiusCard
    Column(Modifier.fillMaxWidth(BOT_FRACTION), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .aeroGlass(RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = 6.dp))
                .padding(horizontal = 17.dp, vertical = 15.dp)
                .testTag("chat-loading"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AeroLoader()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AeroText(
                    if (thinking) "Tali está pensando…" else "Analisando e calculando estimativa...",
                    Modifier.testTag(if (thinking) "chat-thinking" else "chat-loading-text"),
                    style = type.bodyStrong.copy(color = c.textMuted),
                )
                AeroText(AeroTextTokens.labelSection.cased("Micro & macronutrientes"), style = type.labelSection.copy(color = c.textDim))
            }
        }
        AeroText("Tali", Modifier.padding(start = 4.dp), style = type.caption.copy(color = c.textMuted))
    }
}

/** No gold: the failure bubble, glass with the status/bad border, tap retries. */
@Composable
private fun FailedBubble(onRetry: () -> Unit) {
    val c = Aero.colors
    val r = AeroDimens.radiusCard
    Box(
        Modifier
            .aeroGlass(RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = 6.dp), border = c.statusBad)
            .dietaClick(onClick = onRetry)
            .padding(horizontal = 19.dp, vertical = 15.dp)
            .testTag("chat-failed"),
    ) {
        AeroText("Não deu. Toque para tentar de novo.", style = Aero.type.body.copy(color = c.statusBad))
    }
}

// ----------------------------------------------------------------------------- footer

/** chat0: suggestion chips (Chip/Log Neutral) in a horizontal scroll, the last one cut at the edge. */
@Composable
private fun SuggestionRow(onPick: (String) -> Unit, onCamera: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("📸", "Tirar foto do prato", tag = "chat-suggestion-photo", onClick = onCamera)
        SUGGESTIONS.forEachIndexed { i, (emoji, text) -> Chip(emoji, text, tag = "chat-suggestion-$i") { onPick(text) } }
    }
}

@Composable
private fun Chip(emoji: String, text: String, tag: String? = null, onClick: () -> Unit) {
    AeroChipLog(
        "$emoji $text",
        Modifier.clip(CircleShape).dietaClick(onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier),
        tone = AeroChipTone.Neutral,
    )
}

/** The composer alone at the bottom, 24 dp from it (A61: the actions moved into the thread). */
@Composable
private fun Footer(
    ui: ChatUiState,
    onComposer: (String) -> Unit,
    onSend: () -> Unit,
    onPhoto: () -> Unit,
    onRemoveAttachment: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Composer(ui, onComposer, onSend, onPhoto, onRemoveAttachment)
    }
}

/** chatRB (A60 part A): what the plan goes over and what was reserved, muted, below the bubble. */
@Composable
private fun BudgetLines(note: BudgetNote, modifier: Modifier = Modifier) {
    val style = Aero.type.caption.copy(color = Aero.colors.textMuted)
    Column(modifier.testTag("chat-budget-note"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AeroText("Passa ${note.overKcal} kcal do que sobra.", style = style)
        note.reserved.forEach { (kcal, label) -> AeroText("Reservei $kcal kcal para $label.", style = style) }
    }
}

/** A64 (ADR-053 § 2): a permanent fact this answer saved, as stored; the caption line of `Não registrado`. */
@Composable
private fun NotedLabel(text: String, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(modifier.testTag("chat-noted"), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
        AeroIcon(AeroIconName.Checks, c.iconMuted, size = 14.dp, modifier = Modifier.padding(top = 2.dp))
        AeroText("Anotado: $text", style = Aero.type.caption.copy(color = c.textMuted))
    }
}

/** A64: the day with the estimate waiting for Registrar, in the caption style of the receipt's balance line. */
@Composable
private fun ProjectionLabel(text: String, modifier: Modifier = Modifier) {
    AeroText(text, modifier.testTag("chat-projection"), style = Aero.type.caption.copy(color = Aero.colors.textMuted))
}

/** chatRL (A60 part D): the plan holds the reservation of its meal. */
@Composable
private fun ReservedLabel(slot: String, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(modifier.testTag("chat-reserved"), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        AeroIcon(AeroIconName.CalendarCheck, c.iconMuted, size = 16.dp)
        AeroText("Reservado para o $slot", style = Aero.type.caption.copy(color = c.textMuted), maxLines = 1)
    }
}

/**
 * Chat/PlanOption (D25, chatO): an option of an open request on a `border/line` outline, `radius/card`, 12 dp in: the title
 * in Body/Strong, its items as bullets, the total with the macro colours, and Registrar | Reservar side by side.
 */
@Composable
private fun PlanOptionCard(option: OptionView, onRecord: () -> Unit, onReserve: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    val strong = type.bodyStrong.fontWeight
    val style = type.body.copy(color = c.textPrimary)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Aero.shapes.card)
            .border(1.dp, c.borderLine, Aero.shapes.card)
            .padding(12.dp)
            .testTag("chat-option-${option.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AeroText(option.title, style = type.bodyStrong.copy(color = c.textPrimary))
        // List/Bullet rows: 28 dp apart (24 dp line + 4 dp), tighter than the blocks of the card.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            option.items.forEach { line ->
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(OPTION_BULLET).padding(start = 4.dp)) { AeroText("•", style = style) }
                    AeroText(line, Modifier.weight(1f), style = style)
                }
            }
        }
        AeroText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textPrimary)) { append("~${option.kcal} kcal") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroProtein)) { append("${option.p}P") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroCarbs)) { append("${option.c}C") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroFat)) { append("${option.g}G") }
            },
            style = style,
        )
        // A70 (ADR-056 § 8): the fit and the day with this option, in the caption style of the budget note.
        if (option.fit != null || option.day != null) {
            val caption = type.caption.copy(color = c.textMuted)
            // D26 `Budget lines`: 1 dp more above and below than the block's 8 dp gap.
            Column(
                Modifier.fillMaxWidth().padding(vertical = 1.dp).testTag("chat-option-budget-${option.id}"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                option.fit?.let { AeroText(it, style = caption) }
                option.day?.let { AeroText(it, style = caption) }
            }
        }
        if (option.canRecord || option.canReserve) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (option.canRecord) {
                    AeroActionBar("Registrar", AeroIconName.CheckCircle, onRecord, Modifier.weight(1f).testTag("chat-option-record-${option.id}"))
                }
                if (option.canReserve) {
                    AeroActionBar("Reservar", AeroIconName.CalendarCheck, onReserve, Modifier.weight(1f).testTag("chat-option-reserve-${option.id}"))
                }
            }
        }
    }
}

/** A70 (chatO): the lead or trailing text of a plan with options in Body, markers kept (ADR-045), no macro colours. */
@Composable
private fun OptionReplyText(text: String) {
    if (text.isBlank()) return
    val blocks = remember(text) { ReplyMarkup.parse(text).takeIf { ReplyMarkup.formatted(text) } }
    if (blocks != null) {
        AeroReplyBlocks(blocks)
    } else {
        val lines = remember(text) { text.trim().lines().filter { it.isNotBlank() }.joinToString("\n") { it.trim() } }
        AeroText(lines, style = Aero.type.body.copy(color = Aero.colors.textPrimary))
    }
}

/** The bullet column of D17 (17 dp), as in the reply blocks. */
private val OPTION_BULLET = 17.dp

/** chatR: Registrar assim under the plan. */
@Composable
private fun PlanBar(actions: EstimateActions, onClick: () -> Unit) {
    AeroActionBar("Registrar assim", AeroIconName.CheckCircle, onClick, Modifier.testTag("chat-record-plan"))
}

/** chatQ: Forçar estimativa under the latest question. */
@Composable
private fun ForceBar(onClick: () -> Unit) {
    AeroActionBar("Forçar estimativa", AeroIconName.FastForward, onClick, Modifier.testTag("chat-force-estimate"), haptic = Haptic.Light)
}

/**
 * Chat/Composer: glass pill (Default); a 20 dp card past one line or with a photo; status/bad border, muted
 * camera and send and "Texto muito longo" under it past the limit (TooLong, ADR-022).
 */
@Composable
private fun Composer(ui: ChatUiState, onComposer: (String) -> Unit, onSend: () -> Unit, onPhoto: () -> Unit, onRemoveAttachment: () -> Unit) {
    val c = Aero.colors
    val attached = ui.attachment != null
    var multiLine by remember { mutableStateOf(false) }
    val tooLong = ui.composerTooLong
    val tall = attached || multiLine || tooLong
    val shape = if (tall) Aero.shapes.card else CircleShape
    val stroke = if (tooLong) 1.5.dp else 1.dp
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .aeroGlass(shape, border = if (tooLong) c.statusBad else c.borderGlass, borderWidth = stroke)
                .padding(
                    start = if (tooLong) 13.5.dp else 9.dp,
                    end = if (tooLong) 13.5.dp else 9.dp,
                    top = if (attached) 15.dp else if (tooLong) 13.5.dp else 9.dp,
                    bottom = if (tooLong) 13.5.dp else 9.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ui.attachment?.let { AttachmentThumb(it, onRemoveAttachment) }
            ComposerRow(ui, onComposer, onSend, onPhoto, bottomAligned = multiLine || tooLong) { lines -> multiLine = lines > 1 }
        }
        if (tooLong) {
            // Start of the text field: the 44 dp camera, the 10 dp gap and the box inset.
            AeroText(
                "Texto muito longo",
                Modifier.padding(start = 66.dp).testTag("chat-too-long"),
                style = Aero.type.caption.copy(color = c.statusBad),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ComposerRow(
    ui: ChatUiState,
    onComposer: (String) -> Unit,
    onSend: () -> Unit,
    onPhoto: () -> Unit,
    bottomAligned: Boolean,
    onLines: (Int) -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = if (bottomAligned) Alignment.Bottom else Alignment.CenterVertically,
    ) {
        AeroIconButton(
            AeroIconName.Camera,
            onPhoto,
            contentDescription = "Enviar foto",
            tint = if (blocked) c.textDim else c.iconPrimary,
            enabled = ui.canAttach,
            modifier = Modifier.testTag("chat-photo"),
        )
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
            textStyle = type.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accentDefault),
            onTextLayout = { onLines(it.lineCount) },
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically).focusRequester(focus).testTag("chat-input"),
            decorationBox = { inner ->
                Box {
                    if (ui.composer.isEmpty()) {
                        AeroText(
                            "Descreva sua refeição ou envie foto...",
                            style = type.body.copy(color = c.textDim),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(if (blocked) Modifier.background(c.surface2) else Modifier.background(c.accentDefault).aeroGloss(CircleShape))
                .dietaClick(Haptic.Confirm, enabled = ui.canSend, onClick = onSend)
                .testTag("chat-send"),
            contentAlignment = Alignment.Center,
        ) {
            AeroIcon(AeroIconName.ArrowUp, if (blocked) c.textDim else c.accentOn, size = 20.dp, contentDescription = "Enviar")
        }
    }
}

// ----------------------------------------------------------------------------- overlays

/** chatT: Sheet/Bottom "Selecione a refeição" with Sheet/SlotList (Row/SlotPick per slot). */
@Composable
private fun BoxScope.SlotSheet(ui: ChatUiState, onSelect: (Long) -> Unit, onConfirm: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    AeroScrim(onClose)
    val addition = ui.sheetAddition
    AeroSheet(
        title = if (addition != null) "Escolher outra refeição" else "Selecione a refeição",
        primary = "Confirmar refeição",
        onPrimary = onConfirm,
        secondary = "Cancelar",
        onSecondary = onClose,
        primaryEnabled = ui.sheetSelection != null,
        primaryTag = "chat-sheet-confirm",
        secondaryTag = "chat-sheet-cancel",
        primaryIcon = AeroIconName.ArrowRight,
        bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        modifier = Modifier.align(Alignment.BottomCenter).testTag("chat-sheet"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (addition != null) {
                AeroText(
                    "Só o acréscimo vai para a refeição escolhida. O ${addition.sourceName} fica como está.",
                    style = Aero.type.body.copy(color = Aero.colors.textMuted),
                )
                AdditionBox(addition)
            } else {
                AeroText("Escolha o momento do dia para salvar este registro:", style = Aero.type.body.copy(color = Aero.colors.textMuted))
            }
            ui.slots.forEach { slot ->
                AeroSlotPickRow(
                    name = slot.name,
                    time = if (slot.id == ui.sheetCurrent) "${slot.time} (atual)" else slot.time,
                    icon = sheetIcon(slot.minutes),
                    selected = slot.id == ui.sheetSelection,
                    onClick = { onSelect(slot.id) },
                    modifier = Modifier.testTag("chat-sheet-slot-${slot.id}"),
                )
            }
            // A72 (D28 `Sheet/SlotList` Show extra): a record outside the meals; never for an addition.
            if (ui.sheetAddition == null) {
                AeroSlotPickRow(
                    name = "Extra",
                    time = "fora das refeições",
                    icon = AeroIconName.Clock,
                    selected = ui.sheetSelection == app.fibrai.android.domain.Extras.TARGET,
                    onClick = { onSelect(app.fibrai.android.domain.Extras.TARGET) },
                    modifier = Modifier.testTag("chat-sheet-slot-extra"),
                )
            }
        }
    }
}

/** chatTI (A47): the added food and its +kcal on a surface/2 box above the meals. */
@Composable
private fun AdditionBox(addition: SheetAddition) {
    val c = Aero.colors
    val type = Aero.type
    val shape = Aero.shapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.borderLine, shape)
            .padding(horizontal = 17.dp, vertical = 12.dp)
            .testTag("chat-sheet-addition"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AeroText(addition.food, Modifier.weight(1f).alignByBaseline(), style = type.body.copy(color = c.textPrimary))
        AeroText("+${addition.kcal} kcal", Modifier.alignByBaseline(), style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
    }
}

private fun sheetIcon(minutes: Int): AeroIconName = when (SlotSuggestions.bandOf(minutes)) {
    SlotBand.BREAKFAST -> AeroIconName.Coffee
    SlotBand.MORNING_SNACK, SlotBand.AFTERNOON_SNACK -> AeroIconName.Cookie
    SlotBand.LUNCH -> AeroIconName.ForkKnife
    SlotBand.DINNER -> AeroIconName.BowlFood
    SlotBand.NIGHT -> AeroIconName.Moon
}

// ----------------------------------------------------------------------------- photo (A6)

/** Photo chooser (no gold): Sheet/Bottom with Sheet/PhotoSource, Tirar foto and Escolher da galeria, Cancelar. */
@Composable
private fun BoxScope.PhotoSheet(onCamera: () -> Unit, onGallery: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    AeroScrim(onClose)
    AeroSheet(
        title = "Enviar foto do prato",
        primary = null,
        onPrimary = {},
        secondary = "Cancelar",
        onSecondary = onClose,
        secondaryTag = "chat-photo-cancel",
        bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        modifier = Modifier.align(Alignment.BottomCenter).testTag("chat-photo-sheet"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AeroText("O texto digitado vai junto como legenda. Até 16 MB.", style = Aero.type.body.copy(color = Aero.colors.textMuted))
            PhotoSourceRow(AeroIconName.Camera, "Tirar foto", "chat-photo-camera", onCamera)
            PhotoSourceRow(AeroIconName.Image, "Escolher da galeria", "chat-photo-gallery", onGallery)
        }
    }
}

@Composable
private fun PhotoSourceRow(icon: AeroIconName, label: String, tag: String, onClick: () -> Unit) {
    val c = Aero.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .aeroGlass(Aero.shapes.card, backdropBlurred = true)
            .dietaClick(onClick = onClick)
            .padding(horizontal = 17.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).border(1.dp, c.borderLine, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { AeroIcon(icon, c.accentDefault, size = 20.dp) }
        AeroText(label, style = Aero.type.bodyStrong.copy(color = c.textPrimary))
    }
}

/** One-shot message above the composer ("Foto grande demais."), no gold: a status/bad pill. */
@Composable
private fun Notice(text: String, onShown: () -> Unit) {
    LaunchedEffect(text) {
        delay(NOTICE_MS)
        onShown()
    }
    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
        AeroChipLog(text, Modifier.testTag("chat-notice"), tone = AeroChipTone.Bad)
    }
}

private const val NOTICE_MS = 3_000L

/** chatCC (A61 part B, Android 12 and earlier): `Mensagem copiada` / `{n} mensagens copiadas` 10 dp above the composer, ~2 s. */
@Composable
private fun CopyToast(count: Int, onShown: () -> Unit) {
    LaunchedEffect(count) {
        delay(COPIED_MS)
        onShown()
    }
    val label = if (count == 1) "Mensagem copiada" else "$count mensagens copiadas"
    AeroCopyToast(label, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("chat-copied"))
}

private const val COPIED_MS = 2_000L
