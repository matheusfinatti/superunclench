package com.mfinatti.noclenchingsrs.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SrsEngineTest {

    private val engine = SrsEngine()

    @Test
    fun `new users start at level 1 sub-level 0`() {
        assertEquals(SrsState(level = 1, subLevel = 0), SrsState())
    }

    @Test
    fun `table matches the spec`() {
        val expected = listOf(
            Triple(1, 5.minutes, 3),
            Triple(2, 10.minutes, 3),
            Triple(3, 15.minutes, 4),
            Triple(4, 30.minutes, 4),
            Triple(5, 45.minutes, 4),
            Triple(6, 1.hours, 5),
            Triple(7, 2.hours, 5),
            Triple(8, 3.hours, 5),
        )
        assertEquals(8, engine.maxLevel)
        expected.forEach { (level, interval, subLevels) ->
            assertEquals("interval L$level", interval, engine.interval(level))
            assertEquals("sub-levels L$level", subLevels, engine.subLevelCount(level))
        }
    }

    @Test
    fun `good at L1 0 of 3 fills one sub-level`() {
        val result = engine.apply(SrsState(1, 0), Answer.GOOD)
        assertEquals(SrsState(1, 1), result.state)
        assertEquals(LevelChange.PROGRESSED, result.change)
    }

    @Test
    fun `good at L1 2 of 3 promotes to L2 0`() {
        val result = engine.apply(SrsState(1, 2), Answer.GOOD)
        assertEquals(SrsState(2, 0), result.state)
        assertEquals(LevelChange.PROMOTED, result.change)
    }

    @Test
    fun `good at L3 3 of 4 promotes to L4 0`() {
        val result = engine.apply(SrsState(3, 3), Answer.GOOD)
        assertEquals(SrsState(4, 0), result.state)
        assertEquals(LevelChange.PROMOTED, result.change)
    }

    @Test
    fun `bad with progress resets sub-level and keeps level`() {
        val result = engine.apply(SrsState(3, 2), Answer.BAD)
        assertEquals(SrsState(3, 0), result.state)
        assertEquals(LevelChange.SUB_LEVEL_RESET, result.change)
    }

    @Test
    fun `bad without progress demotes one level`() {
        val result = engine.apply(SrsState(3, 0), Answer.BAD)
        assertEquals(SrsState(2, 0), result.state)
        assertEquals(LevelChange.DEMOTED, result.change)
    }

    @Test
    fun `bad at L1 0 stays at L1 0`() {
        val result = engine.apply(SrsState(1, 0), Answer.BAD)
        assertEquals(SrsState(1, 0), result.state)
        assertEquals(LevelChange.UNCHANGED, result.change)
    }

    @Test
    fun `two bads in a row from mid-level drop one level`() {
        val first = engine.apply(SrsState(5, 3), Answer.BAD).state
        val second = engine.apply(first, Answer.BAD).state
        assertEquals(SrsState(4, 0), second)
    }

    @Test
    fun `good at max level only records history`() {
        val result = engine.apply(SrsState(8, 0), Answer.GOOD)
        assertEquals(SrsState(8, 0), result.state)
        assertEquals(LevelChange.UNCHANGED, result.change)
        assertTrue(engine.isMaxLevel(8))
        assertFalse(engine.isMaxLevel(7))
    }

    @Test
    fun `bad at max level with no progress demotes to L7`() {
        val result = engine.apply(SrsState(8, 0), Answer.BAD)
        assertEquals(SrsState(7, 0), result.state)
        assertEquals(LevelChange.DEMOTED, result.change)
    }

    @Test
    fun `28 goods take a new user from L1 to L8`() {
        var state = SrsState()
        var promotions = 0
        repeat(27) {
            val result = engine.apply(state, Answer.GOOD)
            if (result.change == LevelChange.PROMOTED) promotions++
            state = result.state
        }
        assertEquals("27 goods are one short of max", SrsState(7, 4), state)
        val last = engine.apply(state, Answer.GOOD)
        assertEquals(SrsState(8, 0), last.state)
        assertEquals(LevelChange.PROMOTED, last.change)
        assertEquals(6, promotions)
    }

    @Test
    fun `next alarm uses the interval of the given level`() {
        val now = 1_000_000L
        assertEquals(now + 5 * 60_000L, engine.nextAlarmAt(now, 1))
        assertEquals(now + 15 * 60_000L, engine.nextAlarmAt(now, 3))
        assertEquals(now + 3 * 3_600_000L, engine.nextAlarmAt(now, 8))
    }

    @Test
    fun `normalize clamps out-of-range persisted state`() {
        assertEquals(SrsState(1, 0), engine.normalize(SrsState(0, -2)))
        assertEquals(SrsState(8, 4), engine.normalize(SrsState(12, 9)))
        assertEquals(SrsState(2, 2), engine.normalize(SrsState(2, 7)))
        val valid = SrsState(4, 3)
        assertEquals(valid, engine.normalize(valid))
    }

    @Test
    fun `setLevel clamps and resets sub-level`() {
        assertEquals(SrsState(3, 0), engine.setLevel(3))
        assertEquals(SrsState(1, 0), engine.setLevel(0))
        assertEquals(SrsState(8, 0), engine.setLevel(99))
    }

    @Test
    fun `incrementSubLevel never promotes`() {
        assertEquals(SrsState(1, 1), engine.incrementSubLevel(SrsState(1, 0)))
        assertEquals(SrsState(1, 2), engine.incrementSubLevel(SrsState(1, 2)))
        assertEquals(SrsState(3, 3), engine.incrementSubLevel(SrsState(3, 3)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `table must be numbered in order`() {
        SrsEngine(listOf(SrsLevel(2, "x", 5.minutes, 3)))
    }
}
