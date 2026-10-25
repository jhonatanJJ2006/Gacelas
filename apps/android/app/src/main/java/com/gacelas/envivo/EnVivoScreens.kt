package com.gacelas.envivo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.comunidad.presentation.ui.DetalleEnVivoContenido
import com.gacelas.comunidad.presentation.ui.ListaEnVivoContenido
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.contenedor
import org.maplibre.android.geometry.LatLng

/** Corredores en vivo que puedo ver. Se actualiza cada 10 s. */
@Composable
fun ListaEnVivoScreen(onAtras: () -> Unit, onSesion: (String) -> Unit, onMapa: (() -> Unit)? = null) {
    val c = contenedor()
    val vm = viewModel { c.listaEnVivoViewModel() }
    val m by ajustesState()
    ListaEnVivoContenido(vm, m, onAtras, onSesion, onMapa)
}

/** Mapa en vivo de un corredor. La web usa Supabase Realtime; aqui se consulta cada 5 s. */
@Composable
fun DetalleEnVivoScreen(id: String, onAtras: () -> Unit) {
    val c = contenedor()
    val vm = viewModel(key = "en-vivo-$id") { c.detalleEnVivoViewModel(id) }
    val a by ajustesState()
    DetalleEnVivoContenido(vm, a, onAtras) { puntos, modifier, oscuro, alternativa ->
        MapaGacelas(puntos.map { LatLng(it.lat, it.lng) }, modifier, seguir = true, oscuro = oscuro) { alternativa() }
    }
}
