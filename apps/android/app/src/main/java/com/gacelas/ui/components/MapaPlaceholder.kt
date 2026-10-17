package com.gacelas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gacelas.ui.theme.GColors

/**
 * Mapa estatico dibujado con Canvas (misma maqueta SVG que la web). Se usa cuando no hay
 * el mapa nativo (pruebas JVM o vista previa, ver [MapaGacelas]).
 */
@Composable
fun MapaPlaceholder(modifier: Modifier = Modifier, rutaPrincipal: Boolean? = true) {
    Canvas(modifier.fillMaxSize()) {
        val sx = size.width / 600f
        val sy = size.height / 900f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawRect(Color(0xFF090F1A))
        val paso = 40f * sx
        var x = 0f
        while (x < size.width) { drawLine(Color(0xFF162238), Offset(x, 0f), Offset(x, size.height), 1f); x += paso }
        var y = 0f
        while (y < size.height) { drawLine(Color(0xFF162238), Offset(0f, y), Offset(size.width, y), 1f); y += paso }
        val rio = Path().apply {
            moveTo(330 * sx, 0f)
            cubicTo(310 * sx, 220 * sy, 340 * sx, 380 * sy, 290 * sx, 560 * sy)
            cubicTo(260 * sx, 680 * sy, 280 * sx, 800 * sy, 270 * sx, 900 * sy)
        }
        drawPath(rio, Color(0xFF1E3A5F), alpha = 0.7f, style = Stroke(12f * sx, cap = StrokeCap.Round))
        val parque = Path().apply {
            moveTo(280 * sx, 110 * sy)
            cubicTo(380 * sx, 100 * sy, 450 * sx, 150 * sy, 430 * sx, 230 * sy)
            cubicTo(380 * sx, 280 * sy, 300 * sx, 270 * sy, 260 * sx, 210 * sy)
            close()
        }
        drawPath(parque, GColors.MapParks, alpha = 0.5f)
        drawPath(parque, GColors.Secondary500, style = Stroke(1.5f))
        when (rutaPrincipal) {
            true -> {
                val r = Path().apply {
                    moveTo(330 * sx, 210 * sy)
                    cubicTo(310 * sx, 260 * sy, 270 * sx, 320 * sy, 280 * sx, 380 * sy)
                    cubicTo(290 * sx, 450 * sy, 270 * sx, 520 * sy, 310 * sx, 570 * sy)
                    cubicTo(330 * sx, 590 * sy, 350 * sx, 610 * sy, 380 * sx, 620 * sy)
                }
                drawPath(r, GColors.Primary, style = Stroke(8f * sx, cap = StrokeCap.Round))
            }
            false -> {
                val r = Path().apply {
                    moveTo(250 * sx, 220 * sy)
                    cubicTo(240 * sx, 320 * sy, 220 * sx, 450 * sy, 250 * sx, 560 * sy)
                    cubicTo(270 * sx, 610 * sy, 320 * sx, 620 * sy, 370 * sx, 630 * sy)
                }
                drawPath(r, GColors.Secondary500, style = Stroke(8f * sx, cap = StrokeCap.Round))
            }
            null -> Unit
        }
        drawCircle(GColors.Primary, 12f * sx, p(330f, 210f))
        drawCircle(GColors.Secondary500, 10f * sx, p(380f, 620f))
    }
}
