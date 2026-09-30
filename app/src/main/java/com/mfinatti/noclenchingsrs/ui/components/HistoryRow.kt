package com.mfinatti.noclenchingsrs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInOutcome
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular

/** design-system §9.6 `HistoryRow`: badge, outcome, "Level n · name", time. */
@Composable
fun HistoryRow(
    outcome: CheckInOutcome,
    levelLine: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    val extended = SuperUnclenchTheme.extendedColors
    val (container: Color, content: Color, icon: Int, label: Int) = when (outcome) {
        CheckInOutcome.GOOD -> Quad(extended.goodContainer, extended.onGoodContainer, R.drawable.ic_good, R.string.answer_good)
        CheckInOutcome.BAD -> Quad(extended.badContainer, extended.onBadContainer, R.drawable.ic_bad, R.string.answer_bad)
        CheckInOutcome.MISSED ->
            Quad(extended.missedContainer, extended.onMissedContainer, R.drawable.ic_missed, R.string.history_missed)
    }
    ListItem(
        modifier = modifier
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {},
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = { Text(stringResource(label), style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(levelLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

private data class Quad(val container: Color, val content: Color, val icon: Int, val label: Int)
