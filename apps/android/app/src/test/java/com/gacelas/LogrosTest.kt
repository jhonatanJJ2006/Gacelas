package com.gacelas

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.social.Logro
import com.gacelas.social.logrosCumplidos
import com.gacelas.social.progresoLogro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogrosTest {
    private fun c(km: Double, dia: Long) = Carrera(id = "$dia", titulo = "c", inicioMs = dia * 86_400_000L, segundos = 1800, distanciaKm = km)

    private val historicos = setOf(Logro.PrimeraCarrera, Logro.Km5, Logro.Km10, Logro.Racha7)

    @Test
    fun reglasSimples() {
        assertEquals(emptyList<Logro>(), logrosCumplidos(emptyList(), 0))
        assertEquals(listOf(Logro.PrimeraCarrera), logrosCumplidos(listOf(c(3.0, 1)), 100 * 86_400_000L).filter { it in historicos })
        assertEquals(listOf(Logro.Km5, Logro.Km10, Logro.PrimeraCarrera), logrosCumplidos(listOf(c(3.0, 1), c(10.5, 2)), 100 * 86_400_000L).filter { it in historicos })
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

    private fun conDesnivel(km: Double, dia: Long, subida: Int) = c(km, dia).copy(desnivelM = subida)

    @Test
    fun logrosQueEranMedallas() {
        val ahora = 100 * 86_400_000L
        assertEquals(26, Logro.entries.size)
        assertEquals(26, Logro.entries.map { it.titulo }.toSet().size)
        // Maraton y media maraton: una sola carrera.
        assertTrue(Logro.Km42 in logrosCumplidos(listOf(c(42.2, 1)), ahora))
        assertTrue(Logro.Km42 !in logrosCumplidos(listOf(c(42.0, 1)), ahora))
        assertEquals(41, progresoLogro(Logro.Km42, listOf(c(42.1, 1)), ahora))
        // Acumulados.
        val muchas = (1L..21L).map { c(5.0, it) }
        val cumplidos = logrosCumplidos(muchas, ahora)
        assertTrue(Logro.Total50 in cumplidos && Logro.Total100 in cumplidos)
        assertTrue(Logro.Total500 !in cumplidos)
        assertEquals(105, progresoLogro(Logro.Total500, muchas, ahora))
        // Desnivel acumulado (y el de una sola carrera sigue aparte).
        val subidas = logrosCumplidos(listOf(conDesnivel(5.0, 1, 600), conDesnivel(5.0, 2, 450)), ahora)
        assertTrue(Logro.Desnivel1000 in subidas && Logro.Escalador in subidas && Logro.Desnivel5000 !in subidas)
        // Las rachas cuentan la mejor: una racha de 7 dias rota ayer sigue dando el logro.
        val siete = (1L..7L).map { c(3.0, it) }
        val rachas = logrosCumplidos(siete, ahora)
        assertTrue(Logro.Racha3 in rachas && Logro.Racha7 in rachas && Logro.Racha30 !in rachas)
    }

    @Test
    fun cadaLogroTieneUnidadYMetaPositiva() {
        Logro.entries.forEach { assertTrue("${it.codigo} sin meta", it.meta > 0) }
        assertEquals(Logro.entries.map { it.categoria }, Logro.entries.sortedBy { it.categoria.ordinal }.map { it.categoria })
    }
}
