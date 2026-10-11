package com.gacelas.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3D
import com.gacelas.ui.three.soporta3D
import com.gacelas.ui.theme.GType
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import io.github.sceneview.math.Position
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.gacelas.BuildConfig
import com.gacelas.data.carreras.PuntoGps
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/** Configuracion de Google Maps y trazados de ejemplo (iguales a la web y a 003_seed_loja.sql). */
object Mapas {
    val CENTRO_LOJA = LatLng(-3.9931, -79.2042)

    fun configurado(clave: String = BuildConfig.GOOGLE_MAPS_API_KEY) = clave.isNotBlank()

    val trazos: Map<String, List<LatLng>> = mapOf(
        "1" to listOf(LatLng(-3.9746, -79.2017), LatLng(-3.9712, -79.2001), LatLng(-3.969, -79.203), LatLng(-3.9718, -79.2058), LatLng(-3.9746, -79.2017)),
        "2" to listOf(LatLng(-3.9931, -79.2005), LatLng(-3.999, -79.1985), LatLng(-4.006, -79.197), LatLng(-4.0125, -79.195)),
        "3" to listOf(LatLng(-3.995, -79.225), LatLng(-3.999, -79.24), LatLng(-4.001, -79.255), LatLng(-4.0002, -79.264)),
        "primary" to listOf(LatLng(-3.9746, -79.2017), LatLng(-3.9845, -79.2035), LatLng(-3.9931, -79.2005), LatLng(-4.0045, -79.1958)),
        "alt" to listOf(LatLng(-3.9746, -79.2017), LatLng(-3.988, -79.2075), LatLng(-3.999, -79.201)),
    )

    fun trazo(id: String?): List<LatLng> = id?.removePrefix("ruta-")?.let { trazos[it] } ?: emptyList()

    /** Estilo Gacelas (como la web): azul marino/carbon, vias apagadas, sin POIs ni transporte. */
    const val ESTILO_GACELAS = """[
      {"elementType":"geometry","stylers":[{"color":"#0d1422"}]},
      {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
      {"elementType":"labels.text.fill","stylers":[{"color":"#6b7a90"}]},
      {"elementType":"labels.text.stroke","stylers":[{"color":"#0d1422"},{"weight":3}]},
      {"featureType":"administrative","elementType":"geometry","stylers":[{"visibility":"off"}]},
      {"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#9aa6b8"}]},
      {"featureType":"administrative.neighborhood","elementType":"labels","stylers":[{"visibility":"off"}]},
      {"featureType":"poi","stylers":[{"visibility":"off"}]},
      {"featureType":"poi.park","stylers":[{"visibility":"on"}]},
      {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#0f2a24"}]},
      {"featureType":"poi.park","elementType":"labels","stylers":[{"visibility":"off"}]},
      {"featureType":"landscape.man_made","elementType":"geometry","stylers":[{"color":"#111a2b"}]},
      {"featureType":"landscape.natural","elementType":"geometry","stylers":[{"color":"#0e1726"}]},
      {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#1c2638"}]},
      {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#0d1422"}]},
      {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#5d6b80"}]},
      {"featureType":"road.highway","elementType":"geometry.fill","stylers":[{"color":"#2a3446"}]},
      {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#3a2a1e"}]},
      {"featureType":"road.local","elementType":"labels","stylers":[{"visibility":"off"}]},
      {"featureType":"transit","stylers":[{"visibility":"off"}]},
      {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0a2340"}]},
      {"featureType":"water","elementType":"labels","stylers":[{"visibility":"off"}]}
    ]"""

    /** Alias: todas las pantallas usan ya el estilo Gacelas. */
    const val ESTILO_OSCURO = ESTILO_GACELAS

    /** Estilo oscuro anterior de docs/DESIGN.md, se conserva como referencia. */
    const val ESTILO_OSCURO_V1 = """[
      {"elementType":"geometry","stylers":[{"color":"#0b1220"}]},
      {"elementType":"labels.text.fill","stylers":[{"color":"#94a3b8"}]},
      {"elementType":"labels.text.stroke","stylers":[{"color":"#0b1220"}]},
      {"featureType":"road","elementType":"geometry","stylers":[{"color":"#374151"}]},
      {"featureType":"poi","elementType":"labels","stylers":[{"visibility":"off"}]},
      {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#064E3B"}]},
      {"featureType":"water","elementType":"geometry","stylers":[{"color":"#1E3A8A"}]},
      {"featureType":"transit","stylers":[{"visibility":"off"}]}
    ]"""

