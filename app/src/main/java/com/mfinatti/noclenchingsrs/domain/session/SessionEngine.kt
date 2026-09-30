package com.mfinatti.noclenchingsrs.domain.session

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.domain.srs.SrsState

enum class SessionStatus { STOPPED, RUNNING, PAUSED }

/**
 * Everything about the check-in session that must survive process death.
 *
 * @property nextAlarmAtMillis epoch millis of the scheduled alarm, or null when none is scheduled.
 * @property pendingCheckInAtMillis epoch millis at which the currently unanswered check-in fired, or null.
 * @property pausedUntilMillis end of a timed pause; null while paused means "until I resume".
 * @property autoPaused true when the pause was caused by consecutive misses (US-07).
 */
data class SessionState(
    val status: SessionStatus = SessionStatus.STOPPED,
    val srs: SrsState = SrsState(),
    val nextAlarmAtMillis: Long? = null,
    val pendingCheckInAtMillis: Long? = null,
    val consecutiveMisses: Int = 0,
    val pausedUntilMillis: Long? = null,
    val autoPaused: Boolean = false,
    val lastAnswer: LastAnswer? = null,
    /** "Not now" on the exact-timing banner; reset on every Start (US-05 §2). */
    val exactTimingBannerDismissed: Boolean = false,
)

/**
 * The most recent Good/Bad answer and what it did, so Home can show level feedback for answers
 * made from the notification just before the app was opened (US-04 §4).
 */
data class LastAnswer(
    val atMillis: Long,
    val answer: Answer,
    val change: LevelChange,
)

/** Result of a session transition: the new state plus an optional history event to record. */
data class SessionTransition(
    val state: SessionState,
    val event: CheckInEvent? = null,
    val levelChange: LevelChange? = null,
)

/**
 * Pure session rules on top of [SrsEngine] (Start/Pause/Resume/Stop, answers, misses, auto-pause).
 * Scheduling side effects (AlarmManager, notifications) are performed by callers based on the
 * returned [SessionState.nextAlarmAtMillis].
 */
