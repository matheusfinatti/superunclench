package com.mfinatti.noclenchingsrs.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

private const val FILL_MILLIS = 300
private const val DRAIN_STAGGER_MILLIS = 80
private const val PULSE_SCALE = 1.04f

/**
 * design-system §9.1 `SegmentedProgressBar`: [segments] equal pills, [filled] of them in `primary`
 * (all `tertiary` when [maxed]). Filling animates left→right; draining empties right→left with a
 * stagger. Changing [pulseKey] (e.g. on promotion) plays a short celebratory pulse.
 */
@Composable
fun SegmentedProgressBar(
    segments: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    maxed: Boolean = false,
    contentDescription: String? = null,
    pulseKey: Any? = null,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != null) {
            val celebrate = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
            scale.animateTo(PULSE_SCALE, celebrate)
            scale.animateTo(1f, celebrate)
        }
    }
    val filledCount = if (maxed) segments else filled.coerceIn(0, segments)
    val fillColor = if (maxed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    // outlineVariant: visible on the surfaceContainer card (design-system §9.1, changed after US-04).
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = filledCount.toFloat(),
                    range = 0f..segments.toFloat(),
                    steps = (segments - 1).coerceAtLeast(0),
                )
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(segments) { index ->
            val target = if (index < filledCount) 1f else 0f
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = FILL_MILLIS,
                    // Drain right→left: the rightmost segment empties first.
                    delayMillis = if (target == 0f) (segments - 1 - index) * DRAIN_STAGGER_MILLIS else 0,
                ),
                label = "segment$index",
            )
            Segment(fraction = fraction, fillColor = fillColor, emptyColor = emptyColor, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Segment(fraction: Float, fillColor: Color, emptyColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(emptyColor),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(fillColor),
            )
        }
    }
}

@Preview(name = "Segmented progress - light", showBackground = true)
@Composable
private fun SegmentedProgressBarPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SegmentedProgressBar(segments = 3, filled = 1)
                SegmentedProgressBar(segments = 4, filled = 2)
                SegmentedProgressBar(segments = 5, filled = 0, maxed = true)
            }
        }
    }
}

@Preview(name = "Segmented progress - dark", showBackground = true)
@Composable
private fun SegmentedProgressBarDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SegmentedProgressBar(segments = 3, filled = 1)
                SegmentedProgressBar(segments = 5, filled = 0, maxed = true)
            }
        }
    }
}
