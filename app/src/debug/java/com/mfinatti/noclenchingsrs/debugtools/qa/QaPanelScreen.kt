package com.mfinatti.noclenchingsrs.debugtools.qa

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.data.debug.DebugOverrides
import com.mfinatti.noclenchingsrs.feature.home.ui.NotificationRationaleSheet
import com.mfinatti.noclenchingsrs.feature.home.ui.RationaleMode
import com.mfinatti.noclenchingsrs.ui.components.AppSnackbarHost
import com.mfinatti.noclenchingsrs.di.AppContainer
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.domain.srs.SrsLevels
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.isDark
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test tags from docs/design/US-02-qa-panel.md §9 (only those implemented in this story). */
object QaTestTags {
    const val STATE = "qa_state"
    const val STATE_SESSION = "qa_state_session"
    const val STATE_LEVEL = "qa_state_level"
    const val STATE_NEXT_ALARM = "qa_state_next_alarm"
    const val STATE_MISSES = "qa_state_misses"
    const val STATE_SHORT = "qa_state_short"
    const val STATE_QUIET = "qa_state_quiet"
    const val SET_NAME = "qa_set_name"
    const val CLEAR_NAME = "qa_clear_name"
    const val THEME_CYCLE = "qa_theme_cycle"
    const val SHOW_DISCLAIMER = "qa_show_disclaimer"
    const val OPEN_SHADE = "qa_open_shade"
    const val FIRE_ALARM = "qa_fire_alarm"
    const val ANSWER_GOOD = "qa_answer_good"
    const val ANSWER_BAD = "qa_answer_bad"
    const val PREVIEW_NOTIF_OFF = "qa_preview_notif_off"
    const val PREVIEW_API32 = "qa_preview_api32"
    const val PREVIEW_RATIONALE = "qa_preview_rationale"
    const val PREVIEW_RATIONALE_BLOCKED = "qa_preview_rationale_blocked"
    const val RESET_ALL = "qa_reset_all"
    const val RESET_PROGRESS = "qa_reset_progress"
    const val SUB_PLUS = "qa_sub_plus"
    const val SHORT_INTERVALS = "qa_short_intervals"
    const val SIMULATE_REBOOT = "qa_simulate_reboot"
    const val PREVIEW_EXACT_DENIED = "qa_preview_exact_denied"
    const val NOTIF_STYLE = "qa_notification_style"
    const val STATE_LAST5 = "qa_state_last5"
    const val STATE_EVENTS = "qa_state_events"
    const val EVENTS_TOGGLE = "qa_state_events_toggle"
    const val AUTO_PAUSE = "qa_auto_pause"
    const val MARK_MISSED = "qa_mark_missed"
    const val SEED_HISTORY = "qa_seed_history"
    const val CLEAR_HISTORY = "qa_clear_history"
    const val END_PAUSE = "qa_end_pause"
    const val STATE_AUTO_PAUSE = "qa_state_auto_pause"

    fun jump(key: String): String = "qa_jump_$key"
    const val KILL_APP = "qa_kill_app"

    fun level(level: Int): String = "qa_level_$level"
    const val BACK = "qa_back"
}

/** Accordion sections implemented so far; later stories add History and Quiet hours. */
internal enum class QaSection(
    @StringRes val title: Int,
    val testTag: String,
    @StringRes val jumpLabel: Int,
    val jumpKey: String,
) {
    PROFILE(R.string.qa_section_profile, "qa_section_profile", R.string.qa_jump_profile, "profile"),
    SESSION(R.string.qa_section_session, "qa_section_session", R.string.qa_jump_session, "session"),
    LEVEL(R.string.qa_section_level, "qa_section_level", R.string.qa_jump_level, "level"),
    HISTORY(R.string.qa_section_history, "qa_section_history", R.string.qa_jump_history, "history"),
    PERMS(R.string.qa_section_perms, "qa_section_perms", R.string.qa_jump_perms, "perms"),
    DANGER(R.string.qa_section_danger, "qa_section_danger", R.string.qa_jump_danger, "danger"),
}

