package com.gacelas

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.gacelas.acceso.PortadaRuta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccesoPortadaTest {
    @Test fun etiquetaUsaLargoRealDelTrazoDeJipiro() {
        assertEquals(1.8, PortadaRuta.km(), 0.05)
        assertEquals("Jipiro · 1.8 KM", PortadaRuta.etiqueta())
    }

    @Test fun focoRutaCentraLaRutaSinDejarBordes() {
        val f = com.gacelas.acceso.FocoRuta
        val img = IntSize(1080, 1920)
        // Hueco bajo (login con teclado): la ruta (42% de alto) queda centrada.
        val o = f.align(img, IntSize(1080, 400), LayoutDirection.Ltr)
        assertEquals(200.0, (o.y + (1920 * 0.42f).toInt()).toDouble(), 2.0)
        // Nunca deja huecos arriba ni abajo.
        val alto = f.align(img, IntSize(1080, 1900), LayoutDirection.Ltr)
        assertTrue(alto.y <= 0 && alto.y >= 1900 - 1920)
        assertEquals(IntOffset(0, 0).x, f.align(img, IntSize(1080, 400), LayoutDirection.Ltr).x)
    }
}
