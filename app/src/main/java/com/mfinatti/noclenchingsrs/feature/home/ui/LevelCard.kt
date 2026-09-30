package com.mfinatti.noclenchingsrs.feature.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.session.LastAnswer
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.ui.components.SegmentedProgressBar
import com.mfinatti.noclenchingsrs.ui.format.formatInterval
import com.mfinatti.noclenchingsrs.ui.format.formatIntervalLong
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Test tags from docs/design/US-04-srs-progression.md §9. */
object LevelCardTestTags {
    const val CARD = "level_card"
    const val NUMBER = "level_number"
    const val NAME = "level_name"
    const val PROGRESS_BAR = "level_progress_bar"
    const val PROGRESS_CAPTION = "level_progress_caption"
    const val INTERVAL_LINE = "interval_line"
    const val MESSAGE = "level_message"
}

private const val NUMBER_ANIM_MILLIS = 500
private const val BAR_CROSSFADE_MILLIS = 300
private const val MESSAGE_ANIM_MILLIS = 300
private const val MESSAGE_VISIBLE_MILLIS = 4_000L

/** Answers older than this when Home appears don't replay their message (US-04 §4). */
private const val MESSAGE_REPLAY_WINDOW_MILLIS = 10_000L
private const val LARGE_FONT_SCALE = 1.5f

/**
 * Home slot C: the level card (US-04 §2). Shows the level number and name, sub-level progress,
 * the current interval and a transient feedback message after answers.
 *
 * @param nowMillis wall clock used to decide whether a recent answer's message should still show.
 */
@Composable
fun LevelCard(
    level: Int,
    subLevel: Int,
    subLevelCount: Int,
    maxLevel: Int,
    interval: Duration,
    lastAnswer: LastAnswer?,
    modifier: Modifier = Modifier,
    expandedWidth: Boolean = false,
    nowMillis: () -> Long = { System.currentTimeMillis() },
    shortInterval: Duration? = null,
) {
    val resources = LocalResources.current
    val isMax = level >= maxLevel
    val levelNames = stringArrayResource(R.array.level_names)
    val levelName = levelNames.getOrElse(level - 1) { "" }
    val intervalLabel = resources.formatInterval(interval)
    val progressText = if (isMax) {
        stringResource(R.string.level_card_a11y_max)
    } else {
        stringResource(R.string.level_progress, subLevel, subLevelCount, level + 1)
    }
    val cardDescription = stringResource(
        R.string.level_card_a11y,
        level,
        levelName,
        progressText,
        resources.formatIntervalLong(interval),
    )
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

    // Feedback message: only the most recent answer, only if it's fresh (answered while Home is
    // visible, or within the last 10 s before the app was opened), never replayed twice.
    var lastShownAt by rememberSaveable { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<LevelMessage?>(null) }
    var messageVisible by remember { mutableStateOf(false) }
    val answerAt = lastAnswer?.atMillis
    LaunchedEffect(answerAt) {
        val answer = lastAnswer ?: return@LaunchedEffect
        if (answer.atMillis == lastShownAt) return@LaunchedEffect
        lastShownAt = answer.atMillis
        if (nowMillis() - answer.atMillis > MESSAGE_REPLAY_WINDOW_MILLIS) return@LaunchedEffect
        message = LevelMessage.from(answer) ?: return@LaunchedEffect
        messageVisible = true
        delay(MESSAGE_VISIBLE_MILLIS)
        messageVisible = false
    }
    // Pulse the bar on promotion.
    val pulseKey = lastAnswer?.takeIf { it.change == LevelChange.PROMOTED }?.atMillis

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(LevelCardTestTags.CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            // Level summary: one merged node with the spelled-out description.
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = cardDescription },
            ) {
                HeaderRow(levelName = levelName, stacked = largeFont)
                NumberRow(level = level, maxLevel = maxLevel, expandedWidth = expandedWidth, stacked = largeFont)
                Spacer(Modifier.height(Spacing.md))
                AnimatedContent(
                    targetState = BarState(level, subLevelCount, subLevel, isMax),
                    contentKey = { it.level },
                    transitionSpec = {
                        fadeIn(tween(BAR_CROSSFADE_MILLIS)) togetherWith fadeOut(tween(BAR_CROSSFADE_MILLIS))
                    },
                    label = "levelBar",
                ) { bar ->
                    SegmentedProgressBar(
                        segments = bar.segments,
                        filled = bar.filled,
                        maxed = bar.maxed,
                        pulseKey = pulseKey,
                        modifier = Modifier.testTag(LevelCardTestTags.PROGRESS_BAR),
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                ProgressCaption(
                    isMax = isMax,
                    level = level,
                    subLevel = subLevel,
                    subLevelCount = subLevelCount,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.lg),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier.testTag(LevelCardTestTags.INTERVAL_LINE),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(Spacing.sm))
                    Text(
                        text = stringResource(R.string.interval_line, intervalLabel),
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (shortInterval != null) {
                        // Debug short intervals (US-05 §5): "Check-ins every 5 min (short: 5 s)".
                        Spacer(Modifier.size(Spacing.xs))
                        Text(
                            text = stringResource(R.string.interval_short_suffix, shortInterval.inWholeSeconds.toInt()),
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = messageVisible,
                enter = expandVertically(tween(MESSAGE_ANIM_MILLIS)) + fadeIn(tween(MESSAGE_ANIM_MILLIS)),
                exit = shrinkVertically(tween(MESSAGE_ANIM_MILLIS)) + fadeOut(tween(MESSAGE_ANIM_MILLIS)),
            ) {
                // `message` stays set while the row collapses.
                message?.let { current ->
                    LevelMessageRow(
                        message = current,
                        level = level,
                        intervalLabel = intervalLabel,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }
            }
        }
    }
}

private data class BarState(val level: Int, val segments: Int, val filled: Int, val maxed: Boolean)

@Composable
private fun HeaderRow(levelName: String, stacked: Boolean) {
    val overline: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.level_overline).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val name: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = levelName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.testTag(LevelCardTestTags.NAME),
        )
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            overline()
            name(Modifier)
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { overline() }
            name(Modifier)
        }
    }
}

