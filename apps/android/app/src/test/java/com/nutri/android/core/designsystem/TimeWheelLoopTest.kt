package com.nutri.android.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeWheelLoopTest {
    @Test fun startsAtEverySuppliedHourAndMinute() {
        for (period in listOf(24, 60)) for (value in 0 until period) {
            val index = TimeWheelLoop.initialIndex(value, period)
            assertEquals(value, TimeWheelLoop.value(index, period))
            assertTrue(index in 499_900..500_100)
        }
    }

    @Test fun wrapsBothWaysAtMidnightAndMinuteBoundary() {
        for (period in listOf(24, 60)) {
            val last = TimeWheelLoop.initialIndex(period - 1, period)
            val zero = TimeWheelLoop.initialIndex(0, period)
            assertEquals(0, TimeWheelLoop.value(last + 1, period))
            assertEquals(period - 1, TimeWheelLoop.value(zero - 1, period))
            assertEquals(last + 1, TimeWheelLoop.nearestIndex(last, 0, period))
            assertEquals(zero - 1, TimeWheelLoop.nearestIndex(zero, period - 1, period))
        }
    }

    @Test fun virtualIndicesKeepValuesThroughManyCycles() {
        for (period in listOf(24, 60)) {
            val index = TimeWheelLoop.initialIndex(7, period)
            assertEquals(7, TimeWheelLoop.value(index + period * 100, period))
            assertEquals(7, TimeWheelLoop.value(index - period * 100, period))
        }
    }
}
