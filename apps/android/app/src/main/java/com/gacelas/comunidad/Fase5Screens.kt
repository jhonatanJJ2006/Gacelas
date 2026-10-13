package com.gacelas.comunidad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.comunidad.presentation.ui.ClubesContenido
import com.gacelas.comunidad.presentation.ui.DetalleClubContenido
import com.gacelas.comunidad.presentation.ui.EventoEnVivoContenido
import com.gacelas.ui.aviso
import com.gacelas.ui.components.CirculoMapa
import com.gacelas.ui.components.MapaLibre
import com.gacelas.ui.components.MapaMat
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.TrazoMapa
import com.gacelas.ui.contenedor
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

fun colorCorredor(i: Int) = com.gacelas.comunidad.presentation.ui.colorCorredor(i)

@Composable
fun ClubesScreen(onAtras: () -> Unit, onClub: (String) -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val vm = viewModel { c.clubesViewModel() }
    ClubesContenido(vm, conServidor = c.clubes != null, onAtras = onAtras, onClub = onClub, onAviso = { context.aviso(it) })
}

@Composable
fun DetalleClubScreen(id: String, onAtras: () -> Unit, onEvento: (String) -> Unit, onCrearEvento: (String) -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val vm = viewModel(key = "detalle-club-$id") { c.detalleClubViewModel(id) }
    DetalleClubContenido(id, vm, conServidor = c.clubes != null, onAtras = onAtras, onEvento = onEvento, onCrearEvento = onCrearEvento, onAviso = { context.aviso(it) })
}

/** Mapa en vivo de todos los corredores del evento, clasificacion y repeticion (MapLibre Android). */
@Composable
fun EventoEnVivoScreen(id: String, onAtras: () -> Unit, onCorrer: () -> Unit) {
    val c = contenedor()
    val vm = viewModel(key = "evento-en-vivo-$id") { c.eventoEnVivoViewModel(id) }
    EventoEnVivoContenido(
        vm, onAtras, onCorrer,
        mapa = if (!Mapas.configurado()) null else { trazos, circulos, encuadre ->
            var mapaEvento by remember { mutableStateOf<MapLibreMap?>(null) }
            LaunchedEffect(encuadre.size, mapaEvento) {
                val m = mapaEvento ?: return@LaunchedEffect
                if (encuadre.size < 2) return@LaunchedEffect
                runCatching { m.moveCamera(CameraUpdateFactory.newLatLngBounds(MapaMat.limites(encuadre.map { LatLng(it.lat, it.lng) }), 120)) }
            }
            MapaLibre(
                Modifier.fillMaxSize().testTag("mapa-evento"),
                trazos = trazos.map { t -> TrazoMapa(t.puntos.map { LatLng(it.lat, it.lng) }, Color(t.argb), t.anchoDp, t.opacidad) },
                circulos = circulos.map { k -> CirculoMapa(LatLng(k.centro.lat, k.centro.lng), k.radioDp, Color(k.relleno), Color(k.borde), k.bordeDp, k.id) },
                alListo = { mapaEvento = it },
            )
        },
    )
}
