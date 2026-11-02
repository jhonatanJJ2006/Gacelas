package com.gacelas.clima

import com.gacelas.clima.domain.Coordenadas
import com.gacelas.clima.domain.LecturaClima
import com.gacelas.clima.domain.ReglasClima
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReglasClimaTest {
    @Test fun codigosWmoEnEspanol() {
        val casos = mapOf(
            0 to "despejado", 1 to "mayormente despejado", 2 to "parcialmente nublado", 3 to "nublado",
            45 to "niebla", 48 to "niebla", 51 to "llovizna", 57 to "llovizna", 61 to "lluvia", 67 to "lluvia",
            71 to "nieve", 77 to "nieve", 86 to "nieve", 80 to "chubascos", 82 to "chubascos", 95 to "tormenta", 99 to "tormenta",
        )
        casos.forEach { (c, t) -> assertEquals(t, ReglasClima.descripcion(c, esDeDia = true), "codigo $c") }
        assertEquals("noche despejada", ReglasClima.descripcion(0, esDeDia = false))
        assertEquals("noche mayormente despejada", ReglasClima.descripcion(1, esDeDia = false))
        assertEquals("nublado", ReglasClima.descripcion(3, esDeDia = false))
        assertEquals("nublado", ReglasClima.descripcion(1234, esDeDia = true))
    }

    @Test fun refrescoMinimoDe15Minutos() {
        val t = 1_000_000_000L
        assertTrue(ReglasClima.debeRefrescar(null, t))
        assertFalse(ReglasClima.debeRefrescar(t, t + 14 * 60_000L))
        assertTrue(ReglasClima.debeRefrescar(t, t + 15 * 60_000L))
        assertTrue(ReglasClima.debeRefrescar(t, t - 1), "reloj hacia atras")
    }

    @Test fun etiquetaDeLugar() {
        assertEquals("Loja", ReglasClima.etiquetaLugar(null, "Quito"))
        assertEquals("Loja", ReglasClima.etiquetaLugar(Coordenadas(-4.03, -79.20), "Malacatos"))
        assertEquals("Quito", ReglasClima.etiquetaLugar(Coordenadas(-0.18, -78.47), " Quito "))
        assertEquals("Tu zona", ReglasClima.etiquetaLugar(Coordenadas(-2.17, -79.92), null))
        assertEquals("Tu zona", ReglasClima.etiquetaLugar(Coordenadas(-2.17, -79.92), ""))
    }

    @Test fun coordenadasAproximadasA2Decimales() {
        assertEquals(Coordenadas(-3.99, -79.2), ReglasClima.aproximar(ReglasClima.LOJA))
    }

    @Test fun temperaturaRedondeada() {
        val c = ReglasClima.aClima(LecturaClima(16.5, 3, true), "Loja", 5L)
        assertEquals(17, c.temperatura); assertEquals("nublado", c.descripcion)
        assertEquals(-2, ReglasClima.aClima(LecturaClima(-2.4, 0, true), "X", 0).temperatura)
    }
}
