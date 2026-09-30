package com.mfinatti.noclenchingsrs.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatTest {
    @Test
    fun `mm-ss under an hour, h-mm-ss from an hour, never negative`() {
        assertEquals("05:00", formatCountdown(5 * 60_000L))
        assertEquals("04:59", formatCountdown(4 * 60_000L + 59_000L))
        assertEquals("00:05", formatCountdown(4_200L)) // rounds up partial seconds
        assertEquals("59:59", formatCountdown(3_599_000L))
        assertEquals("1:00:00", formatCountdown(3_600_000L))
        assertEquals("1:59:59", formatCountdown(7_199_000L))
        assertEquals("00:00", formatCountdown(-5_000L))
    }
}
