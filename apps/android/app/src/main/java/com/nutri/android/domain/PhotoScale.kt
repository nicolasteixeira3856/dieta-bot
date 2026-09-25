package com.nutri.android.domain

object PhotoScale {
    const val MAX_SIDE = 1280
    const val JPEG_QUALITY = 70

    fun dimensions(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSide || longest == 0) return width to height
        val factor = maxSide.toDouble() / longest.toDouble()
        val w = (width * factor).toInt().coerceAtLeast(1)
        val h = (height * factor).toInt().coerceAtLeast(1)
        return w to h
    }
}
