package com.mfinatti.noclenchingsrs.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mfinatti.noclenchingsrs.data.setOrRemove
import com.mfinatti.noclenchingsrs.data.toEnumOrDefault
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

    suspend fun setQuietHoursPreset(preset: QuietHoursPreset) {
        dataStore.edit { prefs -> prefs.setOrRemove(Keys.QUIET_HOURS_PRESET, preset.name) }
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
            quietHoursPreset = this[Keys.QUIET_HOURS_PRESET].toEnumOrDefault(defaults.quietHoursPreset),
            notificationPermissionRequested = this[Keys.NOTIFICATION_PERMISSION_REQUESTED]
                ?: defaults.notificationPermissionRequested,
        )
    }

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISCLAIMER_DISMISSED = booleanPreferencesKey("disclaimer_dismissed")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_PRESET = stringPreferencesKey("quiet_hours_preset")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
