package com.mfinatti.noclenchingsrs.domain.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The single scaling point for every scheduled duration (SRS intervals now; pause durations and
 * quiet-hour shifts later). Release builds always use [Real]; debug builds default to
 * [MinutesAsSeconds] ("Short intervals" in the QA panel).
 */
fun interface IntervalScale {
    fun scale(interval: Duration): Duration

    companion object {
        val Real: IntervalScale = IntervalScale { it }

        /** 5 min → 5 s, 10 min → 10 s … 3 h → 180 s. */
        val MinutesAsSeconds: IntervalScale = IntervalScale { it.inWholeMinutes.seconds }
    }
}
