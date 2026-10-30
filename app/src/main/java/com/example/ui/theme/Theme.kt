package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RacingDarkColorScheme = darkColorScheme(
    primary = ApexCyan,
    onPrimary = CarbonBlack,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = ApexOrange,
    onSecondary = CarbonBlack,
    secondaryContainer = Color(0xFF5D2800),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = ApexGold,
    onTertiary = CarbonBlack,
    background = CarbonBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextMuted,
    error = ApexRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forced stylized racing dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
