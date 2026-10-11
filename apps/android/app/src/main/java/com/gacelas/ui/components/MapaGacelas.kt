package com.gacelas.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GType
import com.gacelas.ui.three.Runner3D
import com.gacelas.ui.three.soporta3D
import io.github.sceneview.math.Position
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

private val NARANJA = Color(0xFFFF6A13)

/**
 * Mapa estilo Gacelas (MapLibre + OpenStreetMap, funciona sin red en las zonas descargadas):
 * trazo con contorno y brillo, pines de inicio y meta, "estas aqui" con halo y controles propios.
 * Con [seguir] la camara sigue al corredor; con [modo3d] se inclina con el rumbo hacia arriba,
 * acerca el zoom segun la velocidad (ver [ZoomNavegacion]) y dibuja el corredor 3D encima.
 * En pruebas JVM se muestra [alternativa].
 */
@Composable
fun MapaGacelas(
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
    if (!Mapas.configurado() || LocalInspectionMode.current) {
        alternativa()
        return
    }
    val context = LocalContext.current
    val con3d = modo3d && seguir && remember { soporta3D(context) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var siguiendo by remember { mutableStateOf(true) }
    var rumboCamara by remember { mutableFloatStateOf(0f) }
    var pantalla by remember { mutableStateOf<Offset?>(null) }
    val ultimo = puntos.lastOrNull()
    val ultimoActual by rememberUpdatedState(ultimo)
    val rumbo = remember(puntos.size) { MapaMat.rumboReciente(puntos) }
    val velocidadMs = ritmoSegKm?.takeIf { it in 1..1800 }?.let { 1000.0 / it } ?: 0.0

    LaunchedEffect(mapa, puntos.size, ultimo, modo3d, siguiendo) {
        val m = mapa ?: return@LaunchedEffect
        runCatching {
            when {
                ultimo == null -> if (plan.size >= 2) m.animateCamera(CameraUpdateFactory.newLatLngBounds(MapaMat.limites(plan), 80))
                seguir && !siguiendo -> Unit
                seguir && modo3d -> {
                    val r = (rumbo ?: m.cameraPosition.bearing.toFloat()).toDouble()
                    val z = ZoomNavegacion.para(velocidadMs)
                    val centro = MapaMat.desplazar(ultimo, r, adelanteM * Math.pow(2.0, 18.0 - z))
                    // Animacion lineal del largo del intervalo GPS (2 s): el suelo avanza de forma continua.
                    m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(centro).zoom(z).tilt(60.0).bearing(r).build()), 1900, false)
                }
                seguir || puntos.size < 2 -> m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(ultimo).zoom(zoom.toDouble()).tilt(0.0).bearing(0.0).build()), 900)
                else -> m.animateCamera(CameraUpdateFactory.newLatLngBounds(MapaMat.limites(puntos), 80))
            }
        }
    }

    val trazos = buildList {
        if (plan.size >= 2) {
            add(TrazoMapa(plan, NARANJA, 22f, 0.22f))
            add(TrazoMapa(plan, Color(0xFFFFD2B3), 6f, 0.55f))
        }
        if (puntos.size >= 2) {
            add(TrazoMapa(puntos, NARANJA, 14f, 0.18f))
            add(TrazoMapa(puntos, Color(0xFF05080F), 9f))
            add(TrazoMapa(puntos, NARANJA, 5f))
        }
    }
    val circulos = buildList {
        if (puntos.size >= 2 && !seguir) {
            add(CirculoMapa(puntos.first(), 7f, Color(0xFF10B981), bordeDp = 2.5f))
            add(CirculoMapa(puntos.last(), 7f, Color(0xFF111827), bordeDp = 2.5f))
        }
        if (seguir && !con3d && ultimo != null) {
            add(CirculoMapa(ultimo, 16f, NARANJA.copy(alpha = 0.18f), NARANJA.copy(alpha = 0.35f), 1f))
            add(CirculoMapa(ultimo, 6f, NARANJA, bordeDp = 2.5f))
        }
    }

    Box(modifier) {
        MapaLibre(
            Modifier.fillMaxSize().testTag("mapa-gacelas"),
            trazos = trazos,
            circulos = circulos,
            oscuro = oscuro,
            centro = ultimo ?: plan.firstOrNull() ?: Mapas.CENTRO_LOJA,
            zoom = zoom.toDouble(),
            alGesto = { if (seguir) siguiendo = false },
            alMoverCamara = { m ->
                rumboCamara = m.cameraPosition.bearing.toFloat()
                pantalla = ultimoActual?.let { m.projection.toScreenLocation(it).let { p -> Offset(p.x, p.y) } }
            },
            alListo = { mapa = it },
        )
        if (con3d && ultimo != null) {
            // Corredor 3D anclado al punto proyectado del usuario (la camara gira con el rumbo).
            val px = pantalla
            val densidad = LocalDensity.current
            if (px != null) {
                val lado = 190.dp
                val ladoPx = with(densidad) { lado.toPx() }
                Box(Modifier.offset { IntOffset((px.x - ladoPx / 2).toInt(), (px.y - ladoPx * 0.86f).toInt()) }.size(lado).testTag("corredor-3d-mapa")) {
                    Runner3D(MapaMat.clipPorRitmo(ritmoSegKm), camara = Position(0f, 1.1f, -3.6f))
                }
            }
        }
        if (controles) ControlesMapa(mapa, rumboCamara, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        if (seguir && !siguiendo) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(CircleShape).background(Color(0xF2121A2A))
                    .clickable { siguiendo = true }.padding(horizontal = 16.dp, vertical = 10.dp).testTag("recentrar"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, tint = NARANJA)
                Text("  Recentrar", color = Color.White, style = GType.labelLg)
            }
        }
    }
}

/** Zoom y brujula propios (estilo Gacelas). */
@Composable
private fun ControlesMapa(mapa: MapLibreMap?, rumbo: Float, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xE6121A2A))) {
            IconButton(onClick = { mapa?.animateCamera(CameraUpdateFactory.zoomIn()) }) { Icon(Icons.Filled.Add, "Acercar", tint = Color.White) }
            IconButton(onClick = { mapa?.animateCamera(CameraUpdateFactory.zoomOut()) }) { Icon(Icons.Filled.Remove, "Alejar", tint = Color.White) }
        }
        IconButton(
            onClick = { mapa?.let { m -> m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).bearing(0.0).tilt(0.0).build())) } },
            modifier = Modifier.clip(CircleShape).background(Color(0xE6121A2A)),
        ) { Icon(Icons.Filled.Navigation, "Orientar al norte", tint = NARANJA, modifier = Modifier.rotate(-rumbo)) }
    }
}
