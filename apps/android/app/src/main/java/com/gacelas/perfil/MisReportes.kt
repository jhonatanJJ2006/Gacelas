package com.gacelas.perfil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.ui.contenedor
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.ui.compartir

/** Inicios de las rutas oficiales de Loja para detectar rutas favoritas. */
val iniciosRutasLoja: List<InicioRuta> by lazy {
    RutasLoja.todas.mapNotNull { r -> r.puntos.firstOrNull()?.let { InicioRuta(r.id, r.nombre, it.first, it.second) } }
}

@Composable
fun MisReportesScreen(onAtras: () -> Unit) {
    // Casos de uso de Analitica (VerResumenPersonal) a traves de su ViewModel.
    val c = contenedor()
    val vm = viewModel(key = "reportes-personales") { c.reportesPersonalesViewModel({ iniciosRutasLoja }, { System.currentTimeMillis() }) }
    val estado by vm.estado.collectAsState()
    val context = LocalContext.current
    MisReportesContenido(estado, vm::elegir, onAtras, onCompartir = { context.compartir(it) })
}
