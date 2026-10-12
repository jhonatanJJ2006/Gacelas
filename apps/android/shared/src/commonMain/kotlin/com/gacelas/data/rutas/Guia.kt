package com.gacelas.data.rutas

import com.gacelas.shared.grados
import com.gacelas.shared.rad

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/** Indicacion de navegacion sobre la ruta elegida. */
data class Indicacion(
    /** Metros recorridos sobre la ruta (proyeccion de la posicion). */
    val avanceM: Double,
    /** Metros que faltan hasta la meta. */
    val restanteM: Double,
    /** Metros hasta el proximo giro (o hasta la meta si no hay mas giros). */
    val hastaGiroM: Double,
    val texto: String,
    val llegada: Boolean,
    /** Distancia de la posicion a la ruta: si es grande, el corredor se salio. */
    val desvioM: Double,
)

/**
 * Guia de navegacion calculada solo con la geometria de la ruta (funciona sin red):
 * proyecta la posicion sobre la polilinea, mide lo que falta y detecta giros por cambio de rumbo.
 */
object Guia {
    const val GIRO_MIN_GRADOS = 35.0
    const val LLEGADA_M = 25.0
    const val DESVIO_M = 50.0

    private fun xy(origen: Pair<Double, Double>, p: Pair<Double, Double>): Pair<Double, Double> {
        val kx = 111_320.0 * cos(rad(origen.first))
        return (p.second - origen.second) * kx to (p.first - origen.first) * 110_540.0
    }

    fun distanciaM(a: Pair<Double, Double>, b: Pair<Double, Double>): Double = xy(a, b).let { hypot(it.first, it.second) }

    fun longitudM(trazo: List<Pair<Double, Double>>): Double = trazo.zipWithNext { a, b -> distanciaM(a, b) }.sum()

    private fun rumbo(a: Pair<Double, Double>, b: Pair<Double, Double>): Double = xy(a, b).let { grados(atan2(it.first, it.second)) }

    /** Giro con signo en grados (-180..180]: positivo a la derecha. */
    fun giro(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Double {
        var d = rumbo(b, c) - rumbo(a, b)
        while (d > 180) d -= 360
        while (d <= -180) d += 360
        return d
    }

    /** Proyecta [p] sobre la ruta: (metros desde el inicio, distancia a la ruta). */
    fun proyectar(trazo: List<Pair<Double, Double>>, p: Pair<Double, Double>): Pair<Double, Double> {
        var mejor = Double.MAX_VALUE
        var avance = 0.0
        var acumulado = 0.0
        for ((a, b) in trazo.zipWithNext()) {
            val (bx, by) = xy(a, b)
            val (px, py) = xy(a, p)
            val l2 = bx * bx + by * by
            val t = if (l2 == 0.0) 0.0 else ((px * bx + py * by) / l2).coerceIn(0.0, 1.0)
            val d = hypot(px - t * bx, py - t * by)
            val largo = kotlin.math.sqrt(l2)
            if (d < mejor) { mejor = d; avance = acumulado + t * largo }
            acumulado += largo
        }
        return avance to mejor
    }

    fun calcular(trazo: List<Pair<Double, Double>>, posicion: Pair<Double, Double>?): Indicacion? {
        if (trazo.size < 2) return null
        val total = longitudM(trazo)
        val (avance, desvio) = posicion?.let { proyectar(trazo, it) } ?: (0.0 to 0.0)
        val restante = (total - avance).coerceAtLeast(0.0)
        if (posicion != null && restante <= LLEGADA_M && desvio <= DESVIO_M) return Indicacion(avance, 0.0, 0.0, "Llegaste a tu destino", true, desvio)
        // Distancia acumulada hasta cada vertice intermedio, y su giro.
        var acumulado = 0.0
        for (i in 1 until trazo.lastIndex) {
            acumulado += distanciaM(trazo[i - 1], trazo[i])
            if (acumulado <= avance + 5) continue
            val g = giro(trazo[i - 1], trazo[i], trazo[i + 1])
            if (abs(g) >= GIRO_MIN_GRADOS) {
                val lado = if (g > 0) "derecha" else "izquierda"
                val texto = if (abs(g) >= 120) "Giro cerrado a la $lado" else "Gira a la $lado"
                return Indicacion(avance, restante, acumulado - avance, texto, false, desvio)
            }
        }
        return Indicacion(avance, restante, restante, "Continúa recto hasta la meta", false, desvio)
    }
}
