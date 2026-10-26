package com.gacelas.correr

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.ui.theme.GColors

/** Dibuja el recorrido GPS normalizado al tamano disponible. */
@Composable
fun TrazadoRuta(puntos: List<PuntoGps>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (puntos.size < 2) return@Canvas
        val minLat = puntos.minOf { it.lat }
        val maxLat = puntos.maxOf { it.lat }
        val minLng = puntos.minOf { it.lng }
        val maxLng = puntos.maxOf { it.lng }
        val rango = maxOf(maxLat - minLat, maxLng - minLng, 1e-6)
        val margen = 16f
        val escala = (minOf(size.width, size.height) - 2 * margen) / rango.toFloat()
        val dx = (size.width - ((maxLng - minLng).toFloat() * escala)) / 2
        val dy = (size.height - ((maxLat - minLat).toFloat() * escala)) / 2
        fun o(p: PuntoGps) = Offset(dx + ((p.lng - minLng).toFloat() * escala), size.height - (dy + ((p.lat - minLat).toFloat() * escala)))
        val path = Path().apply {
            moveTo(o(puntos.first()).x, o(puntos.first()).y)
            puntos.drop(1).forEach { lineTo(o(it).x, o(it).y) }
        }
        drawPath(path, GColors.Primary, style = Stroke(6f, cap = StrokeCap.Round))
        drawCircle(GColors.Secondary500, 8f, o(puntos.first()))
        drawCircle(GColors.Danger, 8f, o(puntos.last()))
    }
}

