package com.mfinatti.noclenchingsrs.feature.stats.domain

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import java.util.Calendar
import java.util.TimeZone

/**
 * The deterministic 30-day sample history from docs/design/US-08-stats.md §9 (debug "Seed sample
 * history"). Expected: Today 5/2/1 (71%), 7 days 45/12/1 (79%), 30 days 188/98/23 (66%), streak 4,
 * 309 events.
 */
object SampleHistory {

    private const val G = 'G'

    private val PATTERN_6_4 = "GBGBGBGBGG"
    private val PATTERN_7_3 = "GBGGBGGBGG"
    private val PATTERN_8_2 = "GBGGGBGGGG"
    private val PATTERN_DAY_10 = "BBBGBBGBG"

    /** Days ago → (answer pattern, missed count). Day 4 is a rest day; today is explicit. */
    private fun dayPlan(daysAgo: Int): Pair<String, Int>? = when (daysAgo) {
        in 15..29 -> PATTERN_6_4 to 1
        in 11..14, 8, 9 -> PATTERN_7_3 to 1
        10 -> PATTERN_DAY_10 to 1
        1, 2, 3, 5, 6, 7 -> PATTERN_8_2 to 0
        else -> null
    }

    /** Today's events as (hour, minute, outcome). */
    private val TODAY = listOf(
        Triple(8, 5, CheckInOutcome.BAD),
        Triple(9, 10, CheckInOutcome.GOOD),
        Triple(10, 15, CheckInOutcome.GOOD),
        Triple(11, 40, CheckInOutcome.MISSED),
        Triple(12, 20, CheckInOutcome.BAD),
        Triple(14, 25, CheckInOutcome.GOOD),
        Triple(15, 30, CheckInOutcome.GOOD),
        Triple(17, 35, CheckInOutcome.GOOD),
    )

    fun generate(nowMillis: Long, timeZone: TimeZone, srsEngine: SrsEngine = SrsEngine()): List<CheckInEvent> {
        val raw = mutableListOf<Pair<Long, CheckInOutcome>>()
        for (daysAgo in 29 downTo 1) {
            val (pattern, missed) = dayPlan(daysAgo) ?: continue
            val outcomes = pattern.map { if (it == G) CheckInOutcome.GOOD else CheckInOutcome.BAD } +
                List(missed) { CheckInOutcome.MISSED }
            outcomes.forEachIndexed { k, outcome ->
                raw += at(nowMillis, timeZone, daysAgo, 8 * 60 + 45 * k) to outcome
            }
        }
        raw += todayEvents(nowMillis, timeZone)

        // Replay the SRS engine from L1/0 so each event carries a realistic level.
        var srs = SrsState()
        return raw.sortedBy { it.first }.map { (time, outcome) ->
            val before = srs
            if (outcome != CheckInOutcome.MISSED) {
                srs = srsEngine.apply(srs, if (outcome == CheckInOutcome.GOOD) Answer.GOOD else Answer.BAD).state
            }
            CheckInEvent(
                atMillis = time,
                outcome = outcome,
                level = before.level,
                subLevel = before.subLevel,
                source = CheckInSource.SEED,
                levelAfter = srs.level,
                subLevelAfter = srs.subLevel,
            )
        }
    }

    /**
     * Today's events at their nominal times, shifted earlier so the last one is at or before now
     * (Decisions log 2026-10-01); compressed into [midnight, now] if a plain shift would cross
     * midnight. Counts never change.
     */
    private fun todayEvents(nowMillis: Long, timeZone: TimeZone): List<Pair<Long, CheckInOutcome>> {
        val nominal = TODAY.map { (h, m, outcome) -> at(nowMillis, timeZone, 0, h * 60 + m) to outcome }
        val first = nominal.first().first
        val last = nominal.last().first
        if (last <= nowMillis) return nominal
        val dayStart = StatsCalculator.startOfDay(nowMillis, timeZone).timeInMillis
        val shift = nowMillis - last
        if (first + shift >= dayStart) return nominal.map { (t, o) -> (t + shift) to o }
        val span = (last - first).toDouble()
        val room = (nowMillis - dayStart).toDouble()
        return nominal.map { (t, o) -> (dayStart + ((t - first) / span * room).toLong()) to o }
    }

    private fun at(nowMillis: Long, timeZone: TimeZone, daysAgo: Int, minuteOfDay: Int): Long =
        StatsCalculator.startOfDay(nowMillis, timeZone).run {
            add(Calendar.DAY_OF_MONTH, -daysAgo)
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            timeInMillis
        }
}
