package com.gacelas.ui.three

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CicloCorredorTest {
    @Test
    fun noDibujaSinSuperficieNiTamano() {
        val c = CicloCorredor()
        assertFalse(c.puedeDibujar())
        c.conSuperficie()
        assertFalse(c.puedeDibujar())
        assertFalse(c.redimensionar(0, 0))
        assertFalse(c.puedeDibujar())
        assertFalse(c.redimensionar(300, 0))
        assertFalse(c.puedeDibujar())
        assertTrue(c.redimensionar(300, 400))
        assertTrue(c.puedeDibujar())
        c.sinSuperficie()
        assertFalse(c.puedeDibujar())
    }

    @Test
    fun cerrarEsIdempotenteYBloqueaDibujo() {
        val c = CicloCorredor()
        c.conSuperficie(); c.redimensionar(10, 10)
        assertTrue(c.cerrar())
        assertFalse(c.cerrar())
        c.conSuperficie()
        assertFalse(c.puedeDibujar())
    }
}
