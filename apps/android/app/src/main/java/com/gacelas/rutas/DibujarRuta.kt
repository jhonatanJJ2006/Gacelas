package com.gacelas.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.Dificultad
import com.gacelas.data.mock.Ruta
import com.gacelas.data.rutas.ETIQUETAS_RUTA
import com.gacelas.data.rutas.Elevacion
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.aviso
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private class ItemRuta(val ruta: Ruta) : ClusterItem {
    override fun getPosition() = LatLng(ruta.puntos.first().first, ruta.puntos.first().second)
    override fun getTitle() = ruta.nombre
    override fun getSnippet() = "%.1f km".format(ruta.distanciaKm)
    override fun getZIndex() = 0f
}

/** Salidas de las rutas en el mapa, agrupadas en clusters (maps-compose-utils). */
@Composable
fun MapaRutasAgrupadas(rutas: List<Ruta>, onRuta: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!Mapas.configurado()) {
        Text("El mapa necesita Google Maps configurado.", style = GType.bodyMd, color = GColors.TextSecondary)
        return
    }
    val items = remember(rutas) { rutas.filter { it.puntos.isNotEmpty() }.map(::ItemRuta) }
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(Mapas.CENTRO_LOJA, 12f) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        properties = MapProperties(mapStyleOptions = MapStyleOptions(Mapas.ESTILO_OSCURO)),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
    ) {
        Clustering(items = items, onClusterItemClick = { onRuta(it.ruta.id); true })
    }
}

/** Crear una ruta tocando el mapa punto a punto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DibujarRutaScreen(onAtras: () -> Unit, onCreada: (String) -> Unit) {
    val repo = contenedor().explorar
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var puntos by remember { mutableStateOf(listOf<LatLng>()) }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var dificultad by remember { mutableStateOf(Dificultad.Moderado) }
    var etiquetas by remember { mutableStateOf(setOf<String>()) }
    var guardando by remember { mutableStateOf(false) }
    val km = puntos.zipWithNext().sumOf { (a, b) -> haversineKm(a, b) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Dibujar ruta", onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                if (Mapas.configurado()) {
                    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(Mapas.CENTRO_LOJA, 15f) }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camara,
                        properties = MapProperties(mapStyleOptions = MapStyleOptions(Mapas.ESTILO_OSCURO)),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                        onMapClick = { puntos = puntos + it },
                    ) {
                        if (puntos.size >= 2) Polyline(points = puntos, color = Color(0xFFF59E0B), width = 12f)
                        puntos.forEachIndexed { i, p -> Circle(center = p, radius = if (i == 0) 8.0 else 5.0, fillColor = if (i == 0) Color(0xFF10B981) else Color(0xFFF59E0B), strokeColor = Color.White, strokeWidth = 3f) }
                    }
                } else Text("Dibujar rutas necesita Google Maps configurado.", color = GColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%.2f km · %d puntos".format(km, puntos.size), style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { puntos = puntos.dropLast(1) }, enabled = puntos.isNotEmpty()) { Text("Deshacer", color = GColors.Primary400) }
                TextButton(onClick = { puntos = emptyList() }, enabled = puntos.isNotEmpty()) { Text("Borrar", color = GColors.Primary400) }
            }
            Text("Toca el mapa para ir agregando puntos en orden.", style = GType.bodySm, color = GColors.TextSecondary)
            CampoTexto("Nombre", nombre, { nombre = it.take(80) }, "Nombre de la ruta")
            CampoTexto("Descripción", descripcion, { descripcion = it.take(500) }, "Descripción (opcional)", lineas = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Dificultad.entries.forEach { d -> Pastilla(d.etiqueta, d == dificultad, { dificultad = d }) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ETIQUETAS_RUTA.forEach { (k, v) -> Pastilla("#$v", k in etiquetas, { etiquetas = if (k in etiquetas) etiquetas - k else etiquetas + k }) }
            }
            BotonPrimario(if (guardando) "Publicando..." else "Publicar ruta", {
                when {
                    repo == null -> context.aviso("Inicia sesión para publicar rutas")
                    nombre.trim().length < 3 -> context.aviso("Ponle un nombre de al menos 3 letras")
                    puntos.size < 2 -> context.aviso("Toca el mapa al menos dos veces")
                    else -> {
                        guardando = true
                        scope.launch {
                            val pares = puntos.map { it.latitude to it.longitude }
                            val perfil = Elevacion.perfil(pares)
                            runCatching { repo.crear(nombre.trim(), descripcion.trim(), dificultad, etiquetas.toList(), pares, Elevacion.desnivelPositivo(perfil), null, perfil, "dibujada") }
                                .onSuccess { context.aviso("Ruta publicada"); onCreada(it) }
                                .onFailure { context.aviso(it.message ?: "No se pudo publicar"); guardando = false }
                        }
                    }
                }
            }, enabled = !guardando, icono = null)
        }
    }
}

private fun haversineKm(a: LatLng, b: LatLng): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude); val dLng = Math.toRadians(b.longitude - a.longitude)
    val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * Math.sin(dLng / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(h))
}
