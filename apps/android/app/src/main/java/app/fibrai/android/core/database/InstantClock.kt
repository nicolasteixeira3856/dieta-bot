package app.fibrai.android.core.database

import java.time.Instant

fun interface InstantClock {
    fun now(): Instant
}
