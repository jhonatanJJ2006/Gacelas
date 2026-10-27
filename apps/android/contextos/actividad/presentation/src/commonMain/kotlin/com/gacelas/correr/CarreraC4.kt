package com.gacelas.correr

import com.gacelas.actividad.presentation.tiempoTexto
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.ui.theme.SpaceGrotesk
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.theme.Inter

/** Valores del export de Stitch "Carrera V1 Barra superior" (tools/carrera/V1-*.html). */
object K1 {
    val Fondo = Color(0xFF0C1322)
    val Tarjeta = Color(0xFF191F2F)
    val Secondary = Color(0xFFA8B0C2)
    val Naranja = Color(0xFF7B1FA2)
    val Magenta = Color(0xFFC084FC)
    val Rojo = Color(0xFFE5484D)
    val Verde = Color(0xFF10B981)
    val Ambar = Color(0xFFA855F7)
    fun b(a: Float) = Color.White.copy(alpha = a)
}

private val num = SpaceGrotesk

/** Tarjeta glass: #131926/90 (sin blur en Compose: 94%), borde blanco 10%, linea superior en degradado (inset 16). */
@Composable
fun Modifier.vidrio(forma: Shape, alpha: Float = 0.94f, sombra: Dp = 16.dp): Modifier =
    this.shadow(sombra, forma, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
        .clip(forma).background(K1.Tarjeta.copy(alpha = alpha)).border(1.dp, K1.b(0.10f), forma)

@Composable
private fun BoxScope.LineaSuperior(inset: Dp = 16.dp) {
    Box(Modifier.align(Alignment.TopCenter).padding(horizontal = inset).fillMaxWidth().height(1.dp)
        .background(Brush.horizontalGradient(listOf(Color.Transparent, K1.b(0.20f), Color.Transparent))))
}

/** Estado visible de la pantalla (separado del tracker para tests/previas). */
data class VistaCarrera(
    val estado: EstadoCarrera,
    val metricas: Boolean = true,
)

typealias ControlesMapaC4 = @Composable BoxScope.(recentrar: () -> Unit, norte: () -> Unit, rumbo: Float, siguiendo: Boolean) -> Unit

/**
 * Pantalla de carrera V1: mapa a pantalla completa, tarjeta de metricas arriba, chips, pausa/stop
 * flotando abajo y panel del cronometro que entra desde la derecha. Sin scroll.
 */
@Composable
fun ContenidoCarreraC4(
    vista: VistaCarrera,
    onPausa: () -> Unit,
    onStop: () -> Unit,
    onVuelta: () -> Unit,
    onAtras: () -> Unit,
    panelInicial: Boolean = false,
    extraCabecera: @Composable () -> Unit = {},
    vozActiva: Boolean = false,
    onVoz: (() -> Unit)? = null,
    autopausa: Boolean = false,
    /** Pulsaciones; null mientras no haya un sensor real conectado (el bloque queda oculto). */
    pulsaciones: Int? = null,
    /** Descarta la carrera desde el modal de terminar; null oculta la opcion. */
    onDescartar: (() -> Unit)? = null,
    mapa: @Composable (vista3d: Boolean, controles: ControlesMapaC4, panelAbierto: Boolean) -> Unit,
) {
    var bloqueo by rememberSaveable { mutableStateOf(false) }
    val e = vista.estado
    val m = vista.metricas
    var panel by rememberSaveable { mutableStateOf(panelInicial) }
    var vista3d by rememberSaveable { mutableStateOf(true) }
    var modalFin by rememberSaveable { mutableStateOf(false) }
    var modalPausa by rememberSaveable { mutableStateOf(false) }
    var gpsAvisado by rememberSaveable { mutableStateOf(false) }
    val gpsDebil = (e.ultimaPrecisionM ?: 0f) > 50f && !e.pausada
    ManejarAtras { if (panel) panel = false else onAtras() }

    BoxWithConstraints(Modifier.fillMaxSize().background(K1.Fondo).testTag("pantalla-carrera")) {
        val bajo = maxHeight < 640.dp
        val controles: ControlesMapaC4 = { recentrar, norte, rumbo, siguiendo ->
            Column(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 14.dp, top = 58.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End,
            ) {
                // Sin seguimiento (tras arrastrar) el boton se enciende en violeta.
                BotonMapa("Recentrar", recentrar, if (siguiendo) "boton-recentrar" else "boton-recentrar-activo", fondo = if (siguiendo) null else K1.Naranja) {
                    Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                BotonMapa("Vista ${if (vista3d) "2D" else "3D"}", { vista3d = !vista3d }, "boton-3d") {
                    Text(if (vista3d) "3D" else "2D", style = TextStyle(fontFamily = num, fontSize = 13.sp, fontWeight = FontWeight.Bold), color = Color.White)
                }
                BotonMapa("Orientar al norte", norte, "boton-brujula") {
                    Text("N", style = TextStyle(fontFamily = num, fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.rotate(-rumbo))
                }
                BotonMapa("Ver cronómetro", { panel = true }, "boton-cronometro") { Icon(Icons.Filled.Timer, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                if (onVoz != null) BotonMapa(if (vozActiva) "Apagar voz" else "Activar voz", onVoz, "boton-voz", fondo = if (vozActiva) null else GColorsC2.S2) {
                    Icon(if (vozActiva) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, null, tint = if (vozActiva) GColorsC2.Violeta else K1.Secondary, modifier = Modifier.size(20.dp))
                }
                extraCabecera()
            }
        }
        mapa(vista3d, controles, panel)

        // Cabecera C2: chips de estado y ritmo sobre el mapa (sin logo, como el frame Carrera en curso).
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp).padding(top = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Volver pasa por onAtras: la app pide confirmacion antes de interrumpir la carrera.
                com.gacelas.ui.components.BotonVolverMapa(onAtras)
                val gps = e.ultimaPrecisionM
                val senal = when { gps == null -> "BUSCANDO GPS"; gps < 15f -> "GPS FUERTE"; gps < 50f -> "GPS MEDIO"; else -> "GPS DÉBIL" }
                ChipC2(
                    "${if (e.pausada) "EN PAUSA" else "EN CURSO"} · $senal",
                    punto = when { gps == null -> K1.Secondary; gps < 15f -> GColorsC2.Menta; else -> K1.Ambar },
                    modifier = Modifier.weight(1.15f), tag = "chip-gps",
                )
                ChipC2("PROMEDIO ${formatearRitmo(Unidades.ritmo(e.ritmoPromedioSegKm, m))}", modifier = Modifier.weight(1f), tag = "chip-Prom")
            }
            if (e.puntos.size < 2) Text(
                if (e.pausada) "Carrera en pausa" else "Esperando señal GPS. Muévete al aire libre.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = K1.Secondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }

        // Panel compacto C2 (frame Carrera en curso): asa, metricas pequenas en linea, barras de parciales y controles.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 12.dp)
                .clip(RoundedCornerShape(16.dp)).background(GColorsC2.Panel)
                .border(1.dp, GColorsC2.Linea, RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)
                .testTag("tarjeta-metricas"),
            verticalArrangement = Arrangement.spacedBy(if (bajo) 6.dp else 8.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(34.dp).height(4.dp).clip(CircleShape).background(K1.b(0.18f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1.35f)) {
                    Text(if (e.pausada) "EN PAUSA" else "DISTANCIA", style = etiqueta.copy(fontWeight = FontWeight.ExtraBold), color = GColorsC2.Muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Unidades.texto(e.distanciaKm, m), style = TextStyle(fontFamily = num, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = if (bajo) 34.sp else 40.sp, lineHeight = if (bajo) 36.sp else 42.sp), color = GColorsC2.Violeta, modifier = Modifier.testTag("distancia"))
                        Text(if (m) " km" else " mi", style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Bold), color = GColorsC2.Muted, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                MetricaLado("TIEMPO", e.tiempoTexto, "", Modifier.weight(1f))
                MetricaLado("RITMO", formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm ?: e.ritmoPromedioSegKm, m)), "", Modifier.weight(0.85f), GColorsC2.Menta)
                MetricaLado("DESNIVEL", "+${e.desnivelM.toInt()}", "", Modifier.weight(0.85f), GColorsC2.Cielo)
            }
            BarrasParciales(e)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("PARCIALES", style = etiqueta.copy(fontWeight = FontWeight.ExtraBold), color = GColorsC2.Muted, modifier = Modifier.weight(1f))
                val km = e.parciales.filter { !it.manual }
                val mejor = km.minByOrNull { it.segundos / it.distanciaKm.coerceAtLeast(0.001) }
                val ultimo = km.lastOrNull()
                Text(buildAnnotatedString {
                    if (ultimo == null) append("Primer km en curso")
                    else {
                        append("KM ${ultimo.numero} ")
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append(formatearMs(ultimo.segundos)) }
                        if (mejor != null) {
                            append(" · MEJOR KM ${mejor.numero} ")
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append(formatearMs(mejor.segundos)) }
                        }
                    }
                }, style = etiqueta, color = GColorsC2.Muted, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(GColorsC2.S2).clickable { bloqueo = true }.testTag("boton-bloqueo"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Lock, "Bloquear pantalla", tint = Color.White, modifier = Modifier.size(22.dp)) }
                Row(
                    Modifier.weight(1f).height(52.dp).clip(com.gacelas.ui.components.FormaBotonC2)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF5E1781), Color(0xFF7B1FA2))))
                        .clickable { if (!e.pausada) modalPausa = true; onPausa() }.testTag("boton-pausa"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (e.pausada) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (e.pausada) "REANUDAR" else "PAUSAR", style = TextStyle(fontFamily = Inter, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 15.sp, fontWeight = FontWeight.Black), color = Color.White)
                }
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(GColorsC2.S2).clickable { modalFin = true }.testTag("boton-stop"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Flag, "Terminar carrera", tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }

        if (bloqueo) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {}.testTag("bloqueo-toque")) {
            Text("Desbloquear", style = TextStyle(fontFamily = num, fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).clip(CircleShape).background(K1.Fondo).clickable { bloqueo = false }.padding(horizontal = 24.dp, vertical = 12.dp))
        }
        // Panel del cronometro.
        AnimatedVisibility(panel, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f))
                .clickable(remember { MutableInteractionSource() }, indication = null) { panel = false }.testTag("scrim-cronometro"))
        }
        AnimatedVisibility(
            panel, modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(320)) { it }, exit = slideOutHorizontally(tween(260)) { it },
        ) {
            PanelCronometro(e, m, bajo, onVuelta, onCerrar = { panel = false }, Modifier.width(maxWidth * 0.85f))
        }
    }
    val resumen: @Composable () -> Unit = {
        com.gacelas.ui.components.ResumenModalC2(listOf(
            "${Unidades.texto(e.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}" to "Distancia",
            e.tiempoTexto to "Tiempo",
            formatearRitmo(Unidades.ritmo(e.ritmoPromedioSegKm, m)) to "Ritmo",
        ))
    }
    if (modalFin) com.gacelas.ui.components.ModalC2(
        icono = Icons.Filled.Flag, titulo = "¿Terminar carrera?",
        texto = "Se guardará tu recorrido y verás el resumen.",
        primario = "Terminar y guardar", onPrimario = { modalFin = false; onStop() }, onCerrar = { modalFin = false },
        secundario = if (onDescartar != null) "Descartar carrera" else "Seguir corriendo",
        onSecundario = { modalFin = false; onDescartar?.invoke() }, tag = "modal-terminar", contenido = resumen,
    )
    else if (modalPausa && e.pausada) com.gacelas.ui.components.ModalC2(
        icono = Icons.Filled.Pause, titulo = "Carrera en pausa",
        texto = "El cronómetro está detenido. Reanuda cuando estés listo.",
        primario = "Reanudar", onPrimario = { modalPausa = false; onPausa() }, onCerrar = { modalPausa = false },
        secundario = "Terminar carrera", onSecundario = { modalPausa = false; modalFin = true }, tag = "modal-pausa", contenido = resumen,
    )
    else if (gpsDebil && !gpsAvisado) com.gacelas.ui.components.ModalC2(
        icono = Icons.Filled.GpsOff, titulo = "GPS sin señal",
        texto = "La precisión es baja (±${e.ultimaPrecisionM?.toInt()} m). Sal a un lugar abierto, lejos de edificios altos.",
        primario = "Entendido", onPrimario = { gpsAvisado = true }, onCerrar = { gpsAvisado = true },
        secundario = "Pausar carrera", onSecundario = { gpsAvisado = true; onPausa() }, tag = "modal-gps",
    )
}

