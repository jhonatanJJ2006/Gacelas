package com.gacelas.inicio

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
import androidx.compose.runtime.remember
import com.gacelas.data.carreras.Estadisticas
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
fun InicioScreen(onCorrer: () -> Unit, onEventos: () -> Unit, onEvento: (String) -> Unit = { onEventos() }) {
    val evento = MockData.eventos.first { !it.pasado }
    val perfil by sesionState()
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val ahora = remember { System.currentTimeMillis() }
    val semana = remember(carreras) { Estadisticas.semana(carreras, ahora) }
    val hoy = remember { Estadisticas.diaSemana(ahora) }
    val hora = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val saludo = when (hora) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }
    val nombre = perfil?.nombre?.substringBefore(' ')?.ifBlank { null } ?: "Corredor"
    Pantalla {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                    Text(" Loja, Ecuador", style = GType.labelMd, color = GColors.TextSecondary)
                }
                Text("$saludo, $nombre", style = GType.headlineMd, color = GColors.TextPrimary)
            }
            Row(Modifier.clip(CircleShape).background(GColors.SurfaceContainer).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbTwilight, null, tint = GColors.Primary400, modifier = Modifier.size(16.dp))
                Text(" 14°C Fresco", style = GType.labelMd, color = GColors.TextPrimary)
            }
        }
        Tarjeta(fondo = GColors.SurfaceLow, padding = 0.dp) {
            Box(Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(GColors.SurfaceLow, GColors.Primary.copy(alpha = 0.12f))))) {
                Runner3D(ClipCorredor.Idle, autoRotar = true, camara = Position(0f, 0.6f, 3.6f))
                Row(Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(GColors.Secondary500))
                    Text("  AUTOPILOT ACTIVO", style = GType.labelMd, color = GColors.Secondary500)
                }
                Row(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = GColors.Primary500, modifier = Modifier.size(16.dp))
                    Text(" Racha ${semana.racha}d", style = GType.labelMd, color = GColors.TextPrimary)
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("${semana.diasCorridos.size}", "Días", color = GColors.TextPrimary)
                Metrica(Unidades.texto(semana.km, m, 1), Unidades.etiquetaDistancia(m), color = GColors.Primary500)
                Metrica(formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m)), Unidades.etiquetaRitmo(m), color = GColors.Secondary500)
            }
        }
        TituloSeccion("Acción rápida")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(GColors.Primary, GColors.Primary700)))
                .clickable(onClick = onCorrer).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Empezar a correr", style = GType.headlineSm, color = GColors.TextPrimary)
                Text("Graba tu siguiente carrera", style = GType.bodyMd, color = GColors.TextPrimary.copy(alpha = 0.85f))
            }
            IconoCirculo(Icons.Filled.PlayArrow, GColors.TextPrimary.copy(alpha = 0.2f), GColors.TextPrimary, 56.dp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Semana atlética", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${semana.diasCorridos.size} de 5 días", style = GType.labelMd, color = GColors.Secondary500)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, dia ->
                val hecho = i in semana.diasCorridos
                val esHoy = i == hoy
                Column(
                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (hecho) GColors.Secondary.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                        .border(if (hecho || esHoy) 2.dp else 1.dp, if (hecho) GColors.Secondary else if (esHoy) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(dia, style = GType.labelMd, color = if (hecho) GColors.Secondary500 else GColors.TextSecondary)
                    if (hecho) Icon(Icons.Filled.CheckCircle, null, tint = GColors.Secondary500, modifier = Modifier.size(14.dp))
                }
            }
        }
        TituloSeccion("Próximo encuentro", accion = "Ver todos", onAccion = onEventos)
        Tarjeta(onClick = { onEvento(evento.id) }) {
            Row(verticalAlignment = Alignment.Top) {
                IconoCirculo(Icons.Filled.Event, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                        Text(" ${evento.fecha}, ${evento.hora}", style = GType.labelMd, color = GColors.Primary500)
                    }
                    Text(evento.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                    Text(evento.descripcion, style = GType.bodyMd, color = GColors.TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(72.dp)) {
                            (0..2).forEach { i -> Box(Modifier.offset(x = (i * 18).dp)) { AvatarCirculo(28.dp, i) } }
                        }
                        Text("+12 corredores", style = GType.labelMd, color = GColors.TextSecondary)
                    }
                }
            }
        }
    }
}
