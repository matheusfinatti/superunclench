package com.mfinatti.noclenchingsrs.feature.stats.ui

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.di.LocalAppContainer
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInEvent
import com.mfinatti.noclenchingsrs.feature.stats.domain.FrameStats
import com.mfinatti.noclenchingsrs.feature.stats.domain.SampleHistory
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsCalculator
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsFrame
import com.mfinatti.noclenchingsrs.ui.components.HistoryRow
import com.mfinatti.noclenchingsrs.ui.components.StatTile
import com.mfinatti.noclenchingsrs.ui.components.StatTileKind
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Test tags from docs/design/US-08-stats.md §13. */
object StatsTestTags {
    const val FRAME_TODAY = "stats_frame_today"
    const val FRAME_7D = "stats_frame_7d"
    const val FRAME_30D = "stats_frame_30d"
    const val TILE_GOOD = "tile_good"
    const val TILE_BAD = "tile_bad"
    const val TILE_MISSED = "tile_missed"
    const val TILE_RATE = "tile_rate"
    const val TILE_STREAK = "tile_streak"
    const val EMPTY = "stats_empty"
    const val EMPTY_GO_HOME = "stats_empty_go_home"
    const val HISTORY_LIST = "history_list"

    fun historyRow(index: Int) = "history_row_$index"
}

private const val HISTORY_SIZE = 20
private const val CLOCK_REFRESH_MILLIS = 60_000L
private const val SINGLE_COLUMN_FONT_SCALE = 1.5f

@Composable
fun StatsRoute(onGoHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: StatsViewModel = viewModel { StatsViewModel(container.checkInLogRepository) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    // Re-evaluate "today" at least once a minute (day rollover while open).
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_REFRESH_MILLIS)
            value = System.currentTimeMillis()
        }
    }
    StatsScreen(events = events, nowMillis = now, timeZone = TimeZone.getDefault(), onGoHome = onGoHome)
}

