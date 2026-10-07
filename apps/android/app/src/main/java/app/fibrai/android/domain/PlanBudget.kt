package app.fibrai.android.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** A food or meal the server kept out of the plan's window (`plan_budget.reserved`): `Reservei {kcal} kcal para {label}.` */
@Serializable
data class PlanReservation(val label: String, val kcal: Int)

/**
 * The server's budget check of a plan (A60 part A, ADR-039): the window it was checked against, by how much the plan goes
 * over it, what was reserved and what the user already said ([choice]). [local] is the choice made on this device
 * ([OVER_OK] once Pode passar is tapped).
 */
@Serializable
data class PlanBudget(
    val limitKcal: Int,
    val overKcal: Int,
    val reserved: List<PlanReservation> = emptyList(),
    val choice: String? = null,
    val local: String? = null,
) {
    /** The over-budget choice shows (chatRB): over the window, something to adjust to, not accepted yet. */
    val choosing: Boolean get() = overKcal > 0 && limitKcal >= 1 && local != OVER_OK

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val OVER_OK = "over_ok"
        const val FIT = "fit"

        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): PlanBudget? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        /**
         * The response object, read field by field: a missing or malformed `limit_kcal` or `over_kcal` means no choice UI
         * (null); a malformed reservation is dropped; an unknown choice is null.
         */
        fun parse(raw: JsonElement?): PlanBudget? {
            val o = raw as? JsonObject ?: return null
            val limit = o.int("limit_kcal") ?: return null
            val over = o.int("over_kcal")?.takeIf { it >= 0 } ?: return null
            val reserved = (o["reserved"] as? JsonArray).orEmpty().mapNotNull { entry ->
                val r = entry as? JsonObject ?: return@mapNotNull null
                val label = (r["label"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                val kcal = r.int("kcal")?.takeIf { it > 0 } ?: return@mapNotNull null
                PlanReservation(label, kcal)
            }
            val choice = (o["choice"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it == OVER_OK || it == FIT }
            return PlanBudget(limit, over, reserved, choice)
        }

        private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
    }
}
