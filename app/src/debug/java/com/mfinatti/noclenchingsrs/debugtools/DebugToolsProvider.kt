package com.mfinatti.noclenchingsrs.debugtools

import com.mfinatti.noclenchingsrs.debugtools.qa.QaDebugTools
import com.mfinatti.noclenchingsrs.di.AppContainer

/** Debug builds: the Developer / QA panel (US-02). */
object DebugToolsProvider {
    fun create(container: AppContainer): DebugTools = QaDebugTools(container)
}
