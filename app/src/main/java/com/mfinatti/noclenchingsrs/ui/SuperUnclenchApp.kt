package com.mfinatti.noclenchingsrs.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.debugtools.DebugTools
import com.mfinatti.noclenchingsrs.feature.home.ui.HomeRoute
import com.mfinatti.noclenchingsrs.feature.settings.ui.AboutScreen
import com.mfinatti.noclenchingsrs.feature.settings.ui.SettingsRoute
import com.mfinatti.noclenchingsrs.feature.stats.ui.StatsRoute
import com.mfinatti.noclenchingsrs.ui.navigation.SettingsDestination
import com.mfinatti.noclenchingsrs.ui.navigation.TopLevelDestination

private const val TAB_CROSSFADE_MILLIS = 150

/**
 * App shell: Home / Stats / Settings in a NavigationSuiteScaffold (bottom bar on compact,
 * rail on medium/expanded). Secondary screens (Developer / QA) are pushed inside the Settings tab.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SuperUnclenchApp(
    debugTools: DebugTools,
    openHomeRequests: Int = 0,
) {
    var currentDestination by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    var settingsScreen by rememberSaveable { mutableStateOf(SettingsDestination.MAIN) }
    val tabStateHolder = rememberSaveableStateHolder()

    // Notification body tap deep-links to Home and clears secondary screens (US-01 §1).
    LaunchedEffect(openHomeRequests) {
        if (openHomeRequests > 0) {
            settingsScreen = SettingsDestination.MAIN
            currentDestination = TopLevelDestination.HOME
        }
    }

    val onSecondaryScreen = currentDestination == TopLevelDestination.SETTINGS &&
        settingsScreen != SettingsDestination.MAIN
    // Back: secondary screen -> Settings; Stats/Settings -> Home; Home -> exit (not intercepted).
    BackHandler(enabled = currentDestination != TopLevelDestination.HOME) {
        if (onSecondaryScreen) {
            settingsScreen = SettingsDestination.MAIN
        } else {
            currentDestination = TopLevelDestination.HOME
        }
    }

    NavigationSuiteScaffold(
        // Expose test tags as resource-ids so UI Automator / adb-driven QA scripts can find them.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == currentDestination
                item(
                    selected = selected,
                    onClick = {
                        if (destination == TopLevelDestination.SETTINGS && selected) {
                            // Re-selecting Settings pops back to its root.
                            settingsScreen = SettingsDestination.MAIN
                        }
                        currentDestination = destination
                    },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag(destination.testTag),
                )
            }
        },
    ) {
        Crossfade(
            targetState = currentDestination,
            animationSpec = tween(durationMillis = TAB_CROSSFADE_MILLIS),
            label = "tab",
        ) { destination ->
            tabStateHolder.SaveableStateProvider(destination.name) {
                when (destination) {
                    TopLevelDestination.HOME -> HomeRoute(
                        onOpenStats = { currentDestination = TopLevelDestination.STATS },
                    )
                    TopLevelDestination.STATS -> StatsRoute(
                        onGoHome = { currentDestination = TopLevelDestination.HOME },
                    )
                    TopLevelDestination.SETTINGS -> when (settingsScreen) {
                        SettingsDestination.MAIN -> SettingsRoute(
                            onOpenAbout = { settingsScreen = SettingsDestination.ABOUT },
                            debugEntry = {
                                debugTools.SettingsEntry(onClick = { settingsScreen = SettingsDestination.DEVELOPER_QA })
                            },
                        )
                        SettingsDestination.ABOUT -> AboutScreen(onBack = { settingsScreen = SettingsDestination.MAIN })
                        SettingsDestination.DEVELOPER_QA -> debugTools.Panel(onBack = { settingsScreen = SettingsDestination.MAIN })
                    }
                }
            }
        }
    }
}