/**
 * Stats (US-08): time-frame selector, summary tiles, streak, stacked Good/Bad chart and the last
 * 20 check-ins.
 *
 * @param events full history (oldest first), or null while loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    events: List<CheckInEvent>?,
    nowMillis: Long,
    timeZone: TimeZone,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Always opens on Today (not persisted across app restarts, for deterministic QA).
    var frame by rememberSaveable { mutableStateOf(StatsFrame.TODAY) }
    var selected by rememberSaveable(frame) { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.stats_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val gutter = if (maxWidth >= 600.dp) Dimens.gutterWide else Dimens.gutterCompact
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = Dimens.maxContentWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = gutter,
                    end = gutter,
                    top = Spacing.sm,
                    bottom = Dimens.bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                when {
                    events == null -> Unit
                    // No history at all: just the empty state, no frame selector (US-08 sign-off).
                    events.isEmpty() -> item(key = "empty") { EmptyState(onGoHome = onGoHome) }
                    else -> {
                        item(key = "frames") {
                            FrameSelector(frame = frame, onFrame = { frame = it })
                        }
                        val stats = StatsCalculator.compute(events, frame, nowMillis, timeZone)
                        val streak = StatsCalculator.streak(events, nowMillis, timeZone)
                        item(key = "tiles") { SummaryTiles(stats) }
                        item(key = "streak") { StreakTile(streak) }
                        item(key = "chart") {
                            ChartSection(
                                stats = stats,
                                nowMillis = nowMillis,
                                timeZone = timeZone,
                                selected = selected,
                                onSelect = { selected = it },
                            )
                        }
                        item(key = "history") {
                            RecentHistory(events = events, nowMillis = nowMillis, timeZone = timeZone)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameSelector(frame: StatsFrame, onFrame: (StatsFrame) -> Unit) {
    val options = listOf(
        Triple(StatsFrame.TODAY, R.string.frame_today, StatsTestTags.FRAME_TODAY),
        Triple(StatsFrame.WEEK, R.string.frame_7d, StatsTestTags.FRAME_7D),
        Triple(StatsFrame.MONTH, R.string.frame_30d, StatsTestTags.FRAME_30D),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label, tag) ->
            SegmentedButton(
                selected = frame == value,
                onClick = { onFrame(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier
                    .heightIn(min = Dimens.minTouchTarget)
                    .testTag(tag),
                label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
private fun SummaryTiles(stats: FrameStats) {
    val singleColumn = LocalDensity.current.fontScale >= SINGLE_COLUMN_FONT_SCALE
    val rate = stats.goodRatePercent?.let { "$it%" } ?: stringResource(R.string.tile_rate_empty)
    val rateA11y = stats.goodRatePercent?.let { stringResource(R.string.tile_rate_a11y, it) }
        ?: stringResource(R.string.tile_rate_a11y_empty)
    val tiles: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            StatTile(stringResource(R.string.tile_good), stats.good.toString(), R.drawable.ic_good, StatTileKind.GOOD,
                m.testTag(StatsTestTags.TILE_GOOD))
        },
        { m ->
            StatTile(stringResource(R.string.tile_bad), stats.bad.toString(), R.drawable.ic_bad, StatTileKind.BAD,
                m.testTag(StatsTestTags.TILE_BAD))
        },
        { m ->
            StatTile(stringResource(R.string.tile_missed), stats.missed.toString(), R.drawable.ic_missed,
                StatTileKind.MISSED, m.testTag(StatsTestTags.TILE_MISSED))
        },
        { m ->
            StatTile(stringResource(R.string.tile_rate), rate, R.drawable.ic_rate, StatTileKind.NEUTRAL,
                m.testTag(StatsTestTags.TILE_RATE), a11yLabel = rateA11y)
        },
    )
    if (singleColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tiles.forEach { it(Modifier.fillMaxWidth()) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    pair.forEach { it(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StreakTile(streak: Int) {
    val value = LocalResources.current.getQuantityString(R.plurals.streak_value, streak, streak)
    val label = stringResource(R.string.tile_streak)
    val support = stringResource(R.string.streak_support)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StatsTestTags.TILE_STREAK)
            .clearAndSetSemantics { contentDescription = "$label: $value. $support" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_streak),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(Spacing.md))
                Text(value, style = MaterialTheme.typography.headlineSmall.tabular())
            }
            Text(support, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartSection(
    stats: FrameStats,
    nowMillis: Long,
    timeZone: TimeZone,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val todayLabel = stringResource(R.string.chart_today_label)
    val labels = remember(stats, todayLabel) { chartLabels(stats, context, timeZone, todayLabel) }
    val summary = remember(stats, labels) { chartSummary(stats, labels, resources) }
    StatsChartCard(
        stats = stats,
        labels = labels,
        selected = selected?.takeIf { it in stats.buckets.indices },
        onSelect = onSelect,
        summaryDescription = summary,
    )
}

/** Locale-aware axis + bucket labels (US-08 §4.4, §4.6). */
private fun chartLabels(
    stats: FrameStats,
    context: android.content.Context,
    timeZone: TimeZone,
    todayLabel: String,
): ChartLabels {
    val locale = Locale.getDefault()
    val is24h = DateFormat.is24HourFormat(context)
    val hourFormat = SimpleDateFormat(
        DateFormat.getBestDateTimePattern(locale, if (is24h) "Hm" else "ha"),
        locale,
    ).apply { this.timeZone = timeZone }
    fun hourLabel(millis: Long): String = hourFormat.format(Date(millis))
    fun fmt(pattern: String, millis: Long) =
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, pattern), locale)
            .apply { this.timeZone = timeZone }
            .format(Date(millis))
    val buckets = stats.buckets
    return when (stats.frame) {
        StatsFrame.TODAY -> ChartLabels(
            axis = buckets.indices.map { hour ->
                if (hour % 6 != 0) {
                    null
                } else if (is24h) {
                    String.format(locale, "%02d", hour)
                } else {
                    val h12 = if (hour % 12 == 0) 12 else hour % 12
                    "$h12${if (hour < 12) "a" else "p"}"
                }
            },
            boldAxisIndex = null,
            // Compact one-line range: "11 AM–12 PM" / "11:00–12:00" (US-08 sign-off).
            bucket = buckets.map { "${hourLabel(it.startMillis)}–${hourLabel(it.endMillis)}" },
        )
        StatsFrame.WEEK -> ChartLabels(
            axis = buckets.mapIndexed { i, b -> if (i == buckets.lastIndex) todayLabel else fmt("EEE", b.startMillis) },
            boldAxisIndex = buckets.lastIndex,
            bucket = buckets.map { fmt("EEEMMMd", it.startMillis) },
        )
        StatsFrame.MONTH -> ChartLabels(
            axis = buckets.mapIndexed { i, b ->
                when {
                    i == buckets.lastIndex -> todayLabel
                    i % 7 == 0 && i < buckets.lastIndex - 3 -> fmt("MMMd", b.startMillis)
                    else -> null
                }
            },
            boldAxisIndex = buckets.lastIndex,
            bucket = buckets.map { fmt("EEEMMMd", it.startMillis) },
        )
    }
}

