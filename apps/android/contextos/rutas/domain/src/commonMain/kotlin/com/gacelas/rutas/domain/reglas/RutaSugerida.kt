package com.gacelas.rutas.domain.reglas

import com.gacelas.rutas.domain.modelo.Ruta
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Ruta sugerida para hoy (contexto Rutas). La regla oficial vive en Supabase (ruta_sugerida_hoy,
 * migracion 035); esta copia pura se usa sin red y aplica lo mismo:
 * 1. solo rutas seguras (ids marcados como seguros por el servidor en la ultima consulta),
 * 2. sin rutas corridas ni sugeridas en los dias de Ecuador hoy-3 .. hoy,
 * 3. orden por cercania a la distancia objetivo y al usuario, desempate rotativo por dia.
 * Ver docs/RUTA-SUGERIDA.md.
 */

const val DIAS_SIN_REPETIR = 3
private const val OFFSET_ECUADOR_MS = 5L * 3600 * 1000
private const val DIA_MS = 86_400_000L

/** Dia de Ecuador (UTC-5 fijo) como numero de dias desde 1970-01-01. */
fun diaEcuador(ms: Long): Long = (ms - OFFSET_ECUADOR_MS).floorDiv(DIA_MS)

/** Resumen de una carrera para la regla: inicio, distancia y punto de partida. */
data class CarreraSugerida(val inicioMs: Long, val distanciaKm: Double, val inicio: Pair<Double, Double>? = null)

data class SugerenciaPrevia(val dia: Long, val rutaId: String)

sealed interface ResultadoSugerida {
    data class Ok(val ruta: Ruta, val dia: Long, val objetivoKm: Double) : ResultadoSugerida
    data class SinRuta(val dia: Long, val motivo: String) : ResultadoSugerida
}

const val MOTIVO_SIN_SEGURAS = "Ahora ninguna ruta tiene un nivel de seguridad suficiente para esta hora."
const val MOTIVO_TODAS_REPETIDAS = "Ya corriste o te sugerimos todas las rutas seguras en los últimos 3 días."

/** Media de las ultimas 5 carreras; 5 km sin historial. */
fun distanciaObjetivo(carreras: List<CarreraSugerida>): Double {
    val ultimas = carreras.filter { it.distanciaKm > 0 }.sortedByDescending { it.inicioMs }.take(5)
    return if (ultimas.isEmpty()) 5.0 else maxOf(0.5, ultimas.map { it.distanciaKm }.average())
}

fun distanciaM(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val r = 6_371_000.0
    val dLat = (b.first - a.first) * PI / 180
    val dLng = (b.second - a.second) * PI / 180
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(a.first * PI / 180) * cos(b.first * PI / 180) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
}

/** Una carrera cuenta como esa ruta si empezo a 300 m o menos de su inicio y midio 70-130 %. */
fun carreraCoincide(c: CarreraSugerida, ruta: Ruta): Boolean {
    val ini = ruta.puntos.firstOrNull() ?: return false
    val p = c.inicio ?: return false
    return distanciaM(p, ini) <= 300 && c.distanciaKm in (ruta.distanciaKm * 0.7)..(ruta.distanciaKm * 1.3)
}

/** Hash estable (FNV-1a) para rotar el desempate cada dia. */
private fun giro(texto: String): Long {
    var h = 0x811c9dc5L
    for (ch in texto) {
        h = h xor ch.code.toLong()
        h = (h * 0x01000193L) and 0xffffffffL
    }
    return h
}

fun elegirRutaSugerida(
    rutas: List<Ruta>,
    carreras: List<CarreraSugerida>,
    seguras: Set<String>,
    historial: List<SugerenciaPrevia>,
    ahoraMs: Long,
    ubicacion: Pair<Double, Double>? = null,
): ResultadoSugerida {
    val hoy = diaEcuador(ahoraMs)
    val objetivo = distanciaObjetivo(carreras)
    historial.firstOrNull { it.dia == hoy }?.let { previa ->
        rutas.firstOrNull { it.id == previa.rutaId && it.id in seguras }?.let { return ResultadoSugerida.Ok(it, hoy, objetivo) }
    }
    val candidatas = rutas.filter { it.id in seguras && it.distanciaKm > 0 }
    if (candidatas.isEmpty()) return ResultadoSugerida.SinRuta(hoy, MOTIVO_SIN_SEGURAS)
    val ventana = (hoy - DIAS_SIN_REPETIR)..hoy
    val sugeridas = historial.filter { it.dia in ventana }.map { it.rutaId }.toSet()
    val recientes = carreras.filter { diaEcuador(it.inicioMs) in ventana }
    val libres = candidatas.filter { r -> r.id !in sugeridas && recientes.none { carreraCoincide(it, r) } }
    if (libres.isEmpty()) return ResultadoSugerida.SinRuta(hoy, MOTIVO_TODAS_REPETIDAS)
    fun puntaje(r: Ruta): Double {
        var p = abs(r.distanciaKm - objetivo) / objetivo
        val ini = r.puntos.firstOrNull()
        if (ubicacion != null && ini != null) p += distanciaM(ubicacion, ini) / 10_000
        return p
    }
    val elegida = libres.sortedWith(compareBy<Ruta>({ puntaje(it) }, { giro(it.id + hoy) })).first()
    return ResultadoSugerida.Ok(elegida, hoy, objetivo)
}
