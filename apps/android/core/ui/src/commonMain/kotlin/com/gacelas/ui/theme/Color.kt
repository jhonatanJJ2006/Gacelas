package com.gacelas.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Tokens de color equivalentes a apps/web/tailwind.config.js. Paleta morada Social Run Loja (docs/PALETA.md). */
object GColors {
    /** Rediseño C2: la app solo usa fondos oscuros; este estado se conserva por compatibilidad con Ajustes.
     * Tema claro activo. Se lee como estado de Compose, asi la UI se recompone al cambiarlo. */
    var claro by mutableStateOf(false)

    val Primary = Color(0xFF7B1FA2)
    val Primary400 = Color(0xFFC084FC)
    val Primary500 = Color(0xFFC084FC)
    val Primary700 = Color(0xFF5E1781)
    val PrimaryLight = Color(0xFFE9D5FF)
    val PrimaryContainer = Color(0xFF7B1FA2)
    val OnPrimary = Color(0xFFFFFFFF)
    /** Texto sobre superficies de color (botones). */
    val Primary600 = Color(0xFF5E1781)
    val PrimaryFixed = Color(0xFFF3E8FF)
    val PrimaryFixedDim = Color(0xFFA855F7)
    val InversePrimary = Color(0xFFE9D5FF)
    val SobreColor = Color(0xFFF9FAFB)
    val OnPrimaryContainer = Color(0xFFFFFFFF)

    val Secondary = Color(0xFF059669)
    val Secondary500 = Color(0xFF10B981)
    val Secondary700 = Color(0xFF047857)
    val SecondaryLight = Color(0xFF68DBA9)
    val OnSecondary = Color(0xFF003825)

    val Tertiary = Color(0xFF96CCFF)
    val Info = Color(0xFF3B82F6)
    val Danger = Color(0xFFDC2626)

    val Surface: Color get() = Color(0xFF0C1322)
    val SurfaceLowest: Color get() = Color(0xFF070E1D)
    val SurfaceLow: Color get() = Color(0xFF141B2B)
    val SurfaceContainer: Color get() = Color(0xFF191F2F)
    val SurfaceHigh: Color get() = Color(0xFF232A3A)
    val SurfaceHighest: Color get() = Color(0xFF2E3545)

    val BgPrimary: Color get() = Color(0xFF0C1322)
    val BgSecondary: Color get() = Color(0xFF191F2F)
    val TextPrimary: Color get() = Color(0xFFFFFFFF)
    val TextSecondary: Color get() = Color(0xFFA8B0C2)
    val BorderSubtle: Color get() = Color(0xFF2C3446)
    val OnSurface: Color get() = Color(0xFFDCE2F7)
    val OnSurfaceVariant = Color(0xFFC9CFDB)
    val Outline = Color(0xFF8C95A8)

    val MapWater = Color(0xFF1E3A8A)
    val MapRoads = Color(0xFF374151)
    val MapParks = Color(0xFF064E3B)

    /** Tokens C2 Deporte cinetico (rediseno-c2/tokens.css). */
    val C2Bg = Color(0xFF0C1322)
    val C2Card = Color(0xFF191F2F)
    val C2S2 = Color(0xFF232A3A)
    val C2Linea = Color(0xFF2C3446)
    val C2Muted = Color(0xFFA8B0C2)
    val C2Violeta = Color(0xFFC084FC)
    val C2Morado = Color(0xFFA855F7)
    val C2Menta = Color(0xFF68DBA9)
    val C2Cielo = Color(0xFF96CCFF)
}
