package com.gacelas.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.gacelas.mapa.Coord
import com.gacelas.mapa.EncuadreMapa
import com.gacelas.mapa.PaddingPx
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * Encuadre de rutas del mapa: helper unico de Android (lista de rutas, detalle de ruta y de carrera,
 * eventos, seguridad, mapa en vivo). Anima la camara hasta mostrar TODA la ruta dentro de la franja
 * libre (vista menos [padding]); con un solo punto centra con [zoomUnPunto]. Si el mapa aun no tiene
 * tamano espera hasta ~2 s. Devuelve false sin puntos validos.
 *
 * Las cuentas viven en [EncuadreMapa] (comunes, con pruebas). Con las animaciones del sistema
 * apagadas (escala 0) la camara salta sin animar.
 */
suspend fun MapLibreMap.encuadrarRuta(
    contexto: Context,
    puntos: List<LatLng>,
    padding: PaddingPx,
    duracionMs: Int = EncuadreMapa.DURACION_MS,
    zoomMaximo: Double = EncuadreMapa.ZOOM_MAXIMO,
    zoomUnPunto: Double = EncuadreMapa.ZOOM_UN_PUNTO,
    inclinacion: Double = 0.0,
): Boolean {
    // El mapa puede estar listo antes de medirse: se espera (hasta ~2 s) a que tenga tamano.
    var intentos = 0
    while ((width <= 0f || height <= 0f) && intentos++ < 40) kotlinx.coroutines.delay(50)
    val camara = EncuadreMapa.camaraParaPuntos(
        puntos.map { Coord(it.latitude, it.longitude) }, width.toFloat(), height.toFloat(), padding,
        zoomMaximo = zoomMaximo, zoomUnPunto = zoomUnPunto,
    ) ?: return false
    val destino = CameraPosition.Builder().target(LatLng(camara.centro.lat, camara.centro.lng)).zoom(camara.zoom).bearing(0.0).tilt(inclinacion).build()
    val ms = if (animacionesApagadas(contexto)) 0 else duracionMs
    if (ms == 0) moveCamera(CameraUpdateFactory.newCameraPosition(destino))
    else animateCamera(CameraUpdateFactory.newCameraPosition(destino), ms)
    return true
}

private fun animacionesApagadas(contexto: Context) =
    Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Efecto de Compose: cada vez que cambia [clave] (por defecto, la ruta) encuadra [puntos] con
 * [padding]. Con [clave] explicita, solo ella decide cuando volver a encuadrar (ideal para
 * posiciones en vivo que no deben mover la camara). [padding] se lee al encuadrar.
 */
@Composable
fun EncuadrarRutaEfecto(
    mapa: MapLibreMap?,
    puntos: List<LatLng>,
    padding: PaddingPx,
    clave: Any? = puntos,
    activo: Boolean = true,
    duracionMs: Int = EncuadreMapa.DURACION_MS,
    zoomUnPunto: Double = EncuadreMapa.ZOOM_UN_PUNTO,
) {
    val contexto = LocalContext.current
    val puntosActuales by rememberUpdatedState(puntos)
    val paddingActual by rememberUpdatedState(padding)
    LaunchedEffect(mapa, clave, activo) {
        val m = mapa ?: return@LaunchedEffect
        if (!activo || puntosActuales.isEmpty()) return@LaunchedEffect
        runCatching { m.encuadrarRuta(contexto, puntosActuales, paddingActual, duracionMs, zoomUnPunto = zoomUnPunto) }
    }
}