/** Pinned row: State · Profile · Session · Level · Perms · Danger (wraps, never scrolls). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JumpChips(onJump: (QaSection?) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutterCompact),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AssistChip(
            onClick = { onJump(null) },
            label = { Text(stringResource(R.string.qa_jump_state)) },
            modifier = Modifier.testTag(QaTestTags.jump("state")),
        )
        QaSection.entries.forEach { section ->
            AssistChip(
                onClick = { onJump(section) },
                label = { Text(stringResource(section.jumpLabel)) },
                modifier = Modifier.testTag(QaTestTags.jump(section.jumpKey)),
            )
        }
    }
}

private const val JUMP_SCROLL_DELAY_MILLIS = 50L

/** Device facts shown in the readout that don't live in our repositories. */
internal data class QaDeviceInfo(
    val notificationsEnabled: Boolean,
    val exactAlarms: String,
    val systemDark: Boolean,
)

private const val TICK_MILLIS = 1_000L
private const val ACCORDION_MILLIS = 300
private const val LARGE_FONT_SCALE = 1.3f

/** Keeps the last section clear of the snackbar (design sign-off US-02, follow-up 2). */
private val LIST_BOTTOM_PADDING = 72.dp

@Composable
internal fun QaPanelRoute(container: AppContainer, onBack: () -> Unit) {
    val viewModel: QaPanelViewModel = viewModel { QaPanelViewModel(container) }
    val readout by viewModel.readout.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    fun showSnackbar(text: String) {
        scope.launch {
            // Rapid taps: replace the visible snackbar instead of queueing.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when {
                message.themeArg != null ->
                    resources.getString(message.resId, resources.getString(themeLabel(message.themeArg)))
                message.formatArgs.isNotEmpty() ->
                    resources.getString(message.resId, *message.formatArgs.toTypedArray())
                else -> resources.getString(message.resId)
            }
            showSnackbar(text)
        }
    }

    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(TICK_MILLIS)
        }
    }
    val deviceInfo = readDeviceInfo(context, systemDark = ThemeMode.SYSTEM.isDark(), tick = nowMillis)

    var previewRationale by rememberSaveable { mutableStateOf<RationaleMode?>(null) }

    QaPanelScreen(
        readout = readout,
        deviceInfo = deviceInfo,
        nowMillis = nowMillis,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAction = viewModel::onAction,
        onOpenShade = {
            val opened = expandNotificationShade(context)
            showSnackbar(
                resources.getString(if (opened) R.string.qa_msg_shade_opening else R.string.qa_msg_shade_failed),
            )
        },
        onPreviewRationale = { mode -> previewRationale = mode },
        onSetLevel = viewModel::onSetLevel,
        onKillApp = { killAppForPersistenceTest(context) },
    )

    // Visual preview only: both buttons just close the sheet.
    val previewMode = previewRationale
    if (previewMode != null) {
        NotificationRationaleSheet(
            mode = previewMode,
            onPrimary = { previewRationale = null },
            onDismiss = { previewRationale = null },
        )
    }
}

@Suppress("UNUSED_PARAMETER") // tick forces a re-read once per second.
private fun readDeviceInfo(context: Context, systemDark: Boolean, tick: Long): QaDeviceInfo {
    val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (alarmManager?.canScheduleExactAlarms() == true) "granted" else "denied"
    } else {
        "n/a"
    }
    return QaDeviceInfo(
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarms = exact,
        systemDark = systemDark,
    )
}

