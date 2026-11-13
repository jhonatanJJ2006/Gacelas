package com.gacelas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/*
 * Piezas del rediseno movil (docs del rediseno, fase 1 y 2): chips en paralelogramo, selector segmentado,
 * cifras Anton, tira de cifras, hoja inferior pegada al borde y mini graficas en Canvas (trazo, barras,
 * linea, anillo y mapa de calor). Ninguna inventa datos: todas dibujan lo que reciben.
 */

/** Gradiente de marca C2 (#7B1FA2 a #A855F7). */
val GradienteC2: Brush get() = Brush.linearGradient(listOf(GColors.Primary, GColors.C2Morado))

/** Paralelogramo inclinado (chips del hero de Inicio). */
val FormaParalelogramo = GenericShape { size, _ ->
    val d = size.height * 0.28f
    moveTo(d, 0f); lineTo(size.width, 0f); lineTo(size.width - d, size.height); lineTo(0f, size.height); close()
}

/** Chip en paralelogramo con punto de color opcional. */
@Composable
fun ChipParalelogramo(texto: String, modifier: Modifier = Modifier, color: Color = GColors.C2Violeta, fondo: Color = GColors.C2S2, punto: Color? = null, tag: String? = null) {
    Row(
        modifier.clip(FormaParalelogramo).background(fondo).padding(horizontal = 14.dp, vertical = 5.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto != null) { Box(Modifier.size(7.dp).clip(CircleShape).background(punto)); Spacer(Modifier.width(6.dp)) }
        Text(texto.uppercase(), style = GType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em), color = color, maxLines = 1)
    }
}

/** Chip redondeado de estado (sobre mapas). */
@Composable
fun ChipEstadoC2(texto: String, modifier: Modifier = Modifier, punto: Color? = null, color: Color = GColors.TextPrimary, tag: String? = null) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier.heightIn(min = 34.dp).clip(forma).background(GColors.C2Bg.copy(alpha = 0.92f)).border(1.dp, GColors.C2Linea, forma)
            .padding(horizontal = 12.dp, vertical = 6.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto != null) { Box(Modifier.size(8.dp).clip(CircleShape).background(punto)); Spacer(Modifier.width(7.dp)) }
        Text(texto, style = GType.labelMd.copy(fontWeight = FontWeight.Bold), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Selector segmentado: todas las opciones a la vista y la actual resaltada. */
@Composable
fun SelectorSegmentadoC2(opciones: List<String>, seleccion: Int, onSeleccion: (Int) -> Unit, modifier: Modifier = Modifier, tag: String = "segmentado") {
    val forma = RoundedCornerShape(10.dp)
    Row(modifier.fillMaxWidth().clip(forma).background(GColors.C2Card).border(1.dp, GColors.C2Linea, forma).padding(3.dp).testTag(tag)) {
        opciones.forEachIndexed { i, o ->
            val activa = i == seleccion
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(8.dp))
                    .then(if (activa) Modifier.background(GradienteC2) else Modifier)
                    .clickable(role = Role.Tab) { onSeleccion(i) }.semantics { selected = activa }.testTag("$tag-$i"),
                contentAlignment = Alignment.Center,
            ) {
                TextoAjustableC2(o, GType.labelLg.copy(fontWeight = FontWeight.Bold), if (activa) Color.White else GColors.C2Muted,
                    Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
            }
        }
    }
}

/**
 * Texto de una linea que se encoge hasta [minimo] de su tamano antes de cortarse con puntos.
 * Para piezas de ancho fijo en pantallas de 360 dp o con la fuente del sistema grande.
 */
@Composable
fun TextoAjustableC2(texto: String, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier, minimo: Float = 0.75f) {
    var escala by androidx.compose.runtime.remember(texto) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Text(texto, style = style.copy(fontSize = style.fontSize * escala), color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        modifier = modifier, onTextLayout = { if (it.hasVisualOverflow && escala > minimo + 0.01f) escala = maxOf(minimo, escala - 0.06f) })
}

