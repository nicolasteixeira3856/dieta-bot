package com.nutri.android.domain

data class LogEstavel(val janela: String, val estavel: Boolean)

data class Chip(val janela: String, val pergunta: Boolean)

/**
 * Chip nasce no 2º log estável da mesma janela.
 * Pergunta uma vez. Dá para remover.
 * Dia 1 do app não mostra chip.
 */
fun chipDaJanela(
    diaApp: Int,
    janelaAtual: String,
    logs: List<LogEstavel>,
    removidas: Set<String>,
    perguntadas: Set<String>,
): Chip? {
    if (diaApp <= 1) return null
    val contagem = logs.filter { it.estavel }.groupingBy { it.janela }.eachCount()
    val candidatas = contagem.filter { (janela, n) -> n >= 2 && janela !in removidas }.keys
    if (candidatas.isEmpty()) return null
    val janela = if (janelaAtual in candidatas) janelaAtual else candidatas.first()
    return Chip(janela = janela, pergunta = janela !in perguntadas)
}
