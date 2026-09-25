package com.nutri.android.data

import java.time.Instant

fun interface InstantClock {
    fun now(): Instant
}
