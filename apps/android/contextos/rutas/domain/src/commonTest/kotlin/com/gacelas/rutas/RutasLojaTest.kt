package com.gacelas.rutas

import com.gacelas.rutas.domain.modelo.CategoriaRuta
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.rutas.domain.reglas.aplicarFiltros
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RutasLojaTest {
    @Test fun rutasOficialesCompletas() {
        val t = RutasLoja.todas
        assertTrue(t.size in 20..30)
        assertEquals(t.size, t.map { it.id }.toSet().size)
        t.forEach {
            assertTrue(it.oficial, it.id)
            assertTrue(it.categoria != null, it.id)
            assertTrue(it.distanciaKm > 0, it.id)
            assertTrue(it.fuente.orEmpty().contains("OpenStreetMap"), it.id)
        }
        assertTrue(CategoriaRuta.entries.filter { c -> t.any { it.categoria == c } }.size >= 4)
    }

    @Test fun filtroPorCategoriaYFusion() {
        val montanas = aplicarFiltros(RutasLoja.todas, FiltrosRuta(categoria = CategoriaRuta.Montana))
        assertTrue(montanas.isNotEmpty())
        assertTrue(montanas.all { it.categoria == CategoriaRuta.Montana })
        val primera = RutasLoja.todas.first()
        val fusion = RutasLoja.conOficiales(listOf(primera.copy(nombre = "Del servidor")))
        assertEquals(RutasLoja.todas.size, fusion.size)
        assertEquals("Del servidor", fusion.first().nombre)
        assertEquals(CategoriaRuta.Parque, CategoriaRuta.desdeClave("parque"))
    }
}
