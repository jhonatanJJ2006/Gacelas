package com.gacelas.ui.movimiento

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CifrasAnimadasTest {
    @Test
    fun partirConservaPrefijoDecimalesSeparadorYUnidad() {
        val c = assertNotNull(partirCifra("+12,4 km"))
        assertEquals("+", c.prefijo)
        assertEquals(12.4, c.valor, 1e-9)
        assertEquals(1, c.decimales)
        assertEquals(',', c.separador)
        assertEquals(" km", c.sufijo)
    }

    @Test
    fun enteroSinSeparador() {
        val c = assertNotNull(partirCifra("75%"))
        assertEquals(75.0, c.valor, 1e-9)
        assertEquals(0, c.decimales)
        assertNull(c.separador)
        assertEquals("%", c.sufijo)
    }

    @Test
    fun textosSinNumeroAnimableNoSePartenCuandoNoAplica() {
        assertNull(partirCifra("Aun ninguno"))
        assertNull(partirCifra("1.234"))
        assertNull(partirCifra("5:30"))
        assertNull(partirCifra("00:32:10"))
    }

    @Test
    fun escribirRespetaDecimalesYSeparador() {
        val c = assertNotNull(partirCifra("12,40 km"))
        assertEquals("0,00 km", escribirCifra(c, 0.0))
        assertEquals("6,20 km", escribirCifra(c, 6.2))
        assertEquals("12,40 km", escribirCifra(c, 12.4))
        val entero = assertNotNull(partirCifra("+120 m"))
        assertEquals("+0 m", escribirCifra(entero, 0.2))
        assertEquals("+120 m", escribirCifra(entero, 120.0))
    }

    @Test
    fun ultimoCuadroEscribeElTextoOriginal() {
        listOf("12,4 km", "5.42", "+120 m", "75%", "3").forEach { t ->
            val c = assertNotNull(partirCifra(t))
            assertEquals(t, escribirCifra(c, c.valor), "no reproduce $t")
        }
    }

    @Test
    fun entradaListaSoloAnimaLosPrimerosElementos() {
        // Los indices altos no deben animar (listas perezosas): el modificador es la identidad.
        val base = androidx.compose.ui.Modifier
        kotlin.test.assertSame(base, base.entradaLista(MovimientoC2.ESCALONES_MAX + 2))
        kotlin.test.assertSame(base, base.entradaLista(-1))
    }
}
