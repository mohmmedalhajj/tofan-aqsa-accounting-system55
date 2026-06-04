package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50), // PalGreenLight
    onPrimary = Color(0xFFFFFFFF), // PalWhitePure
    primaryContainer = Color(0xFF104620), // PalGreenDark
    onPrimaryContainer = Color(0xFFF7FDF9), // PalWhiteSoft
    secondary = Color(0xFF1A1A1A), // PalBlackLight
    onSecondary = Color(0xFFFFFFFF), // PalWhitePure
    tertiary = Color(0xFFEF5350), // PalRedLight
    onTertiary = Color(0xFFFFFFFF), // PalWhitePure
    background = Color(0xFF0A0A0A), // PalBlackDark
    onBackground = Color(0xFFF7FDF9), // PalWhiteSoft
    surface = Color(0xFF111111), // PalBlackNormal
    onSurface = Color(0xFFF7FDF9), // PalWhiteSoft
    surfaceVariant = Color(0xFF1A1A1A), // PalBlackLight
    onSurfaceVariant = Color(0xFF8E8E93), // PalWhiteMuted
    error = Color(0xFFCE2029), // PalRedNormal
    onError = Color(0xFFFFFFFF) // PalWhitePure
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E753C), // PalGreenNormal
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2F3E7),
    onPrimaryContainer = Color(0xFF104620), // PalGreenDark
    secondary = Color(0xFFEFEFEF),
    onSecondary = Color(0xFF0A0A0A), // PalBlackDark
    tertiary = Color(0xFFCE2029), // PalRedNormal
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FDF9), // PalWhiteSoft
    onBackground = Color(0xFF0A0A0A), // PalBlackDark
    surface = Color(0xFFFFFFFF), // PalWhitePure
    onSurface = Color(0xFF0A0A0A), // PalBlackDark
    surfaceVariant = Color(0xFFF2F4F3),
    onSurfaceVariant = Color(0xFF555555),
    error = Color(0xFFCE2029), // PalRedNormal
    onError = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
