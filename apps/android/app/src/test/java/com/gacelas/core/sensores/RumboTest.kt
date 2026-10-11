package com.gacelas.core.sensores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RumboTest {
    @Test fun normalizaYDiferenciaPorCaminoCorto() {
        assertEquals(350f, Rumbo.normalizar(-10f), 0.001f)
        assertEquals(20f, Rumbo.diferencia(350f, 10f), 0.001f)
        assertEquals(-20f, Rumbo.diferencia(10f, 350f), 0.001f)
    }

    @Test fun mezclaCruzandoElNorte() {
        assertEquals(0f, Rumbo.mezclar(350f, 10f, 0.5f), 0.001f)
        assertEquals(350f, Rumbo.mezclar(350f, 10f, 0f), 0.001f)
        assertEquals(10f, Rumbo.mezclar(350f, 10f, 1f), 0.001f)
    }

    @Test fun quietoOLentoMandaElSensor() {
        assertEquals(90f, Rumbo.combinar(90f, 180f, 0.0)!!, 0.001f)
        assertEquals(90f, Rumbo.combinar(90f, 180f, null)!!, 0.001f)
        assertEquals(90f, Rumbo.combinar(90f, 180f, 1.0)!!, 0.001f)
    }

    @Test fun corriendoMandaElGps() {
        assertEquals(180f, Rumbo.combinar(90f, 180f, 3.0)!!, 0.001f)
    }

    @Test fun transicionGradualEntreAmbos() {
        val medio = (Rumbo.VEL_SENSOR_MS + Rumbo.VEL_GPS_MS) / 2
        assertEquals(135f, Rumbo.combinar(90f, 180f, medio)!!, 0.01f)
        var previo = Rumbo.combinar(90f, 180f, 0.0)!!
        var v = 0.0
        while (v <= 3.0) {
            val r = Rumbo.combinar(90f, 180f, v)!!
            assertTrue("salto brusco a $v m/s", r - previo <= 10f)
            previo = r; v += 0.1
        }
    }

    @Test fun sinUnaFuenteSeUsaLaOtra() {
        assertEquals(45f, Rumbo.combinar(null, 45f, 0.0)!!, 0.001f)
        assertEquals(45f, Rumbo.combinar(45f, null, 3.0)!!, 0.001f)
        assertNull(Rumbo.combinar(null, null, 3.0))
    }

    @Test fun ignoraCambiosMinimos() {
        assertTrue(Rumbo.cambioRelevante(null, 10f))
        assertFalse(Rumbo.cambioRelevante(10f, 12f))
        assertTrue(Rumbo.cambioRelevante(358f, 5f))
    }
}
