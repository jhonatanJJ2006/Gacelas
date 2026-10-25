package com.gacelas.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.reglas.InicioRuta
import com.gacelas.carreras.domain.reglas.PeriodoReporte
import com.gacelas.carreras.domain.reglas.ReportePersonal
import com.gacelas.carreras.domain.reglas.ReportesPersonales
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.ui.carrerasState
import com.gacelas.ui.compartir
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Inicios de las rutas oficiales de Loja para detectar rutas favoritas. */
val iniciosRutasLoja: List<InicioRuta> by lazy {
    RutasLoja.todas.mapNotNull { r -> r.puntos.firstOrNull()?.let { InicioRuta(r.id, r.nombre, it.first, it.second) } }
}

@Composable
fun MisReportesScreen(onAtras: () -> Unit) {
    val carreras by carrerasState()
    val context = LocalContext.current
    MisReportesContenido(carreras, System.currentTimeMillis(), onAtras, onCompartir = { context.compartir(it) })
}

/** Reporte personal C2: semana, mes y año con comparacion, mejores marcas, racha y rutas favoritas. */
@Composable
fun MisReportesContenido(carreras: List<Carrera>, ahoraMs: Long, onAtras: () -> Unit, onCompartir: (String) -> Unit) {
    var periodo by remember { mutableStateOf(PeriodoReporte.Semana) }
    val r = remember(carreras, periodo, ahoraMs) { ReportesPersonales.calcular(carreras, periodo, ahoraMs, iniciosRutasLoja) }
    Column(Modifier.fillMaxSize().background(GColors.C2Bg)) {
        EncabezadoAtras("Mis reportes", onAtras) {
            IconButton(onClick = { onCompartir(ReportesPersonales.textoCompartir(r)) }, modifier = Modifier.testTag("reporte-compartir")) {
                Icon(Icons.Filled.Share, "Compartir reporte", tint = GColors.C2Violeta)
            }
            IconButton(onClick = { onCompartir(ReportesPersonales.csv(r)) }, modifier = Modifier.testTag("reporte-csv")) {
                Icon(Icons.Filled.TableChart, "Exportar CSV", tint = GColors.C2Violeta)
            }
        }
        Pantalla(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoReporte.entries.forEach { p -> Pastilla(p.etiqueta, p == periodo, { periodo = p }) }
            }
            ResumenReporte(r)
            GraficoKm(r)
            Tarjeta(fondo = GColors.C2Card, borde = GColors.C2Linea) {
                Text("MEJORES MARCAS", style = GType.labelMd, color = GColors.C2Muted)
                Row(Modifier.fillMaxWidth()) {
                    Dato("${ReportesPersonales.formato1(r.masLargaKm)} km", "Más larga", Modifier.weight(1f))
                    Dato("${ReportesPersonales.ritmoTexto(r.mejorRitmoSegKm)} /km", "Mejor ritmo", Modifier.weight(1f))
                    Dato("${r.masDesnivelM} m", "Más desnivel", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    Dato(dias(r.rachaActual), "Racha actual", Modifier.weight(1f))
                    Dato(dias(r.mejorRacha), "Mejor racha", Modifier.weight(1f))
                }
            }
            Tarjeta(fondo = GColors.C2Card, borde = GColors.C2Linea, modifier = Modifier.testTag("rutas-favoritas")) {
                Text("RUTAS FAVORITAS", style = GType.labelMd, color = GColors.C2Muted)
                if (r.rutasFavoritas.isEmpty()) Text("Corre una ruta oficial de Loja para verla aquí.", style = GType.bodySm, color = GColors.C2Muted)
                r.rutasFavoritas.forEach { (nombre, n) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(nombre, style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("$n ${if (n == 1) "vez" else "veces"}", style = GType.labelMd, color = GColors.C2Violeta)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenReporte(r: ReportePersonal) {
    Tarjeta(fondo = GColors.C2Card, borde = GColors.C2Linea, modifier = Modifier.testTag("reporte-resumen")) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(ReportesPersonales.formato1(r.actual.km), style = GType.headlineMd, color = GColors.TextPrimary)
            Text(" km", style = GType.labelMd, color = GColors.C2Muted, modifier = Modifier.padding(bottom = 4.dp))
            Box(Modifier.weight(1f))
            r.variacionKm?.let { v ->
                Text(
                    "${if (v >= 0) "+" else ""}${ReportesPersonales.formato1(v)}% vs. anterior", style = GType.labelMd,
                    color = if (v >= 0) GColors.C2Menta else GColors.C2Peligro,
                )
            } ?: Text("Sin periodo anterior", style = GType.labelMd, color = GColors.C2Muted)
        }
        Row(Modifier.fillMaxWidth()) {
            Dato("${r.actual.carreras}", "Carreras", Modifier.weight(1f))
            Dato(ReportesPersonales.duracionTexto(r.actual.segundos), "Tiempo", Modifier.weight(1f))
            Dato(ReportesPersonales.ritmoTexto(r.actual.ritmoSegKm), "Ritmo /km", Modifier.weight(1f))
            Dato("${r.actual.desnivelM} m", "Desnivel", Modifier.weight(1f))
        }
        Text("Anterior: ${ReportesPersonales.formato1(r.previo.km)} km en ${r.previo.carreras} carreras", style = GType.bodySm, color = GColors.C2Muted)
    }
}

@Composable
private fun GraficoKm(r: ReportePersonal) {
    val max = (r.serie.maxOfOrNull { it.km } ?: 0.0).coerceAtLeast(1.0)
    Tarjeta(fondo = GColors.C2Card, borde = GColors.C2Linea, modifier = Modifier.testTag("reporte-grafico")) {
        Text("KM POR ${if (r.periodo == PeriodoReporte.Anio) "MES" else "DÍA"}", style = GType.labelMd, color = GColors.C2Muted)
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            r.serie.forEach { b ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight((b.km / max).toFloat().coerceAtLeast(0.02f)).background(if (b.km > 0) GColors.C2Violeta else GColors.C2Linea))
                }
            }
        }
        if (r.serie.size <= 12) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            r.serie.forEach { Text(it.etiqueta, style = GType.bodySm, color = GColors.C2Muted, modifier = Modifier.weight(1f), maxLines = 1) }
        }
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(valor, style = GType.labelLg, color = GColors.TextPrimary, maxLines = 1)
        Text(etiqueta, style = GType.bodySm, color = GColors.C2Muted, maxLines = 1)
    }
}

private fun dias(n: Int) = "$n ${if (n == 1) "día" else "días"}"
