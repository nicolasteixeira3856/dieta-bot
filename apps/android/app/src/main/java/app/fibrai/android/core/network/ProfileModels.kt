package app.fibrai.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors server/profile_build.py ProfileRequestIn (POST /v1/profile, S41, ADR-057). */
@Serializable
data class ProfileIn(
    @SerialName("local_time") val localTime: String,
    val body: ProfileBody,
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("p_target") val pTarget: Int,
    @SerialName("c_target") val cTarget: Int,
    @SerialName("g_target") val gTarget: Int,
    @SerialName("eat_back") val eatBack: ProfileEatBack,
    val slots: List<ChatSlot>,
    val tone: String,
    val notifications: ProfileNotifications,
    val goal: ChatGoal? = null,
    val answers: ProfileAnswers,
)

@Serializable
data class ProfileBody(
    val sex: String,
    val age: Int,
    @SerialName("height_cm") val heightCm: Int,
    @SerialName("weight_kg") val weightKg: Double,
)

/** `pct` only for `partial` (the server rejects it elsewhere): null is not encoded. */
@Serializable
data class ProfileEatBack(val mode: String, val pct: Int? = null)

@Serializable
data class ProfileNotifications(val enabled: Boolean, @SerialName("closure_time") val closureTime: String? = null)

/** The free-text answers; null = skipped (not encoded, read by the server as null). */
@Serializable
data class ProfileAnswers(
    val restrictions: String? = null,
    val measuring: String? = null,
    val foods: String? = null,
    val dislikes: String? = null,
    val equipment: String? = null,
)

/** A goal weight and an optional ISO date (`profile.goal` of /v1/chat and /v1/close, the accepted goal of /v1/profile). */
@Serializable
data class ChatGoal(@SerialName("weight_kg") val weightKg: Double, val date: String? = null)

@Serializable
data class ProfileOut(
    val goal: ChatGoal? = null,
    @SerialName("goal_refused") val goalRefused: Boolean = false,
    val facts: List<ProfileFact> = emptyList(),
    val summary: String = "",
    @SerialName("request_id") val requestId: String? = null,
)

/** A declared fact of the profile build: `kind` `permanent` | `dynamic`; a routine carries the request slot id and its numbers. */
@Serializable
data class ProfileFact(
    val kind: String = "permanent",
    val category: String = "preference",
    val key: String = "",
    val text: String = "",
    val slot: String? = null,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
)
