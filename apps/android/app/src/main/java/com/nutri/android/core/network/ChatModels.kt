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
)

@Serializable
data class ChatOut(
    val reply: String = "",
    val estimate: ChatEstimate? = null,
    val digest: String? = null,
    val model: String = "",
)
