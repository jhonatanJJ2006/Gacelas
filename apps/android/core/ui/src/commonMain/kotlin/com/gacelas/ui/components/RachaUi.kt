package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
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

private val Aviso get() = GColors.C2Morado
private val Fuego = Color(0xFFF97316)

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
fun TarjetaRacha(r: Racha, carreras: List<Carrera>, onClick: (() -> Unit)? = null) {
    val ahora = com.gacelas.shared.ahoraMs()
    val semanas = Rachas.mapaCalor(carreras, ahora)
    val max = maxOf(1.0, semanas.flatten().maxOf { it.km })
    Tarjeta(Modifier.testTag("tarjeta-racha"), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = GColors.Primary500)
            Text(" Rachas", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            if (Rachas.enRiesgo(r, ahora)) Text("Corre hoy para no perderla", style = GType.labelMd, color = Aviso)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Bloque(Modifier.weight(1f), "Racha diaria", plural(r.diaria, "día", "días"), "Mejor: ${plural(r.mejorDiaria, "día", "días")}", GColors.Primary500)
            Bloque(Modifier.weight(1f), "Racha semanal", plural(r.semanal, "semana", "semanas"), "Semana ${minOf(r.carrerasSemana, r.metaSemanal)}/${r.metaSemanal} · Mejor: ${r.mejorSemanal}", GColors.Secondary500)
        }
        MapaCalorRacha(semanas, max)
    }
}

@Composable
private fun MapaCalorRacha(semanas: List<List<com.gacelas.actividad.domain.reglas.CeldaCalor>>, max: Double) {
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

/** Hitos de racha diaria: el siguiente se muestra con su barra. */
val HITOS_RACHA = listOf(3, 7, 14, 30, 60, 100, 365)

/**
 * Hoja de racha al estilo de las rachas de TikTok: llama grande con el numero que late, la semana
 * con llamas encendidas o apagadas, el aviso si hoy aun no corriste, el proximo hito y el mapa de calor.
 */
@Composable
fun HojaRachaC2(r: Racha, carreras: List<Carrera>, onCerrar: () -> Unit, onCorrer: () -> Unit) {
    val ahora = com.gacelas.shared.ahoraMs()
    val semanas = Rachas.mapaCalor(carreras, ahora)
    val semana = semanas.last()
    val hoy = Rachas.inicioDia(ahora)
    val riesgo = Rachas.enRiesgo(r, ahora)
    val corrioHoy = semana.any { it.diaMs == hoy && it.km > 0 }
    HojaC2(onCerrar, modifier = Modifier.testTag("hoja-racha")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val pulso = androidx.compose.animation.core.rememberInfiniteTransition(label = "llama")
            val activo = com.gacelas.ui.movimiento.Mov.activo && r.diaria > 0
            val escala by pulso.animateFloat(
                1f, if (activo) 1.08f else 1f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "escala",
            )
            val tono = if (r.diaria > 0) Fuego else GColors.C2Muted
            Box(Modifier.size(150.dp).graphicsLayer { scaleX = escala; scaleY = escala }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(tono.copy(alpha = 0.35f), Color.Transparent))))
                Icon(Icons.Filled.LocalFireDepartment, null, tint = if (r.diaria > 0) Fuego else GColors.C2Muted.copy(alpha = 0.5f), modifier = Modifier.size(120.dp))
                Text(
                    "${r.diaria}", style = GType.headlineLg.copy(fontSize = androidx.compose.ui.unit.TextUnit(40f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.TextPrimary,
                    modifier = Modifier.padding(top = 30.dp).testTag("racha-numero"),
                )
            }
            Text(if (r.diaria == 1) "día de racha" else "días de racha", style = GType.headlineSm, color = GColors.TextPrimary)
            Text(
                when {
                    r.diaria == 0 -> "Corre hoy para encender tu racha."
                    corrioHoy -> "Hoy ya sumaste. Vuelve mañana para mantenerla."
                    riesgo -> "Tu racha se apaga a medianoche. Corre hoy para no perderla."
                    else -> "Vas en racha. Sigue así."
                },
                style = GType.bodyMd, color = if (riesgo) Fuego else GColors.C2Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            // Semana con llamas: encendida si corriste, contorno el dia de hoy.
            Row(Modifier.fillMaxWidth().testTag("racha-semana"), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, d ->
                    val c = semana[i]
                    val encendida = c.km > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (encendida) Fuego.copy(alpha = 0.2f) else GColors.C2S2)
                                .then(if (c.diaMs == hoy) Modifier.border(2.dp, if (encendida) Fuego else GColors.C2Violeta, CircleShape) else Modifier)
                                .semantics { contentDescription = "$d ${if (encendida) "corrido" else if (c.futuro) "por llegar" else "sin carrera"}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = if (encendida) Fuego else GColors.C2Muted.copy(alpha = if (c.futuro) 0.25f else 0.5f), modifier = Modifier.size(22.dp))
                        }
                        Text(d, style = GType.labelMd, color = if (c.diaMs == hoy) GColors.TextPrimary else GColors.C2Muted)
                    }
                }
            }
            if (!corrioHoy) BotonPrimario(if (r.diaria == 0) "Empezar mi racha" else "Correr hoy", onCorrer, Modifier.testTag("racha-correr"))
            val siguiente = HITOS_RACHA.firstOrNull { it > r.diaria } ?: HITOS_RACHA.last()
            Tarjeta(modifier = Modifier.testTag("racha-hito")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Próximo hito: $siguiente días", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("${r.diaria}/$siguiente", style = GType.labelLg, color = Fuego)
                }
                com.gacelas.ui.movimiento.BarraLlenadoC2((r.diaria.toFloat() / siguiente).coerceIn(0f, 1f), Fuego, Modifier.fillMaxWidth().height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HITOS_RACHA.take(5).forEach { h ->
                        val logrado = r.mejorDiaria >= h
                        Text(
                            "$h", style = GType.labelMd, color = if (logrado) GColors.TextPrimary else GColors.C2Muted,
                            modifier = Modifier.clip(CircleShape).background(if (logrado) Fuego else GColors.C2S2).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Bloque(Modifier.weight(1f), "Mejor racha", plural(r.mejorDiaria, "día", "días"), "Tu récord personal", Fuego)
                Bloque(Modifier.weight(1f), "Racha semanal", plural(r.semanal, "semana", "semanas"), "Semana ${minOf(r.carrerasSemana, r.metaSemanal)}/${r.metaSemanal} carreras", GColors.Secondary500)
            }
            Text("Últimas 12 semanas", style = GType.labelLg, color = GColors.C2Muted, modifier = Modifier.fillMaxWidth())
            MapaCalorRacha(semanas, maxOf(1.0, semanas.flatten().maxOf { it.km }))
        }
    }
}
