package com.mfinatti.noclenchingsrs.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mfinatti.noclenchingsrs.R

/** Top-level destinations in the NavigationSuiteScaffold (order fixed by design US-01 §1). */
enum class TopLevelDestination(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
    val testTag: String,
) {
    HOME(R.string.nav_home, R.drawable.ic_home, R.drawable.ic_home_filled, "nav_home"),
    STATS(R.string.nav_stats, R.drawable.ic_stats, R.drawable.ic_stats_filled, "nav_stats"),
    SETTINGS(R.string.nav_settings, R.drawable.ic_settings, R.drawable.ic_settings_filled, "nav_settings"),
}

/** Secondary screens pushed inside the Settings tab. */
enum class SettingsDestination {
    MAIN,

    /** About & disclaimer (US-09). */
    ABOUT,

    /** Debug builds only (US-02). */
    DEVELOPER_QA,
}
