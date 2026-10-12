package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.actualizacion.textoVersion
import com.gacelas.core.location.LocationService
import com.gacelas.core.sensores.Rumbo
import com.gacelas.data.actualizacion.ActualizacionRepository
import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.ui.components.MapaCompacto
import com.gacelas.ui.components.MapaMat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.maplibre.android.geometry.LatLng

class Feedback068Test {
    @get:Rule val tmp = TemporaryFolder()

    private fun json(code: Int) = """{"versionCode":$code,"versionName":"0.6.${code % 100}","apkUrl":"https://x/a.apk"}"""

    @Test fun revisaSiempreYMasTardeSoloEnLaSesion() = runBlocking {
        val store = PreferenceDataStoreFactory.create { tmp.newFile("a.preferences_pb") }
        var servidor = 607; var pedidos = 0
        val repo = ActualizacionRepository(store, "https://x/latest.json", 607, descargar = { pedidos++; json(servidor) })
        assertTrue(repo.revisar() is Aviso.Nada)
        servidor = 608
        assertTrue("sin cache diaria", repo.revisar() is Aviso.Nueva)
        repo.posponer(608)
        assertTrue(repo.revisar() is Aviso.Nada)
        assertEquals(3, pedidos)
        // Nueva sesion (otro repositorio, mismo almacen): vuelve a avisar.
        val otra = ActualizacionRepository(store, "https://x/latest.json", 607, descargar = { json(608) })
        assertTrue(otra.revisar() is Aviso.Nueva)
    }

    @Test fun urlSinCache() {
        assertEquals("https://x/latest.json?t=5", ReglasActualizacion.sinCache("https://x/latest.json", 5))
        assertEquals("https://x/l?a=1&t=5", ReglasActualizacion.sinCache("https://x/l?a=1", 5))
    }

    @Test fun versionVisible() = assertEquals("Versión 0.6.9 (609)", textoVersion("0.6.9", 609))

    @Test fun ubicacionCadaSegundo() {
        assertEquals(1_000L, LocationService.INTERVALO_MS)
        assertEquals(1_000, MapaCompacto.ANIMACION_PUNTO_MS)
        assertEquals(8_000L, MapaCompacto.SEGUIR_DE_NUEVO_MS)
    }

    @Test fun trotandoMandaElCursoGpsNoLaBrujula() {
        // 9 min/km = 1,85 m/s: el rumbo es el del GPS (oeste) aunque el telefono apunte al norte.
        val v = 1000.0 / (9 * 60)
        assertEquals(270f, Rumbo.combinar(sensor = 0f, gps = 270f, velocidadMs = v)!!, 0.01f)
        assertEquals(0f, Rumbo.combinar(sensor = 0f, gps = 270f, velocidadMs = 0.0)!!, 0.01f)
    }

    @Test fun rumboHaciaElOesteEnIsidroAyora() {
        // Dos puntos de Av. Isidro Ayora (Loja) hacia el oeste.
        val r = MapaMat.rumbo(LatLng(-3.9930, -79.2040), LatLng(-3.9932, -79.2060))
        assertTrue("r=$r", r in 260f..280f)
        assertEquals(-180f + 90f, Rumbo.relativo(270f, 0f), 0.01f) // camara al norte: el corredor gira 90° a la izquierda
        assertEquals(0f, Rumbo.relativo(270f, 270f), 0.01f)
        assertEquals(0f, Rumbo.relativo(null, 123f), 0.01f)
    }
}
