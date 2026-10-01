package com.mfinatti.noclenchingsrs.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mfinatti.noclenchingsrs.data.setOrRemove
import com.mfinatti.noclenchingsrs.data.toEnumOrDefault
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.domain.settings.MINUTES_PER_DAY
import com.mfinatti.noclenchingsrs.domain.settings.QuietHoursPreset
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toUserSettings() }
        .distinctUntilChanged()

    suspend fun setName(name: String?) {
        val cleaned = name?.trim()?.takeIf { it.isNotEmpty() }
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.NAME, cleaned) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.THEME_MODE, mode.name) }
    }

    suspend fun setDisclaimerDismissed(dismissed: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.DISCLAIMER_DISMISSED, dismissed) }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.QUIET_HOURS_ENABLED, enabled) }
    }

    /**
     * Saves a quiet-hours range (minutes after local midnight). Start and end must differ (US-10
     * AC4). Drops the legacy preset key so the explicit times win from now on.
     */
    suspend fun setQuietHoursRange(startMinutes: Int, endMinutes: Int) {
        require(startMinutes in 0 until MINUTES_PER_DAY && endMinutes in 0 until MINUTES_PER_DAY)
        require(startMinutes != endMinutes) { "Quiet hours start and end can't be the same" }
        dataStore.edit { prefs ->
            prefs[Keys.QUIET_START] = startMinutes
            prefs[Keys.QUIET_END] = endMinutes
            prefs.remove(Keys.LEGACY_QUIET_HOURS_PRESET)
        }
    }

    suspend fun setAlertStyle(style: AlertStyle) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.ALERT_STYLE, style.name) }
    }

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.NOTIFICATION_PERMISSION_REQUESTED, requested) }
    }

    /** Restores every setting to its default. */
    suspend fun clear() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            name = this[Keys.NAME]?.takeIf { it.isNotBlank() },
            themeMode = this[Keys.THEME_MODE].toEnumOrDefault(defaults.themeMode),
            disclaimerDismissed = this[Keys.DISCLAIMER_DISMISSED] ?: defaults.disclaimerDismissed,
            quietHoursEnabled = this[Keys.QUIET_HOURS_ENABLED] ?: defaults.quietHoursEnabled,
            quietStartMinutes = quietRange(defaults).first,
            quietEndMinutes = quietRange(defaults).second,
            alertStyle = this[Keys.ALERT_STYLE].toEnumOrDefault(defaults.alertStyle),
            notificationPermissionRequested = this[Keys.NOTIFICATION_PERMISSION_REQUESTED]
                ?: defaults.notificationPermissionRequested,
        )
    }

    /**
     * Explicit start/end if stored and valid; otherwise the legacy preset (the US-10 preset build
     * stored only an enum name — migrated on read and replaced on the next save); else 22:00–07:00.
     */
    private fun Preferences.quietRange(defaults: UserSettings): Pair<Int, Int> {
        val start = this[Keys.QUIET_START]
        val end = this[Keys.QUIET_END]
        if (start != null && end != null && start != end &&
            start in 0 until MINUTES_PER_DAY && end in 0 until MINUTES_PER_DAY
        ) {
            return start to end
        }
        val legacy = this[Keys.LEGACY_QUIET_HOURS_PRESET]
            ?.let { name -> QuietHoursPreset.entries.firstOrNull { it.name == name } }
        return if (legacy != null) {
            legacy.startMinutes to legacy.endMinutes
        } else {
            defaults.quietStartMinutes to defaults.quietEndMinutes
        }
    }

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISCLAIMER_DISMISSED = booleanPreferencesKey("disclaimer_dismissed")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_minutes")
        val QUIET_END = intPreferencesKey("quiet_end_minutes")
        val LEGACY_QUIET_HOURS_PRESET = stringPreferencesKey("quiet_hours_preset")
        val ALERT_STYLE = stringPreferencesKey("alert_style")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
