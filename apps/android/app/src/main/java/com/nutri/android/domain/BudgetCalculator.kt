package com.nutri.android.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Teto do dia e orçamento da janela. Dado puro, sem Android e sem rede. */
object SaoPaulo {
    val zona: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun data(instant: Instant): LocalDate = instant.atZone(zona).toLocalDate()
}

sealed interface TetoPerfil {
    fun tetoNaData(data: LocalDate): Int
}

data class TetoMesmoTodosOsDias(val kcal: Int) : TetoPerfil {
    override fun tetoNaData(data: LocalDate): Int = kcal
}

data class TetoUtilFds(val util: Int, val fds: Int) : TetoPerfil {
    override fun tetoNaData(data: LocalDate): Int {
        val fim = data.dayOfWeek == DayOfWeek.SATURDAY || data.dayOfWeek == DayOfWeek.SUNDAY
        return if (fim) fds else util
    }
}

data class TetoSeteDias(
    val segunda: Int,
    val terca: Int,
    val quarta: Int,
    val quinta: Int,
    val sexta: Int,
    val sabado: Int,
    val domingo: Int,
) : TetoPerfil {
    override fun tetoNaData(data: LocalDate): Int = when (data.dayOfWeek) {
        DayOfWeek.MONDAY -> segunda
        DayOfWeek.TUESDAY -> terca
        DayOfWeek.WEDNESDAY -> quarta
        DayOfWeek.THURSDAY -> quinta
        DayOfWeek.FRIDAY -> sexta
        DayOfWeek.SATURDAY -> sabado
        DayOfWeek.SUNDAY -> domingo
    }
}

enum class PoliticaCredito { ZERO, PARCIAL, CEM }

data class EntradaOrcamento(
    val data: LocalDate,
    val perfil: TetoPerfil,
    val politica: PoliticaCredito,
    val percentual: Int? = null,
    val treinoKcal: Int? = null,
    val consumido: Int = 0,
    val reservaProximas: Int = 0,
)

data class ResultadoOrcamento(
    val tetoBase: Int,
    val creditoTreino: Int,
    val tetoEfetivo: Int,
    val orcamentoJanela: Int,
)

class BudgetCalculator {
    fun calcular(entrada: EntradaOrcamento): ResultadoOrcamento {
        val tetoBase = entrada.perfil.tetoNaData(entrada.data)
        val credito = creditoTreino(entrada.politica, entrada.percentual, entrada.treinoKcal)
        val tetoEfetivo = tetoBase + credito
        val bruto = tetoEfetivo - entrada.consumido - entrada.reservaProximas
        return ResultadoOrcamento(
            tetoBase = tetoBase,
            creditoTreino = credito,
            tetoEfetivo = tetoEfetivo,
            orcamentoJanela = maxOf(0, bruto),
        )
    }
}

/** Política 0, ou treino do dia ainda não informado, zera o crédito. Sem cap. */
fun creditoTreino(politica: PoliticaCredito, percentual: Int?, treinoKcal: Int?): Int {
    if (politica == PoliticaCredito.ZERO || treinoKcal == null) return 0
    if (politica == PoliticaCredito.CEM) return treinoKcal
    val pct = percentual ?: error("percentual")
    return treinoKcal * pct / 100
}
