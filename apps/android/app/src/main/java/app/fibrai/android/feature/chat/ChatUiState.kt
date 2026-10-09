package app.fibrai.android.feature.chat

import androidx.compose.runtime.Immutable
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.domain.ProjectedDay
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.ReplyMarkup
import app.fibrai.android.domain.SlotSuggestions

@Immutable
data class EstimateView(
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    /** "Deseja registrar essa refeição no {slot}?" when a slot is known. */
    val slotQuestion: String?,
    /** Server follow-up when confidence is not high. */
    val question: String?,
    /** A47 (chatI, chatIC): what the numbers are, from the structured proposal. */
    val kind: EstimateKind = EstimateKind.MEAL,
    /** ADDITION: the added food and its quantity; REVISION: the revised meal. */
    val label: String? = null,
)

/**
 * A47: MEAL = the whole meal (ENERGIA TOTAL ~kcal). ADDITION = only the added food, every number with a +.
 * REVISION = the revised meal's NOVO TOTAL.
 */
enum class EstimateKind { MEAL, ADDITION, REVISION }

/**
 * Memory notices under a bubble (A29, chatM): `Memória atualizada` when this turn changed the memory,
 * then the kinds of the facts the answer used. Informative only.
 */
@Immutable
data class MemoryNotice(val updated: Boolean = false, val permanent: Boolean = false, val dynamic: Boolean = false) {
    val any: Boolean get() = updated || permanent || dynamic
}

/** A strong routine for the empty slot of the hour (A29, chatS). Never stored, never sent. */
@Immutable
data class RoutineSuggestion(
    val factId: String,
    val slot: SlotRef,
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val permanent: Boolean,
)

@Immutable
sealed interface ChatItem {
    val key: String

    data class DateSeparator(val label: String) : ChatItem {
        override val key = "date-$label"
    }

    data class User(
        val id: Long,
        val text: String,
        val time: String,
        val pending: Boolean = false,
        /** JPEG in filesDir/photos (A6); the bubble shows a ≤720 px preview. */
        val photoPath: String? = null,
    ) : ChatItem {
        override val key = "u-$id"
    }

    data class Assistant(
        val id: Long,
        val text: String,
        val time: String,
        /** Item names to highlight in gold inside [text]. */
        val highlights: List<String> = emptyList(),
        val estimate: EstimateView? = null,
        /** A plan of today (A29, chatR): the day projected with it, recomputed on every render. */
        val plan: ProjectedDay? = null,
        val memory: MemoryNotice = MemoryNotice(),
        /** A34: an `ask` or a pending replace that expired: `Não registrado` below the bubble. */
        val notRecorded: Boolean = false,
        /**
         * A47: false for an addition or revision, whose answer is the server's numeric template: the cards show those
         * numbers from the structured fields, so the prose is not repeated (chatI, chatIC).
         */
        val prose: Boolean = true,
        /** A60 part A (chatRB): the lines below an over-budget plan while its choice is open. */
        val budget: BudgetNote? = null,
        /** A60 part D (chatRL): `Reservado para o {slot}` below the plan whose reservation it holds. */
        val reservedFor: String? = null,
        /** A60 part C (chatR, chatE, chatRK): the reply's subset blocks; null = plain prose. */
        val blocks: List<app.fibrai.android.domain.ReplyBlock>? = null,
        /** A64: `Projeção: …` below an estimate still waiting for Registrar; the day with it, computed by the app. */
        val projection: String? = null,
        /** A64 (ADR-053 § 2): one `Anotado: {text}` line per permanent fact this answer saved, as stored. */
        val noted: List<String> = emptyList(),
    ) : ChatItem {
        override val key = "a-$id"
    }

    /** chatI (A47): Adicionar ao {slot}? right below the addition, for its current destination. */
    data class AdditionPrompt(val estimateId: Long, val confirm: AdditionConfirm) : ChatItem {
        override val key = "ap-$estimateId"
    }

    /** chatIC (A47): Atualizar {slot}? right below the revision. */
    data class RevisionPrompt(val estimateId: Long, val confirm: RevisionConfirm) : ChatItem {
        override val key = "rv-$estimateId"
    }

    /** chatSD (A59): `Pular {slot}?` below an answer that skipped a meal with a record, after its receipts. */
    data class SkipDeletePrompt(val answerId: Long, val confirm: SkipDeleteConfirm) : ChatItem {
        override val key = "sd-$answerId-${confirm.slot.id}"
    }

    /** A59: a listed skip that ended without a skip: [kept] = `Registro mantido`, else `Não registrado`. */
    data class SkipMark(val answerId: Long, val slotId: Long, val kept: Boolean) : ChatItem {
        override val key = "sm-$answerId-$slotId"
    }

