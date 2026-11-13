package com.gacelas.correr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.gacelas.core.location.EventosAlertaZona
import com.gacelas.ui.components.AvisosC2

/**
 * Alertas de zona en la pantalla de la carrera (86e3uf09b). Desde 0.9.15 las calcula y notifica el
 * servicio de ubicacion en primer plano (AlertasZonaServicio), tambien con la pantalla apagada; aqui
 * solo se repiten como aviso arriba cuando la app esta abierta.
 */
@Composable
internal fun AlertasZonaCarrera(@Suppress("UNUSED_PARAMETER") tracker: com.gacelas.actividad.domain.SesionCarrera) {
    LaunchedEffect(Unit) {
        EventosAlertaZona.flujo.collect { AvisosC2.mostrar(it, error = true) }
    }
}
