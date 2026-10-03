package com.nutri.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors server/main.py ChatIn (POST /v1/chat). */
@Serializable
data class ChatIn(
    @SerialName("local_time") val localTime: String,
    val profile: ChatProfile,
    val memory: String = "",
    val day: ChatDay,
    val digests: List<String> = emptyList(),
    val messages: List<ChatTurn> = emptyList(),
    val text: String,
    @SerialName("image_b64") val imageB64: String? = null,
    val compact: Boolean = false,
    /** Meals of the 7 days before today (S11). */
    val recent: List<ChatRecentMeal> = emptyList(),
    /**
     * Memory v2 (A28). Present, even empty, = v2 client: the server reads these instead of [memory]
     * and answers memory_updates / memory_used. Null only to talk as a legacy client.
     */
    val facts: List<ChatFact>? = null,
    /**
     * Question rounds already shown for the pending meal, 0-3 (A30, ADR-026). Always sent: the server
     * may answer a question only and releases the estimate after 3. No default on purpose: Json does not
     * encode defaults, and an absent field makes the server treat the app as a legacy client.
     */
    @SerialName("clarify_rounds") val clarifyRounds: Int,
    /** Forçar estimativa: the server releases the estimate now. */
    @SerialName("force_estimate") val forceEstimate: Boolean = false,
    /**
     * The client records by itself (A34, ADR-028): the server answers [ChatOut.record]. No default, like
     * [clarifyRounds]: Json does not encode defaults and the field must always go.
     */
    @SerialName("auto_record") val autoRecord: Boolean,
    /**
     * The client stores temp facts (A38, ADR-029): v5 client, the server may propose `kind: temp` and answers
     * [ChatOut.questionSlot]. No default, like [autoRecord]: the field must always go.
     */
    @SerialName("temp_facts") val tempFacts: Boolean,
)

/** A memory fact as the server sees it (api-contract). slot is a profile slot id or null. kind "temp" = a T id (A38). */
@Serializable
data class ChatFact(
    val id: String,
    val kind: String,
    val category: String,
    val key: String,
    val text: String,
    val slot: String?,
    @SerialName("days_seen") val daysSeen: Int,
    @SerialName("last_seen") val lastSeen: String?,
)

/** op: "add" | "reinforce" | "replace" | "remove". id is null only on add. */
@Serializable
data class ChatMemoryUpdate(
    val op: String,
    val id: String? = null,
    val kind: String,
    val category: String,
    val key: String = "",
    val text: String = "",
    val slot: String? = null,
)

/** slotId null = "Outros" (orphan or deleted slot). */
@Serializable
data class ChatRecentMeal(
    val date: String,
    @SerialName("slot_id") val slotId: String?,
    @SerialName("slot_name") val slotName: String,
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
)

@Serializable
data class ChatProfile(
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("p_target") val pTarget: Int,
    @SerialName("c_target") val cTarget: Int,
    @SerialName("g_target") val gTarget: Int,
    @SerialName("eat_back") val eatBack: String,
    val slots: List<ChatSlot> = emptyList(),
)

@Serializable
data class ChatSlot(val id: String, val name: String, val time: String)

@Serializable
data class ChatDay(
    val date: String,
    @SerialName("eaten_kcal") val eatenKcal: Int = 0,
    @SerialName("eaten_p") val eatenP: Int = 0,
    @SerialName("eaten_c") val eatenC: Int = 0,
    @SerialName("eaten_g") val eatenG: Int = 0,
    @SerialName("workout_kcal") val workoutKcal: Int? = null,
    /** Effective ceiling - eaten; may be negative (S11). */
    @SerialName("remaining_kcal") val remainingKcal: Int? = null,
    val slots: List<ChatDaySlot> = emptyList(),
)

/** status: "empty" | "eaten" | "skipped". */
@Serializable
data class ChatDaySlot(
    val id: String,
    val status: String,
    val text: String? = null,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
)

/** role: "user" | "assistant". */
@Serializable
data class ChatTurn(val role: String, val text: String)

@Serializable
data class ChatEstimate(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val g: Double = 0.0,
    val confidence: String = "low",
    val question: String? = null,
    val items: List<ItemOut> = emptyList(),
    @SerialName("suggested_slot") val suggestedSlot: String? = null,
    /** The whole meal as corrected by the conversation (S11). The app records this text. */
    @SerialName("meal_text") val mealText: String? = null,
)

@Serializable
data class ChatOut(
    val reply: String = "",
    /** "log" | "plan" | "question" | "skip" (S14); null = server before S11, handled as today. */
    val intent: String? = null,
    val estimate: ChatEstimate? = null,
    /** At most 5 proposals; the app applies them with MemoryRules (A28). */
    @SerialName("memory_updates") val memoryUpdates: List<ChatMemoryUpdate> = emptyList(),
    /** Fact ids the reply relied on. */
    @SerialName("memory_used") val memoryUsed: List<String> = emptyList(),
    val digest: String? = null,
    val model: String = "",
    /** Question before the estimate (A30): non-blank with estimate null = a question-only turn. */
    val question: String? = null,
    /** "auto" | "ask" | "none" (S14, A34). Null = server before S14. */
    val record: String? = null,
    /** Profile slot id of a skip by text (intent "skip"), else null. */
    @SerialName("skip_slot") val skipSlot: String? = null,
    /** Slot the server held with a question-only turn (S16, v5 client); null otherwise. */
    @SerialName("question_slot") val questionSlot: String? = null,
)
