package com.mfinatti.noclenchingsrs.feature.settings.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.settings.quietDurationMinutes
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import java.util.Calendar

/** Test tags from docs/design/US-10-quiet-hours.md §9. */
object QuietTestTags {
    const val SWITCH = "quiet_switch"
    const val STARTS = "quiet_starts"
    const val ENDS = "quiet_ends"
    const val DURATION = "quiet_duration"
    const val PICKER = "quiet_picker"
    const val PICKER_MODE_TOGGLE = "quiet_picker_mode_toggle"
    const val PICKER_OK = "quiet_picker_ok"
    const val PICKER_CANCEL = "quiet_picker_cancel"
    const val PICKER_ERROR = "quiet_picker_error"
}

/** Which end of the range a picker edits. */
enum class QuietEnd { START, END }

private const val DISABLED_ALPHA = 0.38f
private const val ROW_INDENT_DP = 56

/** Clock text in the device's 12/24 h format ("10:00 PM" / "22:00"). */
fun quietClock(context: Context, minutes: Int): String = DateFormat.getTimeFormat(context).format(
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
    }.time,
)

/**
 * US-10 §2 (revised): Quiet hours switch, Starts/Ends rows that open an M3 time picker, and the
 * "9 h of quiet" line. With the switch off the rows stay visible at 38% and can't be tapped.
 */
@Composable
fun QuietHoursSettings(
    enabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onRangeChange: (startMinutes: Int, endMinutes: Int) -> Unit,
) {
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<QuietEnd?>(null) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.quiet_title)) },
        supportingContent = { Text(stringResource(R.string.quiet_support)) },
        leadingContent = { QuietRowIcon(R.drawable.ic_bedtime) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .heightIn(min = 72.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
            .testTag(QuietTestTags.SWITCH),
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    val alpha by animateFloatAsState(if (enabled) 1f else DISABLED_ALPHA, tween(150), label = "quietRows")
    val nextDay = endMinutes <= startMinutes
    Column(
        modifier = Modifier
            .padding(bottom = Spacing.lg)
            .graphicsLayer { this.alpha = alpha },
    ) {
        TimeRow(
            label = stringResource(R.string.quiet_starts),
            time = quietClock(context, startMinutes),
            caption = null,
            enabled = enabled,
            tag = QuietTestTags.STARTS,
            onClick = { editing = QuietEnd.START },
        )
        TimeRow(
            label = stringResource(R.string.quiet_ends),
            time = quietClock(context, endMinutes),
            caption = if (nextDay) stringResource(R.string.quiet_next_day) else null,
            enabled = enabled,
            tag = QuietTestTags.ENDS,
            onClick = { editing = QuietEnd.END },
        )
        QuietDuration(minutes = quietDurationMinutes(startMinutes, endMinutes))
    }
    editing?.let { which ->
        QuietTimePickerDialog(
            which = which,
            initialMinutes = if (which == QuietEnd.START) startMinutes else endMinutes,
            otherMinutes = if (which == QuietEnd.START) endMinutes else startMinutes,
            onConfirm = { minutes ->
                editing = null
                if (which == QuietEnd.START) onRangeChange(minutes, endMinutes) else onRangeChange(startMinutes, minutes)
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: String,
    caption: String?,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val spoken = listOfNotNull(label, time, caption?.lowercase()).joinToString(", ")
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .heightIn(min = 56.dp)
            .clickable(
                enabled = enabled,
                onClickLabel = stringResource(R.string.quiet_change_action),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = spoken }
            .padding(start = (ROW_INDENT_DP - 16).dp)
            .testTag(tag),
    )
}

@Composable
private fun QuietDuration(minutes: Int) {
    val hours = minutes / 60
    val mins = minutes % 60
    val short = when {
        hours == 0 -> stringResource(R.string.duration_short_min, mins)
        mins == 0 -> stringResource(R.string.duration_short_h, hours)
        else -> stringResource(R.string.duration_short_h_min, hours, mins)
    }
    val resources = LocalResources.current
    val spokenParts = listOfNotNull(
        hours.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.duration_hours, it, it) },
        mins.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.duration_minutes, it, it) },
    ).joinToString(" ")
    val spoken = stringResource(R.string.quiet_duration, spokenParts)
    Text(
        text = stringResource(R.string.quiet_duration, short),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = ROW_INDENT_DP.dp, top = Spacing.sm, end = Spacing.lg)
            .semantics { contentDescription = spoken }
            .testTag(QuietTestTags.DURATION),
    )
}

/**
 * US-10 §2.1: M3 time picker (dial ↔ keyboard input) in a basic dialog. OK is disabled while the
 * value equals the other end of the range (AC4).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun QuietTimePickerDialog(
    which: QuietEnd,
    initialMinutes: Int,
    otherMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val picked = state.hour * 60 + state.minute
    val sameAsOther = picked == otherMinutes
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            // Dialogs are separate windows: re-enable test tags as resource ids for adb/uiautomator.
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(QuietTestTags.PICKER),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        if (which == QuietEnd.START) R.string.quiet_picker_title_start else R.string.quiet_picker_title_end,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.xl),
                )
                // Designer follow-up: calmer AM/PM toggle (secondary instead of the default tertiary).
                val pickerColors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Crossfade(targetState = keyboard, animationSpec = tween(150), label = "pickerMode") { input ->
                    if (input) {
                        TimeInput(state = state, colors = pickerColors)
                    } else {
                        TimePicker(state = state, colors = pickerColors)
                    }
                }
                if (sameAsOther) {
                    Text(
                        text = stringResource(R.string.quiet_picker_error_same),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag(QuietTestTags.PICKER_ERROR),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = { keyboard = !keyboard },
                        modifier = Modifier.testTag(QuietTestTags.PICKER_MODE_TOGGLE),
                    ) {
                        Icon(
                            painter = painterResource(if (keyboard) R.drawable.ic_missed else R.drawable.ic_keyboard),
                            contentDescription = stringResource(
                                if (keyboard) R.string.quiet_picker_to_dial_cd else R.string.quiet_picker_to_input_cd,
                            ),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag(QuietTestTags.PICKER_CANCEL)) {
                        Text(stringResource(R.string.quiet_picker_cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(picked) },
                        enabled = !sameAsOther,
                        modifier = Modifier.testTag(QuietTestTags.PICKER_OK),
                    ) {
                        Text(stringResource(R.string.quiet_picker_ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietRowIcon(@DrawableRes icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}

@Preview(name = "Quiet hours — overnight", showBackground = true)
@Composable
private fun QuietHoursSettingsPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                QuietHoursSettings(
                    enabled = true,
                    startMinutes = 23 * 60 + 30,
                    endMinutes = 6 * 60 + 15,
                    onEnabledChange = {},
                    onRangeChange = { _, _ -> },
                )
            }
        }
    }
}

@Preview(name = "Quiet hours — off, same day", showBackground = true)
@Composable
private fun QuietHoursSettingsOffPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                QuietHoursSettings(
                    enabled = false,
                    startMinutes = 13 * 60,
                    endMinutes = 14 * 60,
                    onEnabledChange = {},
                    onRangeChange = { _, _ -> },
                )
            }
        }
    }
}
