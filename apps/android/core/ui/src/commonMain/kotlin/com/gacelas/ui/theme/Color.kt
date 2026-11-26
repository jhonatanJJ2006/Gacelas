package com.gacelas.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Tokens de color equivalentes a apps/web/tailwind.config.js. Paleta morada Social Run Loja (docs/PALETA.md).
 *
 * Dos modos: oscuro (por defecto) y claro. [claro] es un estado de Compose: la raiz de la app lo fija
 * segun Ajustes.tema (y el tema del sistema) y todo lo que lea un token se recompone al cambiarlo.
 * Los valores claros salen de la misma tabla que la web (apps/web/src/index.css, clase `tema-claro`);
 * contraste WCAG AA documentado en docs/TEMA.md. Los rellenos de marca (Primary, Primary600...) son
 * iguales en los dos modos: el texto encima usa [OnPrimary] o [SobreColor], nunca [TextPrimary].
 */
object GColors {
    /** Modo claro activo. Se lee como estado de Compose, asi la UI se recompone al cambiarlo. */
    var claro by mutableStateOf(false)

    private fun modo(oscuro: Long, claroValor: Long) = Color(if (claro) claroValor else oscuro)

    // Marca (iguales en los dos modos): rellenos de botones y contenedores.
    val Primary = Color(0xFF7B1FA2)
    val Primary700 = Color(0xFF5E1781)
    val PrimaryContainer = Color(0xFF7B1FA2)
    val OnPrimary = Color(0xFFFFFFFF)
    /** Texto sobre superficies de color (botones). */
    val Primary600 = Color(0xFF5E1781)
    val PrimaryFixed = Color(0xFFF3E8FF)
    val PrimaryFixedDim = Color(0xFFA855F7)
    val SobreColor = Color(0xFFF9FAFB)
    val OnPrimaryContainer = Color(0xFFFFFFFF)

    // Acentos que se usan como texto o icono: cambian de modo para mantener el contraste.
    val Primary400: Color get() = modo(0xFFC084FC, 0xFF6E1DA5)
    val Primary500: Color get() = modo(0xFFC084FC, 0xFF7A20B6)
    val PrimaryLight: Color get() = modo(0xFFE9D5FF, 0xFF5B257E)
    val InversePrimary: Color get() = modo(0xFFE9D5FF, 0xFF5B257E)

    val Secondary: Color get() = modo(0xFF059669, 0xFF09714F)
    val Secondary500: Color get() = modo(0xFF10B981, 0xFF0A7B55)
    val Secondary700 = Color(0xFF047857)
    val SecondaryLight: Color get() = modo(0xFF68DBA9, 0xFF09714F)
    val OnSecondary = Color(0xFF003825)

    val Tertiary: Color get() = modo(0xFF96CCFF, 0xFF1257A5)
    val Info = Color(0xFF3B82F6)
    val Danger = Color(0xFFDC2626)

    val Surface: Color get() = modo(0xFF0C1322, 0xFFF3F3F7)
    val SurfaceLowest: Color get() = modo(0xFF070E1D, 0xFFFFFFFF)
    val SurfaceLow: Color get() = modo(0xFF141B2B, 0xFFF7F7FA)
    val SurfaceContainer: Color get() = modo(0xFF191F2F, 0xFFFFFFFF)
    val SurfaceHigh: Color get() = modo(0xFF232A3A, 0xFFEAEAF1)
    val SurfaceHighest: Color get() = modo(0xFF2E3545, 0xFFDFDEE8)

    val BgPrimary: Color get() = modo(0xFF0C1322, 0xFFF3F3F7)
    val BgSecondary: Color get() = modo(0xFF191F2F, 0xFFFFFFFF)
    val TextPrimary: Color get() = modo(0xFFFFFFFF, 0xFF161226)
    val TextSecondary: Color get() = modo(0xFFA8B0C2, 0xFF4F4C61)
    val BorderSubtle: Color get() = modo(0xFF2C3446, 0xFFD0CFDE)
    val OnSurface: Color get() = modo(0xFFDCE2F7, 0xFF161226)
    val OnSurfaceVariant: Color get() = modo(0xFFC9CFDB, 0xFF4F4C61)
    val Outline: Color get() = modo(0xFF8C95A8, 0xFF6F6C85)

