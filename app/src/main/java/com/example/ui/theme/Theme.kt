package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One branded scheme for the whole app (navy + gold), independent of the system
 * light/dark setting, so every screen matches the splash, login and launcher icon.
 */
private val AmanColorScheme = darkColorScheme(
    primary = YemenGold,
    onPrimary = BrandInk,
    primaryContainer = BrandSurfaceHigh,
    onPrimaryContainer = PureWhite,
    secondary = BrandCyan,
    onSecondary = BrandInk,
    secondaryContainer = BrandSurfaceHigh,
    onSecondaryContainer = PureWhite,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    error = AlertRed,
    onError = PureWhite,
    errorContainer = RedTint,
    onErrorContainer = Color(0xFFFFB4B4),
    background = BrandBg,
    onBackground = TextPrimaryLight,
    surface = BrandSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = BrandSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = BrandBg,
    surfaceContainerLow = BrandSurface,
    surfaceContainer = BrandSurface,
    surfaceContainerHigh = BrandSurfaceHigh,
    surfaceContainerHighest = BrandSurfaceHigh,
    outline = BrandBorder,
    outlineVariant = BrandBorder,
    inverseSurface = PureWhite,
    inverseOnSurface = BrandInk
)

@Composable
fun MyApplicationTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AmanColorScheme,
        typography = Typography,
        content = content
    )
}
