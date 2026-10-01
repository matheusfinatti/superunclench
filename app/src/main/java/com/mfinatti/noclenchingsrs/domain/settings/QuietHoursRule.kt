package com.mfinatti.noclenchingsrs.domain.settings

import java.util.Calendar
import java.util.TimeZone

/**
 * Quiet-hours rule (US-10). Clock times are local wall-clock times and are **not** scaled by debug
 * short intervals. Windows may cross midnight (22:00–07:00). All calendar maths goes through
 * [Calendar] with an explicit [TimeZone], so DST days are handled by the platform.
 *
 * @property simulate debug "Simulate quiet hours now": treat the period from now until the next
 * end time as quiet, whatever the clock says.
 */
data class QuietHoursRule(
    val enabled: Boolean,
    val startMinutes: Int,
    val endMinutes: Int,
    val simulate: Boolean = false,
    val timeZone: TimeZone = TimeZone.getDefault(),
) {
    /** True if [timeMillis] falls inside the configured window (ignores [enabled] and [simulate]). */
    fun isInsideWindow(timeMillis: Long): Boolean {
        val minute = minuteOfDay(timeMillis)
        return if (startMinutes <= endMinutes) {
            minute in startMinutes until endMinutes
        } else {
            minute >= startMinutes || minute < endMinutes
        }
    }

    /**
     * True when [nextAlarmMillis] sits exactly on an end time of this window that it was deferred
     * to (the minute before it is quiet), i.e. the alarm is a quiet-hours deferral, not a real due time.
     */
    fun isDeferredAlarm(nextAlarmMillis: Long, nowMillis: Long): Boolean {
        if (!enabled) return false
        val before = nextAlarmMillis - 1
        val quietBefore = isInsideWindow(before) || (simulate && before >= nowMillis)
        return quietBefore && endAfter(before) == nextAlarmMillis
    }

    /** Quiet hours are in effect at [nowMillis]. */
    fun isActive(nowMillis: Long): Boolean = enabled && (simulate || isInsideWindow(nowMillis))

    /** The first occurrence of the end clock time strictly after [timeMillis]. */
    fun endAfter(timeMillis: Long): Long = Calendar.getInstance(timeZone).run {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        set(Calendar.MINUTE, endMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= timeMillis) add(Calendar.DAY_OF_MONTH, 1)
        timeInMillis
    }

    /**
     * Where an alarm due at [dueMillis] should really fire, evaluated at [nowMillis]: unchanged
     * outside quiet hours, otherwise deferred to the end of the quiet period it falls in.
     */
    fun adjust(dueMillis: Long, nowMillis: Long): Long {
        if (!enabled) return dueMillis
        if (simulate) {
            val simulatedEnd = endAfter(nowMillis)
            if (dueMillis < simulatedEnd) return simulatedEnd
        }
        return if (isInsideWindow(dueMillis)) endAfter(dueMillis) else dueMillis
    }

    private fun minuteOfDay(timeMillis: Long): Int = Calendar.getInstance(timeZone).run {
        timeInMillis = timeMillis
        get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
    }

    companion object {
        fun from(settings: UserSettings, simulate: Boolean, timeZone: TimeZone = TimeZone.getDefault()) =
            QuietHoursRule(
                enabled = settings.quietHoursEnabled,
                startMinutes = settings.quietStartMinutes,
                endMinutes = settings.quietEndMinutes,
                simulate = simulate,
                timeZone = timeZone,
            )

        /** No quiet hours (tests, legacy callers). */
        val Off = QuietHoursRule(enabled = false, startMinutes = 0, endMinutes = 0)
    }
}
