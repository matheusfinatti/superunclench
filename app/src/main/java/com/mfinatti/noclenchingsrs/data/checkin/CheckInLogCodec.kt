package com.mfinatti.noclenchingsrs.data.checkin

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** On-disk JSON format of the check-in history (versioned for future migrations). */
@Serializable
internal data class CheckInLogDto(
    val version: Int = CheckInLogCodec.CURRENT_VERSION,
    val events: List<CheckInEventDto> = emptyList(),
)

@Serializable
internal data class CheckInEventDto(
    val at: Long,
    val outcome: String,
    val level: Int,
    val subLevel: Int,
    // Added in log version 2; absent in version 1 files.
    val source: String? = null,
    val levelAfter: Int? = null,
    val subLevelAfter: Int? = null,
)

object CheckInLogCodec {
    const val CURRENT_VERSION: Int = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(events: List<CheckInEvent>): String = json.encodeToString(
        CheckInLogDto.serializer(),
        CheckInLogDto(version = CURRENT_VERSION, events = events.map { it.toDto() }),
    )

    /** Decodes [text]; corrupt or empty input yields an empty history rather than a crash. */
    fun decode(text: String): List<CheckInEvent> {
        if (text.isBlank()) return emptyList()
        return try {
            json.decodeFromString(CheckInLogDto.serializer(), text).events.mapNotNull { it.toDomainOrNull() }
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization.SerializationException extends IllegalArgumentException.
            emptyList()
        }
    }

    private fun CheckInEvent.toDto() = CheckInEventDto(
        at = atMillis,
        outcome = outcome.name,
        level = level,
        subLevel = subLevel,
        source = source.name,
        levelAfter = levelAfter,
        subLevelAfter = subLevelAfter,
    )

    private fun CheckInEventDto.toDomainOrNull(): CheckInEvent? {
        val parsed = CheckInOutcome.entries.firstOrNull { it.name == outcome } ?: return null
        return CheckInEvent(
            atMillis = at,
            outcome = parsed,
            level = level,
            subLevel = subLevel,
            source = CheckInSource.entries.firstOrNull { it.name == source } ?: CheckInSource.UNKNOWN,
            levelAfter = levelAfter ?: level,
            subLevelAfter = subLevelAfter ?: subLevel,
        )
    }
}
