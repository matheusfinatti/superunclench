package com.mfinatti.noclenchingsrs.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    checkInLogRepository: CheckInLogRepository,
) : ViewModel() {

    /** Full history, oldest first; null until loaded (no empty-state flash). */
    val events: StateFlow<List<CheckInEvent>?> = checkInLogRepository.events.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
