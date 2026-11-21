package com.seerah.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val SeerahLightColorScheme = lightColorScheme(
    primary = SeerahEmerald,
    onPrimary = SeerahIvory,
    primaryContainer = SeerahDeepGreen,
    onPrimaryContainer = SeerahParchment,
    secondary = SeerahAntiqueGold,
    onSecondary = Color.White,
    secondaryContainer = SeerahParchment,
    onSecondaryContainer = SeerahHistoricalBrown,
    tertiary = SeerahMutedBlue,
    onTertiary = Color.White,
    background = SeerahIvory,
    onBackground = SeerahInkDark,
    surface = SeerahIvory,
    onSurface = SeerahInkDark,
    surfaceVariant = SeerahParchment,
    onSurfaceVariant = SeerahHistoricalBrown,
    outline = SeerahAntiqueGold
)

private val SeerahDarkColorScheme = darkColorScheme(
    primary = SeerahGoldLight,
    onPrimary = SeerahDeepGreen,
    primaryContainer = SeerahEmerald,
    onPrimaryContainer = SeerahNightText,
    secondary = SeerahAntiqueGold,
    onSecondary = SeerahDeepGreen,
    secondaryContainer = SeerahNightCard,
    onSecondaryContainer = SeerahParchment,
    tertiary = SeerahMutedBlue,
    onTertiary = SeerahDeepGreen,
    background = SeerahNightBg,
    onBackground = SeerahNightText,
    surface = SeerahNightSurface,
    onSurface = SeerahNightText,
    surfaceVariant = SeerahNightCard,
    onSurfaceVariant = SeerahNightMutedText,
    outline = SeerahAntiqueGold
)

@Composable
fun SeerahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SeerahDarkColorScheme else SeerahLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildSeerahTypography(fontScale),
            content = content
        )
    }
}
