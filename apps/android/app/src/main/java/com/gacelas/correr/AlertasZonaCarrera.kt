package com.gacelas.correr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gacelas.seguridad.domain.AlertasZona
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.AvisosC2
import com.gacelas.ui.contenedor

/**
 * Alertas de zona en la carrera (86e3uf09b): lee los reportes recientes al empezar y cada 5 min y, con
 * cada punto GPS, avisa una sola vez por reporte a menos de 200 m (vibracion, tono y aviso arriba).
 * Se desactivan en Ajustes, Privacidad y ubicacion.
 */
@Composable
internal fun AlertasZonaCarrera(tracker: com.gacelas.actividad.domain.SesionCarrera) {
    val c = contenedor()
    val repo = c.incidentes ?: return
    val ajustes by ajustesState()
    if (!ajustes.alertasZona) return
    val context = LocalContext.current
    val avisos = remember { com.gacelas.tracking.Avisos(context) }
    DisposableEffect(Unit) { onDispose { avisos.liberar() } }
    var reportes by remember { mutableStateOf<List<IncidentePublico>>(emptyList()) }
    val avisados = remember { mutableSetOf<String>() }
    LaunchedEffect(Unit) {
        while (true) {
            reportes = runCatching { repo.recientes() }.getOrDefault(reportes)
            kotlinx.coroutines.delay(AlertasZona.REFRESCO_MS)
        }
    }
    val estado by tracker.estado.collectAsState()
    val ultimo = estado.puntos.lastOrNull()
    LaunchedEffect(ultimo, reportes) {
        val p = ultimo ?: return@LaunchedEffect
        val nuevas = AlertasZona.nuevas(p.lat, p.lng, reportes, avisados, System.currentTimeMillis())
        if (nuevas.isEmpty()) return@LaunchedEffect
        nuevas.forEach { avisados += it.first.id }
        val (i, m) = nuevas.first()
        avisos.alertaZona()
        AvisosC2.mostrar(AlertasZona.mensaje(i, m), error = true)
    }
}
