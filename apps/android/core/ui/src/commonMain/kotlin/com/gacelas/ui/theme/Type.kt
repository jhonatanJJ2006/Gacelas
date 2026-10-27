package com.gacelas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Familias tipograficas de la app (mismos archivos en Android e iOS).
 * Android usa las fuentes variables de res/font con FontVariation; iOS usa instancias estaticas
 * de esas mismas fuentes por peso (Compose iOS 1.7 no aplica FontVariation), generadas con
 * tools/fuentes-ios.py.
 */
/** Rediseño C2: en Android apunta a Archivo. Inter para textos de cuerpo (400/500/600/700). */
expect val Inter: FontFamily

/** Rediseño C2: en Android apunta a Archivo. Roboto Flex para titulos, etiquetas y metricas (400/500/600/700/900). */
expect val RobotoFlex: FontFamily

/** Rediseño C2: en Android apunta a Anton (titulares display). Space Grotesk, titulares del diseño C4 (500/600/700). */
expect val SpaceGrotesk: FontFamily

/** Escala tipografica del DESIGN.md (body, label, title, headline, metric). */
object GType {
    val bodySm = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp)
    val bodyMd = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyLg = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp)
    val labelMd = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em, fontWeight = FontWeight.SemiBold)
    val labelLg = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em, fontWeight = FontWeight.SemiBold)
    val titleMd = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val headlineSm = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val headlineMd = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.SemiBold)
    val headlineLg = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.Bold)
    val headlineXl = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 32.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em, fontWeight = FontWeight.Bold)
    val metricHuge = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 36.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em, fontWeight = FontWeight.Bold)
}

val Typography = Typography(
    bodySmall = GType.bodySm,
    bodyMedium = GType.bodyMd,
    bodyLarge = GType.bodyLg,
    labelMedium = GType.labelMd,
    labelLarge = GType.labelLg,
    titleMedium = GType.titleMd,
    headlineSmall = GType.headlineSm,
    headlineMedium = GType.headlineMd,
    headlineLarge = GType.headlineLg,
    displaySmall = GType.headlineXl,
    displayMedium = GType.metricHuge,
)
