package com.mfinatti.noclenchingsrs.buildtype

/** Release builds: always real timings; short intervals cannot be enabled. */
object BuildTypeDefaults {
    const val SHORT_INTERVALS_AVAILABLE: Boolean = false
    const val SHORT_INTERVALS_DEFAULT: Boolean = false
}
