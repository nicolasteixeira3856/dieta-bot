package com.nutri.android.domain

/** Chat text limit (ADR-022): the same count as the server's Pydantic max_length on /v1/chat. */
object ChatText {
    const val MAX_CHARS = 2000

    /** Code points of the trimmed text: an emoji counts 1, like Python's len(). */
    fun length(text: String): Int {
        val trimmed = text.trim()
        return trimmed.codePointCount(0, trimmed.length)
    }

    fun tooLong(text: String): Boolean = length(text) > MAX_CHARS

    /** First [max] code points: a prompt defence that never splits an emoji. */
    fun clip(text: String, max: Int = MAX_CHARS): String {
        if (text.length <= max) return text
        val count = text.codePointCount(0, text.length)
        if (count <= max) return text
        return text.substring(0, text.offsetByCodePoints(0, max))
    }
}