/** Tokens C2 usados por la carrera. */
private object GColorsC2 {
    val Panel = Color(0xF20C1322)
    val Linea = Color(0xFF2C3446)
    val S2 = Color(0xFF232A3A)
    val Muted = Color(0xFFA8B0C2)
    val Violeta = Color(0xFFC084FC)
    val Menta = Color(0xFF68DBA9)
    val Cielo = Color(0xFF96CCFF)
}

private val etiqueta = TextStyle(fontFamily = Inter, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)

@Composable
private fun MetricaLado(titulo: String, valor: String, unidad: String, modifier: Modifier, color: Color = Color.White) {
    Column(modifier) {
        Text(titulo, style = etiqueta.copy(fontWeight = FontWeight.ExtraBold), color = K1.Secondary)
        Text(valor, style = TextStyle(fontFamily = num, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.015.em), color = color, maxLines = 1)
        if (unidad.isNotEmpty()) Text(unidad, style = TextStyle(fontFamily = Inter, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium), color = K1.Secondary)
    }
}

/** Chip C2 de cabecera: paralelogramo oscuro, texto en mayusculas cursiva y punto de estado opcional. */
@Composable
private fun ChipC2(texto: String, modifier: Modifier = Modifier, punto: Color? = null, tag: String) {
    Row(
        modifier.height(32.dp).clip(com.gacelas.ui.components.FormaBotonC2).background(GColorsC2.Panel).border(1.dp, GColorsC2.Linea, com.gacelas.ui.components.FormaBotonC2)
            .padding(horizontal = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (punto != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
            Spacer(Modifier.width(7.dp))
        }
        Text(texto, style = TextStyle(fontFamily = Inter, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.02.em), color = Color.White, maxLines = 1)
    }
}

/** Barras de parciales: una por km (alto segun velocidad), la del km en curso en menta. */
@Composable
private fun BarrasParciales(e: EstadoCarrera) {
    val km = e.parciales.filter { !it.manual }.takeLast(6)
    val ritmos = km.map { it.segundos / it.distanciaKm.coerceAtLeast(0.001) }
    val rapido = ritmos.minOrNull() ?: 1.0
    val lento = ritmos.maxOrNull() ?: 1.0
    Row(Modifier.fillMaxWidth().height(26.dp).testTag("barras-parciales"), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        ritmos.forEach { r ->
            val f = if (lento - rapido < 1.0) 0.75f else (0.5f + 0.5f * ((lento - r) / (lento - rapido)).toFloat())
            Box(Modifier.weight(1f).fillMaxHeight(f).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(GColorsC2.Violeta, Color(0xFF7B1FA2)))))
        }
        Box(Modifier.weight(if (km.isEmpty()) 6f else 0.8f).fillMaxHeight(0.55f).clip(RoundedCornerShape(2.dp)).background(GColorsC2.Menta.copy(alpha = if (km.isEmpty()) 0.25f else 1f)))
    }
}

