package com.mfinatti.noclenchingsrs.feature.stats.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SlotIndexTest {
    @Test
    fun `x maps to the slot drawn under it for every frame size`() {
        listOf(24, 7, 30).forEach { n ->
            val width = 828f
            val slot = width / n
            (0 until n).forEach { i ->
                assertEquals("n=$n i=$i centre", i, slotIndexAt(slot * i + slot / 2, width, n))
                assertEquals("n=$n i=$i left edge", i, slotIndexAt(slot * i + 0.5f, width, n))
            }
            assertEquals(0, slotIndexAt(-5f, width, n))
            assertEquals(n - 1, slotIndexAt(width + 5f, width, n))
        }
    }
}
