@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.gacelas.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import com.gacelas.core.sensores.Rumbo
import com.gacelas.core.sensores.fuenteRumboNativa
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay
import platform.UIKit.UIView
import kotlin.math.pow

private const val NARANJA = 0xFFFF6A13
private const val MAGENTA = 0xFFD6338A
private fun conAlfa(argb: Long, a: Float) = ((a * 255).toLong().coerceIn(0, 255) shl 24) or (argb and 0xFFFFFF)

fun List<PuntoGps>.aCoords() = map { Coord(it.lat, it.lng) }

/** Vista MapLibre (Swift) con trazos y circulos GeoJSON; mismo papel que MapaLibre en Android. */
@Composable
fun MapaLibreIos(
    modifier: Modifier,
    trazos: List<TrazoComun> = emptyList(),
    circulos: List<CirculoComun> = emptyList(),
    oscuro: Boolean = true,
    centro: Coord = MapaGacelasConst.CENTRO_LOJA,
    zoom: Double = 14.0,
    alTocarCirculo: ((String) -> Unit)? = null,
    alTocar: ((Coord) -> Unit)? = null,
    alGesto: () -> Unit = {},
    alMoverCamara: (Double) -> Unit = {},
    alListo: (UIView) -> Unit = {},
) {
    val puente = MapaIos.puente ?: return
    val gesto by rememberUpdatedState(alGesto)
    val mover by rememberUpdatedState(alMoverCamara)
    val tocar by rememberUpdatedState(alTocarCirculo)
    val tocarMapa by rememberUpdatedState(alTocar)
    val oyente = remember {
        object : OyenteMapa {
            override fun gesto() = gesto()
            override fun camara(rumbo: Double) = mover(rumbo)
            override fun tocoCirculo(id: String) { tocar?.invoke(id) }
            override fun tocoMapa(lat: Double, lng: Double) { tocarMapa?.invoke(Coord(lat, lng)) }
        }
    }
    // Sin red se usa el estilo con las teselas fijas de la region descargada (como Android).
    val vista = remember(oscuro) {
        val sinRed = if (MapaIos.offline?.conRed() == false) MapaOfflineIos.estiloSinRed(oscuro) else null
        puente.crear(oscuro, centro.lat, centro.lng, zoom, sinRed, oyente)
    }
    LaunchedEffect(vista) { alListo(vista) }
    LaunchedEffect(vista, trazos) { puente.trazos(vista, geojsonTrazos(trazos)) }
    LaunchedEffect(vista, circulos) { puente.circulos(vista, geojsonCirculos(circulos)) }
    UIKitView(factory = { vista }, modifier = modifier)
}

/**
 * Mapa estilo Gacelas en iOS: misma logica que MapaGacelas (Android): trazo con contorno y brillo, pines
 * de inicio y meta, "estas aqui" con halo, seguimiento centrado, rumbo (sensor + GPS con Rumbo.combinar)
 * y vista de navegacion inclinada con [modo3d]. El corredor 3D (SceneView) no existe en iOS: se usa el
 * mismo halo que Android cuando el telefono no soporta 3D. Sin puente MapLibre se muestra [alternativa].
 */
