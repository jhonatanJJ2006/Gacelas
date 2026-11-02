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
fun InicioScreen(onCorrer: () -> Unit, onEventos: () -> Unit, onEvento: (String) -> Unit = { onEventos() }, onComunidad: () -> Unit = {}, onRutas: () -> Unit = {}) {
    val (unidos, inscritos) = com.gacelas.comunidad.eventosState()
    val cont = com.gacelas.ui.contenedor()
    val creados by cont.eventos.creados.collectAsState(initial = emptyList())
    // Con Supabase los eventos vienen de la base; los de ejemplo solo sin backend.
    val todos = if (cont.feed != null && creados.isNotEmpty()) creados else unidos
    val social by com.gacelas.ui.socialState()
    val evento = todos.filter { !it.yaPaso() }.minByOrNull { it.inicioMs ?: Long.MAX_VALUE } ?: todos.maxBy { it.inicioMs ?: 0L }
    val perfil by sesionState()
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val ahora = remember { System.currentTimeMillis() }
    val semana = remember(carreras) { Estadisticas.semana(carreras, ahora) }
    val hoy = remember { Estadisticas.diaSemana(ahora) }
    val hora = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val saludo = saludoPorHora(hora)
    val nombre = perfil?.nombre?.substringBefore(' ')?.ifBlank { null } ?: "Corredor"
    // Ruta sugerida para hoy: la decide Supabase (ruta_sugerida_hoy); ver docs/RUTA-SUGERIDA.md.
    val delDia by androidx.compose.runtime.produceState<com.gacelas.rutas.infrastructure.RutaDelDia?>(null, carreras.size) {
        value = cont.rutaSugerida.hoy(
            com.gacelas.data.mock.MockData.rutas,
            carreras.map { c -> com.gacelas.rutas.domain.reglas.CarreraSugerida(c.inicioMs, c.distanciaKm, c.puntos.firstOrNull()?.let { it.lat to it.lng }) },
            System.currentTimeMillis(),
        )
    }
    // Clima real: cache al instante y refresco al abrir Inicio (el repositorio respeta 15 min).
    val climaVm = androidx.lifecycle.viewmodel.compose.viewModel { cont.climaInicioViewModel { System.currentTimeMillis() } }
    val clima by climaVm.clima.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { climaVm.refrescar() }
    InicioContenido(
        saludo = saludo, nombre = nombre, semana = semana, hoy = hoy, m = m, racha = com.gacelas.ui.rachaState(), evento = evento,
        onCorrer = onCorrer, onEventos = onEventos, onEvento = onEvento,
        corredor3D = { Runner3D(ClipCorredor.Idle, autoRotar = true, camara = Position(0f, 0.3f, 4.4f), sobreFondo = true) },
        totales = remember(carreras) { com.gacelas.actividad.domain.reglas.EstadisticasPerfil.totalesSemana(carreras, ahora) },
        meta = ajustes.metaSemanal,
        rango = remember { rangoSemana(ahora) },
        reto = remember(carreras) { com.gacelas.actividad.domain.reglas.EstadisticasPerfil.retoMes(carreras, ahora) },
        amigos = social.amigos,
        inscrito = evento.id in inscritos,
        onComunidad = onComunidad,
        rutaSugerida = (delDia as? com.gacelas.rutas.infrastructure.RutaDelDia.Ok)?.ruta,
        rutaSugeridaNota = when (val d = delDia) {
            is com.gacelas.rutas.infrastructure.RutaDelDia.Ok ->
                if (d.sinConexion) "Segura según la última consulta. No la corriste en 3 días" else "Zona segura a esta hora. No la corriste en 3 días"
            is com.gacelas.rutas.infrastructure.RutaDelDia.SinRuta -> d.motivo
            null -> "Buscando una ruta segura para hoy..."
        },
        onRutas = onRutas,
        nivel = com.gacelas.comunidad.domain.modelo.nivelCorredor(carreras.sumOf { it.distanciaKm }),
        clima = clima,
    )
}

/** "19 - 25 oct" de la semana en curso (lunes a domingo). */
fun rangoSemana(ahoraMs: Long): String {
    val lunes = com.gacelas.shared.Fechas.fecha(Estadisticas.inicioSemana(ahoraMs))
    val domingo = com.gacelas.shared.Fechas.fecha(com.gacelas.shared.Fechas.sumarDias(Estadisticas.inicioSemana(ahoraMs), 6))
    return "${lunes.dayOfMonth} - ${domingo.dayOfMonth} ${com.gacelas.actividad.domain.reglas.EstadisticasPerfil.MESES_CORTOS[domingo.monthNumber - 1].lowercase()}"
}
