package com.gacelas.seguridad.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Alertas de zona durante la carrera (ticket 86e3uf09b, N-11 y VI-08): al acercarse a menos de
 * [RADIO_M] de un reporte reciente se avisa una sola vez por reporte. Se apagan en Ajustes,
 * Privacidad y ubicacion (prefs.alertasZona, compartido con la web).
 */
object AlertasZona {
    const val RADIO_M = 200.0
    /** Reportes de hasta 7 dias; los mas viejos no alertan aunque sigan en el mapa. */
    const val VIGENCIA_MS = 7L * 24 * 60 * 60 * 1000
    /** Cada cuanto se vuelven a leer los reportes durante una carrera. */
    const val REFRESCO_MS = 5L * 60 * 1000

    fun distanciaM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = ((lat2 - lat1) * PI / 180.0)
        val dLng = ((lng2 - lng1) * PI / 180.0)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(((lat1) * PI / 180.0)) * cos(((lat2) * PI / 180.0)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Reportes vigentes a menos de [RADIO_M] que aun no se avisaron, del mas cercano al mas lejano. */
    fun nuevas(lat: Double, lng: Double, reportes: List<IncidentePublico>, avisados: Set<String>, ahoraMs: Long): List<Pair<IncidentePublico, Int>> =
        reportes.asSequence()
            .filter { it.id !in avisados && (it.ocurridoMs == 0L || ahoraMs - it.ocurridoMs <= VIGENCIA_MS) }
            .map { it to distanciaM(lat, lng, it.lat, it.lng) }
            .filter { it.second <= RADIO_M }
            .sortedBy { it.second }
            .map { it.first to it.second.toInt() }
            .toList()

    fun mensaje(i: IncidentePublico, metros: Int): String =
        "Atención: reporte de ${i.tipo.nombre.lowercase()} a $metros m" + if (i.verificado) " (verificado)" else ""
}

/**
 * Estado de las alertas de una carrera, usado por el servicio de ubicacion en primer plano (0.9.15):
 * avisa una sola vez por reporte y solo vuelve a calcular cuando el corredor se movio [minMovimientoM]
 * o paso [minIntervaloMs], para no gastar CPU con cada punto GPS de 1 s.
 */
class MonitorAlertasZona(
    private val minMovimientoM: Double = 25.0,
    private val minIntervaloMs: Long = 15_000L,
) {
    private val avisados = mutableSetOf<String>()
    private var ultimoLat: Double? = null
    private var ultimoLng = 0.0
    private var ultimoMs = 0L

    /** Reportes vigentes; al cambiar se vuelve a evaluar con el siguiente punto. */
    var reportes: List<IncidentePublico> = emptyList()
        set(v) { field = v; ultimoLat = null }

    val yaAvisados: Set<String> get() = avisados

    /** Devuelve el reporte mas cercano que hay que avisar ahora, o null. */
    fun evaluar(lat: Double, lng: Double, ahoraMs: Long): Pair<IncidentePublico, Int>? {
        if (reportes.isEmpty()) return null
        val prev = ultimoLat
        if (prev != null && ahoraMs - ultimoMs < minIntervaloMs && AlertasZona.distanciaM(prev, ultimoLng, lat, lng) < minMovimientoM) return null
        ultimoLat = lat; ultimoLng = lng; ultimoMs = ahoraMs
        val nuevas = AlertasZona.nuevas(lat, lng, reportes, avisados, ahoraMs)
        if (nuevas.isEmpty()) return null
        nuevas.forEach { avisados += it.first.id }
        return nuevas.first()
    }

    /** Nueva carrera: se olvidan los avisos y la ultima posicion. */
    fun reiniciar() { avisados.clear(); ultimoLat = null; reportes = emptyList() }
}
