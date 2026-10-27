package com.gacelas.ui.intro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/*
 * Simbolos de marca compartidos (icono dibujado en la app). La intro de arranque ya no se dibuja
 * en Compose: es un video pre-renderizado (res/raw/intro.mp4, docs/MARCA.md).
 */

/** Paleta de marca (docs/MARCA.md). */
internal object Tinta {
    val Morado = Color(0xFFA855F7)
    val MoradoClaro = Color(0xFFC084FC)
    val MoradoOscuro = Color(0xFF7B1FA2)
    val Lila = Color(0xFFF3E8FF)
    val Verde = Color(0xFF10B981)
    val Fondo = Color(0xFF0C1322)
    val FondoProfundo = Color(0xFF070E1D)
    val Violeta = Color(0xFF1A1030)
}

internal const val GAZ = "M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 " +
    "L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 " +
    "C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z"

internal val gacela: Path by lazy { PathParser().parsePathString(GAZ).toPath() }
