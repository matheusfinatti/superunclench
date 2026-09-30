package com.mfinatti.noclenchingsrs.domain.srs

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * One row of the SRS table (see docs/product/user-stories.md §2).
 *
 * @property number 1-based level number.
 * @property name working name, used for debug readouts (UI copy lives in resources).
 * @property interval time from an answer (or Start/Resume) to the next alarm at this level.
 * @property subLevelCount number of Good answers needed at this level to promote.
 */
data class SrsLevel(
    val number: Int,
    val name: String,
    val interval: Duration,
    val subLevelCount: Int,
)

object SrsLevels {
    /** The v1 SRS table. Total Goods from L1 to L8 with no Bads: 28. */
    val DEFAULT: List<SrsLevel> = listOf(
        SrsLevel(number = 1, name = "Warm-up", interval = 5.minutes, subLevelCount = 3),
        SrsLevel(number = 2, name = "Noticing", interval = 10.minutes, subLevelCount = 3),
        SrsLevel(number = 3, name = "Aware", interval = 15.minutes, subLevelCount = 4),
        SrsLevel(number = 4, name = "Steady", interval = 30.minutes, subLevelCount = 4),
        SrsLevel(number = 5, name = "Relaxed", interval = 45.minutes, subLevelCount = 4),
        SrsLevel(number = 6, name = "Grounded", interval = 1.hours, subLevelCount = 5),
        SrsLevel(number = 7, name = "Loose", interval = 2.hours, subLevelCount = 5),
        SrsLevel(number = 8, name = "Unclenched", interval = 3.hours, subLevelCount = 5),
    )
}
