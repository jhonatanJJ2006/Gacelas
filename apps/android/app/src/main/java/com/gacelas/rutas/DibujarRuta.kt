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
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA
import com.gacelas.rutas.data.Elevacion
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.CirculoMapa
import com.gacelas.ui.components.MapaLibre
import com.gacelas.ui.components.TrazoMapa
import com.gacelas.ui.aviso
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.launch

/** Salidas de las rutas en el mapa; al tocar una se abre su detalle. */
@Composable
fun MapaRutasAgrupadas(rutas: List<Ruta>, onRuta: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!Mapas.configurado()) {
        Text("El mapa no está disponible en este dispositivo.", style = GType.bodyMd, color = GColors.TextSecondary)
        return
    }
    val salidas = remember(rutas) {
        rutas.filter { it.puntos.isNotEmpty() }.map { CirculoMapa(LatLng(it.puntos.first().first, it.puntos.first().second), 9f, Color(0xFFFF6A13), bordeDp = 2.5f, id = it.id) }
    }
    MapaLibre(modifier, circulos = salidas, zoom = 12.0, alTocarCirculo = onRuta)
}

/** Crear una ruta tocando el mapa punto a punto (UI compartida en shared, MapLibre Android). */
@Composable
fun DibujarRutaScreen(onAtras: () -> Unit, onCreada: (String) -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.dibujarRutaViewModel() }
    com.gacelas.rutas.presentation.ui.DibujarRutaContenido(
        vm, onAtras, onCreada, onAviso = { context.aviso(it) },
        mapa = if (!Mapas.configurado()) null else { trazos, circulos, alTocar ->
            MapaLibre(
                Modifier.fillMaxSize(),
                trazos = trazos.map { t -> TrazoMapa(t.puntos.map { LatLng(it.lat, it.lng) }, Color(t.argb), t.anchoDp, t.opacidad) },
                circulos = circulos.map { k -> CirculoMapa(LatLng(k.centro.lat, k.centro.lng), k.radioDp, Color(k.relleno), Color(k.borde), k.bordeDp, k.id) },
                zoom = 15.0,
                alTocar = { alTocar(com.gacelas.mapa.Coord(it.latitude, it.longitude)) },
            )
        },
    )
}
