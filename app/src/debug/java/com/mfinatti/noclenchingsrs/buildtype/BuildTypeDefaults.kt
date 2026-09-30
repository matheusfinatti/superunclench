package com.mfinatti.noclenchingsrs.buildtype

/** Debug builds: fast timings for QA (founder request, US-05). */
object BuildTypeDefaults {
    /** Short intervals (minutes → seconds) can be switched on. */
    const val SHORT_INTERVALS_AVAILABLE: Boolean = true

    /** Short intervals are ON unless QA switches to real timings in the Developer / QA panel. */
    const val SHORT_INTERVALS_DEFAULT: Boolean = true
}