@StringRes
private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.qa_theme_system
    ThemeMode.LIGHT -> R.string.qa_theme_light
    ThemeMode.DARK -> R.string.qa_theme_dark
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QaPanelScreen(
    readout: QaReadout?,
    deviceInfo: QaDeviceInfo,
    nowMillis: Long,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAction: (QaAction) -> Unit,
    onOpenShade: () -> Unit,
    modifier: Modifier = Modifier,
    onPreviewRationale: (RationaleMode) -> Unit = {},
    onSetLevel: (Int) -> Unit = {},
    onKillApp: () -> Unit = {},
) {
    // Fresh entry: at the top, all sections collapsed (not restored after process death, US-02 §3).
    var openSection by remember { mutableStateOf<QaSection?>(null) }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val stateItems = if (readout != null) 1 else 0
    fun openAndScroll(section: QaSection?) {
        openSection = section
        scope.launch {
            // Let the accordion re-layout before scrolling so the header lands at the top.
            delay(JUMP_SCROLL_DELAY_MILLIS)
            listState.animateScrollToItem(if (section == null) 0 else stateItems + section.ordinal)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.qa_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(QaTestTags.BACK)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.cd_back),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
        // Tap-only reachability (US-02 §3, US-05 B1b): pinned jump chips open a section and scroll it
        // to the top, so every button (incl. Danger zone → Kill app) is ≤ 2 taps away.
        JumpChips(
            onJump = { section -> openAndScroll(section) },
        )
        // One scrolling list with the readout as its first item.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING),
        ) {
                if (readout != null) {
                    item(key = "state") {
                        StateCard(
                            readout = readout,
                            deviceInfo = deviceInfo,
                            nowMillis = nowMillis,
                            modifier = Modifier.padding(horizontal = Dimens.gutterCompact, vertical = Spacing.sm),
                        )
                    }
                }
                QaSection.entries.forEach { section ->
                    item(key = section.name) {
                        AccordionSection(
                            section = section,
                            expanded = openSection == section,
                            onToggle = {
                                if (openSection == section) openSection = null else openAndScroll(section)
                            },
                        ) {
                            SectionButtons(
                                section = section,
                                themeMode = readout?.settings?.themeMode ?: ThemeMode.SYSTEM,
                                overrides = readout?.overrides ?: DebugOverrides(),
                                onAction = onAction,
                                onOpenShade = onOpenShade,
                                onPreviewRationale = onPreviewRationale,
                                onSetLevel = onSetLevel,
                                onKillApp = onKillApp,
                            )
                        }
                    }
                }
        }
        }
    }
}

@Composable
private fun StateCard(
    readout: QaReadout,
    deviceInfo: QaDeviceInfo,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    var expanded by rememberSaveable { mutableStateOf(false) }
    val session = readout.session
    val settings = readout.settings
    val levelText = QaFormat.level(
        level = session.srs.level,
        subLevel = session.srs.subLevel,
        subLevelCount = readout.levelInfo.subLevelCount,
        name = readout.levelInfo.name,
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(QaTestTags.STATE),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            if (largeFont) {
                // At large font scales the readout collapses to a one-line summary (US-02 §3).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.minTouchTarget)
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.qa_state_header) + ": " + QaFormat.session(session) +
                            " · L${session.srs.level} ${session.srs.subLevel}/${readout.levelInfo.subLevelCount}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    ExpandIcon(expanded = expanded)
                }
            }
            if (!largeFont || expanded) {
                StateRow(R.string.qa_label_session, QaFormat.session(session), QaTestTags.STATE_SESSION)
                StateRow(R.string.qa_label_level, levelText, QaTestTags.STATE_LEVEL)
                StateRow(
                    R.string.qa_label_interval,
                    QaFormat.interval(readout.levelInfo.interval) +
                        if (readout.overrides.shortIntervalsActive) {
                            "  [short: ${readout.overrides.intervalScale.scale(readout.levelInfo.interval).inWholeSeconds} s]"
                        } else {
                            ""
                        },
                )
                StateRow(
                    R.string.qa_label_next_alarm,
                    QaFormat.nextAlarm(session.nextAlarmAtMillis, nowMillis),
                    QaTestTags.STATE_NEXT_ALARM,
                )
                StateRow(R.string.qa_label_pending, QaFormat.pending(session.pendingCheckInAtMillis))
                StateRow(R.string.qa_label_misses, QaFormat.misses(session.consecutiveMisses), QaTestTags.STATE_MISSES)
                StateRow(R.string.qa_label_pause, QaFormat.pause(session))
                StateRow(
                    R.string.qa_label_short,
                    if (readout.overrides.shortIntervalsActive) "ON (1 min = 1 s)" else "OFF",
                    QaTestTags.STATE_SHORT,
                )
                StateRow(
                    R.string.qa_label_quiet,
                    QaFormat.quietHours(settings.quietHoursEnabled, settings.quietHoursPreset),
                    QaTestTags.STATE_QUIET,
                )
                StateRow(
                    R.string.qa_label_perms,
                    (if (deviceInfo.notificationsEnabled) "granted" else "denied") + "  Exact: " + deviceInfo.exactAlarms +
                        if (readout.overrides.previewExactAlarmsDenied) " (preview: denied)" else "",
                )
                StateRow(
                    R.string.qa_label_theme,
                    stringResource(themeLabel(settings.themeMode)) +
                        if (settings.themeMode == ThemeMode.SYSTEM) {
                            if (deviceInfo.systemDark) " (dark)" else " (light)"
                        } else {
                            ""
                        },
                )
                StateRow(R.string.qa_label_name, settings.name ?: QaFormat.NONE)
                StateRow(
                    R.string.qa_label_disclaimer,
                    if (settings.disclaimerDismissed) "dismissed" else "shown",
                )
                StateRow(R.string.qa_label_history, readout.historyCount.toString())
                StateRow(R.string.qa_label_last5, QaFormat.lastFive(readout.recentEvents), QaTestTags.STATE_LAST5)
                StateRow(
                    R.string.qa_label_auto_pause,
                    if (readout.overrides.autoPause) "ON (after 3 misses)" else "OFF",
                    QaTestTags.STATE_AUTO_PAUSE,
                )
                StateRow(
                    R.string.qa_label_notif_style,
                    if (readout.overrides.standardNotificationButtons) "standard actions" else "custom pills",
                )
                StateRow(
                    R.string.qa_label_previews,
                    "notif-off " + (if (readout.overrides.previewNotificationsOff) "ON" else "OFF") +
                        " · api32 " + (if (readout.overrides.previewLegacyNotificationPermission) "ON" else "OFF") +
                        " · exact-denied " + (if (readout.overrides.previewExactAlarmsDenied) "ON" else "OFF"),
                )
                // Last 10 events, newest first (US-04 O1 diagnostics); collapsed by default.
                var eventsExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.minTouchTarget)
                        .clickable { eventsExpanded = !eventsExpanded }
                        .testTag(QaTestTags.EVENTS_TOGGLE),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.qa_label_last_events, readout.recentEvents.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    ExpandIcon(expanded = eventsExpanded)
                }
                if (eventsExpanded) {
                    Text(
                        text = QaFormat.lastEvents(readout.recentEvents),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace).tabular(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(QaTestTags.STATE_EVENTS),
                    )
                }
            }
        }
    }
}

