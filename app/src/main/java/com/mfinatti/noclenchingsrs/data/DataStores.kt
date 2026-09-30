package com.mfinatti.noclenchingsrs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** User preferences (name, theme, disclaimer, quiet hours). */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Session + SRS state. Kept separate so "Reset all data" / "Reset progress" stay simple. */
internal val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** QA preview switches (written only by the debug panel). */
internal val Context.debugOverridesDataStore: DataStore<Preferences> by preferencesDataStore(name = "debug_overrides")
