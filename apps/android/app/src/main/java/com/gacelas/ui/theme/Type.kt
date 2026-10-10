package com.gacelas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.R

@OptIn(ExperimentalTextApi::class)
private fun interFont(weight: FontWeight) =
    Font(R.font.inter, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

@OptIn(ExperimentalTextApi::class)
private fun robotoFlexFont(weight: FontWeight) =
    Font(R.font.roboto_flex, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Inter para textos de cuerpo. */
val Inter = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold),
)

/** Roboto Flex para titulos, etiquetas y metricas. */
val RobotoFlex = FontFamily(
    robotoFlexFont(FontWeight.Normal),
    robotoFlexFont(FontWeight.Medium),
    robotoFlexFont(FontWeight.SemiBold),
    robotoFlexFont(FontWeight.Bold),
    robotoFlexFont(FontWeight.Black),
)

/** Escala tipografica del DESIGN.md (body, label, title, headline, metric). */
object GType {
    val bodySm = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp)
    val bodyMd = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyLg = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp)
    val labelMd = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em, fontWeight = FontWeight.SemiBold)
    val labelLg = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em, fontWeight = FontWeight.SemiBold)
    val titleMd = TextStyle(fontFamily = RobotoFlex, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val headlineSm = TextStyle(fontFamily = RobotoFlex, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val headlineMd = TextStyle(fontFamily = RobotoFlex, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.SemiBold)
    val headlineLg = TextStyle(fontFamily = RobotoFlex, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.Bold)
    val headlineXl = TextStyle(fontFamily = RobotoFlex, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em, fontWeight = FontWeight.Bold)
    val metricHuge = TextStyle(fontFamily = RobotoFlex, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.03).em, fontWeight = FontWeight.Bold)
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
