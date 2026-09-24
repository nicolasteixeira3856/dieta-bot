package com.nutri.android.domain

fun janelaNaHora(hora: Int): String = when (hora) {
    in 5..9 -> "cafe"
    in 10..11 -> "lanche_manha"
    in 12..14 -> "almoco"
    in 15..17 -> "lanche_tarde"
    in 18..21 -> "janta"
    else -> "ceia"
}

fun tituloJanela(id: String): String = when (id) {
    "cafe" -> "Café"
    "lanche_manha" -> "Lanche manhã"
    "almoco" -> "Almoço"
    "lanche_tarde" -> "Lanche tarde"
    "janta" -> "Janta"
    else -> "Ceia"
}
