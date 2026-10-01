package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.feature.checkin.ring.ExactTimingSettingsBanner
import com.mfinatti.noclenchingsrs.feature.checkin.ring.FullScreenDeniedBanner
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

object AlertStyleTestTags {
    const val NUDGE = "settings_alert_nudge"
    const val RING = "settings_alert_ring"
    const val RING_NOTE = "alert_ring_note"
}

/**
 * US-11 §2: Alert style as two selectable tiles (Nudge / Ring) at the top of the Check-ins card.
 * With Ring selected: the alarm-volume note, and the full-screen / exact-timing banners when needed.
 */
@Composable
fun AlertStyleSettings(
    style: AlertStyle,
    fullScreenDenied: Boolean,
    exactDenied: Boolean,
    onStyleChange: (AlertStyle) -> Unit,
    onAllowFullScreen: () -> Unit,
    onAllowExact: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_alert_style), style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(stringResource(R.string.settings_alert_style_support)) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.heightIn(min = 48.dp),
    )
    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.lg)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AlertTile(
            selected = style == AlertStyle.NUDGE,
            icon = R.drawable.ic_notifications,
            title = stringResource(R.string.alert_nudge),
            description = stringResource(R.string.alert_nudge_desc),
            tag = AlertStyleTestTags.NUDGE,
            onClick = { if (style != AlertStyle.NUDGE) onStyleChange(AlertStyle.NUDGE) },
        )
        AlertTile(
            selected = style == AlertStyle.RING,
            icon = R.drawable.ic_alarm,
            title = stringResource(R.string.alert_ring),
            description = stringResource(R.string.alert_ring_desc),
            tag = AlertStyleTestTags.RING,
            onClick = { if (style != AlertStyle.RING) onStyleChange(AlertStyle.RING) },
        )
    }
    AnimatedVisibility(
        visible = style == AlertStyle.RING,
        enter = expandVertically(tween(300)) + fadeIn(tween(300)),
        exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.testTag(AlertStyleTestTags.RING_NOTE)) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = stringResource(R.string.alert_ring_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fullScreenDenied) FullScreenDeniedBanner(onAllow = onAllowFullScreen)
            if (exactDenied) ExactTimingSettingsBanner(onAllow = onAllowExact)
        }
    }
    Spacer(Modifier.size(Spacing.lg))
}

@Composable
private fun AlertTile(
    selected: Boolean,
    @DrawableRes icon: Int,
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        border = if (selected) BorderStroke(1.dp, colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Preview(name = "Alert style — Ring, banners", showBackground = true)
@Composable
private fun AlertStyleRingPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                AlertStyleSettings(
                    style = AlertStyle.RING,
                    fullScreenDenied = true,
                    exactDenied = true,
                    onStyleChange = {},
                    onAllowFullScreen = {},
                    onAllowExact = {},
                )
            }
        }
    }
}
