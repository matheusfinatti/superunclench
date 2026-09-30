package com.mfinatti.noclenchingsrs.feature.stats.domain

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

/** Stats time frames (US-08 §3). */
enum class StatsFrame(val bucketCount: Int, val hourly: Boolean) {
    /** Local midnight to midnight, 24 hourly buckets. */
    TODAY(bucketCount = 24, hourly = true),

    /** Today + previous 6 calendar days, oldest first. */
    WEEK(bucketCount = 7, hourly = false),

    /** Today + previous 29 calendar days, oldest first. */
    MONTH(bucketCount = 30, hourly = false),
}

/** One chart slot: [startMillis, endMillis) in local time. */
data class StatsBucket(
    val startMillis: Long,
    val endMillis: Long,
    val good: Int,
    val bad: Int,
    val missed: Int,
) {
    val answered: Int get() = good + bad
}

data class FrameStats(
    val frame: StatsFrame,
    val buckets: List<StatsBucket>,
    val good: Int,
    val bad: Int,
    val missed: Int,
    /** round(100 × Good / (Good + Bad)); null when nothing was answered ("—"). */
    val goodRatePercent: Int?,
    /** Y-axis maximum from the fixed ladder (US-08 §4.2). */
    val yMax: Int,
)

/**
 * Pure, time-zone-aware aggregation of the check-in history. All calendar arithmetic goes through
 * [Calendar] with an explicit [TimeZone], so buckets follow local days/hours (DST safe) and tests
 * can pin the zone.
 */
object StatsCalculator {

    private val Y_LADDER = listOf(2, 4, 6, 8, 10, 12, 16, 20, 30, 40, 50, 60, 80, 100, 150, 200)

    fun compute(events: List<CheckInEvent>, frame: StatsFrame, nowMillis: Long, timeZone: TimeZone): FrameStats {
        val buckets = bucketBounds(frame, nowMillis, timeZone).map { (start, end) ->
            var good = 0
            var bad = 0
            var missed = 0
            for (e in events) {
                if (e.atMillis < start || e.atMillis >= end) continue
                when (e.outcome) {
                    CheckInOutcome.GOOD -> good++
                    CheckInOutcome.BAD -> bad++
                    CheckInOutcome.MISSED -> missed++
                }
            }
            StatsBucket(start, end, good, bad, missed)
        }
        val good = buckets.sumOf { it.good }
        val bad = buckets.sumOf { it.bad }
        return FrameStats(
            frame = frame,
            buckets = buckets,
            good = good,
            bad = bad,
            missed = buckets.sumOf { it.missed },
            goodRatePercent = goodRate(good, bad),
            yMax = yMax(buckets.maxOfOrNull { it.answered } ?: 0),
        )
    }

    fun goodRate(good: Int, bad: Int): Int? {
        val answered = good + bad
        if (answered == 0) return null
        return (100.0 * good / answered).roundToInt()
    }

    fun yMax(maxStack: Int): Int = Y_LADDER.firstOrNull { it >= maxStack } ?: (((maxStack + 49) / 50) * 50)

    /**
     * Consecutive calendar days ending today with ≥ 1 answer and a Good rate ≥ 50%. If today has
     * no answers yet it is "pending", so counting starts at yesterday (Decisions log). If today has
     * answers but a Good rate < 50%, the streak is 0.
     */
    fun streak(events: List<CheckInEvent>, nowMillis: Long, timeZone: TimeZone): Int {
        if (events.isEmpty()) return 0
        val earliest = events.minOf { it.atMillis }
        val cal = startOfDay(nowMillis, timeZone)
        var dayEnd = nextDay(cal.timeInMillis, timeZone)
        var first = true
        var count = 0
        while (cal.timeInMillis + DAY_SLACK_MILLIS >= earliest || first) {
            val start = cal.timeInMillis
            var good = 0
            var bad = 0
            for (e in events) {
                if (e.atMillis < start || e.atMillis >= dayEnd) continue
                when (e.outcome) {
                    CheckInOutcome.GOOD -> good++
                    CheckInOutcome.BAD -> bad++
                    CheckInOutcome.MISSED -> Unit
                }
            }
            val answered = good + bad
            if (first && answered == 0) {
                // Today pending: doesn't count and doesn't break.
            } else if (answered > 0 && good * 2 >= answered) {
                count++
            } else {
                break
            }
            first = false
            dayEnd = start
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return count
    }

    /** [start, end) pairs for the frame's buckets, oldest first. */
    fun bucketBounds(frame: StatsFrame, nowMillis: Long, timeZone: TimeZone): List<Pair<Long, Long>> {
        val todayStart = startOfDay(nowMillis, timeZone).timeInMillis
        return if (frame.hourly) {
            val tomorrow = nextDay(todayStart, timeZone)
            (0 until 24).map { hour ->
                val start = atHour(todayStart, hour, timeZone)
                val end = if (hour == 23) tomorrow else atHour(todayStart, hour + 1, timeZone)
                start to maxOf(start, end)
            }
        } else {
            (frame.bucketCount - 1 downTo 0).map { daysAgo ->
                val cal = startOfDay(nowMillis, timeZone)
                cal.add(Calendar.DAY_OF_MONTH, -daysAgo)
                val start = cal.timeInMillis
                start to nextDay(start, timeZone)
            }
        }
    }

    fun startOfDay(millis: Long, timeZone: TimeZone): Calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun nextDay(dayStartMillis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).run {
        timeInMillis = dayStartMillis
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun atHour(dayStartMillis: Long, hour: Int, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).run {
        timeInMillis = dayStartMillis
        set(Calendar.HOUR_OF_DAY, hour)
        timeInMillis
    }

    /** Keeps scanning one day past the earliest event so a streak can include that first day. */
    private const val DAY_SLACK_MILLIS = 26L * 60 * 60 * 1000
}