@Composable
fun MapaGacelasIos(
    puntos: List<Coord>,
    modifier: Modifier = Modifier,
    seguir: Boolean = false,
    oscuro: Boolean = true,
    zoom: Float = 15f,
    modo3d: Boolean = false,
    ritmoSegKm: Int? = null,
    plan: List<Coord> = emptyList(),
    adelanteM: Double = 120.0,
    controles: Boolean = true,
    rutaMagenta: Boolean = false,
    avisoArriba: Dp = 8.dp,
    controlesPropios: (@Composable BoxScope.(recentrar: () -> Unit, norte: () -> Unit, rumbo: Float, siguiendo: Boolean) -> Unit)? = null,
    alternativa: @Composable () -> Unit,
) {
    val puente = MapaIos.puente
    if (puente == null) { alternativa(); return }
    var vista by remember { mutableStateOf<UIView?>(null) }
    var siguiendo by remember { mutableStateOf(true) }
    var gestos by remember { mutableIntStateOf(0) }
    LaunchedEffect(gestos) { if (gestos > 0) { delay(MapaGacelasConst.SEGUIR_DE_NUEVO_MS); siguiendo = true } }
    var rumboCamara by remember { mutableFloatStateOf(0f) }
    val ultimo = puntos.lastOrNull()
    val velocidadMs = ritmoSegKm?.takeIf { it in 1..1800 }?.let { 1000.0 / it } ?: 0.0
    val rumboGps = remember(puntos.size) { MapaMatComun.rumboReciente(puntos) }
    var rumboSensor by remember { mutableStateOf<Float?>(null) }
    val sensorActivo = seguir && modo3d
    DisposableEffect(sensorActivo) {
        val f = if (sensorActivo) fuenteRumboNativa() else null
        f?.iniciar { rumboSensor = it } ?: run { rumboSensor = null }
        onDispose { f?.detener() }
    }
    val rumboCombinado = Rumbo.combinar(rumboSensor, rumboGps, velocidadMs)
    var rumbo by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(rumboCombinado) { if (Rumbo.cambioRelevante(rumbo, rumboCombinado)) rumbo = rumboCombinado }
    var ultimoAnimado by remember { mutableStateOf<Coord?>(null) }
    var altoPx by remember { mutableIntStateOf(0) }
    val densidad = LocalDensity.current
    val compacto = altoPx > 0 && with(densidad) { altoPx.toDp() } < MapaGacelasConst.ALTO_COMPACTO_DP.dp

    LaunchedEffect(vista, puntos.size, ultimo, modo3d, siguiendo, rumbo, compacto) {
        val v = vista ?: return@LaunchedEffect
        fun encuadrar(p: List<Coord>) = puente.encuadrar(v, p.minOf { it.lat }, p.minOf { it.lng }, p.maxOf { it.lat }, p.maxOf { it.lng }, 80.0 / densidad.density)
        when {
            ultimo == null -> if (plan.size >= 2) encuadrar(plan)
            seguir && !siguiendo -> Unit
            seguir && modo3d -> {
                val r = (rumbo ?: rumboCamara).toDouble()
                val z = MapaMatComun.zoomNavegacion(velocidadMs)
                val centro = if (compacto) ultimo else MapaMatComun.desplazar(ultimo, r, adelanteM * 2.0.pow(18.0 - z))
                // MapLibre iOS trabaja en puntos: el padding va en dp como el alto de la vista.
                val arriba = if (compacto) altoPx / densidad.density * MapaGacelasConst.PADDING_ARRIBA else 0.0
                val nuevoPunto = ultimo != ultimoAnimado
                ultimoAnimado = ultimo
                puente.camara(v, centro.lat, centro.lng, z, r, 60.0, arriba, if (nuevoPunto) MapaGacelasConst.ANIMACION_PUNTO_MS else 500, nuevoPunto)
            }
            seguir || puntos.size < 2 -> puente.camara(v, ultimo.lat, ultimo.lng, zoom.toDouble(), 0.0, 0.0, 0.0, 900, false)
            else -> encuadrar(puntos)
        }
    }

    val trazos = buildList {
        if (plan.size >= 2) {
            add(TrazoComun(plan, NARANJA, 22f, 0.22f))
            add(TrazoComun(plan, 0xFFFFD2B3, 6f, 0.55f))
        }
        if (puntos.size >= 2 && rutaMagenta) {
            add(TrazoComun(puntos, MAGENTA, 16f, 0.25f))
            add(TrazoComun(puntos, MAGENTA, 5f))
        } else if (puntos.size >= 2) {
            add(TrazoComun(puntos, NARANJA, 14f, 0.18f))
            add(TrazoComun(puntos, 0xFF05080F, 9f))
            add(TrazoComun(puntos, NARANJA, 5f))
        }
    }
    val circulos = buildList {
        if (puntos.size >= 2 && !seguir) {
            add(CirculoComun(puntos.first(), 7f, 0xFF10B981, bordeDp = 2.5f))
            add(CirculoComun(puntos.last(), 7f, 0xFF111827, bordeDp = 2.5f))
        }
        if (seguir && ultimo != null) {
            add(CirculoComun(ultimo, 16f, conAlfa(NARANJA, 0.18f), conAlfa(NARANJA, 0.35f), 1f))
            add(CirculoComun(ultimo, 6f, NARANJA, bordeDp = 2.5f))
        }
    }

    Box(modifier.onSizeChanged { altoPx = it.height }) {
        MapaLibreIos(
            Modifier.fillMaxSize(),
            trazos = trazos, circulos = circulos, oscuro = oscuro,
            centro = ultimo ?: plan.firstOrNull() ?: MapaGacelasConst.CENTRO_LOJA,
            zoom = zoom.toDouble(),
            alGesto = { if (seguir) { siguiendo = false; gestos++ } },
            alMoverCamara = { rumboCamara = it.toFloat() },
            alListo = { vista = it },
        )
        if (controlesPropios != null) controlesPropios(
            { siguiendo = true; gestos = 0 },
            { vista?.let { puente.norte(it, plano = false) } },
            rumboCamara,
            siguiendo,
        ) else if (controles) ControlesMapaIos(vista, rumboCamara, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        val descarga by MapaOfflineIos.estado.collectAsState()
        val conRed = remember(puntos.size / 10) { MapaIos.offline?.conRed() != false }
        if (!conRed && descarga !is EstadoMapaOffline.Lista) {
            Text(
                "Sin conexión · descarga el mapa de Loja en Ajustes",
                color = Color.White, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = avisoArriba).clip(CircleShape).background(Color(0xE6121A2A))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (seguir && !siguiendo && controlesPropios == null) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(CircleShape).background(Color(0xF2121A2A))
                    .clickable { siguiendo = true }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, tint = Color(NARANJA))
                Text("  Recentrar", color = Color.White, style = GType.labelLg)
            }
        }
    }
}

