package com.gacelas.comunidad.domain.reglas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.Publicacion

data class FilaRanking(val nombre: String, val km: Double, val propio: Boolean)

/** Kilometros de la semana. El club reutiliza la misma suma, limitada a sus miembros. */
object Ranking {
    fun semanal(propias: List<Carrera>, nombre: String, remotas: List<Publicacion>, ahora: Long): List<FilaRanking> {
        val desde = Estadisticas.inicioSemana(ahora)
        val mapa = linkedMapOf<String, FilaRanking>()
        fun sumar(clave: String, n: String, km: Double, propio: Boolean) {
            val previa = mapa[clave]
            mapa[clave] = FilaRanking(n, (previa?.km ?: 0.0) + km, propio)
        }
        propias.filter { it.inicioMs >= desde }.forEach { sumar("yo", nombre, it.distanciaKm, true) }
        remotas.filter { it.inicioMs >= desde }.forEach { p ->
            sumar(p.autorId ?: p.autor, p.autor, p.distanciaKm, false)
        }
        if ("yo" !in mapa) mapa["yo"] = FilaRanking(nombre, 0.0, true)
        return mapa.values.sortedByDescending { it.km }.take(5)
    }

    fun club(
        propias: List<Carrera>,
        nombre: String,
        miId: String?,
        remotas: List<Publicacion>,
        miembros: List<MiembroClub>,
        ahora: Long,
    ): List<FilaRanking> {
        val ids = miembros.filter { it.activo }.map { it.usuarioId }.toSet()
        val soy = miId != null && miId in ids
        return semanal(
            if (soy) propias else emptyList(),
            nombre,
            remotas.filter { it.autorId in ids && it.autorId != miId },
            ahora,
        ).filter { soy || !it.propio }
    }
}
