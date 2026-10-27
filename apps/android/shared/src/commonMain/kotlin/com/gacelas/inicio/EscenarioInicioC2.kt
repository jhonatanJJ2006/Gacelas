package com.gacelas.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.Inter
import com.gacelas.ui.theme.SpaceGrotesk
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Colores del escenario de Inicio, copiados del frame "Movil / Inicio / Inicio" del Figma C2
 * (DPulop8pgEH1Pn7Fb9nhef, clase .stage del prototipo m-inicio.html).
 */
internal object EscenarioC2 {
    val Alto = 430.dp
    /** linear-gradient(160deg, #191F2F, #0C1322) */
    val FondoDesde = Color(0xFF191F2F)
    val FondoHasta = Color(0xFF0C1322)
    /** radial-gradient(60% 55% at 55% 55%, #7B1FA2AA 0, #4A154B66 45%, #0C132200 75%) */
    val Morado = Color(0xAA7B1FA2)
    val Ciruela = Color(0x664A154B)
    val Transparente = Color(0x000C1322)
    /** repeating-linear-gradient(115deg, #C084FC14 0 2px, transparent 2px 38px) */
    val Raya = Color(0x14C084FC)
    /** radial-gradient(50% 50%, #C084FC55, transparent 70%) */
    val Piso = Color(0x55C084FC)
    /** -webkit-text-stroke 1.5px #C084FC40 */
    val Palabra = Color(0x40C084FC)
    val EtiquetaFondo = Color(0xCC0C1322)
    val EtiquetaBorde = Color(0xFF2C3446)
}

/** clip-path: polygon(0 0, 100% 6%, 100% 100%, 0 92%) */
internal val FormaEscenarioC2 = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, size.height * 0.06f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height * 0.92f)
    close()
}

/** Fondo del escenario: degradado lineal 160deg + halo radial morado + rayas a 115deg + brillo del piso. */
@Composable
internal fun FondoEscenarioC2(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().testTag("inicio-escenario-fondo")) {
        val w = size.width
        val h = size.height
        // Angulo CSS: 0deg apunta arriba y gira en sentido horario.
        fun lineaCss(grados: Double): Pair<Offset, Offset> {
            val r = grados * PI / 180
            val dx = sin(r).toFloat()
            val dy = -cos(r).toFloat()
            val mitad = (abs(w * dx) + abs(h * dy)) / 2f
            val c = Offset(w / 2f, h / 2f)
            return (c - Offset(dx, dy) * mitad) to (c + Offset(dx, dy) * mitad)
        }
        val (a, b) = lineaCss(160.0)
        drawRect(Brush.linearGradient(listOf(EscenarioC2.FondoDesde, EscenarioC2.FondoHasta), a, b))
        // Elipse de radios 60% x 55% centrada en (55%, 55%): se dibuja circular y se escala en vertical.
        val centro = Offset(w * 0.55f, h * 0.55f)
        val rx = w * 0.60f
        val ry = h * 0.55f
        scale(1f, ry / rx, pivot = centro) {
            drawCircle(
                Brush.radialGradient(
                    0f to EscenarioC2.Morado, 0.45f to EscenarioC2.Ciruela, 0.75f to EscenarioC2.Transparente, 1f to EscenarioC2.Transparente,
                    center = centro, radius = rx,
                ),
                radius = rx * 1.5f, center = centro,
            )
        }
        // Rayas: 2px de color cada 38px, perpendiculares a la direccion 115deg.
        val diag = kotlin.math.sqrt(w * w + h * h)
        clipRect {
            rotate(115f - 90f, pivot = Offset(w / 2f, h / 2f)) {
                val paso = 38.dp.toPx()
                val ancho = 2.dp.toPx()
                var x = w / 2f - diag
                while (x < w / 2f + diag) {
                    drawRect(EscenarioC2.Raya, topLeft = Offset(x, h / 2f - diag), size = Size(ancho, diag * 2))
                    x += paso
                }
            }
        }
        // Piso: left/right -10%, bottom 30px, alto 90px; radial 50% 50% a transparente en 70%.
        val pisoAlto = 90.dp.toPx()
        val pisoCentro = Offset(w / 2f, h - 30.dp.toPx() - pisoAlto / 2f)
        val pisoRx = w * 1.2f / 2f
        scale(1f, (pisoAlto / 2f) / pisoRx, pivot = pisoCentro) {
            drawCircle(
                Brush.radialGradient(0f to EscenarioC2.Piso, 0.7f to Color.Transparent, center = pisoCentro, radius = pisoRx),
                radius = pisoRx, center = pisoCentro,
            )
        }
    }
}

