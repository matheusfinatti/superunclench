package com.mfinatti.noclenchingsrs.core.time

/** Injectable wall clock so time-dependent logic can be tested and (later) sped up by QA tools. */
fun interface AppClock {
    fun nowMillis(): Long

    companion object {
        val System: AppClock = AppClock { java.lang.System.currentTimeMillis() }
    }
}