class SessionEngine(
    val srs: SrsEngine = SrsEngine(),
    val autoPauseAfterMisses: Int = DEFAULT_AUTO_PAUSE_AFTER_MISSES,
) {

    /** Begins alarms; first alarm at the current level's interval from [nowMillis]. */
    fun start(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
    ): SessionState = state.copy(
        status = SessionStatus.RUNNING,
        srs = srs.normalize(state.srs),
        nextAlarmAtMillis = nextAlarmAt(nowMillis, state.srs.level, scale),
        pendingCheckInAtMillis = null,
        consecutiveMisses = 0,
        pausedUntilMillis = null,
        autoPaused = false,
        exactTimingBannerDismissed = false,
    )

    /** Epoch millis of the next alarm scheduled from [fromMillis] at [level], scaled by [scale]. */
    fun nextAlarmAt(fromMillis: Long, level: Int, scale: IntervalScale = IntervalScale.Real): Long =
        fromMillis + scale.scale(srs.interval(level)).inWholeMilliseconds

    /**
     * Boot / process-restart path (US-05 §3): a running session whose alarm time has passed (or is
     * missing) gets its next alarm at now + current interval. No Missed is recorded.
     */
    fun restoreAfterBoot(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
    ): SessionState {
        if (state.status == SessionStatus.PAUSED) return onPauseEnded(state, nowMillis, scale)
        if (state.status != SessionStatus.RUNNING) return state
        val next = state.nextAlarmAtMillis
        if (next != null && next > nowMillis) return state
        return state.copy(nextAlarmAtMillis = nextAlarmAt(nowMillis, state.srs.level, scale))
    }

    /**
     * A timed pause reached its end time (alarm, boot or process start): resume, with the next
     * alarm at now + current interval. Untimed pauses and pauses still in the future are unchanged.
     */
    fun onPauseEnded(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
    ): SessionState {
        val until = state.pausedUntilMillis ?: return state
        if (state.status != SessionStatus.PAUSED || until > nowMillis) return state
        return resume(state, nowMillis, scale)
    }

    /**
     * True when an alarm delivered at [nowMillis] is not the currently scheduled one: an answer or
     * reschedule already moved the next alarm into the future (the old alarm's broadcast was in
     * flight). Such a delivery must not post a check-in (US-05 O3).
     */
    fun isStaleAlarm(state: SessionState, nowMillis: Long): Boolean {
        val next = state.nextAlarmAtMillis ?: return false
        return nowMillis + ALARM_EARLY_TOLERANCE_MILLIS < next
    }

    /** Re-times a running session's next alarm from now (e.g. after Short intervals is toggled). */
    fun rescheduleFromNow(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
    ): SessionState {
        if (state.status != SessionStatus.RUNNING) return state
        return state.copy(nextAlarmAtMillis = nextAlarmAt(nowMillis, state.srs.level, scale))
    }

    fun dismissExactTimingBanner(state: SessionState): SessionState =
        state.copy(exactTimingBannerDismissed = true)

    /** Ends the session; SRS position is kept. */
    fun stop(state: SessionState): SessionState = state.copy(
        status = SessionStatus.STOPPED,
        nextAlarmAtMillis = null,
        pendingCheckInAtMillis = null,
        consecutiveMisses = 0,
        pausedUntilMillis = null,
        autoPaused = false,
    )

    /** Pauses a running session. [untilMillis] null = "Until I resume". */
    fun pause(state: SessionState, untilMillis: Long?): SessionState {
        if (state.status != SessionStatus.RUNNING) return state
        return state.copy(
            status = SessionStatus.PAUSED,
            nextAlarmAtMillis = null,
            pausedUntilMillis = untilMillis,
            autoPaused = false,
        )
    }

    /** Resumes a paused session; next alarm = now + current interval. */
    fun resume(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
    ): SessionState {
        if (state.status != SessionStatus.PAUSED) return state
        return state.copy(
            status = SessionStatus.RUNNING,
            nextAlarmAtMillis = nextAlarmAt(nowMillis, state.srs.level, scale),
            pausedUntilMillis = null,
            consecutiveMisses = 0,
            autoPaused = false,
        )
    }

    /**
     * An alarm fired at [nowMillis]. If a previous check-in is still pending it is recorded as
     * Missed first (which may auto-pause the session, in which case no new check-in is posted).
     */
    fun onAlarmFired(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
        autoPause: Boolean = true,
    ): SessionTransition {
        if (state.status != SessionStatus.RUNNING) return SessionTransition(state)
        var missEvent: CheckInEvent? = null
        var current = state
        if (state.pendingCheckInAtMillis != null) {
            val missed = onMissed(state, nowMillis, scale, CheckInSource.ALARM, autoPause)
            missEvent = missed.event
            current = missed.state
            if (current.status != SessionStatus.RUNNING) return missed
        }
        return SessionTransition(
            state = current.copy(
                pendingCheckInAtMillis = nowMillis,
                nextAlarmAtMillis = nextAlarmAt(nowMillis, current.srs.level, scale),
            ),
            event = missEvent,
        )
    }

    /**
     * A Good/Bad answer at [nowMillis]. Applies the SRS rule, clears the pending check-in, resets
     * the consecutive-miss counter and, if running, schedules the next alarm from the answer time
     * using the interval of the resulting level.
     */
    fun onAnswer(
        state: SessionState,
        answer: Answer,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
        source: CheckInSource = CheckInSource.UNKNOWN,
    ): SessionTransition {
        val before = srs.normalize(state.srs)
        val result = srs.apply(before, answer)
        val event = CheckInEvent(
            atMillis = nowMillis,
            outcome = if (answer == Answer.GOOD) CheckInOutcome.GOOD else CheckInOutcome.BAD,
            level = before.level,
            subLevel = before.subLevel,
            source = source,
            levelAfter = result.state.level,
            subLevelAfter = result.state.subLevel,
        )
        val next = if (state.status == SessionStatus.RUNNING) {
            nextAlarmAt(nowMillis, result.state.level, scale)
        } else {
            state.nextAlarmAtMillis
        }
        return SessionTransition(
            state = state.copy(
                srs = result.state,
                pendingCheckInAtMillis = null,
                consecutiveMisses = 0,
                nextAlarmAtMillis = next,
                lastAnswer = LastAnswer(atMillis = nowMillis, answer = answer, change = result.change),
            ),
            event = event,
            levelChange = result.change,
        )
    }

    /**
     * A check-in was missed (dismissed, or replaced by a newer alarm) at [nowMillis]. Neutral for
     * the SRS position. After [autoPauseAfterMisses] consecutive misses a running session
     * auto-pauses; otherwise the next alarm is scheduled at the current interval from the miss.
     */
    fun onMissed(
        state: SessionState,
        nowMillis: Long,
        scale: IntervalScale = IntervalScale.Real,
        source: CheckInSource = CheckInSource.ALARM,
        autoPause: Boolean = true,
    ): SessionTransition {
        val current = srs.normalize(state.srs)
        val event = CheckInEvent(
            atMillis = nowMillis,
            outcome = CheckInOutcome.MISSED,
            level = current.level,
            subLevel = current.subLevel,
            source = source,
        )
        val misses = state.consecutiveMisses + 1
        val base = state.copy(pendingCheckInAtMillis = null, consecutiveMisses = misses)
        val newState = when {
            state.status != SessionStatus.RUNNING -> base
            autoPause && misses >= autoPauseAfterMisses -> base.copy(
                status = SessionStatus.PAUSED,
                nextAlarmAtMillis = null,
                pausedUntilMillis = null,
                autoPaused = true,
            )

            else -> base.copy(nextAlarmAtMillis = nextAlarmAt(nowMillis, current.level, scale))
        }
        return SessionTransition(state = newState, event = event, levelChange = LevelChange.UNCHANGED)
    }

    /** Settings "Reset progress": back to L1/0, everything else kept. */
    fun resetProgress(state: SessionState): SessionState = state.copy(srs = SrsState(), lastAnswer = null)

    /** Debug: jump to a level (sub-level 0). */
    fun setLevel(state: SessionState, level: Int): SessionState =
        state.copy(srs = srs.setLevel(level), lastAnswer = null)

    /** Debug: fill one sub-level without promoting. */
    fun incrementSubLevel(state: SessionState): SessionState =
        state.copy(srs = srs.incrementSubLevel(state.srs), lastAnswer = null)

    companion object {
        const val DEFAULT_AUTO_PAUSE_AFTER_MISSES: Int = 3

        /** Alarms are never delivered early; this only absorbs clock jitter. */
        const val ALARM_EARLY_TOLERANCE_MILLIS: Long = 1_000L
    }
}
