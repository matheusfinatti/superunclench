package com.mfinatti.noclenchingsrs.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class QuietHoursRuleTest {

    private val tz = TimeZone.getTimeZone("America/New_York")
    private val rule = QuietHoursRule(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60, timeZone = tz)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `window crosses midnight`() {
        assertFalse(rule.isInsideWindow(at(2026, 10, 1, 21, 59)))
        assertTrue(rule.isInsideWindow(at(2026, 10, 1, 22, 0)))
        assertTrue(rule.isInsideWindow(at(2026, 10, 2, 3, 0)))
        assertTrue(rule.isInsideWindow(at(2026, 10, 2, 6, 59)))
        assertFalse(rule.isInsideWindow(at(2026, 10, 2, 7, 0)))
    }

    @Test
    fun `alarm due inside quiet hours is deferred to the end, outside unchanged`() {
        assertEquals(at(2026, 10, 2, 7), rule.adjust(at(2026, 10, 1, 22, 3), at(2026, 10, 1, 21, 58)))
        assertEquals(at(2026, 10, 2, 7), rule.adjust(at(2026, 10, 2, 1, 0), at(2026, 10, 1, 21, 0)))
        val day = at(2026, 10, 1, 15)
        assertEquals(day, rule.adjust(day, day - 60_000))
    }

    @Test
    fun `disabled never defers`() {
        val off = rule.copy(enabled = false)
        val night = at(2026, 10, 1, 23)
        assertEquals(night, off.adjust(night, night))
        assertFalse(off.isActive(night))
    }

    @Test
    fun `simulate makes now quiet until the next end time`() {
        val sim = rule.copy(simulate = true)
        val noon = at(2026, 10, 1, 12)
        assertTrue(sim.isActive(noon))
        assertEquals(at(2026, 10, 2, 7), sim.adjust(noon + 5_000, noon))
    }

    @Test
    fun `end time is DST safe`() {
        // 2026-11-01: US clocks fall back at 02:00; 07:00 is still 07:00 local.
        val end = rule.endAfter(at(2026, 10, 31, 23))
        val cal = Calendar.getInstance(tz).apply { timeInMillis = end }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `duration wraps midnight and is never zero for a valid range`() {
        assertEquals(9 * 60, quietDurationMinutes(22 * 60, 7 * 60))
        assertEquals(6 * 60 + 45, quietDurationMinutes(23 * 60 + 30, 6 * 60 + 15))
        assertEquals(60, quietDurationMinutes(13 * 60, 14 * 60))
        assertEquals(23 * 60 + 59, quietDurationMinutes(0, 23 * 60 + 59))
        assertEquals(1, quietDurationMinutes(23 * 60 + 59, 0))
    }

    @Test
    fun `minute precision overnight window`() {
        val r = rule.copy(startMinutes = 23 * 60 + 30, endMinutes = 6 * 60 + 15)
        assertFalse(r.isInsideWindow(at(2026, 10, 1, 23, 29)))
        assertTrue(r.isInsideWindow(at(2026, 10, 1, 23, 30)))
        assertTrue(r.isInsideWindow(at(2026, 10, 2, 6, 14)))
        assertFalse(r.isInsideWindow(at(2026, 10, 2, 6, 15)))
        assertEquals(at(2026, 10, 2, 6, 15), r.adjust(at(2026, 10, 1, 23, 45), at(2026, 10, 1, 23, 40)))
    }

    @Test
    fun `same-day window`() {
        val r = rule.copy(startMinutes = 13 * 60, endMinutes = 14 * 60)
        assertTrue(r.isInsideWindow(at(2026, 10, 1, 13, 30)))
        assertFalse(r.isInsideWindow(at(2026, 10, 1, 23, 0)))
        assertEquals(at(2026, 10, 1, 14), r.adjust(at(2026, 10, 1, 13, 10), at(2026, 10, 1, 13, 0)))
    }

    @Test
    fun `a deferral is recognised, a real due time is not`() {
        val now = at(2026, 10, 1, 23)
        assertTrue(rule.isDeferredAlarm(at(2026, 10, 2, 7), now))
        assertFalse(rule.isDeferredAlarm(at(2026, 10, 2, 7, 5), now))
        assertFalse(rule.isDeferredAlarm(at(2026, 10, 1, 12), now))
        assertFalse(rule.copy(enabled = false).isDeferredAlarm(at(2026, 10, 2, 7), now))
    }
}
