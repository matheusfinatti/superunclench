package com.mfinatti.noclenchingsrs.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular

/** Colour roles of a [StatTile] (design-system §9.5). */
enum class StatTileKind { GOOD, BAD, MISSED, NEUTRAL }

@Immutable
private data class TileColors(val container: Color, val content: Color, val icon: Color)

@Composable
private fun tileColors(kind: StatTileKind): TileColors {
    val colors = MaterialTheme.colorScheme
    val extended = SuperUnclenchTheme.extendedColors
    return when (kind) {
        StatTileKind.GOOD -> TileColors(extended.goodContainer, extended.onGoodContainer, extended.onGoodContainer)
        StatTileKind.BAD -> TileColors(extended.badContainer, extended.onBadContainer, extended.onBadContainer)
        StatTileKind.MISSED ->
            TileColors(extended.missedContainer, extended.onMissedContainer, extended.onMissedContainer)
        StatTileKind.NEUTRAL -> TileColors(colors.surfaceContainerHigh, colors.onSurface, colors.primary)
    }
}

/**
 * design-system §9.5 `StatTile`: icon + uppercase label on top, tabular value below; one semantics
 * node ("Good: 12"). [compact] is the Home "Today" variant (72dp, 18dp icon).
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    kind: StatTileKind,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    a11yLabel: String = "$label: $value",
) {
    val tile = tileColors(kind)
    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 72.dp else 88.dp)
            .clearAndSetSemantics { contentDescription = a11yLabel },
        shape = MaterialTheme.shapes.medium,
        color = tile.container,
        contentColor = tile.content,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) Spacing.md else Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = tile.icon,
                    modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = value, style = MaterialTheme.typography.headlineSmall.tabular())
        }
    }
}

@Preview(name = "Stat tiles - light", showBackground = true)
@Composable
private fun StatTilePreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatTile("Good", "5", R.drawable.ic_good, StatTileKind.GOOD, Modifier.fillMaxWidth())
                StatTile("Bad", "2", R.drawable.ic_bad, StatTileKind.BAD, Modifier.fillMaxWidth())
                StatTile("Missed", "1", R.drawable.ic_missed, StatTileKind.MISSED, Modifier.fillMaxWidth())
                StatTile("Good rate", "71%", R.drawable.ic_rate, StatTileKind.NEUTRAL, Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(name = "Stat tiles - dark", showBackground = true)
@Composable
private fun StatTileDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatTile("Good", "5", R.drawable.ic_good, StatTileKind.GOOD, Modifier.fillMaxWidth())
                StatTile("Missed", "1", R.drawable.ic_missed, StatTileKind.MISSED, Modifier.fillMaxWidth(), compact = true)
            }
        }
    }
}
