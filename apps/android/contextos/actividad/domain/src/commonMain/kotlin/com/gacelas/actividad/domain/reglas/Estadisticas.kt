package com.gacelas.actividad.domain.reglas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.shared.Fechas

data class ResumenSemana(
    /** Dias de la semana (0 = lunes ... 6 = domingo) con al menos una carrera. */
    val diasCorridos: Set<Int>,
    val km: Double,
    val ritmoSegKm: Int?,
    val carreras: Int,
    /** Dias seguidos con carrera hasta hoy (o ayer). */
    val racha: Int,
)

/** Estado de un dia en la pantalla "Esta semana". */
enum class EstadoDia { Corrido, SinCarrera, Hoy, Futuro }

data class DiaSemana(val indice: Int, val inicioMs: Long, val estado: EstadoDia, val carreras: List<Carrera>) {
    val km: Double get() = carreras.sumOf { it.distanciaKm }
    val segundos: Int get() = carreras.sumOf { it.segundos }
    val desnivelM: Int get() = carreras.sumOf { it.desnivelM }
    val ritmoSegKm: Int? get() = if (km > 0.01) (segundos / km).toInt() else null
}

data class ResumenPeriodo(val carreras: Int, val km: Double, val segundos: Int, val desnivelM: Int)

object Estadisticas {
    private fun inicioDia(ms: Long): Long = Fechas.inicioDia(ms)

    fun inicioSemana(ahoraMs: Long): Long {
        val d = inicioDia(ahoraMs)
        return Fechas.sumarDias(d, -Fechas.diaSemana(d))
    }

    fun diaSemana(ms: Long): Int = Fechas.diaSemana(ms)

    /** Hora local (0-23) en que empezo una carrera. */
    fun hora(ms: Long): Int = Fechas.hora(ms)

    fun semana(carreras: List<Carrera>, ahoraMs: Long): ResumenSemana {
        val desde = inicioSemana(ahoraMs)
        val semana = carreras.filter { it.inicioMs >= desde && it.inicioMs <= ahoraMs + 86_400_000L }
        val km = semana.sumOf { it.distanciaKm }
        val seg = semana.sumOf { it.segundos }
        return ResumenSemana(
            diasCorridos = semana.map { diaSemana(it.inicioMs) }.toSet(),
            km = km,
            ritmoSegKm = if (km > 0.01) (seg / km).toInt() else null,
            carreras = semana.size,
            racha = racha(carreras, ahoraMs),
        )
    }

    /**
     * Los 7 dias (lunes a domingo) de la semana que empieza en [inicioSemanaMs]: con carrera,
     * pasados sin carrera, hoy sin carrera todavia o aun por llegar.
     */
    fun diasDeSemana(carreras: List<Carrera>, inicioSemanaMs: Long, hoyMs: Long): List<DiaSemana> {
        val hoy = inicioDia(hoyMs)
        return (0..6).map { i ->
            val dia = Fechas.sumarDias(inicioDia(inicioSemanaMs), i)
            val delDia = carreras.filter { inicioDia(it.inicioMs) == dia }.sortedBy { it.inicioMs }
            val estado = when {
                delDia.isNotEmpty() -> EstadoDia.Corrido
                dia == hoy -> EstadoDia.Hoy
                dia > hoy -> EstadoDia.Futuro
                else -> EstadoDia.SinCarrera
            }
            DiaSemana(i, dia, estado, delDia)
        }
    }

    fun racha(carreras: List<Carrera>, ahoraMs: Long): Int {
        val dias = carreras.map { inicioDia(it.inicioMs) }.toSet()
        var dia = inicioDia(ahoraMs)
        if (dia !in dias) dia = Fechas.sumarDias(dia, -1)
        var n = 0
        while (dia in dias) {
            n++
            dia = Fechas.sumarDias(dia, -1)
        }
        return n
    }

    fun mes(carreras: List<Carrera>, ahoraMs: Long): ResumenPeriodo {
        val c = Fechas.fecha(ahoraMs)
        val delMes = carreras.filter {
            val x = Fechas.fecha(it.inicioMs)
            x.year == c.year && x.monthNumber == c.monthNumber
        }
        return ResumenPeriodo(delMes.size, delMes.sumOf { it.distanciaKm }, delMes.sumOf { it.segundos }, delMes.sumOf { it.desnivelM })
    }
}
