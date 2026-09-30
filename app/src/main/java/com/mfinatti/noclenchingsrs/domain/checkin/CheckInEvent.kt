package com.mfinatti.noclenchingsrs.domain.checkin

enum class CheckInOutcome { GOOD, BAD, MISSED }

/** Where a history event came from (debug diagnostics, US-04 O1). */
enum class CheckInSource {
    /** Good/Bad action on the check-in notification. */
    NOTIFICATION,

    /** Good/Bad on the in-app pending check-in card. */
    CARD,

    /** Debug QA panel "Answer Good/Bad". */
    PANEL,

    /** Missed because a newer alarm replaced the unanswered check-in. */
    ALARM,

    /** Missed because the notification was dismissed (US-07). */
    DISMISSED,

    /** Debug "Seed sample history" (US-08). */
    SEED,

    /** Recorded before sources were tracked. */
    UNKNOWN,
}

/**
 * One entry of the check-in history.
 *
 * @property atMillis epoch millis of the answer / miss.
 * @property level SRS level at the time of the check-in (before the answer was applied).
 * @property subLevel SRS sub-level at the time of the check-in (before the answer was applied).
 * @property levelAfter / [subLevelAfter] the SRS position after the event was applied.
 */
data class CheckInEvent(
    val atMillis: Long,
    val outcome: CheckInOutcome,
    val level: Int,
    val subLevel: Int,
    val source: CheckInSource = CheckInSource.UNKNOWN,
    val levelAfter: Int = level,
    val subLevelAfter: Int = subLevel,
)
