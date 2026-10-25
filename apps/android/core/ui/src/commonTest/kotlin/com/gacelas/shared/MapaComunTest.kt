package com.gacelas.shared

import com.gacelas.mapa.CirculoComun
import com.gacelas.mapa.Coord
import com.gacelas.mapa.EstadoMapaOffline
import com.gacelas.mapa.ReglasTeselas
import com.gacelas.mapa.TrazoComun
import com.gacelas.mapa.geojsonCirculos
import com.gacelas.mapa.geojsonTrazos
import com.gacelas.mapa.textoEstadoMapaOffline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapaComunTest {
    @Test fun plantillaYEstiloFijo() {
        val p = ReglasTeselas.plantillaTeselas("""{"tiles":["https://tiles.openfreemap.org/planet/20261004_113936_pt/{z}/{x}/{y}.pbf"]}""")
        assertEquals("https://tiles.openfreemap.org/planet/20261004_113936_pt/{z}/{x}/{y}.pbf", p)
        val e = ReglasTeselas.fijarTeselas("""{"version":8,"sources":{"openmaptiles":{"type":"vector","url":"https://tiles.openfreemap.org/planet"}},"layers":[]}""", p!!)!!
        assertTrue("20261004_113936_pt" in e && "\"url\"" !in e && "\"maxzoom\":14" in e)
    }

    @Test fun descargaSolaConRed() {
        assertTrue(ReglasTeselas.descargarSola(existe = false, completa = false, wifi = true))
        assertTrue(!ReglasTeselas.descargarSola(existe = true, completa = true, wifi = true))
        assertTrue(!ReglasTeselas.descargarSola(existe = false, completa = false, wifi = false))
    }

    @Test fun textosComoAndroid() {
        assertEquals("Descargando 42% (12.3 MB)", textoEstadoMapaOffline(EstadoMapaOffline.Descargando(42, 12.34)))
        assertEquals("Mapa offline listo (40.0 MB)", textoEstadoMapaOffline(EstadoMapaOffline.Lista(39.98)))
    }

    @Test fun geojson() {
        val t = geojsonTrazos(listOf(TrazoComun(listOf(Coord(-4.0, -79.2), Coord(-4.1, -79.3)), 0xFFC084FC, 5f)))
        assertTrue("\"color\":\"#C084FC\"" in t && "[-79.2,-4.0]" in t)
        val c = geojsonCirculos(listOf(CirculoComun(Coord(-4.0, -79.2), 9f, 0xFFC084FC, id = "r\"1")))
        assertTrue("\"id\":\"r\\\"1\"" in c)
    }
}
