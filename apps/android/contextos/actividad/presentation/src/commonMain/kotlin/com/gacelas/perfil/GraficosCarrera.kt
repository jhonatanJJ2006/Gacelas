package com.gacelas.perfil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.reglas.AnalisisCarrera
import com.gacelas.tracking.formatearMs
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Barras de ritmo: la mas alta es el kilometro mas rapido. */
@Composable
fun GraficoRitmo(parciales: List<Parcial>, modifier: Modifier = Modifier) {
    val km = AnalisisCarrera.parcialesPorKm(parciales)
    if (km.isEmpty()) return
    val ritmos = km.map { it.segundos / it.distanciaKm }
    val rapido = ritmos.min()
    val lento = ritmos.max()
    Tarjeta(modifier.fillMaxWidth().testTag("grafico-ritmo")) {
        Text("Ritmo por kilómetro", style = GType.titleMd, color = GColors.TextPrimary)
        Row(Modifier.fillMaxWidth().height(112.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            km.forEachIndexed { i, p ->
                val r = ritmos[i]
                val alto = if (lento - rapido < 1.0) 0.7f else (0.4f + 0.6f * ((lento - r) / (lento - rapido)).toFloat())
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatearMs(p.segundos), style = GType.labelMd, color = if (r == rapido) GColors.Primary500 else GColors.TextSecondary, maxLines = 1)
                    Canvas(Modifier.fillMaxWidth().height((72 * alto).dp)) {
                        drawRect(if (r == rapido) GColors.Primary500 else GColors.Primary500.copy(alpha = 0.7f))
                    }
                    Text("${p.numero}", style = GType.labelMd, color = GColors.TextSecondary)
                }
            }
        }
    }
}

/** Altura contra la distancia de la carrera. */
@Composable
fun GraficoElevacion(puntos: List<PuntoGps>, modifier: Modifier = Modifier) {
    val serie = AnalisisCarrera.serieElevacion(puntos)
    if (serie.size < 2) return
    val min = serie.minOf { it.altitudM }
    val max = serie.maxOf { it.altitudM }
    val rango = (max - min).coerceAtLeast(1.0)
    val total = serie.last().distanciaKm
    Tarjeta(modifier.fillMaxWidth().testTag("grafico-elevacion")) {
        Row {
            Text("Elevación", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${min.toInt()}–${max.toInt()} m · ${"%.1f".format(total)} km", style = GType.bodySm, color = GColors.TextSecondary)
        }
        Canvas(
            Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "Elevación entre ${min.toInt()} y ${max.toInt()} metros" },
        ) {
            val pts = serie.map {
                val x = if (total <= 0.0) 0f else (it.distanciaKm / total).toFloat() * size.width
                val y = size.height - 4f - ((it.altitudM - min) / rango).toFloat() * (size.height - 12f)
                Offset(x, y)
            }
            val linea = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(linea, GColors.Primary500, style = Stroke(width = 5f))
        }
    }
}
