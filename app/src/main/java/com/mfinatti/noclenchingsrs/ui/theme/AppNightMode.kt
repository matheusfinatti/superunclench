package com.mfinatti.noclenchingsrs.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode

/**
 * Makes the *launch window* match the in-app theme (US-03 B1 / US-09).
 *
 * - API 31+: [UiModeManager.setApplicationNightMode] is persisted by the system per app and applied
 *   to our process configuration and splash/starting window before the first frame, so the
 *   `values-night` window background is right on the next cold start.
 * - API 24–30 (no AppCompat): the theme is mirrored in a tiny SharedPreferences file that
 *   MainActivity reads synchronously to pick a forced light/dark window theme before
 *   `super.onCreate`. The system preview window can still follow the system mode there.
 */
object AppNightMode {

    private const val PREFS = "theme_mirror"
    private const val KEY_MODE = "mode"

    fun apply(context: Context, mode: ThemeMode) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode.name).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(UiModeManager::class.java) ?: return
            val target = when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
            manager.setApplicationNightMode(target)
        }
    }

    /** Last applied mode (synchronous; defaults to System). */
    fun mirrored(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
    }

    /** Window theme for API 24–30 so the activity window background matches before Compose draws. */
    fun windowTheme(mode: ThemeMode): Int? = when (mode) {
        ThemeMode.SYSTEM -> null
        ThemeMode.LIGHT -> R.style.Theme_SuperUnclench_ForcedLight
        ThemeMode.DARK -> R.style.Theme_SuperUnclench_ForcedDark
    }
}
