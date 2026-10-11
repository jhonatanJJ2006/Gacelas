package com.gacelas.ui.components

import androidx.compose.runtime.Composable
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

    /** Estilo oscuro de docs/DESIGN.md (carreteras #374151, parques #064E3B, agua #1E3A8A). */
    const val ESTILO_OSCURO = """[
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
 * Mapa de Google (Maps Compose) con el trazado en naranja. Sin GOOGLE_MAPS_API_KEY en
 * local.properties se muestra [alternativa] (el mapa dibujado de siempre).
 */
@Composable
fun MapaGoogle(
    puntos: List<LatLng>,
    modifier: Modifier = Modifier,
    seguir: Boolean = false,
    oscuro: Boolean = true,
    zoom: Float = 15f,
    alternativa: @Composable () -> Unit,
) {
    if (!Mapas.configurado()) {
        alternativa()
        return
    }
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(puntos.firstOrNull() ?: Mapas.CENTRO_LOJA, zoom) }
    LaunchedEffect(puntos.size, puntos.lastOrNull()) {
        val ultimo = puntos.lastOrNull() ?: return@LaunchedEffect
        runCatching {
            if (seguir || puntos.size < 2) camara.animate(CameraUpdateFactory.newLatLngZoom(ultimo, zoom))
            else camara.animate(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply { puntos.forEach { include(it) } }.build(), 80))
        }
    }
    GoogleMap(
        modifier = modifier.testTag("mapa-google"),
        cameraPositionState = camara,
        properties = MapProperties(mapStyleOptions = MapStyleOptions(if (oscuro) Mapas.ESTILO_OSCURO else Mapas.ESTILO_CLARO)),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
    ) {
        if (puntos.size >= 2) Polyline(points = puntos, color = if (oscuro) Color(0xFFF59E0B) else Color(0xFFD97706), width = 12f)
        puntos.lastOrNull()?.let { Circle(center = it, radius = 6.0, fillColor = Color(0xFF10B981), strokeColor = Color.White, strokeWidth = 4f) }
    }
}
