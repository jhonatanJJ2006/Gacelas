package com.gacelas.carreras.domain.reglas

import com.gacelas.carreras.domain.modelo.Parcial
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.shared.rad
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Geometria de puntos GPS. */
object Geo {
    private const val RADIO_TIERRA_M = 6_371_000.0

    /** Distancia en metros entre dos coordenadas (formula de haversine). */
    fun haversineM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = rad(lat2 - lat1)
        val dLng = rad(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(rad(lat1)) * cos(rad(lat2)) * sin(dLng / 2).pow(2)
        return 2 * RADIO_TIERRA_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun haversineM(a: PuntoGps, b: PuntoGps): Double = haversineM(a.lat, a.lng, b.lat, b.lng)
}

/** Filtro de puntos GPS: que se descarta como impreciso, ruido o salto imposible. */
object ReglasGps {
    /** Puntos con peor precision se descartan. */
    const val PRECISION_MAX_M = 35f
    /** Saltos menores se consideran ruido del GPS. */
    const val MOVIMIENTO_MIN_M = 2.0
    /** Velocidad maxima creible para una persona corriendo (m/s). */
    const val VELOCIDAD_MAX_MS = 12.0
    /** Sin avance valido durante este tiempo se considera que estas quieto. */
    const val PARADO_MS = 6_000L

    fun impreciso(p: PuntoGps) = p.precisionM > PRECISION_MAX_M

    /** True si el avance de [previo] a [p] es ruido (muy corto) o imposible (demasiado rapido). */
    fun descartar(previo: PuntoGps, p: PuntoGps, metros: Double = Geo.haversineM(previo, p)): Boolean {
        val dt = (p.tiempoMs - previo.tiempoMs).coerceAtLeast(1L) / 1000.0
        return metros < MOVIMIENTO_MIN_M || metros / dt > VELOCIDAD_MAX_MS
    }

    fun parado(previo: PuntoGps, p: PuntoGps) = p.tiempoMs - previo.tiempoMs >= PARADO_MS

    /** Desnivel positivo entre dos puntos (0 si falta altitud o es bajada). */
    fun subida(previo: PuntoGps, p: PuntoGps): Double =
        if (previo.altitudM != null && p.altitudM != null) (p.altitudM - previo.altitudM).coerceAtLeast(0.0) else 0.0
}

/** Ritmo actual sobre una ventana de tiempo. */
object ReglasRitmo {
    const val VENTANA_MS = 30_000L

    /** Ritmo (seg/km) de los ultimos [ventanaMs] de puntos, o null con menos de 5 m. */
    fun ventana(puntos: List<PuntoGps>, ventanaMs: Long = VENTANA_MS): Int? {
        val fin = puntos.lastOrNull() ?: return null
        val ventana = puntos.filter { fin.tiempoMs - it.tiempoMs <= ventanaMs }
        if (ventana.size < 2) return null
        val metros = ventana.zipWithNext { a, b -> Geo.haversineM(a, b) }.sum()
        val seg = (fin.tiempoMs - ventana.first().tiempoMs) / 1000.0
        return if (metros < 5) null else (seg / (metros / 1000.0)).toInt()
    }
}

/** Parciales automaticos por kilometro y vueltas manuales. */
object ReglasParciales {
    /** Parcial automatico nuevo si se completo otro km, o null. */
    fun automatico(parciales: List<Parcial>, distanciaKm: Double, segundos: Int, segundosUltimoParcial: Int): Parcial? {
        val automaticos = parciales.count { !it.manual }
        return if (distanciaKm.toInt() > automaticos) Parcial(automaticos + 1, 1.0, segundos - segundosUltimoParcial) else null
    }

    /** Vuelta manual: distancia y tiempo desde la vuelta anterior. */
    fun vuelta(parciales: List<Parcial>, distanciaKm: Double, segundos: Int): Parcial {
        val manuales = parciales.filter { it.manual }
        return Parcial(
            manuales.size + 1,
            (distanciaKm - manuales.sumOf { it.distanciaKm }).coerceAtLeast(0.0),
            segundos - manuales.sumOf { it.segundos },
            manual = true,
        )
    }
}

fun tituloPorHora(ms: Long): String {
    val h = com.gacelas.shared.Fechas.hora(ms)
    return when (h) {
        in 5..11 -> "Carrera matutina"
        in 12..18 -> "Carrera de la tarde"
        else -> "Carrera nocturna"
    }
}
