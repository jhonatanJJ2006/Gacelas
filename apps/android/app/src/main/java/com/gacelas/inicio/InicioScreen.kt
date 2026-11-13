package com.gacelas.inicio

import com.gacelas.comunidad.domain.reglas.yaPaso
import com.gacelas.ui.components.InsigniaRacha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.data.mock.MockData
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.carrerasState
import com.gacelas.ui.sesionState
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.components.TituloSeccion
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3D
import io.github.sceneview.math.Position

@Composable
fun InicioScreen(
    onCorrer: () -> Unit,
    onEventos: () -> Unit,
    onEvento: (String) -> Unit = { onEventos() },
    onComunidad: () -> Unit = {},
    onRutas: () -> Unit = {},
    onRuta: ((String) -> Unit)? = null,
    onBuscar: () -> Unit = onRutas,
    onNotificaciones: () -> Unit = {},
    onProgreso: () -> Unit = {},
    onMapaVivo: () -> Unit = {},
) {
    val (unidos, inscritos) = com.gacelas.comunidad.eventosState()
    val cont = com.gacelas.ui.contenedor()
    val creados by cont.eventos.creados.collectAsState(initial = emptyList())
    // Eventos reales: los de la base con servidor o los creados por el usuario; nunca de ejemplo.
    val todos = if (cont.feed != null) creados else unidos
    val evento = todos.filter { !it.yaPaso() }.minByOrNull { it.inicioMs ?: Long.MAX_VALUE }
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val ahora = remember { System.currentTimeMillis() }
    val semana = remember(carreras) { Estadisticas.semana(carreras, ahora) }
    val hoy = remember { Estadisticas.diaSemana(ahora) }
    // Ruta sugerida para hoy: la decide Supabase (ruta_sugerida_hoy); ver docs/RUTA-SUGERIDA.md.
    val delDia by androidx.compose.runtime.produceState<com.gacelas.rutas.infrastructure.RutaDelDia?>(null, carreras.size) {
        value = cont.rutaSugerida.hoy(
            com.gacelas.data.mock.MockData.rutas,
            carreras.map { c -> com.gacelas.rutas.domain.reglas.CarreraSugerida(c.inicioMs, c.distanciaKm, c.puntos.firstOrNull()?.let { it.lat to it.lng }) },
            System.currentTimeMillis(),
        )
    }
    val ruta = (delDia as? com.gacelas.rutas.infrastructure.RutaDelDia.Ok)?.ruta
    // Clima real: cache al instante y refresco al abrir Inicio (el repositorio respeta 15 min).
    val climaVm = androidx.lifecycle.viewmodel.compose.viewModel { cont.climaInicioViewModel { System.currentTimeMillis() } }
    val clima by climaVm.clima.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { climaVm.refrescar() }
    // Seguridad real (migracion 040) y corredores en vivo (runner_positions); sin servidor se ocultan.
    val zona by androidx.compose.runtime.produceState<com.gacelas.seguridad.domain.ResumenZona?>(null, cont.incidentes) { value = cont.incidentes?.resumenZona() }
    val seguridadRutas by androidx.compose.runtime.produceState(emptyMap<String, com.gacelas.seguridad.domain.SeguridadRuta>(), cont.incidentes) { value = cont.incidentes?.seguridadRutas().orEmpty() }
    val vivos by androidx.compose.runtime.produceState(emptyList<com.gacelas.vivo.CorredorEnMapa>(), cont.mapaVivo) { value = cont.mapaVivo?.listar().orEmpty() }
    val incidentes = com.gacelas.seguridad.ui.incidentesRecientes()
    val ajustesNotif = ajustes
    val base by com.gacelas.notificaciones.notificacionesBase()
    val sinLeer = com.gacelas.comunidad.domain.reglas.notificacionesVisibles(ajustesNotif, base).count { !it.leida }
    val trazo = remember(ruta?.id) { ruta?.let { r -> r.puntos.map { org.maplibre.android.geometry.LatLng(it.first, it.second) }.ifEmpty { com.gacelas.ui.components.Mapas.trazo(r.id) } }.orEmpty() }
    InicioTablero(
        semana = semana, hoy = hoy, m = m, racha = com.gacelas.ui.rachaState(),
        mapa = {
            com.gacelas.ui.components.MapaTablero(
                trazo, Modifier.fillMaxSize(),
                circulos = vivos.map { com.gacelas.ui.components.CirculoMapa(org.maplibre.android.geometry.LatLng(it.lat, it.lng), 7f, GColors.C2Menta) } +
                    incidentes.take(20).map { com.gacelas.ui.components.CirculoMapa(org.maplibre.android.geometry.LatLng(it.lat, it.lng), 6f, Color(0xFFF59E0B)) },
                margenArriba = 110.dp, margenAbajo = 110.dp,
            )
        },
        onCorrer = onCorrer, onBuscar = onBuscar, onNotificaciones = onNotificaciones, onProgreso = onProgreso, onMapaVivo = onMapaVivo,
        onComunidad = onComunidad, onEvento = onEvento, onRutas = onRutas, onRuta = { id -> onRuta?.invoke(id) ?: onRutas() },
        sinLeer = sinLeer,
        metaKm = ajustes.metaKmSemana,
        kmPorDia = remember(carreras) {
            val desde = Estadisticas.inicioSemana(ahora)
            val km = DoubleArray(7)
            carreras.filter { it.inicioMs >= desde && it.inicioMs <= ahora + 86_400_000L }.forEach { km[Estadisticas.diaSemana(it.inicioMs)] += it.distanciaKm }
            km.toList()
        },
        clima = clima,
        ritmoSemanas = remember(carreras) { ritmoPorSemana(carreras, ahora) },
        evento = evento,
        inscrito = evento?.id in inscritos,
        rutaDelDia = ruta,
        rutaDelDiaNota = when (val d = delDia) {
            is com.gacelas.rutas.infrastructure.RutaDelDia.Ok -> null
            is com.gacelas.rutas.infrastructure.RutaDelDia.SinRuta -> d.motivo
            null -> "Buscando una ruta segura para hoy..."
        },
        seguridadRuta = ruta?.let { r -> seguridadRutas[r.id.removePrefix("ruta-")]?.texto ?: seguridadRutas[r.id]?.texto },
        zona = zona,
        corriendo = vivos.size,
        avisoPendientes = { com.gacelas.ui.AvisoCarrerasPendientes() },
    )
}

