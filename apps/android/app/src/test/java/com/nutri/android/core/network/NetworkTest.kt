package com.nutri.android.core.network

import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DAY_PREF_KEY
import com.nutri.android.core.database.DAY_STORE_NAME
import io.mockk.mockk
import okhttp3.Request
import org.junit.Test

class NetworkTest {
    @Test
    fun `day store keys are English`() {
        assertThat(DAY_STORE_NAME).isEqualTo("nutri_day")
        assertThat(DAY_PREF_KEY).isEqualTo("day")
        assertThat(DAY_STORE_NAME).doesNotContain("dia")
        assertThat(DAY_PREF_KEY).isNotEqualTo("dia")
    }

    @Test
    fun `header X-Invite is sent on the request`() {
        val interceptor = InviteInterceptor("troca-isto")
        val chain = mockk<okhttp3.Interceptor.Chain>()
        val original = Request.Builder().url("http://127.0.0.1:8080/health").build()
        var sent: Request? = null
        io.mockk.every { chain.request() } returns original
        io.mockk.every { chain.proceed(any()) } answers {
            sent = firstArg()
            mockk(relaxed = true)
        }
        interceptor.intercept(chain)
        assertThat(sent!!.header("X-Invite")).isEqualTo("troca-isto")
    }
}
