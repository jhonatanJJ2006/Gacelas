package com.gacelas.ui.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Linea de tiempo de la intro "Opcion E" (3,0 s, sin mapa), copia exacta de escena_E en
 * render_opciones.py (tiempos, curvas y geometria). Funciones puras de t (segundos): un solo
 * reloj las alimenta y el dibujo (IntroMarcaE) solo las lee. Coordenadas en el lienzo de
 * referencia de 1080x1920; la gacela en sus unidades del SVG (assets/marca/primer_plano.svg).
 */
object LineaTiempoIntroE {
    const val DURACION_S = 3.0f
    const val ANCHO = 1080f
    const val ALTO = 1920f

    /** Fin de la aproximacion del punto de luz / fin del contorno. */
    const val T_A = 0.32f
    const val T_D = 1.1f

    /** Centro y escala del logo (mat(W/2, 860, 6.2) con pivote 56,48 de la gacela). */
    const val LOGO_X = 540f
    const val LOGO_Y = 860f
    const val LOGO_ESCALA = 6.2f
    const val PIVOTE_X = 56f
    const val PIVOTE_Y = 48f

    /** Inicio del contorno en pantalla: (28,60) de la gacela. */
    const val GX = LOGO_X + (28f - PIVOTE_X) * LOGO_ESCALA
    const val GY = LOGO_Y + (60f - PIVOTE_Y) * LOGO_ESCALA

    /** Curva en S por la que entra el punto de luz (abajo-izquierda hasta el inicio del contorno). */
    val APROXIMACION = "M -80 1500 C 260 1700 820 1420 700 1120 S ${GX - 260f} ${GY + 40f} $GX $GY"

    /** Lineas de velocidad (unidades de la gacela): y, x final (punta) y x inicial. */
    val LINEAS = listOf(Triple(52f, 10f, 24f), Triple(60f, 6f, 22f), Triple(68f, 12f, 20f))

    const val TEXTO = "RUNCITY"
    const val LEMA = "LOJA · ECUADOR"
    const val BASE_TEXTO = 1250f // WY
    const val TAM_TEXTO = 190f
    const val TAM_LEMA = 40f

    // ---------- easing (los mismos de render_opciones.py) ----------
    fun cl(v: Float, a: Float = 0f, b: Float = 1f) = max(a, min(b, v))
    fun eo(t: Float, p: Int = 3) = 1f - (1f - cl(t)).pow(p)
    fun esin(t: Float) = 0.5f - 0.5f * cos(PI.toFloat() * cl(t))
    fun eback(t: Float, s: Float = 1.3f): Float { val x = cl(t) - 1f; return 1f + (s + 1f) * x * x * x + s * x * x }
    fun lerp(a: Float, b: Float, u: Float) = a + (b - a) * u
    private fun campana(t: Float) = sin(PI.toFloat() * cl(t))

    // ---------- canales ----------
    /** Intensidad del brillo morado del fondo. */
    fun brilloFondo(t: Float) = 0.55f + 0.45f * esin((t - 0.9f) / 0.7f)

    /** Pulso de escala del logo (+5 %). */
    fun escalaLogo(t: Float) = 1f + 0.05f * campana((t - 1.22f) / 0.42f)

    /** Opacidad del contorno (se desvanece al rellenarse). */
    fun alfaContorno(t: Float) = 1f - eo((t - 1.12f) / 0.3f)

    /** Avance del punto de luz por la curva de aproximacion (0..1); 1 despues de T_A. */
    fun avanceAproximacion(t: Float): Float {
        if (t >= T_A) return 1f
        val u = cl(t / T_A)
        return cl(0.15f * u + 0.85f * esin(u) * u.pow(0.3f))
    }

    /** Estela de la aproximacion que se recoge tras llegar (1 = completa, 0 = ya no se ve). */
    fun estelaAproximacion(t: Float) = if (t < T_A) 1f else 1f - esin((t - T_A) / 0.25f)

    /** Avance lineal del dibujo del contorno (0..1). */
    fun avanceContornoLineal(t: Float) = cl((t - T_A) / (T_D - T_A))

    /** Fraccion dibujada del contorno de la gacela (0..1). */
    fun fraccionContorno(t: Float): Float {
        if (t < T_A) return 0f
        val u = avanceContornoLineal(t)
        return if (u < 1f) 0.6f * u + 0.4f * esin(u) else 1f
    }

    /** Opacidad del punto de luz (cabeza). */
    fun alfaCabeza(t: Float) = esin(t / 0.06f) * (1f - esin((t - 1.06f) / 0.16f))

    /** Avance de cada linea de velocidad (j = 0..2), escalonadas. */
    fun avanceLinea(t: Float, j: Int) = esin((t - 0.92f - j * 0.06f) / 0.28f)

    /** Opacidad del relleno degradado. */
    fun alfaRelleno(t: Float) = esin((t - 1.0f) / 0.32f)

    /** Brillo (glow) del relleno: base + pulso. */
    fun brilloRelleno(t: Float) = 0.7f * alfaRelleno(t) + 0.4f * campana((t - 1.22f) / 0.42f)

    /** Posicion del barrido de luz (0..1) o null fuera de 1,25..1,7 s. */
    fun barrido(t: Float): Float? = if (t > 1.25f && t < 1.7f) cl((t - 1.25f) / 0.45f) else null

    /** Progreso del anillo (0..1); fuera de (0,1) no se dibuja. */
    fun anillo(t: Float) = cl((t - 1.24f) / 0.6f)
    fun radioAnillo(k: Float) = lerp(260f, 560f, eo(k, 2))
    fun alfaAnillo(k: Float) = if (k <= 0f || k >= 1f) 0f else 0.5f * (1f - k)

    /** Letra i de RUNCITY: progreso, opacidad y desplazamiento vertical (rebote suave). */
    fun progresoLetra(t: Float, i: Int) = cl((t - 1.5f - i * 0.06f) / 0.42f)
    fun alfaLetra(t: Float, i: Int) = eo(progresoLetra(t, i) / 0.5f, 2)
    fun dyLetra(t: Float, i: Int) = (1f - eback(progresoLetra(t, i), 1.4f)) * 36f

    /** Lema LOJA · ECUADOR: opacidad y espaciado extra que se cierra. */
    fun alfaLema(t: Float) = eo((t - 1.95f) / 0.3f)
    fun espaciadoLema(t: Float) = lerp(16f, 0f, eo((t - 1.95f) / 0.45f))

    /** Velo de fondo #0C1322 al entrar (1 en t=0) y al salir (1 en t=3,0). */
    fun veloEntrada(t: Float) = 1f - esin(t / 0.08f)
    fun veloSalida(t: Float) = esin((t - 2.5f) / 0.45f)
    fun velo(t: Float) = max(veloEntrada(t), veloSalida(t))
}