    val MapWater = Color(0xFF1E3A8A)
    val MapRoads = Color(0xFF374151)
    val MapParks = Color(0xFF064E3B)

    /** Tokens C2 Deporte cinetico (rediseno-c2/tokens.css). */
    val C2Bg: Color get() = modo(0xFF0C1322, 0xFFF3F3F7)
    val C2Card: Color get() = modo(0xFF191F2F, 0xFFFFFFFF)
    val C2S2: Color get() = modo(0xFF232A3A, 0xFFEAEAF1)
    val C2Linea: Color get() = modo(0xFF2C3446, 0xFFD0CFDE)
    val C2Muted: Color get() = modo(0xFFA8B0C2, 0xFF4F4C61)
    val C2Violeta: Color get() = modo(0xFFC084FC, 0xFF7A20B6)
    val C2Morado: Color get() = modo(0xFFA855F7, 0xFF7B21CA)
    val C2Menta: Color get() = modo(0xFF68DBA9, 0xFF09714F)
    val C2Cielo: Color get() = modo(0xFF96CCFF, 0xFF1257A5)
    val C2Peligro: Color get() = modo(0xFFBE185D, 0xFFA71B39)

    /** Texto sobre un relleno C2Violeta o C2Menta (pastilla activa): oscuro en el modo oscuro, blanco en el claro. */
    val SobreVioleta: Color get() = modo(0xFF0C1322, 0xFFFFFFFF)
    /** Texto de alerta (ambar) y de error sobre el fondo del tema. */
    val Alerta: Color get() = modo(0xFFFBBF24, 0xFF955104)
    val Rosa: Color get() = modo(0xFFF472B6, 0xFF9C1157)
    val PeligroTexto: Color get() = modo(0xFFFCA5A5, 0xFFA71B1B)

    // Superficies y textos que antes eran literales oscuros sueltos en las pantallas.
    val Panel: Color get() = modo(0xFF131926, 0xFFFFFFFF)
    val PanelAlto: Color get() = modo(0xFF0F1626, 0xFFF7F7FA)
    val PanelOpcion: Color get() = modo(0xFF141C2E, 0xFFFFFFFF)
    val BordeOpcion: Color get() = modo(0xFF1F2B45, 0xFFD0CFDE)
    val PanelSuave: Color get() = modo(0xFF1C1433, 0xFFFFFFFF)
    /** Vidrio de los controles sobre el mapa. */
    val Vidrio: Color get() = modo(0xE6121A2A, 0xE6FFFFFF)
    val VidrioFuerte: Color get() = modo(0xF2121A2A, 0xF2FFFFFF)
    /** Velo de las tarjetas flotantes sobre el mapa. */
    val Velo: Color get() = modo(0xCC1A0B2E, 0xE6FFFFFF)
    val BordeHoja: Color get() = modo(0x40C084FC, 0x407A20B6)
    val RosaSuave: Color get() = modo(0xFFF9A8D4, 0xFF9C1157)
    val AvisoFondo: Color get() = modo(0xFF2A1F0A, 0xFFFFF4E0)
    val AvisoTexto: Color get() = modo(0xFFF6AD55, 0xFF8A4B05)
    val MapaFondo: Color get() = modo(0xFF0A0E16, 0xFFE9E7F1)
    val MapaLinea: Color get() = modo(0xFF162238, 0xFFD4D2E2)
    val MapaRio: Color get() = modo(0xFF1E3A5F, 0xFFB9CCE8)
    /** Contorno de la ruta dibujada sobre el mapa: casi negro en el mapa oscuro, blanco en el claro. */
    val ContornoRuta: Color get() = modo(0xFF05080F, 0xFFFFFFFF)
}
