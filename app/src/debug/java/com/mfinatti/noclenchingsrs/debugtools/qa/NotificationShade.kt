package com.mfinatti.noclenchingsrs.debugtools.qa

import android.annotation.SuppressLint
import android.content.Context

/**
 * Expands the notification shade via the hidden StatusBarManager API (needs EXPAND_STATUS_BAR,
 * declared in the debug manifest only). Returns false if the platform blocks the call; QA can
 * then fall back to `adb shell cmd statusbar expand-notifications`.
 */
@SuppressLint("WrongConstant", "PrivateApi")
internal fun expandNotificationShade(context: Context): Boolean = try {
    val service = context.getSystemService("statusbar")
    if (service == null) {
        false
    } else {
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
        true
    }
} catch (e: Exception) {
    false
}
