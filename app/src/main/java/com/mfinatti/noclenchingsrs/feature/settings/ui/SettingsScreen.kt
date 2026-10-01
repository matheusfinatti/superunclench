package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.feature.checkin.ring.FullScreenStatus
import com.mfinatti.noclenchingsrs.feature.checkin.ring.RingSettingsLinks
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mfinatti.noclenchingsrs.BuildConfig
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.di.LocalAppContainer
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.ui.components.AppSnackbarHost
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test tags from docs/design/US-09-settings.md §9. */
object SettingsTestTags {
    const val NAME_FIELD = "settings_name_field"
    const val NAME_CLEAR = "settings_name_clear"
    const val THEME_LIGHT = "theme_light"
    const val THEME_DARK = "theme_dark"
    const val THEME_SYSTEM = "theme_system"
    const val RESET = "settings_reset"
    const val DIALOG_CONFIRM = "dialog_reset_confirm"
    const val DIALOG_CANCEL = "dialog_reset_cancel"
    const val ABOUT = "settings_about"
    const val QUIET_SWITCH = QuietTestTags.SWITCH
}

/** Max name length (US-09 §2); the counter appears from [NAME_COUNTER_FROM] characters. */
const val NAME_MAX_LENGTH = 24
private const val NAME_COUNTER_FROM = 20
private const val NAME_SAVE_DEBOUNCE_MILLIS = 300L

@Composable
fun SettingsRoute(
    onOpenAbout: () -> Unit,
    debugEntry: @Composable () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            settingsRepository = container.settingsRepository,
            checkInController = container.checkInController,
            initial = container.settingsState.value ?: UserSettings(),
        )
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    // US-11: permission state behind the Ring banners, re-checked on every resume.
    val overrides by container.debugOverridesRepository.overrides.collectAsStateWithLifecycle(initialValue = null)
    var fsiGranted by remember { mutableStateOf(FullScreenStatus.canUseFullScreenIntent(context)) }
    var exactGranted by remember { mutableStateOf(container.alarmScheduler.canScheduleExactAlarms()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        fsiGranted = FullScreenStatus.canUseFullScreenIntent(context)
        exactGranted = container.alarmScheduler.canScheduleExactAlarms()
    }
    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    SettingsScreen(
        settings = settings,
        onNameChange = viewModel::onNameChange,
        onThemeChange = viewModel::onThemeChange,
        onResetConfirmed = {
            scope.launch {
                viewModel.resetProgress()
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(resources.getString(R.string.snackbar_reset))
            }
        },
        onOpenAbout = onOpenAbout,
        onQuietEnabledChange = { enabled ->
            viewModel.onQuietHoursEnabled(enabled)
            showSnackbar(resources.getString(if (enabled) R.string.snackbar_quiet_on else R.string.snackbar_quiet_off))
        },
        fullScreenDenied = !fsiGranted || overrides?.previewFullScreenDenied == true,
        exactDenied = !exactGranted || overrides?.previewExactAlarmsDenied == true,
        onAlertStyleChange = { style ->
            viewModel.onAlertStyle(style)
            val name = resources.getString(if (style == AlertStyle.RING) R.string.alert_ring else R.string.alert_nudge)
            showSnackbar(resources.getString(R.string.snackbar_alert_style, name))
        },
        onAllowFullScreen = { RingSettingsLinks.openFullScreenSettings(context) },
        onAllowExact = { RingSettingsLinks.openExactAlarmSettings(context) },
        onQuietRangeChange = { start, end ->
            viewModel.onQuietHoursRange(start, end)
            showSnackbar(
                resources.getString(R.string.snackbar_quiet_range, quietClock(context, start), quietClock(context, end)),
            )
        },
        snackbarHostState = snackbarHostState,
        debugEntry = debugEntry,
    )
}

