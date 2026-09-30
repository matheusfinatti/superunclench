package com.mfinatti.noclenchingsrs.domain.session

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    private val engine = SessionEngine()
    private val now = 1_700_000_000_000L
    private val minute = 60_000L

    private fun running(srs: SrsState = SrsState(), misses: Int = 0, pendingAt: Long? = null) = SessionState(
        status = SessionStatus.RUNNING,
        srs = srs,
        nextAlarmAtMillis = now + 5 * minute,
        pendingCheckInAtMillis = pendingAt,
        consecutiveMisses = misses,
    )

    @Test
    fun `default session is stopped at L1 0`() {
        val state = SessionState()
        assertEquals(SessionStatus.STOPPED, state.status)
        assertEquals(SrsState(1, 0), state.srs)
        assertNull(state.nextAlarmAtMillis)
        assertEquals(0, state.consecutiveMisses)
    }

    @Test
    fun `start schedules first alarm at current level interval`() {
        val started = engine.start(SessionState(), now)
        assertEquals(SessionStatus.RUNNING, started.status)
        assertEquals(now + 5 * minute, started.nextAlarmAtMillis)

        val atL3 = engine.start(SessionState(srs = SrsState(3, 1)), now)
        assertEquals(now + 15 * minute, atL3.nextAlarmAtMillis)
        assertEquals(SrsState(3, 1), atL3.srs)
    }

    @Test
    fun `stop keeps level and clears scheduling`() {
        val stopped = engine.stop(running(srs = SrsState(4, 2), pendingAt = now))
        assertEquals(SessionStatus.STOPPED, stopped.status)
        assertEquals(SrsState(4, 2), stopped.srs)
        assertNull(stopped.nextAlarmAtMillis)
        assertNull(stopped.pendingCheckInAtMillis)
    }

    @Test
    fun `answer schedules next alarm from answer time using resulting level`() {
        val answeredAt = now + 7 * minute
        val t = engine.onAnswer(running(srs = SrsState(1, 2), pendingAt = now), Answer.GOOD, answeredAt)
        assertEquals(SrsState(2, 0), t.state.srs)
        assertEquals(LevelChange.PROMOTED, t.levelChange)
        assertEquals(answeredAt + 10 * minute, t.state.nextAlarmAtMillis)
        assertNull(t.state.pendingCheckInAtMillis)
    }

    @Test
    fun `answer records event with level before the answer`() {
        val t = engine.onAnswer(running(srs = SrsState(3, 2)), Answer.BAD, now)
        val event = t.event!!
        assertEquals(CheckInOutcome.BAD, event.outcome)
        assertEquals(3, event.level)
        assertEquals(2, event.subLevel)
        assertEquals(now, event.atMillis)
        assertEquals(SrsState(3, 0), t.state.srs)
    }

    @Test
    fun `answer while stopped applies SRS without scheduling`() {
        val t = engine.onAnswer(SessionState(), Answer.GOOD, now)
        assertEquals(SrsState(1, 1), t.state.srs)
        assertEquals(SessionStatus.STOPPED, t.state.status)
        assertNull(t.state.nextAlarmAtMillis)
    }

    @Test
    fun `missed is neutral and reschedules at current interval`() {
        val missedAt = now + 2 * minute
        val t = engine.onMissed(running(srs = SrsState(3, 2), pendingAt = now), missedAt)
        assertEquals(SrsState(3, 2), t.state.srs)
        assertEquals(1, t.state.consecutiveMisses)
        assertEquals(missedAt + 15 * minute, t.state.nextAlarmAtMillis)
        assertEquals(CheckInOutcome.MISSED, t.event!!.outcome)
        assertEquals(SessionStatus.RUNNING, t.state.status)
    }

    @Test
    fun `three consecutive misses auto-pause`() {
        var state = running()
        repeat(3) { state = engine.onMissed(state, now).state }
        assertEquals(SessionStatus.PAUSED, state.status)
        assertTrue(state.autoPaused)
        assertNull(state.nextAlarmAtMillis)
        assertNull(state.pausedUntilMillis)
        assertEquals(3, state.consecutiveMisses)
    }

    @Test
    fun `good after two misses resets the miss counter`() {
        var state = running()
        repeat(2) { state = engine.onMissed(state, now).state }
        assertEquals(2, state.consecutiveMisses)
        state = engine.onAnswer(state, Answer.GOOD, now).state
        assertEquals(0, state.consecutiveMisses)
        assertEquals(SessionStatus.RUNNING, state.status)
    }

    @Test
    fun `alarm firing with a pending check-in records it as missed`() {
        val firedAt = now + 15 * minute
        val t = engine.onAlarmFired(running(srs = SrsState(3, 2), pendingAt = now), firedAt)
        assertEquals(CheckInOutcome.MISSED, t.event!!.outcome)
        assertEquals(SrsState(3, 2), t.state.srs)
        assertEquals(1, t.state.consecutiveMisses)
        assertEquals(firedAt, t.state.pendingCheckInAtMillis)
        assertEquals(firedAt + 15 * minute, t.state.nextAlarmAtMillis)
    }

    @Test
    fun `alarm firing that causes the third miss auto-pauses without a new check-in`() {
        val t = engine.onAlarmFired(running(misses = 2, pendingAt = now), now + 5 * minute)
        assertEquals(SessionStatus.PAUSED, t.state.status)
        assertTrue(t.state.autoPaused)
        assertNull(t.state.pendingCheckInAtMillis)
    }

    @Test
    fun `alarm firing without pending posts a check-in`() {
        val t = engine.onAlarmFired(running(), now)
        assertNull(t.event)
        assertEquals(now, t.state.pendingCheckInAtMillis)
    }

    @Test
    fun `alarm firing when not running does nothing`() {
        val stopped = SessionState()
        assertEquals(stopped, engine.onAlarmFired(stopped, now).state)
    }

    @Test
    fun `pause and resume`() {
        val paused = engine.pause(running(srs = SrsState(2, 1)), untilMillis = now + 60 * minute)
        assertEquals(SessionStatus.PAUSED, paused.status)
        assertNull(paused.nextAlarmAtMillis)
        assertEquals(now + 60 * minute, paused.pausedUntilMillis)
        assertFalse(paused.autoPaused)

        val resumedAt = now + 10 * minute
        val resumed = engine.resume(paused, resumedAt)
        assertEquals(SessionStatus.RUNNING, resumed.status)
        assertEquals(resumedAt + 10 * minute, resumed.nextAlarmAtMillis)
        assertNull(resumed.pausedUntilMillis)
        assertEquals(SrsState(2, 1), resumed.srs)
    }

    @Test
    fun `pause is ignored unless running and resume unless paused`() {
        val stopped = SessionState()
        assertEquals(stopped, engine.pause(stopped, null))
        assertEquals(stopped, engine.resume(stopped, now))
    }

    @Test
    fun `reset progress keeps everything but the SRS position`() {
        val state = running(srs = SrsState(6, 3), misses = 1)
        val reset = engine.resetProgress(state)
        assertEquals(SrsState(1, 0), reset.srs)
        assertEquals(state.copy(srs = SrsState(1, 0)), reset)
    }

    @Test
    fun `answer records the last answer for level feedback`() {
        val t = engine.onAnswer(running(srs = SrsState(1, 2)), Answer.GOOD, now)
        assertEquals(LastAnswer(now, Answer.GOOD, LevelChange.PROMOTED), t.state.lastAnswer)
    }

    @Test
    fun `debug level changes clear the last answer`() {
        val answered = engine.onAnswer(running(), Answer.GOOD, now).state
        assertNull(engine.setLevel(answered, 5).lastAnswer)
        assertEquals(SrsState(5, 0), engine.setLevel(answered, 5).srs)
        assertNull(engine.incrementSubLevel(answered).lastAnswer)
        assertNull(engine.resetProgress(answered).lastAnswer)
    }

    @Test
    fun `promotion schedules the next alarm with the new level interval`() {
        val t = engine.onAnswer(running(srs = SrsState(2, 2)), Answer.GOOD, now)
        assertEquals(SrsState(3, 0), t.state.srs)
        assertEquals(now + 15 * minute, t.state.nextAlarmAtMillis)
    }

    @Test
    fun `bad at level 8 demotes to 7 with a 2 h interval`() {
        val t = engine.onAnswer(running(srs = SrsState(8, 0)), Answer.BAD, now)
        assertEquals(SrsState(7, 0), t.state.srs)
        assertEquals(now + 120 * minute, t.state.nextAlarmAtMillis)
    }

    @Test
    fun `minutes-as-seconds scale turns every level interval into seconds`() {
        val scale = IntervalScale.MinutesAsSeconds
        val expected = listOf(5, 10, 15, 30, 45, 60, 120, 180)
        expected.forEachIndexed { index, seconds ->
            assertEquals(now + seconds * 1_000L, engine.nextAlarmAt(now, index + 1, scale))
        }
        assertEquals(now + 5 * minute, engine.nextAlarmAt(now, 1, IntervalScale.Real))
    }

    @Test
    fun `restore after boot reschedules only past or missing alarms and records nothing`() {
        val past = running().copy(nextAlarmAtMillis = now - minute)
        assertEquals(now + 5 * minute, engine.restoreAfterBoot(past, now).nextAlarmAtMillis)
        val future = running().copy(nextAlarmAtMillis = now + minute)
        assertEquals(future, engine.restoreAfterBoot(future, now))
        val stopped = SessionState()
        assertEquals(stopped, engine.restoreAfterBoot(stopped, now))
    }

    @Test
    fun `start resets the exact banner dismissal`() {
        val dismissed = engine.dismissExactTimingBanner(SessionState())
        assertTrue(dismissed.exactTimingBannerDismissed)
        assertFalse(engine.start(dismissed, now).exactTimingBannerDismissed)
    }

    @Test
    fun `misses from a replaced alarm are tagged ALARM and keep the level`() {
        val t = engine.onAlarmFired(running(srs = SrsState(2, 2), pendingAt = now), now + 10 * minute)
        val event = t.event!!
        assertEquals(com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.ALARM, event.source)
        assertEquals(2, event.levelAfter)
        assertEquals(2, event.subLevelAfter)
        assertEquals(SrsState(2, 2), t.state.srs)
    }
}
