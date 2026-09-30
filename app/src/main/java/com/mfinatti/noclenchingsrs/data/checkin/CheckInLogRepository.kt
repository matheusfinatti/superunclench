package com.mfinatti.noclenchingsrs.data.checkin

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Append-mostly check-in history stored as a single JSON file (see [CheckInLogCodec]).
 * v1 volumes are small (a few dozen events per day), so the whole log is kept in memory.
 */
class CheckInLogRepository(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<CheckInEvent>>(emptyList())

    @Volatile
    private var loaded = false

    /** All events, oldest first. */
    val events: Flow<List<CheckInEvent>> = flow {
        ensureLoaded()
        emitAll(state)
    }

    suspend fun append(event: CheckInEvent) = mutate { it + event }

    suspend fun replaceAll(events: List<CheckInEvent>) = mutate { events.sortedBy { it.atMillis } }

    suspend fun clear() = mutate { emptyList() }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock { loadLocked() }
    }

    /** Must be called with [mutex] held. */
    private suspend fun loadLocked() {
        if (loaded) return
        state.value = withContext(ioDispatcher) { readFromDisk() }
        loaded = true
    }

    private suspend fun mutate(transform: (List<CheckInEvent>) -> List<CheckInEvent>) {
        mutex.withLock {
            loadLocked()
            val updated = transform(state.value)
            withContext(ioDispatcher) { writeToDisk(updated) }
            state.value = updated
        }
    }

    private fun readFromDisk(): List<CheckInEvent> = try {
        if (file.exists()) CheckInLogCodec.decode(file.readText()) else emptyList()
    } catch (e: IOException) {
        emptyList()
    }

    private fun writeToDisk(events: List<CheckInEvent>) {
        val text = CheckInLogCodec.encode(events)
        val dir = file.parentFile
        dir?.mkdirs()
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            // Fallback for file systems where rename-over-existing fails.
            file.writeText(text)
            tmp.delete()
        }
    }
}