    const val ESTILO_CLARO = """[
      {"featureType":"road","elementType":"geometry","stylers":[{"color":"#E5E7EB"}]},
      {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#D1FAE5"}]},
      {"featureType":"water","elementType":"geometry","stylers":[{"color":"#BFDBFE"}]},
      {"featureType":"poi","elementType":"labels","stylers":[{"visibility":"off"}]}
    ]"""
}

fun List<PuntoGps>.aLatLng() = map { LatLng(it.lat, it.lng) }

/**
 * Mapa de Google estilo Gacelas: trazo con contorno y brillo, pines de inicio/meta, "estas aqui"
 * con halo y controles propios. Con [seguir] la camara sigue al corredor; con [modo3d] se inclina
 * con el rumbo hacia arriba y el corredor 3D se dibuja encima, anclado a su posicion proyectada.
 * Sin GOOGLE_MAPS_API_KEY en local.properties se muestra [alternativa].
 */
@Composable
fun MapaGoogle(
    puntos: List<LatLng>,
    modifier: Modifier = Modifier,
    seguir: Boolean = false,
    oscuro: Boolean = true,
    zoom: Float = 15f,
    modo3d: Boolean = false,
    ritmoSegKm: Int? = null,
    plan: List<LatLng> = emptyList(),
    adelanteM: Double = 120.0,
    controles: Boolean = true,
    alternativa: @Composable () -> Unit,
) {
    if (!Mapas.configurado()) {
        alternativa()
        return
    }
    val context = LocalContext.current
    val con3d = modo3d && seguir && !LocalInspectionMode.current && remember { soporta3D(context) }
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(puntos.firstOrNull() ?: plan.firstOrNull() ?: Mapas.CENTRO_LOJA, zoom) }
    var siguiendo by remember { mutableStateOf(true) }
    val rumbo = remember(puntos.size) { MapaMat.rumboReciente(puntos) }
    LaunchedEffect(camara.isMoving, camara.cameraMoveStartedReason) {
        if (camara.isMoving && camara.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) siguiendo = false
    }
    LaunchedEffect(puntos.size, puntos.lastOrNull(), modo3d, siguiendo) {
        val ultimo = puntos.lastOrNull()
        runCatching {
            when {
                ultimo == null -> if (plan.size >= 2) camara.animate(CameraUpdateFactory.newLatLngBounds(MapaMat.limites(plan), 80))
                seguir && !siguiendo -> Unit
                seguir && modo3d -> {
                    val r = rumbo ?: camara.position.bearing
                    val z = maxOf(camara.position.zoom, 17.5f)
                    val centro = MapaMat.desplazar(ultimo, r.toDouble(), adelanteM * Math.pow(2.0, 18.0 - z))
                    camara.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(centro, z, 60f, r)), 900)
                }
                seguir || puntos.size < 2 -> camara.animate(CameraUpdateFactory.newLatLngZoom(ultimo, zoom), 900)
                else -> camara.animate(CameraUpdateFactory.newLatLngBounds(MapaMat.limites(puntos), 80))
            }
        }
    }
    val naranja = Color(0xFFFF6A13)
    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize().testTag("mapa-google"),
            cameraPositionState = camara,
            properties = MapProperties(mapStyleOptions = MapStyleOptions(if (oscuro) Mapas.ESTILO_GACELAS else Mapas.ESTILO_CLARO), isBuildingEnabled = modo3d),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false, compassEnabled = false, tiltGesturesEnabled = true),
        ) {
            if (plan.size >= 2) {
                Polyline(points = plan, color = naranja.copy(alpha = 0.22f), width = 60f)
                Polyline(points = plan, color = Color(0xFFFFD2B3).copy(alpha = 0.55f), width = 16f)
            }
            if (puntos.size >= 2) {
                Polyline(points = puntos, color = naranja.copy(alpha = 0.18f), width = 40f)
                Polyline(points = puntos, color = Color(0xFF05080F), width = 24f)
                Polyline(points = puntos, color = naranja, width = 13f)
                if (!seguir) {
                    Circle(center = puntos.first(), radius = 9.0, fillColor = Color(0xFF10B981), strokeColor = Color.White, strokeWidth = 6f, zIndex = 3f)
                    Circle(center = puntos.last(), radius = 9.0, fillColor = Color(0xFF111827), strokeColor = Color.White, strokeWidth = 6f, zIndex = 3f)
                }
            }
            if (seguir && !con3d) puntos.lastOrNull()?.let {
                Circle(center = it, radius = 22.0, fillColor = naranja.copy(alpha = 0.18f), strokeColor = naranja.copy(alpha = 0.35f), strokeWidth = 2f, zIndex = 4f)
                Circle(center = it, radius = 6.0, fillColor = naranja, strokeColor = Color.White, strokeWidth = 7f, zIndex = 5f)
            }
        }
        if (con3d) puntos.lastOrNull()?.let { ultimo ->
            // Corredor 3D anclado al punto proyectado del usuario (la camara gira con el rumbo).
            val proy = camara.projection
            val px = remember(camara.position, proy, ultimo) { proy?.toScreenLocation(ultimo) }
            val densidad = LocalDensity.current
            if (px != null) {
                val lado = 190.dp
                val ladoPx = with(densidad) { lado.toPx() }
                Box(Modifier.offset { IntOffset((px.x - ladoPx / 2).toInt(), (px.y - ladoPx * 0.86f).toInt()) }.size(lado).testTag("corredor-3d-mapa")) {
                    Runner3D(MapaMat.clipPorRitmo(ritmoSegKm), camara = Position(0f, 1.1f, -3.6f))
                }
            }
        }
        if (controles) ControlesMapa(camara, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        if (seguir && !siguiendo) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(CircleShape).background(Color(0xF2121A2A))
                    .clickable { siguiendo = true }.padding(horizontal = 16.dp, vertical = 10.dp).testTag("recentrar"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, tint = naranja)
                Text("  Recentrar", color = Color.White, style = GType.labelLg)
            }
        }
    }
}

