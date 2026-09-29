package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = PureWhite,
    secondary = WarningAmber,
    onSecondary = Navy900,
    secondaryContainer = Navy800,
    onSecondaryContainer = PureWhite,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    error = AlertRed,
    background = DarkBackground,
    onBackground = TextPrimaryLight,
    surface = DarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DarkBorder,
    onSurfaceVariant = TextSecondaryLight
)

private val LightColorScheme = lightColorScheme(
    primary = Navy700,
    onPrimary = PureWhite,
    primaryContainer = Navy800,
    onPrimaryContainer = PureWhite,
    secondary = WarningAmber,
    onSecondary = Navy900,
    secondaryContainer = WarningAmberLight,
    onSecondaryContainer = Navy900,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    error = AlertRed,
    onError = PureWhite,
    errorContainer = AlertRedLight,
    onErrorContainer = AlertRedDark,
    background = GrayBackgroundLight,
    onBackground = TextPrimaryDark,
    surface = GraySurfaceLight,
    onSurface = TextPrimaryDark,
    surfaceVariant = GrayBorderLight,
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded theme for consistent Yemeni identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
