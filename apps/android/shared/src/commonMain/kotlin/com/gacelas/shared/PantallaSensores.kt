package com.gacelas.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.core.sensores.EstadoSensores
import com.gacelas.core.sensores.Rumbo
import com.gacelas.carreras.domain.FaseCarrera
import com.gacelas.tracking.formatearRitmo

/** Pantalla de prueba (etapa 3 de iOS): GPS cada 1 s, rumbo combinado y cronometro compartido. */
@Composable
fun PantallaSensores(e: EstadoSensores, onCarrera: () -> Unit = {}, onVolver: () -> Unit = {}) {
    val l = e.lectura
    Column(
        Modifier.fillMaxSize().background(ColoresC4.Fondo).safeDrawingPadding().padding(20.dp).testTag("sensores"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Inicio", color = ColoresC4.Secondary, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onVolver))
            Spacer(Modifier.weight(1f))
            Text("GPS + BRÚJULA", color = ColoresC4.Magenta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { Rosa(e.rumbo) }
        Text(
            e.rumbo?.let { "${it.toInt()}° ${cardinal(it)}" } ?: "Rumbo: esperando…",
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("rumbo"),
        )
        val fuente = when {
            e.rumbo == null -> "sin datos"
            Rumbo.pesoGps(l?.velocidadMs) >= 1f -> "curso GPS (≥ ${Rumbo.VEL_GPS_MS} m/s)"
            Rumbo.pesoGps(l?.velocidadMs) <= 0f || e.rumboSensor != null && l?.cursoGrados == null -> "brújula (< ${Rumbo.VEL_SENSOR_MS} m/s)"
            else -> "mezcla brújula/GPS"
        }
        Text("Fuente: $fuente", color = ColoresC4.Secondary, fontSize = 13.sp)
        Tarjeta {
            Fila("Latitud", l?.lat?.let { decimales(it, 6) } ?: "—")
            Fila("Longitud", l?.lng?.let { decimales(it, 6) } ?: "—")
            Fila("Precisión", l?.let { "±${decimales(it.precisionM.toDouble(), 0)} m" } ?: "—")
            Fila("Velocidad", l?.velocidadMs?.let { "${decimales(it, 1)} m/s" } ?: "—")
            Fila("Lecturas", "${e.lecturas} (cada 1 s)")
        }
        val c = e.carrera
        Tarjeta {
            Fila("Carrera", when (c.fase) { FaseCarrera.Corriendo -> "en curso"; FaseCarrera.Pausada -> "pausada"; else -> "detenida" })
            Fila("Tiempo", c.tiempoTexto)
            Fila("Distancia", "${decimales(c.distanciaKm, 2)} km")
            Fila("Ritmo", "${formatearRitmo(c.ritmoActualSegKm ?: c.ritmoPromedioSegKm)} /km")
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(ColoresC4.Naranja).clickable(onClick = onCarrera),
            contentAlignment = Alignment.Center,
        ) { Text(if (c.activa) "Detener carrera" else "Iniciar carrera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}

fun cardinal(g: Float): String = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")[((Rumbo.normalizar(g) + 22.5f) / 45f).toInt() % 8]

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ColoresC4.Tarjeta)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { contenido() }
}

@Composable
private fun Fila(k: String, v: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(k, color = ColoresC4.Secondary, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(v, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Rosa(rumbo: Float?) {
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.12f), style = Stroke(2.dp.toPx()))
            for (i in 0 until 36) {
                val a = rad(i * 10.0 - 90)
                val r1 = size.minDimension / 2; val r2 = r1 - (if (i % 9 == 0) 16.dp else 8.dp).toPx()
                drawLine(Color.White.copy(alpha = if (i % 9 == 0) 0.7f else 0.25f),
                    Offset(center.x + r1 * kotlin.math.cos(a).toFloat(), center.y + r1 * kotlin.math.sin(a).toFloat()),
                    Offset(center.x + r2 * kotlin.math.cos(a).toFloat(), center.y + r2 * kotlin.math.sin(a).toFloat()), 2.dp.toPx())
            }
        }
        Text("N", color = ColoresC4.Magenta, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp))
        Canvas(Modifier.size(110.dp).rotate(rumbo ?: 0f)) {
            val p = Path().apply {
                moveTo(center.x, 0f); lineTo(center.x + size.width * 0.28f, size.height * 0.85f)
                lineTo(center.x, size.height * 0.68f); lineTo(center.x - size.width * 0.28f, size.height * 0.85f); close()
            }
            drawPath(p, if (rumbo == null) Color.Gray else ColoresC4.Naranja)
        }
    }
}
