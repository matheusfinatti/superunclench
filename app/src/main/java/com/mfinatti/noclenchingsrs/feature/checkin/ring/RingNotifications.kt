package com.mfinatti.noclenchingsrs.feature.checkin.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.feature.checkin.alarm.CheckInActionReceiver

/** Android 14+ full-screen intent permission (US-11 AC9). Always granted at install on ≤ 13. */
object FullScreenStatus {
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
    }

    /** The system "Full screen notifications" page for this app (app details on ≤ 13). */
    fun settingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * The ringing check-in notification (US-11 §4): "Check-in alarms" channel, CATEGORY_ALARM, ongoing,
 * Good/Bad pills + Silence, body tap → alarm screen, full-screen intent → alarm screen (when allowed).
 *
 * Sound and vibration are the channel's (alarm sound on the alarm stream, soft long pulses), played
 * by the system and looped by FLAG_INSISTENT until the notification is updated "silenced" (an
 * ONLY_ALERT_ONCE update stops an insistent sound) or removed. See [RingService] for why (US-11 B1).
 */
object RingNotifications {
    /**
     * v2: channel sound/usage can't change after creation; the first US-11 build's silent
     * "checkin_alarms" channel is deleted in [ensureChannel].
     */
    const val CHANNEL_ID = "checkin_alarms_v2"
    private const val LEGACY_CHANNEL_ID = "checkin_alarms"

    /** Soft long pulses, ~1.4 s cycle (US-11 §6), distinct from Nudge's two short buzzes. */
    val VIBRATION_PATTERN = longArrayOf(0L, 600L, 800L)
    const val NOTIFICATION_ID = 1002
    private const val REQUEST_ALARM_SCREEN = 20
    private const val REQUEST_FULL_SCREEN = 21
    private const val REQUEST_GOOD = 22
    private const val REQUEST_BAD = 23
    private const val REQUEST_SILENCE = 24

    /** Creates the "Check-in alarms" channel (API 26+). Safe to call repeatedly. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ring_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.ring_channel_desc)
            // The device's alarm sound (follows the user's choice), on the alarm stream: rings in
            // silent/vibrate mode at alarm volume, and DND treats it as an alarm.
            setSound(
                Settings.System.DEFAULT_ALARM_ALERT_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(channel)
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }

    fun build(
        context: Context,
        checkInAtMillis: Long,
        fullScreen: Boolean,
        silenced: Boolean,
    ): Notification {
        val good = answerIntent(context, Answer.GOOD, checkInAtMillis)
        val bad = answerIntent(context, Answer.BAD, checkInAtMillis)
        val silence = silenceIntent(context, checkInAtMillis)
        val expanded = RemoteViews(context.packageName, R.layout.notification_ring_expanded).apply {
            setOnClickPendingIntent(R.id.notif_pill_good, good)
            setOnClickPendingIntent(R.id.notif_pill_bad, bad)
            setContentDescription(R.id.notif_pill_good, context.getString(R.string.answer_good_a11y))
            setContentDescription(R.id.notif_pill_bad, context.getString(R.string.answer_bad_a11y))
            if (silenced) {
                setTextViewText(R.id.notif_silence, context.getString(R.string.alarm_silenced))
                setTextViewText(R.id.notif_body, context.getString(R.string.alarm_still_waiting))
            } else {
                setOnClickPendingIntent(R.id.notif_silence, silence)
                setContentDescription(R.id.notif_silence, context.getString(R.string.alarm_silence_a11y))
            }
        }
        val collapsed = RemoteViews(context.packageName, R.layout.notification_ring_collapsed)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setColor(ContextCompat.getColor(context, R.color.notif_accent))
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.ring_notif_collapsed_text))
            .setSubText(context.getString(if (silenced) R.string.ring_notif_subtext_silenced else R.string.ring_notif_subtext))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(checkInAtMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVibrate(VIBRATION_PATTERN)
            // US-11 B1: Android 12+ defers a foreground service's notification by ~10 s when the app
            // isn't in the foreground — that deferral delayed the sound and the full-screen launch
            // of every "cold" ring. An alarm must show at once.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // The "silenced" update must not alert: on an insistent notification that makes the
            // system stop the looping sound and vibration (Silence).
            .setOnlyAlertOnce(silenced)
            .setContentIntent(alarmScreenIntent(context, checkInAtMillis, REQUEST_ALARM_SCREEN))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomHeadsUpContentView(expanded)
            .setCustomBigContentView(expanded)
            .extend(
                NotificationCompat.WearableExtender()
                    .addAction(NotificationCompat.Action.Builder(R.drawable.ic_good, context.getString(R.string.answer_good), good).build())
                    .addAction(NotificationCompat.Action.Builder(R.drawable.ic_bad, context.getString(R.string.answer_bad), bad).build()),
            )
        if (fullScreen) {
            builder.setFullScreenIntent(alarmScreenIntent(context, checkInAtMillis, REQUEST_FULL_SCREEN), true)
        }
        return builder.build().apply {
            // Not cleared by "Clear all"; the sound and vibration repeat until answered/silenced.
            // INSISTENT stays on the silenced update too: that is what makes the system clear it.
            flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_INSISTENT
        }
    }

    fun alarmScreenIntent(context: Context, checkInAtMillis: Long, requestCode: Int = REQUEST_ALARM_SCREEN): PendingIntent {
        val intent = AlarmActivity.intent(context, checkInAtMillis)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun answerIntent(context: Context, answer: Answer, checkInAtMillis: Long): PendingIntent {
        val intent = Intent(context, CheckInActionReceiver::class.java)
            .setAction(if (answer == Answer.GOOD) CheckInActionReceiver.ACTION_GOOD else CheckInActionReceiver.ACTION_BAD)
            .putExtra(CheckInActionReceiver.EXTRA_CHECK_IN_AT, checkInAtMillis)
            .setData(Uri.parse("superunclench://ring/$checkInAtMillis/${answer.name.lowercase()}"))
        return PendingIntent.getBroadcast(
            context,
            if (answer == Answer.GOOD) REQUEST_GOOD else REQUEST_BAD,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun silenceIntent(context: Context, checkInAtMillis: Long): PendingIntent {
        val intent = Intent(context, CheckInActionReceiver::class.java)
            .setAction(CheckInActionReceiver.ACTION_SILENCE)
            .putExtra(CheckInActionReceiver.EXTRA_CHECK_IN_AT, checkInAtMillis)
            .setData(Uri.parse("superunclench://ring/$checkInAtMillis/silence"))
        return PendingIntent.getBroadcast(
            context,
            REQUEST_SILENCE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
