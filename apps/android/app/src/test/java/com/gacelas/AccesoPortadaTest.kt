package com.gacelas

import com.gacelas.acceso.PortadaRuta
import org.junit.Assert.assertEquals
import org.junit.Test

class AccesoPortadaTest {
    @Test fun etiquetaUsaLargoRealDelTrazoDeJipiro() {
        assertEquals(1.8, PortadaRuta.km(), 0.05)
        assertEquals("Jipiro · 1.8 KM", PortadaRuta.etiqueta())
    }
}
