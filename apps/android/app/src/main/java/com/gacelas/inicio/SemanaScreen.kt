package com.gacelas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.reglas.DiaSemana
import com.gacelas.actividad.domain.reglas.EstadoDia
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.shared.Fechas
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.carrerasState
import com.gacelas.ui.components.AnilloC2
import com.gacelas.ui.components.CeldaTablero
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EstadoVacioC2
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private fun dec1(x: Double) = "%.1f".format(java.util.Locale.US, x).replace('.', ',')

private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** "Esta semana": resumen y un dia por fila; se puede volver a semanas anteriores. */
@Composable
fun SemanaScreen(onAtras: () -> Unit, onCorrer: () -> Unit, onCarrera: (String) -> Unit) {
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val ahora = remember { System.currentTimeMillis() }
    var semanasAtras by rememberSaveable { mutableIntStateOf(0) }
    val inicio = Fechas.sumarDias(Estadisticas.inicioSemana(ahora), -7 * semanasAtras)
    val dias = remember(carreras, inicio) { Estadisticas.diasDeSemana(carreras, inicio, ahora) }
    val km = dias.sumOf { it.km }
    val seg = dias.sumOf { it.segundos }
    val n = dias.sumOf { it.carreras.size }
    Column(Modifier.background(com.gacelas.ui.components.Tablero.Fondo)) {
        EncabezadoAtras(if (semanasAtras == 0) "Esta semana" else "Semana anterior", onAtras)
        Pantalla(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Row(Modifier.fillMaxWidth().testTag("semana-selector"), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { semanasAtras++ }, modifier = Modifier.testTag("semana-anterior")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior", tint = GColors.TextPrimary)
                }
                Text(rangoSemana(inicio), style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { semanasAtras-- }, enabled = semanasAtras > 0, modifier = Modifier.testTag("semana-siguiente")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente", tint = if (semanasAtras > 0) GColors.TextPrimary else GColors.C2Muted.copy(alpha = 0.3f))
                }
            }
            Tarjeta(modifier = Modifier.testTag("semana-resumen")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val meta = ajustes.metaKmSemana
                    if (meta > 0) {
                        val p = (km / meta).toFloat().coerceIn(0f, 1f)
                        AnilloC2(p, tamano = 92.dp, grosor = 10.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(p * 100).toInt()}%", style = GType.titleMd, color = GColors.TextPrimary)
                                Text("de $meta ${Unidades.etiquetaDistancia(m)}", style = GType.bodySm, color = GColors.C2Muted)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CeldaTablero("Distancia", dec1(Unidades.distancia(km, m)), Modifier.weight(1f), unidad = Unidades.etiquetaDistancia(m), color = GColors.C2Violeta, tamano = 22)
                            CeldaTablero("Carreras", "$n", Modifier.weight(1f), tamano = 22)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CeldaTablero("Tiempo", formatearHms(seg), Modifier.weight(1f), tamano = 18)
                            CeldaTablero("Ritmo", formatearRitmo(if (km > 0.01) Unidades.ritmo((seg / km).toInt(), m) else null), Modifier.weight(1f), unidad = Unidades.etiquetaRitmo(m), tamano = 18)
                        }
                    }
                }
            }
            dias.forEach { d -> FilaDia(d, m, onCorrer, onCarrera) }
        }
    }
}

@Composable
private fun FilaDia(d: DiaSemana, m: Boolean, onCorrer: () -> Unit, onCarrera: (String) -> Unit) {
    val nombre = "${DIAS[d.indice]} ${Fechas.fecha(d.inicioMs).dayOfMonth}"
    when (d.estado) {
        EstadoDia.Corrido -> Tarjeta(
            modifier = Modifier.testTag("dia-${d.indice}"),
            borde = GColors.C2Violeta.copy(alpha = 0.5f),
            onClick = { onCarrera(d.carreras.last().id) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = GColors.C2Menta, modifier = Modifier.size(20.dp))
                Text(nombre, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                if (d.carreras.size > 1) Text("${d.carreras.size} carreras", style = GType.labelMd, color = GColors.C2Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CeldaTablero("Distancia", dec1(Unidades.distancia(d.km, m)), Modifier.weight(1f), unidad = Unidades.etiquetaDistancia(m), color = GColors.C2Violeta, tamano = 18)
                CeldaTablero("Tiempo", formatearHms(d.segundos), Modifier.weight(1f), tamano = 16)
                CeldaTablero("Ritmo", formatearRitmo(d.ritmoSegKm?.let { Unidades.ritmo(it, m) }), Modifier.weight(1f), tamano = 16)
                CeldaTablero("Subida", "+${d.desnivelM}", Modifier.weight(1f), unidad = "m", color = GColors.C2Cielo, tamano = 16)
            }
        }
        EstadoDia.Hoy -> Tarjeta(modifier = Modifier.testTag("dia-${d.indice}"), borde = GColors.C2Violeta) {
            EstadoVacioC2(
                Icons.AutoMirrored.Filled.DirectionsRun, "$nombre · Hoy", "Aún estás a tiempo: una salida corta también suma a tu racha.",
                accion = "Correr", onAccion = onCorrer, compacto = true, tag = "dia-hoy",
            )
        }
        EstadoDia.SinCarrera -> Tarjeta(modifier = Modifier.testTag("dia-${d.indice}")) {
            EstadoVacioC2(
                Icons.Filled.HourglassEmpty, nombre, "Este día no corriste. El próximo es una nueva oportunidad.",
                compacto = true, acento = GColors.C2Muted, tag = "dia-sin-carrera",
            )
        }
        EstadoDia.Futuro -> Box(Modifier.alpha(0.55f)) {
            Tarjeta(modifier = Modifier.testTag("dia-${d.indice}"), borde = null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GColors.C2S2), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.EventAvailable, null, tint = GColors.C2Muted, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(nombre, style = GType.labelLg, color = GColors.TextPrimary)
                        Text("Este día aún no llega.", style = GType.bodySm, color = GColors.C2Muted)
                    }
                    Box(Modifier.width(4.dp))
                }
            }
        }
    }
}
