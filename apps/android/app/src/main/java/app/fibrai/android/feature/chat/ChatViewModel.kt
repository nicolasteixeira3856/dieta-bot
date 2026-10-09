package app.fibrai.android.feature.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.closure.DayTotals
import app.fibrai.android.core.closure.dayTotals
import app.fibrai.android.core.database.ChatMessageEntity
import app.fibrai.android.core.database.slotsOfDay
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.budgetOn
import app.fibrai.android.core.database.metaOn
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.photo.PhotoFiles
import app.fibrai.android.core.photo.PhotoResult
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatMemoryUpdate
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.telemetry.ChatFallback
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.RequestIds
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.core.database.RecordGuard
import app.fibrai.android.core.database.SkipMove
import app.fibrai.android.domain.ChatText
import app.fibrai.android.domain.DayBalance
import app.fibrai.android.domain.EstimateNumbers
import app.fibrai.android.domain.MealChanges
import app.fibrai.android.domain.MealProposal
import app.fibrai.android.domain.SlotCheck
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.Macros
import app.fibrai.android.domain.MemoryResult
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.MemoryUpdate
import app.fibrai.android.domain.PlanBudget
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.core.database.ReserveResult
import app.fibrai.android.domain.ReplyMarkup
import app.fibrai.android.domain.ProjectedDay
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.ReceiptRules
import app.fibrai.android.domain.RoutineUpdate
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SkipEntry
import app.fibrai.android.domain.SkipOutcomes
import app.fibrai.android.domain.SlotChange
import app.fibrai.android.domain.SlotClock
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.domain.UndoData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The only network call of the Chat (spec rule 13: no /v1/estimate, no /v1/fit). */
fun interface ChatService {
    suspend fun chat(body: ChatIn): ChatOut
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: DayRepository,
    private val service: ChatService,
    private val clock: InstantClock,
    private val memory: FactMemory,
    private val photos: PhotoFiles,
    private val telemetry: Telemetry = NoopTelemetry,
    private val requestIds: RequestIds = RequestIds(),
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    /** Not loaded until the first page arrives: the thread never draws empty and then fills (A32). */
    private val _uiState = MutableStateFlow(ChatUiState(loaded = false))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Today's whole conversation, never the paged window: question rounds, actions, Forçar estimativa. */
    private var todayMessages: List<ChatMessageEntity> = emptyList()

    /** Rows asked of the window (A32): grows by [PAGE_SIZE] on [loadOlder]. */
    private val pageLimit = MutableStateFlow(PAGE_SIZE)

    /** Limit of the last window that arrived; below [pageLimit] = an older page is loading. */
    private var windowLimit = 0
    private var windowFull = false

    /**
     * The newest [limit] rows, oldest first, and today's whole conversation read with them: one Room
     * observation, so the actions never arrive a frame before their bubble.
     */
    private data class Window(
        val limit: Int,
        val rows: List<ChatMessageEntity>,
        val today: List<ChatMessageEntity>,
        /** Receipts of the 60 days (A34): the latest of each slot carries the actions. */
        val receipts: List<ChatMessageEntity> = emptyList(),
        /** Slot states of the days before today touched by an active receipt; today comes from the snapshot. */
        val pastSlots: Map<String, Map<Long, SlotState>> = emptyMap(),
    )

    /** VM-only state: never in Room until the server answers (spec: failure leaves Room untouched). */
    private data class Local(
        val composer: String = "",
        /** Counted once per change, not per render: a huge paste stays linear. */
        val composerTooLong: Boolean = false,
        val pending: String? = null,
        val failed: Boolean = false,
        /** Trocar sheet (chatT): for an estimate (Outra refeição, a plan without slot) or a receipt (Trocar refeição). */
        val sheet: Sheet? = null,
        val sheetSelection: Long? = null,
        /** Trocar refeição into a slot with a record (A34): Substituir below the receipt, until the next send. */
        val moveConfirm: MoveConfirm? = null,
        val openedAt: Instant? = null,
        /** JPEG of the pending send (A6). Stored in chat_message only after the server answers. */
        val pendingPhoto: String? = null,
        /** The pending send is Forçar estimativa (A30): a retry keeps the flag. */
        val pendingForce: Boolean = false,
        val photoSheet: Boolean = false,
        /** JPEG attached in the composer (chatA): leaves only on Enviar. One at a time. */
        val attachment: String? = null,
        /** One-shot snackbar ("Foto grande demais."). */
        val notice: String? = null,
        /** The first send of this screen hides the routine card until the Chat opens again (A29). */
        val sentOnce: Boolean = false,
        val focusComposer: Int = 0,
        /** A47: the unrecorded addition the pending send continues, taken on its first attempt; a retry keeps it. */
        val pendingAddition: MealProposal? = null,
        /** A60 part A: the pending send is Ajustar para caber with this target; a retry keeps it. */
        val pendingFit: Int? = null,
        /** A61 part B (chatCP): keys of the selected text bubbles, and the day the selection started. */
        val selected: Set<String> = emptySet(),
        val selectedOn: String? = null,
        /** The latest wipe of today when the selection started: a newer wipe ends it. */
        val selectedWipe: Long = 0,
        /** A61 part B (chatCC, Android 12 and earlier): messages just copied, for the app's own confirmation. */
        val copied: Int? = null,
        /** A64 (ADR-054 § 3): the pending send has waited [WAITING_MS]; the loading bubble says Tali is thinking. */
        val slow: Boolean = false,
    )

    private sealed interface Sheet {
        val id: Long

        data class Estimate(override val id: Long) : Sheet

        /** [current]: the record's slot, marked "(atual)". */
        data class Receipt(override val id: Long, val current: Long) : Sheet
    }

    private data class MoveConfirm(val receiptId: Long, val target: SlotRef, val state: SlotState, val confirm: ReplaceConfirm)

    private val recorder = ChatRecorder(repository, memory, telemetry, clock) { result ->
        memoryChanged(result)
        reloadFacts()
    }

    /** Registrar pills already reported as shown, and pending replaces already being expired. */
    private val shownAsks = mutableSetOf<Long>()
    private val expiring = mutableSetOf<Long>()

    /** A59: delete proposals (answer, slot) already being expired. */
    private val expiringSkips = mutableSetOf<Pair<Long, Long>>()

    /** A47: answers with a record tap in flight ([once]). */
    private val inFlight = mutableSetOf<Long>()

    /** The latest wipe row of today in the last render (A61: a wipe ends the selection). */
    private var wipeMark = 0L

    /** Facts after expiration, for the routine card (A29). Reloaded after every memory change. */
    private val facts = MutableStateFlow<List<Fact>>(emptyList())

    /** Minute tick: the slot of the hour changes with the clock, not with Room. */
    private val minute = MutableStateFlow(0L)

    /** Routine cards already reported as shown on this screen. */
    private val shownRoutines = mutableSetOf<String>()

    /** TakePicture target while the camera is open. */
    private var captureFile: File? = null

    init {
        local.update { it.copy(openedAt = clock.now()) }
        val day = minute.map { SaoPaulo.date(clock.now()).toString() }.distinctUntilChanged()
        // Any chat_message write re-runs the window query (Room invalidates the table); today is read with it,
        // cut at the window's newest id (ids grow with inserts): a row written in between waits for the next emission.
        val window = combine(pageLimit, day, ::Pair).flatMapLatest { (limit, date) ->
            repository.observeLatestMessages(limit).map { latest ->
                val newest = latest.firstOrNull()?.id ?: 0L
                val receipts = repository.receipts().filter { it.id <= newest }
                // Other days change only through the Chat, which writes chat_message too: read with the window.
                val pastDates = receipts.filter { it.receiptState == null }
                    .flatMap { UndoData.decode(it.undoData)?.slots.orEmpty().map { c -> c.date } }
                    .filter { it != date }.toSet()
                Window(
                    limit,
                    latest.asReversed(),
                    repository.messagesOf(date).filter { it.id <= newest },
                    receipts,
                    pastDates.associateWith { repository.slotStates(it) },
                )
            }
        }
        viewModelScope.launch {
            combine(repository.observeToday(), window, local, facts, combine(minute, pageLimit) { _, limit -> limit }) { d, w, l, f, limit ->
                Rendered(d, w, l, f, limit)
            }.collect { r ->
                todayMessages = r.w.today
                windowLimit = r.w.limit
                windowFull = r.w.rows.size >= r.w.limit
                _uiState.value = render(r.d, r.w, r.l, r.f).copy(
                    loaded = true,
                    hasOlder = windowFull,
                    loadingOlder = r.limit > r.w.limit,
                )
            }
        }
        viewModelScope.launch { reloadFacts() }
        // A59: skips an answer listed but never applied (the screen or the process died in between).
        viewModelScope.launch { resumeSkips() }
        viewModelScope.launch {
            while (true) {
                delay(MINUTE_MS - clock.now().toEpochMilli() % MINUTE_MS)
                tick()
            }
        }
    }

    private data class Rendered(
        val d: DaySnapshot,
        val w: Window,
        val l: Local,
        val f: List<Fact>,
        val limit: Int,
    )

    /**
     * The thread reached its oldest drawn items: [PAGE_SIZE] more, within the 60 days. One page in
     * flight at a time; nothing when the last window came back short (no older rows).
     */
    fun loadOlder() {
        if (!windowFull || pageLimit.value > windowLimit) return
        pageLimit.update { it + PAGE_SIZE }
    }

    /** Re-evaluates what depends on the hour (the routine card). Called every minute. */
    internal fun tick() = minute.update { it + 1 }

    private suspend fun reloadFacts() {
        // An unreadable memory shows no card.
        facts.value = runCatching { memory.read(SaoPaulo.date(clock.now())).facts }.getOrDefault(emptyList())
    }

    /** Kept exactly as typed or pasted: nothing is cut (ADR-022). Over the limit the composer shows chatX. */
    fun setComposer(text: String) = local.update { it.copy(composer = text, composerTooLong = ChatText.tooLong(text)) }

    fun useSuggestion(text: String) = setComposer(text)

    /** Text, attachment or both. The attachment becomes the pending photo of this send. */
    fun send() {
        val text = local.value.composer.trim()
        val photo = local.value.attachment
        if (text.isEmpty() && photo == null || local.value.pending != null || local.value.composerTooLong) return
        local.update {
            it.copy(
                composer = "", composerTooLong = false, attachment = null, pending = text, pendingPhoto = photo, pendingForce = false,
                failed = false, sentOnce = true, pendingAddition = null, pendingFit = null, selected = emptySet(),
            )
        }
        viewModelScope.launch { post(text, photo) }
    }

    /**
     * Forçar estimativa (A30): sends [FORCE_TEXT] with force_estimate through the normal send flow.
     * Only while the button shows; the composer draft stays where it is.
     */
    fun forceEstimate() {
        if (!_uiState.value.forceEstimate || local.value.pending != null) return
        val round = PromptBuilder.clarifyRounds(todayMessages.filter { it.date == SaoPaulo.date(clock.now()).toString() })
        telemetry.event(TelemetryEvents.CHAT_FORCE_ESTIMATE, mapOf("round" to round))
        local.update {
            it.copy(pending = FORCE_TEXT, pendingPhoto = null, pendingForce = true, failed = false, sentOnce = true, pendingAddition = null, pendingFit = null, selected = emptySet())
        }
        viewModelScope.launch { post(FORCE_TEXT, force = true) }
    }

    /** A61 part B (ADR-048): a long press on a text bubble starts the selection with it, or toggles it while selecting. */
    fun longPress(key: String) = toggleSelected(key)

    /** While selecting, a tap on a text bubble adds or removes it; removing the last one ends the selection. */
    fun tapWhileSelecting(key: String) {
        if (_uiState.value.selected.isNotEmpty()) toggleSelected(key)
    }

    /** ✕, the system back, or a tap on an action. */
    fun clearSelection() = local.update { it.copy(selected = emptySet()) }

    private fun toggleSelected(key: String) {
        val ui = _uiState.value
        if (ui.items.none { it.key == key && it.copyText != null }) return
        val next = if (key in ui.selected) ui.selected - key else ui.selected + key
        local.update { it.copy(selected = next, selectedOn = SaoPaulo.date(clock.now()).toString(), selectedWipe = wipeMark) }
    }

    /**
     * Copiar: the selected messages' text in conversation order, a blank line apart, and the selection ends. Null when
     * nothing is selected. The caller puts the text on the clipboard; telemetry counts the messages, never the text.
     */
    fun copySelection(): String? {
        val ui = _uiState.value
        val picked = ui.items.filter { it.key in ui.selected }.mapNotNull { item -> item.copyText?.let { item to it } }
        if (picked.isEmpty()) return null
        local.update { it.copy(selected = emptySet()) }
        telemetry.event(
            TelemetryEvents.MESSAGE_COPIED,
            mapOf("count" to picked.size, "has_user" to picked.any { it.first is ChatItem.User }, "has_tali" to picked.any { it.first !is ChatItem.User }),
        )
        return picked.joinToString("\n\n") { it.second }
    }

    /** Android 12 and earlier (chatCC): the app's own confirmation of [count] copied messages. */
    fun showCopied(count: Int) = local.update { it.copy(copied = count) }

    fun dismissCopied() = local.update { it.copy(copied = null) }

    fun retry() {
        val text = local.value.pending ?: return
        if (!local.value.failed) return
        local.update { it.copy(failed = false) }
        viewModelScope.launch { post(text, local.value.pendingPhoto, local.value.pendingForce, local.value.pendingFit) }
    }

    // ------------------------------------------------------------------ over-budget choice (A60 part A, ADR-039)

    /** Pode passar (chatRB): no request; the choice is stored on the plan, the pills leave and Registrar assim returns. */
    fun acceptOver(estimateId: Long) {
        val choice = _uiState.value.actions?.takeIf { it.estimateId == estimateId }?.choice ?: return
        viewModelScope.launch {
            val row = repository.message(estimateId) ?: return@launch
            val budget = PlanBudget.decode(row.planBudget)?.takeIf { it.choosing } ?: return@launch
            repository.setPlanBudget(estimateId, budget.copy(local = PlanBudget.OVER_OK).encode())
            budgetChoice(PlanBudget.OVER_OK, choice.overKcal)
        }
    }

    /**
     * Ajustar para caber (chatRB): sends `Ajusta para caber em {limit} kcal.` with fit_kcal through the normal send flow;
     * a retry keeps the target. The answer is a normal plan, with the choice again when it is still over.
     */
    fun adjustToFit(estimateId: Long) {
        val choice = _uiState.value.actions?.takeIf { it.estimateId == estimateId }?.choice ?: return
        if (local.value.pending != null) return
        val text = "Ajusta para caber em ${choice.limitKcal} kcal."
        budgetChoice(PlanBudget.FIT, choice.overKcal)
        local.update {
            it.copy(pending = text, pendingPhoto = null, pendingForce = false, failed = false, sentOnce = true, pendingAddition = null, pendingFit = choice.limitKcal)
        }
        viewModelScope.launch { post(text, fit = choice.limitKcal) }
    }

    // ------------------------------------------------------------------ reserve a plan (A60 part D, ADR-046)

    /**
     * Reservar para o {slot} (chatR → chatRL): the plan becomes the reservation of its meal of today, in one transaction
     * that rechecks the day, the wipe and that the meal has nothing eaten. Reserving again from another plan replaces it.
     */
    fun reserve(estimateId: Long) {
        val slot = _uiState.value.actions?.takeIf { it.estimateId == estimateId }?.reserve ?: return
        once(estimateId) {
            val plan = openEstimate(estimateId)?.takeIf { it.isPlanEstimate } ?: return@once
            val date = SaoPaulo.date(clock.now()).toString()
            val reservation = PlannedSlot(
                text = plan.estimateMealText?.takeIf { it.isNotBlank() } ?: ReplyMarkup.plain(plan.text),
                kcal = plan.estimateKcal ?: return@once,
                p = plan.estimateP ?: 0,
                c = plan.estimateC ?: 0,
                g = plan.estimateG ?: 0,
                sourceMessageId = plan.id,
            )
            val result = repository.reserve(date, repository.latestWipeToday(), slot.id, reservation)
            if (result is ReserveResult.Reserved) planReserved(if (result.replaced != null) "replaced" else "reserved")
        }
    }

    private fun planReserved(action: String) = telemetry.event(TelemetryEvents.PLAN_RESERVED, mapOf("action" to action))

    private fun budgetChoice(choice: String, overKcal: Int) =
        telemetry.event(TelemetryEvents.PLAN_BUDGET_CHOICE, mapOf("choice" to choice, "over_kcal" to overKcal))

    // ------------------------------------------------------------------ photo (A6)

    /** Disabled in the chatX state like Enviar: the caption would be the too-long composer. */
    fun openPhotoSheet() {
        if (local.value.composerTooLong) return
        local.update { it.copy(photoSheet = true) }
    }

    fun closePhotoSheet() = local.update { it.copy(photoSheet = false) }

    fun dismissNotice() = local.update { it.copy(notice = null) }

    fun cameraDenied() = local.update { it.copy(photoSheet = false, notice = "Sem permissão da câmera.") }

    /** Uri for TakePicture; the file lives in filesDir/photos. */
    fun newCapture(): Uri {
        val (file, uri) = photos.newCapture()
        captureFile = file
        local.update { it.copy(photoSheet = false) }
        return uri
    }

    fun onCaptured(success: Boolean) {
        val file = captureFile ?: return
        captureFile = null
        if (!success) {
            photos.delete(file.path)
            return
        }
        viewModelScope.launch { onPhoto(photos.acceptCapture(file)) }
    }

    fun onPicked(uri: Uri?) {
        local.update { it.copy(photoSheet = false) }
        if (uri == null) return
        viewModelScope.launch { onPhoto(photos.import(uri)) }
    }

    /** The photo becomes the composer attachment; nothing is sent until Enviar. A new one replaces the old. */
    private fun onPhoto(result: PhotoResult) {
        when (result) {
            is PhotoResult.Ready -> {
                val previous = local.value.attachment
                local.update { it.copy(attachment = result.path) }
                previous?.takeIf { it != result.path }?.let { photos.delete(it) }
            }
            PhotoResult.TooLarge -> local.update { it.copy(notice = "Foto grande demais.") }
            PhotoResult.Failed -> local.update { it.copy(notice = "Não deu para abrir a foto.") }
        }
    }

    /** ✕ on the thumbnail: the file goes too. */
    fun removeAttachment() {
        val path = local.value.attachment ?: return
        local.update { it.copy(attachment = null) }
        photos.delete(path)
    }

    override fun onCleared() {
        // A photo that never got an answer is not in chat_message: nothing points to it.
        local.value.pendingPhoto?.let { photos.delete(it) }
        local.value.attachment?.let { photos.delete(it) }
        captureFile?.let { photos.delete(it.path) }
    }

    /** One turn; after [WAITING_MS] without an answer the loading bubble changes its copy (A64, ADR-054 § 3). */
    private suspend fun post(text: String, photo: String? = null, force: Boolean = false, fit: Int? = null) {
        val waiting = viewModelScope.launch {
            delay(WAITING_MS)
            local.update { it.copy(slow = true) }
        }
        try {
            postTurn(text, photo, force, fit)
        } finally {
            waiting.cancel()
            local.update { it.copy(slow = false) }
        }
    }

    private suspend fun postTurn(text: String, photo: String?, force: Boolean, fit: Int?) {
        val sentAt = clock.now()
        val date = SaoPaulo.date(sentAt).toString()
        val today = repository.messagesOf(date)
        val snapshot = repository.observeToday().first()
        // A47: what DAY carries, captured with the request: every slot of today and the latest wipe of the day.
        val wipeId = today.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
        val states = todayStates(snapshot).let { s -> snapshot.slotsOfDay.associate { it.id to (s[it.id] ?: SlotState.EMPTY) } }
        // A47: the addition this message continues, taken before the send expires it, only while its source still
        // matches DAY. A retry keeps what the first attempt took.
        val continued = (local.value.pendingAddition ?: continuedAddition(today))
            ?.takeIf { it.sourceHolds(date, wipeId) { id -> states[id] ?: SlotState.EMPTY } }
        local.update { it.copy(pendingAddition = continued) }
        val pendingAddition = continued?.let(PromptBuilder::pendingAddition)
        // A34: Registrar and a pending Substituir die on the next send (a retry is a new send).
        expireOpen()
        telemetry.event(
            TelemetryEvents.CHAT_SEND,
            mapOf("has_photo" to (photo != null), "text_len" to TelemetryEvents.lengthBucket(text.length)),
        )
        var digests = repository.digestsToday()
        // An unreadable memory never blocks the turn: it goes empty.
        val facts = runCatching { memory.read(SaoPaulo.date(sentAt)).facts }.getOrDefault(emptyList())
        val recentLogs = repository.recentLogs()
        // A64: the totals of the days before today; a failed read never blocks the turn.
        val pastDays = runCatching { pastDays(snapshot, SaoPaulo.date(sentAt)) }.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyList())
        val image = photo?.let { photos.base64(it) }
        if (photo != null && image == null) {
            chatResult("error")
            local.update { it.copy(failed = true) }
            return
        }
        var turn = PromptBuilder.build(
            snapshot, today, digests, text, sentAt, facts = facts, recentLogs = recentLogs, forceEstimate = force, pendingAddition = pendingAddition,
            fitKcal = fit, pastDays = pastDays,
        )
        if (turn.needsCompact) {
            // A38: the oldest block(s), the open tail stays raw. A failed compact never fails the turn: nothing
            // stored, the newest 12 raw go as they are and the next send tries again. A digest is stored only on
            // the day and after the wipe it was asked on.
            val sentOn = SaoPaulo.date(sentAt).toString()
            val wipeId = today.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
            val planned = turn
            var stored = 0
            for (block in planned.blocks) {
                val digest = runCatching { service.chat(PromptBuilder.compact(turn, block)).digest }.getOrNull()
                if (digest.isNullOrBlank() || !repository.upsertDigest(digest, block.coversUntilId, sentOn, wipeId)) break
                stored++
                digests = repository.digestsToday()
                turn = PromptBuilder.build(
                    snapshot, today, digests, text, sentAt, facts = facts, recentLogs = recentLogs, forceEstimate = force,
                    pendingAddition = pendingAddition, fitKcal = fit, pastDays = pastDays,
                )
            }
            if (stored > 0) {
                val summarised = planned.blocks.take(stored).sumOf { it.messages.size }
                val kept = planned.kept + planned.blocks.drop(stored).sumOf { it.messages.size }
                telemetry.event(TelemetryEvents.CHAT_COMPACT, mapOf("blocks" to stored, "kept" to kept, "summarised" to summarised))
            }
        }
        // The photo rides only on the turn, never on the compact request.
        val out = runCatching { service.chat(turn.body.copy(imageB64 = image)) }.getOrNull()
        // A question-only turn (A30): no estimate yet, the question is the answer.
        val question = out?.question?.trim()?.takeIf { out.estimate == null && it.isNotEmpty() }
        if (out == null || out.reply.isBlank() && out.estimate == null && question == null) {
            chatResult("error")
            local.update { it.copy(failed = true) }
            return
        }
        // The server fallback arrives as a normal reply: shown as is, reported as a non-fatal (A11).
        if (out.estimate == null && out.reply.trim() == SERVER_FALLBACK_REPLY) {
            telemetry.nonFatal(ChatFallback(requestIds.last, hasPhoto = photo != null))
            chatResult("fallback")
        } else {
            chatResult("ok", hasEstimate = out.estimate != null, confidence = out.estimate?.confidence, intent = out.intent, questionOnly = question != null, record = out.record)
        }
        val slots = snapshot.slotsOfDay.map { it.id }.toSet()
        val suggested = out.estimate?.suggestedSlot?.toLongOrNull()?.takeIf { it in slots }
        // A38: the slot held with a question-only turn goes back in HISTORY as the suggested meal.
        val held = out.questionSlot?.toLongOrNull()?.takeIf { question != null && it in slots }
        val intent = if (question != null) "log" else out.intent?.takeIf { it in INTENTS }
        val recordMode = recordModeOf(out, intent, question, suggested)
        // A59: the listed skips of today, stored with the answer before any write; the log's own slot is never one.
        val skips = out.skipSlots?.let { ids ->
            val listed = ids.mapNotNull { raw -> raw.toLongOrNull()?.takeIf { it in slots } ?: null.also { recordGuard(GUARD_SLOT_NOT_TODAY) } }
                .distinct()
                .filter { intent != "log" || it != suggested }
            listed.takeIf { it.isNotEmpty() }?.let { SkipOutcomes(date = date, wipeId = wipeId, with = intent ?: "log", slots = it.map(::SkipEntry)) }
        }
        // A47: the structured proposal, checked against the states the request carried; never read from the prose.
        val proposal = MealChanges.proposal(
            MealChanges.parse(out.mealChange),
            out.estimate?.let { EstimateNumbers(it.kcal, it.p, it.c, it.g, it.mealText?.trim(), it.suggestedSlot) },
            recordable = question == null && intent == "log" && out.record in setOf(RECORD_AUTO, RECORD_ASK),
            states = states,
            date = date,
            wipeId = wipeId,
        )
        val fresh = proposal == null || !proposal.actionable || proposalHoldsNow(proposal, sourceOnly = proposal.operation == MealProposal.NEW)
        val recordState = when {
            proposal == null -> null
            !proposal.actionable || !fresh -> ChatRecorder.NOT_RECORDED
            proposal.isRevision -> ChatRecorder.PENDING_REVISE
            // An occupied destination is never changed without Adicionar (ADR-032).
            proposal.isAddition && proposal.destination?.records?.isNotEmpty() == true -> ChatRecorder.PENDING_ADD
            else -> null
        }
        repository.insertMessage(role = "user", text = text, photoPath = photo)
        // Preference, portion and every replace/remove apply now; a routine waits for its record (A28). A47: a rejected
        // proposal brings no memory at all; one that arrived stale brings no routine.
        val (routine, immediate) = if (proposal?.actionable == false) {
            emptyList<ChatMemoryUpdate>() to emptyList()
        } else {
            out.memoryUpdates.partition { it.waitsForRecord }.let { (r, i) -> (if (fresh) r else emptyList()) to i }
        }
        val applied = applyMemory(immediate.map { it.toDomain() })
        val answerId = repository.insertMessage(
            role = "assistant",
            // Question only: the server's history text (draft + question), sent back as is next turn.
            text = out.reply.ifBlank { question.orEmpty() },
            estimateKcal = out.estimate?.kcal?.roundToInt(),
            estimateP = out.estimate?.p?.roundToInt(),
            estimateC = out.estimate?.c?.roundToInt(),
            estimateG = out.estimate?.g?.roundToInt(),
            estimateConfidence = out.estimate?.confidence,
            estimateSlotId = suggested ?: held,
            estimateQuestion = question ?: out.estimate?.question,
            estimateItems = out.estimate?.items?.map { it.name }.orEmpty(),
            estimateMealText = out.estimate?.mealText?.trim()?.takeIf { it.isNotEmpty() },
            intent = intent,
            pendingMemory = routine.takeIf { it.isNotEmpty() }?.let { memoryJson.encodeToString(UPDATES, it) },
            memoryUsedKinds = usedKinds(out.memoryUsed, facts),
            memoryUpdated = applied?.changed == true,
            recordMode = recordMode,
            recordState = recordState,
            mealChange = proposal?.encode(),
            skipOutcomes = skips?.encode(),
            // A60 part A: only a plan with its estimate carries the budget check; malformed or absent = no choice UI.
            planBudget = PlanBudget.parse(out.planBudget)?.takeIf { intent == "plan" && out.estimate != null }?.encode(),
            noted = applied?.let(::notedOf),
        )
        local.update { it.copy(pending = null, pendingPhoto = null, pendingForce = false, failed = false, pendingAddition = null, pendingFit = null) }
        proposal?.let { proposalShown(it, fresh) }
        if (recordMode == RECORD_AUTO && recordState == ChatRecorder.NOT_RECORDED) {
            recordGuard(proposal?.takeIf { !it.actionable }?.reason ?: GUARD_STALE)
        }
        // A65: a turn that only reports a workout is `auto` with nothing else to record.
        val workoutOnly = out.workout != null && out.estimate == null && out.intent != "skip"
        if (recordMode == RECORD_AUTO && recordState == null && !workoutOnly) autoRecord(answerId, out, snapshot, turn.body.clarifyRounds)
        // A59 (ADR-047): the log first, then each listed skip.
        if (skips != null) applySkips(answerId, turn.body.clarifyRounds)
        // A65 (ADR-049): the workout the user stated, with its own receipt, under the request's day and wipe.
        out.workout?.takeIf { it.kcal in 1..WORKOUT_MAX && it.mode in WORKOUT_MODES }?.let { applyWorkout(it.kcal, it.mode, RecordGuard(date, wipeId)) }
    }

    /**
     * A47: the addition proposal of today's latest answer, while nothing was decided on it (Registrar or Adicionar
     * still open). Only the immediately preceding answer counts; a recorded, cancelled or expired one never goes back.
     */
    private fun continuedAddition(today: List<ChatMessageEntity>): MealProposal? {
        val wiped = today.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
        val last = today.filter { it.role == "assistant" && (wiped == null || it.id > wiped) }
            .maxWithOrNull(compareBy({ it.createdAtEpochMs }, { it.id })) ?: return null
        if (last.recordState != null && last.recordState != ChatRecorder.PENDING_ADD) return null
        return MealProposal.decode(last.mealChange)?.takeIf { it.isAddition }
    }

    /** A47: day, wipe and every state [proposal] was built on still hold now ([sourceOnly]: not its destination). */
    private suspend fun proposalHoldsNow(proposal: MealProposal, sourceOnly: Boolean = false): Boolean {
        val date = SaoPaulo.date(clock.now()).toString()
        val current = repository.slotStates(date)
        val wipe = repository.latestWipeToday()
        val state = { id: Long -> current[id] ?: SlotState.EMPTY }
        return if (sourceOnly) proposal.sourceHolds(date, wipe, state) else proposal.matches(date, wipe, state)
    }

    private fun proposalShown(proposal: MealProposal, fresh: Boolean) {
        if (proposal.reason == MealProposal.OVERFLOW) local.update { it.copy(notice = OVERFLOW_NOTICE) }
        mealUpdateEvent(proposal.operation, if (fresh) "shown" else "stale", proposal.reason)
    }

    private fun mealUpdateEvent(op: String, action: String, reason: String? = null) = telemetry.event(
        TelemetryEvents.MEAL_UPDATE,
        buildMap<String, Any> {
            put("op", op)
            put("action", action)
            reason?.let { put("reason", it) }
        },
    )

    /**
     * The record mark of an answer (A34): the server's, with the client guards (ADR-028 decision 8). A
     * server without `record` and a recordable log estimate gives `ask`: nothing is recorded without a tap.
     */
    private fun recordModeOf(out: ChatOut, intent: String?, question: String?, suggested: Long?): String {
        val log = out.estimate != null && question == null && (intent == null || intent == "log")
        val mode = out.record?.takeIf { it in RECORD_MODES } ?: return if (log) RECORD_ASK else RECORD_NONE
        if (mode != RECORD_AUTO || intent != "log") return mode
        val reason = when {
            question != null || !out.estimate?.question.isNullOrBlank() -> "question_pending"
            out.estimate == null || out.estimate.kcal <= 0.0 -> "not_recordable"
            out.estimate.suggestedSlot == null -> "no_slot"
            suggested == null -> "slot_not_today"
            else -> return mode
        }
        telemetry.event(TelemetryEvents.RECORD_GUARD, mapOf("reason" to reason))
        return RECORD_ASK
    }

    /**
     * The automatic record of an answer: a record of today's log, or (a server without `skip_slots`) a skip by text. With
     * `skip_slots` a skip goes through [applySkips], next to any intent (A59).
     */
    private suspend fun autoRecord(answerId: Long, out: ChatOut, snapshot: DaySnapshot, rounds: Int) {
        val answer = repository.message(answerId) ?: return
        val slots = snapshot.slotsOfDay.refs()
        if (out.intent == "skip" && out.skipSlots != null) return
        if (out.intent == "skip") {
            val slot = out.skipSlot?.toLongOrNull()?.let { id -> slots.firstOrNull { it.id == id } }
            if (slot == null) {
                telemetry.event(TelemetryEvents.RECORD_GUARD, mapOf("reason" to if (out.skipSlot == null) "no_slot" else "slot_not_today"))
                repository.setRecordState(answerId, ChatRecorder.NOT_RECORDED)
                return
            }
            val skipped = runCatching { repository.slotState(answer.date, slot.id).let { it.open && recorder.skip(answerId, slot, it) } }
                .onFailure { if (it is CancellationException) throw it }
            if (!skipped.getOrDefault(false)) {
                notRecorded(answerId, if (skipped.isFailure) GUARD_WRITE_FAILED else GUARD_STALE)
                return
            }
            mealSkipped("skip")
            autoEvent("skip", "empty", "user", rounds)
            return
        }
        val slot = answer.estimateSlotId?.let { id -> slots.firstOrNull { it.id == id } }
        // A47: an addition into an empty or skipped meal records only the added food.
        val proposal = MealProposal.decode(answer.mealChange)?.takeIf { it.isAddition }
        val target = proposal?.target
        if (slot == null || proposal != null && target == null) {
            notRecorded(answerId, GUARD_NO_TARGET)
            return
        }
        val day = repository.messagesOf(answer.date)
        val outcome = runCatching { if (proposal != null && target != null) addInto(answer, proposal, slot, target) else recordInto(answer, day, slot) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrElse {
                notRecorded(answerId, GUARD_WRITE_FAILED)
                return
            }
        when (outcome) {
            is RecordOutcome.Recorded -> autoEvent("log", if (outcome.before.skipped) "skipped" else "empty", sourceOf(answer, day), rounds)
            // An occupied slot asks Substituir below the answer (chatU): not silent.
            is RecordOutcome.Taken -> Unit
            RecordOutcome.Stale -> notRecorded(
                answerId,
                if (proposal?.addition != null && target != null && MealChanges.compose(target, proposal.addition, "user") == null) GUARD_OVERFLOW else GUARD_STALE,
            )
        }
    }

    /** A65: the day's workout number from the Chat; a day, wipe or number that moved writes nothing (record_guard stale). */
    private suspend fun applyWorkout(kcal: Int, mode: String, guard: RecordGuard) {
        val id = runCatching { recorder.workout(kcal, mode, guard) }.onFailure { if (it is CancellationException) throw it }
        when {
            id.isFailure -> recordGuard(GUARD_WRITE_FAILED)
            id.getOrNull() == null -> recordGuard(GUARD_STALE)
            else -> telemetry.event(TelemetryEvents.WORKOUT_SAVED, mapOf("from" to "chat", "mode" to mode, "kcal" to kcal))
        }
    }

    /** A54: an `auto` answer that ends without a record says so (Não registrado, rule 22) and reports why. */
    private suspend fun notRecorded(answerId: Long, reason: String) {
        repository.closeOpenRecord(answerId, ChatRecorder.NOT_RECORDED)
        recordGuard(reason)
    }

    private fun recordGuard(reason: String) = telemetry.event(TelemetryEvents.RECORD_GUARD, mapOf("reason" to reason))

    // ------------------------------------------------------------------ skips next to other actions (A59, ADR-047)

    /**
     * Each still pending skip of [answerId], in list order, by the slot's state now: empty -> skipped with its own receipt
     * (chatSK); skipped -> nothing; with a record -> `Pular {slot}?` (chatSD). The request day and wipe must still hold.
     * Every move is conditional on the stored outcome, so a retry, a recreated screen or a second caller never applies
     * a skip twice. A write failure marks only that skip `Não registrado`.
     */
    private suspend fun applySkips(answerId: Long, rounds: Int) {
        val skips = SkipOutcomes.decode(repository.message(answerId)?.skipOutcomes) ?: return
        val today = SaoPaulo.date(clock.now()).toString()
        val slots = repository.observeToday().first().slotsOfDay.refs()
        for (entry in skips.slots.filter { it.pending }) {
            val slot = slots.firstOrNull { it.id == entry.slotId }
            if (slot == null || !skips.holds(today, repository.latestWipeToday())) {
                repository.moveSkip(SkipMove(answerId, entry.slotId, null, SkipOutcomes.EXPIRED))
                continue
            }
            val applied = runCatching { applySkip(answerId, slot, skips.with, rounds) }.onFailure { if (it is CancellationException) throw it }
            if (applied.isFailure && runCatching { repository.moveSkip(SkipMove(answerId, slot.id, null, SkipOutcomes.FAILED)) }.getOrDefault(false)) {
                recordGuard(GUARD_WRITE_FAILED)
            }
        }
    }

    /** One skip, read at the moment it is applied; a slot that moves under the write is read again. */
    private suspend fun applySkip(answerId: Long, slot: SlotRef, with: String, rounds: Int) {
        val date = SaoPaulo.date(clock.now()).toString()
        repeat(SKIP_ATTEMPTS) {
            val state = repository.slotState(date, slot.id)
            val done = when {
                state.records.isNotEmpty() -> repository.moveSkip(SkipMove(answerId, slot.id, null, SkipOutcomes.PENDING_DELETE, state))
                    .also { if (it) skipDeleteEvent("shown") }
                state.skipped -> repository.moveSkip(SkipMove(answerId, slot.id, null, SkipOutcomes.ALREADY, state))
                else -> recorder.skipListed(answerId, slot, state).also {
                    if (it) {
                        if (state.planned != null) planReserved("cleared_by_skip")
                        mealSkipped(with)
                        autoEvent("skip", "empty", "user", rounds)
                    }
                }
            }
            // Done, or another caller already moved it: nothing left to do.
            if (done || SkipOutcomes.decode(repository.message(answerId)?.skipOutcomes)?.entry(slot.id)?.pending != true) return
        }
        repository.moveSkip(SkipMove(answerId, slot.id, null, SkipOutcomes.EXPIRED))
    }

    private suspend fun resumeSkips() {
        val today = SaoPaulo.date(clock.now()).toString()
        repository.messagesOf(today)
            .filter { m -> m.role == "assistant" && SkipOutcomes.decode(m.skipOutcomes)?.slots?.any { it.pending } == true }
            .forEach { applySkips(it.id, rounds = 0) }
    }

    /**
     * Excluir e pular (chatSD): the slot's records leave and it is skipped, while the request day, the wipe and the slot
     * still hold what the proposal showed. The memory change of the record's active receipt is reverted; Desfazer on the
     * skip receipt brings both back. Anything else expires the proposal.
     */
    fun confirmSkipDelete(answerId: Long, slotId: Long) = once(answerId) {
        val skips = SkipOutcomes.decode(repository.message(answerId)?.skipOutcomes) ?: return@once
        val entry = skips.entry(slotId)?.takeIf { it.outcome == SkipOutcomes.PENDING_DELETE } ?: return@once
        val state = entry.state ?: return@once
        val today = SaoPaulo.date(clock.now()).toString()
        val slot = todaySlot(slotId)
        val receipt = if (slot != null && skips.holds(today, repository.latestWipeToday())) {
            recorder.deleteAndSkip(answerId, slot, state, activeReceipt(today, slotId, state), RecordGuard(skips.date, skips.wipeId))
        } else {
            null
        }
        if (receipt == null) {
            expireSkip(answerId, slotId)
            return@once
        }
        skipDeleteEvent("confirmed")
        mealSkipped(skips.with)
    }

    /** Manter registro (chatSD): nothing is written; the card becomes `Registro mantido`. */
    fun keepRecord(answerId: Long, slotId: Long) = once(answerId) {
        if (repository.moveSkip(SkipMove(answerId, slotId, SkipOutcomes.PENDING_DELETE, SkipOutcomes.KEPT))) skipDeleteEvent("kept")
    }

    /** An open delete proposal becomes `Não registrado`; one already decided stays. */
    private suspend fun expireSkip(answerId: Long, slotId: Long) {
        if (repository.moveSkip(SkipMove(answerId, slotId, SkipOutcomes.PENDING_DELETE, SkipOutcomes.EXPIRED))) skipDeleteEvent("expired")
    }

    /** The active receipt whose record left [slotId] holding [state] on [date], if the Chat made it. */
    private suspend fun activeReceipt(date: String, slotId: Long, state: SlotState): ChatMessageEntity? = repository.receipts()
        .filter { r -> r.receiptState == null && UndoData.decode(r.undoData)?.recordSlot?.let { it.date == date && it.slotId == slotId && it.after == state } == true }
        .maxWithOrNull(compareBy({ it.createdAtEpochMs }, { it.id }))

    private fun mealSkipped(with: String) =
        telemetry.event(TelemetryEvents.MEAL_SKIPPED, mapOf("from" to "chat", "with" to (with.takeIf { it in INTENTS } ?: "log")))

    private fun skipDeleteEvent(action: String) = telemetry.event(TelemetryEvents.SKIP_DELETE, mapOf("action" to action))

    private fun autoEvent(kind: String, slotState: String, source: String, rounds: Int) = telemetry.event(
        TelemetryEvents.MEAL_AUTO_RECORDED,
        mapOf("kind" to kind, "slot_state" to slotState, "source" to source, "rounds" to rounds),
    )

    /** Applies [updates]; null when there was nothing to apply or the write failed (nothing changed). */
    private suspend fun applyMemory(updates: List<MemoryUpdate>): MemoryResult? {
        if (updates.isEmpty()) return null
        val result = runCatching { memory.apply(updates, SaoPaulo.date(clock.now())) }.getOrNull() ?: return null
        memoryChanged(result)
        reloadFacts()
        return result
    }

    /**
     * A64 (ADR-053 § 2): the permanent facts [result] added or rewrote from an explicit statement, as stored, one per line;
     * null when none. A promoted fact or an untouched text is not news.
     */
    private fun notedOf(result: MemoryResult): String? = result.images
        .mapNotNull { image -> image.after?.takeIf { it.permanent && it.source == MemoryRules.EXPLICIT && it.text != image.before?.text } }
        .joinToString("\n") { it.text }
        .ifEmpty { null }

    /** A64: the days before today since the first day of the app, as the closures read them. */
    private suspend fun pastDays(snapshot: DaySnapshot, today: LocalDate): List<DayTotals> {
        val first = snapshot.firstDay.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return (1..DayRepository.RECENT_DAYS).map { today.minusDays(it) }
            .filter { first == null || !it.isBefore(first) }
            .map { repository.dayTotals(snapshot, it) }
    }

    private fun memoryChanged(result: MemoryResult) {
        if (result.counts.values.none { it > 0 }) return
        val params = buildMap<String, Any> {
            (MemoryRules.OPS + MemoryRules.TEMP_OPS).forEach { put(it, result.counts[it] ?: 0) }
            put("permanent", result.memory.facts.count { it.permanent })
            put("dynamic", result.memory.facts.count { it.dynamic })
            put("temp", result.memory.facts.count { it.temp })
        }
        telemetry.event(TelemetryEvents.MEMORY_CHANGED, params)
    }

    /** memory_used ids as the kinds they had when the answer came; an unknown id is ignored. */
    private fun usedKinds(ids: List<String>, facts: List<Fact>): String? {
        val kinds = ids.mapNotNull { id -> facts.firstOrNull { it.id == id }?.kind }.toSet()
        return listOf(MemoryRules.PERMANENT, MemoryRules.DYNAMIC).filter { it in kinds }.joinToString(",").ifEmpty { null }
    }

    private fun chatResult(
        outcome: String,
        hasEstimate: Boolean = false,
        confidence: String? = null,
        intent: String? = null,
        questionOnly: Boolean = false,
        record: String? = null,
    ) {
        val params = buildMap<String, Any> {
            put("outcome", outcome)
            put("has_estimate", hasEstimate)
            put("question_only", questionOnly)
            confidence?.let { put("confidence", it) }
            // Enum only: an unknown value never leaves the device as free text.
            if (outcome == "ok") {
                put("intent", intent?.takeIf { it in INTENTS } ?: "none")
                put("record", record?.takeIf { it in RECORD_MODES } ?: "missing")
            }
        }
        telemetry.event(TelemetryEvents.CHAT_RESULT, params)
    }

    /** Routine add/reinforce stored with the estimate (A28): they count only with its record. */
    private fun routineOf(estimate: ChatMessageEntity): List<RoutineUpdate> {
        val pending = estimate.pendingMemory ?: return emptyList()
        return runCatching { memoryJson.decodeFromString(UPDATES, pending) }.getOrDefault(emptyList())
            .map { RoutineUpdate(it.op, it.id, it.kind, it.category, it.key, it.text, it.slot) }
    }

    /** The record of [estimate] (A34): the meal_log row, the receipt source and its routine. */
    private fun newRecord(estimate: ChatMessageEntity, day: List<ChatMessageEntity>): NewRecord {
        val source = sourceOf(estimate, day)
        return NewRecord(
            record = SlotRecord(
                text = descriptionOf(estimate, day),
                kcal = estimate.estimateKcal ?: 0,
                p = estimate.estimateP ?: 0,
                c = estimate.estimateC ?: 0,
                g = estimate.estimateG ?: 0,
                source = source,
            ),
            source = if (estimate.isPlanEstimate) SOURCE_PLAN else source,
            routine = routineOf(estimate),
            estimateId = estimate.id,
        )
    }

    /**
     * The one record path of an estimate (A34): automatic, Registrar, Registrar assim, the Trocar sheet.
     * Empty or skipped slot → recorded with a receipt. Slot with a record → pending replace below the answer
     * (chatU); nothing changes until Substituir. No second POST.
     */
    private suspend fun recordInto(estimate: ChatMessageEntity, day: List<ChatMessageEntity>, slot: SlotRef): RecordOutcome {
        val outcome = recorder.record(newRecord(estimate, day), slot)
        when (outcome) {
            is RecordOutcome.Recorded -> {
                if (outcome.before.planned != null) planReserved("cleared_by_record")
                mealSaved(estimate, day, hadPlan = outcome.before.planned != null)
            }
            is RecordOutcome.Taken -> {
                val pending = SlotChange(estimate.date, slot.id, outcome.state, outcome.state)
                repository.setRecordState(estimate.id, ChatRecorder.PENDING_REPLACE, UndoData.encodeChange(pending))
                replaceEvent("shown", "answer")
            }
            RecordOutcome.Stale -> Unit
        }
        return outcome
    }

    /** An estimate of today that can still be recorded: no record decided, or a pending replace. */
    private suspend fun openEstimate(estimateId: Long): ChatMessageEntity? = repository.message(estimateId)?.takeIf {
        it.isRecordable && it.date == SaoPaulo.date(clock.now()).toString() && it.recordState in OPEN_STATES
    }

    private suspend fun todaySlot(slotId: Long): SlotRef? = repository.observeToday().first().slotsOfDay.refs().firstOrNull { it.id == slotId }

    /** Registrar (chatE): the `ask` estimate takes the record path. Without a slot of today, Trocar with nothing picked. */
    fun register(estimateId: Long) {
        val actions = _uiState.value.actions?.takeIf { it.estimateId == estimateId } ?: return
        if (!actions.plan) telemetry.event(TelemetryEvents.RECORD_ASK, mapOf("action" to "tapped"))
        val slot = actions.record
        if (slot == null) {
            local.update { it.copy(sheet = Sheet.Estimate(estimateId), sheetSelection = null) }
            return
        }
        once(estimateId) {
            val estimate = openEstimate(estimateId) ?: return@once
            val proposal = MealProposal.decode(estimate.mealChange)?.takeIf { it.isAddition }
            if (proposal != null) {
                addInto(estimate, proposal, slot, proposal.target ?: return@once)
            } else {
                recordInto(estimate, repository.messagesOf(estimate.date), slot)
            }
        }
    }

    // ------------------------------------------------------------------ additions and revisions (A47, ADR-032)

    /**
     * The addition of [estimate] into [slot], which held [destination] when it was shown or picked. An empty or skipped
     * slot gets only the addition; a slot with a record keeps it and gains the addition, as one record. A source left
     * behind is not written, but it, the day, the wipe and the still open answer are checked in the same transaction.
     * Anything else writes nothing and the proposal expires.
     */
    private suspend fun addInto(estimate: ChatMessageEntity, proposal: MealProposal, slot: SlotRef, destination: SlotState): RecordOutcome {
        val addition = proposal.addition ?: return RecordOutcome.Stale
        val day = repository.messagesOf(estimate.date)
        val source = sourceOf(estimate, day)
        val record = MealChanges.compose(destination, addition, source)
        if (record == null) {
            // Never a cut description: the proposal stays as it is, with a short note.
            local.update { it.copy(notice = OVERFLOW_NOTICE) }
            mealUpdateEvent(proposal.operation, "overflow")
            return RecordOutcome.Stale
        }
        val checks = proposal.sourceSlotId?.takeIf { it != slot.id }?.let { listOf(SlotCheck(proposal.date, it, proposal.source ?: SlotState.EMPTY)) }.orEmpty()
        val outcome = recorder.record(
            NewRecord(record, source, routineOf(estimate), estimate.id),
            slot,
            expected = destination,
            guard = RecordGuard(proposal.date, proposal.wipeId, checks, open = estimate.id),
        )
        if (outcome is RecordOutcome.Recorded) {
            mealUpdateEvent(proposal.operation, "confirmed")
            if (outcome.before.planned != null) planReserved("cleared_by_record")
            mealSaved(estimate, day, record.kcal, hadPlan = outcome.before.planned != null)
        } else {
            expireProposal(estimate, proposal)
        }
        return outcome
    }

    /** Adicionar (chatI): the addition into its current destination, after the user saw its total. */
    fun confirmAddition(estimateId: Long) = once(estimateId) {
        val estimate = openEstimate(estimateId)?.takeIf { it.recordState == ChatRecorder.PENDING_ADD } ?: return@once
        val proposal = MealProposal.decode(estimate.mealChange)?.takeIf { it.isAddition } ?: return@once
        val slot = proposal.destinationSlotId?.let { todaySlot(it) }
        val destination = proposal.destination
        if (slot == null || destination == null) {
            expireProposal(estimate, proposal)
            return@once
        }
        addInto(estimate, proposal, slot, destination)
    }

    /** Escolher outra refeição (chatI → chatTI): the picker for the added food only, nothing picked. */
    fun additionElsewhere(estimateId: Long) {
        mealUpdateEvent(MealProposal.ADD, "elsewhere")
        local.update { it.copy(sheet = Sheet.Estimate(estimateId), sheetSelection = null) }
    }

    /**
     * A destination picked for an addition (chatTI). The source must still hold. The source itself: back to its own
     * confirmation. Empty or skipped: only the addition is recorded there now. Another meal with a record: nothing is
     * written, Adicionar asks again with that meal's numbers.
     */
    private suspend fun chooseDestination(estimate: ChatMessageEntity, proposal: MealProposal, slot: SlotRef) {
        val date = SaoPaulo.date(clock.now()).toString()
        val current = repository.slotStates(date)
        val state = { id: Long -> current[id] ?: SlotState.EMPTY }
        if (!proposal.sourceHolds(date, repository.latestWipeToday(), state)) {
            expireProposal(estimate, proposal)
            return
        }
        val destination = state(slot.id)
        val chosen = proposal.choose(slot.id, destination)
        when {
            destination.records.isEmpty() -> addInto(estimate, chosen, slot, destination)
            slot.id != proposal.sourceSlotId && MealChanges.compose(destination, proposal.addition ?: return, "user") == null -> {
                local.update { it.copy(notice = OVERFLOW_NOTICE) }
                mealUpdateEvent(proposal.operation, "overflow")
            }
            else -> if (repository.setMealChange(estimate.id, ChatRecorder.PENDING_ADD, chosen.encode())) mealUpdateEvent(proposal.operation, "shown")
        }
    }

    /** Atualizar (chatIC): the revised meal replaces its source, which must still be what the revision was built on. */
    fun confirmRevision(estimateId: Long) = once(estimateId) {
        val estimate = openEstimate(estimateId)?.takeIf { it.recordState == ChatRecorder.PENDING_REVISE } ?: return@once
        val proposal = MealProposal.decode(estimate.mealChange)?.takeIf { it.isRevision } ?: return@once
        val slot = proposal.sourceSlotId?.let { todaySlot(it) }
        val day = repository.messagesOf(estimate.date)
        val outcome = slot?.let {
            recorder.record(newRecord(estimate, day), it, expected = proposal.source, guard = RecordGuard(proposal.date, proposal.wipeId, open = estimate.id))
        }
        if (outcome is RecordOutcome.Recorded) {
            mealUpdateEvent(proposal.operation, "confirmed")
            mealSaved(estimate, day)
        } else {
            expireProposal(estimate, proposal)
        }
    }

    /** Cancelar (chatIC): Não registrado; Room and the memory stay as they are. */
    fun cancelRevision(estimateId: Long) = once(estimateId) {
        if (repository.closeOpenRecord(estimateId, ChatRecorder.NOT_RECORDED)) mealUpdateEvent(MealProposal.REVISE, "cancelled")
    }

    /** An open proposal becomes Não registrado; one already decided (a tap that won the race) stays. */
    private suspend fun expireProposal(estimate: ChatMessageEntity, proposal: MealProposal) {
        if (repository.closeOpenRecord(estimate.id, ChatRecorder.NOT_RECORDED)) mealUpdateEvent(proposal.operation, "expired")
    }

    /** One tap at a time per answer: a double tap or a second button never runs a record twice. */
    private fun once(id: Long, block: suspend () -> Unit) {
        if (!inFlight.add(id)) return
        viewModelScope.launch {
            try {
                block()
            } finally {
                inFlight.remove(id)
            }
        }
    }

    /** Registrar assim (chatR): same path as Registrar (A29). */
    fun recordPlan(estimateId: Long) = register(estimateId)

    /** Substituir below an answer (chatU): today's record(s) of the slot become this estimate, one transaction. */
    fun confirmReplace(estimateId: Long) {
        viewModelScope.launch {
            val estimate = openEstimate(estimateId)?.takeIf { it.recordState == ChatRecorder.PENDING_REPLACE } ?: return@launch
            val pending = UndoData.decodeChange(estimate.undoData) ?: return@launch
            val slot = todaySlot(pending.slotId)
            val day = repository.messagesOf(estimate.date)
            val outcome = slot?.let { recorder.record(newRecord(estimate, day), it, expected = pending.before) }
            if (outcome !is RecordOutcome.Recorded) {
                expirePending(estimate)
                return@launch
            }
            replaceEvent("confirmed", "answer")
            mealSaved(estimate, day)
        }
    }

    /** Outra refeição below an answer: Trocar with nothing picked; the confirmation stays until a slot is chosen. */
    fun replaceElsewhere(estimateId: Long) {
        replaceEvent("elsewhere", "answer")
        local.update { it.copy(sheet = Sheet.Estimate(estimateId), sheetSelection = null) }
    }

    private suspend fun expirePending(estimate: ChatMessageEntity) {
        if (repository.closeOpenRecord(estimate.id, ChatRecorder.NOT_RECORDED)) replaceEvent("expired", "answer")
    }

    /**
     * Next send (A34): every open Registrar and pending Substituir becomes `Não registrado`; a pending move
     * confirmation closes.
     */
    private suspend fun expireOpen() {
        local.value.moveConfirm?.let { replaceEvent("expired", "move") }
        local.update { it.copy(moveConfirm = null) }
        repository.openRecords().forEach { row ->
            if (!repository.closeOpenRecord(row.id, ChatRecorder.NOT_RECORDED)) return@forEach
            val proposal = MealProposal.decode(row.mealChange)
            when {
                row.recordState == ChatRecorder.PENDING_REPLACE -> replaceEvent("expired", "answer")
                row.recordState == ChatRecorder.PENDING_ADD || row.recordState == ChatRecorder.PENDING_REVISE ->
                    mealUpdateEvent(proposal?.operation ?: MealProposal.INVALID, "expired")
                else -> telemetry.event(TelemetryEvents.RECORD_ASK, mapOf("action" to "expired"))
            }
        }
        // A59: a delete proposal dies on the next send too.
        repository.openSkips().forEach { row ->
            SkipOutcomes.decode(row.skipOutcomes)?.slots?.filter { it.outcome == SkipOutcomes.PENDING_DELETE }?.forEach { expireSkip(row.id, it.slotId) }
        }
    }

    private fun replaceEvent(action: String, from: String) =
        telemetry.event(TelemetryEvents.REPLACE_CONFIRM, mapOf("action" to action, "from" to from))

    private fun sourceOf(estimate: ChatMessageEntity, day: List<ChatMessageEntity>) =
        if (userBefore(estimate, day)?.photoPath != null) "photo" else "user"

    private fun mealSaved(estimate: ChatMessageEntity, day: List<ChatMessageEntity>, kcal: Int = estimate.estimateKcal ?: 0, hadPlan: Boolean = false) =
        telemetry.event(
            TelemetryEvents.MEAL_SAVED,
            mapOf("from" to "chat", "has_photo" to (userBefore(estimate, day)?.photoPath != null), "kcal" to kcal, "had_plan" to hadPlan),
        )

    // ------------------------------------------------------------------ receipt actions (A34)

    /** Desfazer · Excluir · Trocar refeição · Editar on the latest receipt of its slot (ADR-028 decision 5). */
    fun receiptAction(receiptId: Long, action: ReceiptAction) {
        val item = _uiState.value.items.firstOrNull { it is ChatItem.Receipt && it.id == receiptId } as? ChatItem.Receipt
        if (item == null || action !in item.actions) return
        viewModelScope.launch {
            val receipt = repository.message(receiptId)?.takeIf { it.receiptState == null } ?: return@launch
            when (action) {
                ReceiptAction.DELETE -> recorder.delete(receipt, ChatRecorder.DELETED) ?: return@launch
                ReceiptAction.UNDO -> {
                    if (!recorder.undo(receipt, ::slotName)) return@launch
                    // A59: Desfazer of an Excluir e pular brings the record back.
                    if (receipt.role == ReceiptRules.SKIPPED && UndoData.decode(receipt.undoData)?.slots?.any { it.before.records.isNotEmpty() } == true) {
                        skipDeleteEvent("undone")
                    }
                }
                ReceiptAction.EDIT -> {
                    val records = recorder.delete(receipt, ChatRecorder.EDITED) ?: return@launch
                    val text = records.joinToString(", ") { it.text }
                    local.update {
                        it.copy(composer = text, composerTooLong = ChatText.tooLong(text), focusComposer = it.focusComposer + 1)
                    }
                }
                ReceiptAction.MOVE -> {
                    val from = UndoData.decode(receipt.undoData)?.recordSlot ?: return@launch
                    local.update { it.copy(sheet = Sheet.Receipt(receiptId, from.slotId), sheetSelection = null, moveConfirm = null) }
                }
            }
            recorder.receiptEvent(action, receipt)
        }
    }

    /** Substituir below a receipt (Trocar refeição into a slot with a record): move and replace in one transaction. */
    fun confirmMove() {
        val confirm = local.value.moveConfirm ?: return
        local.update { it.copy(moveConfirm = null) }
        viewModelScope.launch {
            val receipt = repository.message(confirm.receiptId)?.takeIf { it.receiptState == null } ?: return@launch
            if (recorder.move(receipt, confirm.target, expected = confirm.state) is RecordOutcome.Recorded) {
                replaceEvent("confirmed", "move")
            } else {
                replaceEvent("expired", "move")
            }
        }
    }

    /** Outra refeição below a receipt: Trocar again with nothing picked. */
    fun moveElsewhere() {
        val confirm = local.value.moveConfirm ?: return
        replaceEvent("elsewhere", "move")
        viewModelScope.launch {
            val from = repository.message(confirm.receiptId)?.let { UndoData.decode(it.undoData)?.recordSlot } ?: return@launch
            local.update { it.copy(moveConfirm = null, sheet = Sheet.Receipt(confirm.receiptId, from.slotId), sheetSelection = null) }
        }
    }

    /** Slot name for a restored receipt: the profile's, any weekday. */
    private suspend fun slotName(slotId: Long): String =
        repository.observeToday().first().slots.firstOrNull { it.id == slotId }?.name ?: "Outros"

    // ------------------------------------------------------------------ routine suggestion (A29)

    /**
     * Registrar (chatS): the routine as is, in its slot. No POST. Receipt like any record, the routine
     * reinforced with this record (and reverted by Excluir / Editar).
     */
    fun recordRoutine() {
        val suggestion = _uiState.value.routine ?: return
        viewModelScope.launch {
            val slot = suggestion.slot
            val today = repository.observeToday().first()
            // The card only exists for an empty slot; a record that raced it wins.
            if (today.slotsOfDay.none { it.id == slot.id }) return@launch
            val new = NewRecord(
                record = SlotRecord(suggestion.text, suggestion.kcal, suggestion.p, suggestion.c, suggestion.g, SOURCE_ROUTINE),
                source = SOURCE_ROUTINE,
                routine = listOf(RoutineUpdate(MemoryRules.REINFORCE, suggestion.factId, MemoryRules.DYNAMIC, MemoryRules.ROUTINE, slot = slot.id.toString())),
                estimateId = null,
            )
            val state = repository.slotState(today.date, slot.id).takeIf { it.open } ?: return@launch
            if (recorder.record(new, slot, expected = state) !is RecordOutcome.Recorded) return@launch
            routineEvent("record")
            telemetry.event(TelemetryEvents.MEAL_SAVED, mapOf("from" to SOURCE_ROUTINE, "has_photo" to false, "kcal" to suggestion.kcal))
        }
    }

    /** Quase igual: the routine text replaces the composer, focused, cursor at the end. Nothing recorded. */
    fun editRoutine() {
        val suggestion = _uiState.value.routine ?: return
        local.update {
            it.copy(
                composer = suggestion.text,
                composerTooLong = ChatText.tooLong(suggestion.text),
                focusComposer = it.focusComposer + 1,
            )
        }
        routineEvent("edit")
    }

    private fun routineEvent(action: String) = telemetry.event(TelemetryEvents.ROUTINE_SUGGESTION, mapOf("action" to action))

    fun selectInSheet(slotId: Long) = local.update { it.copy(sheetSelection = slotId) }

    fun closeSheet() = local.update { it.copy(sheet = null, sheetSelection = null) }

    /** Trocar → Confirmar: records the estimate into the chosen slot, or moves the receipt's record there. */
    fun confirmSheet() {
        val l = local.value
        val sheet = l.sheet ?: return
        val slotId = l.sheetSelection ?: return
        local.update { it.copy(sheet = null, sheetSelection = null) }
        viewModelScope.launch {
            val slot = todaySlot(slotId) ?: return@launch
            when (sheet) {
                is Sheet.Estimate -> {
                    val estimate = openEstimate(sheet.id) ?: return@launch
                    val proposal = MealProposal.decode(estimate.mealChange)?.takeIf { it.isAddition }
                    if (proposal != null) {
                        if (inFlight.add(estimate.id)) {
                            try {
                                chooseDestination(estimate, proposal, slot)
                            } finally {
                                inFlight.remove(estimate.id)
                            }
                        }
                    } else {
                        recordInto(estimate, repository.messagesOf(estimate.date), slot)
                    }
                }
                is Sheet.Receipt -> {
                    val receipt = repository.message(sheet.id)?.takeIf { it.receiptState == null } ?: return@launch
                    val outcome = recorder.move(receipt, slot)
                    if (outcome is RecordOutcome.Taken) {
                        val kcal = UndoData.decode(receipt.undoData)?.recordSlot?.after?.kcal ?: 0
                        val confirm = ReplaceConfirm(slot, oldKcal = outcome.state.kcal, newKcal = kcal)
                        local.update { it.copy(moveConfirm = MoveConfirm(receipt.id, slot, outcome.state, confirm)) }
                        replaceEvent("shown", "move")
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ render

    /** [w]: the loaded page (oldest first, A32), today's whole conversation and the receipts of the 60 days. */
    private fun render(d: DaySnapshot, w: Window, l: Local, facts: List<Fact>): ChatUiState {
        val window = w.rows
        val now = clock.now()
        val today = SaoPaulo.date(now)
        val todayIso = today.toString()
        val slots = d.slotsOfDay.refs()
        val slotById = slots.associateBy { it.id }
        val allSlotTimes = d.slots.associate { it.id to SlotSuggestions.format(it.minutesFromMidnight) }
        val eaten = Macros(d.logs.sumOf { it.kcal }, d.logs.sumOf { it.p }, d.logs.sumOf { it.carbs }, d.logs.sumOf { it.fat })
        val budget = d.budgetOn(today)
        val targets = Macros(0, d.proteinTargetG, d.carbTargetG, d.fatTargetG)
        // Date pills, plans and memory notices: the window only. Actions and rounds: all of today.
        val todayRows = w.today.filter { it.date == todayIso }
        val project = { m: ChatMessageEntity ->
            val plan = Macros(m.estimateKcal ?: 0, m.estimateP ?: 0, m.estimateC ?: 0, m.estimateG ?: 0)
            // A recorded plan is already inside the day: it is counted once, not projected on top.
            val base = if (recordedPlan(m, todayRows)) eaten.minus(plan) else eaten
            // A60 part D: the plans reserved for the other meals count in the day; this plan's own meal is the plan itself.
            val others = d.planned.filterKeys { it != m.estimateSlotId }.values
            val reserved = Macros(others.sumOf { it.kcal }, others.sumOf { it.p }, others.sumOf { it.c }, others.sumOf { it.g })
            ProjectedDay.of(budget, base, plan, targets, reserved)
        }
        val todayStates = todayStates(d)
        val stateOf = { date: String, slotId: Long ->
            (if (date == todayIso) todayStates else w.pastSlots[date].orEmpty())[slotId] ?: SlotState.EMPTY
        }
        // A65: today's workout from the snapshot; another day's is never undone from the Chat.
        val receiptActions = receiptActions(w.receipts, stateOf) { date -> if (date == todayIso) d.workoutKcal else WORKOUT_UNKNOWN }
        // Registrar (chatE) or Registrar assim (chatR): the last estimate of today that is still open.
        val lastEstimate = todayRows.lastOrNull { it.role == "assistant" && it.isRecordable }
        val open = lastEstimate?.takeIf { l.pending == null && isOpen(it, todayRows) }
        // A60 part A (chatRB): the most recent plan of today, before any new send, over its window and not accepted.
        val choice = open?.takeIf { p -> p.isPlanEstimate && todayRows.none { it.role == "user" && it.id > p.id } }
            ?.let { PlanBudget.decode(it.planBudget) }?.takeIf { it.choosing }
        val stalePending = mutableListOf<ChatMessageEntity>()
        val wipeToday = todayRows.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
        // A64: the day balance rides on today's newest active record receipt; an estimate waiting for Registrar projects it.
        val meta = d.metaOn(today)
        val balanceReceipt = todayRows.lastOrNull { it.role in BALANCE_RECEIPTS && it.receiptState == null && (wipeToday == null || it.id > wipeToday) }?.id
        val balance = DayBalance.line(eaten, meta, d.proteinTargetG)
        wipeMark = wipeToday ?: 0L

        val items = mutableListOf<ChatItem>()
        var lastDate: String? = null
        // A59: an answer's skip proposal and marks wait below its receipts (the log's and the skips' own).
        val trailing = mutableListOf<ChatItem>()
        val staleSkips = mutableListOf<Pair<Long, Long>>()
        // The wipe marker only cuts the prompt; it is never drawn.
        val visible = window.filter { it.role != DayRepository.ROLE_WIPED }
        for (m in visible) {
            if (m.role !in ReceiptRules.ROLES || m.date != lastDate) {
                items += trailing
                trailing.clear()
            }
            if (m.date != lastDate) {
                items += ChatItem.DateSeparator(dateLabel(LocalDate.parse(m.date), today))
                lastDate = m.date
            }
            val time = timeOf(m.createdAtEpochMs)
            if (m.role in ReceiptRules.ROLES) {
                items += receipt(m, allSlotTimes, receiptActions[m.id].orEmpty(), l.moveConfirm?.takeIf { it.receiptId == m.id }?.confirm)
                    .let { r -> if (m.id == balanceReceipt) r.copy(balance = balance) else r }
                continue
            }
            if (m.role == "user") {
                items += ChatItem.User(m.id, m.text, time, photoPath = m.photoPath)
                continue
            }
            // A30: a question before the estimate is only its question bubble (chatQ).
            if (PromptBuilder.isQuestionOnly(m)) {
                items += ChatItem.Question(m.id, m.estimateQuestion.orEmpty().trim(), time, standalone = true, memory = memoryOf(m))
                continue
            }
            // A34: a pending replace asks below the answer while its slot still holds what it asked about.
            val pending = UndoData.decodeChange(m.undoData)?.takeIf {
                m.recordState == ChatRecorder.PENDING_REPLACE && m.date == todayIso
            }
            val pendingSlot = pending?.let { slotById[it.slotId] }
            val asking = pending != null && pendingSlot != null && stateOf(pending.date, pending.slotId) == pending.before
            if (pending != null && !asking) stalePending += m
            // A47: an addition or revision still open asks while the day, the wipe and every state it was built on hold.
            val proposal = MealProposal.decode(m.mealChange)?.takeIf { it.actionable }
            val proposalOpen = proposal != null && m.date == todayIso &&
                (m.recordState == ChatRecorder.PENDING_ADD || m.recordState == ChatRecorder.PENDING_REVISE || m.recordState == null && proposal.isAddition)
            val holds = proposal != null && proposal.matches(todayIso, wipeToday) { stateOf(todayIso, it) } &&
                proposal.destinationSlotId?.let { it in slotById } != false
            if (proposalOpen && !holds) stalePending += m
            val notRecorded = m.recordMode != null && m.isRecordable && when (m.recordState) {
                ChatRecorder.NOT_RECORDED -> true
                ChatRecorder.PENDING_REPLACE -> !asking
                ChatRecorder.PENDING_ADD, ChatRecorder.PENDING_REVISE -> !(proposalOpen && holds)
                null -> m.recordMode == RECORD_ASK && m.date != todayIso
                else -> false
            }
            items += assistant(
                m,
                time,
                slotById,
                plan = if (m.isPlanEstimate && m.date == todayIso) project(m) else null,
                offer = open?.id == m.id && !m.isPlanEstimate,
                notRecorded = notRecorded,
                budget = choice?.takeIf { open?.id == m.id }?.let { BudgetNote(it.overKcal, it.reserved.map { r -> r.kcal to r.label }) },
                reservedFor = m.estimateSlotId?.takeIf { m.date == todayIso && d.planned[it]?.sourceMessageId == m.id }?.let { slotById[it]?.name },
                projection = m.takeIf { open?.id == it.id && !it.isPlanEstimate }?.let { projected(it) }?.let { DayBalance.projection(eaten, it, meta, d.proteinTargetG) },
            )
            if (proposalOpen && holds && proposal != null) {
                when (m.recordState) {
                    ChatRecorder.PENDING_ADD -> additionPrompt(m.id, proposal, slotById)?.let { items += it }
                    ChatRecorder.PENDING_REVISE -> revisionPrompt(m, proposal, slotById)?.let { items += it }
                }
            }
            if (m.isLogEstimate) {
                m.estimateQuestion?.takeIf { it.isNotBlank() }?.let { items += ChatItem.Question(m.id, it, time) }
            }
            if (asking) {
                items += ChatItem.ReplacePrompt(m.id, ReplaceConfirm(pendingSlot!!, oldKcal = pending!!.before.kcal, newKcal = m.estimateKcal ?: 0))
            }
            val ownCard = asking || proposalOpen && holds && m.recordState in setOf(ChatRecorder.PENDING_ADD, ChatRecorder.PENDING_REVISE)
            trailing += skipItems(m, ownCard, todayIso, wipeToday, slotById, stateOf, staleSkips)
        }
        items += trailing
        expireStale(stalePending)
        expireStaleSkips(staleSkips)

        val emptyDay = todayRows.none { it.role != DayRepository.ROLE_WIPED } && l.pending == null
        if (emptyDay) {
            if (lastDate != todayIso) items += ChatItem.DateSeparator(dateLabel(today, today))
            items += ChatItem.Greeting(timeOf((l.openedAt ?: now).toEpochMilli()))
        }
        l.pending?.let { text ->
            if (lastDate != todayIso && !emptyDay) items += ChatItem.DateSeparator(dateLabel(today, today))
            items += ChatItem.User(-1, text, timeOf(now.toEpochMilli()), pending = true, photoPath = l.pendingPhoto)
            items += when {
                l.failed -> ChatItem.Failed
                l.slow -> ChatItem.Thinking
                else -> ChatItem.Loading
            }
        }

        val nowMinutes = SlotClock.minutesFromMidnight(now)
        val current = SlotClock.current(slots, nowMinutes) { it.minutes }
        val actions = open?.let {
            EstimateActions(
                it.id,
                record = it.estimateSlotId?.let { id -> slotById[id] },
                plan = it.isPlanEstimate,
                choice = choice?.let { b -> BudgetChoice(b.limitKcal, b.overKcal) },
                // A60 part D: a plan of today for a meal of today with nothing eaten, not already its reservation.
                reserve = it.estimateSlotId?.takeIf { id ->
                    it.isPlanEstimate && (todayStates[id]?.open != false) && d.planned[id]?.sourceMessageId != it.id
                }?.let { id -> slotById[id] },
            )
        }
        // A30: from the second question in a row, Forçar estimativa takes the actions slot (chatQ).
        val forceEstimate = l.pending == null && l.attachment == null &&
            todayRows.lastOrNull()?.let(PromptBuilder::isQuestionOnly) == true &&
            PromptBuilder.clarifyRounds(todayRows) >= FORCE_FROM_ROUND
        val shownActions = actions.takeUnless { forceEstimate }
        shownActions?.takeIf { !it.plan && shownAsks.add(it.estimateId) }?.let {
            telemetry.event(TelemetryEvents.RECORD_ASK, mapOf("action" to "shown"))
        }
        // A47 (chatTI): the picker of an addition with a source meal directs the added food only.
        val sheetProposal = (l.sheet as? Sheet.Estimate)?.let { s -> todayRows.firstOrNull { it.id == s.id } }
            ?.let { MealProposal.decode(it.mealChange) }?.takeIf { it.isAddition }
        val sheetAddition = sheetProposal?.let { p ->
            val source = p.sourceSlotId?.let(slotById::get) ?: return@let null
            p.addition?.let { SheetAddition(it.mealText, it.kcal, source.name) }
        }
        val routine = if (l.sentOnce || l.pending != null) null else routineFor(current, d, facts)
        routine?.let { items += ChatItem.Routine(it) }
        routine?.takeIf { shownRoutines.add(it.factId + "@" + it.slot.id) }?.let { routineEvent("shown") }
        // A61 part B: the selection holds while its messages are drawn, on the day it started, before a send or a wipe.
        val selected = if (l.selectedOn != todayIso || l.pending != null || (wipeToday ?: 0L) != l.selectedWipe) emptySet() else {
            items.filter { it.key in l.selected && it.copyText != null }.mapTo(LinkedHashSet()) { it.key }
        }
        return ChatUiState(
            items = items,
            composer = l.composer,
            composerTooLong = l.composerTooLong,
            sending = l.pending != null && !l.failed,
            emptyDay = emptyDay,
            metaRemaining = (meta - eaten.kcal).coerceAtLeast(0),
            metaTotal = meta,
            actions = shownActions,
            forceEstimate = forceEstimate,
            slots = slots,
            currentSlotId = current?.id,
            sheetFor = l.sheet?.id,
            photoSheet = l.photoSheet,
            attachment = l.attachment,
            notice = l.notice,
            sheetSelection = l.sheetSelection,
            sheetCurrent = when (val sheet = l.sheet) {
                is Sheet.Receipt -> sheet.current
                // An addition is never directed by the hour: no "(atual)".
                else -> current?.id.takeIf { sheetProposal == null }
            },
            sheetAddition = sheetAddition,
            routine = routine,
            focusComposer = l.focusComposer,
            selected = selected,
            copied = l.copied,
        )
    }

    /**
     * Registrar or Registrar assim still applies to [m] (A34): an `ask` log with nothing decided, or a plan
     * not recorded yet (an old plan row: no receipt after it, as before A34). An old log row has no actions.
     */
    private fun isOpen(m: ChatMessageEntity, todayRows: List<ChatMessageEntity>): Boolean = when {
        m.isPlanEstimate -> m.recordState == null &&
            todayRows.none { it.createdAtEpochMs >= m.createdAtEpochMs && it.id != m.id && it.role in CLOSERS }
        else -> m.recordMode == RECORD_ASK && m.recordState == null
    }

    /** Today's slots as the receipts compare them: logs in id order, the skip and the reservation (A60 part D). */
    private fun todayStates(d: DaySnapshot): Map<Long, SlotState> {
        val logs = d.logs.filter { it.slotId != null }.groupBy { it.slotId!! }
        return (logs.keys + d.skippedSlotIds + d.planned.keys).associateWith { id ->
            SlotState(
                logs[id].orEmpty().map { SlotRecord(it.text, it.kcal, it.p, it.carbs, it.fat, it.source, it.window, it.stable) },
                id in d.skippedSlotIds,
                d.planned[id],
            )
        }
    }

    /**
     * The buttons of each receipt (A34): active, the latest receipt of every slot it touched
     * ([ReceiptRules.latest]), and every slot still exactly as the receipt left it (a change by another path
     * hides them, so an action never reverses a state that is gone).
     */
    private fun receiptActions(
        receipts: List<ChatMessageEntity>,
        stateOf: (String, Long) -> SlotState,
        workoutOf: (String) -> Int?,
    ): Map<Long, List<ReceiptAction>> {
        val undo = receipts.associate { it.id to UndoData.decode(it.undoData) }
        val rule = receipts.map { r ->
            val touched = undo[r.id]?.let { u -> (u.slots.map { it.date to it.slotId } + listOfNotNull(u.workout?.let { it.date to ReceiptRules.WORKOUT_SLOT })).toSet() }
                ?: setOfNotNull(r.estimateSlotId?.let { r.date to it })
            ReceiptRules.Receipt(r.id, r.createdAtEpochMs, touched, active = r.receiptState == null && undo[r.id] != null)
        }
        val latest = ReceiptRules.latest(rule)
        return receipts.filter { it.id in latest }.mapNotNull { r ->
            val data = undo[r.id] ?: return@mapNotNull null
            if (data.slots.any { stateOf(it.date, it.slotId) != it.after }) return@mapNotNull null
            // A65: Desfazer of a workout only while the day still holds the number it wrote (the Home dialog may change it).
            if (data.workout?.let { workoutOf(it.date) != it.after } == true) return@mapNotNull null
            r.id to ReceiptRules.actions(r.role, r.recordSource, data)
        }.toMap()
    }

    /**
     * A pending replace whose slot looked changed by another path: `Não registrado`, after checking Room again
     * (the day and the thread are two observations and can arrive one frame apart).
     */
    private fun expireStale(rows: List<ChatMessageEntity>) {
        rows.filter { expiring.add(it.id) }.forEach { row ->
            viewModelScope.launch {
                val fresh = repository.message(row.id)
                if (fresh?.recordState == ChatRecorder.PENDING_REPLACE) {
                    val pending = UndoData.decodeChange(fresh.undoData)
                    if (pending != null && repository.slotState(pending.date, pending.slotId) != pending.before) expirePending(fresh)
                } else {
                    // A47: an open addition or revision whose day, wipe, source or destination changed by another path.
                    val proposal = MealProposal.decode(fresh?.mealChange)?.takeIf { it.actionable }
                    if (fresh != null && proposal != null && fresh.recordState in OPEN_STATES) {
                        val slotGone = proposal.destinationSlotId?.let { todaySlot(it) == null } == true
                        if (slotGone || !proposalHoldsNow(proposal)) expireProposal(fresh, proposal)
                    }
                }
                expiring.remove(row.id)
            }
        }
    }

    /**
     * A59: what an answer's skips show below it. One card at a time: the answer's own confirmation (chatU, chatI, chatIC)
     * first, then the first delete proposal that still holds (chatSD); kept ones show `Registro mantido`, expired or failed
     * ones `Não registrado`. A proposal whose day, wipe or slot moved is reported in [stale] and shown as expired.
     */
    private fun skipItems(
        m: ChatMessageEntity,
        ownCard: Boolean,
        todayIso: String,
        wipeToday: Long?,
        slotById: Map<Long, SlotRef>,
        stateOf: (String, Long) -> SlotState,
        stale: MutableList<Pair<Long, Long>>,
    ): List<ChatItem> {
        val skips = SkipOutcomes.decode(m.skipOutcomes) ?: return emptyList()
        var carded = ownCard
        return skips.slots.mapNotNull { e ->
            when (e.outcome) {
                SkipOutcomes.PENDING_DELETE -> {
                    val slot = slotById[e.slotId]
                    val state = e.state
                    val live = slot != null && state != null && skips.holds(todayIso, wipeToday) && stateOf(todayIso, e.slotId) == state
                    when {
                        !live -> {
                            stale += m.id to e.slotId
                            ChatItem.SkipMark(m.id, e.slotId, kept = false)
                        }
                        carded -> null
                        else -> {
                            carded = true
                            ChatItem.SkipDeletePrompt(m.id, SkipDeleteConfirm(slot!!, state!!.kcal))
                        }
                    }
                }
                SkipOutcomes.KEPT -> ChatItem.SkipMark(m.id, e.slotId, kept = true)
                SkipOutcomes.EXPIRED, SkipOutcomes.FAILED -> ChatItem.SkipMark(m.id, e.slotId, kept = false)
                else -> null
            }
        }
    }

    /** A delete proposal that looked stale: expired after checking Room again (two observations, one frame apart). */
    private fun expireStaleSkips(stale: List<Pair<Long, Long>>) {
        stale.filter { expiringSkips.add(it) }.forEach { (answerId, slotId) ->
            viewModelScope.launch {
                val skips = SkipOutcomes.decode(repository.message(answerId)?.skipOutcomes)
                val entry = skips?.entry(slotId)
                if (skips != null && entry?.outcome == SkipOutcomes.PENDING_DELETE) {
                    val today = SaoPaulo.date(clock.now()).toString()
                    val holds = skips.holds(today, repository.latestWipeToday()) && todaySlot(slotId) != null &&
                        repository.slotState(today, slotId) == entry.state
                    if (!holds) expireSkip(answerId, slotId)
                }
                expiringSkips.remove(answerId to slotId)
            }
        }
    }

    /** chatI: the current destination of [proposal], its recorded kcal and the meal after the addition (the record's numbers). */
    private fun additionPrompt(estimateId: Long, proposal: MealProposal, slotById: Map<Long, SlotRef>): ChatItem.AdditionPrompt? {
        val slot = proposal.destinationSlotId?.let(slotById::get) ?: return null
        val destination = proposal.destination ?: return null
        val total = MealChanges.compose(destination, proposal.addition ?: return null, "user") ?: return null
        return ChatItem.AdditionPrompt(estimateId, AdditionConfirm(slot, destination.kcal, Macros(total.kcal, total.p, total.c, total.g)))
    }

    /** chatIC: the source's recorded kcal and the revised meal's. */
    private fun revisionPrompt(m: ChatMessageEntity, proposal: MealProposal, slotById: Map<Long, SlotRef>): ChatItem.RevisionPrompt? {
        val slot = proposal.sourceSlotId?.let(slotById::get) ?: return null
        val source = proposal.source ?: return null
        return ChatItem.RevisionPrompt(m.id, RevisionConfirm(slot, source.kcal, m.estimateKcal ?: return null))
    }

    private fun receipt(m: ChatMessageEntity, slotTimes: Map<Long, String>, actions: List<ReceiptAction>, moveConfirm: ReplaceConfirm?): ChatItem.Receipt {
        val undo = UndoData.decode(m.undoData)
        return ChatItem.Receipt(
            id = m.id,
            kind = when (m.role) {
                ReceiptRules.REPLACED -> ReceiptKind.REPLACED
                ReceiptRules.SKIPPED -> ReceiptKind.SKIPPED
                ReceiptRules.MOVED -> ReceiptKind.MOVED
                ReceiptRules.RESTORED -> ReceiptKind.RESTORED
                ReceiptRules.WORKOUT -> if (m.text == ChatRecorder.WORKOUT_ADD) ReceiptKind.WORKOUT_ADDED else ReceiptKind.WORKOUT
                else -> ReceiptKind.LOGGED
            },
            // A65: a workout receipt keeps its mode in the text, not a meal name.
            slotName = if (m.role == ReceiptRules.WORKOUT) "" else m.text,
            slotTime = m.estimateSlotId?.let { slotTimes[it] },
            kcal = m.estimateKcal,
            fromKcal = undo?.takeIf { m.role == ReceiptRules.REPLACED }?.recordSlot?.before?.kcal,
            memoryUpdated = m.memoryUpdated,
            mark = when (m.receiptState) {
                ChatRecorder.UNDONE -> ReceiptMark.UNDONE
                ChatRecorder.DELETED -> ReceiptMark.DELETED
                ChatRecorder.MOVED -> ReceiptMark.MOVED
                ChatRecorder.EDITED -> ReceiptMark.EDITED
                else -> null
            },
            actions = actions,
            moveConfirm = moveConfirm?.takeIf { ReceiptAction.MOVE in actions },
            planLine = undo?.recordSlot?.takeIf { m.role in setOf(ReceiptRules.LOGGED, ReceiptRules.REPLACED) }?.let { change ->
                change.before.planned?.let { plan ->
                    val diff = change.after.kcal - plan.kcal
                    val signed = if (diff < 0) "\u2212${-diff}" else "+$diff"
                    "Plano: ${plan.kcal} · Registrado: ${change.after.kcal} ($signed kcal)"
                }
            },
        )
    }

    /** The plan's own receipt: recorded (A34), or the first receipt after it, before any other estimate, is a record. */
    private fun recordedPlan(plan: ChatMessageEntity, sorted: List<ChatMessageEntity>): Boolean =
        plan.recordState == ChatRecorder.RECORDED || sorted
            .dropWhile { it.id != plan.id }
            .drop(1)
            .firstOrNull { it.role in CLOSERS || it.role == "assistant" && it.isRecordable }
            ?.role.let { it == ReceiptRules.LOGGED || it == ReceiptRules.REPLACED }

    private fun Macros.minus(o: Macros) =
        Macros((kcal - o.kcal).coerceAtLeast(0), (p - o.p).coerceAtLeast(0), (c - o.c).coerceAtLeast(0), (g - o.g).coerceAtLeast(0))

    /**
     * chatS (ADR-023 decision 8): the slot of the hour has no record and no skip today, and a strong
     * routine of that slot exists ([MemoryRules.isStrongRoutine]). Several: the one seen on more days.
     */
    private fun routineFor(current: SlotRef?, d: DaySnapshot, facts: List<Fact>): RoutineSuggestion? {
        current ?: return null
        if (d.logs.any { it.slotId == current.id } || current.id in d.skippedSlotIds) return null
        val fact = facts
            .filter { MemoryRules.isStrongRoutine(it) && it.slot == current.id.toString() }
            .maxWithOrNull(compareBy<Fact>({ it.days.size }, { it.permanent })) ?: return null
        return RoutineSuggestion(
            factId = fact.id,
            slot = current,
            text = fact.text,
            kcal = fact.kcal ?: 0,
            p = fact.p ?: 0,
            c = fact.c ?: 0,
            g = fact.g ?: 0,
            permanent = fact.permanent,
        )
    }

    /** A64: what an estimate waiting for Registrar adds to the day: the added food of an addition, else the whole meal. */
    private fun projected(m: ChatMessageEntity): Macros? {
        val proposal = MealProposal.decode(m.mealChange)
        if (proposal?.actionable == false || proposal?.isRevision == true) return null
        proposal?.addition?.takeIf { proposal.isAddition }?.let { return Macros(it.kcal, it.p, it.c, it.g) }
        val kcal = m.estimateKcal ?: return null
        return Macros(kcal, m.estimateP ?: 0, m.estimateC ?: 0, m.estimateG ?: 0)
    }

    /** [offer]: Registrar is open for this answer: the slot question stays in the bubble (chatE). */
    private fun assistant(
        m: ChatMessageEntity,
        time: String,
        slotById: Map<Long, SlotRef>,
        plan: ProjectedDay?,
        offer: Boolean,
        notRecorded: Boolean,
        budget: BudgetNote? = null,
        reservedFor: String? = null,
        projection: String? = null,
    ): ChatItem.Assistant {
        val slotQuestion = m.estimateSlotId?.takeIf { offer }?.let { id -> slotById[id] }?.let { s -> "Deseja registrar essa refeição no ${s.name}?" }
        // A47: an addition shows only the added food (+ numbers), a revision its NOVO TOTAL; a rejected proposal no card.
        val proposal = MealProposal.decode(m.mealChange)
        val addition = proposal?.addition?.takeIf { proposal.isAddition }
        val estimate = when {
            proposal?.actionable == false -> null
            addition != null -> EstimateView(addition.kcal, addition.p, addition.c, addition.g, slotQuestion, null, EstimateKind.ADDITION, addition.mealText)
            // A plan keeps its estimate in Room (A29) but shows only the bubble: no card, no actions.
            else -> m.estimateKcal?.takeIf { m.isLogEstimate }?.let {
                EstimateView(
                    kcal = it,
                    p = m.estimateP ?: 0,
                    c = m.estimateC ?: 0,
                    g = m.estimateG ?: 0,
                    slotQuestion = slotQuestion,
                    question = m.estimateQuestion,
                    kind = if (proposal?.isRevision == true) EstimateKind.REVISION else EstimateKind.MEAL,
                    label = m.estimateMealText.takeIf { proposal?.isRevision == true },
                )
            }
        }
        return ChatItem.Assistant(
            id = m.id,
            text = m.text,
            time = time,
            highlights = m.itemNames,
            estimate = estimate,
            plan = plan,
            memory = memoryOf(m),
            notRecorded = notRecorded,
            prose = estimate == null || estimate.kind == EstimateKind.MEAL,
            budget = budget,
            reservedFor = reservedFor,
            // A60 part C: a reply with the subset's markers is drawn as blocks (ADR-045); plain prose stays as it was.
            blocks = m.text.takeIf { ReplyMarkup.formatted(it) }?.let(ReplyMarkup::parse),
            projection = projection,
            noted = m.noted?.lines()?.filter { it.isNotBlank() }.orEmpty(),
        )
    }

    private fun memoryOf(m: ChatMessageEntity) = m.memoryUsedKinds.orEmpty().split(',').let { kinds ->
        MemoryNotice(updated = m.memoryUpdated, permanent = MemoryRules.PERMANENT in kinds, dynamic = MemoryRules.DYNAMIC in kinds)
    }

    /** [day]: the estimate's whole day, oldest first ([DayRepository.messagesOf]). */
    private fun userBefore(estimate: ChatMessageEntity, day: List<ChatMessageEntity>): ChatMessageEntity? = day
        .filter { it.role == "user" && it.createdAtEpochMs <= estimate.createdAtEpochMs && it.id < estimate.id }
        .maxByOrNull { it.id }

    /**
     * Timeline text (spec rule 12, A27): the server meal_text; else the first user message of the
     * estimate's chain, never an answer to a question (a photo gives the AI text); else the item names.
     */
    private fun descriptionOf(estimate: ChatMessageEntity, day: List<ChatMessageEntity>): String {
        estimate.estimateMealText?.takeIf { it.isNotBlank() }?.let { return it }
        val user = chainStart(estimate, day)
        if (user != null) {
            // A60 part C: a record never keeps the markers of the reply (ADR-045).
            if (user.photoPath != null) return ReplyMarkup.plain(estimate.text)
            if (user.text.isNotBlank()) return user.text
        }
        return estimate.itemNames.joinToString(", ").ifBlank { ReplyMarkup.plain(estimate.text) }
    }

    /**
     * Walks back from the user message right before [estimate]: while it answers a question (the
     * assistant message right before it had estimateQuestion), steps to the user message before that one.
     * A receipt or a wipe in between ends the chain.
     */
    private fun chainStart(estimate: ChatMessageEntity, day: List<ChatMessageEntity>): ChatMessageEntity? {
        val thread = day.filter { it.date == estimate.date }
        var i = thread.indexOfFirst { it.id == estimate.id } - 1
        if (thread.getOrNull(i)?.role != "user") return userBefore(estimate, day)
        while (thread.getOrNull(i - 1).asked() && thread.getOrNull(i - 2)?.role == "user") i -= 2
        return thread[i]
    }

    private fun ChatMessageEntity?.asked() = this?.role == "assistant" && !estimateQuestion.isNullOrBlank()

    private fun timeOf(epochMs: Long) = Instant.ofEpochMilli(epochMs).atZone(SaoPaulo.zone).format(TIME)

    companion object {
        /** server/shaping.py CHAT_FALLBACK_REPLY: bad JSON, empty reply or model error on the server. */
        const val SERVER_FALLBACK_REPLY = "nao deu pra estimar"

        /** User message of Forçar estimativa (A30). */
        const val FORCE_TEXT = "Pode estimar assim."

        /** Forçar estimativa shows from this question round on (owner decision, ADR-026). */
        const val FORCE_FROM_ROUND = 2

        /** A64 (ADR-054 § 3): a send without an answer this long shows `Tali está pensando…`. */
        const val WAITING_MS = 4_000L

        /** Rows per page of the thread (A32). */
        const val PAGE_SIZE = 20

        /** A64: the receipts that leave a record in a slot; the newest active one of today carries the day balance. */
        private val BALANCE_RECEIPTS = setOf(ReceiptRules.LOGGED, ReceiptRules.REPLACED, ReceiptRules.MOVED, ReceiptRules.RESTORED, ReceiptRules.WORKOUT)

        /** A65: server bounds of `workout.kcal` and its modes (S35). */
        private const val WORKOUT_MAX = 5000
        private val WORKOUT_MODES = setOf(ChatRecorder.WORKOUT_REPLACE, ChatRecorder.WORKOUT_ADD)

        /** A65: a number no day holds, so a workout receipt of another day never matches. */
        private const val WORKOUT_UNKNOWN = Int.MIN_VALUE

        /** Receipt of Substituir (ADR-017). UI only, like "logged". */
        const val ROLE_REPLACED = ReceiptRules.REPLACED

        /** Rows that closed an old plan row (before A34): its Registrar assim goes away. A wipe closes it too. */
        private val CLOSERS = ReceiptRules.ROLES - ReceiptRules.WORKOUT + DayRepository.ROLE_WIPED

        /** server `record` (S14, A34). */
        const val RECORD_AUTO = "auto"
        const val RECORD_ASK = "ask"
        const val RECORD_NONE = "none"
        private val RECORD_MODES = setOf(RECORD_AUTO, RECORD_ASK, RECORD_NONE)

        /** Receipt source of a plan record (Registrar assim). */
        const val SOURCE_PLAN = "plan"

        /** A47: an addition whose complete description would pass the contract bound is never cut. */
        const val OVERFLOW_NOTICE = "Não registrado: a descrição ficaria longa demais."

        /** A54 `record_guard` reasons of an `auto` answer left without a record; with [MealProposal.MALFORMED], [MealProposal.CONTRADICTS] and [MealProposal.OVERFLOW]. */
        private const val GUARD_NO_TARGET = "no_target"
        private const val GUARD_STALE = "stale"
        private const val GUARD_OVERFLOW = MealProposal.OVERFLOW
        private const val GUARD_WRITE_FAILED = "write_failed"

        /** A59: a listed skip whose id is not a slot of today. */
        private const val GUARD_SLOT_NOT_TODAY = "slot_not_today"

        /** A59: reads of a slot that moves under its skip before the skip gives up (expired). */
        private const val SKIP_ATTEMPTS = 3

        /** recordState of an answer that can still be recorded: nothing decided, or a pending confirmation. */
        private val OPEN_STATES = setOf(null, ChatRecorder.PENDING_REPLACE, ChatRecorder.PENDING_ADD, ChatRecorder.PENDING_REVISE)

        private val memoryJson = Json { ignoreUnknownKeys = true }
        private val UPDATES = ListSerializer(ChatMemoryUpdate.serializer())

        /** Routine add/reinforce: applied only when the estimate is recorded (ADR-023). */
        private val ChatMemoryUpdate.waitsForRecord: Boolean
            get() = category == MemoryRules.ROUTINE && (op == MemoryRules.ADD || op == MemoryRules.REINFORCE)

        private fun ChatMemoryUpdate.toDomain() = MemoryUpdate(op, id, kind, category, key, text, slot)

        /** server/shaping.py intents (S11, S14). */
        private val INTENTS = setOf("log", "plan", "question", "skip")

        /** An estimate the user can record: intent "log", or null (old row or server before S11). */
        private val ChatMessageEntity.isLogEstimate: Boolean
            get() = estimateKcal != null && (intent == null || intent == "log")

        /** A plan with its estimate (chatR): recorded by Registrar assim (A29). */
        private val ChatMessageEntity.isPlanEstimate: Boolean
            get() = estimateKcal != null && intent == "plan"

        private val ChatMessageEntity.isRecordable: Boolean
            get() = isLogEstimate || isPlanEstimate

        /** meal_log.source of a Registrar on the routine card. */
        const val SOURCE_ROUTINE = "routine"

        private const val MINUTE_MS = 60_000L
        private val TIME = DateTimeFormatter.ofPattern("HH:mm")
        private val DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

        fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
            today -> "Hoje, ${date.format(DAY)}"
            today.minusDays(1) -> "Ontem, ${date.format(DAY)}"
            else -> date.format(DAY)
        }
    }
}
