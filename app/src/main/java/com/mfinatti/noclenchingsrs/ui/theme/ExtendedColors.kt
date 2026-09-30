package com.mfinatti.noclenchingsrs.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that are not part of the M3 ColorScheme (design-system §2.4).
 * Good/Bad/Missed must always be paired with an icon and a label — never color alone.
 */
@Immutable
data class ExtendedColors(
    val good: Color,
    val onGood: Color,
    val goodContainer: Color,
    val onGoodContainer: Color,
    val bad: Color,
    val onBad: Color,
    val badContainer: Color,
    val onBadContainer: Color,
    val missed: Color,
    val onMissed: Color,
    val missedContainer: Color,
    val onMissedContainer: Color,
)

internal val LightExtendedColors = ExtendedColors(
    good = Color(0xFF276C3E),
    onGood = Color(0xFFFFFFFF),
    goodContainer = Color(0xFFB8F0C3),
    onGoodContainer = Color(0xFF0D5228),
    bad = Color(0xFFB3261E),
    onBad = Color(0xFFFFFFFF),
    badContainer = Color(0xFFFFDAD6),
    onBadContainer = Color(0xFF8C1D18),
    missed = Color(0xFF5D616C),
    onMissed = Color(0xFFFFFFFF),
    missedContainer = Color(0xFFE1E2EC),
    onMissedContainer = Color(0xFF454A55),
)

internal val DarkExtendedColors = ExtendedColors(
    good = Color(0xFF8DD89F),
    onGood = Color(0xFF00391A),
    goodContainer = Color(0xFF0D5228),
    onGoodContainer = Color(0xFFB8F0C3),
    bad = Color(0xFFFFB4AB),
    onBad = Color(0xFF690005),
    badContainer = Color(0xFF8C1D18),
    onBadContainer = Color(0xFFFFDAD6),
    missed = Color(0xFFC3C6CF),
    onMissed = Color(0xFF2E3035),
    missedContainer = Color(0xFF454A55),
    onMissedContainer = Color(0xFFE1E2EC),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
