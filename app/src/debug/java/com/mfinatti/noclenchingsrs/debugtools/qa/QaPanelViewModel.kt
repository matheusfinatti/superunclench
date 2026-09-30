package com.mfinatti.noclenchingsrs.debugtools.qa

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.data.debug.DebugOverrides
import com.mfinatti.noclenchingsrs.di.AppContainer
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.SrsLevel
import com.mfinatti.noclenchingsrs.feature.checkin.domain.AnswerResult
import com.mfinatti.noclenchingsrs.feature.checkin.domain.FireResult
import com.mfinatti.noclenchingsrs.feature.checkin.domain.MissResult
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.feature.stats.domain.SampleHistory
import java.util.TimeZone
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Panel actions implemented so far (US-02, US-03). */
enum class QaAction {
    SET_NAME_ALEX,
    CLEAR_NAME,
    CYCLE_THEME,
    SHOW_DISCLAIMER,
    FIRE_ALARM,
    ANSWER_GOOD,
    ANSWER_BAD,
    SUB_LEVEL_PLUS,
    TOGGLE_SHORT_INTERVALS,
    TOGGLE_AUTO_PAUSE,
    MARK_MISSED,
    SEED_HISTORY,
    CLEAR_HISTORY,
    END_PAUSE,
    SIMULATE_REBOOT,
    TOGGLE_NOTIFICATION_STYLE,
    TOGGLE_PREVIEW_EXACT_DENIED,
    TOGGLE_PREVIEW_NOTIFICATIONS_OFF,
    TOGGLE_PREVIEW_LEGACY_PERMISSION,
    RESET_ALL,
    RESET_PROGRESS,
}

/** Snackbar text as a string resource with format args, or a theme-mode label argument. */
data class QaMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
    val themeArg: ThemeMode? = null,
)

/** Everything the state readout shows, captured from the repositories. */
data class QaReadout(
    val settings: UserSettings,
    val session: SessionState,
    val levelInfo: SrsLevel,
    val historyCount: Int,
    val overrides: DebugOverrides = DebugOverrides(),
    /** Newest last, at most [RECENT_EVENTS] entries. */
    val recentEvents: List<CheckInEvent> = emptyList(),
)

private const val RECENT_EVENTS = 10

class QaPanelViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val readout: StateFlow<QaReadout?> = combine(
        container.settingsRepository.settings,
        container.sessionRepository.session,
        container.checkInLogRepository.events,
        container.debugOverridesRepository.overrides,
    ) { settings, session, events, overrides ->
        QaReadout(
            settings = settings,
            session = session,
            levelInfo = container.srsEngine.levelInfo(session.srs.level),
            historyCount = events.size,
            overrides = overrides,
            recentEvents = events.takeLast(RECENT_EVENTS),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
    )

    private val messageChannel = Channel<QaMessage>(Channel.BUFFERED)

    /** One-off snackbar confirmations. */
    val messages: Flow<QaMessage> = messageChannel.receiveAsFlow()

    fun onAction(action: QaAction) {
        viewModelScope.launch {
            messageChannel.send(perform(action))
        }
    }

    private suspend fun perform(action: QaAction): QaMessage = when (action) {
        QaAction.SET_NAME_ALEX -> {
            container.settingsRepository.setName(QA_NAME)
            QaMessage(R.string.qa_msg_name_set)
        }

        QaAction.CLEAR_NAME -> {
            container.settingsRepository.setName(null)
            QaMessage(R.string.qa_msg_name_cleared)
        }

        QaAction.CYCLE_THEME -> {
            val next = container.settingsRepository.settings.first().themeMode.next()
            container.settingsRepository.setThemeMode(next)
            QaMessage(R.string.qa_msg_theme, themeArg = next)
        }

        QaAction.SHOW_DISCLAIMER -> {
            container.settingsRepository.setDisclaimerDismissed(false)
            QaMessage(R.string.qa_msg_disclaimer)
        }

        QaAction.FIRE_ALARM -> when (container.checkInController.fireAlarm(force = true)) {
            FireResult.NotRunning, FireResult.Stale -> QaMessage(R.string.qa_msg_not_running)
            is FireResult.Fired -> QaMessage(R.string.qa_msg_alarm_fired)
            FireResult.AutoPaused -> QaMessage(R.string.qa_msg_auto_paused)
            FireResult.PauseEnded -> QaMessage(R.string.qa_msg_pause_ended)
        }

        QaAction.SEED_HISTORY -> {
            // Deterministic 30-day dataset (US-08 §9); replaces history, level/session untouched.
            val events = SampleHistory.generate(
                nowMillis = System.currentTimeMillis(),
                timeZone = TimeZone.getDefault(),
                srsEngine = container.srsEngine,
            )
            container.checkInLogRepository.replaceAll(events)
            QaMessage(R.string.qa_seeded, formatArgs = listOf(events.size))
        }

        QaAction.CLEAR_HISTORY -> {
            container.checkInLogRepository.clear()
            QaMessage(R.string.qa_history_cleared)
        }

        QaAction.MARK_MISSED -> {
            // Same path as swiping the notification away (its deleteIntent), for tap-only QA.
            val pendingAt = container.sessionRepository.session.first().pendingCheckInAtMillis
            val result = if (pendingAt == null) {
                MissResult.NoPending
            } else {
                container.checkInController.markMissed(pendingAt, CheckInSource.DISMISSED)
            }
            when (result) {
                MissResult.NoPending -> QaMessage(R.string.qa_no_pending)
                is MissResult.Missed -> QaMessage(
                    if (result.autoPaused) R.string.qa_marked_missed_autopaused else R.string.qa_marked_missed,
                    formatArgs = listOf(result.consecutiveMisses),
                )
            }
        }

        QaAction.TOGGLE_AUTO_PAUSE -> {
            val enabled = !container.debugOverridesRepository.overrides.first().autoPause
            container.debugOverridesRepository.setAutoPause(enabled)
            QaMessage(if (enabled) R.string.qa_msg_auto_pause_on else R.string.qa_msg_auto_pause_off)
        }

        QaAction.END_PAUSE -> {
            val before = container.sessionRepository.session.first()
            if (before.status == SessionStatus.PAUSED) {
                container.checkInController.resume()
                QaMessage(R.string.qa_msg_pause_ended)
            } else {
                QaMessage(R.string.qa_msg_not_paused)
            }
        }

        QaAction.ANSWER_GOOD -> answer(Answer.GOOD)

        QaAction.ANSWER_BAD -> answer(Answer.BAD)

        QaAction.SUB_LEVEL_PLUS -> {
            val srs = container.checkInController.incrementSubLevel().srs
            QaMessage(
                R.string.qa_msg_sub_level,
                formatArgs = listOf(srs.subLevel, container.srsEngine.subLevelCount(srs.level)),
            )
        }

        QaAction.TOGGLE_SHORT_INTERVALS -> {
            val enabled = !container.debugOverridesRepository.overrides.first().shortIntervals
            container.debugOverridesRepository.setShortIntervals(enabled)
            // Re-time a running session's next alarm with the new scale.
            container.checkInController.rescheduleFromNow()
            QaMessage(if (enabled) R.string.qa_msg_short_on else R.string.qa_msg_short_off)
        }

        QaAction.SIMULATE_REBOOT -> {
            // Same path as BOOT_COMPLETED: alarms are gone after a reboot, then re-armed.
            container.alarmScheduler.cancel()
            val state = container.checkInController.restoreSchedule()
            val next = state.nextAlarmAtMillis
            if (state.status == SessionStatus.RUNNING && next != null) {
                QaMessage(R.string.qa_msg_reboot_next, formatArgs = listOf(QaFormat.clockTime(next)))
            } else {
                QaMessage(R.string.qa_msg_reboot_not_running)
            }
        }

        QaAction.TOGGLE_NOTIFICATION_STYLE -> {
            val standard = !container.debugOverridesRepository.overrides.first().standardNotificationButtons
            container.debugOverridesRepository.setStandardNotificationButtons(standard)
            QaMessage(if (standard) R.string.qa_msg_notif_style_standard else R.string.qa_msg_notif_style_custom)
        }

        QaAction.TOGGLE_PREVIEW_EXACT_DENIED -> {
            val enabled = !container.debugOverridesRepository.overrides.first().previewExactAlarmsDenied
            container.debugOverridesRepository.setPreviewExactAlarmsDenied(enabled)
            container.checkInController.restoreSchedule()
            QaMessage(if (enabled) R.string.qa_msg_preview_exact_on else R.string.qa_msg_preview_exact_off)
        }

        QaAction.TOGGLE_PREVIEW_NOTIFICATIONS_OFF -> {
            val enabled = !container.debugOverridesRepository.overrides.first().previewNotificationsOff
            container.debugOverridesRepository.setPreviewNotificationsOff(enabled)
            QaMessage(if (enabled) R.string.qa_msg_preview_notif_off_on else R.string.qa_msg_preview_notif_off_off)
        }

        QaAction.TOGGLE_PREVIEW_LEGACY_PERMISSION -> {
            val enabled =
                !container.debugOverridesRepository.overrides.first().previewLegacyNotificationPermission
            container.debugOverridesRepository.setPreviewLegacyNotificationPermission(enabled)
            QaMessage(if (enabled) R.string.qa_msg_preview_api32_on else R.string.qa_msg_preview_api32_off)
        }

        QaAction.RESET_ALL -> {
            container.resetAllData()
            QaMessage(R.string.qa_msg_reset_all)
        }

        QaAction.RESET_PROGRESS -> {
            container.checkInController.resetProgress()
            QaMessage(R.string.qa_msg_reset_progress)
        }
    }

    /** Debug "Set level N": level N, sub-level 0. */
    fun onSetLevel(level: Int) {
        viewModelScope.launch {
            val srs = container.checkInController.setLevel(level).srs
            messageChannel.send(QaMessage(R.string.qa_msg_level_set, formatArgs = listOf(srs.level)))
        }
    }

    /** Applies the SRS rule even without a pending check-in (Decisions log, 2026-09-30). */
    private suspend fun answer(answer: Answer): QaMessage {
        val result = container.checkInController.answer(answer, checkInAtMillis = null)
        val srs = (result as? AnswerResult.Applied)?.srs
            ?: container.sessionRepository.session.first().srs
        return QaMessage(
            resId = if (answer == Answer.GOOD) R.string.qa_msg_answered_good else R.string.qa_msg_answered_bad,
            formatArgs = listOf(srs.level, srs.subLevel, container.srsEngine.subLevelCount(srs.level)),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val QA_NAME = "Alex"
    }
}