/** Etiqueta .tg del escenario: rotulo pequeno en mayusculas y valor grande violeta, inclinada -10deg. */
@Composable
internal fun EtiquetaEscenarioC2(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier.sesgoX(-10f)
            .background(EscenarioC2.EtiquetaFondo).border(1.dp, EscenarioC2.EtiquetaBorde)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(rotulo.uppercase(), style = TextStyle(fontFamily = Inter, fontStyle = FontStyle.Italic, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp), color = Color.White)
        Text(valor, style = TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 22.sp), color = GColors.C2Violeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** transform: skewX(grados), respecto al centro como en CSS. Solo afecta al dibujo, no a la medida. */
internal fun Modifier.sesgoX(grados: Float): Modifier = this.then(
    Modifier.drawWithContent {
        val t = kotlin.math.tan(grados * PI / 180).toFloat()
        drawContext.canvas.save()
        drawContext.canvas.translate(-t * size.height / 2f, 0f)
        // Matriz explicita: Canvas.skew de Compose recibe la tangente en Android y grados en Skia.
        drawContext.canvas.concat(androidx.compose.ui.graphics.Matrix().apply { values[androidx.compose.ui.graphics.Matrix.SkewX] = t })
        drawContent()
        drawContext.canvas.restore()
    },
)

/**
 * Escenario de Inicio C2: a todo el ancho (anula el margen lateral de la pantalla), 430dp de alto,
 * borde superior e inferior en diagonal, palabra "CORRE LOJA" en contorno y el boton de empezar abajo.
 */
@Composable
fun EscenarioInicioC2(
    etiquetaIzq: String,
    valorIzq: String,
    etiquetaDer: String,
    valorDer: String,
    onCorrer: () -> Unit,
    margenLateral: Dp,
    corredor3D: @Composable BoxScope.() -> Unit,
) {
    Box(
        Modifier
            .layout { medible, restricciones ->
                val extra = (margenLateral * 2).roundToPx()
                val ancho = restricciones.maxWidth + extra
                val p = medible.measure(restricciones.copy(minWidth = ancho, maxWidth = ancho))
                layout(restricciones.maxWidth, p.height) { p.place(-extra / 2, 0) }
            }
            .fillMaxWidth().height(EscenarioC2.Alto).clip(FormaEscenarioC2).testTag("inicio-escenario"),
    ) {
        FondoEscenarioC2()
        val trazo = with(LocalDensity.current) { 1.5.dp.toPx() }
        Text(
            "CORRE LOJA",
            style = TextStyle(
                fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 120.sp, lineHeight = 96.sp,
                drawStyle = Stroke(width = trazo),
            ),
            color = EscenarioC2.Palabra, maxLines = 1, softWrap = false,
            modifier = Modifier.align(Alignment.BottomStart).wrapContentWidth(Alignment.Start, unbounded = true).offset(x = (-6).dp).padding(bottom = 24.dp),
        )
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.84f)) { corredor3D() }
        EtiquetaEscenarioC2(etiquetaIzq, valorIzq, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 40.dp).testTag("escenario-etiqueta-izq"))
        EtiquetaEscenarioC2(etiquetaDer, valorDer, Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 96.dp).testTag("escenario-etiqueta-der"))
        com.gacelas.ui.components.BotonPrimario(
            "Empezar a correr", onCorrer, icono = Icons.Filled.PlayArrow,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 30.dp).fillMaxWidth(),
        )
    }
}
