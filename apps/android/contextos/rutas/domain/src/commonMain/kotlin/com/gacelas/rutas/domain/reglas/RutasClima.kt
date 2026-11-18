package com.gacelas.rutas.domain.reglas

import com.gacelas.rutas.domain.modelo.CategoriaRuta
import com.gacelas.rutas.domain.modelo.Ruta

/** Condiciones del dia que cambian que ruta conviene (vienen del pronostico diario). */
data class CondicionClima(val lluvia: Boolean, val calor: Boolean, val viento: Boolean)

/** Rutas segun el clima: con lluvia nada de montana, sendero ni tierra; con viento, nada alto. */
object RutasClima {
    /** Desnivel a partir del cual una ruta se considera de subida fuerte. */
    const val DESNIVEL_ALTO_M = 150

    private val ETIQUETAS_MONTANA = setOf("montana", "sendero")

    /** Montana, sendero, superficie de tierra o mucho desnivel: resbala y se embarra con lluvia. */
    fun deMontana(r: Ruta): Boolean =
        r.categoria == CategoriaRuta.Montana || r.categoria == CategoriaRuta.Sendero ||
            r.etiquetas.any { it in ETIQUETAS_MONTANA } || r.superficie == "tierra" || r.elevacionM >= DESNIVEL_ALTO_M

    fun recomendar(c: CondicionClima, rutas: List<Ruta>, n: Int = 5): List<Ruta> {
        val aptas = when {
            c.lluvia -> rutas.filterNot(::deMontana)
            c.viento -> rutas.filter { it.elevacionM < DESNIVEL_ALTO_M }
            else -> rutas
        }
        val orden = when {
            // Lluvia: primero pavimento y pista, luego las mas planas.
            c.lluvia -> aptas.sortedWith(compareBy<Ruta>({ it.superficie !in setOf("pavimento", "tartan") }, { it.elevacionM }))
            // Calor: cortas y con poco desnivel.
            c.calor -> aptas.sortedWith(compareBy<Ruta>({ it.distanciaKm }, { it.elevacionM }))
            // Buen dia: las oficiales y panoramicas primero.
            else -> aptas.sortedWith(compareBy<Ruta>({ !it.oficial }, { "panoramica" !in it.etiquetas }))
        }
        return orden.take(n)
    }

    fun motivo(c: CondicionClima): String = when {
        c.lluvia -> "Día de lluvia: te sugerimos rutas urbanas y planas, sin montaña ni senderos de tierra."
        c.viento -> "Viento fuerte: mejor rutas bajas, lejos de cerros y miradores."
        c.calor -> "Día caluroso: sal temprano y elige rutas cortas con poco desnivel."
        else -> "Buen día para correr: cualquier ruta te va bien, incluso las de montaña."
    }
}
