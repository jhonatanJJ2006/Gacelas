package com.gacelas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GacelasDarkScheme = darkColorScheme(
    primary = GColors.Primary,
    onPrimary = GColors.TextPrimary,
    primaryContainer = GColors.PrimaryContainer,
    onPrimaryContainer = GColors.OnPrimaryContainer,
    secondary = GColors.Secondary,
    onSecondary = GColors.TextPrimary,
    tertiary = GColors.Tertiary,
    background = GColors.BgPrimary,
    onBackground = GColors.TextPrimary,
    surface = GColors.BgPrimary,
    onSurface = GColors.TextPrimary,
    surfaceVariant = GColors.SurfaceContainer,
    onSurfaceVariant = GColors.TextSecondary,
    surfaceContainer = GColors.SurfaceContainer,
    surfaceContainerLow = GColors.SurfaceLow,
    surfaceContainerHigh = GColors.SurfaceHigh,
    surfaceContainerHighest = GColors.SurfaceHighest,
    outline = GColors.BorderSubtle,
    outlineVariant = GColors.BorderSubtle,
    error = GColors.Danger,
)

/** Tema de Gacelas: siempre oscuro, igual que la web. */
@Composable
fun GacelasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GacelasDarkScheme, typography = Typography, content = content)
}
