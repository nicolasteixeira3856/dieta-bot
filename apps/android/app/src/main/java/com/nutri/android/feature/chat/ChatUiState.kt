package com.nutri.android.feature.chat

import androidx.compose.runtime.Immutable
import com.nutri.android.core.database.MealSlot
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
    ) : ChatItem {
        override val key = "a-$id"
    }

    data class Receipt(val id: Long, val skipped: Boolean, val slotName: String, val slotTime: String?, val kcal: Int?) : ChatItem {
        override val key = "r-$id"
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
    val sending: Boolean = false,
    /** Empty day: meta card + suggestion chips (chat0). */
    val emptyDay: Boolean = true,
    val metaRemaining: Int = 0,
    val metaTotal: Int = 0,
    /** Actions for the latest estimate that has no receipt yet. */
    val actions: EstimateActions? = null,
    val slots: List<SlotRef> = emptyList(),
    val currentSlotId: Long? = null,
    /** Estimate id whose "Trocar" sheet is open. */
    val sheetFor: Long? = null,
    val sheetSelection: Long? = null,
    /** Slot waiting for the skip confirmation (chatP). */
    val skipConfirm: SlotRef? = null,
    /** Camera / gallery chooser (A6). */
    val photoSheet: Boolean = false,
    /** Snackbar text, shown once. */
    val notice: String? = null,
) {
    val canSend: Boolean get() = composer.isNotBlank() && !sending
}

@Immutable
data class EstimateActions(
    val estimateId: Long,
    /** Null when the suggested slot is not in the profile: only Trocar and Pular show. */
    val record: SlotRef?,
    val skip: SlotRef?,
)

internal fun List<MealSlot>.refs() = sortedBy { it.minutesFromMidnight }
    .map { SlotRef(it.id, it.name, SlotSuggestions.format(it.minutesFromMidnight), it.minutesFromMidnight) }
