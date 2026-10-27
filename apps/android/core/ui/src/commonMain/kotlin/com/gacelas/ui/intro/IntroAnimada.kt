package com.gacelas.ui.intro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.gacelas.ui.theme.SpaceGrotesk
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/*
 * Intro de marca "Ruta y destello" (propuesta A, ver docs/MARCA.md). Es la misma animacion que
 * assets/marca/intro-prototipo.html?v=A y que el componente web IntroAnimada.tsx: lienzo de
 * diseño 1080 x 1920, escalado para caber en la pantalla. Codigo comun (Android e iOS via KMP).
 */

/** Paleta de la intro (docs/MARCA.md). */
internal object Tinta {
    val Morado = Color(0xFFA855F7)
    val MoradoClaro = Color(0xFFC084FC)
    val MoradoOscuro = Color(0xFF7B1FA2)
    val Lila = Color(0xFFF3E8FF)
    val Verde = Color(0xFF10B981)
    val Fondo = Color(0xFF0C1322)
    val FondoProfundo = Color(0xFF070E1D)
    val Violeta = Color(0xFF1A1030)
}

private const val W = 1080f
private const val H = 1920f
internal const val GAZ = "M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 " +
    "L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 " +
    "C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z"

/** Curvas de la intro, identicas al prototipo HTML y a la version web. */
object CurvasIntro {
    fun cl(v: Float, a: Float = 0f, b: Float = 1f) = v.coerceIn(a, b)
    /** ease-out cubica. */
    fun e(t: Float) = 1f - (1f - cl(t)).pow(3)
    /** ease-in-out cubica. */
    fun io(t: Float): Float { val x = cl(t); return if (x < .5f) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2 }

    /** Ruta luminosa: 201 puntos en el lienzo de diseño. */
    val ruta: List<Offset> = (0..200).map { i ->
        val u = i / 200f
        Offset(140 + u * 800, (1180 - sin(u * PI * 2.2) * 120 - u * 160 + sin(u * 17.0) * 14).toFloat())
    }
}

internal val gacela: Path by lazy { PathParser().parsePathString(GAZ).toPath() }

/** Objetos reutilizados en cada cuadro: el bucle de dibujo no asigna rutas, trazos ni pinceles. */
private object Reuso {
    val ruta = Path()
    val trazos = listOf(Stroke(46f, cap = StrokeCap.Round, join = StrokeJoin.Round) to .1f,
        Stroke(28f, cap = StrokeCap.Round, join = StrokeJoin.Round) to .2f,
        Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round) to 1f)
    val relleno = Brush.linearGradient(0f to Tinta.Lila, .4f to Tinta.MoradoClaro, .75f to Tinta.Morado, 1f to Tinta.MoradoOscuro,
        start = Offset(-400f, -400f), end = Offset(400f, 400f))
    val verdeHalo = Tinta.Verde.copy(alpha = .25f)
    var fondo: Brush? = null
    var fondoTam = Size.Zero
    /** Texto medido por espaciado (redondeado a medio pixel) y escala. */
    val textos = HashMap<Long, androidx.compose.ui.text.TextLayoutResult>()
    var textoK = 0f
}

