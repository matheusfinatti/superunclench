package com.mfinatti.noclenchingsrs.feature.checkin.domain

import com.mfinatti.noclenchingsrs.domain.session.IntervalScale
import kotlin.time.Duration

/** Schedules the single pending check-in alarm (AlarmManager in production). */
interface AlarmScheduler {
    /**
     * Replaces any scheduled alarm with one at [atMillis] (epoch millis). Uses an exact alarm when
     * [allowExact] and the platform permits it, otherwise an inexact one.
     */
    fun schedule(atMillis: Long, allowExact: Boolean = true)

    fun cancel()
}

/** Shows / removes the single check-in notification. */
interface CheckInNotifier {
    /**
     * Posts (or replaces) the check-in notification.
     *
     * @param checkInAtMillis fire time; also the token that answers carry so stale ones are ignored.
     * @param customLayout green/red pill buttons (US-03 Option B) instead of standard actions.
     */
    fun show(checkInAtMillis: Long, level: Int, interval: Duration, customLayout: Boolean = true)

    fun cancel()
}

/** Per-operation timing/presentation policy; debug switches feed it, release uses the defaults. */
data class SchedulingPolicy(
    val scale: IntervalScale = IntervalScale.Real,
    val allowExact: Boolean = true,
    val customNotificationLayout: Boolean = true,
    /** Debug "Auto-pause: OFF" disables pausing after 3 consecutive misses (release: always on). */
    val autoPause: Boolean = true,
)
