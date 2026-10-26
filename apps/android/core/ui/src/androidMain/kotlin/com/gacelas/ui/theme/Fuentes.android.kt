package com.gacelas.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.gacelas.core.ui.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, w: FontWeight) =
    Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

actual val Inter: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { variable(R.font.archivo, it) },
)

actual val RobotoFlex: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black).map { variable(R.font.archivo, it) },
)

/** Saira Semi Condensed 700 (titulares); se registra para todos los pesos usados en titulares. */
actual val SpaceGrotesk: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black).map { Font(R.font.saira_semi_condensed, it) },
)
