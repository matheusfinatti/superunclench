package com.mfinatti.noclenchingsrs.feature.checkin.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mfinatti.noclenchingsrs.SuperUnclenchApplication
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import kotlinx.coroutines.launch

/** Runs [block] on the app scope while keeping the broadcast alive (works with the app killed). */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend (SuperUnclenchApplication) -> Unit) {
    val app = context.applicationContext as SuperUnclenchApplication
    val pending = goAsync()
    app.container.applicationScope.launch {
        try {
            block(app)
        } finally {
            pending.finish()
        }
    }
}

/** The scheduled check-in alarm fired. */
class CheckInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        runAsync(context) { app -> app.container.checkInController.fireAlarm() }
    }

    companion object {
        const val ACTION_FIRE = "com.mfinatti.noclenchingsrs.action.FIRE_CHECK_IN"
    }
}

/** Good / Bad tapped on the notification. */
class CheckInActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val checkInAt = intent.getLongExtra(EXTRA_CHECK_IN_AT, NO_CHECK_IN)
        if (checkInAt == NO_CHECK_IN) return
        val answer = when (intent.action) {
            ACTION_GOOD -> Answer.GOOD
            ACTION_BAD -> Answer.BAD
            ACTION_DISMISSED -> {
                // Swiped away / "Clear all" (the notification's deleteIntent; not sent for our own
                // cancel() or for taps): counts as Missed (US-07).
                runAsync(context) { app -> app.container.checkInController.markMissed(checkInAt) }
                return
            }
            else -> return
        }
        runAsync(context) { app ->
            app.container.checkInController.answer(answer, checkInAt, CheckInSource.NOTIFICATION)
        }
    }

    companion object {
        const val ACTION_GOOD = "com.mfinatti.noclenchingsrs.action.ANSWER_GOOD"
        const val ACTION_BAD = "com.mfinatti.noclenchingsrs.action.ANSWER_BAD"
        const val ACTION_DISMISSED = "com.mfinatti.noclenchingsrs.action.CHECK_IN_DISMISSED"
        const val EXTRA_CHECK_IN_AT = "check_in_at"
        private const val NO_CHECK_IN = -1L
    }
}

/**
 * Re-arms the check-in alarm after a reboot, an app update, or a change of the exact-alarm
 * permission (alarms are cleared in the first two cases; the third lets us switch to exact).
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> runAsync(context) { app -> app.container.checkInController.restoreSchedule() }
        }
    }
}
