package com.mfinatti.noclenchingsrs.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mfinatti.noclenchingsrs.data.setOrRemove
import com.mfinatti.noclenchingsrs.data.toEnumOrDefault
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.LastAnswer
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.domain.srs.SrsEngine
import com.mfinatti.noclenchingsrs.domain.srs.SrsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Persists [SessionState] (session status + SRS position) in a Preferences DataStore. */
class SessionRepository(
    private val dataStore: DataStore<Preferences>,
    private val srsEngine: SrsEngine,
) {

    val session: Flow<SessionState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toSessionState() }
        .distinctUntilChanged()

    /** Atomically reads, transforms and writes the session state; returns the new state. */
    suspend fun update(transform: (SessionState) -> SessionState): SessionState {
        var result = SessionState()
        dataStore.edit { prefs ->
            val next = transform(prefs.toSessionState())
            prefs.write(next)
            result = next
        }
        return result
    }

    /** Back to defaults: stopped, L1/0, nothing scheduled. */
    suspend fun reset() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private fun Preferences.toSessionState(): SessionState {
        val defaults = SessionState()
        return SessionState(
            status = this[Keys.STATUS].toEnumOrDefault(defaults.status),
            srs = srsEngine.normalize(
                SrsState(
                    level = this[Keys.LEVEL] ?: defaults.srs.level,
                    subLevel = this[Keys.SUB_LEVEL] ?: defaults.srs.subLevel,
                ),
            ),
            nextAlarmAtMillis = this[Keys.NEXT_ALARM_AT],
            pendingCheckInAtMillis = this[Keys.PENDING_AT],
            consecutiveMisses = this[Keys.CONSECUTIVE_MISSES] ?: defaults.consecutiveMisses,
            pausedUntilMillis = this[Keys.PAUSED_UNTIL],
            autoPaused = this[Keys.AUTO_PAUSED] ?: defaults.autoPaused,
            lastAnswer = readLastAnswer(),
            exactTimingBannerDismissed = this[Keys.EXACT_BANNER_DISMISSED] ?: false,
            ringCapAtMillis = this[Keys.RING_CAP_AT],
            ringSilenced = this[Keys.RING_SILENCED] ?: false,
        )
    }

    private fun Preferences.readLastAnswer(): LastAnswer? {
        val at = this[Keys.LAST_ANSWER_AT] ?: return null
        val answer = this[Keys.LAST_ANSWER]?.let { name -> Answer.entries.firstOrNull { it.name == name } }
            ?: return null
        val change = this[Keys.LAST_CHANGE]?.let { name -> LevelChange.entries.firstOrNull { it.name == name } }
            ?: return null
        val source = this[Keys.LAST_SOURCE].toEnumOrDefault(CheckInSource.UNKNOWN)
        return LastAnswer(atMillis = at, answer = answer, change = change, source = source)
    }

    private fun MutablePreferences.write(state: SessionState) {
        setOrRemove(Keys.STATUS, state.status.name)
        setOrRemove(Keys.LEVEL, state.srs.level)
        setOrRemove(Keys.SUB_LEVEL, state.srs.subLevel)
        setOrRemove(Keys.NEXT_ALARM_AT, state.nextAlarmAtMillis)
        setOrRemove(Keys.PENDING_AT, state.pendingCheckInAtMillis)
        setOrRemove(Keys.CONSECUTIVE_MISSES, state.consecutiveMisses)
        setOrRemove(Keys.PAUSED_UNTIL, state.pausedUntilMillis)
        setOrRemove(Keys.AUTO_PAUSED, state.autoPaused)
        setOrRemove(Keys.LAST_ANSWER_AT, state.lastAnswer?.atMillis)
        setOrRemove(Keys.LAST_ANSWER, state.lastAnswer?.answer?.name)
        setOrRemove(Keys.LAST_CHANGE, state.lastAnswer?.change?.name)
        setOrRemove(Keys.LAST_SOURCE, state.lastAnswer?.source?.name)
        setOrRemove(Keys.EXACT_BANNER_DISMISSED, state.exactTimingBannerDismissed)
        setOrRemove(Keys.RING_CAP_AT, state.ringCapAtMillis)
        setOrRemove(Keys.RING_SILENCED, state.ringSilenced)
    }

    private object Keys {
        val STATUS = stringPreferencesKey("status")
        val LEVEL = intPreferencesKey("level")
        val SUB_LEVEL = intPreferencesKey("sub_level")
        val NEXT_ALARM_AT = longPreferencesKey("next_alarm_at")
        val PENDING_AT = longPreferencesKey("pending_check_in_at")
        val CONSECUTIVE_MISSES = intPreferencesKey("consecutive_misses")
        val PAUSED_UNTIL = longPreferencesKey("paused_until")
        val AUTO_PAUSED = booleanPreferencesKey("auto_paused")
        val LAST_ANSWER_AT = longPreferencesKey("last_answer_at")
        val LAST_ANSWER = stringPreferencesKey("last_answer")
        val LAST_CHANGE = stringPreferencesKey("last_change")
        val LAST_SOURCE = stringPreferencesKey("last_answer_source")
        val EXACT_BANNER_DISMISSED = booleanPreferencesKey("exact_banner_dismissed")
        val RING_CAP_AT = longPreferencesKey("ring_cap_at")
        val RING_SILENCED = booleanPreferencesKey("ring_silenced")
    }
}
