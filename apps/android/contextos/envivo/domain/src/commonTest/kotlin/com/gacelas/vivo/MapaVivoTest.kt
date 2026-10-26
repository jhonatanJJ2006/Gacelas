package com.gacelas.vivo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapaVivoTest {
    private fun corredor(id: String, hace: Long, ahora: Long) = CorredorEnMapa(id, id, null, -3.99, -79.2, true, ahora - hace)

    @Test fun visiblesOcultaCaducadosYAlPropio() {
        val ahora = 1_000_000L
        val lista = listOf(corredor("a", 10_000, ahora), corredor("b", 121_000, ahora), corredor("yo", 1_000, ahora))
        assertEquals(listOf("a"), MapaVivo.visibles(lista, ahora, "yo").map { it.userId })
    }

    @Test fun tocaPublicarRespetaIntervalos() {
        assertTrue(MapaVivo.tocaPublicar(null, null, 0, true))
        assertFalse(MapaVivo.tocaPublicar(0, true, 14_999, true))
        assertTrue(MapaVivo.tocaPublicar(0, true, 15_000, true))
        assertFalse(MapaVivo.tocaPublicar(0, false, 59_999, false))
        assertTrue(MapaVivo.tocaPublicar(0, false, 60_000, false))
        assertTrue(MapaVivo.tocaPublicar(0, false, 1, true), "cambiar de estado publica")
    }
}
