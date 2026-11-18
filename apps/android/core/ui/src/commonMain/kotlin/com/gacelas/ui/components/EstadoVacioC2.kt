package com.gacelas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Estado vacio ilustrado: circulo tenue, icono grande, puntos de acento y un destello,
 * con titulo, texto y una accion opcional. `compacto` es la variante en fila para listas.
 */
@Composable
fun EstadoVacioC2(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: (() -> Unit)? = null,
    iconoAccion: ImageVector? = Icons.Filled.Add,
    secundaria: String? = null,
    onSecundaria: (() -> Unit)? = null,
    compacto: Boolean = false,
    acento: Color = GColors.C2Violeta,
    tag: String = "estado-vacio",
) {
    if (compacto) {
        Row(
            modifier.fillMaxWidth().testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IlustracionVacio(icono, 56.dp, acento)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = GType.labelLg, color = GColors.TextPrimary)
                Text(texto, style = GType.bodySm, color = GColors.C2Muted)
            }
            if (accion != null && onAccion != null) BotonChipC2(accion, onAccion, Modifier.testTag("$tag-accion"))
        }
        return
    }
    Column(
        modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IlustracionVacio(icono, 168.dp, acento)
        Spacer(Modifier.height(4.dp))
        Text(titulo, style = GType.headlineSm, color = GColors.TextPrimary, textAlign = TextAlign.Center)
        Text(texto, style = GType.bodyMd, color = GColors.C2Muted, textAlign = TextAlign.Center)
        if (accion != null && onAccion != null) {
            Spacer(Modifier.height(8.dp))
            BotonPrimario(accion, onAccion, Modifier.padding(horizontal = 24.dp).testTag("$tag-accion"), icono = iconoAccion)
        }
        if (secundaria != null && onSecundaria != null) {
            BotonSecundario(secundaria, onSecundaria, Modifier.testTag("$tag-secundaria"), color = GColors.Primary400)
        }
    }
}

/** Ilustracion de las referencias: circulo de fondo, icono, puntos y destello de 4 puntas. */
@Composable
fun IlustracionVacio(icono: ImageVector, tam: Dp, acento: Color = GColors.C2Violeta) {
    Box(Modifier.size(tam), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(GColors.C2Morado.copy(alpha = 0.2f), r * 0.78f, c)
            drawCircle(acento.copy(alpha = 0.35f), r * 0.78f, c, style = Stroke(width = r * 0.025f))
            drawCircle(GColors.C2Cielo, r * 0.06f, Offset(c.x - r * 0.82f, c.y - r * 0.62f))
            drawCircle(GColors.C2Menta, r * 0.045f, Offset(c.x + r * 0.9f, c.y + r * 0.1f))
            drawCircle(GColors.C2Cielo.copy(alpha = 0.6f), r * 0.03f, Offset(c.x + r * 0.55f, c.y - r * 0.92f))
            destello(Offset(c.x - r * 0.88f, c.y + r * 0.6f), r * 0.14f, GColors.C2Cielo)
            destello(Offset(c.x + r * 0.78f, c.y - r * 0.55f), r * 0.09f, GColors.C2Menta)
        }
        Box(
            Modifier.size(tam * 0.5f).clip(CircleShape).background(GColors.C2Card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, null, tint = acento, modifier = Modifier.size(tam * 0.28f))
        }
    }
}

private fun DrawScope.destello(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
