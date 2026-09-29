package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatTextTest {
    @Test
    fun `2000 is ok, 2001 is too long`() {
        assertThat(ChatText.tooLong("a".repeat(2000))).isFalse()
        assertThat(ChatText.tooLong("a".repeat(2001))).isTrue()
    }

    @Test
    fun `spaces at the ends do not count`() {
        val text = "  \n" + "a".repeat(2000) + " \n\t"
        assertThat(ChatText.length(text)).isEqualTo(2000)
        assertThat(ChatText.tooLong(text)).isFalse()
    }

    @Test
    fun `an emoji counts 1 like the server`() {
        assertThat(ChatText.length("🍚".repeat(2000))).isEqualTo(2000)
        assertThat(ChatText.tooLong("🍚".repeat(2000))).isFalse()
        assertThat(ChatText.tooLong("🍚".repeat(2001))).isTrue()
    }

    @Test
    fun `clip keeps 2000 code points and never splits an emoji`() {
        assertThat(ChatText.clip("abc")).isEqualTo("abc")
        assertThat(ChatText.clip("a".repeat(2001))).isEqualTo("a".repeat(2000))
        assertThat(ChatText.clip("🍚".repeat(2001))).isEqualTo("🍚".repeat(2000))
    }
}
