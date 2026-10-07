package com.gacelas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color.Black,
    secondary = Color(0xFF10B981),
    onSecondary = Color.Black,
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFFAFAFA)
)

@Composable
fun GacelasTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
