package com.mfinatti.noclenchingsrs.domain.srs

import kotlin.time.Duration

/** The user's answer to "Were you clenching just now?". */
enum class Answer { GOOD, BAD }

/** Persisted SRS position. New users start at L1, sub-level 0. */
data class SrsState(
    val level: Int = 1,
    val subLevel: Int = 0,
)

/** What an answer did to the SRS position (drives "Level up!" style feedback). */
enum class LevelChange {
    /** Good: one more sub-level filled, same level. */
    PROGRESSED,

    /** Good: sub-levels completed, moved up one level. */
    PROMOTED,

    /** Bad with progress in the current level: progress lost, level kept. */
    SUB_LEVEL_RESET,

    /** Bad with no progress in the current level: moved down one level. */
    DEMOTED,

    /** No change (Good at max level, or Bad at L1 with no progress). */
    UNCHANGED,
}

data class SrsResult(
    val state: SrsState,
    val change: LevelChange,
)

/**
 * Pure implementation of the SRS rules from docs/product/user-stories.md §2.
 * No Android dependencies; fully unit-tested in `src/test`.
 */
class SrsEngine(
    val levels: List<SrsLevel> = SrsLevels.DEFAULT,
) {
    init {
        require(levels.isNotEmpty()) { "SRS table must not be empty" }
        levels.forEachIndexed { index, level ->
            require(level.number == index + 1) { "Levels must be numbered 1..n in order" }
            require(level.subLevelCount >= 1) { "Level ${level.number} needs at least one sub-level" }
        }
    }

    val minLevel: Int get() = 1
    val maxLevel: Int get() = levels.size

    fun levelInfo(level: Int): SrsLevel = levels[level.coerceIn(minLevel, maxLevel) - 1]

    fun interval(level: Int): Duration = levelInfo(level).interval

    fun subLevelCount(level: Int): Int = levelInfo(level).subLevelCount

    fun isMaxLevel(level: Int): Boolean = level >= maxLevel

    /** Clamps an arbitrary (e.g. persisted or corrupted) state into the valid range. */
    fun normalize(state: SrsState): SrsState {
        val level = state.level.coerceIn(minLevel, maxLevel)
        val subLevel = state.subLevel.coerceIn(0, subLevelCount(level) - 1)
        return if (level == state.level && subLevel == state.subLevel) state else SrsState(level, subLevel)
    }

    /** Applies one Good/Bad answer. */
    fun apply(state: SrsState, answer: Answer): SrsResult {
        val current = normalize(state)
        return when (answer) {
            Answer.GOOD -> applyGood(current)
            Answer.BAD -> applyBad(current)
        }
    }

    private fun applyGood(current: SrsState): SrsResult {
        // At max level a Good only records history.
        if (isMaxLevel(current.level)) return SrsResult(current, LevelChange.UNCHANGED)
        val nextSub = current.subLevel + 1
        return if (nextSub >= subLevelCount(current.level)) {
            SrsResult(SrsState(level = current.level + 1, subLevel = 0), LevelChange.PROMOTED)
        } else {
            SrsResult(current.copy(subLevel = nextSub), LevelChange.PROGRESSED)
        }
    }

    private fun applyBad(current: SrsState): SrsResult = when {
        current.subLevel > 0 ->
            SrsResult(current.copy(subLevel = 0), LevelChange.SUB_LEVEL_RESET)

        current.level > minLevel ->
            SrsResult(SrsState(level = current.level - 1, subLevel = 0), LevelChange.DEMOTED)

        else -> SrsResult(current, LevelChange.UNCHANGED)
    }

    /** Debug/QA: jump to [level] (clamped to 1..max) with sub-level 0. */
    fun setLevel(level: Int): SrsState = SrsState(level = level.coerceIn(minLevel, maxLevel), subLevel = 0)

    /** Debug/QA: fill one more sub-level without ever promoting. */
    fun incrementSubLevel(state: SrsState): SrsState {
        val current = normalize(state)
        val capped = minOf(current.subLevel + 1, subLevelCount(current.level) - 1)
        return current.copy(subLevel = capped)
    }

    /** Epoch millis of the next alarm when scheduled from [fromMillis] at [level]. */
    fun nextAlarmAt(fromMillis: Long, level: Int): Long = fromMillis + interval(level).inWholeMilliseconds
}
