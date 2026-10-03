package com.nutri.android.core.telemetry

/**
 * A11 (ADR-014): crash, non-fatal and usage signals. dev binds Firebase (src/dev), prod binds [NoopTelemetry].
 *
 * Params carry enums and numbers only: never user text, photo, memory or invite. Conversation
 * content stays in the dev server log (ADR-015), found by the request id.
 */
interface Telemetry {
    fun event(name: String, params: Map<String, Any> = emptyMap())
    fun breadcrumb(message: String)
    fun nonFatal(error: Throwable)
    fun setKey(key: String, value: String)
}

object NoopTelemetry : Telemetry {
    override fun event(name: String, params: Map<String, Any>) = Unit
    override fun breadcrumb(message: String) = Unit
    override fun nonFatal(error: Throwable) = Unit
    override fun setKey(key: String, value: String) = Unit
}

/** Network or HTTP failure of an API call. */
class ApiFailure(route: String, detail: String, requestId: String) :
    Exception("route=$route detail=$detail request_id=$requestId")

/** The server answered the Chat with its "nao deu pra estimar" fallback. */
class ChatFallback(requestId: String?, hasPhoto: Boolean) :
    Exception("request_id=${requestId ?: "none"} has_photo=$hasPhoto")

object TelemetryEvents {
    const val SCREEN_VIEW = "screen_view"
    const val API_CALL = "api_call"
    const val CHAT_SEND = "chat_send"
    const val CHAT_RESULT = "chat_result"
    const val MEAL_SAVED = "meal_saved"
    const val MEAL_SKIPPED = "meal_skipped"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val PUSH_ACTION = "push_action"

    /** A28: count per applied memory operation + permanent/dynamic totals; A38 adds temp totals and temp_* ops. Numbers only. */
    const val MEMORY_CHANGED = "memory_changed"

    /** A38: a send that compacted, `blocks` = digests stored, `summarised` / `kept` = raw messages in / out of them. Numbers only. */
    const val CHAT_COMPACT = "chat_compact"

    /** A29: routine card of the Chat, `action` = shown | record | edit. Enum only. */
    const val ROUTINE_SUGGESTION = "routine_suggestion"

    /** A30: Forçar estimativa tapped, `round` = question rounds shown. Number only. */
    const val CHAT_FORCE_ESTIMATE = "chat_force_estimate"

    /** A34: a record or skip made by the Chat with no tap. kind, slot_state, source (enums), rounds. */
    const val MEAL_AUTO_RECORDED = "meal_auto_recorded"

    /** A34: the Registrar pill, `action` = shown | tapped | expired. */
    const val RECORD_ASK = "record_ask"

    /** A34: inline Substituir, `action` = shown | confirmed | elsewhere | expired, `from` = answer | move. */
    const val REPLACE_CONFIRM = "replace_confirm"

    /** A34: a receipt button. action, receipt, source (enums), age_s, same_day. */
    const val RECEIPT_ACTION = "receipt_action"

    /** A34: memory revert of a receipt, counts of facts `reverted` and `kept`. */
    const val MEMORY_REVERTED = "memory_reverted"

    /** A34: `auto` downgraded to `ask` by a client guard, `reason` enum. */
    const val RECORD_GUARD = "record_guard"

    const val KEY_ENV = "env"
    const val KEY_LAST_REQUEST_ID = "last_request_id"

    /** Text length as a bucket: the text itself never leaves the device through telemetry. */
    fun lengthBucket(length: Int): String = when {
        length < 20 -> "<20"
        length <= 100 -> "20-100"
        else -> ">100"
    }
}
