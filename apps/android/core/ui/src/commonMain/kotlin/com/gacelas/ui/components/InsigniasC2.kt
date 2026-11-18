package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Insignia de logro o medalla en una cuadricula. Bloqueada se ve como su estado vacio: silueta
 * apagada, candado pequeno y la barra de progreso hacia la meta. Siempre se puede tocar.
 */
@Composable
fun InsigniaC2(
    icono: ImageVector, titulo: String, subtitulo: String, lograda: Boolean, progreso: Float,
    onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = GColors.C2Violeta, tag: String = "insignia",
) {
    Tarjeta(modifier.testTag(tag), padding = 10.dp, onClick = onClick, borde = if (lograda) color.copy(alpha = 0.6f) else GColors.C2Linea) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(if (lograda) color.copy(alpha = 0.25f) else GColors.SurfaceHigh)
                        .border(2.dp, if (lograda) color else GColors.BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(icono, null, tint = if (lograda) color else GColors.C2Muted.copy(alpha = 0.55f), modifier = Modifier.size(28.dp)) }
                if (!lograda) Box(Modifier.size(20.dp).clip(CircleShape).background(GColors.C2Bg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, "Bloqueada", tint = GColors.C2Muted, modifier = Modifier.size(12.dp))
                }
            }
            Text(titulo, style = GType.labelMd, color = if (lograda) GColors.TextPrimary else GColors.C2Muted, textAlign = TextAlign.Center, maxLines = 2)
            if (!lograda) com.gacelas.ui.movimiento.BarraLlenadoC2(progreso.coerceIn(0f, 1f), color.copy(alpha = 0.7f), Modifier.fillMaxWidth().height(4.dp))
            Text(subtitulo, style = GType.bodySm, color = GColors.C2Muted, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Detalle de una insignia: icono grande, que es, como conseguirla, progreso X de Y y fecha si ya se gano. */
@Composable
fun HojaInsigniaC2(
    icono: ImageVector, titulo: String, descripcion: String, comoConseguirla: String,
    actual: Int, meta: Int, unidad: String, fecha: String?, lograda: Boolean, onCerrar: () -> Unit,
    color: Color = GColors.C2Violeta,
) {
    HojaC2(onCerrar, Modifier.testTag("hoja-insignia")) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.alpha(if (lograda) 1f else 0.7f)) {
                IlustracionVacio(icono, 150.dp, if (lograda) color else GColors.C2Muted)
            }
            Text(titulo, style = GType.headlineSm, color = GColors.TextPrimary, textAlign = TextAlign.Center)
            Text(descripcion, style = GType.bodyMd, color = GColors.C2Muted, textAlign = TextAlign.Center)
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (lograda) Icons.Filled.CheckCircle else Icons.Filled.Lock, null, tint = if (lograda) GColors.C2Menta else GColors.C2Muted, modifier = Modifier.size(20.dp))
                    Text(if (lograda) "Conseguida${fecha?.let { " el $it" } ?: ""}" else "Cómo conseguirla", style = GType.labelLg, color = GColors.TextPrimary)
                }
                if (!lograda) Text(comoConseguirla, style = GType.bodyMd, color = GColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Progreso", style = GType.labelMd, color = GColors.C2Muted, modifier = Modifier.weight(1f))
                    Text("$actual de $meta${if (unidad.isNotBlank()) " $unidad" else ""}", style = GType.labelLg, color = color, modifier = Modifier.testTag("insignia-progreso"))
                }
                com.gacelas.ui.movimiento.BarraLlenadoC2(if (meta > 0) (actual.toFloat() / meta).coerceIn(0f, 1f) else 0f, color, Modifier.fillMaxWidth().height(8.dp))
            }
            BotonPrimario(if (lograda) "Genial" else "Entendido", onCerrar, icono = null)
        }
    }
}
