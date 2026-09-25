package com.nutri.android.domain

fun windowAtHour(hour: Int): String = when (hour) {
    in 5..9 -> "breakfast"
    in 10..11 -> "morningSnack"
    in 12..14 -> "lunch"
    in 15..17 -> "afternoonSnack"
    in 18..21 -> "dinner"
    else -> "eveningSnack"
}

fun windowTitle(id: String): String = when (id) {
    "breakfast" -> "Café"
    "morningSnack" -> "Lanche manhã"
    "lunch" -> "Almoço"
    "afternoonSnack" -> "Lanche tarde"
    "dinner" -> "Janta"
    else -> "Ceia"
}
