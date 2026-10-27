package com.gacelas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.ui.intro.Tinta
import com.gacelas.ui.intro.gacela
import com.gacelas.ui.theme.SpaceGrotesk

/**
 * Icono de marca A "Gacela en ruta" (docs/MARCA.md) dibujado en Compose: fondo de marca con halo
 * morado, ruta punteada con punto verde y la gacela con el degradado del icono de la app.
 * Sustituye al cuadro con el icono de corredor que se usaba antes como logo.
 */
@Composable
fun IconoGacelas(tam: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam).semantics { contentDescription = "Runcity" }) {
        val k = size.minDimension / 108f
        drawRoundRect(
            Brush.linearGradient(0f to Tinta.Violeta, .55f to Tinta.Fondo, 1f to Tinta.FondoProfundo, start = Offset.Zero, end = Offset(size.width, size.height)),
            cornerRadius = CornerRadius(24 * k),
        )
        drawRoundRect(Tinta.MoradoClaro.copy(alpha = .35f), cornerRadius = CornerRadius(24 * k), style = Stroke(maxOf(1f, 1.5f * k)))
        // En tamanos de cabecera se acerca el simbolo (1,25x) para que la gacela se lea.
        withTransform({ scale(1.25f, 1.25f, Offset(54 * k, 54 * k)) }) {
        drawCircle(Brush.radialGradient(0f to Tinta.MoradoOscuro.copy(alpha = .75f), 1f to Color.Transparent,
            center = Offset(54 * k, 48 * k), radius = 50 * k), 50 * k, Offset(54 * k, 48 * k))
        // Ruta punteada bajo la gacela.
        for (i in 0..9) {
            val u = i / 9f
            val x = (20 + u * 70) * k
            val y = (86 - kotlin.math.sin(u * 3.0).toFloat() * 8 - u * 6) * k
            drawCircle(Tinta.MoradoClaro.copy(alpha = .35f + .65f * u), 1.6f * k, Offset(x, y))
        }
        drawCircle(Tinta.Verde, 3.2f * k, Offset(90 * k, 74 * k))
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawPath(gacela, Brush.linearGradient(0f to Tinta.Lila, .4f to Tinta.MoradoClaro, .75f to Tinta.Morado, 1f to Tinta.MoradoOscuro,
                start = Offset(20f, 18f), end = Offset(90f, 78f)))
            drawPath(gacela, Tinta.Lila.copy(alpha = .25f), style = Stroke(.8f))
        }
        }
    }
}

/** Icono A con la palabra RUNCITY (como el final de la intro) y un subtitulo opcional. */
@Composable
fun MarcaGacelasA(
    tam: Dp = 44.dp,
    subtitulo: String? = "LOJA, ECUADOR",
    modifier: Modifier = Modifier,
    tamTexto: Float = 20f,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconoGacelas(tam)
        Column {
            Text("RUNCITY", style = TextStyle(fontFamily = SpaceGrotesk, fontSize = tamTexto.sp, fontWeight = FontWeight.Black, letterSpacing = .08.em), color = Color.White)
            if (subtitulo != null) Text(subtitulo, style = TextStyle(fontSize = (tamTexto * .45f).sp, fontWeight = FontWeight.SemiBold, letterSpacing = .14.em), color = Tinta.MoradoClaro)
        }
    }
}