    /** chatU (A34): `Substituir {slot}?` right below the answer whose slot already has a record. */
    data class ReplacePrompt(val estimateId: Long, val confirm: ReplaceConfirm) : ChatItem {
        override val key = "rp-$estimateId"
    }

    /**
     * A question in its own bubble. [standalone] = a question before the estimate (A30, chatQ): the
     * "Tali" label above, no reply bubble. Else the follow-up right below an old estimate row.
     */
    data class Question(
        val messageId: Long,
        val text: String,
        val time: String,
        val standalone: Boolean = false,
        val memory: MemoryNotice = MemoryNotice(),
    ) : ChatItem {
        override val key = "q-$messageId"
    }

    data class Receipt(
        val id: Long,
        val kind: ReceiptKind,
        val slotName: String,
        val slotTime: String?,
        val kcal: Int?,
        /** Replacement (A34): the slot's kcal before it, chip `{fromKcal} → {kcal} kcal`. Null on old rows. */
        val fromKcal: Int? = null,
        /** The record applied a routine to the memory (A28): `Memória atualizada` below it. */
        val memoryUpdated: Boolean = false,
        /** A34: what was done to this receipt; dims it (chatD). */
        val mark: ReceiptMark? = null,
        /** A34: the stacked buttons of the latest receipt of its slot (chatG, chatF, chatD). */
        val actions: List<ReceiptAction> = emptyList(),
        /** A34: Trocar refeição into a slot with a record asks right below the receipt (chatU). */
        val moveConfirm: ReplaceConfirm? = null,
        /** A60 part D: a record into a reserved meal, `Plano: {kcal} · Registrado: {kcal} ({+n} kcal)`. */
        val planLine: String? = null,
        /** A64: the day after the record, on today's newest active record receipt only (`{eaten} de {ceiling} kcal · …`). */
        val balance: String? = null,
        /** A66: the typed-actions answer that wrote this receipt by itself; its Desfazer reverts the batch. */
        val batch: Long? = null,
    ) : ChatItem {
        override val key = "r-$id"
        val skipped: Boolean get() = kind == ReceiptKind.SKIPPED
    }

    /** `O de sempre no {slot}?` card at the end of the thread (chatS). UI only. */
    data class Routine(val suggestion: RoutineSuggestion) : ChatItem {
        override val key = "routine"
    }

    /** Static greeting of an empty day; never stored nor sent. */
    data class Greeting(val time: String) : ChatItem {
        override val key = "greeting"
    }

    data object Loading : ChatItem {
        override val key = "loading"
    }

    /** A64 (ADR-054 § 3): the same bubble after [ChatViewModel.WAITING_MS] without an answer, `Tali está pensando…`. */
    data object Thinking : ChatItem {
        override val key = "loading"
    }

    data object Failed : ChatItem {
        override val key = "failed"
    }
}

/** Receipt roles (A34): title and icon of the card. */
enum class ReceiptKind { LOGGED, REPLACED, SKIPPED, MOVED, RESTORED, WORKOUT, WORKOUT_ADDED }

/** The mark of a receipt that lost its actions by a tap on it (A34). */
enum class ReceiptMark(val label: String) {
    UNDONE("Desfeito"),
    DELETED("Excluído"),
    MOVED("Movido"),
    EDITED("Removido para editar"),
}

@Immutable
data class SlotRef(val id: Long, val name: String, val time: String, val minutes: Int)

