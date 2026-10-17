package com.gacelas.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.gacelas.notificaciones.notificacionesBase
import com.gacelas.comunidad.domain.reglas.notificacionesVisibles
import com.gacelas.ui.ajustesState

/** Scaffold principal (compartido) con el contador de notificaciones sin leer de Android. */
@Composable
fun ScaffoldPrincipal(rutaActual: String, onNavegar: (String) -> Unit, content: @Composable () -> Unit) {
    val ajustes by ajustesState()
    val base by notificacionesBase()
    ScaffoldPrincipal(rutaActual, notificacionesVisibles(ajustes, base).count { !it.leida }, onNavegar, content)
}