/** Dibuja el cuadro de la intro en el instante [t] (segundos, 0..[IntroArranque.DURACION_S]). */
fun DrawScope.dibujarIntro(t: Float, medidor: TextMeasurer) {
    with(CurvasIntro) {
        // Fondo radial (ocupa toda la pantalla aunque el lienzo de diseño quede encajado).
        if (Reuso.fondo == null || Reuso.fondoTam != size) {
            Reuso.fondo = Brush.radialGradient(0f to Tinta.Violeta, .6f to Tinta.Fondo, 1f to Tinta.FondoProfundo,
                center = Offset(size.width / 2, size.height * .45f), radius = 1100f * escala())
            Reuso.fondoTam = size
        }
        drawRect(Reuso.fondo!!)
        val k = escala()
        translate((size.width - W * k) / 2, (size.height - H * k) / 2) {
            withTransform({ scale(k, k, Offset.Zero) }) {
                // Ruta luminosa que se dibuja.
                val d = e(t / 1.3f)
                val n = floor(d * 200).toInt()
                val alfaRuta = 1 - cl((t - 1.4f) / .4f)
                if (n > 0 && alfaRuta > 0) {
                    val p = Reuso.ruta.apply { rewind(); moveTo(ruta[0].x, ruta[0].y); for (i in 1..n) lineTo(ruta[i].x, ruta[i].y) }
                    for ((trazo, a) in Reuso.trazos) drawPath(p, Tinta.Morado, alpha = a * alfaRuta, style = trazo)
                    if (t < 1.5f) {
                        val c = ruta[n]
                        drawCircle(Reuso.verdeHalo, 44f, c, alpha = alfaRuta)
                        drawCircle(Tinta.Verde, 22f, c, alpha = alfaRuta)
                    }
                }
                // Destello.
                val f = cl(1 - abs(t - 1.45f) / .25f)
                if (f > 0) {
                    drawRect(Brush.radialGradient(0f to Tinta.Lila.copy(alpha = f), .3f to Tinta.Morado.copy(alpha = f * .6f), 1f to Color.Transparent,
                        center = Offset(W / 2, H * .45f), radius = 900 * f + 1), Offset(-W, -H), Size(W * 3, H * 3))
                }
                // Gacela y palabra GACELAS, luego zoom hacia dentro.
                if (t > 1.35f) {
                    val kk = e((t - 1.35f) / .4f)
                    var s = 9 * (.7f + .3f * kk)
                    var al = kk
                    if (t > 2.2f) { val z = io((t - 2.2f) / .6f); s *= 1 + z * 14; al = 1 - z }
                    if (al > 0) {
                        withTransform({ translate(W / 2, H * .45f); scale(s, s, Offset.Zero); translate(-56f, -48f) }) {
                            val relleno = Reuso.relleno
                            drawPath(gacela, Tinta.Morado, alpha = .1f * al, style = Stroke(60f / s, join = StrokeJoin.Round))
                            drawPath(gacela, Tinta.Morado, alpha = .16f * al, style = Stroke(28f / s, join = StrokeJoin.Round))
                            drawPath(gacela, relleno, alpha = al)
                        }
                        val alTexto = al * e((t - 1.6f) / .4f)
                        if (alTexto > 0) {
                            if (Reuso.textoK != k) { Reuso.textos.clear(); Reuso.textoK = k }
                            val esp = (20 - 10 * kk) * k
                            val r = Reuso.textos.getOrPut((esp * 2).toLong()) { medidor.measure("GACELAS", estiloPalabra(120f * k, esp)) }
                            val x = W / 2 - r.size.width / k / 2
                            val y = H * .62f - r.firstBaseline / k
                            withTransform({ translate(x, y); scale(1 / k, 1 / k, Offset.Zero) }) {
                                drawText(r, color = Color.White, alpha = alTexto)
                            }
                        }
                    }
                }
            }
        }
        // Fundido final al fondo de la app.
        if (t > 2.5f) drawRect(Tinta.Fondo.copy(alpha = io((t - 2.5f) / .3f)))
    }
}

private fun DrawScope.escala() = min(size.width / W, size.height / H)

/** Un cuadro fijo de la intro (pruebas de captura y vista previa). */
@Composable
fun IntroCuadro(t: Float, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    Canvas(modifier.fillMaxSize()) { dibujarIntro(t, medidor) }
}

/**
 * Intro animada a pantalla completa. Dura [IntroArranque.DURACION_S] segundos de reloj (no se
 * estira con la escala de animaciones), se salta con un toque y llama a [alTerminar] una sola vez.
 */
@Composable
fun IntroAnimada(alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val fin by rememberUpdatedState(alTerminar)
    val estado = remember { EstadoIntro() }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val inicio = withFrameNanos { it }
        while (!estado.terminada) {
            withFrameNanos { ahora -> t = (ahora - inicio) / 1e9f }
            if (t >= IntroArranque.DURACION_S && estado.terminar()) fin()
        }
    }
    val medidor = rememberTextMeasurer()
    Canvas(
        modifier.fillMaxSize().testTag("intro-animada").semantics { contentDescription = "Intro de Gacelas, toca para saltar" }
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (estado.terminar()) fin() },
    ) { dibujarIntro(t.coerceAtMost(IntroArranque.DURACION_S), medidor) }
}

/** Anton (familia de titulares) a [px] pixeles de pantalla, con el espaciado del prototipo. */
private fun DrawScope.estiloPalabra(px: Float, espaciadoPx: Float) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontSize = px.toSp(),
    letterSpacing = TextUnit(espaciadoPx / px, TextUnitType.Em),
)
