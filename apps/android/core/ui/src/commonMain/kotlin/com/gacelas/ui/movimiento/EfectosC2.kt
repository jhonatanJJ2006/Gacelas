package com.gacelas.ui.movimiento

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.gacelas.ui.theme.GColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Feedback de pulsacion C2: el elemento se encoge a [MovimientoC2.ESCALA_PULSADO] mientras se
 * mantiene pulsado. Solo toca graphicsLayer (sin relayout) y no retrasa el clic.
 */
fun Modifier.pulsacionC2(interaccion: InteractionSource): Modifier = composed {
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pulsado) MovimientoC2.ESCALA_PULSADO else 1f, Mov.corto(), label = "pulsacion")
    graphicsLayer { scaleX = escala; scaleY = escala }
}

/**
 * Entrada escalonada: fundido + subida corta al aparecer por primera vez. [indice] es la posicion
 * del elemento en su lista; a partir de [MovimientoC2.ESCALONES_MAX] ya no espera mas. Con
 * animaciones quitadas el elemento aparece directamente. No bloquea toques: el elemento es
 * interactivo desde el primer cuadro.
 */
fun Modifier.entradaEscalonada(indice: Int = 0): Modifier = composed {
    val escala = Mov.escala
    if (escala <= 0f) return@composed this
    val progreso = remember { Animatable(0f) }
    val desplazamiento = with(LocalDensity.current) { MovimientoC2.DESPLAZAMIENTO_DP.dp2px(density) }
    LaunchedEffect(Unit) {
        progreso.animateTo(1f, MovimientoC2.spec(MovimientoC2.LARGO, escala, MovimientoC2.Entrar, MovimientoC2.retraso(indice, escala)))
    }
    graphicsLayer {
        val p = progreso.value
        alpha = p
        translationY = (1f - p) * desplazamiento
    }
}

private fun Int.dp2px(densidad: Float) = this * densidad

/**
 * Barra de progreso que se llena desde 0 al aparecer (y anima los cambios). El relleno usa
 * fillMaxWidth(fraccion) leido en la fase de layout de un Box pequeno: es barato.
 */
@Composable
fun BarraLlenadoC2(fraccion: Float, color: Color, modifier: Modifier, forma: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape) {
    val escala = Mov.escala
    val valor = remember { Animatable(if (escala <= 0f) fraccion.coerceIn(0f, 1f) else 0f) }
    LaunchedEffect(fraccion) {
        valor.animateTo(fraccion.coerceIn(0f, 1f), MovimientoC2.spec(MovimientoC2.CONTEO, escala, MovimientoC2.Estandar, MovimientoC2.CORTO))
    }
    Box(modifier) {
        Box(Modifier.fillMaxWidth(valor.value).fillMaxHeight().clip(forma).background(color).testTag("barra-llenado"))
    }
}

/**
 * Cifra que cuenta hacia arriba desde 0 hasta [objetivo] (resumen de carrera). [formato] convierte
 * el valor intermedio en texto. Con animaciones quitadas muestra el valor final directamente.
 */
@Composable
fun CifraAnimada(objetivo: Double, formato: (Double) -> String, estilo: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val escala = Mov.escala
    val valor = remember { Animatable(if (escala <= 0f) objetivo.toFloat() else 0f) }
    LaunchedEffect(objetivo) {
        valor.animateTo(objetivo.toFloat(), MovimientoC2.spec(MovimientoC2.CONTEO, escala, MovimientoC2.Entrar))
    }
    // Al terminar se muestra el valor exacto (sin perdida de precision del Float).
    val texto = if (valor.isRunning || valor.value != objetivo.toFloat()) formato(valor.value.toDouble()) else formato(objetivo)
    Text(texto, style = estilo, color = color, modifier = modifier)
}

/** Particulas del destello: posicion angular y color fijos, calculados una vez. */
internal data class ParticulaC2(val angulo: Float, val alcance: Float, val color: Color, val giro: Float)

internal fun particulasC2(n: Int = 28): List<ParticulaC2> {
    val colores = listOf(GColors.C2Violeta, GColors.C2Morado, GColors.C2Menta, Color.White)
    return List(n) { i ->
        val a = (i.toFloat() / n) * 2f * PI.toFloat() + (i % 3) * 0.13f
        ParticulaC2(a, 0.55f + (i * 37 % 45) / 100f, colores[i % colores.size], (i * 53 % 360).toFloat())
    }
}

/**
 * Destello C2 al terminar una carrera o batir un record: confeti sesgado (paralelogramos) que sale
 * del centro y se desvanece. Dibujado en un Canvas (una sola capa, sin composables por particula).
 * No intercepta toques. Con animaciones quitadas no se dibuja.
 */
@Composable
fun DestelloC2(modifier: Modifier = Modifier, activo: Boolean = true) {
    val escala = Mov.escala
    if (!activo || escala <= 0f) return
    val progreso = remember { Animatable(0f) }
    val particulas = remember { particulasC2() }
    LaunchedEffect(Unit) { progreso.animateTo(1f, MovimientoC2.spec(1200, escala, LinearEasing)) }
    if (progreso.value >= 1f) return
    Canvas(modifier.fillMaxSize().testTag("destello-c2")) {
        val p = progreso.value
        val centro = Offset(size.width / 2f, size.height * 0.3f)
        val radio = size.minDimension * 0.6f
        val salida = 1f - (1f - p) * (1f - p) * (1f - p)
        val alfa = if (p < 0.6f) 1f else (1f - (p - 0.6f) / 0.4f)
        particulas.forEach { q ->
            val r = radio * q.alcance * salida
            val pos = Offset(centro.x + cos(q.angulo) * r, centro.y + sin(q.angulo) * r + 120f * p * p)
            rotate(q.giro + 220f * p, pos) {
                drawRect(q.color.copy(alpha = alfa.coerceIn(0f, 1f)), topLeft = pos - Offset(7f, 3f), size = Size(14f, 6f))
            }
        }
    }
}

/** Capa para colocar el destello encima de una pantalla sin afectar su layout. */
@Composable
fun BoxScope.DestelloEncima(activo: Boolean) {
    DestelloC2(Modifier.matchParentSize(), activo)
}
