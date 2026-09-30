package com.mfinatti.noclenchingsrs.feature.checkin.domain

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mfinatti.noclenchingsrs.core.time.AppClock
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.data.session.SessionRepository
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.session.SessionEngine
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration

class CheckInControllerTest {

    private class FakeScheduler : AlarmScheduler {
        var scheduledAt: Long? = null
        var lastAllowExact: Boolean? = null
        override fun schedule(atMillis: Long, allowExact: Boolean) {
            lastAllowExact = allowExact
            scheduledAt = atMillis
        }

        override fun cancel() {
            scheduledAt = null
        }
    }

    private class FakeNotifier : CheckInNotifier {
        var shownAt: Long? = null
        var shownLevel: Int? = null
        override fun show(checkInAtMillis: Long, level: Int, interval: Duration, customLayout: Boolean) {
            shownAt = checkInAtMillis
            shownLevel = level
        }

        override fun cancel() {
            shownAt = null
        }
    }

    private val minute = 60_000L
    private var now = 1_700_000_000_000L
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope
    private lateinit var sessions: SessionRepository
    private lateinit var log: CheckInLogRepository
    private lateinit var scheduler: FakeScheduler
    private lateinit var notifier: FakeNotifier
    private lateinit var controller: CheckInController

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("checkin-test").toFile()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val engine = SrsEngine()
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(dir, "session.preferences_pb")
        }
        sessions = SessionRepository(dataStore, engine)
        log = CheckInLogRepository(File(dir, "log.json"), Dispatchers.IO)
        scheduler = FakeScheduler()
        notifier = FakeNotifier()
        controller = CheckInController(
            sessionRepository = sessions,
            checkInLog = log,
            sessionEngine = SessionEngine(engine),
            scheduler = scheduler,
            notifier = notifier,
            clock = AppClock { now },
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    @Test
    fun `start schedules the first alarm 5 min out`() = runBlocking {
        val state = controller.start()
        assertEquals(SessionStatus.RUNNING, state.status)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertNull(notifier.shownAt)
    }

    @Test
    fun `fire when not running does nothing`() = runBlocking {
        assertEquals(FireResult.NotRunning, controller.fireAlarm())
        assertNull(notifier.shownAt)
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `fire posts the notification and schedules the next alarm`() = runBlocking {
        controller.start()
        now += 5 * minute
        val result = controller.fireAlarm()
        assertEquals(FireResult.Fired(previousMissed = false), result)
        assertEquals(now, notifier.shownAt)
        assertEquals(1, notifier.shownLevel)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertEquals(now, sessions.session.first().pendingCheckInAtMillis)
    }

    @Test
    fun `answer removes the notification, records history and reschedules from the answer`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val firedAt = now
        now += 1 * minute
        val result = controller.answer(Answer.GOOD, firedAt)
        assertEquals(AnswerResult.Applied(SrsState(1, 1), com.mfinatti.noclenchingsrs.domain.srs.LevelChange.PROGRESSED), result)
        assertNull(notifier.shownAt)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        val session = sessions.session.first()
        assertNull(session.pendingCheckInAtMillis)
        // Persisted through DataStore (survives process death), incl. the level-feedback event.
        assertEquals(SrsState(1, 1), session.srs)
        assertEquals(com.mfinatti.noclenchingsrs.domain.srs.LevelChange.PROGRESSED, session.lastAnswer?.change)
        assertEquals(now, session.lastAnswer?.atMillis)
        val events = log.events.first()
        assertEquals(1, events.size)
        assertEquals(CheckInOutcome.GOOD, events.single().outcome)
    }

    @Test
    fun `answer for a replaced check-in is ignored`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val oldFiredAt = now
        now += 5 * minute
        controller.fireAlarm() // replaces the first (recorded as Missed)
        assertEquals(AnswerResult.Stale, controller.answer(Answer.BAD, oldFiredAt))
        assertEquals(now, notifier.shownAt)
        val events = log.events.first()
        assertEquals(listOf(CheckInOutcome.MISSED), events.map { it.outcome })
    }

    @Test
    fun `debug answer without a pending check-in still applies`() = runBlocking {
        val result = controller.answer(Answer.GOOD, checkInAtMillis = null)
        assertTrue(result is AnswerResult.Applied)
        assertEquals(SrsState(1, 1), sessions.session.first().srs)
        // Not running: nothing scheduled.
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `stop removes notification and alarm and keeps the level`() = runBlocking {
        controller.start()
        controller.answer(Answer.GOOD, null)
        now += 5 * minute
        controller.fireAlarm()
        val state = controller.stop()
        assertEquals(SessionStatus.STOPPED, state.status)
        assertEquals(SrsState(1, 1), state.srs)
        assertNull(notifier.shownAt)
        assertNull(scheduler.scheduledAt)
        assertEquals(FireResult.NotRunning, controller.fireAlarm())
    }

    @Test
    fun `third consecutive miss auto-pauses and clears side effects`() = runBlocking {
        controller.start()
        repeat(3) {
            now += 5 * minute
            controller.fireAlarm()
        }
        now += 5 * minute
        assertEquals(FireResult.AutoPaused, controller.fireAlarm())
        assertNull(notifier.shownAt)
        assertNull(scheduler.scheduledAt)
        assertEquals(SessionStatus.PAUSED, sessions.session.first().status)
    }

    @Test
    fun `answers record their source and the level after`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        controller.answer(Answer.GOOD, now, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.NOTIFICATION)
        controller.answer(Answer.GOOD, null) // debug panel default
        val events = log.events.first()
        assertEquals(
            listOf(
                com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.NOTIFICATION,
                com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.PANEL,
            ),
            events.map { it.source },
        )
        assertEquals(1, events.last().levelAfter)
        assertEquals(2, events.last().subLevelAfter)
    }

    @Test
    fun `restoreSchedule re-arms a running session whose alarm passed, without posting`() = runBlocking {
        controller.start()
        now += 60 * minute // phone was off past the alarm time
        scheduler.cancel() // reboot clears alarms
        val state = controller.restoreSchedule()
        assertEquals(now + 5 * minute, state.nextAlarmAtMillis)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertNull(notifier.shownAt)
        assertTrue(log.events.first().isEmpty())
    }

    @Test
    fun `restoreSchedule keeps a future alarm and does nothing when stopped`() = runBlocking {
        controller.start()
        val next = sessions.session.first().nextAlarmAtMillis
        now += 1 * minute
        assertEquals(next, controller.restoreSchedule().nextAlarmAtMillis)
        controller.stop()
        assertEquals(SessionStatus.STOPPED, controller.restoreSchedule().status)
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `policy scales intervals and controls exact alarms`() = runBlocking {
        val shortController = CheckInController(
            sessionRepository = sessions,
            checkInLog = log,
            sessionEngine = SessionEngine(SrsEngine()),
            scheduler = scheduler,
            notifier = notifier,
            clock = AppClock { now },
            policy = {
                SchedulingPolicy(
                    scale = com.mfinatti.noclenchingsrs.domain.session.IntervalScale.MinutesAsSeconds,
                    allowExact = false,
                )
            },
        )
        shortController.start()
        assertEquals(now + 5_000L, scheduler.scheduledAt) // 5 min -> 5 s
        assertEquals(false, scheduler.lastAllowExact)
    }

    // --- US-05 O3: answer-then-alarm race -------------------------------------------------

    @Test
    fun `answer cancels the pending alarm by rescheduling it and clears pending`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val firedAt = now
        // Answer exactly when the next alarm was due (5 s cadence in debug short mode).
        now += 5 * minute
        controller.answer(Answer.GOOD, firedAt, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.CARD)
        val afterAnswer = sessions.session.first()
        assertNull(afterAnswer.pendingCheckInAtMillis)
        // The single AlarmManager alarm was replaced by the new time (same PendingIntent).
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertEquals(now + 5 * minute, afterAnswer.nextAlarmAtMillis)
    }

    @Test
    fun `late delivery of the replaced alarm is ignored - no check-in, no miss`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val firedAt = now
        now += 5 * minute
        controller.answer(Answer.GOOD, firedAt, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.CARD)
        // The old alarm's broadcast was already in flight and arrives just after the answer.
        now += 200
        assertEquals(FireResult.Stale, controller.fireAlarm())
        assertNull(notifier.shownAt)
        assertNull(sessions.session.first().pendingCheckInAtMillis)
        // The real next alarm then fires normally: a new check-in, and no Missed for the answered one.
        now = sessions.session.first().nextAlarmAtMillis!!
        assertTrue(controller.fireAlarm() is FireResult.Fired)
        val outcomes = log.events.first().map { it.outcome }
        assertEquals(listOf(CheckInOutcome.GOOD), outcomes)
    }

    @Test
    fun `QA fire alarm now is forced past the stale check`() = runBlocking {
        controller.start()
        assertTrue(controller.fireAlarm(force = true) is FireResult.Fired)
        assertEquals(now, notifier.shownAt)
    }

    // --- US-06 pause / resume -------------------------------------------------------------

    @Test
    fun `timed pause cancels the check-in alarm and schedules the pause end`() = runBlocking {
        controller.start()
        val state = controller.pause(kotlin.time.Duration.parse("1h"))
        assertEquals(SessionStatus.PAUSED, state.status)
        assertNull(state.nextAlarmAtMillis)
        assertEquals(now + 60 * minute, state.pausedUntilMillis)
        assertEquals(now + 60 * minute, scheduler.scheduledAt)
    }

    @Test
    fun `pause duration scales with short intervals`() = runBlocking {
        val shortController = CheckInController(
            sessionRepository = sessions,
            checkInLog = log,
            sessionEngine = SessionEngine(SrsEngine()),
            scheduler = scheduler,
            notifier = notifier,
            clock = AppClock { now },
            policy = { SchedulingPolicy(scale = com.mfinatti.noclenchingsrs.domain.session.IntervalScale.MinutesAsSeconds) },
        )
        shortController.start()
        val state = shortController.pause(kotlin.time.Duration.parse("15m"))
        assertEquals(now + 15_000L, state.pausedUntilMillis) // 15 min pause = 15 s
    }

    @Test
    fun `timed pause end alarm resumes with a full interval`() = runBlocking {
        controller.start()
        controller.pause(kotlin.time.Duration.parse("15m"))
        now += 15 * minute
        assertEquals(FireResult.PauseEnded, controller.fireAlarm())
        val state = sessions.session.first()
        assertEquals(SessionStatus.RUNNING, state.status)
        assertEquals(now + 5 * minute, state.nextAlarmAtMillis)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertNull(notifier.shownAt)
    }

    @Test
    fun `untimed pause ignores alarms, resume restarts the countdown, stop from paused`() = runBlocking {
        controller.start()
        controller.pause(null)
        assertNull(scheduler.scheduledAt)
        now += 60 * minute
        assertEquals(FireResult.NotRunning, controller.fireAlarm())
        val resumed = controller.resume()
        assertEquals(now + 5 * minute, resumed.nextAlarmAtMillis)
        controller.pause(null)
        assertEquals(SessionStatus.STOPPED, controller.stop().status)
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `answering while paused updates level but schedules nothing new`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val firedAt = now
        controller.pause(null)
        controller.answer(Answer.GOOD, firedAt, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.NOTIFICATION)
        val state = sessions.session.first()
        assertEquals(SessionStatus.PAUSED, state.status)
        assertEquals(SrsState(1, 1), state.srs)
        assertNull(state.nextAlarmAtMillis)
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `boot after a timed pause ended resumes, a future one keeps its end alarm`() = runBlocking {
        controller.start()
        controller.pause(kotlin.time.Duration.parse("15m"))
        now += 1 * minute
        assertEquals(SessionStatus.PAUSED, controller.restoreSchedule().status)
        assertEquals(now + 14 * minute, scheduler.scheduledAt)
        now += 30 * minute
        assertEquals(SessionStatus.RUNNING, controller.restoreSchedule().status)
    }

    // --- US-07 prerequisites ---------------------------------------------------------------

    @Test
    fun `auto-pause cancels the live notification like Stop, and a stale answer is ignored`() = runBlocking {
        controller.start()
        repeat(3) {
            now += 5 * minute
            controller.fireAlarm()
        }
        val lastCheckIn = notifier.shownAt!!
        now += 5 * minute
        assertEquals(FireResult.AutoPaused, controller.fireAlarm())
        assertNull(notifier.shownAt)
        assertEquals(AnswerResult.Stale, controller.answer(Answer.GOOD, lastCheckIn))
    }

    @Test
    fun `debug auto-pause OFF keeps running after many misses`() = runBlocking {
        val noAutoPause = CheckInController(
            sessionRepository = sessions,
            checkInLog = log,
            sessionEngine = SessionEngine(SrsEngine()),
            scheduler = scheduler,
            notifier = notifier,
            clock = AppClock { now },
            policy = { SchedulingPolicy(autoPause = false) },
        )
        noAutoPause.start()
        repeat(6) {
            now += 5 * minute
            noAutoPause.fireAlarm()
        }
        val state = sessions.session.first()
        assertEquals(SessionStatus.RUNNING, state.status)
        assertEquals(5, state.consecutiveMisses)
    }

    // --- US-07 missed check-ins & auto-pause ----------------------------------------------

    @Test
    fun `AC1 - replaced check-in is Missed, level unchanged, one notification`() = runBlocking {
        controller.setLevel(3)
        controller.incrementSubLevel()
        controller.incrementSubLevel()
        controller.start()
        now += 15 * minute
        controller.fireAlarm()
        now += 15 * minute
        controller.fireAlarm()
        val state = sessions.session.first()
        assertEquals(SrsState(3, 2), state.srs)
        assertEquals(1, state.consecutiveMisses)
        assertEquals(now, notifier.shownAt) // the single notification id now shows the new check-in
        assertEquals(listOf(CheckInOutcome.MISSED), log.events.first().map { it.outcome })
    }

    @Test
    fun `AC2 - Good after two misses resets the counter`() = runBlocking {
        controller.start()
        repeat(3) {
            now += 5 * minute
            controller.fireAlarm()
        }
        assertEquals(2, sessions.session.first().consecutiveMisses)
        controller.answer(Answer.GOOD, now, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.CARD)
        assertEquals(0, sessions.session.first().consecutiveMisses)
    }

    @Test
    fun `AC3 - three dismissals auto-pause, remove the notification, and stop alarms`() = runBlocking {
        controller.start()
        repeat(3) { index ->
            now += 5 * minute
            controller.fireAlarm()
            val result = controller.markMissed(now)
            assertEquals(MissResult.Missed(consecutiveMisses = index + 1, autoPaused = index == 2), result)
            assertNull(notifier.shownAt)
        }
        val state = sessions.session.first()
        assertEquals(SessionStatus.PAUSED, state.status)
        assertTrue(state.autoPaused)
        assertNull(scheduler.scheduledAt)
        now += 60 * minute
        assertEquals(FireResult.NotRunning, controller.fireAlarm())
        // Resume clears the misses and restarts with a full interval.
        val resumed = controller.resume()
        assertEquals(0, resumed.consecutiveMisses)
        assertEquals(now + 5 * minute, resumed.nextAlarmAtMillis)
    }

    @Test
    fun `AC4 - dismissal counts as Missed with source DISMISSED and reschedules at the current interval`() = runBlocking {
        controller.start()
        now += 5 * minute
        controller.fireAlarm()
        val firedAt = now
        now += 30_000
        assertTrue(controller.markMissed(firedAt) is MissResult.Missed)
        val event = log.events.first().single()
        assertEquals(CheckInOutcome.MISSED, event.outcome)
        assertEquals(com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.DISMISSED, event.source)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertNull(sessions.session.first().pendingCheckInAtMillis)
    }

    @Test
    fun `dismissing with nothing pending or a replaced check-in is ignored`() = runBlocking {
        controller.start()
        assertEquals(MissResult.NoPending, controller.markMissed(null))
        now += 5 * minute
        controller.fireAlarm()
        assertEquals(MissResult.NoPending, controller.markMissed(now - 1))
        assertTrue(log.events.first().isEmpty())
    }

    // --- US-09 reset progress --------------------------------------------------------------

    @Test
    fun `reset progress keeps history and a running session restarts at the L1 interval`() = runBlocking {
        controller.setLevel(6)
        controller.start()
        now += 60 * minute
        controller.fireAlarm()
        controller.answer(Answer.GOOD, now, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.CARD)
        now += 1 * minute
        val state = controller.resetProgress()
        assertEquals(SrsState(1, 0), state.srs)
        assertEquals(SessionStatus.RUNNING, state.status)
        assertEquals(now + 5 * minute, state.nextAlarmAtMillis)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
        assertEquals(1, log.events.first().size)
    }

    @Test
    fun `reset progress while stopped schedules nothing`() = runBlocking {
        controller.setLevel(4)
        val state = controller.resetProgress()
        assertEquals(SrsState(1, 0), state.srs)
        assertNull(scheduler.scheduledAt)
    }
}
