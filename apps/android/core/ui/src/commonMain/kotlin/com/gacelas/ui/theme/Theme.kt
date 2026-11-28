package com.gacelas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private fun gacelasScheme(claro: Boolean) = (if (claro) lightColorScheme() else darkColorScheme()).copy(
    primary = GColors.Primary,
    onPrimary = GColors.OnPrimary,
    primaryContainer = GColors.PrimaryContainer,
    onPrimaryContainer = GColors.OnPrimaryContainer,
    secondary = GColors.Secondary,
    onSecondary = GColors.OnPrimary,
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

/** Tema de Runcity: claro u oscuro segun [GColors.claro] (Ajustes: Claro, Oscuro o Sistema). */
@Composable
fun GacelasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = gacelasScheme(GColors.claro), typography = Typography, content = content)
}
