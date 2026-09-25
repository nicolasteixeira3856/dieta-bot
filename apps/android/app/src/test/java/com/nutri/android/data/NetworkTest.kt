package com.nutri.android.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `network failure becomes low confidence and the one-line question`() = runTest {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } throws java.io.IOException("down")
        val gate = EstimateGate(api)
        flow {
            emit(
                gate.estimate(
                    EstimateIn(local_time = "2026-09-24T09:00:00-03:00", window = "breakfast", text = "pao"),
                ),
            )
        }.test {
            val item = awaitItem()
            assertThat(item.confidence).isEqualTo("low")
            assertThat(item.question).isEqualTo("descreve em 1 linha")
            awaitComplete()
        }
    }

    @Test
    fun `high confidence omits the question`() = runTest {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } returns EstimateOut(kcal = 385.0, p = 18.0, confidence = "high", question = "sumir")
        val out = EstimateGate(api).estimate(
            EstimateIn(local_time = "2026-09-24T09:00:00-03:00", window = "breakfast", text = "pao"),
        )
        assertThat(out.question).isNull()
        assertThat(out.kcal).isEqualTo(385.0)
    }
}
