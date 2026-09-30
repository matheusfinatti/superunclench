package com.mfinatti.noclenchingsrs.feature.checkin.domain

import com.mfinatti.noclenchingsrs.core.time.AppClock
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.data.session.SessionRepository
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.SessionEngine
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
        val state = sessionRepository.update { sessionEngine.start(it, clock.nowMillis(), p.scale) }
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
        val state = sessionRepository.update { sessionEngine.pause(it, until) }
        applyAlarm(state, p)
        state
    }

    /** Resume a paused session: next alarm = now + current interval. */
    suspend fun resume(): SessionState = mutex.withLock {
        val p = policy()
        val state = sessionRepository.update { sessionEngine.resume(it, clock.nowMillis(), p.scale) }
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
                    val resumed = sessionEngine.onPauseEnded(current, now, p.scale)
                    outcome = if (resumed.status == SessionStatus.RUNNING) FireResult.PauseEnded else FireResult.NotRunning
                    resumed
                }
                current.status != SessionStatus.RUNNING -> {
                    outcome = FireResult.NotRunning
                    current
                }
                !force && sessionEngine.isStaleAlarm(current, now) -> {
                    outcome = FireResult.Stale
                    current
                }
                else -> {
                    val t = sessionEngine.onAlarmFired(current, now, p.scale, p.autoPause)
                    transition = t
                    t.state
                }
            }
        }
        outcome?.let { result ->
            if (result == FireResult.PauseEnded || result == FireResult.Stale) applyAlarm(state, p)
            return@withLock result
        }
        val missedEvent = transition?.event
        if (missedEvent != null) checkInLog.append(missedEvent)
        val pendingAt = state.pendingCheckInAtMillis
        if (state.status == SessionStatus.RUNNING && pendingAt != null) {
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
                val t = sessionEngine.onAnswer(current, answer, clock.nowMillis(), p.scale, source)
                transition = t
                t.state
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
                val t = sessionEngine.onMissed(current, clock.nowMillis(), p.scale, source, p.autoPause)
                transition = t
                t.state
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
        val state = sessionRepository.update { sessionEngine.restoreAfterBoot(it, clock.nowMillis(), p.scale) }
        applyAlarm(state, p)
        state
    }

    /** Re-times the next alarm from now (debug Short intervals toggle). */
    suspend fun rescheduleFromNow(): SessionState = mutex.withLock {
        val p = policy()
        val state = sessionRepository.update { sessionEngine.rescheduleFromNow(it, clock.nowMillis(), p.scale) }
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
        val state = sessionRepository.update { current ->
            sessionEngine.rescheduleFromNow(sessionEngine.resetProgress(current), clock.nowMillis(), p.scale)
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

    /** One alarm at a time: the next check-in while running, or the end of a timed pause. */
    private fun applyAlarm(state: SessionState, policy: SchedulingPolicy) {
        val next = state.nextAlarmAtMillis
        val pauseEnd = state.pausedUntilMillis
        when {
            state.status == SessionStatus.RUNNING && next != null ->
                scheduler.schedule(next, allowExact = policy.allowExact)
            state.status == SessionStatus.PAUSED && pauseEnd != null ->
                scheduler.schedule(pauseEnd, allowExact = policy.allowExact)
            else -> scheduler.cancel()
        }
    }
}
