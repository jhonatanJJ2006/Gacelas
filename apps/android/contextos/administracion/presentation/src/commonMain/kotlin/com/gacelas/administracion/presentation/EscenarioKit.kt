package com.gacelas.administracion.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.ui.movimiento.pulsacionC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/*
 * Kit visual de la propuesta 1 "Escenario" del panel (movil). Lo usan solo las pantallas del panel.
 * Los colores se leen de GColors.claro para respetar el modo claro/oscuro de la app; sin el modo
 * claro activo, usan la paleta oscura Escenario (docs/PALETA.md, tokens del rediseno).
 */

private fun modo(oscuro: Long, claro: Long) = Color(if (GColors.claro) claro else oscuro)

internal object Esc {
    val Fondo get() = modo(0xFF070B14, 0xFFF4F3F8)
    val Card get() = modo(0xFF0F1626, 0xFFFFFFFF)
    val CardAlta get() = modo(0xFF161E33, 0xFFEDEBF4)
    val Borde get() = modo(0xFF232C45, 0xFFD9D5E6)
    val Texto get() = modo(0xFFF3F5FA, 0xFF14111F)
    val Texto2 get() = modo(0xFFA4ADC3, 0xFF54506A)
    val Texto3 get() = modo(0xFF6F7993, 0xFF7C7893)
    val Violeta get() = modo(0xFFC084FC, 0xFF7B1FA2)
    val Morado get() = Color(0xFF7B1FA2)
    val Menta get() = modo(0xFF68DBA9, 0xFF047857)
    val Ambar get() = modo(0xFFFBBF24, 0xFFB45309)
    val Peligro get() = modo(0xFFFB7185, 0xFFBE123C)
    val Cielo get() = modo(0xFF96CCFF, 0xFF1D4ED8)
    val Ambar1 = Color(0xFFD97706)
    val Ambar2 = Color(0xFFB45309)
}

internal enum class Tono { Neutro, Violeta, Menta, Ambar, Peligro, Cielo }

@Composable
internal fun colorTono(t: Tono): Color = when (t) {
    Tono.Neutro -> Esc.Texto2
    Tono.Violeta -> Esc.Violeta
    Tono.Menta -> Esc.Menta
    Tono.Ambar -> Esc.Ambar
    Tono.Peligro -> Esc.Peligro
    Tono.Cielo -> Esc.Cielo
}

private val FormaTarjeta = RoundedCornerShape(18.dp)

/** Titulo de pantalla: etiqueta pequena en violeta y titulo grande. */
@Composable
internal fun TituloEsc(eyebrow: String, titulo: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(eyebrow.uppercase(), style = GType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = Esc.Violeta)
        Text(titulo, style = GType.bodyMd.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
    }
}

@Composable
internal fun TarjetaEsc(modifier: Modifier = Modifier, fondo: Color = Esc.Card, borde: Color = Esc.Borde, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    var m = modifier.fillMaxWidth().clip(FormaTarjeta).background(fondo).border(BorderStroke(1.dp, borde), FormaTarjeta)
    if (onClick != null) m = m.pulsacionC2(interaccion).clickable(interaccion, androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
    Column(m.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
internal fun ChipEsc(texto: String, tono: Tono = Tono.Neutro, modifier: Modifier = Modifier) {
    val c = colorTono(tono)
    Text(
        texto,
        style = GType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
        color = c, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Pestanas segmentadas con scroll horizontal si no caben. */
@Composable
internal fun <T> PestanasEsc(opciones: List<T>, activa: T, etiqueta: (T) -> String, onElegir: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(14.dp)).background(Esc.Card)
            .border(BorderStroke(1.dp, Esc.Borde), RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opciones.forEach { o ->
            val on = o == activa
            Text(
                etiqueta(o),
                style = GType.labelMd.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = if (on) Color.White else Esc.Texto2, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (on) Brush.linearGradient(listOf(Esc.Morado, Color(0xFF5E1781))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable(role = Role.Tab) { onElegir(o) }.semantics { selected = on }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

/** Filtros por tipo: chips pequenos con scroll horizontal. */
@Composable
internal fun <T> FiltrosEsc(opciones: List<T>, activa: T, etiqueta: (T) -> String, onElegir: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { o ->
            val on = o == activa
            Text(
                etiqueta(o),
                style = GType.labelMd.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = if (on) Esc.Violeta else Esc.Texto2, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Esc.Violeta.copy(alpha = 0.16f) else Esc.Card)
                    .border(BorderStroke(1.dp, if (on) Esc.Violeta.copy(alpha = 0.5f) else Esc.Borde), RoundedCornerShape(50))
                    .clickable(role = Role.Button) { onElegir(o) }.semantics { selected = on }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

internal enum class TipoBoton { Primario, Secundario, Peligro, Ok, Fantasma }

@Composable
internal fun BotonEsc(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, tipo: TipoBoton = TipoBoton.Primario, enabled: Boolean = true) {
    val forma = RoundedCornerShape(14.dp)
    val interaccion = remember { MutableInteractionSource() }
    val (fondo, color) = when {
        !enabled -> Brush.linearGradient(listOf(Esc.CardAlta, Esc.CardAlta)) to Esc.Texto3
        tipo == TipoBoton.Primario -> Brush.linearGradient(listOf(Esc.Morado, Color(0xFF5E1781))) to Color.White
        tipo == TipoBoton.Peligro -> Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C))) to Color.White
        tipo == TipoBoton.Ok -> Brush.linearGradient(listOf(Color(0xFF68DBA9), Color(0xFF34D399))) to Color(0xFF053B28)
        tipo == TipoBoton.Secundario -> Brush.linearGradient(listOf(Esc.CardAlta, Esc.CardAlta)) to Esc.Texto
        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) to Esc.Texto2
    }
    Box(
        modifier.height(46.dp).pulsacionC2(interaccion).clip(forma).background(fondo)
            .then(if (tipo == TipoBoton.Secundario || !enabled) Modifier.border(BorderStroke(1.dp, Esc.Borde), forma) else Modifier)
            .clickable(interaccion, androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp), color = color, maxLines = 1) }
}

@Composable
internal fun AvatarEsc(nombre: String, tam: Int = 38) {
    val ini = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.let { p ->
        if (p.isEmpty()) "?" else (p.first().take(1) + (if (p.size > 1) p.last().take(1) else "")).uppercase()
    }
    Box(Modifier.size(tam.dp).clip(CircleShape).background(Esc.Violeta.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Text(ini, style = GType.labelMd.copy(fontSize = (tam * 0.34f).sp, fontWeight = FontWeight.ExtraBold), color = Esc.Violeta)
    }
}

/** Cifra con etiqueta, para mosaicos. */
@Composable
internal fun KpiEsc(valor: String, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(FormaTarjeta).background(Esc.Card).border(BorderStroke(1.dp, Esc.Borde), FormaTarjeta).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(valor, style = GType.bodyMd.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
        Text(etiqueta, style = GType.bodySm, color = Esc.Texto2, maxLines = 1)
    }
}

/** "hace 3 h" a partir de una marca ISO; vacio si no se puede leer. */
internal fun haceCuanto(iso: String?, ahora: Instant = Clock.System.now()): String {
    val t = try { Instant.parse(iso ?: return "") } catch (_: IllegalArgumentException) { return "" }
    val min = (ahora - t).inWholeMinutes.coerceAtLeast(0)
    return when {
        min < 1 -> "hace un momento"
        min < 60 -> "hace $min min"
        min < 60 * 24 -> "hace ${min / 60} h"
        else -> "hace ${min / (60 * 24)} d"
    }
}
