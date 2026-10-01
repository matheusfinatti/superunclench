package com.mfinatti.noclenchingsrs.feature.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.settings.QuietHoursRule
import com.mfinatti.noclenchingsrs.ui.components.QuietChip
import com.mfinatti.noclenchingsrs.ui.components.StatusChip
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Test tags from docs/design/US-06-countdown-pause-resume.md §8 (plus US-03's). */
object SessionCardTestTags {
    const val CARD = "session_card"
    const val COUNTDOWN = "countdown"
    const val COUNTDOWN_AT = "countdown_at"
    const val PAUSE = "btn_pause"
    const val PAUSE_15 = "pause_chip_15"
    const val PAUSE_60 = "pause_chip_60"
    const val PAUSE_INDEF = "pause_chip_indef"
    const val PAUSE_CANCEL = "pause_cancel"
    const val PAUSED_UNTIL = "paused_until"
    const val RESUME = "btn_resume"
    const val AUTOPAUSE_BANNER = "autopause_banner"
    const val RINGING_NOW = "session_ringing_now"
    const val QUIET_STATE = "quiet_state"
    const val QUIET_NEXT_AT = "quiet_next_at"
}

/** Pause durations offered by the picker (real time; debug short mode scales them). */
enum class PauseChoice(val duration: Duration?) {
    FIFTEEN_MINUTES(15.minutes),
    ONE_HOUR(1.hours),
    UNTIL_RESUME(null),
}

private const val TICK_MILLIS = 1_000L
private const val CROSSFADE_MILLIS = 300
private const val STACK_FONT_SCALE = 1.5f
private val CANCEL_TRIM = 12.dp
private const val QUIET_TICK_MILLIS = 15_000L

/** "04:59" under 1 h, "1:59:59" at 1 h or more; never negative (US-06 §2.1). */
fun formatCountdown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Home slot D (US-03 + US-06): status chip, countdown / paused state, and the Start, Pause,
 * Resume and Stop controls with the inline pause picker.
 */
@Composable
fun SessionCard(
    status: SessionStatus,
    nextAlarmAtMillis: Long?,
    pausedUntilMillis: Long?,
    autoPaused: Boolean,
    notificationsBlocked: Boolean,
    intervalLabel: String,
    quiet: QuietHoursRule = QuietHoursRule.Off,
    /** US-11: a Ring check-in is unresolved, so no countdown runs ("Ringing now"). */
    ringing: Boolean = false,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPause: (PauseChoice) -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    val stacked = LocalDensity.current.fontScale >= STACK_FONT_SCALE
    var picking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (status != SessionStatus.RUNNING) picking = false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SessionCardTestTags.CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Quiet hours are evaluated against a slow clock so the card flips at the window edges.
            val clock by produceState(initialValue = nowProvider(), quiet) {
                while (true) {
                    delay(QUIET_TICK_MILLIS)
                    value = nowProvider()
                }
            }
            val quietActive = status == SessionStatus.RUNNING && quiet.isActive(clock)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusChip(status = status, modifier = Modifier.testTag(HomeTestTags.STATUS_CHIP))
                if (quietActive) QuietChip()
            }
            AnimatedContent(
                targetState = status,
                transitionSpec = { fadeIn(tween(CROSSFADE_MILLIS)) togetherWith fadeOut(tween(CROSSFADE_MILLIS)) },
                label = "sessionState",
            ) { shownStatus ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    when (shownStatus) {
                        SessionStatus.STOPPED -> StoppedContent(intervalLabel = intervalLabel, onStartClick = onStartClick)
                        SessionStatus.RUNNING -> {
                            if (ringing) {
                                RingingNow()
                            } else if (quietActive) {
                                QuietHoursContent(nextAlarmAtMillis = nextAlarmAtMillis)
                            } else {
                                Countdown(nextAlarmAtMillis = nextAlarmAtMillis, nowProvider = nowProvider)
                            }
                            NotificationsBlockedCaption(visible = notificationsBlocked)
                            AnimatedContent(
                                targetState = picking,
                                transitionSpec = {
                                    fadeIn(tween(CROSSFADE_MILLIS)) togetherWith fadeOut(tween(CROSSFADE_MILLIS))
                                },
                                label = "pausePicker",
                            ) { showPicker ->
                                if (showPicker) {
                                    PausePicker(
                                        onChoose = { choice ->
                                            picking = false
                                            onPause(choice)
                                        },
                                        onCancel = { picking = false },
                                    )
                                } else {
                                    ButtonPair(
                                        stacked = stacked,
                                        first = { m ->
                                            FilledTonalButton(
                                                onClick = { picking = true },
                                                modifier = m.testTag(SessionCardTestTags.PAUSE),
                                            ) { ButtonContent(R.drawable.ic_pause, R.string.action_pause) }
                                        },
                                        second = { m -> StopButton(onStopClick = onStopClick, modifier = m) },
                                    )
                                }
                            }
                        }
                        SessionStatus.PAUSED -> {
                            if (autoPaused) {
                                AutoPausedBanner()
                            } else {
                                PausedContent(pausedUntilMillis = pausedUntilMillis)
                            }
                            NotificationsBlockedCaption(visible = notificationsBlocked)
                            ButtonPair(
                                stacked = stacked,
                                first = { m ->
                                    Button(
                                        onClick = onResume,
                                        modifier = m.testTag(SessionCardTestTags.RESUME),
                                    ) { ButtonContent(R.drawable.ic_play, R.string.action_resume) }
                                },
                                second = { m -> StopButton(onStopClick = onStopClick, modifier = m) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoppedContent(intervalLabel: String, onStartClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.primaryCtaHeight)
                .testTag(HomeTestTags.START_BUTTON),
        ) { ButtonContent(R.drawable.ic_play, R.string.action_start) }
        Text(
            text = stringResource(R.string.session_stopped_helper, intervalLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuietHoursContent(nextAlarmAtMillis: Long?) {
    val context = LocalContext.current
    val time = remember(nextAlarmAtMillis) {
        nextAlarmAtMillis?.let { android.text.format.DateFormat.getTimeFormat(context).format(Date(it)) } ?: ""
    }
    val a11y = stringResource(R.string.quiet_a11y, time)
    Column(
        modifier = Modifier
            .testTag(SessionCardTestTags.QUIET_STATE)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.quiet_overline).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.quiet_next_at, time),
            style = MaterialTheme.typography.titleLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SessionCardTestTags.QUIET_NEXT_AT),
        )
        Text(
            text = stringResource(R.string.quiet_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** US-11 §5: replaces the countdown while a Ring check-in is unresolved. */
@Composable
private fun RingingNow() {
    val a11y = stringResource(R.string.session_ringing_now_a11y)
    Column(
        modifier = Modifier
            .testTag(SessionCardTestTags.RINGING_NOW)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.next_overline).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.session_ringing_now),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Countdown(nextAlarmAtMillis: Long?, nowProvider: () -> Long) {
    val now by produceState(initialValue = nowProvider(), nextAlarmAtMillis) {
        while (true) {
            value = nowProvider()
            delay(TICK_MILLIS - value % TICK_MILLIS)
        }
    }
    val next = nextAlarmAtMillis ?: now
    val remaining = (next - now).coerceAtLeast(0L)
    val countdown = formatCountdown(remaining)
    val context = LocalContext.current
    val resources = LocalResources.current
    val atTime = remember(next) { android.text.format.DateFormat.getTimeFormat(context).format(Date(next)) }
    // Spoken label changes at most once a minute (until the last minute), so TalkBack isn't spammed.
    // Label changes only when the rounded minute changes, so TalkBack isn't spammed.
    val spokenBucket = if (remaining < 60_000L) -1L else (remaining + 30_000L) / 60_000L
    val a11y = remember(spokenBucket, atTime) {
        resources.getString(R.string.next_a11y, spokenDuration(resources, remaining), atTime)
    }
    Column(
        modifier = Modifier
            .testTag(SessionCardTestTags.COUNTDOWN)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.next_overline).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.next_in_prefix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                text = countdown,
                style = MaterialTheme.typography.displaySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Text(
            text = stringResource(R.string.next_at, atTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(SessionCardTestTags.COUNTDOWN_AT),
        )
    }
}

/**
 * Minutes-only spoken countdown (US-06 §7, revised): rounded to the nearest minute, "1 hour
 * 5 minutes" from an hour, "less than a minute" under one minute.
 */
internal fun spokenDuration(resources: android.content.res.Resources, remainingMillis: Long): String {
    if (remainingMillis < 60_000L) return resources.getString(R.string.duration_less_than_minute)
    val totalMinutes = ((remainingMillis + 30_000L) / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> resources.getQuantityString(R.plurals.interval_hours_long, hours, hours) +
            if (minutes > 0) " " + resources.getQuantityString(R.plurals.interval_minutes_long, minutes, minutes) else ""
        else -> resources.getQuantityString(R.plurals.interval_minutes_long, minutes, minutes)
    }
}

@Composable
private fun PausedContent(pausedUntilMillis: Long?) {
    if (pausedUntilMillis != null) {
        val context = LocalContext.current
        val time = remember(pausedUntilMillis) {
            android.text.format.DateFormat.getTimeFormat(context).format(Date(pausedUntilMillis))
        }
        val a11y = stringResource(R.string.paused_until_a11y, time)
        Column(
            modifier = Modifier
                .testTag(SessionCardTestTags.PAUSED_UNTIL)
                .semantics(mergeDescendants = true) { contentDescription = a11y },
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.paused_until_overline).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.paused_auto_resume),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(
            modifier = Modifier.testTag(SessionCardTestTags.PAUSED_UNTIL),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.paused_indef_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.paused_level_kept),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** US-07 §2: the explanation sits next to Resume, in neutral attention colours (no red, no guilt). */
@Composable
private fun AutoPausedBanner() {
    val title = stringResource(R.string.autopause_title)
    val body = stringResource(R.string.autopause_body)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SessionCardTestTags.AUTOPAUSE_BANNER)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$title $body"
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(modifier = Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(
                painter = painterResource(R.drawable.ic_missed),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NotificationsBlockedCaption(visible: Boolean) {
    if (!visible) return
    // US-05 §4: the session keeps running, but say why nothing arrives.
    Text(
        text = stringResource(R.string.countdown_notif_off_caption),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(HomeTestTags.NOTIF_OFF_CAPTION),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PausePicker(onChoose: (PauseChoice) -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.pause_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AssistChip(
                onClick = { onChoose(PauseChoice.FIFTEEN_MINUTES) },
                label = { Text(stringResource(R.string.pause_15), style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.testTag(SessionCardTestTags.PAUSE_15),
            )
            AssistChip(
                onClick = { onChoose(PauseChoice.ONE_HOUR) },
                label = { Text(stringResource(R.string.pause_60), style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.testTag(SessionCardTestTags.PAUSE_60),
            )
            AssistChip(
                onClick = { onChoose(PauseChoice.UNTIL_RESUME) },
                label = { Text(stringResource(R.string.pause_indef), style = MaterialTheme.typography.labelLarge) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.testTag(SessionCardTestTags.PAUSE_INDEF),
            )
        }
        // The 48dp Cancel touch target adds ~12dp of blank space below its label; let it overlap the
        // card's 20dp bottom padding so the picker ends like the other states (US-06 sign-off).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val trim = CANCEL_TRIM.roundToPx().coerceAtMost(placeable.height)
                    layout(placeable.width, placeable.height - trim) { placeable.place(0, 0) }
                },
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .heightIn(min = Dimens.minTouchTarget)
                    .testTag(SessionCardTestTags.PAUSE_CANCEL),
            ) { Text(stringResource(R.string.pause_cancel), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Two equal buttons side by side, or stacked full-width at large font scales (US-06 §7). */
@Composable
private fun ButtonPair(
    stacked: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    val base = Modifier.heightIn(min = Dimens.buttonHeight)
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            first(base.fillMaxWidth())
            second(base.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            first(base.weight(1f))
            second(base.weight(1f))
        }
    }
}

@Composable
private fun StopButton(onStopClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onStopClick,
        modifier = modifier.testTag(HomeTestTags.STOP_BUTTON),
    ) { ButtonContent(R.drawable.ic_stop, R.string.action_stop) }
}

@Composable
private fun ButtonContent(icon: Int, label: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(Dimens.buttonIconSize),
    )
    Spacer(Modifier.size(Spacing.sm))
    Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
}

// region Previews

@Composable
private fun SessionCardPreview(status: SessionStatus, pausedUntil: Long? = null, autoPaused: Boolean = false) {
    SessionCard(
        status = status,
        nextAlarmAtMillis = 1_790_000_754_000L,
        pausedUntilMillis = pausedUntil,
        autoPaused = autoPaused,
        notificationsBlocked = false,
        intervalLabel = "5 min",
        onStartClick = {},
        onStopClick = {},
        onPause = {},
        onResume = {},
        modifier = Modifier.padding(Spacing.lg),
        nowProvider = { 1_790_000_000_000L },
    )
}

@Preview(name = "Session - running - light", showBackground = true)
@Composable
private fun SessionRunningPreview() {
    SuperUnclenchTheme(darkTheme = false) { SessionCardPreview(SessionStatus.RUNNING) }
}

@Preview(name = "Session - paused until - dark", showBackground = true)
@Composable
private fun SessionPausedTimedPreview() {
    SuperUnclenchTheme(darkTheme = true) { SessionCardPreview(SessionStatus.PAUSED, 1_790_003_600_000L) }
}

@Preview(name = "Session - paused indefinitely - light", showBackground = true)
@Composable
private fun SessionPausedIndefPreview() {
    SuperUnclenchTheme(darkTheme = false) { SessionCardPreview(SessionStatus.PAUSED) }
}

@Preview(name = "Session - auto-paused - light", showBackground = true)
@Composable
private fun SessionAutoPausedPreview() {
    SuperUnclenchTheme(darkTheme = false) { SessionCardPreview(SessionStatus.PAUSED, autoPaused = true) }
}

@Preview(name = "Session - auto-paused - dark", showBackground = true)
@Composable
private fun SessionAutoPausedDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) { SessionCardPreview(SessionStatus.PAUSED, autoPaused = true) }
}

@Preview(name = "Session - stopped - 200% font", showBackground = true, fontScale = 2f)
@Composable
private fun SessionStoppedLargeFontPreview() {
    SuperUnclenchTheme(darkTheme = false) { SessionCardPreview(SessionStatus.STOPPED) }
}

// endregion
