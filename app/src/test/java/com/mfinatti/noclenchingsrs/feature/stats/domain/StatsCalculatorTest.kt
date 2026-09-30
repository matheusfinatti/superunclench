package com.mfinatti.noclenchingsrs.feature.stats.domain

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsCalculatorTest {

    private fun localTime(tz: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private val zones = listOf("UTC", "America/New_York", "Europe/London", "Asia/Kolkata", "Pacific/Auckland")
        .map { TimeZone.getTimeZone(it) }

    /** 2026-03-09 20:00 local: the 30-day window crosses the US (Mar 8) DST change. */
    private fun seededNow(tz: TimeZone) = localTime(tz, 2026, 3, 9, 20)

    @Test
    fun `seed has 309 events`() {
        zones.forEach { tz -> assertEquals(tz.id, 309, SampleHistory.generate(seededNow(tz), tz).size) }
    }

    @Test
    fun `seed expected values - today 5 2 1 71 percent`() {
        zones.forEach { tz ->
            val now = seededNow(tz)
            val stats = StatsCalculator.compute(SampleHistory.generate(now, tz), StatsFrame.TODAY, now, tz)
            assertEquals(tz.id, listOf(5, 2, 1, 71), listOf(stats.good, stats.bad, stats.missed, stats.goodRatePercent))
            assertEquals(24, stats.buckets.size)
            assertEquals(2, stats.yMax)
            // Bars at 08 (B), 09 (G), 10 (G), 12 (B), 14 (G), 15 (G), 17 (G).
            val byHour = stats.buckets.mapIndexed { h, b -> h to (b.good to b.bad) }.filter { it.second != (0 to 0) }
            assertEquals(
                tz.id,
                listOf(8 to (0 to 1), 9 to (1 to 0), 10 to (1 to 0), 12 to (0 to 1), 14 to (1 to 0), 15 to (1 to 0), 17 to (1 to 0)),
                byHour,
            )
            assertEquals(1, stats.buckets[11].missed)
        }
    }

    @Test
    fun `seed expected values - 7 days 45 12 1 79 percent`() {
        zones.forEach { tz ->
            val now = seededNow(tz)
            val stats = StatsCalculator.compute(SampleHistory.generate(now, tz), StatsFrame.WEEK, now, tz)
            assertEquals(tz.id, listOf(45, 12, 1, 79), listOf(stats.good, stats.bad, stats.missed, stats.goodRatePercent))
            assertEquals(7, stats.buckets.size)
            assertEquals(10, stats.yMax)
            // Oldest first: days 6..0; day 4 (index 2) is the rest day.
            assertEquals(listOf(10, 10, 0, 10, 10, 10, 7), stats.buckets.map { it.answered })
        }
    }

    @Test
    fun `seed expected values - 30 days 188 98 23 66 percent`() {
        zones.forEach { tz ->
            val now = seededNow(tz)
            val stats = StatsCalculator.compute(SampleHistory.generate(now, tz), StatsFrame.MONTH, now, tz)
            assertEquals(tz.id, listOf(188, 98, 23, 66), listOf(stats.good, stats.bad, stats.missed, stats.goodRatePercent))
            assertEquals(30, stats.buckets.size)
            assertEquals(10, stats.yMax)
            assertEquals(9, stats.buckets[29 - 10].answered) // day 10: 3 Good + 6 Bad
        }
    }

    @Test
    fun `seed streak is 4 days`() {
        zones.forEach { tz ->
            val now = seededNow(tz)
            assertEquals(tz.id, 4, StatsCalculator.streak(SampleHistory.generate(now, tz), now, tz))
        }
    }

    @Test
    fun `seed levels replay the SRS engine from L1`() {
        val tz = TimeZone.getTimeZone("UTC")
        val events = SampleHistory.generate(seededNow(tz), tz)
        assertEquals(1, events.first().level)
        assertEquals(0, events.first().subLevel)
        // Each event's "after" is the next event's "before".
        events.zipWithNext().forEach { (a, b) ->
            assertEquals(a.levelAfter, b.level)
            assertEquals(a.subLevelAfter, b.subLevel)
        }
    }

    @Test
    fun `streak - today pending counts back from yesterday, a bad today breaks it`() {
        val tz = TimeZone.getTimeZone("Europe/Berlin")
        val now = localTime(tz, 2026, 9, 30, 9)
        fun ev(day: Int, hour: Int, outcome: CheckInOutcome) =
            CheckInEvent(localTime(tz, 2026, 9, day, hour), outcome, 1, 0)
        val history = listOf(
            ev(27, 10, CheckInOutcome.BAD), // rate 0% breaks here
            ev(28, 10, CheckInOutcome.GOOD),
            ev(28, 11, CheckInOutcome.BAD), // 50% counts
            ev(29, 10, CheckInOutcome.GOOD),
            ev(29, 11, CheckInOutcome.MISSED),
        )
        assertEquals(2, StatsCalculator.streak(history, now, tz))
        assertEquals(3, StatsCalculator.streak(history + ev(30, 8, CheckInOutcome.GOOD), now, tz))
        assertEquals(0, StatsCalculator.streak(history + ev(30, 8, CheckInOutcome.BAD), now, tz))
        assertEquals(0, StatsCalculator.streak(emptyList(), now, tz))
        // Only missed today: still pending.
        assertEquals(2, StatsCalculator.streak(history + ev(30, 8, CheckInOutcome.MISSED), now, tz))
    }

    @Test
    fun `buckets follow local midnight, not UTC`() {
        val tz = TimeZone.getTimeZone("America/Los_Angeles")
        val now = localTime(tz, 2026, 9, 30, 12)
        // 23:30 local on Sep 29 is Sep 30 in UTC; it must land in yesterday's bucket.
        val lateYesterday = CheckInEvent(localTime(tz, 2026, 9, 29, 23, 30), CheckInOutcome.GOOD, 1, 0)
        val earlyToday = CheckInEvent(localTime(tz, 2026, 9, 30, 0, 10), CheckInOutcome.BAD, 1, 0)
        val week = StatsCalculator.compute(listOf(lateYesterday, earlyToday), StatsFrame.WEEK, now, tz)
        assertEquals(1 to 0, week.buckets[5].good to week.buckets[5].bad)
        assertEquals(0 to 1, week.buckets[6].good to week.buckets[6].bad)
        val today = StatsCalculator.compute(listOf(lateYesterday, earlyToday), StatsFrame.TODAY, now, tz)
        assertEquals(1, today.buckets[0].bad)
        assertEquals(0, today.good)
    }

    @Test
    fun `DST days have correct bounds`() {
        val tz = TimeZone.getTimeZone("America/New_York")
        // 2026-03-08 has 23 hours in New York.
        val now = localTime(tz, 2026, 3, 8, 18)
        val bounds = StatsCalculator.bucketBounds(StatsFrame.TODAY, now, tz)
        assertEquals(24, bounds.size)
        assertEquals(23L * 3_600_000L, bounds.last().second - bounds.first().first)
        val week = StatsCalculator.bucketBounds(StatsFrame.WEEK, now, tz)
        assertEquals(23L * 3_600_000L, week.last().second - week.last().first)
    }

    @Test
    fun `good rate and y ladder`() {
        assertNull(StatsCalculator.goodRate(0, 0))
        assertEquals(71, StatsCalculator.goodRate(5, 2))
        assertEquals(50, StatsCalculator.goodRate(1, 1))
        assertEquals(2, StatsCalculator.yMax(0))
        assertEquals(10, StatsCalculator.yMax(9))
        assertEquals(16, StatsCalculator.yMax(13))
        assertEquals(200, StatsCalculator.yMax(200))
        assertEquals(250, StatsCalculator.yMax(201))
    }
}
