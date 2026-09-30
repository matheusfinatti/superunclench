package com.mfinatti.noclenchingsrs.di

import android.content.Context
import com.mfinatti.noclenchingsrs.core.time.AppClock
import com.mfinatti.noclenchingsrs.data.checkin.CheckInLogRepository
import com.mfinatti.noclenchingsrs.data.debug.DebugOverridesRepository
import com.mfinatti.noclenchingsrs.data.debugOverridesDataStore
import com.mfinatti.noclenchingsrs.data.session.SessionRepository
import com.mfinatti.noclenchingsrs.data.sessionDataStore
import com.mfinatti.noclenchingsrs.data.settings.SettingsRepository
import com.mfinatti.noclenchingsrs.data.settingsDataStore
import com.mfinatti.noclenchingsrs.debugtools.DebugTools
import com.mfinatti.noclenchingsrs.debugtools.DebugToolsProvider
import com.mfinatti.noclenchingsrs.domain.session.SessionEngine
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.feature.checkin.alarm.AndroidAlarmScheduler
import com.mfinatti.noclenchingsrs.feature.checkin.domain.CheckInController
import com.mfinatti.noclenchingsrs.feature.checkin.domain.SchedulingPolicy
import com.mfinatti.noclenchingsrs.feature.checkin.notification.AndroidCheckInNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.io.File

/**
 * Manual dependency container (no Hilt/kapt/KSP). One instance per process, owned by
 * [com.mfinatti.noclenchingsrs.SuperUnclenchApplication].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Process-lifetime scope for hot flows and fire-and-forget writes. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock: AppClock = AppClock.System

    val srsEngine: SrsEngine = SrsEngine()

    val sessionEngine: SessionEngine = SessionEngine(srsEngine)

    val settingsRepository: SettingsRepository = SettingsRepository(appContext.settingsDataStore)

    val sessionRepository: SessionRepository = SessionRepository(appContext.sessionDataStore, srsEngine)

    val checkInLogRepository: CheckInLogRepository =
        CheckInLogRepository(File(appContext.filesDir, CHECK_IN_LOG_FILE))

    val debugOverridesRepository: DebugOverridesRepository =
        DebugOverridesRepository(appContext.debugOverridesDataStore)

    val checkInNotifier: AndroidCheckInNotifier = AndroidCheckInNotifier(appContext)

    val alarmScheduler: AndroidAlarmScheduler = AndroidAlarmScheduler(appContext)

    val checkInController: CheckInController = CheckInController(
        sessionRepository = sessionRepository,
        checkInLog = checkInLogRepository,
        sessionEngine = sessionEngine,
        scheduler = alarmScheduler,
        notifier = checkInNotifier,
        clock = clock,
        policy = { schedulingPolicy() },
    )

    /** The single place debug switches turn into timing/presentation policy (release: defaults). */
    suspend fun schedulingPolicy(): SchedulingPolicy {
        val overrides = debugOverridesRepository.overrides.first()
        return SchedulingPolicy(
            scale = overrides.intervalScale,
            allowExact = !overrides.previewExactAlarmsDenied,
            customNotificationLayout = !overrides.standardNotificationButtons,
            autoPause = overrides.autoPause,
        )
    }

    /**
     * Settings as a hot StateFlow: null until DataStore's first emission. The activity waits for a
     * non-null value before drawing so the theme and disclaimer never flash.
     */
    val settingsState: StateFlow<UserSettings?> =
        settingsRepository.settings.stateIn(applicationScope, SharingStarted.Eagerly, null)

    /** Debug-only tooling; a no-op in release builds (see src/debug and src/release). */
    val debugTools: DebugTools by lazy { DebugToolsProvider.create(this) }

    /** Everything back to a fresh install: alarms, notification, settings, session/SRS state and history. */
    suspend fun resetAllData() {
        checkInController.cancelAllSideEffects()
        debugOverridesRepository.clear()
        settingsRepository.clear()
        sessionRepository.reset()
        checkInLogRepository.clear()
    }

    private companion object {
        const val CHECK_IN_LOG_FILE = "check_in_log.json"
    }
}
