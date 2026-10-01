package com.mfinatti.noclenchingsrs.feature.checkin.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mfinatti.noclenchingsrs.feature.checkin.domain.AlarmScheduler

/**
 * AlarmManager-backed scheduler (US-05). Exact while-idle alarms when `SCHEDULE_EXACT_ALARM` is
 * available (pre-granted on API 31–32, user-granted on 33+), otherwise an inexact while-idle alarm.
 */
class AndroidAlarmScheduler(context: Context) : AlarmScheduler {

    private val appContext: Context = context.applicationContext
    private val alarmManager: AlarmManager? = appContext.getSystemService(AlarmManager::class.java)

    /** True when the platform lets us schedule exact alarms right now. */
    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    override fun schedule(atMillis: Long, allowExact: Boolean, alarmClock: Boolean) {
        val manager = alarmManager ?: return
        val pendingIntent = alarmPendingIntent()
        if (allowExact && canScheduleExactAlarms()) {
            try {
                if (alarmClock) {
                    // US-11 B1: an alarm-clock alarm brings the device out of Doze before it fires
                    // and is never batched or deferred, like a real alarm clock.
                    manager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent()), pendingIntent)
                    return
                }
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
                return
            } catch (e: SecurityException) {
                // Permission revoked between the check and the call: fall through to inexact.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
    }

    /** Tapping the "next alarm" in the status bar / clock opens Home. */
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        appContext,
        REQUEST_SHOW,
        Intent(appContext, com.mfinatti.noclenchingsrs.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(com.mfinatti.noclenchingsrs.MainActivity.EXTRA_OPEN_HOME, true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun cancel() {
        alarmManager?.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_ALARM,
        Intent(appContext, CheckInAlarmReceiver::class.java).setAction(CheckInAlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_ALARM = 20
        const val REQUEST_SHOW = 21
    }
}