/** Zoom y brujula propios (estilo Gacelas). */
@Composable
private fun ControlesMapa(camara: CameraPositionState, modifier: Modifier) {
    val alcance = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xE6121A2A))) {
            IconButton(onClick = { alcance.launch { runCatching { camara.animate(CameraUpdateFactory.zoomIn()) } } }) { Icon(Icons.Filled.Add, "Acercar", tint = Color.White) }
            IconButton(onClick = { alcance.launch { runCatching { camara.animate(CameraUpdateFactory.zoomOut()) } } }) { Icon(Icons.Filled.Remove, "Alejar", tint = Color.White) }
        }
        IconButton(
            onClick = { alcance.launch { runCatching { camara.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(camara.position.target, camara.position.zoom, 0f, 0f))) } } },
            modifier = Modifier.clip(CircleShape).background(Color(0xE6121A2A)),
        ) { Icon(Icons.Filled.Navigation, "Orientar al norte", tint = Color(0xFFFF6A13), modifier = Modifier.rotate(-camara.position.bearing)) }
    }
}

/** Matematicas del mapa (compartidas con las pruebas). */
object MapaMat {
    private fun rad(g: Double) = Math.toRadians(g)
    fun rumbo(a: LatLng, b: LatLng): Float {
        val f1 = rad(a.latitude); val f2 = rad(b.latitude); val dl = rad(b.longitude - a.longitude)
        val y = sin(dl) * cos(f2); val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }
    fun distanciaM(a: LatLng, b: LatLng): Double {
        val dLat = rad(b.latitude - a.latitude); val dLng = rad(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(rad(a.latitude)) * cos(rad(b.latitude)) * sin(dLng / 2).pow(2)
        return 2 * 6371000 * asin(sqrt(h))
    }
    fun desplazar(p: LatLng, rumboGrados: Double, metros: Double): LatLng {
        val d = metros / 6371000; val r = rad(rumboGrados); val f1 = rad(p.latitude); val l1 = rad(p.longitude)
        val f2 = asin(sin(f1) * cos(d) + cos(f1) * sin(d) * cos(r))
        val l2 = l1 + atan2(sin(r) * sin(d) * cos(f1), cos(d) - sin(f1) * sin(f2))
        return LatLng(Math.toDegrees(f2), Math.toDegrees(l2))
    }
    fun rumboReciente(p: List<LatLng>, metros: Double = 15.0): Float? {
        if (p.size < 2) return null
        val fin = p.last()
        for (i in p.size - 2 downTo 0) if (distanciaM(p[i], fin) >= metros || i == 0) return rumbo(p[i], fin)
        return null
    }
    fun clipPorRitmo(segKm: Int?): ClipCorredor = when {
        segKm == null || segKm <= 0 || segKm > 900 -> ClipCorredor.Idle
        segKm > 360 -> ClipCorredor.Jog
        else -> ClipCorredor.Run
    }
    fun limites(p: List<LatLng>): LatLngBounds = LatLngBounds.builder().apply { p.forEach { include(it) } }.build()
}
