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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.navigation.Destinos
import com.gacelas.ui.LocalIdioma
import com.gacelas.ui.tr
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.theme.SpaceGrotesk

/*
 * Direccion A "Tablero" del rediseno movil v2 (docs/REDISENO-MOVIL.md): mapa vivo como protagonista,
 * widgets modulares de vidrio morado y dock flotante con CORRER integrado a la derecha.
 */

/** Paleta del tablero: fondo morado nocturno con degradado (nunca negro plano). */
object Tablero {
    // Oscuro: morado nocturno. Claro: lavanda muy clara con el mismo degradado vertical.
    val Noche: Color get() = if (GColors.claro) Color(0xFFF6F2FC) else Color(0xFF1A0B2E)
    val Noche2: Color get() = if (GColors.claro) Color(0xFFF3F1F9) else Color(0xFF140F2B)
    val Noche3: Color get() = if (GColors.claro) Color(0xFFF3F3F7) else Color(0xFF0F1325)
    val Hoja: Color get() = if (GColors.claro) Color(0xFFFFFFFF) else Color(0xFF261042)
    val Vidrio: Color get() = if (GColors.claro) Color(0xE6FFFFFF) else Color(0xB8120E26)
    val VidrioBorde: Color get() = if (GColors.claro) Color(0x4D7A20B6) else Color(0x38C084FC)
    val WidgetBorde: Color get() = if (GColors.claro) Color(0x337A20B6) else Color(0x2EC084FC)
    val Ambar: Color get() = if (GColors.claro) Color(0xFF955104) else Color(0xFFF59E0B)
    val AmbarFondo = Color(0xFFB45309)
    val Texto2: Color get() = GColors.TextPrimary.copy(alpha = if (GColors.claro) 0.72f else 0.62f)
    val Fondo: Brush get() = Brush.verticalGradient(0f to Noche, 0.45f to Noche, 0.75f to Noche2, 1f to Noche3)
    val FondoWidget: Brush get() = if (GColors.claro) Brush.linearGradient(listOf(Color(0xFFF1E8FB), Color(0xFFFFFFFF)), start = Offset(0f, 0f), end = Offset(400f, 700f))
        else Brush.linearGradient(listOf(Color(0x614C1D95), Color(0xE6191F2F)), start = Offset(0f, 0f), end = Offset(400f, 700f))
    val FondoHoja: Brush get() = if (GColors.claro) Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xFFF6F2FC)))
        else Brush.verticalGradient(listOf(Color(0xF5261042), Color(0xFC140F28)))
    val FormaWidget = RoundedCornerShape(20.dp)
    val FormaHoja = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/** Superficie de vidrio sobre el mapa (fondo translucido y borde violeta). */
fun Modifier.vidrioC2(forma: Shape = RoundedCornerShape(16.dp)): Modifier =
    this.clip(forma).background(Tablero.Vidrio).border(1.dp, Tablero.VidrioBorde, forma)

/** Rotulo de los widgets: mayuscula inicial y 12 sp (las mayusculas espaciadas de 9,5 sp no se leian). */
@Composable
fun RotuloWidget(texto: String, modifier: Modifier = Modifier, color: Color = GColors.TextPrimary.copy(alpha = 0.7f)) {
    TextoAjustableC2(texto,GType.labelMd.copy(fontSize = spAcotado(12f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp), color, modifier, minimo = 0.75f)
}

/** Cifra del tablero en Anton (sin cursiva) con unidad pequena. */
@Composable
fun CifraTablero(valor: String, unidad: String = "", modifier: Modifier = Modifier, tamano: Int = 34, color: Color = GColors.TextPrimary, tag: String? = null) {
    Row(modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier), verticalAlignment = Alignment.Bottom) {
        TextoAjustableC2(valor, androidx.compose.ui.text.TextStyle(fontFamily = SpaceGrotesk, fontSize = tamano.sp, lineHeight = (tamano * 1.02).sp), color, minimo = 0.7f)
        if (unidad.isNotEmpty()) Text(" $unidad", style = GType.bodySm.copy(fontSize = 12.sp), color = Tablero.Texto2, modifier = Modifier.padding(bottom = (tamano * 0.08).dp), maxLines = 1)
    }
}

