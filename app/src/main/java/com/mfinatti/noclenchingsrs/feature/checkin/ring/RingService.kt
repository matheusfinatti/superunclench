package com.mfinatti.noclenchingsrs.feature.checkin.ring

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mfinatti.noclenchingsrs.SuperUnclenchApplication
import kotlinx.coroutines.launch

/**
 * US-11 Ring: keeps the ringing check-in notification in the foreground until it is resolved.
 *
 * **Who makes the noise (US-11 B1).** The sound is the ring channel's alarm sound, played *by the
 * system* (NotificationManager → SystemUI ringtone player) on the alarm stream, looping because the
 * notification is FLAG_INSISTENT; the channel's vibration loops with it. A first version played a
 * MediaPlayer in this service, but on Android 16+ "audio hardening" mutes playback of apps without
 * the foreground-audio capability, which a foreground service only gets when started while the app
 * is visible — never from an alarm (alarm deliveries carry no background-activity-start rights), so
 * the first locked ring was muted for up to 30 s. System-played notification sound isn't subject to
 * that, starts the instant the notification is posted, follows the user's DND alarm setting, and
 * is silenced natively by the volume keys (`silenceNotificationSound`).
 *
 * **What the service still does:** keeps the process alive and unfrozen so Silence works from the
 * power key (screen off) and volume keys; vibrates itself in *silent* ringer mode, where the system
 * skips notification vibration; and owns the notification so it stays until resolved. It never
 * re-posts a ringing notification (an update would re-trigger or clear the sound); Silence posts an
 * ONLY_ALERT_ONCE update, which makes the system stop the insistent sound and vibration.
 *
 * **FGS type** (Android 14+): `systemExempted`, documented for apps holding SCHEDULE_EXACT_ALARM that
 * use a foreground service to continue alarms; no runtime limit, allowed from boot. Without exact
 * alarms it isn't allowed, so `mediaPlayback` is the fallback (API 29–33: always). If the platform
 * refuses the start, the notifier posts the same ringing notification directly.
 */
class RingService : Service() {

    private var vibrator: Vibrator? = null
    private var receiverRegistered = false
    private var checkInAtMillis = NO_CHECK_IN
    private var fullScreen = true

    /** Power key (screen off) or a volume key while ringing = Silence (US-11 AC6). */
    private val silencer = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!ringingNow) return
            val app = applicationContext as SuperUnclenchApplication
            app.container.applicationScope.launch { app.container.checkInController.silenceRing() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // notify() only updates our own notification; without the permission Android doesn't show it.
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                val checkInAt = intent.getLongExtra(EXTRA_CHECK_IN_AT, NO_CHECK_IN)
                val silenced = intent.getBooleanExtra(EXTRA_SILENCED, false)
                fullScreen = intent.getBooleanExtra(EXTRA_FULL_SCREEN, true)
                if (running && checkInAt == checkInAtMillis) {
                    // Same check-in already ringing: re-posting would restart or clear the sound.
                    return START_NOT_STICKY
                }
                checkInAtMillis = checkInAt
                val notification = RingNotifications.build(this, checkInAt, fullScreen, silenced)
                if (!startInForeground(notification)) {
                    runCatching { NotificationManagerCompat.from(this).notify(RingNotifications.NOTIFICATION_ID, notification) }
                    stopSelf()
                    return START_NOT_STICKY
                }
                running = true
                ringingNow = !silenced
                registerSilencer()
                if (!silenced) vibrateIfRingerSilent()
            }
            ACTION_SILENCE -> {
                ringingNow = false
                stopVibration()
                if (running) {
                    runCatching {
                        NotificationManagerCompat.from(this).notify(
                            RingNotifications.NOTIFICATION_ID,
                            RingNotifications.build(this, checkInAtMillis, fullScreen, silenced = true),
                        )
                    }
                } else {
                    stopSelf()
                }
            }
            else -> if (!running) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopVibration()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(silencer) }
            receiverRegistered = false
        }
        running = false
        ringingNow = false
        // Removing the notification also stops the system's insistent sound and vibration.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification): Boolean {
        for (type in foregroundTypes()) {
            try {
                ServiceCompat.startForeground(this, RingNotifications.NOTIFICATION_ID, notification, type)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "startForeground(type=$type) refused", e)
            }
        }
        return false
    }

    private fun foregroundTypes(): List<Int> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val exact = getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            listOfNotNull(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED.takeIf { exact },
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else -> listOf(0)
    }

    private fun registerSilencer() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(VOLUME_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(this, silencer, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    /**
     * In *silent* ringer mode the system plays the alarm-stream sound but skips notification
     * vibration (US-11 AC6 wants both), so vibrate here with alarm usage. Normal/vibrate modes use
     * the channel's own looping vibration.
     */
    private fun vibrateIfRingerSilent() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_SILENT) return
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!v.hasVibrator()) return
        vibrator = v
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> v.vibrate(
                VibrationEffect.createWaveform(RingNotifications.VIBRATION_PATTERN, 0),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                v.vibrate(
                    VibrationEffect.createWaveform(RingNotifications.VIBRATION_PATTERN, 0),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
            }
            else -> {
                @Suppress("DEPRECATION")
                v.vibrate(RingNotifications.VIBRATION_PATTERN, 0)
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    companion object {
        private const val TAG = "RingService"
        private const val NO_CHECK_IN = -1L
        const val ACTION_RING = "com.mfinatti.noclenchingsrs.action.RING"
        const val ACTION_SILENCE = "com.mfinatti.noclenchingsrs.action.SILENCE_RING"
        const val EXTRA_CHECK_IN_AT = "check_in_at"
        const val EXTRA_FULL_SCREEN = "full_screen"
        const val EXTRA_SILENCED = "silenced"

        /** Not public API, but sent by the system for every volume-key change. */
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

        /** The service is in the foreground (ring notification posted). */
        @Volatile
        var running: Boolean = false
            private set

        /** The check-in is ringing (not silenced) — QA readout, tests. */
        @Volatile
        var ringingNow: Boolean = false
            private set

        fun ringIntent(context: Context, checkInAtMillis: Long, fullScreen: Boolean, silenced: Boolean): Intent =
            Intent(context, RingService::class.java)
                .setAction(ACTION_RING)
                .putExtra(EXTRA_CHECK_IN_AT, checkInAtMillis)
                .putExtra(EXTRA_FULL_SCREEN, fullScreen)
                .putExtra(EXTRA_SILENCED, silenced)
                .setData(Uri.parse("superunclench://ring/$checkInAtMillis"))

        fun silenceIntent(context: Context): Intent =
            Intent(context, RingService::class.java).setAction(ACTION_SILENCE)
    }
}
