package com.nutri.android.data

import com.nutri.android.domain.PratoOferta
import com.nutri.android.domain.ofertasQueCabem

const val PERGUNTA_FALHA = "descreve em 1 linha"

class EstimateGate(private val api: NutriApi) {
    suspend fun estimar(body: EstimateIn): EstimateOut {
        return try {
            val out = api.estimate(body)
            if (out.confianca == "alto") out.copy(pergunta = null) else {
                out.copy(pergunta = out.pergunta?.takeIf { it.isNotBlank() } ?: PERGUNTA_FALHA)
            }
        } catch (_: Exception) {
            EstimateOut(confianca = "baixa", pergunta = PERGUNTA_FALHA)
        }
    }

    suspend fun encaixar(body: FitIn): FitOut {
        return try {
            val out = api.fit(body)
            val cabem = ofertasQueCabem(
                pratos = (out.opcoes + out.prato).map { PratoOferta(it.nome, it.kcal) },
                orcamentoKcal = body.orcamento.kcal,
            ).map { it.nome }.toSet()
            val opcoes = out.opcoes.filter { it.nome in cabem }
            val prato = if (out.prato.nome in cabem) out.prato else PratoOut()
            out.copy(
                prato = prato,
                opcoes = opcoes,
                cabe = prato.nome.isNotBlank() || opcoes.isNotEmpty(),
                pergunta = out.pergunta.ifBlank { PERGUNTA_FALHA },
            )
        } catch (_: Exception) {
            FitOut(cabe = false, pergunta = PERGUNTA_FALHA)
        }
    }
}