@Composable
private fun NumberRow(level: Int, maxLevel: Int, expandedWidth: Boolean, stacked: Boolean) {
    val numberStyle = (if (expandedWidth) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium)
        .tabular()
    val number: @Composable (Modifier) -> Unit = { modifier ->
        AnimatedContent(
            targetState = level,
            transitionSpec = {
                val up = targetState > initialState
                val enter = slideInVertically(tween(NUMBER_ANIM_MILLIS)) { height -> if (up) height else -height } +
                    fadeIn(tween(NUMBER_ANIM_MILLIS))
                val exit = slideOutVertically(tween(NUMBER_ANIM_MILLIS)) { height -> if (up) -height else height } +
                    fadeOut(tween(NUMBER_ANIM_MILLIS))
                enter togetherWith exit
            },
            modifier = modifier.testTag(LevelCardTestTags.NUMBER),
            label = "levelNumber",
        ) { value ->
            Text(
                text = value.toString(),
                style = numberStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    val ofTotal: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = stringResource(R.string.level_of_total, maxLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
    if (stacked) {
        Column {
            number(Modifier)
            ofTotal(Modifier)
        }
    } else {
        // "of 8" sits right after the number, on its baseline (US-04 design follow-up 2).
        Row(modifier = Modifier.fillMaxWidth()) {
            number(Modifier.alignByBaseline())
            Spacer(Modifier.width(Spacing.sm))
            ofTotal(Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun ProgressCaption(isMax: Boolean, level: Int, subLevel: Int, subLevelCount: Int) {
    Column(modifier = Modifier.testTag(LevelCardTestTags.PROGRESS_CAPTION)) {
        if (isMax) {
            Text(
                text = stringResource(R.string.level_max, level),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.level_max_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.level_progress, subLevel, subLevelCount, level + 1),
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelMessageRow(
    message: LevelMessage,
    level: Int,
    intervalLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extended = SuperUnclenchTheme.extendedColors
    val container: Color
    val content: Color
    val icon: Int
    when (message) {
        LevelMessage.LEVEL_UP -> {
            container = colors.tertiaryContainer
            content = colors.onTertiaryContainer
            icon = R.drawable.ic_level_up
        }
        LevelMessage.MAX_GOOD -> {
            container = extended.goodContainer
            content = extended.onGoodContainer
            icon = R.drawable.ic_good
        }
        LevelMessage.PROGRESS_RESET -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            icon = R.drawable.ic_restart
        }
        LevelMessage.LEVEL_DOWN -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            icon = R.drawable.ic_level_down
        }
        LevelMessage.FLOOR -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            icon = R.drawable.ic_info
        }
    }
    val titleWeight = MaterialTheme.typography.titleSmall.fontWeight
    val text = when (message) {
        LevelMessage.LEVEL_UP -> {
            val title = stringResource(R.string.msg_level_up_title)
            val body = stringResource(R.string.msg_level_up_body, intervalLabel)
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = titleWeight)) { append(title) }
                append(" ")
                append(body)
            }
        }
        LevelMessage.MAX_GOOD -> buildAnnotatedString { append(stringResource(R.string.msg_max_good)) }
        LevelMessage.PROGRESS_RESET -> buildAnnotatedString { append(stringResource(R.string.msg_reset, level)) }
        LevelMessage.LEVEL_DOWN ->
            buildAnnotatedString { append(stringResource(R.string.msg_demote, level, intervalLabel)) }
        LevelMessage.FLOOR -> buildAnnotatedString { append(stringResource(R.string.msg_floor, intervalLabel)) }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(LevelCardTestTags.MESSAGE)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(Spacing.md))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// region Previews

@Composable
private fun LevelCardPreviewContent(
    level: Int,
    subLevel: Int,
    subLevelCount: Int,
    interval: Duration,
    change: LevelChange? = null,
    answer: Answer = Answer.GOOD,
) {
    LevelCard(
        level = level,
        subLevel = subLevel,
        subLevelCount = subLevelCount,
        maxLevel = 8,
        interval = interval,
        lastAnswer = change?.let { LastAnswer(atMillis = 1L, answer = answer, change = it) },
        modifier = Modifier.padding(Spacing.lg),
        nowMillis = { 1L },
    )
}

@Preview(name = "Level card - L1 1/3 - light", showBackground = true)
@Composable
private fun LevelCardL1Preview() {
    SuperUnclenchTheme(darkTheme = false) { LevelCardPreviewContent(1, 1, 3, 5.minutes) }
}

@Preview(name = "Level card - L3 2/4 - dark", showBackground = true)
@Composable
private fun LevelCardL3DarkPreview() {
    SuperUnclenchTheme(darkTheme = true) { LevelCardPreviewContent(3, 2, 4, 15.minutes) }
}

@Preview(name = "Level card - L8 max - light", showBackground = true)
@Composable
private fun LevelCardMaxPreview() {
    SuperUnclenchTheme(darkTheme = false) { LevelCardPreviewContent(8, 0, 5, 3.hours) }
}

@Preview(name = "Level card - 200% font", showBackground = true, fontScale = 2f)
@Composable
private fun LevelCardLargeFontPreview() {
    SuperUnclenchTheme(darkTheme = false) { LevelCardPreviewContent(3, 2, 4, 15.minutes) }
}

// endregion
