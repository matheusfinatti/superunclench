package com.mfinatti.noclenchingsrs.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfinatti.noclenchingsrs.data.debug.DebugOverridesRepository
import com.mfinatti.noclenchingsrs.data.session.SessionRepository
import com.mfinatti.noclenchingsrs.data.settings.SettingsRepository
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.feature.checkin.domain.CheckInController
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsCalculator
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsFrame
import java.util.TimeZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    sessionRepository: SessionRepository,
    debugOverridesRepository: DebugOverridesRepository,
    private val checkInController: CheckInController,
    checkInLogRepository: CheckInLogRepository,
    srsEngine: SrsEngine,
) : ViewModel() {

    /** Null until settings, session and overrides have been read once (avoids a flash of wrong state). */
    val uiState: StateFlow<HomeUiState?> = combine(
        settingsRepository.settings,
        sessionRepository.session,
        debugOverridesRepository.overrides,
        checkInLogRepository.events,
    ) { settings, session, overrides, events ->
        val today = StatsCalculator.compute(events, StatsFrame.TODAY, System.currentTimeMillis(), TimeZone.getDefault())
        HomeUiState.from(settings, session, overrides, srsEngine).copy(
            todayGood = today.good,
            todayBad = today.bad,
            todayMissed = today.missed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
    )

    fun onDismissDisclaimer() {
        viewModelScope.launch { settingsRepository.setDisclaimerDismissed(true) }
    }

    /** Starts the session; returns the new state (Home shows a note when starting in quiet hours). */
    suspend fun start(): SessionState = checkInController.start()

    /** US-11: Silence on the pending card. */
    fun onSilenceRing() {
        viewModelScope.launch { checkInController.silenceRing() }
    }

    fun onStop() {
        viewModelScope.launch { checkInController.stop() }
    }

    /** Pause for [duration] (null = until resumed); returns the new state for the snackbar. */
    suspend fun pause(duration: kotlin.time.Duration?): SessionState = checkInController.pause(duration)

    fun onResume() {
        viewModelScope.launch { checkInController.resume() }
    }

    fun onAnswer(answer: Answer, checkInAtMillis: Long) {
        viewModelScope.launch { checkInController.answer(answer, checkInAtMillis, CheckInSource.CARD) }
    }

    fun onDismissExactTimingBanner() {
        viewModelScope.launch { checkInController.dismissExactTimingBanner() }
    }

    fun onNotificationPermissionRequested() {
        viewModelScope.launch { settingsRepository.setNotificationPermissionRequested(true) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
