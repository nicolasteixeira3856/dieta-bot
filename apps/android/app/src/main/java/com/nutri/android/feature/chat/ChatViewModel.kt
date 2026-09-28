package com.nutri.android.feature.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.ChatMessageEntity
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.metaOn
import com.nutri.android.core.memory.MemoryStore
import com.nutri.android.core.photo.PhotoFiles
import com.nutri.android.core.photo.PhotoResult
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatOut
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.SlotClock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The only network call of the Chat (spec rule 13: no /v1/estimate, no /v1/fit). */
fun interface ChatService {
    suspend fun chat(body: ChatIn): ChatOut
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: DayRepository,
    private val service: ChatService,
    private val clock: InstantClock,
    private val memory: MemoryStore,
    private val photos: PhotoFiles,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var day: DaySnapshot = DaySnapshot()
    private var messages: List<ChatMessageEntity> = emptyList()

    /** VM-only state: never in Room until the server answers (spec: failure leaves Room untouched). */
    private data class Local(
        val composer: String = "",
        val pending: String? = null,
        val failed: Boolean = false,
        val sheetFor: Long? = null,
        val sheetSelection: Long? = null,
        val skipConfirm: SlotRef? = null,
        val openedAt: Instant? = null,
        /** JPEG of the pending send (A6). Stored in chat_message only after the server answers. */
        val pendingPhoto: String? = null,
        val photoSheet: Boolean = false,
        /** One-shot snackbar ("Foto grande demais."). */
        val notice: String? = null,
    )

    /** TakePicture target while the camera is open. */
    private var captureFile: File? = null

    init {
        local.update { it.copy(openedAt = clock.now()) }
        viewModelScope.launch {
            combine(repository.observeToday(), repository.observeMessages(), local) { d, m, l -> Triple(d, m, l) }
                .collect { (d, m, l) ->
                    day = d
                    messages = m
                    _uiState.value = render(d, m, l)
                }
        }
    }

    fun setComposer(text: String) = local.update { it.copy(composer = text.take(1000)) }

    fun useSuggestion(text: String) = setComposer(text)

    fun send() {
        val text = local.value.composer.trim()
        if (text.isEmpty() || local.value.pending != null) return
        local.update { it.copy(composer = "", pending = text, failed = false) }
        viewModelScope.launch { post(text) }
    }

    fun retry() {
        val text = local.value.pending ?: return
        if (!local.value.failed) return
        local.update { it.copy(failed = false) }
        viewModelScope.launch { post(text, local.value.pendingPhoto) }
    }

    // ------------------------------------------------------------------ photo (A6)

    fun openPhotoSheet() = local.update { it.copy(photoSheet = true) }

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

    /** The photo goes at once, captioned by what is typed (may be empty). Over 16 MB: no POST. */
    private fun onPhoto(result: PhotoResult) {
        when (result) {
            is PhotoResult.Ready -> {
                if (local.value.pending != null) {
                    photos.delete(result.path)
                    return
                }
                val text = local.value.composer.trim()
                local.update { it.copy(composer = "", pending = text, pendingPhoto = result.path, failed = false) }
                viewModelScope.launch { post(text, result.path) }
            }
            PhotoResult.TooLarge -> local.update { it.copy(notice = "Foto grande demais.") }
            PhotoResult.Failed -> local.update { it.copy(notice = "Não deu para abrir a foto.") }
        }
    }

    override fun onCleared() {
        // A photo that never got an answer is not in chat_message: nothing points to it.
        local.value.pendingPhoto?.let { photos.delete(it) }
        captureFile?.let { photos.delete(it.path) }
    }

    private suspend fun post(text: String, photo: String? = null) {
        val sentAt = clock.now()
        val today = messages.filter { it.date == SaoPaulo.date(sentAt).toString() }
        val snapshot = repository.observeToday().first()
        var digests = repository.digestsToday()
        val memoryText = memory.read()
        // Open clarifying question of the last estimate: this send is the user's answer (A8).
        val asked = openQuestion(SaoPaulo.date(sentAt).toString())
        val image = photo?.let { photos.base64(it) }
        if (photo != null && image == null) {
            local.update { it.copy(failed = true) }
            return
        }
        var turn = PromptBuilder.build(snapshot, today, digests, text, sentAt, memory = memoryText)
        if (turn.needsCompact) {
            // A failed compact never fails the turn: nothing stored, the newest 12 raw go as they are
            // and the next send tries again.
            val digest = runCatching { service.chat(PromptBuilder.compact(turn)).digest }.getOrNull()
            if (!digest.isNullOrBlank()) {
                repository.upsertDigest(digest)
                digests = repository.digestsToday()
                turn = PromptBuilder.build(snapshot, today, digests, text, sentAt, memory = memoryText)
            }
        }
        // The photo rides only on the turn, never on the compact request.
        val out = runCatching { service.chat(turn.body.copy(imageB64 = image)) }.getOrNull()
        if (out == null || out.reply.isBlank() && out.estimate == null) {
            local.update { it.copy(failed = true) }
            return
        }
        val slots = snapshot.slots.map { it.id }.toSet()
        val suggested = out.estimate?.suggestedSlot?.toLongOrNull()?.takeIf { it in slots }
        repository.insertMessage(role = "user", text = text, photoPath = photo)
        repository.insertMessage(
            role = "assistant",
            text = out.reply,
            estimateKcal = out.estimate?.kcal?.roundToInt(),
            estimateP = out.estimate?.p?.roundToInt(),
            estimateC = out.estimate?.c?.roundToInt(),
            estimateG = out.estimate?.g?.roundToInt(),
            estimateConfidence = out.estimate?.confidence,
            estimateSlotId = suggested,
            estimateQuestion = out.estimate?.question,
            estimateItems = out.estimate?.items?.map { it.name }.orEmpty(),
        )
        local.update { it.copy(pending = null, pendingPhoto = null, failed = false) }
        asked?.let { memory.append("Respondeu \"$it\": $text") }
    }

    /** Question of the last estimate of today when nothing closed it yet (no receipt, no wipe). */
    private fun openQuestion(todayIso: String): String? {
        val sorted = messages.sortedWith(compareBy({ it.createdAtEpochMs }, { it.id }))
        val last = sorted.lastOrNull { it.role == "assistant" && it.date == todayIso } ?: return null
        val question = last.estimateQuestion?.takeIf { last.estimateKcal != null && it.isNotBlank() } ?: return null
        val closed = sorted.any { it.id != last.id && it.createdAtEpochMs >= last.createdAtEpochMs && it.role in RECEIPTS }
        return if (closed) null else question
    }

    /** Tap Gravar: local addLog with the last estimate. No second POST (spec rule 5). */
    fun record(estimateId: Long, slotId: Long) {
        val estimate = messages.firstOrNull { it.id == estimateId && it.estimateKcal != null } ?: return
        val slot = day.slots.firstOrNull { it.id == slotId } ?: return
        local.update { it.copy(sheetFor = null, sheetSelection = null) }
        viewModelScope.launch {
            repository.addLog(
                window = "",
                text = descriptionOf(estimate),
                kcal = estimate.estimateKcal ?: 0,
                p = estimate.estimateP ?: 0,
                stable = true,
                slotId = slot.id,
                carbs = estimate.estimateC ?: 0,
                fat = estimate.estimateG ?: 0,
                source = if (userBefore(estimate)?.photoPath != null) "photo" else "user",
            )
            repository.insertMessage(role = "logged", text = slot.name, estimateKcal = estimate.estimateKcal, estimateSlotId = slot.id)
            memory.append("${slot.name}: ${descriptionOf(estimate)} (${estimate.estimateKcal ?: 0} kcal)")
        }
    }

    fun openSheet(estimateId: Long) = local.update {
        val preset = _uiState.value.actions?.record?.id ?: _uiState.value.currentSlotId
        it.copy(sheetFor = estimateId, sheetSelection = preset)
    }

    fun selectInSheet(slotId: Long) = local.update { it.copy(sheetSelection = slotId) }

    fun closeSheet() = local.update { it.copy(sheetFor = null, sheetSelection = null) }

    /** Trocar → Confirmar: records into the chosen slot. */
    fun confirmSheet() {
        val l = local.value
        val estimateId = l.sheetFor ?: return
        val slotId = l.sheetSelection ?: return
        record(estimateId, slotId)
    }

    fun askSkip(slot: SlotRef) = local.update { it.copy(skipConfirm = slot) }

    fun cancelSkip() = local.update { it.copy(skipConfirm = null) }

    /** Pular: skip status on the slot, no kcal (spec rule 7). Never automatic. */
    fun confirmSkip() {
        val slot = local.value.skipConfirm ?: return
        local.update { it.copy(skipConfirm = null) }
        viewModelScope.launch {
            repository.addSkip(slot.id)
            repository.insertMessage(role = "skipped", text = slot.name, estimateSlotId = slot.id)
        }
    }

    // ------------------------------------------------------------------ render

    private fun render(d: DaySnapshot, all: List<ChatMessageEntity>, l: Local): ChatUiState {
        val now = clock.now()
        val today = SaoPaulo.date(now)
        val slots = d.slots.refs()
        val slotById = slots.associateBy { it.id }
        val sorted = all.sortedWith(compareBy({ it.createdAtEpochMs }, { it.id }))
        val items = mutableListOf<ChatItem>()
        var lastDate: String? = null
        // The wipe marker only cuts the prompt; it is never drawn.
        val visible = sorted.filter { it.role != DayRepository.ROLE_WIPED }
        for (m in visible) {
            if (m.date != lastDate) {
                items += ChatItem.DateSeparator(dateLabel(LocalDate.parse(m.date), today))
                lastDate = m.date
            }
            val time = timeOf(m.createdAtEpochMs)
            items += when (m.role) {
                "user" -> ChatItem.User(m.id, m.text, time, photoPath = m.photoPath)
                "logged", "skipped" -> ChatItem.Receipt(
                    id = m.id,
                    skipped = m.role == "skipped",
                    slotName = m.text,
                    slotTime = m.estimateSlotId?.let { slotById[it]?.time },
                    kcal = m.estimateKcal,
                )
                else -> ChatItem.Assistant(
                    id = m.id,
                    text = m.text,
                    time = time,
                    highlights = m.itemNames,
                    estimate = m.estimateKcal?.let {
                        EstimateView(
                            kcal = it,
                            p = m.estimateP ?: 0,
                            c = m.estimateC ?: 0,
                            g = m.estimateG ?: 0,
                            slotQuestion = m.estimateSlotId?.let { id -> slotById[id] }?.let { s -> "Deseja registrar essa refeição no ${s.name}?" },
                            question = m.estimateQuestion,
                        )
                    },
                )
            }
        }
        val todayIso = today.toString()
        val emptyDay = visible.none { it.date == todayIso } && l.pending == null
        if (emptyDay) {
            if (lastDate != todayIso) items += ChatItem.DateSeparator(dateLabel(today, today))
            items += ChatItem.Greeting(timeOf((l.openedAt ?: now).toEpochMilli()))
        }
        l.pending?.let { text ->
            if (lastDate != todayIso && !emptyDay) items += ChatItem.DateSeparator(dateLabel(today, today))
            items += ChatItem.User(-1, text, timeOf(now.toEpochMilli()), pending = true, photoPath = l.pendingPhoto)
            items += if (l.failed) ChatItem.Failed else ChatItem.Loading
        }

        val nowMinutes = SlotClock.minutesFromMidnight(now)
        val current = SlotClock.current(slots, nowMinutes) { it.minutes }
        val lastEstimate = sorted.lastOrNull { it.role == "assistant" && it.estimateKcal != null && it.date == todayIso }
        val closed = lastEstimate == null || sorted.any { it.createdAtEpochMs >= lastEstimate.createdAtEpochMs && it.id != lastEstimate.id && it.role in RECEIPTS }
        val actions = if (closed || l.pending != null) null else {
            val record = lastEstimate!!.estimateSlotId?.let { slotById[it] }
            EstimateActions(lastEstimate.id, record = record, skip = record ?: current)
        }
        val meta = d.metaOn(today)
        val eaten = d.logs.sumOf { it.kcal }
        return ChatUiState(
            items = items,
            composer = l.composer,
            sending = l.pending != null && !l.failed,
            emptyDay = emptyDay,
            metaRemaining = (meta - eaten).coerceAtLeast(0),
            metaTotal = meta,
            actions = actions,
            slots = slots,
            currentSlotId = current?.id,
            sheetFor = l.sheetFor,
            photoSheet = l.photoSheet,
            notice = l.notice,
            sheetSelection = l.sheetSelection,
            skipConfirm = l.skipConfirm,
        )
    }

    private fun userBefore(estimate: ChatMessageEntity): ChatMessageEntity? = messages
        .filter { it.role == "user" && it.createdAtEpochMs <= estimate.createdAtEpochMs && it.id < estimate.id }
        .maxByOrNull { it.id }

    /** Timeline text: what the user wrote; with a photo, the AI description (spec rule 12). */
    private fun descriptionOf(estimate: ChatMessageEntity): String {
        val user = userBefore(estimate)
        return if (user != null && user.photoPath == null && user.text.isNotBlank()) user.text else estimate.text
    }

    private fun timeOf(epochMs: Long) = Instant.ofEpochMilli(epochMs).atZone(SaoPaulo.zone).format(TIME)

    companion object {
        /** Rows that close the last estimate: its actions go away. A wipe closes it too. */
        private val RECEIPTS = setOf("logged", "skipped", DayRepository.ROLE_WIPED)
        private val TIME = DateTimeFormatter.ofPattern("HH:mm")
        private val DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

        fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
            today -> "Hoje, ${date.format(DAY)}"
            today.minusDays(1) -> "Ontem, ${date.format(DAY)}"
            else -> date.format(DAY)
        }
    }
}
