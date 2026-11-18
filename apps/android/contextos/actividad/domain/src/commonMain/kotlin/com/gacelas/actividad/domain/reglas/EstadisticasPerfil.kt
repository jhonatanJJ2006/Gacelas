package com.gacelas.actividad.domain.reglas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.shared.Fechas
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Totales historicos; mejorRitmo = mejor ritmo medio (s/km) entre carreras de al menos 1 km. */
data class Historico(val carreras: Int, val km: Double, val segundos: Int, val desnivelM: Int, val mejorRitmoSegKm: Int?)

/** Kilometros de un mes (mes 1 = enero). */
data class KmMes(val anio: Int, val mes: Int, val etiqueta: String, val km: Double)

data class RecordPersonal(val etiqueta: String, val km: Double, val segundos: Int?)

/** Medalla del medallero: [actual] de [meta] en [unidad] ("km", "días"...), y como ganarla. */
data class Medalla(
    val id: String, val titulo: String, val descripcion: String, val lograda: Boolean,
    val actual: Int = if (lograda) 1 else 0, val meta: Int = 1, val unidad: String = "", val comoConseguirla: String = descripcion,
)

data class RetoMes(val titulo: String, val metaKm: Double, val km: Double, val diasRestantes: Int)

/**
 * Estadisticas de Perfil e Inicio (secciones de Stitch). Misma regla que
 * apps/web/src/contexts/actividad/domain/estadisticas.ts.
 */
object EstadisticasPerfil {
    val MESES_CORTOS = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    const val META_RETO_KM = 100.0
    private val DISTANCIAS = listOf("1K" to 1.0, "5K" to 5.0, "10K" to 10.0, "21K" to 21.0975)

    private fun sumar(l: List<Carrera>) = ResumenPeriodo(l.size, l.sumOf { it.distanciaKm }, l.sumOf { it.segundos }, l.sumOf { it.desnivelM })

    fun totalesSemana(carreras: List<Carrera>, ahoraMs: Long): ResumenPeriodo {
        val desde = Estadisticas.inicioSemana(ahoraMs)
        return sumar(carreras.filter { it.inicioMs >= desde && it.inicioMs <= ahoraMs + 86_400_000L })
    }

    fun historico(carreras: List<Carrera>): Historico {
        val s = sumar(carreras)
        val mejor = carreras.filter { it.distanciaKm >= 1 && it.segundos > 0 }.minOfOrNull { (it.segundos / it.distanciaKm).toInt() }
        return Historico(s.carreras, s.km, s.segundos, s.desnivelM, mejor)
    }

    fun kmMensual(carreras: List<Carrera>, ahoraMs: Long, n: Int = 6): List<KmMes> {
        val hoy = Fechas.fecha(ahoraMs)
        val primero = LocalDate(hoy.year, hoy.monthNumber, 1)
        return (n - 1 downTo 0).map { i ->
            val d = primero.minus(DatePeriod(months = i))
            val km = carreras.filter { val x = Fechas.fecha(it.inicioMs); x.year == d.year && x.monthNumber == d.monthNumber }.sumOf { it.distanciaKm }
            KmMes(d.year, d.monthNumber, MESES_CORTOS[d.monthNumber - 1], km)
        }
    }

    /** Distancia mas larga con record personal nuevo tras sumar [nueva], o null (igual que la web). */
    fun nuevoRecord(previas: List<Carrera>, nueva: Carrera): RecordPersonal? {
        val antes = records(previas)
        return records(previas + nueva).withIndex().lastOrNull { (i, r) -> r.segundos != null && (antes[i].segundos == null || r.segundos < antes[i].segundos!!) }?.value
    }

    fun records(carreras: List<Carrera>): List<RecordPersonal> = DISTANCIAS.map { (etiqueta, km) ->
        val tiempos = carreras.filter { it.distanciaKm >= km && it.segundos > 0 }
            .map { kotlin.math.round(it.segundos / it.distanciaKm * km).toInt() }.toMutableList()
        if (km == 1.0) carreras.forEach { c -> c.parciales.filter { it.distanciaKm >= 0.99 && it.segundos > 0 }.forEach { tiempos += it.segundos } }
        RecordPersonal(etiqueta, km, tiempos.minOrNull())
    }

