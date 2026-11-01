package com.gacelas.ui.intro

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import com.gacelas.ui.intro.LineaTiempoIntroE as E
import com.gacelas.ui.theme.SpaceGrotesk
import kotlin.random.Random

private val Estela = Color(0xFFE9D5FF)
private val Blanco = Color.White

/**
 * Intro "Opcion E" con UN solo reloj: un bucle de withFrameNanos lleva t de 0 a 3,0 s en un
 * estado de punto flotante que solo lee la fase de dibujo (no recompone por cuadro). Al llegar a
 * 3,0 s llama [alTerminar] una vez (el reloj empieza cuando [activa] es true). Sirve en Android e iOS (commonMain).
 */
@Composable
fun IntroMarcaEAnimada(
    modifier: Modifier = Modifier,
    activa: Boolean = true,
    alTerminar: () -> Unit = {},
) {
    val t = remember { mutableFloatStateOf(0f) }
    val fin = rememberUpdatedState(alTerminar)
    // [activa] permite al anfitrion arrancar el reloj con el primer dibujo real (no al componer).
    LaunchedEffect(activa) {
        if (!activa) return@LaunchedEffect
        // Se acumula el delta de cada cuadro con tope de 100 ms: un tiron (o un reloj que salta,
        // como en Robolectric) no se come la animacion; el tope duro de IntroArranque sigue vigente.
        var previo = withFrameNanos { it }
        var s = 0f
        while (true) {
            val ahora = withFrameNanos { it }
            s += ((ahora - previo) / 1_000_000_000f).coerceIn(0f, 0.1f)
            previo = ahora
            t.floatValue = s.coerceAtMost(E.DURACION_S)
            if (s >= E.DURACION_S) break
        }
        fin.value()
    }
    IntroMarcaE(tiempo = { t.floatValue }, modifier = modifier)
}

/** Dibuja el cuadro de la intro en el instante [tiempo] (segundos). Sin estado: util para capturas. */
@Composable
fun IntroMarcaE(tiempo: () -> Float, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer(cacheSize = 32)
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            // Lienzo de referencia 1080x1920 centrado y ajustado (sin recortar nada).
            val s = minOf(size.width / E.ANCHO, size.height / E.ALTO)
            val ox = (size.width - E.ANCHO * s) / 2f
            val oy = (size.height - E.ALTO * s) / 2f
            val aproximacion = PathParser().parsePathString(E.APROXIMACION).toPath()
            val medAprox = PathMeasure().apply { setPath(aproximacion, false) }
            val medGacela = PathMeasure().apply { setPath(gacela, false) }
            val segA = Path(); val segB = Path(); val segC = Path()
            val rellenoGacela = Brush.linearGradient(
                0f to Tinta.Lila, 0.35f to Tinta.MoradoClaro, 0.75f to Tinta.Morado, 1f to Tinta.MoradoOscuro,
                start = Offset(22f, 18f), end = Offset(91f, 78f),
            )
            val fondoBrillo = Brush.radialGradient(
                listOf(Tinta.MoradoOscuro.copy(alpha = 0.30f), Tinta.MoradoOscuro.copy(alpha = 0f)),
                center = Offset(ox + E.ANCHO / 2f * s, oy + E.ALTO * 0.43f * s), radius = E.ANCHO * 0.95f * s,
            )
            // Grano leve fijo (anti-banding): un solo drawPoints por cuadro.
            val rnd = Random(7)
            val granoClaro = List(1400) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) }
            val granoOscuro = List(1400) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) }
            // Letras precalculadas (Anton cursiva, blanco con glow morado).
            val tamTexto = (E.TAM_TEXTO * s).toSp()
            val letras: List<TextLayoutResult> = E.TEXTO.map {
                medidor.measure(it.toString(), TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = tamTexto, color = Blanco))
            }
            val trk = 0.04f * E.TAM_TEXTO * s
            val total = letras.sumOf { it.size.width.toDouble() }.toFloat() + trk * (letras.size - 1)
            val xsLetras = FloatArray(letras.size).also { xs ->
                var x = ox + E.ANCHO / 2f * s - total / 2f
                letras.forEachIndexed { i, l -> xs[i] = x; x += l.size.width + trk }
            }
            val lema = E.LEMA.map { medidor.measure(it.toString(), TextStyle(fontSize = (E.TAM_LEMA * s).toSp(), color = Tinta.MoradoClaro)) }

            onDrawBehind {
                val t = tiempo().coerceIn(0f, E.DURACION_S)
                drawRect(Tinta.Fondo)
                drawRect(fondoBrillo, alpha = E.brilloFondo(t))
                withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
                    escena(t, s, medAprox, medGacela, segA, segB, segC, rellenoGacela)
                }
                // Texto (en pixeles reales para que la fuente quede nitida).
                letras.forEachIndexed { i, l ->
                    val a = E.alfaLetra(t, i)
                    if (a > 0f) {
                        val y = oy + (E.BASE_TEXTO + E.dyLetra(t, i)) * s - l.firstBaseline
                        val glow = Shadow(Tinta.Morado.copy(alpha = 0.45f * a), Offset.Zero, 24f * s)
                        drawText(l, topLeft = Offset(xsLetras[i], y), alpha = a, shadow = glow)
                    }
                }
                val aLema = E.alfaLema(t)
                if (aLema > 0f) {
                    val sp = (10f + E.espaciadoLema(t)) * s
                    val tot = lema.sumOf { it.size.width.toDouble() }.toFloat() + sp * (lema.size - 1)
                    var x = ox + E.ANCHO / 2f * s - tot / 2f
                    val yBase = oy + (E.BASE_TEXTO + 110f) * s
                    lema.forEach { l -> drawText(l, topLeft = Offset(x, yBase - l.firstBaseline), alpha = aLema * 0.9f); x += l.size.width + sp }
                }
                drawPoints(granoClaro, PointMode.Points, Color.White.copy(alpha = 0.025f), strokeWidth = 1.5f)
                drawPoints(granoOscuro, PointMode.Points, Color.Black.copy(alpha = 0.04f), strokeWidth = 1.5f)
                val velo = E.velo(t)
                if (velo > 0f) drawRect(Tinta.Fondo.copy(alpha = velo))
            }
        },
    )
}

