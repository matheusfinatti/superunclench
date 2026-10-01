package com.mfinatti.noclenchingsrs.feature.checkin.domain

import com.mfinatti.noclenchingsrs.core.time.AppClock
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.data.session.SessionRepository
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.SessionEngine
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.session.SessionTransition
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface FireResult {
    /** No running session: nothing was posted. */
    data object NotRunning : FireResult

    /** A check-in was posted, possibly after recording the previous one as Missed. */
    data class Fired(val previousMissed: Boolean) : FireResult

    /** The previous check-in was the 3rd consecutive miss: the session auto-paused, nothing posted. */
    data object AutoPaused : FireResult

    /** Quiet hours are active: nothing posted, nothing missed; next alarm moved to [untilMillis]. */
    data class QuietDeferred(val untilMillis: Long) : FireResult

    /** US-11: a Ring check-in hit its auto-silence cap unanswered and was recorded as Missed. */
    data class RingTimedOut(val autoPaused: Boolean) : FireResult

    /** The alarm marked the end of a timed pause: the session is running again. */
    data object PauseEnded : FireResult

    /**
     * A late delivery of an alarm that an answer/reschedule already replaced (US-05 O3): ignored,
     * so it can neither post a check-in nor cause a miss.
     */
    data object Stale : FireResult
}

sealed interface MissResult {
    /** Recorded as Missed; [autoPaused] when this was the 3rd in a row (session paused). */
    data class Missed(val consecutiveMisses: Int, val autoPaused: Boolean) : MissResult

    /** Nothing pending (or the dismissed notification was already replaced/answered): ignored. */
    data object NoPending : MissResult
}

sealed interface AnswerResult {
    data class Applied(val srs: SrsState, val change: LevelChange) : AnswerResult

    /** The answer belonged to a check-in that has since been replaced or answered; ignored. */
    data object Stale : AnswerResult
}

/**
 * Single entry point for everything that changes the session: UI buttons, notification actions,
 * alarm/boot broadcasts and the QA panel. Serialises operations with a mutex, persists state,
 * records history and performs the side effects (alarm + notification).
 *
 * @param policy read at the start of every operation (debug short intervals, exact-alarm preview,
 * notification layout); release builds always get the defaults.
 */
