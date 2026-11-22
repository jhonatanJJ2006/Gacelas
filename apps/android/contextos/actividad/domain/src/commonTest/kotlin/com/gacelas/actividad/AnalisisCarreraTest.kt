package com.gacelas.actividad

import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.reglas.AnalisisCarrera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalisisCarreraTest {
    private fun punto(i: Int, alt: Double?) = PuntoGps(-4.0, -79.2 + i * 0.001, 1_700_000_000_000L + i * 1000, 5f, alt)

    @Test fun ritmoSoloConKilometrosAutomaticos() {
        val lista = listOf(Parcial(1, 1.0, 300), Parcial(2, 0.4, 120, manual = true), Parcial(3, 1.0, 310))
        assertEquals(listOf(1, 3), AnalisisCarrera.parcialesPorKm(lista).map { it.numero })
    }

    @Test fun elevacionContraDistancia() {
        val serie = AnalisisCarrera.serieElevacion(listOf(punto(0, 2100.0), punto(1, null), punto(2, 2140.0), punto(3, 2120.0)), 3)
        assertEquals(3, serie.size)
        assertEquals(0.0, serie.first().distanciaKm)
        assertEquals(2100.0, serie.first().altitudM)
        assertEquals(2120.0, serie.last().altitudM)
        assertTrue(serie.last().distanciaKm > serie[1].distanciaKm)
    }

    @Test fun tcxDeCorrer() {
        val xml = AnalisisCarrera.carreraATcx("Rio & <parque>", 1_761_040_800_000L, 400, 1.2, listOf(punto(0, 2100.0), punto(1, 2110.0)))
        assertTrue(xml.contains("Sport=\"Running\""))
        assertTrue(xml.contains("<TotalTimeSeconds>400</TotalTimeSeconds>"))
        assertTrue(xml.contains("<DistanceMeters>1200.0</DistanceMeters>"))
        assertTrue(xml.contains("Rio &amp; &lt;parque&gt;"))
        assertTrue(xml.contains("<AltitudeMeters>2100.0</AltitudeMeters>"))
        assertTrue(AnalisisCarrera.nombreTcx("Rio", 1_761_040_800_000L).endsWith(".tcx"))
    }
}
