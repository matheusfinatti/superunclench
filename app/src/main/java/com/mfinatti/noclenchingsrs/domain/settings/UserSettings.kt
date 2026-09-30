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

/** Quiet-hours presets (US-10). Times are minutes after midnight, local time. */
enum class QuietHoursPreset(val startMinutes: Int, val endMinutes: Int) {
    NIGHT_22_07(startMinutes = 22 * 60, endMinutes = 7 * 60),
    NIGHT_23_08(startMinutes = 23 * 60, endMinutes = 8 * 60),
    NIGHT_21_06(startMinutes = 21 * 60, endMinutes = 6 * 60),
}

data class UserSettings(
    val name: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val disclaimerDismissed: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    val quietHoursPreset: QuietHoursPreset = QuietHoursPreset.NIGHT_22_07,
    /** True once the Android 13+ notification permission dialog has been requested at least once. */
    val notificationPermissionRequested: Boolean = false,
)
