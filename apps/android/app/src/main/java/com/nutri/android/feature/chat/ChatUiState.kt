package com.nutri.android.feature.chat

import androidx.compose.runtime.Immutable
import com.nutri.android.core.database.MealSlot
import com.nutri.android.domain.ProjectedDay
import com.nutri.android.domain.SlotSuggestions

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
)

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
    ) : ChatItem {
        override val key = "a-$id"
    }

    /**
     * A question in its own bubble. [standalone] = a question before the estimate (A30, chatQ): the
     * "Dieta Bot AI" label above, no reply bubble. Else the follow-up right below an old estimate row.
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
        val skipped: Boolean,
        val slotName: String,
        val slotTime: String?,
        val kcal: Int?,
        /** "Atualizado em": the slot's log was replaced (ADR-017). */
        val replaced: Boolean = false,
        /** The record applied a routine to the memory (A28): `Memória atualizada` below it. */
        val memoryUpdated: Boolean = false,
    ) : ChatItem {
        override val key = "r-$id"
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

    data object Failed : ChatItem {
        override val key = "failed"
    }
}

@Immutable
data class SlotRef(val id: Long, val name: String, val time: String, val minutes: Int)

@Immutable
data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val composer: String = "",
    /** Composer over [ChatText.MAX_CHARS][com.nutri.android.domain.ChatText.MAX_CHARS] (chatX): nothing leaves. */
    val composerTooLong: Boolean = false,
    val sending: Boolean = false,
    /** Empty day: meta card + suggestion chips (chat0). */
    val emptyDay: Boolean = true,
    val metaRemaining: Int = 0,
    val metaTotal: Int = 0,
    /** Actions for the latest estimate that has no receipt yet. */
    val actions: EstimateActions? = null,
    /** Forçar estimativa in the actions slot (A30, chatQ): the last message is a question of round 2 or more. */
    val forceEstimate: Boolean = false,
    val slots: List<SlotRef> = emptyList(),
    val currentSlotId: Long? = null,
    /** Estimate id whose "Trocar" sheet is open. */
    val sheetFor: Long? = null,
    val sheetSelection: Long? = null,
    /** Slot waiting for the skip confirmation (chatP). */
    val skipConfirm: SlotRef? = null,
    /** Slot waiting for the replace confirmation (chatP layout, ADR-017). */
    val replaceConfirm: ReplaceConfirm? = null,
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

/** Gravar on a taken slot: "{slot} tem {oldKcal} kcal. Fica com {newKcal} kcal." */
@Immutable
data class ReplaceConfirm(val estimateId: Long, val slot: SlotRef, val oldKcal: Int, val newKcal: Int)

@Immutable
data class EstimateActions(
    val estimateId: Long,
    /** Null when the suggested slot is not in the profile: only Trocar and Pular show. */
    val record: SlotRef?,
    val skip: SlotRef?,
    /** A plan (chatR): one Registrar assim button instead of Gravar | Trocar | Pular. */
    val plan: Boolean = false,
)

internal fun List<MealSlot>.refs() = sortedBy { it.minutesFromMidnight }
    .map { SlotRef(it.id, it.name, SlotSuggestions.format(it.minutesFromMidnight), it.minutesFromMidnight) }
