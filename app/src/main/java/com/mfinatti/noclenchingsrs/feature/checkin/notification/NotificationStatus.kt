package com.mfinatti.noclenchingsrs.feature.checkin.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationStatus {

    /** Runtime permission is needed on Android 13+. */
    val requiresRuntimePermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isPermissionGranted(context: Context): Boolean =
        !requiresRuntimePermission ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * True when a check-in notification can actually reach the user: permission granted, app
     * notifications enabled and the "Check-ins" channel not blocked.
     */
    fun canDeliverCheckIns(context: Context): Boolean {
        if (!isPermissionGranted(context)) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = manager.getNotificationChannel(AndroidCheckInNotifier.CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManagerCompat.IMPORTANCE_NONE) return false
        }
        return true
    }

    /** The app's system notification settings (app details screen on API 24–25). */
    fun appNotificationSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
