package com.gacelas

import com.gacelas.ui.components.MapaMat
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.three.ClipCorredor
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
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

    @Test fun estiloEsJsonValidoYOcultaPois() {
        val j = JSONArray(Mapas.ESTILO_GACELAS)
        assertTrue(j.length() > 10)
        assertTrue((0 until j.length()).any { j.getJSONObject(it).optString("featureType") == "poi" })
    }
}
