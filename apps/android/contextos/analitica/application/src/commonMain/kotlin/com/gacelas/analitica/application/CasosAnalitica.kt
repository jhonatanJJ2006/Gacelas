package com.gacelas.analitica.application

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.analitica.domain.ConsultaAnalitica
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.analitica.domain.PeriodoReporte
import com.gacelas.analitica.domain.ReportePersonal
import com.gacelas.analitica.domain.ReportesPersonales
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reporte personal de un periodo; misma regla que ReportesPersonales.calcular. */
object CalcularReportePersonal {
    operator fun invoke(carreras: List<Carrera>, periodo: PeriodoReporte, ahoraMs: Long, rutas: List<InicioRuta> = emptyList()): ReportePersonal =
        ReportesPersonales.calcular(carreras, periodo, ahoraMs, rutas)
}

/** Reporte personal que se actualiza cuando cambian las carreras. */
class VerResumenPersonal(private val consulta: ConsultaAnalitica) {
    operator fun invoke(periodo: PeriodoReporte, ahoraMs: Long): Flow<ReportePersonal> {
        val rutas = consulta.iniciosRutas()
        return consulta.carreras.map { CalcularReportePersonal(it, periodo, ahoraMs, rutas) }
    }
}

enum class FormatoExportacion { Texto, Csv }

/** Contenido para compartir o exportar un reporte. */
object ExportarReporte {
    operator fun invoke(r: ReportePersonal, formato: FormatoExportacion): String = when (formato) {
        FormatoExportacion.Texto -> ReportesPersonales.textoCompartir(r)
        FormatoExportacion.Csv -> ReportesPersonales.csv(r)
    }
}