/** Opcion en tarjeta (Actividad, Objetivo): titulo y detalle, borde violeta si esta elegida. */
@Composable
fun OpcionTarjetaC2(titulo: String, detalle: String, elegida: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null) {
    val forma = RoundedCornerShape(8.dp)
    Column(
        modifier.clip(forma).background(if (elegida) GColors.Primary.copy(alpha = 0.22f) else GColors.C2S2)
            .border(1.dp, if (elegida) GColors.C2Violeta else GColors.C2Linea, forma)
            .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = elegida }
            .padding(horizontal = 8.dp, vertical = 8.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        TextoAjustableC2(titulo, GType.labelLg.copy(fontWeight = FontWeight.Bold), if (elegida) GColors.C2Violeta else GColors.TextPrimary)
        Text(detalle, style = GType.bodySm, color = GColors.C2Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Cifra grande en Anton cursiva con unidad pequena. */
@Composable
fun CifraC2(valor: String, unidad: String = "", modifier: Modifier = Modifier, color: Color = GColors.TextPrimary, tamano: Int = 22, tag: String? = null) {
    Row(modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier), verticalAlignment = Alignment.Bottom) {
        Text(valor, style = GType.metricHuge.copy(fontSize = tamano.sp, lineHeight = (tamano + 2).sp), color = color, maxLines = 1)
        if (unidad.isNotEmpty()) Text(" $unidad", style = GType.labelMd, color = GColors.C2Muted, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
    }
}

/** Una cifra de la tira: valor, unidad y etiqueta. */
data class DatoCifra(val valor: String, val unidad: String, val etiqueta: String, val color: Color = GColors.TextPrimary)

/** Tira de 3 o 4 cifras en celdas iguales. */
@Composable
fun TiraCifrasC2(datos: List<DatoCifra>, modifier: Modifier = Modifier, tag: String = "tira-cifras", tamano: Int = 20) {
    Row(modifier.fillMaxWidth().testTag(tag), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        datos.forEach { d -> CeldaCifraC2(d, Modifier.weight(1f), tamano) }
    }
}

/**
 * Tamano de texto que crece con la fuente del sistema solo hasta [maximo] veces.
 * Para etiquetas cortas dentro de piezas de ancho fijo (celdas, controles del mapa),
 * donde una fuente al 130 % las cortaria.
 */
@Composable
fun spAcotado(valor: Float, maximo: Float = 1.15f): androidx.compose.ui.unit.TextUnit {
    val f = androidx.compose.ui.platform.LocalDensity.current.fontScale
    return (valor * (if (f > maximo) maximo / f else 1f)).sp
}

@Composable
fun CeldaCifraC2(d: DatoCifra, modifier: Modifier = Modifier, tamano: Int = 20) {
    val forma = RoundedCornerShape(8.dp)
    Column(modifier.clip(forma).background(GColors.C2Card).border(1.dp, GColors.C2Linea, forma).padding(horizontal = 9.dp, vertical = 7.dp)
        .semantics(mergeDescendants = true) {}) {
        Text(d.etiqueta.uppercase(), style = GType.labelMd.copy(fontSize = spAcotado(9.5f), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.02.em), color = GColors.C2Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        CifraC2(d.valor, d.unidad, color = d.color, tamano = tamano)
    }
}

/** Tarjeta C2 con titulo de seccion opcional y accion a la derecha. */
@Composable
fun TarjetaSeccionC2(
    titulo: String?, modifier: Modifier = Modifier, accion: String? = null, onAccion: (() -> Unit)? = null, tag: String? = null,
    padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2).padding(padding)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (titulo != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EtiquetaC2(titulo, Modifier.weight(1f))
            if (accion != null && onAccion != null) Text(accion, style = GType.labelMd.copy(fontWeight = FontWeight.Bold), color = GColors.C2Violeta,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAccion).padding(horizontal = 6.dp, vertical = 4.dp))
        }
        content()
    }
}

/** Barra de progreso fina con gradiente. */
@Composable
fun BarraProgresoC2(progreso: Float, modifier: Modifier = Modifier, alto: Dp = 6.dp, color: Brush = GradienteC2) {
    Box(modifier.fillMaxWidth().height(alto).clip(CircleShape).background(GColors.C2S2)) {
        Box(Modifier.fillMaxWidth(progreso.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
    }
}

/**
 * Hoja inferior pegada al borde: esquinas superiores redondeadas, tirador y margen de la barra de
 * gestos (WindowInsets.navigationBars), para que ningun boton quede tapado.
 */
@Composable
fun HojaInferiorC2(modifier: Modifier = Modifier, borde: Color = GColors.C2Linea, tag: String = "hoja-inferior", content: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Column(
        modifier.fillMaxWidth().clip(forma).background(GColors.C2Bg).border(1.dp, borde, forma)
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
        content()
    }
}

/** Boton principal grande con gradiente, esquina cortada y texto Anton (EMPEZAR, GUARDAR Y PUBLICAR). */
@Composable
fun BotonGrandeC2(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icono: (@Composable () -> Unit)? = null, enabled: Boolean = true, tag: String? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).clip(FormaBotonC2).background(if (enabled) GradienteC2 else Brush.linearGradient(listOf(GColors.C2S2, GColors.C2S2)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) { icono(); Spacer(Modifier.width(8.dp)) }
        TextoAjustableC2(texto.uppercase(), GType.headlineSm.copy(fontWeight = FontWeight.Black), Color.White, minimo = 0.7f)
    }
}

/** Normaliza un trazo lat/lng a la caja dada, conservando la proporcion. */
private fun trazoEnCaja(puntos: List<Pair<Double, Double>>, w: Float, h: Float, margen: Float): List<Offset> {
    if (puntos.size < 2) return emptyList()
    val minLat = puntos.minOf { it.first }; val maxLat = puntos.maxOf { it.first }
    val minLng = puntos.minOf { it.second }; val maxLng = puntos.maxOf { it.second }
    val dx = (maxLng - minLng).coerceAtLeast(1e-6); val dy = (maxLat - minLat).coerceAtLeast(1e-6)
    val escala = minOf((w - 2 * margen) / dx, (h - 2 * margen) / dy)
    val ox = (w - dx * escala) / 2; val oy = (h - dy * escala) / 2
    return puntos.map { Offset((ox + (it.second - minLng) * escala).toFloat(), (h - oy - (it.first - minLat) * escala).toFloat()) }
}

/** Mini mapa del trazo real dibujado en Canvas (sin cargar Google Maps en cada tarjeta). */
@Composable
fun MiniTrazoC2(puntos: List<Pair<Double, Double>>, modifier: Modifier = Modifier, color: Color = GColors.C2Violeta, cuadricula: Boolean = true, grosor: Dp = 3.dp) {
    Canvas(modifier.background(Color(0xFF111827)).semantics { contentDescription = "Trazo del recorrido" }) {
        if (cuadricula) {
            val paso = 22.dp.toPx(); var x = 0f
            while (x < size.width) { drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f); x += paso }
            var y = 0f
            while (y < size.height) { drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f); y += paso }
        }
        val pts = trazoEnCaja(puntos, size.width, size.height, 10.dp.toPx())
        if (pts.size < 2) return@Canvas
        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, color.copy(alpha = 0.25f), style = Stroke(grosor.toPx() * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, color, style = Stroke(grosor.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(GColors.C2Menta, 4.dp.toPx(), pts.first())
        drawCircle(Color.White, 4.dp.toPx(), pts.last())
    }
}

/** Barras verticales; la resaltada va en menta. Etiquetas opcionales debajo. */
@Composable
fun BarrasC2(valores: List<Float>, modifier: Modifier = Modifier, etiquetas: List<String> = emptyList(), resaltada: Int? = null, alto: Dp = 72.dp, color: Color = GColors.C2Morado) {
    val max = (valores.maxOrNull() ?: 0f).coerceAtLeast(0.0001f)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(alto), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            valores.forEachIndexed { i, v ->
                val f = (v / max).coerceIn(0f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight(if (v > 0f) f.coerceAtLeast(0.06f) else 0.04f).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(if (v <= 0f) GColors.C2S2 else if (i == resaltada) GColors.C2Menta else color))
            }
        }
        if (etiquetas.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            etiquetas.forEachIndexed { i, e ->
                Text(e, style = GType.labelMd.copy(fontSize = 10.sp), color = if (i == resaltada) GColors.C2Menta else GColors.C2Muted, maxLines = 1,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** Linea con area (elevacion, ritmo). */
@Composable
fun LineaC2(valores: List<Float>, modifier: Modifier = Modifier, color: Color = GColors.C2Cielo, invertida: Boolean = false) {
    Canvas(modifier.fillMaxWidth()) {
        if (valores.size < 2) return@Canvas
        val min = valores.min(); val max = valores.max(); val r = (max - min).coerceAtLeast(0.0001f)
        val paso = size.width / (valores.size - 1)
        val pts = valores.mapIndexed { i, v ->
            val n = (v - min) / r
            Offset(i * paso, size.height * (if (invertida) n else 1f - n) * 0.9f + size.height * 0.05f)
        }
        val linea = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        val area = Path().apply { addPath(linea); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(linea, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Anillo de progreso con contenido al centro. */
@Composable
fun AnilloC2(progreso: Float, modifier: Modifier = Modifier, tamano: Dp = 64.dp, grosor: Dp = 7.dp, color: Color = GColors.C2Violeta, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tamano)) {
            val s = grosor.toPx(); val d = size.minDimension - s
            drawArc(GColors.C2S2, 0f, 360f, false, Offset(s / 2, s / 2), Size(d, d), style = Stroke(s))
            drawArc(color, -90f, 360f * progreso.coerceIn(0f, 1f), false, Offset(s / 2, s / 2), Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Mapa de calor de constancia: columnas = semanas, filas = dias (L a D). Valores 0..1. */
@Composable
fun MapaCalorC2(semanas: List<List<Float>>, modifier: Modifier = Modifier, alto: Dp = 84.dp) {
    Canvas(modifier.fillMaxWidth().height(alto).semantics { contentDescription = "Constancia de las últimas ${semanas.size} semanas" }) {
        if (semanas.isEmpty()) return@Canvas
        val hueco = 3.dp.toPx()
        val lado = minOf((size.width - hueco * (semanas.size - 1)) / semanas.size, (size.height - hueco * 6) / 7)
        semanas.forEachIndexed { x, dias ->
            dias.forEachIndexed { y, v ->
                val c = if (v <= 0f) GColors.C2S2 else GColors.C2Morado.copy(alpha = 0.35f + 0.65f * v.coerceIn(0f, 1f))
                drawRoundRect(c, Offset(x * (lado + hueco), y * (lado + hueco)), Size(lado, lado), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

/** Fila de estado (Voz, En vivo, SOS) con punto verde o gris. */
@Composable
fun RowScope.EstadoC2(titulo: String, detalle: String, activo: Boolean, onClick: (() -> Unit)? = null, tag: String? = null) {
    val forma = RoundedCornerShape(8.dp)
    Row(
        Modifier.weight(1f).clip(forma).background(GColors.C2S2).border(1.dp, GColors.C2Linea, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 7.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = GType.labelLg.copy(fontWeight = FontWeight.Bold), color = GColors.TextPrimary, maxLines = 1)
            Text(detalle, style = GType.bodySm, color = GColors.C2Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (activo) GColors.C2Menta else GColors.C2Muted.copy(alpha = 0.4f)))
    }
}
