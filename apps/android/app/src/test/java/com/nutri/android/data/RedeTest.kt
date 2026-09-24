package com.nutri.android.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Test

class RedeTest {
    @Test
    fun `header X-Invite vai na request`() {
        val interceptor = InviteInterceptor("troca-isto")
        val chain = mockk<okhttp3.Interceptor.Chain>()
        val original = Request.Builder().url("http://127.0.0.1:8080/health").build()
        var enviado: Request? = null
        io.mockk.every { chain.request() } returns original
        io.mockk.every { chain.proceed(any()) } answers {
            enviado = firstArg()
            mockk(relaxed = true)
        }
        interceptor.intercept(chain)
        assertThat(enviado!!.header("X-Invite")).isEqualTo("troca-isto")
    }

    @Test
    fun `falha de rede vira confianca baixa e a pergunta de uma linha`() = runTest {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } throws java.io.IOException("down")
        val gate = EstimateGate(api)
        flow {
            emit(
                gate.estimar(
                    EstimateIn(local_time = "2026-09-24T09:00:00-03:00", janela = "cafe", text = "pao"),
                ),
            )
        }.test {
            val item = awaitItem()
            assertThat(item.confianca).isEqualTo("baixa")
            assertThat(item.pergunta).isEqualTo("descreve em 1 linha")
            awaitComplete()
        }
    }

    @Test
    fun `confianca alta omite a pergunta`() = runTest {
        val api = mockk<NutriApi>()
        coEvery { api.estimate(any()) } returns EstimateOut(kcal = 385.0, p = 18.0, confianca = "alto", pergunta = "sumir")
        val out = EstimateGate(api).estimar(
            EstimateIn(local_time = "2026-09-24T09:00:00-03:00", janela = "cafe", text = "pao"),
        )
        assertThat(out.pergunta).isNull()
        assertThat(out.kcal).isEqualTo(385.0)
    }
}
