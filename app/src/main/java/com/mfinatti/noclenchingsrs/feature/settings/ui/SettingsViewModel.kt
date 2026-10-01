package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfinatti.noclenchingsrs.data.settings.SettingsRepository
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.feature.checkin.domain.CheckInController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val checkInController: CheckInController,
    initial: UserSettings,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = initial,
    )

    /** Already debounced and trimmed by the field; blank clears the name ("Hi there"). */
    fun onNameChange(name: String) {
        viewModelScope.launch { settingsRepository.setName(name) }
    }

    fun onThemeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** US-11: applies to the next check-in; a ring in progress keeps ringing until answered (AC12). */
    fun onAlertStyle(style: AlertStyle) {
        viewModelScope.launch { settingsRepository.setAlertStyle(style) }
    }

    /** Quiet hours changes re-evaluate a running session's next alarm immediately (US-10 AC7). */
    fun onQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            checkInController.quietHoursChanged { settingsRepository.setQuietHoursEnabled(enabled) }
        }
    }

    /** Start/end from the time pickers; the picker never lets them be equal. */
    fun onQuietHoursRange(startMinutes: Int, endMinutes: Int) {
        if (startMinutes == endMinutes) return
        viewModelScope.launch {
            checkInController.quietHoursChanged { settingsRepository.setQuietHoursRange(startMinutes, endMinutes) }
        }
    }

    /** L1 0/3, history kept; a running session continues at the L1 interval from now. */
    suspend fun resetProgress() {
        checkInController.resetProgress()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
