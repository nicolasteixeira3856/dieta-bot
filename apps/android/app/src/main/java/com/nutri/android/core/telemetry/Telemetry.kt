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

    /** A28: count per applied memory operation + permanent/dynamic totals. Numbers only. */
    const val MEMORY_CHANGED = "memory_changed"

    const val KEY_ENV = "env"
    const val KEY_LAST_REQUEST_ID = "last_request_id"

    /** Text length as a bucket: the text itself never leaves the device through telemetry. */
    fun lengthBucket(length: Int): String = when {
        length < 20 -> "<20"
        length <= 100 -> "20-100"
        else -> ">100"
    }
}
