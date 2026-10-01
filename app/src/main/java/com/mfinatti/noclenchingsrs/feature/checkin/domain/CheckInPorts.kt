package com.mfinatti.noclenchingsrs.feature.checkin.domain

import com.mfinatti.noclenchingsrs.domain.session.IntervalScale
import com.mfinatti.noclenchingsrs.domain.settings.QuietHoursRule
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Schedules the single pending check-in alarm (AlarmManager in production). */
interface AlarmScheduler {
    /**
     * Replaces any scheduled alarm with one at [atMillis] (epoch millis). Uses an exact alarm when
     * [allowExact] and the platform permits it, otherwise an inexact one. [alarmClock] (US-11 Ring)
     * uses `setAlarmClock`: the device leaves Doze ahead of it and delivery is never deferred.
     */
    fun schedule(atMillis: Long, allowExact: Boolean = true, alarmClock: Boolean = false)

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

    /**
     * US-11 Ring: starts ringing for the check-in fired at [checkInAtMillis] (alarm-stream sound +
     * vibration, ringing heads-up, full-screen alarm screen when [fullScreen] and the phone is
     * locked). Also used to restart a ring after process death. [silenced] posts the notification
     * without sound.
     */
    fun showRing(checkInAtMillis: Long, level: Int, capAtMillis: Long, fullScreen: Boolean, silenced: Boolean = false)

    /** US-11: stop sound and vibration but keep the alarm screen / notification. */
    fun silenceRing()

    /** Removes the check-in notification and stops any ring (and closes the alarm screen). */
    fun cancel()
}

/** Per-operation timing/presentation policy; debug switches feed it, release uses the defaults. */
data class SchedulingPolicy(
    val scale: IntervalScale = IntervalScale.Real,
    val allowExact: Boolean = true,
    val customNotificationLayout: Boolean = true,
    /** Debug "Auto-pause: OFF" disables pausing after 3 consecutive misses (release: always on). */
    val autoPause: Boolean = true,
    /** Quiet hours (US-10): from Settings, plus the debug "Simulate quiet hours now" switch. */
    val quiet: QuietHoursRule = QuietHoursRule.Off,
    /** US-11 alert style for the next check-in (Settings). */
    val alertStyle: AlertStyle = AlertStyle.NUDGE,
    /** US-11 auto-silence cap: 10 min, debug "Short ring cap" 15 s; never scaled by short intervals. */
    val ringCap: Duration = DEFAULT_RING_CAP,
    /** US-11: full-screen intents allowed (Android 14+ permission; debug preview can force false). */
    val fullScreenAllowed: Boolean = true,
) {
    companion object {
        val DEFAULT_RING_CAP: Duration = 10.minutes
        val SHORT_RING_CAP: Duration = 15.seconds
    }
}
