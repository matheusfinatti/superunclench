package com.mfinatti.noclenchingsrs.feature.stats.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.feature.stats.domain.FrameStats
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsFrame
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular

/** Test tags from docs/design/US-08-stats.md §13. */
object ChartTestTags {
    const val CHART = "chart"
    const val READOUT = "chart_readout"
    const val PREV = "chart_prev"
    const val NEXT = "chart_next"

    fun bar(index: Int) = "chart_bar_$index"
}

private val PLOT_HEIGHT = 176.dp
private val PLOT_TOP_PADDING = 8.dp
private val X_LABEL_HEIGHT = 24.dp
private val Y_COLUMN = 28.dp
private val Y_GAP = 4.dp
private const val BAR_GROW_MILLIS = 500
private const val STAGGER_MILLIS = 10
private const val MAX_STAGGER_MILLIS = 300
private const val DIMMED_ALPHA = 0.4f

/**
 * Labels for the chart, prepared by the screen (locale-aware formatting stays out of the canvas).
 *
 * @property axis x-axis label per bucket (null = no label).
 * @property boldAxisIndex bucket whose label is "Today" (bold, onSurface).
 * @property bucket long label per bucket for the tooltip / readout ("14:00–15:00", "Tue, Sep 29").
 */
class ChartLabels(
    val axis: List<String?>,
    val boldAxisIndex: Int?,
    val bucket: List<String>,
)

/** US-08 §4: the stacked Good/Bad bar chart card with legend, selection, tooltip and readout. */
@Composable
fun StatsChartCard(
    stats: FrameStats,
    labels: ChartLabels,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    summaryDescription: String,
    modifier: Modifier = Modifier,
) {
    val isEmpty = stats.buckets.all { it.good == 0 && it.bad == 0 && it.missed == 0 }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(if (stats.frame.hourly) R.string.chart_title_hourly else R.string.chart_title_daily),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.sm))
            Legend()
            Spacer(Modifier.height(Spacing.lg))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PLOT_TOP_PADDING + PLOT_HEIGHT + X_LABEL_HEIGHT)
                    .testTag(ChartTestTags.CHART)
                    .semantics { contentDescription = summaryDescription },
            ) {
                BarCanvas(stats = stats, labels = labels, selected = selected, isEmpty = isEmpty)
                if (isEmpty) {
                    Text(
                        text = stringResource(R.string.chart_empty_period),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(start = Y_COLUMN + Y_GAP),
                    )
                } else {
                    SlotOverlay(stats = stats, labels = labels, selected = selected, onSelect = onSelect)
                }
            }
            if (!isEmpty) {
                Readout(stats = stats, labels = labels, selected = selected, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun Legend() {
    val extended = SuperUnclenchTheme.extendedColors
    val hatch = extended.onBad.copy(alpha = 0.35f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendSwatch(fill = extended.good, hatch = null)
        Spacer(Modifier.size(Spacing.sm))
        Text(
            stringResource(R.string.legend_good),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Spacing.lg))
        LegendSwatch(fill = extended.bad, hatch = hatch)
        Spacer(Modifier.size(Spacing.sm))
        Text(
            stringResource(R.string.legend_bad),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendSwatch(fill: Color, hatch: Color?) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val radius = CornerRadius(2.dp.toPx())
        drawRoundRect(color = fill, cornerRadius = radius)
        if (hatch != null) {
            val path = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), radius)) }
            drawHatch(path, Rect(Offset.Zero, size), hatch, spacing = 3.dp.toPx(), stroke = 1.dp.toPx())
        }
    }
}