class CheckInController(
    private val sessionRepository: SessionRepository,
    private val checkInLog: CheckInLogRepository,
    private val sessionEngine: SessionEngine,
    private val scheduler: AlarmScheduler,
    private val notifier: CheckInNotifier,
    private val clock: AppClock,
    private val policy: suspend () -> SchedulingPolicy = { SchedulingPolicy() },
) {
    private val mutex = Mutex()

    /** Start (or restart) a session: first alarm at the current level's interval. */
    suspend fun start(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { sessionEngine.start(it, now, p.scale).quiet(p, now) }
        notifier.cancel()
        applyAlarm(state, p)
        state
    }

    /**
     * Pause a running session for [duration] (scaled by the policy: 15 min = 15 s in debug short
     * mode), or until resumed when null. The next check-in alarm is cancelled; a timed pause
     * schedules its own end. A pending check-in stays answerable.
     */
    suspend fun pause(duration: kotlin.time.Duration?): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val until = duration?.let { now + p.scale.scale(it).inWholeMilliseconds }
        var wasRing = false
        val state = sessionRepository.update { current ->
            wasRing = current.ringActive
            sessionEngine.pause(current, until)
        }
        // US-11 AC8: pausing ends a ring (nothing recorded); a Nudge check-in stays answerable.
        if (wasRing) notifier.cancel()
        applyAlarm(state, p)
        state
    }

    /** Resume a paused session: next alarm = now + current interval. */
    suspend fun resume(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { sessionEngine.resume(it, now, p.scale).quiet(p, now) }
        applyAlarm(state, p)
        state
    }

    /** Stop: cancel alarm and notification; level and history are kept. */
    suspend fun stop(): SessionState = mutex.withLock {
        val state = sessionRepository.update { sessionEngine.stop(it) }
        scheduler.cancel()
        notifier.cancel()
        state
    }

    /**
     * An alarm fired (scheduled), or QA "Fire alarm now" ([force] = true, which skips the
     * stale-delivery check).
     */
    suspend fun fireAlarm(force: Boolean = false): FireResult = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        var transition: SessionTransition? = null
        var outcome: FireResult? = null
        val state = sessionRepository.update { current ->
            when {
                current.status == SessionStatus.PAUSED -> {
                    val resumed = sessionEngine.onPauseEnded(current, now, p.scale).quiet(p, now)
                    outcome = if (resumed.status == SessionStatus.RUNNING) FireResult.PauseEnded else FireResult.NotRunning
                    resumed
                }
                current.status != SessionStatus.RUNNING -> {
                    outcome = FireResult.NotRunning
                    current
                }
                current.ringActive -> {
                    // US-11: while a Ring check-in is unresolved the only alarm is its cap. A
                    // forced fire (QA) never replaces a ring either.
                    val cap = current.ringCapAtMillis ?: now
                    if (now + SessionEngine.ALARM_EARLY_TOLERANCE_MILLIS >= cap) {
                        val t = sessionEngine.onMissed(current, now, p.scale, CheckInSource.TIMEOUT, p.autoPause)
                        transition = t
                        outcome = FireResult.RingTimedOut(autoPaused = t.state.status == SessionStatus.PAUSED)
                        t.state.quiet(p, now)
                    } else {
                        outcome = FireResult.Stale
                        current
                    }
                }
                !force && sessionEngine.isStaleAlarm(current, now) -> {
                    outcome = FireResult.Stale
                    current
                }
                p.quiet.isActive(now) -> {
                    // Quiet hours (US-10): no notification, no Missed; defer to the end of the window.
                    val until = p.quiet.adjust(now, now)
                    outcome = FireResult.QuietDeferred(until)
                    current.copy(nextAlarmAtMillis = until)
                }
                else -> {
                    val cap = if (p.alertStyle == AlertStyle.RING) p.ringCap.inWholeMilliseconds else null
                    val t = sessionEngine.onAlarmFired(current, now, p.scale, p.autoPause, ringCapMillis = cap)
                    transition = t
                    t.state.quiet(p, now)
                }
            }
        }
        outcome?.let { result ->
            if (result is FireResult.RingTimedOut) {
                transition?.event?.let { checkInLog.append(it) }
                notifier.cancel()
                applyAlarm(state, p)
                return@withLock result
            }
            if (result == FireResult.PauseEnded || result == FireResult.Stale || result is FireResult.QuietDeferred) {
                applyAlarm(state, p)
            }
            return@withLock result
        }
        val missedEvent = transition?.event
        if (missedEvent != null) checkInLog.append(missedEvent)
        val pendingAt = state.pendingCheckInAtMillis
        val ringCap = state.ringCapAtMillis
        if (state.status == SessionStatus.RUNNING && pendingAt != null && ringCap != null) {
            notifier.showRing(
                checkInAtMillis = pendingAt,
                level = state.srs.level,
                capAtMillis = ringCap,
                fullScreen = p.fullScreenAllowed,
            )
            applyAlarm(state, p)
            FireResult.Fired(previousMissed = missedEvent != null)
        } else if (state.status == SessionStatus.RUNNING && pendingAt != null) {
            notifier.show(
                checkInAtMillis = pendingAt,
                level = state.srs.level,
                interval = sessionEngine.srs.interval(state.srs.level),
                customLayout = p.customNotificationLayout,
            )
            applyAlarm(state, p)
            FireResult.Fired(previousMissed = missedEvent != null)
        } else {
            scheduler.cancel()
            notifier.cancel()
            FireResult.AutoPaused
        }
    }

    /**
     * Good/Bad answer. [checkInAtMillis] is the fire time carried by the notification / pending
     * card; pass null to apply regardless (debug "Answer Good/Bad", see Decisions log).
     */
    suspend fun answer(
        answer: Answer,
        checkInAtMillis: Long?,
        source: CheckInSource = CheckInSource.PANEL,
    ): AnswerResult = mutex.withLock {
        val p = policy()
        var transition: SessionTransition? = null
        val state = sessionRepository.update { current ->
            if (checkInAtMillis != null && current.pendingCheckInAtMillis != checkInAtMillis) {
                current
            } else {
                val now = clock.nowMillis()
                val t = sessionEngine.onAnswer(current, answer, now, p.scale, source)
                transition = t
                t.state.quiet(p, now)
            }
        }
        val applied = transition ?: return@withLock AnswerResult.Stale
        applied.event?.let { checkInLog.append(it) }
        notifier.cancel()
        applyAlarm(state, p)
        AnswerResult.Applied(srs = state.srs, change = applied.levelChange ?: LevelChange.UNCHANGED)
    }

    /**
     * The pending check-in was dismissed (notification swiped away / cleared, or QA "Mark missed"
     * with [checkInAtMillis] = null). Neutral for the level; may auto-pause after 3 in a row.
     */
    suspend fun markMissed(
        checkInAtMillis: Long?,
        source: CheckInSource = CheckInSource.DISMISSED,
    ): MissResult = mutex.withLock {
        val p = policy()
        var transition: SessionTransition? = null
        val state = sessionRepository.update { current ->
            val pending = current.pendingCheckInAtMillis
            if (pending == null || (checkInAtMillis != null && checkInAtMillis != pending)) {
                current
            } else {
                val now = clock.nowMillis()
                val t = sessionEngine.onMissed(current, now, p.scale, source, p.autoPause)
                transition = t
                t.state.quiet(p, now)
            }
        }
        val missed = transition ?: return@withLock MissResult.NoPending
        missed.event?.let { checkInLog.append(it) }
        notifier.cancel()
        applyAlarm(state, p)
        MissResult.Missed(consecutiveMisses = state.consecutiveMisses, autoPaused = state.autoPaused)
    }

    /**
     * Boot / app-update / process-start / exact-permission-change path (US-05 §3): re-arms the
     * alarm of a running session. If its time passed while the phone was off, the next alarm is at
     * now + current interval; no Missed is recorded and no notification is posted.
     */
    suspend fun restoreSchedule(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { sessionEngine.restoreAfterBoot(it, now, p.scale).quiet(p, now) }
        applyAlarm(state, p)
        // US-11: a ring outlives process death (update, crash, low-memory kill): ring again until
        // answered or capped. A cap that already passed has just been re-armed in the past and
        // fires (timeout) straight away.
        val pendingAt = state.pendingCheckInAtMillis
        val cap = state.ringCapAtMillis
        if (state.ringing && pendingAt != null && cap != null && cap > now) {
            notifier.showRing(pendingAt, state.srs.level, cap, p.fullScreenAllowed)
        }
        state
    }

    /**
     * US-11 Silence (alarm screen button, notification, in-app card, volume/power key): sound and
     * vibration stop, the check-in stays pending until answered or capped. Returns false when
     * nothing was ringing.
     */
    suspend fun silenceRing(): Boolean = mutex.withLock {
        var silenced = false
        sessionRepository.update { current ->
            silenced = current.ringing
            sessionEngine.silenceRing(current)
        }
        if (silenced) notifier.silenceRing()
        silenced
    }

    /** Re-times the next alarm from now (debug Short intervals toggle). */
    suspend fun rescheduleFromNow(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { sessionEngine.rescheduleFromNow(it, now, p.scale).quiet(p, now) }
        applyAlarm(state, p)
        state
    }

    /**
     * Quiet-hours settings changed (switch or range; US-10 AC7). Runs [change] (the save) under the
     * lock, then re-evaluates a running session against the new rule:
     * - an alarm that had been deferred to the old window's end, now outside quiet hours, becomes
     *   now + current interval (as on Resume);
     * - otherwise the scheduled time is kept, unless the new window covers it (then → new end).
     */
    suspend fun quietHoursChanged(change: suspend () -> Unit): SessionState = mutex.withLock {
        val before = policy().quiet
        change()
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { current ->
            val next = current.nextAlarmAtMillis
            if (current.status != SessionStatus.RUNNING || next == null || current.ringActive) {
                current
            } else if (before.isDeferredAlarm(next, now) && !p.quiet.isActive(now)) {
                sessionEngine.rescheduleFromNow(current, now, p.scale).quiet(p, now)
            } else if (before.isDeferredAlarm(next, now)) {
                // Still quiet now: the check-in waits for the (possibly new) end time.
                current.copy(nextAlarmAtMillis = p.quiet.adjust(now, now))
            } else {
                current.quiet(p, now)
            }
        }
        applyAlarm(state, p)
        state
    }

    /**
     * Quiet hours ended early (debug "Simulate quiet hours now: OFF"): like reaching the end time,
     * the next check-in is due right away. Level unchanged.
     */
    suspend fun quietHoursEnded(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { current ->
            if (current.status == SessionStatus.RUNNING) current.copy(nextAlarmAtMillis = now) else current
        }
        applyAlarm(state, p)
        state
    }

    /** Exact-timing banner "Not now": hidden until the next Start. */
    suspend fun dismissExactTimingBanner(): SessionState = mutex.withLock {
        sessionRepository.update { sessionEngine.dismissExactTimingBanner(it) }
    }

    /**
     * Settings "Reset progress": back to L1 0/3, history kept. A running session continues with the
     * L1 interval from now (US-09 §3).
     */
    suspend fun resetProgress(): SessionState = mutex.withLock {
        val p = policy()
        val now = clock.nowMillis()
        val state = sessionRepository.update { current ->
            sessionEngine.rescheduleFromNow(sessionEngine.resetProgress(current), now, p.scale).quiet(p, now)
        }
        applyAlarm(state, p)
        state
    }

    /** Debug: jump to [level] with sub-level 0. The scheduled alarm is kept. */
    suspend fun setLevel(level: Int): SessionState = mutex.withLock {
        sessionRepository.update { sessionEngine.setLevel(it, level) }
    }

    /** Debug: fill one more sub-level without promoting. */
    suspend fun incrementSubLevel(): SessionState = mutex.withLock {
        sessionRepository.update { sessionEngine.incrementSubLevel(it) }
    }

    /** Removes every side effect (used by "Reset all data"). */
    suspend fun cancelAllSideEffects() = mutex.withLock {
        scheduler.cancel()
        notifier.cancel()
    }

    /** Defers a running session's next alarm out of quiet hours (US-10); clock times aren't scaled. */
    private fun SessionState.quiet(policy: SchedulingPolicy, nowMillis: Long): SessionState {
        val next = nextAlarmAtMillis ?: return this
        if (status != SessionStatus.RUNNING) return this
        // A ringing check-in's cap is not a check-in: quiet hours never move it.
        if (ringActive) return this
        val adjusted = policy.quiet.adjust(next, nowMillis)
        return if (adjusted == next) this else copy(nextAlarmAtMillis = adjusted)
    }

    /** One alarm at a time: the next check-in while running, or the end of a timed pause. */
    private fun applyAlarm(state: SessionState, policy: SchedulingPolicy) {
        val next = state.nextAlarmAtMillis
        val pauseEnd = state.pausedUntilMillis
        when {
            state.status == SessionStatus.RUNNING && next != null ->
                scheduler.schedule(
                    next,
                    allowExact = policy.allowExact,
                    // US-11: Ring check-ins and the ring cap are real alarms (alarm clock).
                    alarmClock = state.ringActive || policy.alertStyle == AlertStyle.RING,
                )
            state.status == SessionStatus.PAUSED && pauseEnd != null ->
                scheduler.schedule(pauseEnd, allowExact = policy.allowExact)
            else -> scheduler.cancel()
        }
    }
}
