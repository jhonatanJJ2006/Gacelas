package com.gacelas.carreras.domain.reglas

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.shared.Fechas
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class PeriodoReporte(val etiqueta: String) { Semana("Semana"), Mes("Mes"), Anio("Año") }

data class TotalesReporte(val carreras: Int, val km: Double, val segundos: Int, val desnivelM: Int) {
    /** Segundos por km, null sin distancia. */
    val ritmoSegKm: Int? get() = if (km > 0) (segundos / km).roundToInt() else null
}

/** Inicio de una ruta para asociar carreras (sin depender del contexto rutas). */
data class InicioRuta(val id: String, val nombre: String, val lat: Double, val lng: Double)

data class BarraReporte(val etiqueta: String, val km: Double)

data class ReportePersonal(
    val periodo: PeriodoReporte,
    val desdeMs: Long,
    val hastaMs: Long,
    val actual: TotalesReporte,
    val previo: TotalesReporte,
    /** Variacion porcentual de km respecto al periodo anterior, null si el anterior es 0. */
    val variacionKm: Double?,
    val masLargaKm: Double,
    val mejorRitmoSegKm: Int?,
    val masDesnivelM: Int,
    val rachaActual: Int,
    val mejorRacha: Int,
    val serie: List<BarraReporte>,
    val rutasFavoritas: List<Pair<String, Int>>,
)

/**
 * Reporte personal del corredor (semana desde el lunes, mes y año calendario). Misma regla que
 * apps/web/src/lib/reportesPersonales.ts y public.reporte_personal (migracion 023).
 */
object ReportesPersonales {
    private val DIAS = listOf("L", "M", "X", "J", "V", "S", "D")

    private fun rango(p: PeriodoReporte, refMs: Long, atras: Int = 0): Pair<LocalDate, LocalDate> {
        val hoy = Fechas.fecha(refMs)
        return when (p) {
            PeriodoReporte.Semana -> Fechas.lunes(hoy).plus(DatePeriod(days = -7 * atras)).let { it to it.plus(DatePeriod(days = 7)) }
            PeriodoReporte.Mes -> LocalDate(hoy.year, hoy.monthNumber, 1).plus(DatePeriod(months = -atras)).let { it to it.plus(DatePeriod(months = 1)) }
            PeriodoReporte.Anio -> LocalDate(hoy.year - atras, 1, 1).let { it to it.plus(DatePeriod(years = 1)) }
        }
    }

    fun totales(l: List<Carrera>) = TotalesReporte(
        l.size, (l.sumOf { it.distanciaKm } * 100).roundToInt() / 100.0, l.sumOf { it.segundos }, l.sumOf { it.desnivelM },
    )

    private fun metros(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val x = (bLng - aLng) * cos((aLat + bLat) / 2 * kotlin.math.PI / 180) * 111_320
        val y = (bLat - aLat) * 110_540
        return hypot(x, y)
    }

    /** Ruta cuyo inicio esta a menos de 150 m del inicio de la carrera. */
    fun rutaDeCarrera(c: Carrera, rutas: List<InicioRuta>): InicioRuta? {
        val p = c.puntos.firstOrNull() ?: return null
        return rutas.map { it to metros(p.lat, p.lng, it.lat, it.lng) }.filter { it.second < 150 }.minByOrNull { it.second }?.first
    }

    fun calcular(carreras: List<Carrera>, periodo: PeriodoReporte, refMs: Long, rutas: List<InicioRuta> = emptyList()): ReportePersonal {
        val (d, h) = rango(periodo, refMs)
        val (pd, ph) = rango(periodo, refMs, 1)
        fun en(a: LocalDate, b: LocalDate): List<Carrera> {
            val x = Fechas.inicio(a); val y = Fechas.inicio(b)
            return carreras.filter { it.inicioMs in x until y }
        }
        val actuales = en(d, h)
        val actual = totales(actuales)
        val previo = totales(en(pd, ph))
        val serie = if (periodo == PeriodoReporte.Anio) {
            (0 until 12).map { m ->
                val a = d.plus(DatePeriod(months = m))
                BarraReporte(EstadisticasPerfil.MESES_CORTOS[m], totales(en(a, a.plus(DatePeriod(months = 1)))).km)
            }
        } else {
            generateSequence(d) { it.plus(DatePeriod(days = 1)) }.takeWhile { it < h }.mapIndexed { i, dia ->
                BarraReporte(if (periodo == PeriodoReporte.Semana) DIAS[i] else dia.dayOfMonth.toString(), totales(en(dia, dia.plus(DatePeriod(days = 1)))).km)
            }.toList()
        }
        val conKm = actuales.filter { it.distanciaKm >= 1 }
        val finMs = Fechas.inicio(h)
        val racha = Rachas.calcular(carreras, minOf(refMs, finMs - 1))
        return ReportePersonal(
            periodo = periodo,
            desdeMs = Fechas.inicio(d),
            hastaMs = finMs,
            actual = actual,
            previo = previo,
            variacionKm = if (previo.km > 0) ((actual.km - previo.km) / previo.km * 1000).roundToInt() / 10.0 else null,
            masLargaKm = actuales.maxOfOrNull { it.distanciaKm } ?: 0.0,
            mejorRitmoSegKm = conKm.minOfOrNull { (it.segundos / it.distanciaKm).roundToInt() },
            masDesnivelM = actuales.maxOfOrNull { it.desnivelM } ?: 0,
            rachaActual = racha.diaria,
            mejorRacha = racha.mejorDiaria,
            serie = serie,
            rutasFavoritas = actuales.mapNotNull { rutaDeCarrera(it, rutas)?.nombre }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value },
        )
    }

    fun ritmoTexto(s: Int?) = if (s == null) "--" else "${s / 60}:${(s % 60).toString().padStart(2, '0')}"

    fun duracionTexto(s: Int): String {
        val h = s / 3600; val m = ((s % 3600) / 60.0).roundToInt()
        return if (h > 0) "$h h $m min" else "$m min"
    }

    /** Texto para compartir (igual que la web). */
    fun textoCompartir(r: ReportePersonal): String {
        val v = r.variacionKm?.let { " (${if (it >= 0) "+" else ""}${formato(it)}% vs. periodo anterior)" } ?: ""
        return listOf(
            "Mi ${r.periodo.etiqueta.lowercase()} en Gacelas: ${formato1(r.actual.km)} km$v",
            "${r.actual.carreras} carreras, ${duracionTexto(r.actual.segundos)}, ritmo ${ritmoTexto(r.actual.ritmoSegKm)} /km",
            "Racha: ${r.rachaActual} ${if (r.rachaActual == 1) "día" else "días"}",
        ).joinToString("\n")
    }

    /** CSV con ; para Excel en español. */
    fun csv(r: ReportePersonal): String = buildString {
        append("Periodo;Km\r\n")
        r.serie.forEach { append("${it.etiqueta};${formato(it.km).replace('.', ',')}\r\n") }
        append("\r\nCarreras;${r.actual.carreras}\r\nKm;${formato(r.actual.km).replace('.', ',')}\r\n")
        append("Tiempo (s);${r.actual.segundos}\r\nRitmo medio;${ritmoTexto(r.actual.ritmoSegKm)}\r\nDesnivel (m);${r.actual.desnivelM}\r\n")
    }

    private fun formato(x: Double) = if (x == x.toLong().toDouble()) x.toLong().toString() else ((x * 10).roundToInt() / 10.0).toString()
    fun formato1(x: Double): String { val t = (x * 10).roundToInt(); return "${t / 10}.${kotlin.math.abs(t % 10)}" }
}
