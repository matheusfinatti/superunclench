package com.mfinatti.noclenchingsrs.feature.home.ui

import com.mfinatti.noclenchingsrs.data.debug.DebugOverrides
import com.mfinatti.noclenchingsrs.domain.session.LastAnswer
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {

    private val engine = SrsEngine()

    @Test
    fun `fresh install shows disclaimer, no name, Level 1 0 of 3, stopped`() {
        val state = HomeUiState.from(UserSettings(), SessionState(), DebugOverrides(shortIntervals = false), engine)
        assertEquals(
            HomeUiState(
                userName = null,
                disclaimerVisible = true,
                sessionStatus = SessionStatus.STOPPED,
                level = 1,
                subLevel = 0,
                subLevelCount = 3,
            ),
            state,
        )
    }

    @Test
    fun `name, dismissed disclaimer and level are mapped`() {
        val state = HomeUiState.from(
            UserSettings(name = "Alex", disclaimerDismissed = true),
            SessionState(status = SessionStatus.RUNNING, srs = SrsState(3, 2), pendingCheckInAtMillis = 42L),
            DebugOverrides(),
            engine,
        )
        assertEquals("Alex", state.userName)
        assertEquals(false, state.disclaimerVisible)
        assertEquals(SessionStatus.RUNNING, state.sessionStatus)
        assertEquals(3, state.level)
        assertEquals(2, state.subLevel)
        assertEquals(4, state.subLevelCount)
        assertEquals(42L, state.pendingCheckInAtMillis)
    }

    @Test
    fun `notifications-off banner rules`() {
        val fresh = HomeUiState()
        // Android 13+, never asked yet, stopped: the rationale flow handles it, no banner.
        assertEquals(false, fresh.showNotificationsOffBanner(canDeliver = false, requiresRuntimePermission = true))
        // After "Don't allow".
        val asked = fresh.copy(notificationPermissionRequested = true)
        assertEquals(true, asked.showNotificationsOffBanner(canDeliver = false, requiresRuntimePermission = true))
        // Running without delivery (e.g. revoked later).
        val running = fresh.copy(sessionStatus = SessionStatus.RUNNING)
        assertEquals(true, running.showNotificationsOffBanner(canDeliver = false, requiresRuntimePermission = true))
        // Android 12 and lower: disabled notifications always show the banner.
        assertEquals(true, fresh.showNotificationsOffBanner(canDeliver = false, requiresRuntimePermission = false))
        // Delivery works: no banner, unless QA forces the preview.
        assertEquals(false, asked.showNotificationsOffBanner(canDeliver = true, requiresRuntimePermission = true))
        val preview = fresh.copy(previewNotificationsOff = true)
        assertEquals(true, preview.showNotificationsOffBanner(canDeliver = true, requiresRuntimePermission = true))
    }

    @Test
    fun `level feedback message per answer outcome`() {
        fun msg(answer: Answer, change: LevelChange) = LevelMessage.from(LastAnswer(1L, answer, change))
        assertEquals(null, msg(Answer.GOOD, LevelChange.PROGRESSED))
        assertEquals(LevelMessage.LEVEL_UP, msg(Answer.GOOD, LevelChange.PROMOTED))
        assertEquals(LevelMessage.MAX_GOOD, msg(Answer.GOOD, LevelChange.UNCHANGED))
        assertEquals(LevelMessage.PROGRESS_RESET, msg(Answer.BAD, LevelChange.SUB_LEVEL_RESET))
        assertEquals(LevelMessage.LEVEL_DOWN, msg(Answer.BAD, LevelChange.DEMOTED))
        assertEquals(LevelMessage.FLOOR, msg(Answer.BAD, LevelChange.UNCHANGED))
    }

    @Test
    fun `interval and max level follow the SRS table`() {
        val l3 = HomeUiState.from(UserSettings(), SessionState(srs = SrsState(3, 0)), DebugOverrides(), engine)
        assertEquals(kotlin.time.Duration.parse("15m"), l3.interval)
        assertEquals(false, l3.isMaxLevel)
        val l8 = HomeUiState.from(UserSettings(), SessionState(srs = SrsState(8, 0)), DebugOverrides(), engine)
        assertEquals(kotlin.time.Duration.parse("3h"), l8.interval)
        assertEquals(true, l8.isMaxLevel)
        assertEquals(5, l8.subLevelCount)
    }

    @Test
    fun `short intervals expose the scaled interval`() {
        val state = HomeUiState.from(
            UserSettings(),
            SessionState(srs = SrsState(3, 0)),
            DebugOverrides(shortIntervals = true),
            engine,
        )
        // Debug unit tests: short intervals are available, 15 min -> 15 s.
        assertEquals(kotlin.time.Duration.parse("15s"), state.shortInterval)
    }

    @Test
    fun `exact timing banner rules`() {
        val running = HomeUiState(sessionStatus = SessionStatus.RUNNING)
        assertEquals(true, running.showExactTimingBanner(canScheduleExact = false))
        assertEquals(false, running.showExactTimingBanner(canScheduleExact = true))
        assertEquals(false, HomeUiState().showExactTimingBanner(canScheduleExact = false)) // stopped
        assertEquals(
            false,
            running.copy(exactTimingBannerDismissed = true).showExactTimingBanner(canScheduleExact = false),
        )
        assertEquals(true, running.copy(previewExactAlarmsDenied = true).showExactTimingBanner(canScheduleExact = true))
    }
}
