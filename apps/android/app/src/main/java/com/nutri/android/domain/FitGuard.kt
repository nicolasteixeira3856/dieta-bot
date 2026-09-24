package com.nutri.android.domain

data class PratoOferta(val nome: String, val kcal: Double)

/** Nunca oferecer prato que explode o teto da janela. */
fun ofertasQueCabem(pratos: List<PratoOferta>, orcamentoKcal: Double): List<PratoOferta> {
    return pratos.filter { it.nome.isNotBlank() && it.kcal <= orcamentoKcal }
}
