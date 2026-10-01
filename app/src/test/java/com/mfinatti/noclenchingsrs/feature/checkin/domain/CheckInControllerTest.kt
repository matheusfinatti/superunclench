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
        var lastAlarmClock: Boolean? = null
        override fun schedule(atMillis: Long, allowExact: Boolean, alarmClock: Boolean) {
            lastAllowExact = allowExact
            lastAlarmClock = alarmClock
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

        var ringAt: Long? = null
        var ringCapAt: Long? = null
        var ringFullScreen: Boolean? = null
        var silenced = false
        var cancels = 0

        override fun showRing(checkInAtMillis: Long, level: Int, capAtMillis: Long, fullScreen: Boolean, silenced: Boolean) {
            ringAt = checkInAtMillis
            ringCapAt = capAtMillis
            ringFullScreen = fullScreen
            this.silenced = silenced
        }

        override fun silenceRing() {
            silenced = true
        }

        override fun cancel() {
            shownAt = null
            ringAt = null
            silenced = false
            cancels++
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

    // --- US-10 quiet hours -----------------------------------------------------------------

    private val utc = java.util.TimeZone.getTimeZone("UTC")

    private fun quietController(rule: com.mfinatti.noclenchingsrs.domain.settings.QuietHoursRule) = CheckInController(
        sessionRepository = sessions,
        checkInLog = log,
        sessionEngine = SessionEngine(SrsEngine()),
        scheduler = scheduler,
        notifier = notifier,
        clock = AppClock { now },
        policy = { SchedulingPolicy(quiet = rule) },
    )

    private val night = com.mfinatti.noclenchingsrs.domain.settings.QuietHoursRule(
        enabled = true,
        startMinutes = 22 * 60,
        endMinutes = 7 * 60,
        timeZone = java.util.TimeZone.getTimeZone("UTC"),
    )

    private fun utcAt(day: Int, hour: Int, minute: Int = 0): Long =
        java.util.Calendar.getInstance(utc).apply {
            clear()
            set(2026, 9, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `an alarm that would be due in quiet hours is deferred to the end - no notification, no miss`() = runBlocking {
        val c = quietController(night)
        now = utcAt(1, 21, 58)
        val started = c.start()
        // 22:03 would be inside quiet hours: deferred to 07:00 next day.
        assertEquals(utcAt(2, 7), started.nextAlarmAtMillis)
        assertEquals(utcAt(2, 7), scheduler.scheduledAt)
        assertNull(notifier.shownAt)
        assertTrue(log.events.first().isEmpty())
    }

    @Test
    fun `an alarm firing during quiet hours posts nothing and records nothing`() = runBlocking {
        val c = quietController(night)
        now = utcAt(1, 21, 0)
        c.start()
        now = utcAt(1, 23, 0) // e.g. a stale/forced delivery in the window
        val result = c.fireAlarm(force = true)
        assertEquals(FireResult.QuietDeferred(utcAt(2, 7)), result)
        assertNull(notifier.shownAt)
        assertTrue(log.events.first().isEmpty())
        assertEquals(utcAt(2, 7), sessions.session.first().nextAlarmAtMillis)
    }

    @Test
    fun `at the end time the check-in fires and the level is unchanged`() = runBlocking {
        val c = quietController(night)
        c.setLevel(3)
        now = utcAt(1, 21, 50)
        c.start()
        now = utcAt(2, 7, 0)
        assertTrue(c.fireAlarm() is FireResult.Fired)
        assertEquals(now, notifier.shownAt)
        assertEquals(SrsState(3, 0), sessions.session.first().srs)
    }

    @Test
    fun `quiet hours off - alarms fire at any time`() = runBlocking {
        val c = quietController(night.copy(enabled = false))
        now = utcAt(1, 23, 0)
        val started = c.start()
        assertEquals(now + 5 * minute, started.nextAlarmAtMillis)
        now += 5 * minute
        assertTrue(c.fireAlarm() is FireResult.Fired)
    }

    @Test
    fun `simulated quiet hours defer until the preset end, and ending them makes the check-in due now`() = runBlocking {
        val c = quietController(night.copy(simulate = true))
        now = utcAt(1, 12, 0)
        val started = c.start()
        assertEquals(utcAt(2, 7), started.nextAlarmAtMillis)
        val ended = quietController(night).quietHoursEnded()
        assertEquals(now, ended.nextAlarmAtMillis)
        assertEquals(now, scheduler.scheduledAt)
    }

    // --- US-10 reopened: editing the range while running (AC7) ---------------------------------

    private var liveRule = night

    private fun liveController() = CheckInController(
        sessionRepository = sessions,
        checkInLog = log,
        sessionEngine = SessionEngine(SrsEngine()),
        scheduler = scheduler,
        notifier = notifier,
        clock = AppClock { now },
        policy = { SchedulingPolicy(quiet = liveRule) },
    )

    private fun window(start: Int, end: Int, enabled: Boolean = true) =
        night.copy(enabled = enabled, startMinutes = start, endMinutes = end)

    @Test
    fun `range edit - deferred alarm and now outside the new window - next is now plus interval`() = runBlocking {
        liveRule = night
        val c = liveController()
        now = utcAt(1, 23, 0)
        assertEquals(utcAt(2, 7), c.start().nextAlarmAtMillis)
        val state = c.quietHoursChanged { liveRule = window(13 * 60, 14 * 60) }
        assertEquals(utcAt(1, 23, 5), state.nextAlarmAtMillis) // L1 = 5 min, as on Resume
        assertEquals(utcAt(1, 23, 5), scheduler.scheduledAt)
    }

    @Test
    fun `range edit - deferred alarm still inside the new window - moves to the new end`() = runBlocking {
        liveRule = night
        val c = liveController()
        now = utcAt(1, 23, 0)
        c.start()
        val state = c.quietHoursChanged { liveRule = window(23 * 60, 8 * 60 + 15) }
        assertEquals(utcAt(2, 8, 15), state.nextAlarmAtMillis)
    }

    @Test
    fun `range edit - now inside the new window - next check-in deferred to the new end`() = runBlocking {
        liveRule = window(1 * 60, 2 * 60)
        val c = liveController()
        now = utcAt(1, 12, 0)
        assertEquals(utcAt(1, 12, 5), c.start().nextAlarmAtMillis)
        val state = c.quietHoursChanged { liveRule = window(11 * 60 + 30, 15 * 60 + 45) }
        assertEquals(utcAt(1, 15, 45), state.nextAlarmAtMillis)
        assertEquals(utcAt(1, 15, 45), scheduler.scheduledAt)
    }

    @Test
    fun `range edit - alarm not deferred and outside the new window - kept as is`() = runBlocking {
        liveRule = window(1 * 60, 2 * 60)
        val c = liveController()
        now = utcAt(1, 12, 0)
        c.start()
        now = utcAt(1, 12, 2)
        val state = c.quietHoursChanged { liveRule = night }
        assertEquals(utcAt(1, 12, 5), state.nextAlarmAtMillis)
    }

    @Test
    fun `switch off while deferred - next is now plus interval`() = runBlocking {
        liveRule = night
        val c = liveController()
        now = utcAt(1, 23, 0)
        c.start()
        val state = c.quietHoursChanged { liveRule = night.copy(enabled = false) }
        assertEquals(utcAt(1, 23, 5), state.nextAlarmAtMillis)
    }

    @Test
    fun `range edit while stopped changes nothing`() = runBlocking {
        liveRule = night
        val c = liveController()
        now = utcAt(1, 23, 0)
        val state = c.quietHoursChanged { liveRule = window(13 * 60, 14 * 60) }
        assertEquals(SessionStatus.STOPPED, state.status)
        assertNull(scheduler.scheduledAt)
    }

    // --- US-11 Ring ---------------------------------------------------------------------------

    private var ringPolicy = SchedulingPolicy(alertStyle = com.mfinatti.noclenchingsrs.domain.settings.AlertStyle.RING)

    private fun ringController() = CheckInController(
        sessionRepository = sessions,
        checkInLog = log,
        sessionEngine = SessionEngine(SrsEngine()),
        scheduler = scheduler,
        notifier = notifier,
        clock = AppClock { now },
        policy = { ringPolicy },
    )

    private val cap = SchedulingPolicy.DEFAULT_RING_CAP.inWholeMilliseconds

    private suspend fun ringing(c: CheckInController): Long {
        c.start()
        now += 5 * minute
        assertTrue(c.fireAlarm() is FireResult.Fired)
        return now
    }

    @Test
    fun `ring - fires as a ring and the only alarm is the cap`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        assertEquals(firedAt, notifier.ringAt)
        assertNull(notifier.shownAt)
        assertEquals(firedAt + cap, scheduler.scheduledAt)
        assertEquals(true, scheduler.lastAlarmClock) // B1: Ring alarms are alarm clocks
        val s = sessions.session.first()
        assertTrue(s.ringing)
        assertEquals(firedAt + cap, s.nextAlarmAtMillis)
    }

    @Test
    fun `ring - no other check-in fires while ringing, even forced`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        now += 5 * minute // an L1 interval later: would normally be the next check-in
        assertEquals(FireResult.Stale, c.fireAlarm())
        assertEquals(FireResult.Stale, c.fireAlarm(force = true))
        assertEquals(firedAt, sessions.session.first().pendingCheckInAtMillis)
        assertTrue(log.events.first().isEmpty())
    }

    @Test
    fun `ring - answer stops it and schedules from the answer`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        now += 30_000
        val result = c.answer(com.mfinatti.noclenchingsrs.domain.srs.Answer.GOOD, firedAt, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.ALARM_SCREEN)
        assertTrue(result is AnswerResult.Applied)
        assertNull(notifier.ringAt)
        val s = sessions.session.first()
        assertNull(s.ringCapAtMillis)
        assertEquals(now + 5 * minute, s.nextAlarmAtMillis)
        assertEquals(com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.ALARM_SCREEN, log.events.first().single().source)
    }

    @Test
    fun `ring - cap records M timeout and schedules from the miss`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        now = firedAt + cap
        val result = c.fireAlarm()
        assertEquals(FireResult.RingTimedOut(autoPaused = false), result)
        val event = log.events.first().single()
        assertEquals(CheckInOutcome.MISSED, event.outcome)
        assertEquals(com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.TIMEOUT, event.source)
        assertNull(notifier.ringAt)
        val s = sessions.session.first()
        assertNull(s.pendingCheckInAtMillis)
        assertEquals(1, s.consecutiveMisses)
        assertEquals(now + 5 * minute, s.nextAlarmAtMillis)
        assertEquals(now + 5 * minute, scheduler.scheduledAt)
    }

    @Test
    fun `ring - three timeouts auto-pause`() = runBlocking {
        val c = ringController()
        c.start()
        repeat(3) { i ->
            now += 5 * minute
            assertTrue(c.fireAlarm() is FireResult.Fired)
            now += cap
            val r = c.fireAlarm()
            assertEquals(FireResult.RingTimedOut(autoPaused = i == 2), r)
        }
        val s = sessions.session.first()
        assertEquals(SessionStatus.PAUSED, s.status)
        assertTrue(s.autoPaused)
        assertNull(scheduler.scheduledAt)
    }

    @Test
    fun `ring - silence keeps it pending and the cap still applies`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        assertTrue(c.silenceRing())
        assertTrue(notifier.silenced)
        val s = sessions.session.first()
        assertTrue(s.ringActive)
        assertTrue(s.ringSilenced)
        assertEquals(firedAt, s.pendingCheckInAtMillis)
        assertTrue(!c.silenceRing()) // already silenced
        now = firedAt + cap
        assertTrue(c.fireAlarm() is FireResult.RingTimedOut)
    }

    @Test
    fun `ring - stop and pause end it with nothing recorded`() = runBlocking {
        val c = ringController()
        ringing(c)
        c.pause(null)
        assertNull(notifier.ringAt)
        var s = sessions.session.first()
        assertNull(s.pendingCheckInAtMillis)
        assertNull(s.ringCapAtMillis)
        assertTrue(log.events.first().isEmpty())
        c.resume()
        now += 5 * minute
        c.fireAlarm()
        c.stop()
        s = sessions.session.first()
        assertNull(s.ringCapAtMillis)
        assertNull(notifier.ringAt)
        assertTrue(log.events.first().isEmpty())
    }

    @Test
    fun `ring - switching to Nudge while ringing does not stop it, the next one is a nudge`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        ringPolicy = ringPolicy.copy(alertStyle = com.mfinatti.noclenchingsrs.domain.settings.AlertStyle.NUDGE)
        assertTrue(sessions.session.first().ringing)
        now += 10_000
        c.answer(com.mfinatti.noclenchingsrs.domain.srs.Answer.BAD, firedAt, com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.NOTIFICATION)
        now += 5 * minute
        assertTrue(c.fireAlarm() is FireResult.Fired)
        assertEquals(now, notifier.shownAt)
        assertNull(notifier.ringAt)
    }

    @Test
    fun `ring - quiet hours win, and the cap is never moved by quiet hours`() = runBlocking {
        ringPolicy = ringPolicy.copy(quiet = night)
        val c = ringController()
        now = utcAt(1, 21, 0)
        c.start()
        now = utcAt(1, 23, 0)
        val r = c.fireAlarm(force = true)
        assertTrue(r is FireResult.QuietDeferred)
        assertNull(notifier.ringAt)
        // A ring that started just before the window keeps its cap inside the window.
        ringPolicy = ringPolicy.copy(quiet = night.copy(startMinutes = 21 * 60 + 58))
        sessions.update { it.copy(nextAlarmAtMillis = utcAt(1, 21, 55)) }
        now = utcAt(1, 21, 55)
        sessions.update { com.mfinatti.noclenchingsrs.domain.session.SessionState(status = SessionStatus.RUNNING, nextAlarmAtMillis = now) }
        assertTrue(c.fireAlarm() is FireResult.Fired)
        assertEquals(now + cap, sessions.session.first().nextAlarmAtMillis)
    }

    @Test
    fun `ring - restore after process death rings again, a passed cap times out`() = runBlocking {
        val c = ringController()
        val firedAt = ringing(c)
        notifier.ringAt = null
        now += 60_000
        c.restoreSchedule()
        assertEquals(firedAt, notifier.ringAt)
        assertEquals(firedAt + cap, scheduler.scheduledAt)
        now = firedAt + cap + 5_000
        c.restoreSchedule()
        assertEquals(firedAt + cap, scheduler.scheduledAt) // past: fires at once
        assertTrue(c.fireAlarm() is FireResult.RingTimedOut)
    }

    @Test
    fun `ring - fullScreen flag follows the policy`() = runBlocking {
        ringPolicy = ringPolicy.copy(fullScreenAllowed = false)
        val c = ringController()
        ringing(c)
        assertEquals(false, notifier.ringFullScreen)
    }

    @Test
    fun `nudge check-ins are plain exact alarms, ring check-ins alarm clocks`() = runBlocking {
        controller.start()
        assertEquals(false, scheduler.lastAlarmClock)
        val c = ringController()
        c.start()
        assertEquals(true, scheduler.lastAlarmClock)
    }
}