/**
 * Widget del tablero: tarjeta de vidrio morado con rotulo y accion opcional. Siempre tiene accion
 * ([onClick]) para que todo el tablero sea tocable.
 */
@Composable
fun WidgetC2(
    titulo: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
    descripcion: String? = null,
    accion: (@Composable RowScope.() -> Unit)? = null,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier.clip(Tablero.FormaWidget).background(Tablero.FondoWidget).border(1.dp, Tablero.WidgetBorde, Tablero.FormaWidget)
    if (onClick != null) m = m.clickable(onClickLabel = descripcion, role = Role.Button, onClick = onClick)
    if (tag != null) m = m.testTag(tag)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (titulo != null || accion != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (titulo != null) RotuloWidget(titulo, Modifier.weight(1f))
            accion?.invoke(this)
        }
        content()
    }
}

/** Boton redondo de vidrio (volver, notificaciones, filtros). */
@Composable
fun BotonVidrio(icono: ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, tamano: Dp = 48.dp, punto: Boolean = false) {
    Box(
        modifier.size(tamano).vidrioC2(RoundedCornerShape(16.dp)).clickable(onClickLabel = descripcion, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion }.then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, null, tint = GColors.TextPrimary, modifier = Modifier.size(22.dp))
        if (punto) Box(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(GColors.C2Morado))
    }
}

/** Chip de vidrio con punto de color o icono. */
@Composable
fun ChipVidrio(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, punto: Color? = null, icono: ImageVector? = null, activo: Boolean = false, tag: String? = null) {
    val forma = CircleShape
    var m = modifier.clip(forma)
    m = if (activo) m.background(GradienteC2) else m.background(Tablero.Vidrio).border(1.dp, Tablero.VidrioBorde, forma)
    Row(
        m.clickable(role = Role.Button, onClick = onClick).semantics { selected = activo }.then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .heightIn(min = 32.dp).padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (punto != null) Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
        if (icono != null) Icon(icono, null, tint = GColors.C2Menta, modifier = Modifier.size(14.dp))
        Text(texto, style = GType.labelMd.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp), color = if (activo) GColors.OnPrimary else GColors.TextPrimary, maxLines = 1)
    }
}

/**
 * Hoja inferior del tablero (pegada al borde, con tirador y respetando la barra de gestos). Con
 * [expandida] el tirador es interactivo: tocarlo alterna y arrastrarlo arriba/abajo despliega/pliega.
 */
@Composable
fun HojaTablero(
    modifier: Modifier = Modifier, tag: String? = null, espacio: Dp = 10.dp,
    expandida: Boolean? = null, onExpandida: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(Tablero.FormaHoja).background(Tablero.FondoHoja).border(1.dp, GColors.BordeHoja, Tablero.FormaHoja)
            .navigationBarsPadding().then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = if (expandida != null) 0.dp else 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(espacio),
    ) {
        if (expandida == null) Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(GColors.TextPrimary.copy(alpha = 0.25f)))
        else TiradorTablero(expandida, onExpandida)
        content()
    }
}

@Composable
private fun TiradorTablero(expandida: Boolean, onExpandida: (Boolean) -> Unit) {
    val actual = rememberUpdatedState(expandida)
    val cambiar = rememberUpdatedState(onExpandida)
    Box(
        Modifier.fillMaxWidth().height(22.dp)
            .pointerInput(Unit) {
                var total = 0f
                val umbral = 16.dp.toPx()
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = {
                    if (total < -umbral && !actual.value) cambiar.value(true) else if (total > umbral && actual.value) cambiar.value(false)
                }) { _, d -> total += d }
            }
            .clickable(remember { MutableInteractionSource() }, null, onClickLabel = if (expandida) "Mostrar menos datos" else "Mostrar más datos") { onExpandida(!expandida) }
            .testTag("tirador-tablero"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(GColors.TextPrimary.copy(alpha = 0.25f)))
    }
}

