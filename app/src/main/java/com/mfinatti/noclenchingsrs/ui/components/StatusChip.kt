package com.mfinatti.noclenchingsrs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

/** design-system §9.3 `StatusChip` — non-interactive session status pill. */
@Composable
fun StatusChip(
    status: SessionStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extended = SuperUnclenchTheme.extendedColors
    val container: Color
    val content: Color
    val dot: Color
    val label: String
    when (status) {
        SessionStatus.RUNNING -> {
            container = extended.goodContainer
            content = extended.onGoodContainer
            dot = extended.good
            label = stringResource(R.string.status_running)
        }
        SessionStatus.PAUSED -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            dot = colors.secondary
            label = stringResource(R.string.status_paused)
        }
        SessionStatus.STOPPED -> {
            container = colors.surfaceContainerHighest
            content = colors.onSurfaceVariant
            dot = colors.outline
            label = stringResource(R.string.status_stopped)
        }
    }
    Surface(
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dot, CircleShape),
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** design-system §9.3 quiet-hours chip: `tertiaryContainer` with the bedtime icon. */
@Composable
fun QuietChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bedtime),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(text = stringResource(R.string.quiet_chip), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "Status chips - light")
@Composable
private fun StatusChipPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SessionStatus.entries.forEach { StatusChip(status = it) }
            }
        }
    }
}

@Preview(name = "Status chips - dark")
@Composable
private fun StatusChipDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SessionStatus.entries.forEach { StatusChip(status = it) }
            }
        }
    }
}
