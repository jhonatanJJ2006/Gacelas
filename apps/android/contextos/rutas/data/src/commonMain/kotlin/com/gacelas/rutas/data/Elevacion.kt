package com.gacelas.rutas.data

import com.gacelas.shared.fmt
import com.gacelas.shared.rad
import com.gacelas.shared.redondear
import kotlin.concurrent.Volatile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Ruta elegida con "Correr esta ruta": se registra como corrida al guardar la carrera. */
object RutaActiva {
    @Volatile var id: String? = null
    /** Ruta que se navega (puede ser una de ejemplo). */
    @Volatile var navegarId: String? = null
}

/** Perfil de elevacion con Open-Meteo (igual que la web, lib/elevacion.ts). */
object Elevacion {
    private fun hv(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val p1 = rad(a.first); val p2 = rad(b.first); val dl = rad(b.second - a.second)
        val h = sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
        return 2 * 6_371_000 * asin(sqrt(h))
    }

    fun muestrear(p: List<Pair<Double, Double>>, n: Int = 60): List<Pair<Double, Double>> {
        if (p.size < 2) return p
        val acum = DoubleArray(p.size).also { for (i in 1 until p.size) it[i] = it[i - 1] + hv(p[i - 1], p[i]) }
        val total = acum.last()
        if (total == 0.0) return listOf(p.first())
        var j = 1
        return (0 until n).map { k ->
            val d = total * k / (n - 1)
            while (j < p.lastIndex && acum[j] < d) j++
            val t = if (acum[j] == acum[j - 1]) 0.0 else (d - acum[j - 1]) / (acum[j] - acum[j - 1])
            val a = p[j - 1]; val b = p[j]
            (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
        }
    }

    fun desnivelPositivo(h: List<Int>, umbral: Int = 2): Int {
        var total = 0; var base = h.firstOrNull() ?: 0
        h.drop(1).forEach { x -> if (x - base >= umbral) { total += x - base; base = x } else if (x < base) base = x }
        return total
    }

    fun url(m: List<Pair<Double, Double>>) = "https://api.open-meteo.com/v1/elevation?latitude=" +
        m.joinToString(",") { "%.5f".fmt(it.first) } + "&longitude=" + m.joinToString(",") { "%.5f".fmt(it.second) }

    fun leer(json: String): List<Int> = (Json.parseToJsonElement(json).jsonObject["elevation"] as? JsonArray)
        ?.mapNotNull { it.jsonPrimitive.doubleOrNull?.let(::redondear)?.toInt() }.orEmpty()

    suspend fun perfil(puntos: List<Pair<Double, Double>>): List<Int> = run {
        runCatching { leer(com.gacelas.shared.Red.texto(url(muestrear(puntos)))) }.getOrDefault(emptyList())
    }
}
