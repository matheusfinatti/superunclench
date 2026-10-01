package com.mfinatti.noclenchingsrs.domain.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** QA panel cycle: System -> Light -> Dark -> System. */
    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}

/**
 * Quiet-hours shortcuts (US-10). Since the founder change request the user picks any start/end in
 * Settings; these remain as QA-panel shortcuts and as the reading of the legacy stored preset.
 * Times are minutes after midnight, local time.
 */
enum class QuietHoursPreset(val startMinutes: Int, val endMinutes: Int) {
    NIGHT_22_07(startMinutes = 22 * 60, endMinutes = 7 * 60),
    NIGHT_23_08(startMinutes = 23 * 60, endMinutes = 8 * 60),
    NIGHT_21_06(startMinutes = 21 * 60, endMinutes = 6 * 60),
}

/** How a check-in gets the user's attention (US-11). */
enum class AlertStyle {
    /** One sound + buzz, heads-up with Good/Bad (default). */
    NUDGE,

    /** A real alarm: full-screen when locked, rings on the alarm stream until answered (10 min cap). */
    RING,
}

data class UserSettings(
    val name: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val disclaimerDismissed: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    /** Quiet hours start, minutes after local midnight (0..1439). Default 22:00. */
    val quietStartMinutes: Int = DEFAULT_QUIET_START,
    /** Quiet hours end, minutes after local midnight (0..1439), never equal to the start. Default 07:00. */
    val quietEndMinutes: Int = DEFAULT_QUIET_END,
    val alertStyle: AlertStyle = AlertStyle.NUDGE,
    /** True once the Android 13+ notification permission dialog has been requested at least once. */
    val notificationPermissionRequested: Boolean = false,
)

const val DEFAULT_QUIET_START = 22 * 60
const val DEFAULT_QUIET_END = 7 * 60
const val MINUTES_PER_DAY = 24 * 60

/** Length of a quiet window in minutes, wrapping midnight (23:30 → 06:15 = 405). Never 0 for a valid range. */
fun quietDurationMinutes(startMinutes: Int, endMinutes: Int): Int =
    ((endMinutes - startMinutes) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
