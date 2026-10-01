package com.mfinatti.noclenchingsrs.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** US-10 reopened: free start/end times, and the preset-build value migrated on read. */
class SettingsRepositoryTest {

    private lateinit var dir: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: SettingsRepository
    private val legacyKey = stringPreferencesKey("quiet_hours_preset")

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("settings-test").toFile()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }
        repo = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    @Test
    fun `defaults are on, 22-00 to 07-00`() = runBlocking {
        val s = repo.settings.first()
        assertTrue(s.quietHoursEnabled)
        assertEquals(22 * 60, s.quietStartMinutes)
        assertEquals(7 * 60, s.quietEndMinutes)
    }

    @Test
    fun `a stored preset from the preset build is read as its times`() = runBlocking {
        dataStore.edit { it[legacyKey] = "NIGHT_23_08" }
        val s = repo.settings.first()
        assertEquals(23 * 60, s.quietStartMinutes)
        assertEquals(8 * 60, s.quietEndMinutes)
    }

    @Test
    fun `saving a range stores minute precision and drops the legacy preset`() = runBlocking {
        dataStore.edit { it[legacyKey] = "NIGHT_21_06" }
        repo.setQuietHoursRange(23 * 60 + 30, 6 * 60 + 15)
        val s = repo.settings.first()
        assertEquals(23 * 60 + 30, s.quietStartMinutes)
        assertEquals(6 * 60 + 15, s.quietEndMinutes)
        assertNull(dataStore.data.first()[legacyKey])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `start equal to end is rejected`() = runBlocking {
        repo.setQuietHoursRange(7 * 60, 7 * 60)
    }
}
