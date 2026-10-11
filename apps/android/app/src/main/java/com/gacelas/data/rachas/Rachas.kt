package com.gacelas.data.rachas

import com.gacelas.data.carreras.CarreraGuardada
import java.util.Calendar

/** Estado de rachas. Misma logica que public.recalcular_racha en Supabase. */
data class Racha(
    val diaria: Int = 0,
    val mejorDiaria: Int = 0,
    val ultimoDiaMs: Long? = null,
    val semanal: Int = 0,
    val mejorSemanal: Int = 0,
    val metaSemanal: Int = 3,
    val carrerasSemana: Int = 0,
)

data class CeldaCalor(val diaMs: Long, val km: Double, val futuro: Boolean)

object Rachas {
    private fun cal(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms; firstDayOfWeek = Calendar.MONDAY }

    fun inicioDia(ms: Long): Long = cal(ms).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun sumarDias(ms: Long, n: Int): Long = cal(ms).apply { add(Calendar.DAY_OF_MONTH, n) }.timeInMillis

    fun inicioSemana(ms: Long): Long {
        val c = cal(inicioDia(ms))
        c.add(Calendar.DAY_OF_MONTH, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        return c.timeInMillis
    }

    private class Islas(val mejor: Int, val finales: Map<Long, Int>, val ultimo: Long?)

    private fun islas(claves: Collection<Long>, paso: (Long) -> Long): Islas {
        var mejor = 0
        var actual = 0
        var previo: Long? = null
        val finales = HashMap<Long, Int>()
        for (k in claves.toSortedSet()) {
            actual = if (previo != null && paso(previo) == k) actual + 1 else 1
            mejor = maxOf(mejor, actual)
            finales[k] = actual
            previo = k
        }
        return Islas(mejor, finales, previo)
    }

    fun calcular(carreras: List<CarreraGuardada>, ahoraMs: Long, metaSemanal: Int = 3): Racha {
        val hoy = inicioDia(ahoraMs)
        val dias = islas(carreras.map { inicioDia(it.inicioMs) }) { sumarDias(it, 1) }
        val diaria = dias.ultimo?.takeIf { it >= sumarDias(hoy, -1) }?.let { dias.finales[it] } ?: 0

        val porSemana = carreras.groupingBy { inicioSemana(it.inicioMs) }.eachCount()
        val semanas = islas(porSemana.filterValues { it >= metaSemanal }.keys) { sumarDias(it, 7) }
        val actual = inicioSemana(ahoraMs)
        val semanal = semanas.ultimo?.takeIf { it >= sumarDias(actual, -7) }?.let { semanas.finales[it] } ?: 0

        return Racha(diaria, dias.mejor, dias.ultimo, semanal, semanas.mejor, metaSemanal, porSemana[actual] ?: 0)
    }

    /** La racha diaria sigue viva pero hoy aun no se corrio. */
    fun enRiesgo(r: Racha, ahoraMs: Long): Boolean = r.diaria > 0 && (r.ultimoDiaMs ?: Long.MAX_VALUE) < inicioDia(ahoraMs)

    /** `semanas` columnas de lunes a domingo que terminan en la semana actual. */
    fun mapaCalor(carreras: List<CarreraGuardada>, ahoraMs: Long, semanas: Int = 12): List<List<CeldaCalor>> {
        val km = carreras.groupBy { inicioDia(it.inicioMs) }.mapValues { (_, l) -> l.sumOf { it.distanciaKm } }
        val primera = sumarDias(inicioSemana(ahoraMs), -7 * (semanas - 1))
        val hoy = inicioDia(ahoraMs)
        return List(semanas) { w ->
            List(7) { d ->
                val dia = sumarDias(primera, w * 7 + d)
                CeldaCalor(dia, km[dia] ?: 0.0, dia > hoy)
            }
        }
    }
}
