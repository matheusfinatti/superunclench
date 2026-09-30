package com.mfinatti.noclenchingsrs.data.checkin

import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInLogCodecTest {

    private val events = listOf(
        CheckInEvent(atMillis = 1_000L, outcome = CheckInOutcome.GOOD, level = 1, subLevel = 0),
        CheckInEvent(atMillis = 2_000L, outcome = CheckInOutcome.BAD, level = 3, subLevel = 2),
        CheckInEvent(atMillis = 3_000L, outcome = CheckInOutcome.MISSED, level = 8, subLevel = 0),
    )

    @Test
    fun `round trip preserves events`() {
        assertEquals(events, CheckInLogCodec.decode(CheckInLogCodec.encode(events)))
    }

    @Test
    fun `empty and corrupt input decode to empty history`() {
        assertTrue(CheckInLogCodec.decode("").isEmpty())
        assertTrue(CheckInLogCodec.decode("{not json").isEmpty())
        assertTrue(CheckInLogCodec.decode("[1,2,3]").isEmpty())
    }

    @Test
    fun `unknown outcomes are skipped and unknown keys ignored`() {
        val json = """
            {"version":1,"extra":true,"events":[
              {"at":5,"outcome":"GOOD","level":2,"subLevel":1,"note":"x"},
              {"at":6,"outcome":"SOMETHING_NEW","level":2,"subLevel":1}
            ]}
        """.trimIndent()
        assertEquals(
            listOf(CheckInEvent(atMillis = 5, outcome = CheckInOutcome.GOOD, level = 2, subLevel = 1)),
            CheckInLogCodec.decode(json),
        )
    }

    @Test
    fun `version 1 files decode with unknown source and after equal to before`() {
        val v1 = """{"version":1,"events":[{"at":7,"outcome":"MISSED","level":3,"subLevel":1}]}"""
        val event = CheckInLogCodec.decode(v1).single()
        assertEquals(com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.UNKNOWN, event.source)
        assertEquals(3, event.levelAfter)
        assertEquals(1, event.subLevelAfter)
    }

    @Test
    fun `source and level after round trip`() {
        val e = CheckInEvent(
            atMillis = 9,
            outcome = CheckInOutcome.GOOD,
            level = 2,
            subLevel = 2,
            source = com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource.CARD,
            levelAfter = 3,
            subLevelAfter = 0,
        )
        assertEquals(listOf(e), CheckInLogCodec.decode(CheckInLogCodec.encode(listOf(e))))
    }
}