private fun trazo(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Trazo con brillo barato: capas anchas y tenues debajo en lugar de un desenfoque por cuadro. */
private fun DrawScope.trazoBrillante(p: Path, c: Color, a: Float, w: Float, brillo: Float) {
    if (a <= 0f) return
    if (brillo > 0f) halo(p, c, 0.5f * a, w, brillo)
    drawPath(p, c, alpha = a, style = trazo(w))
}

/** Desenfoque aproximado: capas concentricas cada vez mas anchas y tenues (suman una caida suave). */
private fun DrawScope.halo(p: Path, c: Color, a: Float, w: Float, radio: Float) {
    if (a <= 0f) return
    for (i in 4 downTo 1) drawPath(p, c, alpha = (0.16f * a).coerceAtMost(1f), style = trazo(w + radio * 2f * i / 4f))
}

private fun DrawScope.punto(centro: Offset, c: Color, a: Float, r: Float, halo: Float) {
    drawCircle(
        Brush.radialGradient(
            0f to c.copy(alpha = a), (r / (r + halo)) to c.copy(alpha = a * 0.85f), 1f to c.copy(alpha = 0f),
            center = centro, radius = r + halo,
        ),
        radius = r + halo, center = centro,
    )
}

private fun DrawScope.escena(
    t: Float, s: Float, medAprox: PathMeasure, medGacela: PathMeasure,
    segA: Path, segB: Path, segC: Path, relleno: Brush,
) {
    val pa = E.escalaLogo(t)
    val esc = E.LOGO_ESCALA * pa
    val oa = E.alfaContorno(t)
    val la = medAprox.length
    val lg = medGacela.length
    fun aLogo(x: Float, y: Float) = Offset(E.LOGO_X + (x - E.PIVOTE_X) * esc, E.LOGO_Y + (y - E.PIVOTE_Y) * esc)
    val cabeza: Offset
    if (t < E.T_A) {
        val d = E.avanceAproximacion(t)
        segA.rewind(); medAprox.getSegment(maxOf(0f, d - 0.35f) * la, d * la, segA, true)
        segB.rewind(); medAprox.getSegment(maxOf(0f, d - 0.22f) * la, d * la, segB, true)
        trazoBrillante(segA, Tinta.Morado, 0.45f, 9f, 6f)
        trazoBrillante(segB, Estela, 0.85f, 3.2f, 1.5f)
        cabeza = medAprox.getPosition(d * la)
    } else {
        val ta = E.estelaAproximacion(t)
        if (ta > 0f) {
            segA.rewind(); medAprox.getSegment((0.65f + 0.35f * (1f - ta)) * la, la, segA, true)
            segB.rewind(); medAprox.getSegment((0.78f + 0.22f * (1f - ta)) * la, la, segB, true)
            trazoBrillante(segA, Tinta.Morado, 0.45f * ta, 9f, 6f)
            trazoBrillante(segB, Estela, 0.85f * ta, 3.2f, 1.5f)
        }
        val ug = E.avanceContornoLineal(t)
        val dg = E.fraccionContorno(t)
        if (oa > 0f) {
            segA.rewind(); medGacela.getSegment(0f, dg * lg, segA, true)
            segC.rewind(); medGacela.getSegment(maxOf(0f, dg - 0.14f) * lg, dg * lg, segC, true)
            enLogo(esc) {
                trazoBrillante(segA, Tinta.Morado, 0.55f * oa, 3f, 3f)
                drawPath(segA, Tinta.Lila, alpha = oa, style = trazo(1.2f))
                trazoBrillante(segC, Estela, 0.85f * oa * (1f - ug * 0.4f), 2.4f, 1.5f)
            }
        }
        val p = medGacela.getPosition(dg * lg)
        cabeza = aLogo(p.x, p.y)
    }
    val ha = E.alfaCabeza(t)
    if (ha > 0f) {
        punto(cabeza, Tinta.Lila, 0.5f * ha * 0.6f, 20f, 56f)
        punto(cabeza, Blanco, ha, 9f, 9f)
    }
    enLogo(esc) {
        E.LINEAS.forEachIndexed { j, (y, xa, xb) ->
            val dd = E.avanceLinea(t, j)
            if (dd > 0f) drawLine(Tinta.MoradoClaro, Offset(xb, y), Offset(E.lerp(xb, xa, dd), y), 2.4f, StrokeCap.Round, alpha = 0.7f)
        }
        val fa = E.alfaRelleno(t)
        if (fa > 0f) {
            val br = E.brilloRelleno(t)
            if (br > 0f) {
                halo(gacela, Tinta.Morado, (br * fa).coerceAtMost(1f), 0f, 3f)
            }
            drawPath(gacela, relleno, alpha = fa)
            E.barrido(t)?.let { sh ->
                val x = -10f + sh * 120f
                clipPath(gacela, ClipOp.Intersect) {
                    drawRect(
                        Brush.linearGradient(
                            0f to Blanco.copy(alpha = 0f), 0.5f to Blanco.copy(alpha = 0.7f * fa), 1f to Blanco.copy(alpha = 0f),
                            start = Offset(x - 14f, 0f), end = Offset(x + 14f, 30f),
                        ),
                    )
                }
            }
            drawCircle(Tinta.Fondo, 1.1f, Offset(80f, 37f), alpha = fa)
        }
    }
    val k = E.anillo(t)
    val aa = E.alfaAnillo(k)
    if (aa > 0f) {
        val r = E.radioAnillo(k)
        drawCircle(Tinta.Morado, r, Offset(540f, 880f), alpha = aa * 0.35f, style = Stroke(9f))
        drawCircle(Tinta.Morado, r, Offset(540f, 880f), alpha = aa, style = Stroke(3.5f))
    }
}

private inline fun DrawScope.enLogo(esc: Float, crossinline bloque: DrawScope.() -> Unit) =
    withTransform({
        translate(E.LOGO_X - E.PIVOTE_X * esc, E.LOGO_Y - E.PIVOTE_Y * esc)
        scale(esc, esc, Offset.Zero)
    }) { bloque() }
