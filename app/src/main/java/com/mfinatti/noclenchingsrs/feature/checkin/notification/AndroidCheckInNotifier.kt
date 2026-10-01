package com.mfinatti.noclenchingsrs.feature.checkin.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mfinatti.noclenchingsrs.MainActivity
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.feature.checkin.alarm.CheckInActionReceiver
import com.mfinatti.noclenchingsrs.feature.checkin.domain.CheckInNotifier
import com.mfinatti.noclenchingsrs.feature.checkin.ring.RingNotifications
import com.mfinatti.noclenchingsrs.feature.checkin.ring.RingService
import kotlin.time.Duration

/**
 * Posts the heads-up check-in notification (US-03 design §4, Option A: standard actions with
 * coloured bold titles). Not full-screen.
 */
class AndroidCheckInNotifier(context: Context) : CheckInNotifier {

    private val appContext: Context = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    /** Creates the "Check-ins" channel (API 26+). Safe to call repeatedly. */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.notif_channel_desc)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        appContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    // No sub-text: on API 31+ it shares the header line and truncates the title (US-03 §4.2).
    @SuppressLint("MissingPermission") // Checked by canPost().
    override fun show(checkInAtMillis: Long, level: Int, interval: Duration, customLayout: Boolean) {
        if (!canPost()) return
        ensureChannel()
        val goodIntent = answerIntent(Answer.GOOD, checkInAtMillis)
        val badIntent = answerIntent(Answer.BAD, checkInAtMillis)
        val goodAction = NotificationCompat.Action.Builder(
            R.drawable.ic_good,
            coloredLabel(R.string.answer_good, R.color.notif_good),
            goodIntent,
        ).build()
        val badAction = NotificationCompat.Action.Builder(
            R.drawable.ic_bad,
            coloredLabel(R.string.answer_bad, R.color.notif_bad),
            badIntent,
        ).build()
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setColor(ContextCompat.getColor(appContext, R.color.notif_accent))
            // Title/text stay set: used by the lock screen, accessibility and bridged devices.
            .setContentTitle(appContext.getString(R.string.notif_title))
            .setContentText(appContext.getString(R.string.notif_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVibrate(VIBRATION_PATTERN)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(checkInAtMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(openHomeIntent())
            .setDeleteIntent(dismissIntent(checkInAtMillis))
        if (customLayout) {
            // Option B (US-03 design sign-off): green/red pills in a decorated custom view. No phone
            // actions (they would duplicate the pills); watches/Auto still get them via the extender.
            val expanded = expandedView(goodIntent, badIntent)
            builder
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(RemoteViews(appContext.packageName, R.layout.notification_checkin_collapsed))
                .setCustomHeadsUpContentView(expanded)
                .setCustomBigContentView(expanded)
                .extend(NotificationCompat.WearableExtender().addAction(goodAction).addAction(badAction))
        } else {
            // Option A fallback: standard actions with coloured, bold titles.
            builder.addAction(goodAction).addAction(badAction)
        }
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun expandedView(goodIntent: PendingIntent, badIntent: PendingIntent): RemoteViews =
        RemoteViews(appContext.packageName, R.layout.notification_checkin_expanded).apply {
            setOnClickPendingIntent(R.id.notif_pill_good, goodIntent)
            setOnClickPendingIntent(R.id.notif_pill_bad, badIntent)
            setContentDescription(R.id.notif_pill_good, appContext.getString(R.string.answer_good_a11y))
            setContentDescription(R.id.notif_pill_bad, appContext.getString(R.string.answer_bad_a11y))
        }

    /** Last ring shown in this process (to re-post the fallback notification as "Silenced"). */
    @Volatile
    private var lastRing: Pair<Long, Boolean>? = null

    /**
     * US-11 Ring: starts [RingService] (alarm sound + vibration + ringing notification with a
     * full-screen intent). If the platform refuses a background foreground-service start, posts an
     * insistent notification instead so the check-in still repeats until answered.
     */
    override fun showRing(checkInAtMillis: Long, level: Int, capAtMillis: Long, fullScreen: Boolean, silenced: Boolean) {
        RingNotifications.ensureChannel(appContext)
        manager.cancel(NOTIFICATION_ID)
        lastRing = checkInAtMillis to fullScreen
        try {
            ContextCompat.startForegroundService(
                appContext,
                RingService.ringIntent(appContext, checkInAtMillis, fullScreen, silenced),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Ring service refused; posting the ringing notification directly", e)
            postRingFallback(checkInAtMillis, fullScreen, silenced)
        }
    }

    override fun silenceRing() {
        if (RingService.running) {
            try {
                appContext.startService(RingService.silenceIntent(appContext))
                return
            } catch (e: Exception) {
                Log.w(TAG, "Can't reach ring service", e)
            }
        }
        lastRing?.let { (checkInAt, fullScreen) ->
            // No service: the "silenced" ONLY_ALERT_ONCE update makes the system stop the sound.
            postRingFallback(checkInAt, fullScreen, silenced = true)
        }
    }

    @SuppressLint("MissingPermission") // Checked by canPost().
    private fun postRingFallback(checkInAtMillis: Long, fullScreen: Boolean, silenced: Boolean) {
        if (!canPost()) return
        manager.notify(
            RingNotifications.NOTIFICATION_ID,
            RingNotifications.build(appContext, checkInAtMillis, fullScreen, silenced),
        )
    }

    override fun cancel() {
        manager.cancel(NOTIFICATION_ID)
        // Stopping the service stops sound/vibration and removes its notification; the alarm
        // screen closes itself when the session no longer has a ringing check-in.
        appContext.stopService(Intent(appContext, RingService::class.java))
        manager.cancel(RingNotifications.NOTIFICATION_ID)
        lastRing = null
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return manager.areNotificationsEnabled()
    }

    private fun coloredLabel(labelRes: Int, @ColorRes colorRes: Int): CharSequence {
        val label = appContext.getString(labelRes)
        return SpannableString(label).apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(appContext, colorRes)),
                0,
                label.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(StyleSpan(Typeface.BOLD), 0, label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun openHomeIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        return PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN_HOME,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dismissIntent(checkInAtMillis: Long): PendingIntent {
        val intent = Intent(appContext, CheckInActionReceiver::class.java)
            .setAction(CheckInActionReceiver.ACTION_DISMISSED)
            .putExtra(CheckInActionReceiver.EXTRA_CHECK_IN_AT, checkInAtMillis)
            .setData(Uri.parse("superunclench://check-in/$checkInAtMillis/dismissed"))
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_DISMISSED,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun answerIntent(answer: Answer, checkInAtMillis: Long): PendingIntent {
        val intent = Intent(appContext, CheckInActionReceiver::class.java)
            .setAction(if (answer == Answer.GOOD) CheckInActionReceiver.ACTION_GOOD else CheckInActionReceiver.ACTION_BAD)
            .putExtra(CheckInActionReceiver.EXTRA_CHECK_IN_AT, checkInAtMillis)
            // Unique data per check-in: each notification gets its own PendingIntent, so an action
            // on an old notification can never be re-targeted to a newer check-in by
            // FLAG_UPDATE_CURRENT (the stale-answer check then rejects it).
            .setData(Uri.parse("superunclench://check-in/$checkInAtMillis/${answer.name.lowercase()}"))
        return PendingIntent.getBroadcast(
            appContext,
            if (answer == Answer.GOOD) REQUEST_GOOD else REQUEST_BAD,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "CheckInNotifier"
        const val CHANNEL_ID = "checkins"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_HOME = 10
        private const val REQUEST_GOOD = 11
        private const val REQUEST_BAD = 12
        private const val REQUEST_DISMISSED = 13
        private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)
    }
}
