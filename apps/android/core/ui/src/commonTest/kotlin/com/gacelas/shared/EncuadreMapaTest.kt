package com.gacelas.shared

import com.gacelas.mapa.Coord
import com.gacelas.mapa.EncuadreMapa
import com.gacelas.mapa.PaddingPx
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncuadreMapaTest {
    private val ruta = listOf(
        Coord(-3.9746, -79.2017), Coord(-3.9845, -79.2035), Coord(-3.9931, -79.2005), Coord(-4.0045, -79.1958),
    )
    private val ancho = 1080f
    private val alto = 2000f

    private fun mx(lng: Double) = (lng + 180.0) / 360.0
    private fun my(lat: Double) = 0.5 - ln(tan(PI / 4 + lat * PI / 360.0)) / (2 * PI)

    /** Pixel donde cae un punto con la camara dada (teselas de 512). */
    private fun pantalla(p: Coord, centro: Coord, zoom: Double): Pair<Double, Double> {
        val mundo = 512.0 * 2.0.pow(zoom)
        return ancho / 2 + (mx(p.lng) - mx(centro.lng)) * mundo to alto / 2 + (my(p.lat) - my(centro.lat)) * mundo
    }

    @Test fun sinPuntosNoHayCamara() {
        assertNull(EncuadreMapa.camaraParaPuntos(emptyList(), ancho, alto, PaddingPx.todos(40f)))
        assertNull(EncuadreMapa.camaraParaPuntos(listOf(Coord(Double.NaN, 0.0)), ancho, alto, PaddingPx.todos(40f)))
        assertNull(EncuadreMapa.camaraParaPuntos(ruta, 0f, alto, PaddingPx.todos(40f)))
    }

    @Test fun unSoloPuntoCentraConZoomRazonable() {
        val c = assertNotNull(EncuadreMapa.camaraParaPuntos(listOf(ruta[0]), ancho, alto, PaddingPx.todos(40f)))
        assertEquals(EncuadreMapa.ZOOM_UN_PUNTO, c.zoom)
        assertEquals(ruta[0].lat, c.centro.lat, 1e-6)
        assertEquals(ruta[0].lng, c.centro.lng, 1e-6)
    }

    @Test fun puntosRepetidosCuentanComoUno() {
        val c = assertNotNull(EncuadreMapa.camaraParaPuntos(listOf(ruta[0], ruta[0]), ancho, alto, PaddingPx.todos(40f), zoomUnPunto = 14.0))
        assertEquals(14.0, c.zoom)
    }

    @Test fun todaLaRutaCaeDentroDelAreaLibre() {
        val pad = PaddingPx(arriba = 380f, derecha = 160f, abajo = 700f, izquierda = 80f)
        val c = assertNotNull(EncuadreMapa.camaraParaPuntos(ruta, ancho, alto, pad))
        ruta.forEach { p ->
            val (px, py) = pantalla(p, c.centro, c.zoom)
            assertTrue(px >= pad.izquierda - 0.5 && px <= ancho - pad.derecha + 0.5, "x=$px")
            assertTrue(py >= pad.arriba - 0.5 && py <= alto - pad.abajo + 0.5, "y=$py")
        }
    }

    @Test fun conCarruselGrandeLaRutaSubeYSeAleja() {
        val libre = assertNotNull(EncuadreMapa.camaraParaPuntos(ruta, ancho, alto, PaddingPx.todos(100f)))
        val conCarrusel = assertNotNull(EncuadreMapa.camaraParaPuntos(ruta, ancho, alto, PaddingPx(100f, 100f, 900f, 100f)))
        assertTrue(conCarrusel.zoom <= libre.zoom)
        // La camara mira mas al sur para que la ruta quede arriba, sobre el carrusel.
        assertTrue(conCarrusel.centro.lat < libre.centro.lat)
    }

    @Test fun usaLaFranjaLibreAlMaximo() {
        val pad = PaddingPx.todos(100f)
        val c = assertNotNull(EncuadreMapa.camaraParaPuntos(ruta, ancho, alto, pad))
        val pts = ruta.map { pantalla(it, c.centro, c.zoom) }
        val w = pts.maxOf { it.first } - pts.minOf { it.first }
        val h = pts.maxOf { it.second } - pts.minOf { it.second }
        assertTrue(abs(w - (ancho - 200)) < 1 || abs(h - (alto - 200)) < 1, "w=$w h=$h")
    }

    @Test fun respetaZoomMaximoConRutasDiminutas() {
        val corta = listOf(Coord(-3.99, -79.2), Coord(-3.99003, -79.2))
        assertEquals(16.0, assertNotNull(EncuadreMapa.camaraParaPuntos(corta, ancho, alto, PaddingPx.todos(40f), zoomMaximo = 16.0)).zoom)
    }

    @Test fun panelesQueTapanCasiTodoNoRompenElCalculo() {
        val c = assertNotNull(EncuadreMapa.camaraParaPuntos(ruta, ancho, alto, PaddingPx(1500f, 0f, 1500f, 0f)))
        assertTrue(c.zoom.isFinite() && c.centro.lat.isFinite() && c.centro.lng.isFinite())
    }

    @Test fun paddingEnDpSeEscalaConLaDensidad() {
        val p = EncuadreMapa.paddingDp(2.5f, arriba = 100f, abajo = 200f)
        assertEquals(250f, p.arriba)
        assertEquals(500f, p.abajo)
        assertEquals(100f, p.izquierda)
    }
}
