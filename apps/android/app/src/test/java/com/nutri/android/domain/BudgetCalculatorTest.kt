package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class BudgetCalculatorTest {
    private val calc = BudgetCalculator()
    private val segunda = LocalDate.of(2026, 9, 21)
    private val sexta = LocalDate.of(2026, 9, 25)
    private val sabado = LocalDate.of(2026, 9, 26)
    private val domingo = LocalDate.of(2026, 9, 27)
    private val quarta = LocalDate.of(2026, 9, 23)

    @Test
    fun `politica 0 com treino 1000 credita 0`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.ZERO,
                treinoKcal = 1000,
            ),
        )
        assertThat(r.creditoTreino).isEqualTo(0)
        assertThat(r.tetoEfetivo).isEqualTo(2000)
    }

    @Test
    fun `treino ausente com politica 100 credita 0`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.CEM,
                treinoKcal = null,
            ),
        )
        assertThat(r.creditoTreino).isEqualTo(0)
        assertThat(r.tetoEfetivo).isEqualTo(2000)
    }

    @Test
    fun `politica 50 com treino 480 credita 240`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.PARCIAL,
                percentual = 50,
                treinoKcal = 480,
            ),
        )
        assertThat(r.creditoTreino).isEqualTo(240)
        assertThat(r.tetoEfetivo).isEqualTo(2240)
    }

    @Test
    fun `politica 100 com treino 480 credita 480`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.CEM,
                treinoKcal = 480,
            ),
        )
        assertThat(r.creditoTreino).isEqualTo(480)
        assertThat(r.tetoEfetivo).isEqualTo(2480)
    }

    @Test
    fun `sem cap treino 2000 a 100 por cento soma 2000 no teto`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.CEM,
                treinoKcal = 2000,
            ),
        )
        assertThat(r.creditoTreino).isEqualTo(2000)
        assertThat(r.tetoEfetivo).isEqualTo(4000)
    }

    @Test
    fun `orcamento janela nunca negativo`() {
        val r = calc.calcular(
            EntradaOrcamento(
                data = quarta,
                perfil = TetoMesmoTodosOsDias(2000),
                politica = PoliticaCredito.ZERO,
                treinoKcal = 1000,
                consumido = 1500,
                reservaProximas = 700,
            ),
        )
        assertThat(r.orcamentoJanela).isEqualTo(0)
        assertThat(r.orcamentoJanela).isAtLeast(0)
    }

    @Test
    fun `teto base difere entre mesmo todos os dias util fds e 7 dias`() {
        assertThat(segunda.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(sabado.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(domingo.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)

        val mesmo = TetoMesmoTodosOsDias(2000)
        assertThat(mesmo.tetoNaData(quarta)).isEqualTo(2000)
        assertThat(mesmo.tetoNaData(domingo)).isEqualTo(2000)

        val util = TetoUtilFds(util = 2000, fds = 2300)
        assertThat(util.tetoNaData(segunda)).isEqualTo(2000)
        assertThat(util.tetoNaData(sexta)).isEqualTo(2000)
        assertThat(util.tetoNaData(sabado)).isEqualTo(2300)
        assertThat(util.tetoNaData(domingo)).isEqualTo(2300)
        assertThat(util.tetoNaData(segunda)).isNotEqualTo(util.tetoNaData(sabado))

        val sete = TetoSeteDias(1900, 2100, 1800, 2400, 2000, 2600, 1700)
        val dias = listOf(
            LocalDate.of(2026, 9, 21),
            LocalDate.of(2026, 9, 22),
            LocalDate.of(2026, 9, 23),
            LocalDate.of(2026, 9, 24),
            LocalDate.of(2026, 9, 25),
            LocalDate.of(2026, 9, 26),
            LocalDate.of(2026, 9, 27),
        )
        val tetos = dias.map { sete.tetoNaData(it) }
        assertThat(tetos).containsExactly(1900, 2100, 1800, 2400, 2000, 2600, 1700).inOrder()
        assertThat(tetos.toSet()).hasSize(7)
    }

    @Test
    fun `data America Sao Paulo muda o weekday perto da meia noite UTC`() {
        val instant = Instant.parse("2026-09-27T02:30:00Z")
        val data = SaoPaulo.data(instant)
        assertThat(data).isEqualTo(LocalDate.of(2026, 9, 26))
        val teto = TetoUtilFds(util = 2000, fds = 2300).tetoNaData(data)
        assertThat(teto).isEqualTo(2300)
    }
}
