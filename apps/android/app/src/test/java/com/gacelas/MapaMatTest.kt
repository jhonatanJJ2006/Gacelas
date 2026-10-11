package com.gacelas

import com.gacelas.ui.components.MapaMat
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.three.ClipCorredor
import org.maplibre.android.geometry.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapaMatTest {
    @Test fun rumbos() {
        assertEquals(0f, MapaMat.rumbo(LatLng(0.0, 0.0), LatLng(1.0, 0.0)), 0.01f)
        assertEquals(90f, MapaMat.rumbo(LatLng(0.0, 0.0), LatLng(0.0, 1.0)), 0.01f)
        assertNull(MapaMat.rumboReciente(listOf(LatLng(0.0, 0.0))))
    }

    @Test fun desplazaMetros() {
        val a = LatLng(-3.99, -79.2)
        val b = MapaMat.desplazar(a, 90.0, 100.0)
        assertEquals(100.0, MapaMat.distanciaM(a, b), 0.5)
        assertEquals(90f, MapaMat.rumbo(a, b), 0.5f)
    }

    @Test fun clipSegunRitmo() {
        assertEquals(ClipCorredor.Idle, MapaMat.clipPorRitmo(null))
        assertEquals(ClipCorredor.Jog, MapaMat.clipPorRitmo(420))
        assertEquals(ClipCorredor.Run, MapaMat.clipPorRitmo(280))
    }

    @Test fun estilosGacelasSonJsonValidosConTeselasOsm() {
        listOf("gacelas-oscuro.json", "gacelas-claro.json").forEach { nombre ->
            val j = org.json.JSONObject(java.io.File("src/main/assets/mapas/$nombre").readText())
            assertEquals(8, j.getInt("version"))
            assertTrue(j.getJSONObject("sources").getJSONObject("openmaptiles").getString("url").startsWith("https://tiles.openfreemap.org/"))
            val capas = j.getJSONArray("layers")
            assertTrue(capas.length() > 10)
            // Sin capa de POIs: el estilo Gacelas los oculta.
            assertTrue((0 until capas.length()).none { capas.getJSONObject(it).optString("source-layer") == "poi" })
        }
        assertTrue(Mapas.ESTILO_GACELAS.startsWith("asset://"))
    }

    @Test fun zoom3dHaceNotarElAvanceCaminando() {
        // A 5-8 km/h el suelo debe moverse al menos 12 dp por segundo (unos 25 dp por punto GPS).
        listOf(5.0, 6.5, 8.0).forEach { kmh ->
            val dpS = com.gacelas.ui.components.ZoomNavegacion.avanceDpPorSegundo(kmh / 3.6)
            assertTrue("a $kmh km/h solo avanza $dpS dp/s", dpS >= 12.0)
        }
        // Corriendo se aleja un poco la camara, sin saltos.
        val lento = com.gacelas.ui.components.ZoomNavegacion.para(1.0)
        val rapido = com.gacelas.ui.components.ZoomNavegacion.para(5.0)
        assertTrue(lento > rapido)
        assertEquals(com.gacelas.ui.components.ZoomNavegacion.ZOOM_LENTO, lento, 1e-9)
    }

    @Test fun regionLojaCubreLaCiudad() {
        val r = com.gacelas.ui.components.MapaOffline.LOJA
        assertTrue(r.contiene(Mapas.CENTRO_LOJA.latitude, Mapas.CENTRO_LOJA.longitude))
        Mapas.trazos.values.flatten().forEach { assertTrue(r.contiene(it.latitude, it.longitude)) }
        assertTrue(r.zoomMax >= 14.0)
    }
}
