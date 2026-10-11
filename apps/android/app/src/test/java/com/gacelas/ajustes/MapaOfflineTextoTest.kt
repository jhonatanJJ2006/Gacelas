package com.gacelas.ajustes

import com.gacelas.ui.components.EstadoDescarga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapaOfflineTextoTest {
    @Test fun textosNoRompenElFormato() {
        assertEquals("Descargando 5% (1.3 MB)", textoEstadoMapa(EstadoDescarga.Descargando(5, 1.26)))
        assertEquals("Descargando 100% (12.0 MB)", textoEstadoMapa(EstadoDescarga.Descargando(100, 12.0)))
        assertEquals("Listo para usar sin red (8.5 MB)", textoEstadoMapa(EstadoDescarga.Lista(8.46)))
    }

    /** La descarga usa un estilo http(s) con la misma fuente y tipografia que los estilos Gacelas. */
    @Test fun estiloDeDescargaCompatible() {
        assertTrue(com.gacelas.ui.components.MapaOffline.ESTILO_DESCARGA.startsWith("https://tiles.openfreemap.org/"))
        listOf("gacelas-oscuro.json", "gacelas-claro.json").forEach { nombre ->
            val j = org.json.JSONObject(java.io.File("src/main/assets/mapas/$nombre").readText())
            assertEquals("https://tiles.openfreemap.org/planet", j.getJSONObject("sources").getJSONObject("openmaptiles").getString("url"))
            assertEquals("https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf", j.getString("glyphs"))
        }
    }
}
