package com.mfinatti.noclenchingsrs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

// design-system §3: system font (Roboto), M3 default scale with the overrides below.
private val BaseTypography = Typography()

val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge,
    // Level number on the Home level card: 45/52, weight 500.
    displayMedium = BaseTypography.displayMedium.copy(fontWeight = FontWeight.Medium),
    displaySmall = BaseTypography.displaySmall,
    headlineLarge = BaseTypography.headlineLarge,
    headlineMedium = BaseTypography.headlineMedium,
    headlineSmall = BaseTypography.headlineSmall,
    titleLarge = BaseTypography.titleLarge,
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = BaseTypography.titleSmall,
    bodyLarge = BaseTypography.bodyLarge,
    bodyMedium = BaseTypography.bodyMedium,
    bodySmall = BaseTypography.bodySmall,
    labelLarge = BaseTypography.labelLarge,
    labelMedium = BaseTypography.labelMedium,
    labelSmall = BaseTypography.labelSmall,
)

/** design-system §3 `numericTabular`: tabular digits for countdowns, counts, times and axis labels. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
