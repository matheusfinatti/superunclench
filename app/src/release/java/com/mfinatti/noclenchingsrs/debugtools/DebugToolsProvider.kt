package com.mfinatti.noclenchingsrs.debugtools

import com.mfinatti.noclenchingsrs.di.AppContainer

/** Release builds: no Developer / QA panel. */
object DebugToolsProvider {
    @Suppress("UNUSED_PARAMETER")
    fun create(container: AppContainer): DebugTools = NoOpDebugTools
}
