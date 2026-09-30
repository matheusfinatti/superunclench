package com.mfinatti.noclenchingsrs.feature.home.ui

import com.mfinatti.noclenchingsrs.data.debug.DebugOverrides
import com.mfinatti.noclenchingsrs.domain.session.LastAnswer
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class HomeUiState(
    val userName: String? = null,
    val disclaimerVisible: Boolean = true,
    val sessionStatus: SessionStatus = SessionStatus.STOPPED,
    val level: Int = 1,
    val subLevel: Int = 0,
    val subLevelCount: Int = 3,
    val interval: Duration = 5.minutes,
    val maxLevel: Int = 8,
    /** Most recent Good/Bad answer, drives the transient level feedback message. */
    val lastAnswer: LastAnswer? = null,
    /** Debug short intervals: the scaled interval actually used, or null with real timings. */
    val shortInterval: Duration? = null,
    val previewExactAlarmsDenied: Boolean = false,
    val exactTimingBannerDismissed: Boolean = false,
    val nextAlarmAtMillis: Long? = null,
    /** Paused after 3 consecutive misses (US-07). */
    val autoPaused: Boolean = false,
    val consecutiveMisses: Int = 0,
    /** Home "Today" tiles (US-08 §8). */
    val todayGood: Int = 0,
    val todayBad: Int = 0,
    val todayMissed: Int = 0,
    val pausedUntilMillis: Long? = null,
    /** Fire time of the unanswered check-in, or null when none is pending. */
    val pendingCheckInAtMillis: Long? = null,
    val notificationPermissionRequested: Boolean = false,
    val previewNotificationsOff: Boolean = false,
    val previewLegacyNotificationPermission: Boolean = false,
) {
    /**
     * Whether to show the "Notifications are off" banner, given whether check-ins can actually be
     * delivered right now. On Android 13+ it stays hidden until the user has been asked once (the
     * rationale flow handles first use); it always shows while a session is running.
     */
    val isMaxLevel: Boolean get() = level >= maxLevel

    /**
     * Exact-timing banner (US-05 §2): only while running or paused, when exact alarms aren't
     * available, and not after "Not now" in this session.
     */
    fun showExactTimingBanner(canScheduleExact: Boolean): Boolean {
        if (sessionStatus == SessionStatus.STOPPED || exactTimingBannerDismissed) return false
        return previewExactAlarmsDenied || !canScheduleExact
    }

    fun showNotificationsOffBanner(canDeliver: Boolean, requiresRuntimePermission: Boolean): Boolean {
        if (previewNotificationsOff) return true
        if (canDeliver) return false
        val usesRuntimePermission = requiresRuntimePermission && !previewLegacyNotificationPermission
        return !usesRuntimePermission || notificationPermissionRequested || sessionStatus != SessionStatus.STOPPED
    }

    companion object {
        fun from(
            settings: UserSettings,
            session: SessionState,
            overrides: DebugOverrides,
            srsEngine: SrsEngine,
        ): HomeUiState {
            val srs = srsEngine.normalize(session.srs)
            return HomeUiState(
                userName = settings.name,
                disclaimerVisible = !settings.disclaimerDismissed,
                sessionStatus = session.status,
                level = srs.level,
                subLevel = srs.subLevel,
                subLevelCount = srsEngine.subLevelCount(srs.level),
                interval = srsEngine.interval(srs.level),
                maxLevel = srsEngine.maxLevel,
                lastAnswer = session.lastAnswer,
                shortInterval = if (overrides.shortIntervalsActive) {
                    overrides.intervalScale.scale(srsEngine.interval(srs.level))
                } else {
                    null
                },
                previewExactAlarmsDenied = overrides.previewExactAlarmsDenied,
                exactTimingBannerDismissed = session.exactTimingBannerDismissed,
                pendingCheckInAtMillis = session.pendingCheckInAtMillis,
                nextAlarmAtMillis = session.nextAlarmAtMillis,
                autoPaused = session.autoPaused,
                consecutiveMisses = session.consecutiveMisses,
                pausedUntilMillis = session.pausedUntilMillis,
                notificationPermissionRequested = settings.notificationPermissionRequested,
                previewNotificationsOff = overrides.previewNotificationsOff,
                previewLegacyNotificationPermission = overrides.previewLegacyNotificationPermission,
            )
        }
    }
}

/** Transient feedback shown in the level card after an answer (US-04 §3). */
enum class LevelMessage {
    LEVEL_UP,
    MAX_GOOD,
    PROGRESS_RESET,
    LEVEL_DOWN,
    FLOOR,
    ;

    companion object {
        /** Null for a plain Good without promotion: the segment fill is the feedback. */
        fun from(lastAnswer: LastAnswer): LevelMessage? = when (lastAnswer.change) {
            LevelChange.PROGRESSED -> null
            LevelChange.PROMOTED -> LEVEL_UP
            LevelChange.SUB_LEVEL_RESET -> PROGRESS_RESET
            LevelChange.DEMOTED -> LEVEL_DOWN
            LevelChange.UNCHANGED -> if (lastAnswer.answer == Answer.GOOD) MAX_GOOD else FLOOR
        }
    }
}
