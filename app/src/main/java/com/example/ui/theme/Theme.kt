package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = IndustrialAccent,
    onPrimary = Color.Black,
    primaryContainer = IndustrialPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = IndustrialTeal,
    onSecondary = Color.White,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color.White,
    onSurface = Color.White,
    error = StatusDanger
)

private val LightColorScheme = lightColorScheme(
    primary = IndustrialPrimary,
    onPrimary = Color.White,
    primaryContainer = IndustrialPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = IndustrialAccent,
    onSecondary = Color.White,
    tertiary = IndustrialTeal,
    onTertiary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = StatusDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography
    ) {
        // Enforce RTL layout for Persian UI
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}
