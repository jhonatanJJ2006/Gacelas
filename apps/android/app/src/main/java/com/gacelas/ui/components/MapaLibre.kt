package com.gacelas.ui.components

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Linea dibujada sobre el mapa (ancho en dp). */
data class TrazoMapa(val puntos: List<LatLng>, val color: Color, val anchoDp: Float, val opacidad: Float = 1f)

/** Circulo de tamano fijo en pantalla (radio en dp). Con [id] se puede tocar. */
data class CirculoMapa(val centro: LatLng, val radioDp: Float, val relleno: Color, val borde: Color = Color.White, val bordeDp: Float = 2f, val id: String? = null)

private const val FUENTE_TRAZOS = "gacelas-trazos"
private const val FUENTE_CIRCULOS = "gacelas-circulos"

private fun Color.hex() = String.format("#%06X", 0xFFFFFF and toArgb())

private fun coleccionTrazos(t: List<TrazoMapa>) = FeatureCollection.fromFeatures(t.filter { it.puntos.size >= 2 }.map { tr ->
    Feature.fromGeometry(LineString.fromLngLats(tr.puntos.map { Point.fromLngLat(it.longitude, it.latitude) })).apply {
        addStringProperty("color", tr.color.hex()); addNumberProperty("ancho", tr.anchoDp); addNumberProperty("op", tr.opacidad * tr.color.alpha)
    }
})

private fun coleccionCirculos(c: List<CirculoMapa>) = FeatureCollection.fromFeatures(c.map { ci ->
    Feature.fromGeometry(Point.fromLngLat(ci.centro.longitude, ci.centro.latitude)).apply {
        addStringProperty("color", ci.relleno.hex()); addNumberProperty("r", ci.radioDp); addNumberProperty("op", ci.relleno.alpha)
        addStringProperty("borde", ci.borde.hex()); addNumberProperty("bw", ci.bordeDp); ci.id?.let { addStringProperty("id", it) }
    }
})

/**
 * Mapa MapLibre con el estilo Gacelas (teselas vectoriales de OpenStreetMap). Las teselas que
 * se descargan con [MapaOffline] se usan sin red. Dibuja [trazos] y [circulos] como capas GeoJSON.
 */
@Composable
fun MapaLibre(
    modifier: Modifier,
    trazos: List<TrazoMapa> = emptyList(),
    circulos: List<CirculoMapa> = emptyList(),
    oscuro: Boolean = true,
    centro: LatLng = Mapas.CENTRO_LOJA,
    zoom: Double = 14.0,
    alTocar: ((LatLng) -> Unit)? = null,
    alTocarCirculo: ((String) -> Unit)? = null,
    alGesto: () -> Unit = {},
    alMoverCamara: (MapLibreMap) -> Unit = {},
    alListo: (MapLibreMap) -> Unit = {},
) {
    val context = LocalContext.current
    val vista = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estilo by remember { mutableStateOf<Style?>(null) }
    val tocar by rememberUpdatedState(alTocar)
    val tocarCirculo by rememberUpdatedState(alTocarCirculo)
    val gesto by rememberUpdatedState(alGesto)
    val mover by rememberUpdatedState(alMoverCamara)
    val listo by rememberUpdatedState(alListo)
    val uriEstilo = if (oscuro) Mapas.ESTILO_OSCURO else Mapas.ESTILO_CLARO

    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> vista.onStart()
                Lifecycle.Event.ON_RESUME -> vista.onResume()
                Lifecycle.Event.ON_PAUSE -> vista.onPause()
                Lifecycle.Event.ON_STOP -> vista.onStop()
                else -> Unit
            }
        }
        ciclo.addObserver(obs)
        onDispose {
            ciclo.removeObserver(obs)
            vista.onPause(); vista.onStop(); vista.onDestroy()
        }
    }

    LaunchedEffect(vista) {
        vista.getMapAsync { m ->
            m.uiSettings.isCompassEnabled = false
            m.uiSettings.isLogoEnabled = false
            m.uiSettings.isRotateGesturesEnabled = true
            m.cameraPosition = CameraPosition.Builder().target(centro).zoom(zoom).build()
            m.addOnCameraMoveStartedListener { r -> if (r == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) gesto() }
            m.addOnCameraMoveListener { mover(m) }
            m.addOnCameraIdleListener { mover(m) }
            m.addOnMapClickListener { p ->
                val px: PointF = m.projection.toScreenLocation(p)
                val id = m.queryRenderedFeatures(px, FUENTE_CIRCULOS).firstNotNullOfOrNull { f -> f.getStringProperty("id") }
                when {
                    id != null && tocarCirculo != null -> { tocarCirculo?.invoke(id); true }
                    tocar != null -> { tocar?.invoke(p); true }
                    else -> false
                }
            }
            mapa = m
        }
    }

    LaunchedEffect(mapa, uriEstilo) {
        val m = mapa ?: return@LaunchedEffect
        estilo = null
        // Sin red se usa el estilo con las teselas fijas de la region descargada (ver MapaOffline.estiloSinRed).
        val sinRed = if (!ReglasDescarga.conRed(context)) MapaOffline.estiloSinRed(context, uriEstilo.removePrefix("asset://")) else null
        m.setStyle(if (sinRed != null) Style.Builder().fromJson(sinRed) else Style.Builder().fromUri(uriEstilo)) { s ->
            s.addSource(GeoJsonSource(FUENTE_TRAZOS))
            s.addSource(GeoJsonSource(FUENTE_CIRCULOS))
            s.addLayer(LineLayer(FUENTE_TRAZOS, FUENTE_TRAZOS).withProperties(
                PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
                PropertyFactory.lineWidth(Expression.get("ancho")),
                PropertyFactory.lineOpacity(Expression.get("op")),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ))
            s.addLayer(CircleLayer(FUENTE_CIRCULOS, FUENTE_CIRCULOS).withProperties(
                PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
                PropertyFactory.circleRadius(Expression.get("r")),
                PropertyFactory.circleOpacity(Expression.get("op")),
                PropertyFactory.circleStrokeColor(Expression.toColor(Expression.get("borde"))),
                PropertyFactory.circleStrokeWidth(Expression.get("bw")),
            ))
            estilo = s
            listo(m)
        }
    }

    LaunchedEffect(estilo, trazos) { estilo?.getSourceAs<GeoJsonSource>(FUENTE_TRAZOS)?.setGeoJson(coleccionTrazos(trazos)) }
    LaunchedEffect(estilo, circulos) { estilo?.getSourceAs<GeoJsonSource>(FUENTE_CIRCULOS)?.setGeoJson(coleccionCirculos(circulos)) }

    AndroidView(factory = { vista }, modifier = modifier)
}
