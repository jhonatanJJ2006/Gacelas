package com.gacelas.perfil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.ui.carrerasState
import com.gacelas.ui.compartir

/** Inicios de las rutas oficiales de Loja para detectar rutas favoritas. */
val iniciosRutasLoja: List<InicioRuta> by lazy {
    RutasLoja.todas.mapNotNull { r -> r.puntos.firstOrNull()?.let { InicioRuta(r.id, r.nombre, it.first, it.second) } }
}

@Composable
fun MisReportesScreen(onAtras: () -> Unit) {
    val carreras by carrerasState()
    val context = LocalContext.current
    MisReportesContenido(carreras, System.currentTimeMillis(), onAtras, onCompartir = { context.compartir(it) }, rutas = iniciosRutasLoja)
}
