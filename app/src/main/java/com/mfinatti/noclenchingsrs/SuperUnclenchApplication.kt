package com.mfinatti.noclenchingsrs

import android.app.Application
import com.mfinatti.noclenchingsrs.di.AppContainer
import kotlinx.coroutines.launch

class SuperUnclenchApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Channel exists before any notify() (US-03 B3).
        container.checkInNotifier.ensureChannel()
        com.mfinatti.noclenchingsrs.feature.checkin.ring.RingNotifications.ensureChannel(this)
        // Re-arm the alarm on every process start: covers force-stop, a revoked exact-alarm
        // permission (which cancels our alarms) and any missed boot broadcast.
        container.applicationScope.launch { container.checkInController.restoreSchedule() }
    }
}
