package com.gacelas.rutas.domain.reglas

import com.gacelas.rutas.domain.modelo.Ruta

import com.gacelas.rutas.domain.modelo.*
import com.gacelas.shared.rad
import kotlin.math.cos
import kotlin.math.hypot

/** Filtra rutas segun pestana, guardadas y texto de busqueda. */
fun filtrarRutas(rutas: List<Ruta>, pestana: PestanaRutas, guardadas: Set<String>, usuarioId: String, consulta: String = ""): List<Ruta> {
    val base = when (pestana) {
        PestanaRutas.Explorar -> rutas
        PestanaRutas.MisRutas -> rutas.filter { it.autorId == usuarioId || it.propia }
        PestanaRutas.Guardadas -> rutas.filter { it.id in guardadas }
    }
    val q = consulta.trim().lowercase()
    return if (q.isEmpty()) base else base.filter { it.nombre.lowercase().contains(q) || it.descripcion.lowercase().contains(q) }
}

fun aplicarFiltros(rutas: List<Ruta>, f: FiltrosRuta) = rutas.filter { r ->
    (f.dificultad == null || r.dificultad == f.dificultad) &&
        when (f.distancia) {
            FiltroDistancia.Todas -> true
            FiltroDistancia.Hasta5 -> r.distanciaKm <= 5
            FiltroDistancia.De5a10 -> r.distanciaKm > 5 && r.distanciaKm <= 10
            FiltroDistancia.Mas10 -> r.distanciaKm > 10
        } &&
        (f.etiqueta == null || f.etiqueta in r.etiquetas) &&
        (f.categoria == null || r.categoria == f.categoria)
}

fun ordenarPorCercania(rutas: List<Ruta>, cerca: Map<String, Double>) = rutas.sortedBy { cerca[it.id] ?: Double.MAX_VALUE }

private fun distanciaASegmentoM(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val kx = 111_320 * cos(rad(a.first)); val ky = 110_540.0
    val px = (p.second - a.second) * kx; val py = (p.first - a.first) * ky
    val bx = (b.second - a.second) * kx; val by = (b.first - a.first) * ky
    val l2 = bx * bx + by * by
    val t = if (l2 == 0.0) 0.0 else ((px * bx + py * by) / l2).coerceIn(0.0, 1.0)
    return hypot(px - t * bx, py - t * by)
}

/** Douglas-Peucker (lat, lng): igual que la web (lib/rutas.ts). */
fun simplificar(puntos: List<Pair<Double, Double>>, toleranciaM: Double = 8.0): List<Pair<Double, Double>> {
    if (puntos.size <= 2) return puntos
    val conservar = BooleanArray(puntos.size).also { it[0] = true; it[puntos.lastIndex] = true }
    val pila = ArrayDeque(listOf(0 to puntos.lastIndex))
    while (pila.isNotEmpty()) {
        val (i, j) = pila.removeLast()
        var max = -1.0; var k = -1
        for (x in i + 1 until j) {
            val d = distanciaASegmentoM(puntos[x], puntos[i], puntos[j])
            if (d > max) { max = d; k = x }
        }
        if (max > toleranciaM && k > 0) { conservar[k] = true; pila.add(i to k); pila.add(k to j) }
    }
    return puntos.filterIndexed { i, _ -> conservar[i] }
}

fun trazoParaRuta(puntos: List<Pair<Double, Double>>, max: Int = 500): List<Pair<Double, Double>> {
    var tol = 5.0
    var r = simplificar(puntos, tol)
    while (r.size > max) { tol *= 2; r = simplificar(puntos, tol) }
    return r
}
