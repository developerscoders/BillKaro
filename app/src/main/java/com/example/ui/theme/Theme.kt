package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekDarkContainer,
    onSecondary = SleekDarkContainerAccent,
    secondaryContainer = SleekPrimaryContainer,
    onSecondaryContainer = SleekOnPrimaryContainer,
    tertiary = SleekDarkContainerAccent,
    background = SleekBackground,
    onBackground = SleekTextPrimary,
    surface = SleekSurface,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekBorder,
    error = SleekOutstandingRed,
    onError = Color.White,
    errorContainer = SleekOutstandingBadgeBg,
    onErrorContainer = SleekOutstandingBadgeText
)

private val DarkColorScheme = darkColorScheme(
    primary = SleekDarkContainerAccent,
    onPrimary = SleekDarkContainerIconText,
    primaryContainer = SleekDarkContainer,
    onPrimaryContainer = SleekDarkContainerAccent,
    secondary = SleekPrimaryContainer,
    onSecondary = SleekOnPrimaryContainer,
    secondaryContainer = SleekDarkContainer,
    onSecondaryContainer = SleekDarkContainerAccent,
    tertiary = SleekDarkContainerAccent,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF49454F),
    error = SleekOutstandingRed,
    onError = Color.White
)

@Composable
fun BillKaroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding
    content: @Composable () -> Unit
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