@Composable
private fun Chip(titulo: String, valor: String, punto: Color? = null, tag: String = "chip-$titulo") {
    Row(
        Modifier.shadow(4.dp, CircleShape).clip(CircleShape).background(K1.Tarjeta.copy(alpha = 0.92f)).border(1.dp, K1.b(0.10f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto != null) {
            Box(Modifier.size(6.dp).shadow(3.dp, CircleShape, ambientColor = punto, spotColor = punto).clip(CircleShape).background(punto))
            Spacer(Modifier.width(6.dp))
        }
        Text(buildAnnotatedString {
            append("$titulo ")
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append(valor) }
        }, style = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium), color = K1.Secondary, maxLines = 1)
    }
}

@Composable
private fun BotonMapa(descripcion: String, onClick: () -> Unit, tag: String, fondo: Color? = null, contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(6.dp)
    Box(Modifier.size(42.dp).vidrio(forma, sombra = 6.dp).then(if (fondo != null) Modifier.background(fondo) else Modifier).clickable(onClickLabel = descripcion, onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        contenido()
    }
}

/** Panel lateral (85% del ancho, radio 28 a la izquierda): cronometro grande, Vuelta y parciales reales. */
@Composable
private fun PanelCronometro(e: EstadoCarrera, m: Boolean, bajo: Boolean, onVuelta: () -> Unit, onCerrar: () -> Unit, modifier: Modifier) {
    val forma = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    var arrastre by remember { mutableStateOf(0f) }
    Box(
        modifier.fillMaxHeight().shadow(24.dp, forma).clip(forma).background(K1.Tarjeta.copy(alpha = 0.97f)).border(1.dp, K1.b(0.10f), forma)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { if (arrastre > 80f) onCerrar(); arrastre = 0f }) { _, d -> arrastre += d }
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .testTag("panel-cronometro"),
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(start = 28.dp).fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(K1.b(0.25f), Color.Transparent))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = K1.Naranja, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("CRONÓMETRO Y PARCIALES", style = TextStyle(fontFamily = num, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.025.em), color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                Box(Modifier.size(32.dp).clip(CircleShape).background(K1.b(0.05f)).border(1.dp, K1.b(0.10f), CircleShape).clickable(onClick = onCerrar).testTag("cerrar-cronometro"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronRight, "Cerrar panel", tint = K1.Secondary, modifier = Modifier.size(20.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(K1.b(0.10f)))
            // Cronometro grande.
            Column(
                Modifier.padding(vertical = if (bajo) 12.dp else 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, K1.b(0.05f), RoundedCornerShape(16.dp)).padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("TIEMPO DE CARRERA", style = etiqueta.copy(letterSpacing = 0.1.em), color = K1.Secondary)
                Spacer(Modifier.height(4.dp))
                val t = e.tiempoTexto
                Text(buildAnnotatedString {
                    append(t.dropLast(2))
                    withStyle(SpanStyle(color = K1.Naranja)) { append(t.takeLast(2)) }
                }, style = TextStyle(fontFamily = num, fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.015.em), color = Color.White, modifier = Modifier.testTag("cronometro-grande"))
                Spacer(Modifier.height(12.dp))
                Text(buildAnnotatedString {
                    append("Ritmo prom: ")
                    withStyle(SpanStyle(color = Color.White, fontFamily = num, fontWeight = FontWeight.SemiBold)) { append(formatearRitmo(Unidades.ritmo(e.ritmoPromedioSegKm, m))) }
                    append("   ·   Dist: ")
                    withStyle(SpanStyle(color = Color.White, fontFamily = num, fontWeight = FontWeight.SemiBold)) { append("${Unidades.texto(e.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}") }
                }, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = K1.Secondary)
            }
            Row(
                Modifier.fillMaxWidth().height(46.dp).shadow(8.dp, CircleShape, spotColor = K1.Naranja.copy(alpha = 0.4f)).clip(CircleShape).background(K1.Naranja)
                    .clickable(onClick = onVuelta).testTag("boton-vuelta"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Flag, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Vuelta", style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
            }
            Spacer(Modifier.height(if (bajo) 14.dp else 24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 8.dp)) {
                val st = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
                Text("PARCIAL / SPLIT", style = st, color = K1.Secondary, modifier = Modifier.weight(1.2f))
                Text("DISTANCIA", style = st, color = K1.Secondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("TIEMPO", style = st, color = K1.Secondary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
            }
            // Sin scroll: se muestran los parciales que caben (los mas recientes primero).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val caben = ((maxHeight.value + 8f) / 50f).toInt().coerceAtLeast(0)
                val lista = e.parciales.reversed()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lista.isEmpty()) Text("Aún no hay parciales. Se agregan solos cada kilómetro o con Vuelta.", style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp), color = K1.Secondary, modifier = Modifier.padding(horizontal = 4.dp))
                    lista.take(caben).forEachIndexed { i, p -> FilaParcial(p, m, actual = i == 0 && p.manual) }
                    if (lista.size > caben && caben > 0) Text("+${lista.size - caben} anteriores", style = TextStyle(fontFamily = Inter, fontSize = 11.sp), color = K1.Secondary.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Row(
                Modifier.clip(CircleShape).background(K1.b(0.04f)).border(1.dp, K1.b(0.10f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val gps = e.ultimaPrecisionM
                Box(Modifier.size(6.dp).clip(CircleShape).background(when { gps == null -> K1.Secondary; gps < 15f -> K1.Verde; else -> K1.Ambar }))
                Spacer(Modifier.width(6.dp))
                Text("GPS ${gps?.let { "±${it.toInt()} m" } ?: "--"}", style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp), color = K1.Secondary)
            }
        }
    }
}

@Composable
private fun FilaParcial(p: Parcial, m: Boolean, actual: Boolean) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(42.dp).clip(forma).background(if (actual) K1.b(0.04f) else Color.Black.copy(alpha = 0.20f))
            .border(1.dp, if (actual) K1.Naranja.copy(alpha = 0.30f) else K1.b(0.05f), forma).padding(horizontal = 12.dp).testTag("parcial"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (p.manual) K1.Naranja else K1.Magenta))
            Spacer(Modifier.width(8.dp))
            Text(if (p.manual) "Vuelta ${p.numero}" else "Km ${p.numero}", style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = if (actual) FontWeight.SemiBold else FontWeight.Medium), color = if (actual) K1.Naranja else Color.White)
        }
        Text("${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}", style = TextStyle(fontFamily = num, fontSize = 12.sp, fontWeight = FontWeight.Medium), color = K1.b(0.9f), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(formatearMs(p.segundos), style = TextStyle(fontFamily = num, fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
    }
}