/**
 * Settings (US-09): Profile (name), Appearance (theme), Progress (reset), About, and the debug-only
 * Developer group ([debugEntry]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onNameChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onResetConfirmed: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    onQuietEnabledChange: (Boolean) -> Unit = {},
    onQuietRangeChange: (startMinutes: Int, endMinutes: Int) -> Unit = { _, _ -> },
    fullScreenDenied: Boolean = false,
    exactDenied: Boolean = false,
    onAlertStyleChange: (AlertStyle) -> Unit = {},
    onAllowFullScreen: () -> Unit = {},
    onAllowExact: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    debugEntry: @Composable () -> Unit = {},
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.gutterCompact)
                .padding(bottom = Dimens.bottomContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = Dimens.maxContentWidth)) {
                SettingsGroup(title = stringResource(R.string.settings_group_profile)) {
                    NameField(initialName = settings.name.orEmpty(), onNameChange = onNameChange)
                }
                SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
                    ThemeSelector(mode = settings.themeMode, onThemeChange = onThemeChange)
                }
                SettingsGroup(title = stringResource(R.string.settings_group_checkins)) {
                    AlertStyleSettings(
                        style = settings.alertStyle,
                        fullScreenDenied = fullScreenDenied,
                        exactDenied = exactDenied,
                        onStyleChange = onAlertStyleChange,
                        onAllowFullScreen = onAllowFullScreen,
                        onAllowExact = onAllowExact,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    QuietHoursSettings(
                        enabled = settings.quietHoursEnabled,
                        startMinutes = settings.quietStartMinutes,
                        endMinutes = settings.quietEndMinutes,
                        onEnabledChange = onQuietEnabledChange,
                        onRangeChange = onQuietRangeChange,
                    )
                }
                SettingsGroup(title = stringResource(R.string.settings_group_progress)) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error)
                        },
                        supportingContent = { Text(stringResource(R.string.settings_reset_support)) },
                        leadingContent = { RowIcon(R.drawable.ic_restart) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .heightIn(min = 72.dp)
                            .clickable { showResetDialog = true }
                            .testTag(SettingsTestTags.RESET),
                    )
                }
                SettingsGroup(title = stringResource(R.string.settings_group_about)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_about)) },
                        supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) },
                        leadingContent = { RowIcon(R.drawable.ic_info) },
                        trailingContent = { RowIcon(R.drawable.ic_chevron_right) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .heightIn(min = 72.dp)
                            .clickable(onClick = onOpenAbout)
                            .testTag(SettingsTestTags.ABOUT),
                    )
                }
                debugEntry()
            }
        }
    }
    if (showResetDialog) {
        ResetDialog(
            onConfirm = {
                showResetDialog = false
                onResetConfirmed()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

/** A titled settings group: `titleSmall` `primary` header, then a `surfaceContainer` card. */
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = Spacing.lg, bottom = Spacing.sm)
                .semantics { heading() },
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            content = content,
        )
    }
}

@Composable
private fun RowIcon(@DrawableRes icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}

/**
 * Name field: saved as you type (debounced 300ms, trimmed; empty = no name), max 24 characters.
 * Stable test tag / resource-id `settings_name_field` so QA can focus it and use `adb shell input text`.
 */
@Composable
fun NameField(initialName: String, onNameChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf(initialName) }
    var edited by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(text, edited) {
        if (!edited) return@LaunchedEffect
        delay(NAME_SAVE_DEBOUNCE_MILLIS)
        onNameChange(text.trim())
    }
    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            text = value.replace("\n", "").take(NAME_MAX_LENGTH)
            edited = true
        },
        label = { Text(stringResource(R.string.settings_name_label)) },
        supportingText = {
            if (text.length >= NAME_COUNTER_FROM) {
                Text("${text.length}/$NAME_MAX_LENGTH")
            } else {
                Text(stringResource(R.string.settings_name_support))
            }
        },
        leadingIcon = { RowIcon(R.drawable.ic_person) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        text = ""
                        edited = true
                    },
                    modifier = Modifier.testTag(SettingsTestTags.NAME_CLEAR),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bad),
                        contentDescription = stringResource(R.string.settings_name_clear_cd),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .testTag(SettingsTestTags.NAME_FIELD),
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(mode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, R.string.theme_light, SettingsTestTags.THEME_LIGHT),
        Triple(ThemeMode.DARK, R.string.theme_dark, SettingsTestTags.THEME_DARK),
        Triple(ThemeMode.SYSTEM, R.string.theme_system, SettingsTestTags.THEME_SYSTEM),
    )
    Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_theme)) },
            leadingContent = { RowIcon(R.drawable.ic_palette) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        ) {
            options.forEachIndexed { index, (value, label, tag) ->
                SegmentedButton(
                    selected = mode == value,
                    onClick = { onThemeChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier
                        .heightIn(min = Dimens.minTouchTarget)
                        .testTag(tag),
                    label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}

@Composable
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { RowIcon(R.drawable.ic_restart) },
        title = { Text(stringResource(R.string.reset_dialog_title)) },
        text = { Text(stringResource(R.string.reset_dialog_body)) },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(SettingsTestTags.DIALOG_CANCEL),
            ) { Text(stringResource(R.string.reset_dialog_cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(SettingsTestTags.DIALOG_CONFIRM),
            ) { Text(stringResource(R.string.reset_dialog_confirm)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

// region Previews

@Preview(name = "Settings - light", showBackground = true, heightDp = 900)
@Composable
private fun SettingsLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        SettingsScreen(
            settings = UserSettings(name = "Alex"),
            onNameChange = {},
            onThemeChange = {},
            onResetConfirmed = {},
            onOpenAbout = {},
        )
    }
}

@Preview(name = "Settings - dark", showBackground = true, heightDp = 900)
@Composable
private fun SettingsDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        SettingsScreen(
            settings = UserSettings(themeMode = ThemeMode.DARK),
            onNameChange = {},
            onThemeChange = {},
            onResetConfirmed = {},
            onOpenAbout = {},
        )
    }
}

@Preview(name = "Reset dialog - light", showBackground = true)
@Composable
private fun ResetDialogPreview() {
    SuperUnclenchTheme(darkTheme = false) { ResetDialog(onConfirm = {}, onDismiss = {}) }
}

// endregion
