package com.gacelas.analitica.application

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.analitica.domain.ConsultaAnalitica
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.analitica.domain.PeriodoReporte
import com.gacelas.analitica.domain.ReportesPersonales
import com.gacelas.shared.Fechas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class CasosAnaliticaTest {
    private fun ms(dia: Int) = LocalDateTime(2026, 10, dia, 7, 0).toInstant(Fechas.zona()).toEpochMilliseconds()
    private val ruta = InicioRuta("r1", "Parque Jipiro", -3.97, -79.2)
    private val carreras = listOf(
        Carrera("1", "a", inicioMs = ms(19), segundos = 1500, distanciaKm = 5.0, puntos = listOf(PuntoGps(ruta.lat, ruta.lng, ms(19)))),
        Carrera("2", "b", inicioMs = ms(14), segundos = 1800, distanciaKm = 6.0),
    )

    @Test fun calcularIgualAlDominio() {
        val ahora = ms(21)
        assertEquals(
            ReportesPersonales.calcular(carreras, PeriodoReporte.Semana, ahora, listOf(ruta)),
            CalcularReportePersonal(carreras, PeriodoReporte.Semana, ahora, listOf(ruta)),
        )
    }

    @Test fun resumenDesdeElPuerto() = runTest {
        val fuente = MutableStateFlow(carreras)
        val consulta = object : ConsultaAnalitica {
            override val carreras = fuente
            override fun iniciosRutas() = listOf(ruta)
        }
        val r = VerResumenPersonal(consulta)(PeriodoReporte.Semana, ms(21)).first()
        assertEquals(1, r.actual.carreras)
        assertEquals(listOf("Parque Jipiro" to 1), r.rutasFavoritas)
    }

    @Test fun exportar() {
        val r = CalcularReportePersonal(carreras, PeriodoReporte.Semana, ms(21))
        assertEquals(ReportesPersonales.textoCompartir(r), ExportarReporte(r, FormatoExportacion.Texto))
        assertEquals(ReportesPersonales.csv(r), ExportarReporte(r, FormatoExportacion.Csv))
    }
}