    fun mejorRacha(carreras: List<Carrera>): Int {
        val dias = carreras.map { Fechas.fecha(it.inicioMs) }.distinct().sorted()
        var mejor = 0
        var actual = 0
        var previo: LocalDate? = null
        for (d in dias) {
            actual = if (previo != null && previo.plus(DatePeriod(days = 1)) == d) actual + 1 else 1
            mejor = maxOf(mejor, actual)
            previo = d
        }
        return mejor
    }

    fun medallas(carreras: List<Carrera>): List<Medalla> {
        val max = carreras.maxOfOrNull { it.distanciaKm } ?: 0.0
        val total = carreras.sumOf { it.distanciaKm }
        val subida = carreras.sumOf { it.desnivelM }
        val racha = mejorRacha(carreras)
        fun m(id: String, titulo: String, desc: String, actual: Double, meta: Int, unidad: String, como: String) =
            Medalla(id, titulo, desc, actual >= meta, kotlin.math.floor(actual).toInt().coerceAtMost(meta), meta, unidad, como)
        return listOf(
            m("primera", "Primera zancada", "Primera carrera guardada", carreras.size.toDouble(), 1, "carrera", "Guarda tu primera carrera."),
            m("5k", "Gacela 5K", "Una carrera de 5 km", max, 5, "km", "Corre 5 km en una sola salida."),
            m("10k", "Diez mil", "Una carrera de 10 km", max, 10, "km", "Corre 10 km en una sola salida."),
            Medalla("21k", "Media maratón", "Una carrera de 21 km", max >= 21.0975, minOf(max.toInt(), 21), 21, "km", "Completa 21,1 km en una sola salida."),
            Medalla("42k", "Maratón", "Una carrera de 42 km", max >= 42.195, minOf(max.toInt(), 42), 42, "km", "Completa los 42,2 km de un maratón."),
            m("centenario", "Centenario", "100 km acumulados", total, 100, "km", "Suma 100 km entre todas tus carreras."),
            m("500k", "Quinientos", "500 km acumulados", total, 500, "km", "Suma 500 km entre todas tus carreras."),
            m("1000k", "Mil kilómetros", "1000 km acumulados", total, 1000, "km", "Suma 1000 km entre todas tus carreras."),
            m("racha7", "Semana perfecta", "7 días seguidos corriendo", racha.toDouble(), 7, "días", "Corre 7 días seguidos sin saltarte ninguno."),
            m("racha30", "Mes de hierro", "30 días seguidos corriendo", racha.toDouble(), 30, "días", "Corre 30 días seguidos sin saltarte ninguno."),
            m("desnivel1000", "Andino", "1000 m de subida acumulada", subida.toDouble(), 1000, "m", "Acumula 1000 m de desnivel positivo entre tus carreras."),
            m("desnivel5000", "Cumbre", "5000 m de subida acumulada", subida.toDouble(), 5000, "m", "Acumula 5000 m de desnivel positivo: más que subir al Chimborazo desde el mar."),
        )
    }

    fun retoMes(carreras: List<Carrera>, ahoraMs: Long): RetoMes {
        val hoy = Fechas.fecha(ahoraMs)
        val ultimo = LocalDate(hoy.year, hoy.monthNumber, 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        return RetoMes("Reto ${MESES[hoy.monthNumber - 1]} Andino", META_RETO_KM, Estadisticas.mes(carreras, ahoraMs).km, ultimo.dayOfMonth - hoy.dayOfMonth)
    }

    /** Dia del mes y mes corto ("Oct") de un instante, para bloques de fecha. */
    fun diaYMes(ms: Long): Pair<Int, String> = Fechas.fecha(ms).let { it.dayOfMonth to MESES_CORTOS[it.monthNumber - 1] }

    /** Texto corto "Hace N min" para listas de actividad. */
    fun haceTiempo(ms: Long, ahoraMs: Long): String {
        val min = maxOf(0L, kotlin.math.round((ahoraMs - ms) / 60000.0).toLong())
        if (min < 1) return "Ahora"
        if (min < 60) return "Hace $min min"
        val h = kotlin.math.round(min / 60.0).toLong()
        if (h < 24) return "Hace $h h"
        val d = kotlin.math.round(h / 24.0).toLong()
        return if (d == 1L) "Ayer" else "Hace $d días"
    }
}