private fun chartSummary(stats: FrameStats, labels: ChartLabels, resources: Resources): String {
    val title = resources.getString(if (stats.frame.hourly) R.string.chart_title_hourly else R.string.chart_title_daily)
    val frame = resources.getString(
        when (stats.frame) {
            StatsFrame.TODAY -> R.string.frame_today
            StatsFrame.WEEK -> R.string.chart_frame_7d_a11y
            StatsFrame.MONTH -> R.string.chart_frame_30d_a11y
        },
    )
    // "Busiest": most answers; ties go to the most recent bucket.
    val highest = stats.buckets.withIndex().lastOrNull { indexed ->
        indexed.value.answered > 0 && indexed.value.answered == stats.buckets.maxOf { it.answered }
    }
    val base = resources.getString(R.string.chart_summary_a11y, title, frame, stats.good, stats.bad)
    return if (highest == null) {
        base
    } else {
        base + " " + resources.getString(
            R.string.chart_summary_highest_a11y,
            labels.bucket[highest.index],
            highest.value.good,
            highest.value.bad,
        )
    }
}

@Composable
private fun RecentHistory(events: List<CheckInEvent>, nowMillis: Long, timeZone: TimeZone) {
    val context = LocalContext.current
    val levelNames = stringArrayResource(R.array.level_names)
    val yesterdayFormat = stringResource(R.string.history_yesterday)
    val recent = remember(events) { events.takeLast(HISTORY_SIZE).asReversed() }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StatsTestTags.HISTORY_LIST),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            recent.forEachIndexed { index, event ->
                HistoryRow(
                    outcome = event.outcome,
                    levelLine = stringResource(
                        R.string.history_level,
                        event.level,
                        levelNames.getOrElse(event.level - 1) { "" },
                    ),
                    time = historyTime(event.atMillis, nowMillis, timeZone, context, yesterdayFormat),
                    modifier = Modifier.testTag(StatsTestTags.historyRow(index)),
                )
            }
        }
    }
}

/** "17:35" today, "Yesterday 17:35", "Sep 27, 17:35" older (US-08 §7). */
private fun historyTime(
    atMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone,
    context: android.content.Context,
    yesterdayFormat: String,
): String {
    val time = DateFormat.getTimeFormat(context).apply { this.timeZone = timeZone }.format(Date(atMillis))
    val todayStart = StatsCalculator.startOfDay(nowMillis, timeZone)
    val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
    return when {
        atMillis >= todayStart.timeInMillis -> time
        atMillis >= yesterdayStart.timeInMillis -> String.format(yesterdayFormat, time)
        else -> {
            val locale = Locale.getDefault()
            val date = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
                .apply { this.timeZone = timeZone }
                .format(Date(atMillis))
            "$date, $time"
        }
    }
}

@Composable
private fun EmptyState(onGoHome: () -> Unit) {
    val a11y = stringResource(R.string.empty_a11y)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xxxl)
            .testTag(StatsTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = a11y },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stats),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.stats_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        FilledTonalButton(
            onClick = onGoHome,
            modifier = Modifier
                .heightIn(min = Dimens.buttonHeight)
                .testTag(StatsTestTags.EMPTY_GO_HOME),
        ) { Text(stringResource(R.string.empty_action), style = MaterialTheme.typography.labelLarge) }
    }
}

// region Previews

private val PreviewTz: TimeZone = TimeZone.getTimeZone("UTC")
private const val PREVIEW_NOW = 1_790_798_400_000L // 2026-09-30 18:00 UTC
private val PreviewEvents by lazy { SampleHistory.generate(PREVIEW_NOW, PreviewTz) }

@Preview(name = "Stats - seeded - light", showBackground = true, heightDp = 1400)
@Composable
private fun StatsSeededLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        StatsScreen(events = PreviewEvents, nowMillis = PREVIEW_NOW, timeZone = PreviewTz, onGoHome = {})
    }
}

@Preview(name = "Stats - seeded - dark", showBackground = true, heightDp = 1400)
@Composable
private fun StatsSeededDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        StatsScreen(events = PreviewEvents, nowMillis = PREVIEW_NOW, timeZone = PreviewTz, onGoHome = {})
    }
}

@Preview(name = "Stats - empty - light", showBackground = true)
@Composable
private fun StatsEmptyPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        StatsScreen(events = emptyList(), nowMillis = PREVIEW_NOW, timeZone = PreviewTz, onGoHome = {})
    }
}

// endregion
