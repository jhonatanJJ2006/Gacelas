package com.gacelas.ajustes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.core.sensores.rumboSensorState
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.components.EtiquetaC2
import com.gacelas.ui.components.FormaC2
import com.gacelas.ui.components.MetricaC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlin.math.roundToInt

/** Punto cardinal abreviado para un rumbo en grados. */
internal fun puntoCardinal(grados: Float): String {
    val nombres = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return nombres[(((grados % 360 + 360) % 360 + 22.5f) / 45f).toInt() % 8]
}

/** Brujula C2: rosa de los vientos que gira con el rumbo del telefono y datos en tarjetas pequenas. */
@Composable
fun BrujulaScreen(onAtras: () -> Unit) {
    val rumbo by rumboSensorState(activo = true)
    var calibrando by remember { mutableStateOf(false) }
    PantallaConEncabezado("Brújula", onAtras) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).aspectRatio(1f).testTag("rosa-brujula"), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).rotate(-(rumbo ?: 0f))) {
                val r = size.minDimension / 2
                drawCircle(GColors.C2Card, r)
                drawCircle(GColors.C2Linea, r, style = Stroke(2.dp.toPx()))
                for (k in 0 until 72) rotate(k * 5f) {
                    val largo = if (k % 18 == 0) 18.dp.toPx() else if (k % 2 == 0) 10.dp.toPx() else 6.dp.toPx()
                    drawLine(if (k % 18 == 0) GColors.C2Violeta else GColors.C2Muted.copy(alpha = 0.6f), Offset(center.x, center.y - r + 6.dp.toPx()), Offset(center.x, center.y - r + 6.dp.toPx() + largo), 2.dp.toPx())
                }
            }
            // Aguja fija: siempre apunta hacia arriba (direccion del telefono).
            Canvas(Modifier.fillMaxWidth(0.5f).aspectRatio(1f)) {
                val w = size.width
                val norte = Path().apply { moveTo(w / 2, 0f); lineTo(w / 2 + w * 0.1f, w / 2); lineTo(w / 2 - w * 0.1f, w / 2); close() }
                val sur = Path().apply { moveTo(w / 2, w); lineTo(w / 2 + w * 0.1f, w / 2); lineTo(w / 2 - w * 0.1f, w / 2); close() }
                drawPath(norte, GColors.C2Violeta)
                drawPath(sur, GColors.C2S2)
                drawCircle(GColors.TextPrimary, w * 0.04f)
            }
            Text("N", style = GType.titleMd.copy(fontWeight = FontWeight.Black), color = GColors.C2Violeta, modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp))
        }
        val texto = rumbo?.let { "${it.roundToInt()}° ${puntoCardinal(it)}" } ?: "Sin sensor"
        Text(texto, style = GType.headlineXl.copy(fontSize = 40.sp, fontWeight = FontWeight.Black), color = GColors.C2Violeta, modifier = Modifier.fillMaxWidth().testTag("rumbo"), textAlign = TextAlign.Center)
        Text(if (rumbo == null) "Este teléfono no tiene brújula" else "Rumbo respecto al norte verdadero", style = GType.bodySm, color = GColors.C2Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricaC2(rumbo?.let { "${it.roundToInt()}°" } ?: "--", "Rumbo", Modifier.weight(1f), color = GColors.C2Violeta)
            MetricaC2(rumbo?.let { puntoCardinal(it) } ?: "--", "Dirección", Modifier.weight(1f), color = GColors.C2Menta)
            MetricaC2(if (rumbo == null) "--" else "Buena", "Calibración", Modifier.weight(1f))
        }
        BotonPrimario("Calibrar brújula", { calibrando = true }, icono = null)
    }
    if (calibrando) {
        DialogoC2(
            onDismissRequest = { calibrando = false },
            confirmButton = { BotonModalC2("Listo", { calibrando = false }) },
            title = { Text("Calibrar brújula") },
            text = { Text("Mueve el teléfono dibujando un ocho en el aire tres veces, lejos de objetos metálicos.") },
        )
    }
}

/** Mapas offline C2: almacenamiento usado y la tarjeta de descarga del mapa de Loja. */
@Composable
fun MapasOfflineScreen(onAtras: () -> Unit) {
    PantallaConEncabezado("Mapas offline", onAtras) {
        Column(Modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaC2("Almacenamiento")
            Text("Los mapas descargados se guardan en el teléfono y sirven para correr sin señal.", style = GType.bodySm, color = GColors.C2Muted)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(GColors.C2S2))
        }
        EtiquetaC2("Descargados", Modifier.padding(top = 8.dp))
        MapaOfflineAjuste()
        Text("Consejo: descarga la zona antes de salir a rutas de montaña como Podocarpus o Villonaco.", style = GType.bodySm, color = GColors.C2Muted)
    }
}
