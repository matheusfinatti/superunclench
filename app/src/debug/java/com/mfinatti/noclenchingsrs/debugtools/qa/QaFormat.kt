package com.mfinatti.noclenchingsrs.debugtools.qa

import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

/** Plain-text formatting for the QA state readout (debug only, English only). */
internal object QaFormat {
    const val NONE = "—"

    fun session(state: SessionState): String = when (state.status) {
        SessionStatus.RUNNING -> "Running"
        SessionStatus.PAUSED -> if (state.autoPaused) "Paused (auto)" else "Paused"
        SessionStatus.STOPPED -> "Stopped"
    }

    fun level(level: Int, subLevel: Int, subLevelCount: Int, name: String): String =
        "$level · sub $subLevel/$subLevelCount ($name)"

    fun interval(duration: Duration): String {
        val minutes = duration.inWholeMinutes
        return when {
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0L -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }

    fun clockTime(epochMillis: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))

    fun shortTime(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

    /** mm:ss below one hour, h:mm:ss above. */
    fun countdown(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun nextAlarm(nextAlarmAtMillis: Long?, nowMillis: Long): String =
        if (nextAlarmAtMillis == null) {
            NONE
        } else {
            "${clockTime(nextAlarmAtMillis)} (in ${countdown(nextAlarmAtMillis - nowMillis)})"
        }

    fun pending(pendingAtMillis: Long?): String =
        if (pendingAtMillis == null) "no" else "yes (fired ${clockTime(pendingAtMillis)})"

    fun misses(count: Int): String = "$count consecutive"

    fun pause(state: SessionState): String = when {
        state.status != SessionStatus.PAUSED -> NONE
        state.pausedUntilMillis != null -> "until ${shortTime(state.pausedUntilMillis)}"
        else -> "until resumed"
    }

    /** "ON 23:30–06:15 · sim: OFF" (24 h, minute precision). */
    fun quietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int, simulated: Boolean = false): String {
        val range = quietRange(startMinutes, endMinutes)
        return "${if (enabled) "ON" else "OFF"} $range · sim: ${if (simulated) "ON" else "OFF"}"
    }

    private fun letter(outcome: CheckInOutcome): String = when (outcome) {
        CheckInOutcome.GOOD -> "G"
        CheckInOutcome.BAD -> "B"
        CheckInOutcome.MISSED -> "M"
    }

    /** "G B M M M", newest last. */
    fun lastFive(events: List<CheckInEvent>): String =
        events.takeLast(5).joinToString(" ") { letter(it.outcome) }.ifEmpty { NONE }

    private fun source(source: CheckInSource): String = when (source) {
        CheckInSource.NOTIFICATION -> "notif"
        CheckInSource.CARD -> "card"
        CheckInSource.PANEL -> "panel"
        CheckInSource.ALARM -> "replaced"
        CheckInSource.DISMISSED -> "dismissed"
        CheckInSource.ALARM_SCREEN -> "alarm"
        CheckInSource.TIMEOUT -> "timeout"
        CheckInSource.SEED -> "seed"
        CheckInSource.UNKNOWN -> "?"
    }

    /** One line per event, newest first: "14:32:05 G notif    L2 2/3 → L3 0/4". */
    fun lastEvents(events: List<CheckInEvent>): String {
        if (events.isEmpty()) return NONE
        return events.asReversed().joinToString("\n") { e ->
            val from = "L${e.level} ${e.subLevel}"
            val to = "L${e.levelAfter} ${e.subLevelAfter}"
            "${clockTime(e.atMillis)} ${letter(e.outcome)} ${source(e.source).padEnd(9)} $from → $to"
        }
    }

    /** "Ring · FSI: granted · ringing: yes (cap 10:52:07)" (US-11 §11). */
    fun alert(style: AlertStyle, fullScreenAllowed: Boolean, session: SessionState): String {
        val name = if (style == AlertStyle.RING) "Ring" else "Nudge"
        val fsi = if (fullScreenAllowed) "granted" else "denied"
        val cap = session.ringCapAtMillis
        val ringing = when {
            !session.ringActive || cap == null -> "no"
            session.ringSilenced -> "silenced (cap ${clockTime(cap)})"
            else -> "yes (cap ${clockTime(cap)})"
        }
        return "$name · FSI: $fsi · ringing: $ringing"
    }

    fun quietRange(startMinutes: Int, endMinutes: Int): String = "${hhmm(startMinutes)}–${hhmm(endMinutes)}"

    private fun hhmm(minutesOfDay: Int): String =
        String.format(Locale.US, "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)
}
