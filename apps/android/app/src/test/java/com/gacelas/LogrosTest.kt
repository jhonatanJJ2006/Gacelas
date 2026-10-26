package com.gacelas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.social.Logro
import com.gacelas.social.logrosCumplidos
import org.junit.Assert.assertEquals
import org.junit.Test

class LogrosTest {
    private fun c(km: Double, dia: Long) = Carrera(id = "$dia", titulo = "c", inicioMs = dia * 86_400_000L, segundos = 1800, distanciaKm = km)

    @Test
    fun reglasSimples() {
        assertEquals(emptyList<Logro>(), logrosCumplidos(emptyList(), 0))
        assertEquals(listOf(Logro.PrimeraCarrera), logrosCumplidos(listOf(c(3.0, 1)), 100 * 86_400_000L))
        assertEquals(listOf(Logro.PrimeraCarrera, Logro.Km5, Logro.Km10), logrosCumplidos(listOf(c(3.0, 1), c(10.5, 2)), 100 * 86_400_000L))
    }
}