@Immutable
data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val composer: String = "",
    /** Composer over [ChatText.MAX_CHARS][app.fibrai.android.domain.ChatText.MAX_CHARS] (chatX): nothing leaves. */
    val composerTooLong: Boolean = false,
    val sending: Boolean = false,
    /** Empty day: meta card + suggestion chips (chat0). */
    val emptyDay: Boolean = true,
    val metaRemaining: Int = 0,
    val metaTotal: Int = 0,
    /** Registrar (an `ask` estimate, chatE) or Registrar assim (a plan, chatR) above the composer. */
    val actions: EstimateActions? = null,
    /** Forçar estimativa in the actions slot (A30, chatQ): the last message is a question of round 2 or more. */
    val forceEstimate: Boolean = false,
    val slots: List<SlotRef> = emptyList(),
    val currentSlotId: Long? = null,
    /** Estimate or receipt id whose Trocar sheet (chatT) is open. */
    val sheetFor: Long? = null,
    val sheetSelection: Long? = null,
    /** Slot marked "(atual)" in the sheet: the record's slot for Trocar refeição, else the slot of the hour. */
    val sheetCurrent: Long? = null,
    /** A47 (chatTI): the sheet picks the destination of an addition only; no "(atual)", nothing picked. */
    val sheetAddition: SheetAddition? = null,
    /** Camera / gallery chooser (A6). */
    val photoSheet: Boolean = false,
    /** JPEG attached in the composer, not sent yet (chatA). */
    val attachment: String? = null,
    /** Snackbar text, shown once. */
    val notice: String? = null,
    /** `O de sempre no {slot}?` (chatS), also drawn as the last thread item. */
    val routine: RoutineSuggestion? = null,
    /** Bumped by Quase igual: the composer takes focus, cursor at the end, keyboard open. */
    val focusComposer: Int = 0,
    /** The first page arrived: until then the thread is not composed (A32). */
    val loaded: Boolean = true,
    /** A61 part B (chatCP): keys of the selected text bubbles; not empty = the selection bar replaces the header. */
    val selected: Set<String> = emptySet(),
    /** A61 part B (chatCC): messages just copied, shown by the app on Android 12 and earlier. */
    val copied: Int? = null,
    /** The last page came back full: older rows may exist within the 60 days. */
    val hasOlder: Boolean = false,
    /** A page of older rows is on its way: indicator at the visual top. */
    val loadingOlder: Boolean = false,
) {
    /** [items] newest first for the reversed thread: a view, built once per state, not per frame. */
    val newestFirst: List<ChatItem> = items.asReversed()

    val canSend: Boolean get() = (composer.isNotBlank() || attachment != null) && !sending && !composerTooLong

    /** Camera / gallery: off while sending and in the chatX state. */
    val canAttach: Boolean get() = !sending && !composerTooLong
}

/**
 * chatI (A47): the destination's recorded amount and the meal after the addition, computed by the app with the same
 * function the record uses. [total] is kcal/P/C/G of the whole meal.
 */
@Immutable
data class AdditionConfirm(val slot: SlotRef, val previousKcal: Int, val total: app.fibrai.android.domain.Macros)

/** chatIC (A47): Antes {beforeKcal} kcal, Novo total {newKcal} kcal. */
@Immutable
data class RevisionConfirm(val slot: SlotRef, val beforeKcal: Int, val newKcal: Int)

/** chatTI (A47): the meal picker directs only the added food; the source meal stays as it is. */
@Immutable
data class SheetAddition(val food: String, val kcal: Int, val sourceName: String)

/** A skip of a meal with a record (chatSD): "{slot} tem {kcal} kcal registrados." */
@Immutable
data class SkipDeleteConfirm(val slot: SlotRef, val kcal: Int)

/** A record into a taken slot (chatU): "{slot} tem {oldKcal} kcal. Fica com {newKcal} kcal." */
@Immutable
data class ReplaceConfirm(val slot: SlotRef, val oldKcal: Int, val newKcal: Int)

@Immutable
data class EstimateActions(
    val estimateId: Long,
    /** Suggested slot of today; null = the tap opens Trocar with nothing picked. */
    val record: SlotRef?,
    /** A plan (chatR): Registrar assim. Else Registrar (chatE). */
    val plan: Boolean = false,
    /** A60 part A (chatRB): Pode passar · Ajustar para caber in place of Registrar assim. */
    val choice: BudgetChoice? = null,
    /** A60 part D (chatR): Reservar para o {slot} below Registrar assim; null = no pill. */
    val reserve: SlotRef? = null,
)

/** chatRB (A60 part A): the window the plan goes over ([limitKcal]) and by how much. */
@Immutable
data class BudgetChoice(val limitKcal: Int, val overKcal: Int)

/** chatRB: `Passa {overKcal} kcal do que sobra.` and one `Reservei {kcal} kcal para {label}.` per reservation. */
@Immutable
data class BudgetNote(val overKcal: Int, val reserved: List<Pair<Int, String>>)

/**
 * A61 part B (ADR-048): what Copiar takes from a text bubble, null when the item is not selectable. The user's text as
 * typed (a photo only with its caption), a Tali reply as shown without markers (bullets keep `- `, table rows
 * `{item}: {gramas}`), a question. Never receipts, cards, chips, the greeting or the meta card.
 */
val ChatItem.copyText: String?
    get() = when (this) {
        is ChatItem.User -> text.takeIf { it.isNotBlank() && !pending }
        is ChatItem.Assistant -> text.takeIf { prose && it.isNotBlank() }?.let { if (blocks != null) ReplyMarkup.plain(it, bullet = "- ") else it }
        is ChatItem.Question -> text.takeIf { it.isNotBlank() }
        else -> null
    }

internal fun List<MealSlot>.refs() = sortedBy { it.minutesFromMidnight }
    .map { SlotRef(it.id, it.name, SlotSuggestions.format(it.minutesFromMidnight), it.minutesFromMidnight) }
