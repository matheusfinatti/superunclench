package com.mfinatti.noclenchingsrs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mfinatti.noclenchingsrs.di.LocalAppContainer
import com.mfinatti.noclenchingsrs.ui.SuperUnclenchApp
import com.mfinatti.noclenchingsrs.ui.theme.AppNightMode
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

class MainActivity : ComponentActivity() {

    /** Incremented whenever the check-in notification body is tapped: the shell jumps to Home. */
    private val openHomeRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // API 24–30: pick a forced light/dark window theme before the window is created, so the
        // activity background matches the in-app theme (API 31+ uses setApplicationNightMode).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            AppNightMode.windowTheme(AppNightMode.mirrored(this))?.let { setTheme(it) }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val container = (application as SuperUnclenchApplication).container
        setContent {
            val settings by container.settingsState.collectAsStateWithLifecycle()
            // Draw nothing until DataStore's first emission (< 100 ms): the window background is
            // already the theme surface color, so there is no flash of the wrong theme.
            val loaded = settings
            if (loaded != null) {
                // Keep the system-level night mode in sync, so the next cold start's launch window
                // (and splash) already uses the in-app theme — no white flash (US-03 B1 / US-09).
                LaunchedEffect(loaded.themeMode) { AppNightMode.apply(this@MainActivity, loaded.themeMode) }
                CompositionLocalProvider(LocalAppContainer provides container) {
                    SuperUnclenchTheme(themeMode = loaded.themeMode) {
                        SuperUnclenchApp(
                            debugTools = container.debugTools,
                            openHomeRequests = openHomeRequests.intValue,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_HOME, false) == true) {
            openHomeRequests.intValue += 1
        }
    }

    companion object {
        /** Set by the check-in notification's content intent. */
        const val EXTRA_OPEN_HOME = "open_home"
    }
}