/** Ritmo medio (seg/km) de cada una de las ultimas 8 semanas, de la mas antigua a la actual. */
fun ritmoPorSemana(carreras: List<com.gacelas.actividad.domain.modelo.Carrera>, ahoraMs: Long): List<Int?> {
    val lunes = Estadisticas.inicioSemana(ahoraMs)
    return (7 downTo 0).map { w ->
        val desde = com.gacelas.shared.Fechas.sumarDias(lunes, -7 * w)
        val hasta = com.gacelas.shared.Fechas.sumarDias(desde, 7)
        val sem = carreras.filter { it.inicioMs in desde until hasta && it.distanciaKm > 0.2 }
        val km = sem.sumOf { it.distanciaKm }
        if (km <= 0.0) null else (sem.sumOf { it.segundos } / km).toInt()
    }
}

/** "19 - 25 oct" de la semana en curso (lunes a domingo). */
fun rangoSemana(ahoraMs: Long): String {
    val lunes = com.gacelas.shared.Fechas.fecha(Estadisticas.inicioSemana(ahoraMs))
    val domingo = com.gacelas.shared.Fechas.fecha(com.gacelas.shared.Fechas.sumarDias(Estadisticas.inicioSemana(ahoraMs), 6))
    return "${lunes.dayOfMonth} - ${domingo.dayOfMonth} ${com.gacelas.actividad.domain.reglas.EstadisticasPerfil.MESES_CORTOS[domingo.monthNumber - 1].lowercase()}"
}