/** Boton principal del tablero (degradado con brillo) y secundario blanco. */
@Composable
fun BotonTablero(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = Icons.Filled.PlayArrow, blanco: Boolean = false, alto: Dp = 58.dp, tag: String? = null, enabled: Boolean = true) {
    val forma = RoundedCornerShape(18.dp)
    var m = modifier.heightIn(min = alto)
    if (!blanco && enabled) m = m.shadow(14.dp, forma, ambientColor = GColors.C2Morado, spotColor = GColors.C2Morado)
    m = m.clip(forma).background(if (!enabled) Brush.linearGradient(listOf(GColors.C2S2, GColors.C2S2)) else if (blanco) Brush.linearGradient(listOf(GColors.OnPrimary, GColors.OnPrimary)) else GradienteC2)
    Row(
        m.clickable(enabled = enabled, role = Role.Button, onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (blanco) Color(0xFF3B0764) else GColors.TextPrimary
        if (icono != null) { Icon(icono, null, tint = color, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)) }
        TextoAjustableC2(texto.uppercase(), androidx.compose.ui.text.TextStyle(fontFamily = SpaceGrotesk, fontSize = 22.sp, letterSpacing = 0.03.em), color, minimo = 0.5f)
    }
}

/** Boton cuadrado con icono y etiqueta debajo (Bloquear, Terminar, Voz). */
@Composable
fun BotonCuadroTablero(etiqueta: String, icono: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, descripcion: String = etiqueta, ancho: Dp = 72.dp, alto: Dp = 60.dp, color: Color = GColors.TextPrimary) {
    Column(
        modifier.width(ancho).height(alto).clip(RoundedCornerShape(18.dp)).background(GColors.TextPrimary.copy(alpha = 0.07f))
            .clickable(onClickLabel = descripcion, role = Role.Button, onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
        TextoAjustableC2(etiqueta, GType.labelMd.copy(fontSize = spAcotado(10.5f), fontWeight = FontWeight.Bold), color, minimo = 0.6f)
    }
}

/** Destino del dock. */
private data class ItemDock(val ruta: String, val etiqueta: String, val icono: ImageVector, val tambien: List<String> = emptyList())

private val itemsDock = listOf(
    ItemDock(Destinos.INICIO, "Hoy", Icons.Filled.Home),
    ItemDock(Destinos.RUTAS, "Rutas", Icons.Filled.Map),
    ItemDock(Destinos.COMUNIDAD, "Club", Icons.Filled.Groups, listOf(Destinos.EVENTOS)),
    ItemDock(Destinos.PERFIL, "Tú", Icons.Filled.Person),
)

/** Alto que ocupa el dock (sin la barra de gestos); el contenido deja este espacio libre. */
val AltoDock = 76.dp

/**
 * Dock flotante de vidrio: Hoy, Rutas, Club y Tu, con CORRER integrado a la derecha (sin boton
 * central). El contenido de la pantalla nunca queda debajo: ScaffoldPrincipal reserva su alto.
 */
@Composable
fun DockTablero(rutaActual: String, onNavegar: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 6.dp, top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(22.dp)).background(if (GColors.claro) Color(0xF2FFFFFF) else Color(0xF2140D2A)).border(1.dp, Tablero.VidrioBorde, RoundedCornerShape(22.dp))
                .padding(horizontal = 7.dp).testTag("dock"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            itemsDock.forEach { item ->
                val activo = rutaActual == item.ruta || rutaActual in item.tambien
                Column(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (activo) GColors.TextPrimary.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable(role = Role.Tab, onClickLabel = item.etiqueta) { onNavegar(item.ruta) }.semantics { selected = activo }.testTag("nav-${item.ruta}"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Icon(item.icono, contentDescription = item.etiqueta, tint = if (activo) GColors.TextPrimary else GColors.TextPrimary.copy(alpha = 0.55f), modifier = Modifier.size(21.dp))
                    TextoAjustableC2(tr(LocalIdioma.current, item.etiqueta), GType.labelMd.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), if (activo) GColors.TextPrimary else GColors.TextPrimary.copy(alpha = 0.55f), minimo = 0.7f)
                }
            }
            Row(
                Modifier.widthIn(min = 112.dp).height(50.dp).shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = GColors.C2Morado, spotColor = GColors.C2Morado)
                    .clip(RoundedCornerShape(16.dp)).background(GradienteC2)
                    .clickable(role = Role.Button, onClickLabel = "Correr") { onNavegar(Destinos.CORRER) }.testTag("nav-correr").padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Correr", tint = GColors.OnPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                TextoAjustableC2(tr(LocalIdioma.current, "CORRER"), GType.labelLg.copy(fontWeight = FontWeight.Black, letterSpacing = 0.06.em), GColors.OnPrimary, minimo = 0.7f)
            }
        }
    }
}

