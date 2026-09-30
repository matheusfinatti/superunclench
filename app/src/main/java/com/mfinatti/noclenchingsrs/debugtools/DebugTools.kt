package com.mfinatti.noclenchingsrs.debugtools

import androidx.compose.runtime.Composable

/**
 * Hooks for the debug-only Developer / QA panel (US-02).
 *
 * The real implementation lives in `src/debug`; `src/release` provides [NoOpDebugTools], so no
 * panel code is compiled into release builds.
 */
interface DebugTools {
    /** True when the Developer / QA panel exists in this build. */
    val isAvailable: Boolean

    /** Entry row shown as the last group in Settings. */
    @Composable
    fun SettingsEntry(onClick: () -> Unit)

    /** The full-screen panel, pushed inside the Settings tab. */
    @Composable
    fun Panel(onBack: () -> Unit)
}

object NoOpDebugTools : DebugTools {
    override val isAvailable: Boolean = false

    @Composable
    override fun SettingsEntry(onClick: () -> Unit) = Unit

    @Composable
    override fun Panel(onBack: () -> Unit) = Unit
}
