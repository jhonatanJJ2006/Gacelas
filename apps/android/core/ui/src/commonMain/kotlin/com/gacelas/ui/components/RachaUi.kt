package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.reglas.Racha
import com.gacelas.actividad.domain.reglas.Rachas
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private val Aviso = Color(0xFFA855F7)

private fun plural(n: Int, uno: String, varios: String) = "$n ${if (n == 1) uno else varios}"

@Composable
fun InsigniaRacha(r: Racha, modifier: Modifier = Modifier) {
    val riesgo = Rachas.enRiesgo(r, com.gacelas.shared.ahoraMs())
    Row(
        modifier.clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.75f))
            .border(1.dp, if (r.diaria > 0) GColors.Primary500.copy(alpha = 0.5f) else Color.Transparent, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = if (r.diaria > 0) GColors.Primary500 else GColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(" Racha ${r.diaria}d", style = GType.labelMd, color = GColors.TextPrimary)
        if (riesgo) Text(" · en riesgo", style = GType.labelMd, color = Aviso)
    }
}

@Composable
fun TarjetaRacha(r: Racha, carreras: List<Carrera>) {
    val ahora = com.gacelas.shared.ahoraMs()
    val semanas = Rachas.mapaCalor(carreras, ahora)
    val max = maxOf(1.0, semanas.flatten().maxOf { it.km })
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = GColors.Primary500)
            Text(" Rachas", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            if (Rachas.enRiesgo(r, ahora)) Text("Corre hoy para no perderla", style = GType.labelMd, color = Aviso)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Bloque(Modifier.weight(1f), "Racha diaria", plural(r.diaria, "día", "días"), "Mejor: ${plural(r.mejorDiaria, "día", "días")}", GColors.Primary500)
            Bloque(Modifier.weight(1f), "Racha semanal", plural(r.semanal, "semana", "semanas"), "Semana ${minOf(r.carrerasSemana, r.metaSemanal)}/${r.metaSemanal} · Mejor: ${r.mejorSemanal}", GColors.Secondary500)
        }
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = "Actividad de las últimas 12 semanas" },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            semanas.forEach { s ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    s.forEach { c ->
                        val color = when {
                            c.futuro -> Color.Transparent
                            c.km == 0.0 -> GColors.SurfaceHighest
                            c.km < max / 3 -> GColors.Primary500.copy(alpha = 0.35f)
                            c.km < 2 * max / 3 -> GColors.Primary500.copy(alpha = 0.65f)
                            else -> GColors.Primary500
                        }
                        Spacer(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    }
                }
            }
        }
    }
}

@Composable
private fun Bloque(modifier: Modifier, titulo: String, valor: String, detalle: String, color: Color) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp)) {
        Text(titulo, style = GType.labelMd, color = GColors.TextSecondary)
        Text(valor, style = GType.titleMd, color = color)
        Text(detalle, style = GType.labelMd, color = GColors.TextSecondary)
    }
}

@Composable
fun CelebracionRacha(antes: Racha, despues: Racha) {
    val diaria = despues.diaria > antes.diaria
    val semanal = despues.semanal > antes.semanal
    if (!diaria && !semanal) return
    Tarjeta(fondo = GColors.Primary500.copy(alpha = 0.15f), borde = GColors.Primary500) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = GColors.Primary500, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                if (diaria) Text("¡Racha de ${plural(despues.diaria, "día", "días")}!", style = GType.titleMd, color = GColors.TextPrimary)
                if (semanal) Text("Meta semanal cumplida: ${plural(despues.semanal, "semana", "semanas")} seguidas", style = GType.bodyMd, color = GColors.TextPrimary)
                Text("Guarda la carrera para sumarla.", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
    }
}
