package com.gacelas.analitica

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.analitica.domain.PeriodoReporte
import com.gacelas.analitica.domain.ReportesPersonales
import com.gacelas.shared.Fechas
import com.gacelas.carreras.domain.modelo.PuntoGps
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportePersonalTest {
    private fun ms(mes: Int, dia: Int, hora: Int = 7) = LocalDateTime(2026, mes, dia, hora, 0).toInstant(Fechas.zona()).toEpochMilliseconds()
    private val jipiro = InicioRuta("loja-jipiro", "Parque Jipiro", -3.9735, -79.1987)
    private fun c(mes: Int, dia: Int, km: Double, seg: Int, enJipiro: Boolean = false) = Carrera(
        "$mes-$dia", "x", inicioMs = ms(mes, dia), segundos = seg, distanciaKm = km, desnivelM = (km * 10).toInt(),
        puntos = if (enJipiro) listOf(PuntoGps(jipiro.lat + 0.0005, jipiro.lng, ms(mes, dia))) else emptyList(),
    )
    // Miercoles 21 de octubre de 2026, igual que el test de la web.
    private val ref = ms(10, 21, 12)
    private val lista = listOf(
        c(10, 19, 5.0, 1500, true), c(10, 20, 10.0, 3300, true), c(10, 21, 3.0, 960), c(10, 14, 6.0, 1900), c(9, 10, 8.0, 2600),
    )

    @Test fun semanaYComparacion() {
        val r = ReportesPersonales.calcular(lista, PeriodoReporte.Semana, ref, listOf(jipiro))
        assertEquals(3, r.actual.carreras)
        assertEquals(18.0, r.actual.km, 0.001)
        assertEquals(320, r.actual.ritmoSegKm)
        assertEquals(6.0, r.previo.km, 0.001)
        assertEquals(200.0, r.variacionKm)
        assertEquals(7, r.serie.size)
        assertEquals(10.0, r.serie[1].km, 0.001)
        assertEquals(300, r.mejorRitmoSegKm)
        assertEquals(3, r.rachaActual)
        assertEquals("Parque Jipiro" to 2, r.rutasFavoritas.first())
        assertTrue(ReportesPersonales.textoCompartir(r).contains("18.0 km (+200% vs. periodo anterior)"))
    }

    @Test fun mesYAnio() {
        val mes = ReportesPersonales.calcular(lista, PeriodoReporte.Mes, ref)
        assertEquals(4, mes.actual.carreras)
        assertEquals(8.0, mes.previo.km, 0.001)
        assertEquals(31, mes.serie.size)
        val anio = ReportesPersonales.calcular(lista, PeriodoReporte.Anio, ref)
        assertEquals(12, anio.serie.size)
        assertEquals(24.0, anio.serie[9].km, 0.001)
        assertTrue(ReportesPersonales.csv(anio).startsWith("Periodo;Km\r\nEne;0"))
    }

    @Test fun vacio() {
        val r = ReportesPersonales.calcular(emptyList(), PeriodoReporte.Semana, ref)
        assertNull(r.actual.ritmoSegKm)
        assertNull(r.variacionKm)
        assertNull(r.mejorRitmoSegKm)
    }
}
