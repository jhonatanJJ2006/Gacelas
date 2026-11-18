package com.gacelas.rutas

import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.rutas.domain.reglas.CondicionClima
import com.gacelas.rutas.domain.reglas.RutasClima
import kotlin.test.Test
import kotlin.test.assertTrue

class RutasClimaTest {
    @Test fun conLluviaNoMandaALaMontana() {
        val r = RutasClima.recomendar(CondicionClima(lluvia = true, calor = false, viento = false), RutasLoja.todas, n = 50)
        assertTrue(r.isNotEmpty())
        assertTrue(r.none { RutasClima.deMontana(it) }, "con lluvia salio una ruta de montaña: ${r.filter { RutasClima.deMontana(it) }.map { it.id }}")
        assertTrue(r.none { it.id == "loja-villonaco" })
    }

    @Test fun conBuenDiaIncluyeMontana() {
        val r = RutasClima.recomendar(CondicionClima(lluvia = false, calor = false, viento = false), RutasLoja.todas, n = 50)
        assertTrue(r.any { RutasClima.deMontana(it) })
    }

    @Test fun conVientoEvitaLasAltas() {
        val r = RutasClima.recomendar(CondicionClima(lluvia = false, calor = false, viento = true), RutasLoja.todas, n = 50)
        assertTrue(r.all { it.elevacionM < RutasClima.DESNIVEL_ALTO_M })
    }
}
