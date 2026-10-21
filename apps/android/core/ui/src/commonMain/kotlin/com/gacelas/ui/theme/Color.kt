package com.gacelas.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Tokens de color equivalentes a apps/web/tailwind.config.js. Paleta morada Social Run Loja (docs/PALETA.md). */
object GColors {
    /** Tema claro activo. Se lee como estado de Compose, asi la UI se recompone al cambiarlo. */
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

    val Surface: Color get() = if (claro) Color(0xFFF8FAFC) else Color(0xFF0C1322)
    val SurfaceLowest: Color get() = if (claro) Color(0xFFFFFFFF) else Color(0xFF070E1D)
    val SurfaceLow: Color get() = if (claro) Color(0xFFF1F5F9) else Color(0xFF141B2B)
    val SurfaceContainer: Color get() = if (claro) Color(0xFFFFFFFF) else Color(0xFF191F2F)
    val SurfaceHigh: Color get() = if (claro) Color(0xFFE2E8F0) else Color(0xFF232A3A)
    val SurfaceHighest: Color get() = if (claro) Color(0xFFCBD5E1) else Color(0xFF2E3545)

    val BgPrimary: Color get() = if (claro) Color(0xFFF8FAFC) else Color(0xFF111827)
    val BgSecondary: Color get() = if (claro) Color(0xFFFFFFFF) else Color(0xFF1F2937)
    val TextPrimary: Color get() = if (claro) Color(0xFF111827) else Color(0xFFF9FAFB)
    val TextSecondary: Color get() = if (claro) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val BorderSubtle: Color get() = if (claro) Color(0xFFD1D5DB) else Color(0xFF374151)
    val OnSurface: Color get() = if (claro) Color(0xFF1F2937) else Color(0xFFDCE2F7)
    val OnSurfaceVariant = Color(0xFFDBC2B0)
    val Outline = Color(0xFFA38C7C)

    val MapWater = Color(0xFF1E3A8A)
    val MapRoads = Color(0xFF374151)
    val MapParks = Color(0xFF064E3B)
}