@Composable
private fun BarCanvas(stats: FrameStats, labels: ChartLabels, selected: Int?, isEmpty: Boolean) {
    val colors = MaterialTheme.colorScheme
    val extended = SuperUnclenchTheme.extendedColors
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.tabular().copy(color = colors.onSurfaceVariant)
    val axisBoldStyle = axisStyle.copy(color = colors.onSurface, fontWeight = FontWeight.Bold)
    val tooltipStyle = MaterialTheme.typography.bodySmall.tabular().copy(color = colors.inverseOnSurface)
    val tooltipCounts = selected?.let { index ->
        val b = stats.buckets[index]
        stringResource(R.string.tooltip_counts, b.good, b.bad)
    }

    // Bars grow from the baseline on first show and whenever the data/frame changes (§4.8).
    val n = stats.buckets.size
    val totalMillis = BAR_GROW_MILLIS + minOf(n * STAGGER_MILLIS, MAX_STAGGER_MILLIS)
    val progress = remember(stats) { Animatable(0f) }
    LaunchedEffect(stats) {
        progress.animateTo(1f, tween(durationMillis = totalMillis, easing = LinearEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val plotLeft = (Y_COLUMN + Y_GAP).toPx()
        val plotTop = PLOT_TOP_PADDING.toPx()
        val plotHeight = PLOT_HEIGHT.toPx()
        val baseline = plotTop + plotHeight
        val plotWidth = size.width - plotLeft
        val slot = plotWidth / n
        val yMax = stats.yMax.toFloat()

        // Gridlines (dashed) at yMax/2 and yMax; solid baseline at 0 (§4.2).
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        listOf(stats.yMax, stats.yMax / 2, 0).forEach { value ->
            val y = baseline - plotHeight * value / yMax
            if (value == 0) {
                drawLine(colors.outline, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            } else {
                drawLine(
                    colors.outlineVariant,
                    Offset(plotLeft, y),
                    Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }
            val layout = textMeasurer.measure(value.toString(), axisStyle)
            drawText(
                layout,
                topLeft = Offset(Y_COLUMN.toPx() - layout.size.width, y - layout.size.height / 2f),
            )
        }

        if (!isEmpty) {
            val barWidth = when (stats.frame) {
                StatsFrame.TODAY -> slot * 0.6f
                StatsFrame.WEEK -> minOf(slot * 0.5f, 32.dp.toPx())
                StatsFrame.MONTH -> slot * 0.7f
            }.coerceAtLeast(3.dp.toPx())
            val radius = if (barWidth < 8.dp.toPx()) 2.dp.toPx() else 4.dp.toPx()
            val minSegment = 2.dp.toPx()
            val gap = 1.dp.toPx()
            val elapsed = progress.value * totalMillis
            stats.buckets.forEachIndexed { index, bucket ->
                if (bucket.answered == 0) return@forEachIndexed
                val delay = minOf(index * STAGGER_MILLIS, MAX_STAGGER_MILLIS)
                val linear = ((elapsed - delay) / BAR_GROW_MILLIS).coerceIn(0f, 1f)
                val grow = FastOutSlowInEasing.transform(linear)
                val alpha = if (selected == null || selected == index) 1f else DIMMED_ALPHA
                val left = plotLeft + slot * index + (slot - barWidth) / 2f
                fun segment(count: Int) =
                    if (count == 0) 0f else maxOf(plotHeight * count / yMax, minSegment) * grow
                val goodH = segment(bucket.good)
                val badH = segment(bucket.bad)
                if (goodH > 0f) {
                    val rect = Rect(left, baseline - goodH, left + barWidth, baseline)
                    val path = barPath(rect, if (bucket.bad == 0) radius else 0f)
                    drawPath(path, extended.good.copy(alpha = alpha))
                }
                if (badH > 0f) {
                    val bottom = baseline - goodH - if (goodH > 0f) gap else 0f
                    val rect = Rect(left, bottom - badH, left + barWidth, bottom)
                    val path = barPath(rect, radius)
                    drawPath(path, extended.bad.copy(alpha = alpha))
                    // Hatch so Bad differs from Good without colour (§4.3).
                    drawHatch(
                        path,
                        rect,
                        extended.onBad.copy(alpha = 0.35f * alpha),
                        spacing = 5.dp.toPx(),
                        stroke = 1.5.dp.toPx(),
                    )
                }
            }
        }

        // X labels, centred under their slot (§4.4).
        labels.axis.forEachIndexed { index, text ->
            if (text == null) return@forEachIndexed
            val style = if (index == labels.boldAxisIndex) axisBoldStyle else axisStyle
            val layout = textMeasurer.measure(text, style)
            val centre = plotLeft + slot * index + slot / 2f
            val x = (centre - layout.size.width / 2f).coerceIn(plotLeft, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(x, baseline + 6.dp.toPx()))
        }

        // Selection: underline + tooltip (§4.6).
        if (selected != null && tooltipCounts != null) {
            val centre = plotLeft + slot * selected + slot / 2f
            val underline = 12.dp.toPx()
            drawLine(
                colors.onSurface,
                Offset(centre - underline / 2f, size.height - 2.dp.toPx()),
                Offset(centre + underline / 2f, size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val bucket = stats.buckets[selected]
            val barTop = if (bucket.answered == 0) {
                baseline
            } else {
                baseline - maxOf(plotHeight * bucket.answered / yMax, 2.dp.toPx())
            }
            drawTooltip(
                lines = listOf(labels.bucket[selected], tooltipCounts),
                anchorX = centre,
                anchorY = barTop - 8.dp.toPx(),
                minX = plotLeft,
                maxX = size.width,
                style = tooltipStyle,
                background = colors.inverseSurface,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private fun barPath(rect: Rect, topRadius: Float): Path = Path().apply {
    val r = CornerRadius(minOf(topRadius, rect.width / 2f, rect.height))
    addRoundRect(
        RoundRect(
            rect = rect,
            topLeft = r,
            topRight = r,
            bottomRight = CornerRadius.Zero,
            bottomLeft = CornerRadius.Zero,
        ),
    )
}

private fun DrawScope.drawHatch(clip: Path, bounds: Rect, color: Color, spacing: Float, stroke: Float) {
    clipPath(clip) {
        var x = bounds.left - bounds.height
        while (x < bounds.right) {
            drawLine(
                color = color,
                start = Offset(x, bounds.bottom),
                end = Offset(x + bounds.height, bounds.top),
                strokeWidth = stroke,
            )
            x += spacing
        }
    }
}

private fun DrawScope.drawTooltip(
    lines: List<String>,
    anchorX: Float,
    anchorY: Float,
    minX: Float,
    maxX: Float,
    style: TextStyle,
    background: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val layouts = lines.map { textMeasurer.measure(it, style) }
    val padX = 8.dp.toPx()
    val padY = 6.dp.toPx()
    val width = layouts.maxOf { it.size.width } + padX * 2
    val height = layouts.sumOf { it.size.height } + padY * 2
    val left = (anchorX - width / 2f).coerceIn(minX, maxOf(minX, maxX - width))
    val top = (anchorY - height).coerceAtLeast(0f)
    // Soft 2dp shadow, then the bubble.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(left, top + 2.dp.toPx()),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    drawRoundRect(
        color = background,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    var y = top + padY
    layouts.forEach { layout ->
        drawText(layout, topLeft = Offset(left + padX, y))
        y += layout.size.height
    }
}

/** Invisible, slot-sized tap targets with per-bucket semantics (§12), layered over the canvas. */
@Composable
private fun SlotOverlay(stats: FrameStats, labels: ChartLabels, selected: Int?, onSelect: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Y_COLUMN + Y_GAP),
    ) {
        stats.buckets.forEachIndexed { index, bucket ->
            val description = stringResource(
                R.string.chart_bar_a11y,
                labels.bucket[index],
                bucket.good,
                bucket.bad,
                bucket.missed,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(ChartTestTags.bar(index))
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                        this.selected = selected == index
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(if (selected == index) null else index) },
            )
        }
    }
}

/** US-08 §4.7: "(<) Tue, Sep 29 · 8 Good · 2 Bad · 0 Missed (>)" — never overlapped by the tooltip. */
@Composable
private fun Readout(stats: FrameStats, labels: ChartLabels, selected: Int?, onSelect: (Int?) -> Unit) {
    val last = stats.buckets.lastIndex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onSelect(if (selected == null) last else selected - 1) },
            enabled = selected == null || selected > 0,
            modifier = Modifier.testTag(ChartTestTags.PREV),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.readout_prev_cd),
                modifier = Modifier.size(24.dp),
            )
        }
        val text = if (selected == null) {
            stringResource(R.string.readout_hint)
        } else {
            val b = stats.buckets[selected]
            stringResource(R.string.readout_value, labels.bucket[selected], b.good, b.bad, b.missed)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .weight(1f)
                .testTag(ChartTestTags.READOUT),
        )
        IconButton(
            onClick = { onSelect(if (selected == null) 0 else selected + 1) },
            enabled = selected == null || selected < last,
            modifier = Modifier.testTag(ChartTestTags.NEXT),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.readout_next_cd),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