/** Zoom y brujula propios (mismos que ControlesMapa en Android). */
@Composable
private fun ControlesMapaIos(vista: UIView?, rumbo: Float, modifier: Modifier) {
    val puente = MapaIos.puente ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xE6121A2A))) {
            IconButton(onClick = { vista?.let { puente.zoom(it, 1.0) } }) { Icon(Icons.Filled.Add, "Acercar", tint = Color.White) }
            IconButton(onClick = { vista?.let { puente.zoom(it, -1.0) } }) { Icon(Icons.Filled.Remove, "Alejar", tint = Color.White) }
        }
        IconButton(
            onClick = { vista?.let { puente.norte(it, plano = true) } },
            modifier = Modifier.clip(CircleShape).background(Color(0xE6121A2A)),
        ) { Icon(Icons.Filled.Navigation, "Orientar al norte", tint = Color(NARANJA), modifier = Modifier.rotate(-rumbo)) }
    }
}

/** Salidas de las rutas como circulos tocables (MapaRutasAgrupadas en Android). */
@Composable
fun MapaRutasAgrupadasIos(rutas: List<com.gacelas.rutas.domain.modelo.Ruta>, onRuta: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!MapaIos.disponible()) {
        Text("El mapa no está disponible en este dispositivo.", style = GType.bodyMd, color = com.gacelas.ui.theme.GColors.TextSecondary)
        return
    }
    val salidas = remember(rutas) {
        rutas.filter { it.puntos.isNotEmpty() }.map { CirculoComun(Coord(it.puntos.first().first, it.puntos.first().second), 9f, NARANJA, bordeDp = 2.5f, id = it.id) }
    }
    MapaLibreIos(modifier, circulos = salidas, zoom = 12.0, alTocarCirculo = onRuta)
}