@Composable
private fun StateRow(@StringRes label: Int, value: String, testTag: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .semantics(mergeDescendants = true) {}
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace).tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExpandIcon(expanded: Boolean) {
    Icon(
        painter = painterResource(R.drawable.ic_expand_more),
        contentDescription = stringResource(if (expanded) R.string.qa_cd_collapse else R.string.qa_cd_expand),
        modifier = Modifier
            .size(24.dp)
            .rotate(if (expanded) 180f else 0f),
    )
}

@Composable
private fun AccordionSection(
    section: QaSection,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(section.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = { ExpandIcon(expanded = expanded) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .clickable(onClick = onToggle)
                .testTag(section.testTag),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(ACCORDION_MILLIS)) + fadeIn(tween(ACCORDION_MILLIS)),
            exit = shrinkVertically(tween(ACCORDION_MILLIS)) + fadeOut(tween(ACCORDION_MILLIS)),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionButtons(
    section: QaSection,
    themeMode: ThemeMode,
    overrides: DebugOverrides,
    onAction: (QaAction) -> Unit,
    onOpenShade: () -> Unit,
    onPreviewRationale: (RationaleMode) -> Unit,
    onSetLevel: (Int) -> Unit,
    onKillApp: () -> Unit,
) {
    if (section == QaSection.LEVEL) {
        LevelButtons(onSetLevel = onSetLevel, onSubLevelPlus = { onAction(QaAction.SUB_LEVEL_PLUS) })
        return
    }
    val on = stringResource(R.string.qa_on)
    val off = stringResource(R.string.qa_off)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutterCompact, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxItemsInEachRow = 2,
    ) {
        when (section) {
            QaSection.PROFILE -> {
                QaButton(stringResource(R.string.qa_btn_set_name), QaTestTags.SET_NAME) {
                    onAction(QaAction.SET_NAME_ALEX)
                }
                QaButton(stringResource(R.string.qa_btn_clear_name), QaTestTags.CLEAR_NAME) {
                    onAction(QaAction.CLEAR_NAME)
                }
                QaButton(
                    stringResource(R.string.qa_btn_theme, stringResource(themeLabel(themeMode))),
                    QaTestTags.THEME_CYCLE,
                ) { onAction(QaAction.CYCLE_THEME) }
                QaButton(stringResource(R.string.qa_btn_show_disclaimer), QaTestTags.SHOW_DISCLAIMER) {
                    onAction(QaAction.SHOW_DISCLAIMER)
                }
            }

            QaSection.SESSION -> {
                // Mark missed / Short intervals / End pause / Simulate reboot arrive with US-05..US-07.
                QaButton(stringResource(R.string.qa_btn_fire_alarm), QaTestTags.FIRE_ALARM) {
                    onAction(QaAction.FIRE_ALARM)
                }
                QaButton(stringResource(R.string.qa_btn_mark_missed), QaTestTags.MARK_MISSED) {
                    onAction(QaAction.MARK_MISSED)
                }
                QaButton(stringResource(R.string.qa_btn_open_shade), QaTestTags.OPEN_SHADE, onClick = onOpenShade)
                QaButton(
                    stringResource(R.string.qa_btn_short_intervals, if (overrides.shortIntervalsActive) on else off),
                    QaTestTags.SHORT_INTERVALS,
                ) { onAction(QaAction.TOGGLE_SHORT_INTERVALS) }
                QaButton(stringResource(R.string.qa_btn_simulate_reboot), QaTestTags.SIMULATE_REBOOT) {
                    onAction(QaAction.SIMULATE_REBOOT)
                }
                QaButton(
                    stringResource(R.string.qa_btn_auto_pause, if (overrides.autoPause) on else off),
                    QaTestTags.AUTO_PAUSE,
                ) { onAction(QaAction.TOGGLE_AUTO_PAUSE) }
                QaButton(stringResource(R.string.qa_btn_end_pause), QaTestTags.END_PAUSE) {
                    onAction(QaAction.END_PAUSE)
                }
                QaButton(
                    stringResource(
                        if (overrides.standardNotificationButtons) {
                            R.string.qa_btn_notif_style_standard
                        } else {
                            R.string.qa_btn_notif_style_custom
                        },
                    ),
                    QaTestTags.NOTIF_STYLE,
                ) { onAction(QaAction.TOGGLE_NOTIFICATION_STYLE) }
                val extended = SuperUnclenchTheme.extendedColors
                QaButton(
                    label = stringResource(R.string.qa_btn_answer_good),
                    testTag = QaTestTags.ANSWER_GOOD,
                    containerColor = extended.goodContainer,
                    contentColor = extended.onGoodContainer,
                ) { onAction(QaAction.ANSWER_GOOD) }
                QaButton(
                    label = stringResource(R.string.qa_btn_answer_bad),
                    testTag = QaTestTags.ANSWER_BAD,
                    containerColor = extended.badContainer,
                    contentColor = extended.onBadContainer,
                ) { onAction(QaAction.ANSWER_BAD) }
            }

            QaSection.PERMS -> {
                QaButton(
                    stringResource(R.string.qa_btn_preview_notif_off, if (overrides.previewNotificationsOff) on else off),
                    QaTestTags.PREVIEW_NOTIF_OFF,
                ) { onAction(QaAction.TOGGLE_PREVIEW_NOTIFICATIONS_OFF) }
                QaButton(
                    stringResource(
                        R.string.qa_btn_preview_api32,
                        if (overrides.previewLegacyNotificationPermission) on else off,
                    ),
                    QaTestTags.PREVIEW_API32,
                ) { onAction(QaAction.TOGGLE_PREVIEW_LEGACY_PERMISSION) }
                QaButton(
                    stringResource(R.string.qa_btn_preview_exact_denied, if (overrides.previewExactAlarmsDenied) on else off),
                    QaTestTags.PREVIEW_EXACT_DENIED,
                ) { onAction(QaAction.TOGGLE_PREVIEW_EXACT_DENIED) }
                QaButton(stringResource(R.string.qa_btn_preview_rationale), QaTestTags.PREVIEW_RATIONALE) {
                    onPreviewRationale(RationaleMode.ASK)
                }
                QaButton(
                    stringResource(R.string.qa_btn_preview_rationale_blocked),
                    QaTestTags.PREVIEW_RATIONALE_BLOCKED,
                ) { onPreviewRationale(RationaleMode.BLOCKED) }
            }

            QaSection.DANGER -> {
                QaDangerButton(stringResource(R.string.qa_btn_reset_all), QaTestTags.RESET_ALL) {
                    onAction(QaAction.RESET_ALL)
                }
                QaDangerButton(stringResource(R.string.qa_btn_reset_progress), QaTestTags.RESET_PROGRESS) {
                    onAction(QaAction.RESET_PROGRESS)
                }
                QaDangerButton(stringResource(R.string.qa_btn_kill_app), QaTestTags.KILL_APP, onClick = onKillApp)
            }

            QaSection.LEVEL -> Unit

            QaSection.HISTORY -> {
                QaButton(stringResource(R.string.qa_btn_seed_history), QaTestTags.SEED_HISTORY) {
                    onAction(QaAction.SEED_HISTORY)
                }
                QaButton(stringResource(R.string.qa_btn_clear_history), QaTestTags.CLEAR_HISTORY) {
                    onAction(QaAction.CLEAR_HISTORY)
                }
            }
        }
    }
}

/** "Set level N" (1–8, square 48dp buttons: one row on ≥ 400dp, else 2 rows of 4) + "Sub-level +1". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelButtons(onSetLevel: (Int) -> Unit, onSubLevelPlus: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutterCompact, vertical = Spacing.sm),
    ) {
        val perRow = if (maxWidth >= 400.dp) 8 else 4
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                maxItemsInEachRow = perRow,
            ) {
                for (level in 1..8) {
                    val description = stringResource(R.string.qa_cd_set_level, level)
                    FilledTonalButton(
                        onClick = { onSetLevel(level) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.minTouchTarget)
                            .testTag(QaTestTags.level(level))
                            .semantics { contentDescription = description },
                    ) {
                        Text(text = level.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Row {
                QaButton(stringResource(R.string.qa_btn_sub_plus), QaTestTags.SUB_PLUS, onClick = onSubLevelPlus)
            }
        }
    }
}

/**
 * Debug: finish the task and kill the process so QA can relaunch from the launcher and verify that
 * state survives (US-04 "force-stop & relaunch" criterion). Alarms stay scheduled.
 */
private fun killAppForPersistenceTest(context: Context) {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) {
            current.finishAffinity()
            break
        }
        current = current.baseContext
    }
    Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, KILL_DELAY_MILLIS)
}

