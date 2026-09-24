package com.nutri.android.domain

object FotoEscala {
    const val LADO_MAXIMO = 1280
    const val QUALIDADE_JPEG = 70

    fun dimensoes(largura: Int, altura: Int, maxLado: Int = LADO_MAXIMO): Pair<Int, Int> {
        val maior = maxOf(largura, altura)
        if (maior <= maxLado || maior == 0) return largura to altura
        val fator = maxLado.toDouble() / maior.toDouble()
        val w = (largura * fator).toInt().coerceAtLeast(1)
        val h = (altura * fator).toInt().coerceAtLeast(1)
        return w to h
    }
}