/**
 * Mapa esquematico (Canvas) con el trazo real: fondo nocturno, manzanas, rio, avenida y la ruta con
 * brillo. Es el respaldo cuando no hay mapa nativo (pruebas, previas, sin clave) y la miniatura de
 * las tarjetas. [puntos] son lat/lng reales; [otros] trazos se dibujan tenues.
 */
@Composable
fun MapaEsquemaC2(
    puntos: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    otros: List<List<Pair<Double, Double>>> = emptyList(),
    yo: Pair<Double, Double>? = null,
    avance: Float = 1f,
    marcadores: List<Pair<Double, Double>> = emptyList(),
    calles: Boolean = true,
    grosor: Dp = 4.dp,
    perspectiva: Boolean = false,
    margenArriba: Dp = 0.dp,
    margenAbajo: Dp = 0.dp,
) {
    Canvas(modifier.clipToBounds().background(Brush.verticalGradient(listOf(Color(0xFF17122B), Color(0xFF0F1424)))).semantics { contentDescription = "Mapa" }) {
        val w = size.width; val h = size.height
        if (calles) {
            // Parques
            listOf(0.2f to 0.25f, 0.75f to 0.62f, 0.35f to 0.82f).forEach { (fx, fy) ->
                drawOval(Color(0x8C123B33), topLeft = Offset(w * fx - w * 0.14f, h * fy - h * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.16f))
            }
            rotate(-10f) {
                var x = -w; var i = 0
                while (x < w * 2) { drawLine(Color(0xFF262A48), Offset(x, -h), Offset(x, h * 2), strokeWidth = if (i % 5 == 3) 3.5f * density else 1.2f * density); x += 36.dp.toPx(); i++ }
                var y = -h; i = 0
                while (y < h * 2) { drawLine(Color(0xFF262A48), Offset(-w, y), Offset(w * 2, y), strokeWidth = if (i % 6 == 2) 3.5f * density else 1.2f * density); y += 32.dp.toPx(); i++ }
            }
            val rio = Path().apply { moveTo(-10f, h * 0.15f); cubicTo(w * 0.3f, h * 0.3f, w * 0.2f, h * 0.7f, w * 0.55f, h * 0.78f); cubicTo(w * 0.8f, h * 0.84f, w * 0.9f, h * 0.95f, w + 20f, h * 0.9f) }
            drawPath(rio, Color(0xE61D3A63), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            val av = Path().apply { moveTo(0f, h * 0.55f); quadraticTo(w * 0.5f, h * 0.45f, w, h * 0.6f) }
            drawPath(av, Color(0xFF34305A), style = Stroke(6.dp.toPx()))
        }
        // Encuadre en la ruta principal (los otros trazos pueden salir del cuadro), en la franja libre
        // entre [margenArriba] y [margenAbajo] para que no la tapen la busqueda ni las tarjetas.
        val todos = (if (puntos.size >= 2) puntos else otros.flatten()) + marcadores + listOfNotNull(yo)
        if (todos.size < 2) return@Canvas
        val sup = margenArriba.toPx().coerceAtMost(h * 0.6f)
        val hu = (h - sup - margenAbajo.toPx()).coerceAtLeast(h * 0.25f)
        val minLat = todos.minOf { it.first }; val maxLat = todos.maxOf { it.first }
        val minLng = todos.minOf { it.second }; val maxLng = todos.maxOf { it.second }
        val margen = 0.16f
        val escala = minOf(w * (1 - 2 * margen) / (maxLng - minLng).coerceAtLeast(1e-6).toFloat(), hu * (1 - 2 * margen) / (maxLat - minLat).coerceAtLeast(1e-6).toFloat())
        val ox = (w - (maxLng - minLng).toFloat() * escala) / 2; val oy = sup + (hu - (maxLat - minLat).toFloat() * escala) / 2
        fun p(c: Pair<Double, Double>) = Offset(ox + ((c.second - minLng).toFloat() * escala), oy + ((maxLat - c.first).toFloat() * escala))
        fun camino(l: List<Pair<Double, Double>>) = Path().apply { l.forEachIndexed { i, c -> val o = p(c); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        otros.filter { it.size >= 2 }.forEach { drawPath(camino(it), Color(0xB38B8FB8), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        if (puntos.size >= 2) {
            val violeta = GColors.C2Violeta
            val k = (puntos.size * avance).toInt().coerceIn(2, puntos.size)
            val hecho = puntos.take(k)
            if (avance < 1f) drawPath(camino(puntos), violeta.copy(alpha = 0.28f), style = Stroke(grosor.toPx(), cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 14f))))
            drawPath(camino(hecho), violeta.copy(alpha = 0.32f), style = Stroke(grosor.toPx() * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(camino(hecho), violeta, style = Stroke(grosor.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Color(0xFF0C1322), 7.dp.toPx(), p(puntos.first())); drawCircle(GColors.C2Menta, 5.dp.toPx(), p(puntos.first()))
            if (avance < 1f) { val u = p(hecho.last()); drawCircle(GColors.C2Morado.copy(alpha = 0.25f), 22.dp.toPx(), u); drawCircle(GColors.C2Morado, 9.dp.toPx(), u); drawCircle(GColors.OnPrimary, 5.5.dp.toPx(), u) }
        }
        marcadores.forEach { c -> val o = p(c); drawCircle(GColors.OnPrimary, 8.dp.toPx(), o); drawCircle(GColors.C2Morado, 6.dp.toPx(), o) }
        yo?.let { val o = p(it); drawCircle(GColors.C2Morado.copy(alpha = 0.22f), 24.dp.toPx(), o); drawCircle(GColors.C2Morado, 9.dp.toPx(), o); drawCircle(GColors.OnPrimary, 5.5.dp.toPx(), o) }
    }
}

/** Celda de cifra del panel de carrera y del resumen (vidrio tenue). */
@Composable
fun CeldaTablero(etiqueta: String, valor: String, modifier: Modifier = Modifier, unidad: String = "", color: Color = GColors.TextPrimary, tamano: Int = 28, extra: String? = null, colorExtra: Color = GColors.C2Menta, tag: String? = null) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(GColors.TextPrimary.copy(alpha = 0.06f)).border(1.dp, GColors.TextPrimary.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
            .then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        RotuloWidget(etiqueta, color = GColors.TextPrimary.copy(alpha = 0.55f))
        CifraTablero(valor, unidad, Modifier.padding(top = 3.dp), tamano = tamano, color = color)
        if (extra != null) Text(extra, style = GType.labelMd.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = colorExtra, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Barra de progreso blanca sobre fondo translucido (tarjeta de distancia del panel). */
@Composable
fun BarraProgresoTablero(p: Float, modifier: Modifier = Modifier, color: Color = GColors.TextPrimary, fondo: Color = GColors.TextPrimary.copy(alpha = 0.2f)) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(fondo)) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
    }
}

/** Fondo de pantalla del tablero (para usar con Box). */
@Composable
fun FondoTablero(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(Tablero.Fondo), content = content)
}
