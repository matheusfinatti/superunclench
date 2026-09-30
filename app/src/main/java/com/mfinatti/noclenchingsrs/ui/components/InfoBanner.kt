package com.mfinatti.noclenchingsrs.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

/** design-system §9.4 banner variants. Never uses errorContainer (keeps the app calm). */
enum class InfoBannerVariant { Neutral, Attention, Calm }

/** A text action shown right-aligned under the banner body. */
data class BannerAction(
    val label: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

/**
 * design-system §9.4 `InfoBanner`.
 *
 * @param a11yDescription optional merged description for the icon + text block (e.g. "Note: …").
 */
@Composable
fun InfoBanner(
    body: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    variant: InfoBannerVariant = InfoBannerVariant.Neutral,
    title: String? = null,
    a11yDescription: String? = null,
    actions: List<BannerAction> = emptyList(),
) {
    val colors = MaterialTheme.colorScheme
    val (container, content, iconTint) = when (variant) {
        InfoBannerVariant.Neutral -> Triple(colors.surfaceContainerHigh, colors.onSurface, colors.onSurfaceVariant)
        InfoBannerVariant.Attention -> Triple(
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
            colors.onTertiaryContainer,
        )
        InfoBannerVariant.Calm -> Triple(
            colors.secondaryContainer,
            colors.onSecondaryContainer,
            colors.onSecondaryContainer,
        )
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            val textBlockModifier = if (a11yDescription != null) {
                Modifier.semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            } else {
                Modifier.semantics(mergeDescendants = true) {}
            }
            Row(
                modifier = textBlockModifier,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (title != null) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(text = body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions.forEach { action ->
                        TextButton(
                            onClick = action.onClick,
                            modifier = Modifier
                                .heightIn(min = Dimens.minTouchTarget)
                                .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (variant == InfoBannerVariant.Neutral) colors.primary else content,
                            ),
                        ) {
                            Text(text = action.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Neutral - light")
@Composable
private fun InfoBannerPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        InfoBanner(
            body = "SuperUnclench is an awareness tool, not medical advice. If you have jaw pain, talk to a dentist or doctor.",
            icon = R.drawable.ic_info,
            actions = listOf(BannerAction("Got it", {})),
        )
    }
}

@Preview(name = "Attention - dark")
@Composable
private fun InfoBannerAttentionPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        InfoBanner(
            title = "Heads up",
            body = "Check-ins may be a few minutes late",
            icon = R.drawable.ic_info,
            variant = InfoBannerVariant.Attention,
            actions = listOf(BannerAction("Allow", {})),
        )
    }
}