private const val KILL_DELAY_MILLIS = 300L

@Composable
private fun RowScope.QaButton(
    label: String,
    testTag: String,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val defaults = ButtonDefaults.filledTonalButtonColors()
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = Dimens.buttonHeight)
            .testTag(testTag),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (containerColor == Color.Unspecified) defaults.containerColor else containerColor,
            contentColor = if (contentColor == Color.Unspecified) defaults.contentColor else contentColor,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RowScope.QaDangerButton(label: String, testTag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = Dimens.buttonHeight)
            .testTag(testTag),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

// region Previews

private val PreviewReadout = QaReadout(
    settings = UserSettings(name = "Alex"),
    session = SessionState(),
    levelInfo = SrsLevels.DEFAULT.first(),
    historyCount = 0,
)

private val PreviewDevice = QaDeviceInfo(notificationsEnabled = true, exactAlarms = "denied", systemDark = false)

@Preview(name = "QA panel - light", showBackground = true, heightDp = 900)
@Composable
private fun QaPanelLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        QaPanelScreen(
            readout = PreviewReadout,
            deviceInfo = PreviewDevice,
            nowMillis = 0L,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onAction = {},
            onOpenShade = {},
        )
    }
}

@Preview(name = "QA panel - dark", showBackground = true, heightDp = 900)
@Composable
private fun QaPanelDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        QaPanelScreen(
            readout = PreviewReadout,
            deviceInfo = PreviewDevice,
            nowMillis = 0L,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onAction = {},
            onOpenShade = {},
        )
    }
}

// endregion
