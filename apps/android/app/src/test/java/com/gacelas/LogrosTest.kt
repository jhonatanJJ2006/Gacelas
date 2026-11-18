package com.gacelas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.social.Logro
import com.gacelas.social.logrosCumplidos
import org.junit.Assert.assertEquals
import org.junit.Test

class LogrosTest {
    private fun c(km: Double, dia: Long) = Carrera(id = "$dia", titulo = "c", inicioMs = dia * 86_400_000L, segundos = 1800, distanciaKm = km)

    private val historicos = setOf(Logro.PrimeraCarrera, Logro.Km5, Logro.Km10, Logro.Racha7)

    @Test
    fun reglasSimples() {
        assertEquals(emptyList<Logro>(), logrosCumplidos(emptyList(), 0))
        assertEquals(listOf(Logro.PrimeraCarrera), logrosCumplidos(listOf(c(3.0, 1)), 100 * 86_400_000L).filter { it in historicos })
        assertEquals(listOf(Logro.PrimeraCarrera, Logro.Km5, Logro.Km10), logrosCumplidos(listOf(c(3.0, 1), c(10.5, 2)), 100 * 86_400_000L).filter { it in historicos })
    }

    @Test
    fun progresoHaciaCadaLogro() {
        val l = listOf(c(3.0, 1), c(10.5, 2), c(4.0, 3))
        assertEquals(10, com.gacelas.social.progresoLogro(Logro.Km15, l, 100 * 86_400_000L))
        assertEquals(17, com.gacelas.social.progresoLogro(Logro.Total50, l, 100 * 86_400_000L))
        assertEquals(3, com.gacelas.social.progresoLogro(Logro.Racha3, l, 100 * 86_400_000L))
        assertEquals(3, com.gacelas.social.progresoLogro(Logro.Carreras10, l, 100 * 86_400_000L))
        // 30 min para 10,5 km es ritmo < 5:00.
        assertEquals(1, com.gacelas.social.progresoLogro(Logro.Ritmo5, l, 100 * 86_400_000L))
        // Nunca pasa de la meta y los codigos no se repiten.
        assertEquals(1, com.gacelas.social.progresoLogro(Logro.PrimeraCarrera, l, 0))
        assertEquals(Logro.entries.size, Logro.entries.map { it.codigo }.toSet().size)
    }
}
