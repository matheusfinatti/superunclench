package com.mfinatti.noclenchingsrs.data.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.mfinatti.noclenchingsrs.buildtype.BuildTypeDefaults
import com.mfinatti.noclenchingsrs.data.setOrRemove
import com.mfinatti.noclenchingsrs.domain.session.IntervalScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * QA switches. Only the debug-only QA panel writes them, so release builds always see the
 * defaults, and [BuildTypeDefaults] makes short intervals impossible in release regardless.
 *
 * @property previewNotificationsOff treat check-in notifications as blocked (banner + Start path).
 * @property previewLegacyNotificationPermission behave as on Android 12 and lower: no runtime
 * notification permission, so Start never shows the rationale sheet or system dialog.
 * @property shortIntervals scale every interval minutes → seconds (debug default ON).
 * @property previewExactAlarmsDenied behave as if exact alarms were not allowed (banner + inexact).
 * @property standardNotificationButtons use standard notification actions instead of the custom
 * green/red pill layout (fallback comparison, US-03 Option B).
 */
data class DebugOverrides(
    val previewNotificationsOff: Boolean = false,
    val previewLegacyNotificationPermission: Boolean = false,
    val shortIntervals: Boolean = BuildTypeDefaults.SHORT_INTERVALS_DEFAULT,
    val previewExactAlarmsDenied: Boolean = false,
    val standardNotificationButtons: Boolean = false,
    val autoPause: Boolean = true,
) {
    /** Short intervals are only honoured where the build type allows them. */
    val shortIntervalsActive: Boolean
        get() = BuildTypeDefaults.SHORT_INTERVALS_AVAILABLE && shortIntervals

    val intervalScale: IntervalScale
        get() = if (shortIntervalsActive) IntervalScale.MinutesAsSeconds else IntervalScale.Real
}

class DebugOverridesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val overrides: Flow<DebugOverrides> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val defaults = DebugOverrides()
            DebugOverrides(
                previewNotificationsOff = prefs[Keys.PREVIEW_NOTIFICATIONS_OFF] ?: defaults.previewNotificationsOff,
                previewLegacyNotificationPermission = prefs[Keys.PREVIEW_LEGACY_PERMISSION]
                    ?: defaults.previewLegacyNotificationPermission,
                shortIntervals = prefs[Keys.SHORT_INTERVALS] ?: defaults.shortIntervals,
                previewExactAlarmsDenied = prefs[Keys.PREVIEW_EXACT_DENIED] ?: defaults.previewExactAlarmsDenied,
                standardNotificationButtons = prefs[Keys.STANDARD_NOTIFICATION_BUTTONS]
                    ?: defaults.standardNotificationButtons,
                autoPause = prefs[Keys.AUTO_PAUSE] ?: defaults.autoPause,
            )
        }
        .distinctUntilChanged()

    suspend fun setPreviewNotificationsOff(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.PREVIEW_NOTIFICATIONS_OFF, enabled) }
    }

    suspend fun setPreviewLegacyNotificationPermission(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.PREVIEW_LEGACY_PERMISSION, enabled) }
    }

    suspend fun setShortIntervals(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.SHORT_INTERVALS, enabled) }
    }

    suspend fun setPreviewExactAlarmsDenied(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.PREVIEW_EXACT_DENIED, enabled) }
    }

    suspend fun setStandardNotificationButtons(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.STANDARD_NOTIFICATION_BUTTONS, enabled) }
    }

    suspend fun setAutoPause(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.AUTO_PAUSE, enabled) }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private object Keys {
        val PREVIEW_NOTIFICATIONS_OFF = booleanPreferencesKey("preview_notifications_off")
        val PREVIEW_LEGACY_PERMISSION = booleanPreferencesKey("preview_legacy_notification_permission")
        val SHORT_INTERVALS = booleanPreferencesKey("short_intervals")
        val PREVIEW_EXACT_DENIED = booleanPreferencesKey("preview_exact_alarms_denied")
        val STANDARD_NOTIFICATION_BUTTONS = booleanPreferencesKey("standard_notification_buttons")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
    }
}
