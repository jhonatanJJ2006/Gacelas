package com.gacelas.clima

import com.gacelas.clima.domain.CacheClima
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.clima.domain.Coordenadas
import com.gacelas.clima.domain.FuenteClima
import com.gacelas.clima.infrastructure.ClimaRepositorioOpenMeteo
import com.gacelas.clima.infrastructure.OpenMeteoFuenteClima
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Respuesta real de Open-Meteo (Loja, 1 nov 2026 19:00 hora de Ecuador). */
const val FIXTURE_OPEN_METEO = """{"latitude":-3.9718807,"longitude":-79.22638,"generationtime_ms":227.7677059173584,"utc_offset_seconds":0,"timezone":"GMT","timezone_abbreviation":"GMT","elevation":2074.0,"current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C","weather_code":"wmo code","is_day":""},"current":{"time":"2026-11-02T00:00","interval":900,"temperature_2m":16.1,"weather_code":3,"is_day":0}}"""

private class CacheMemoria(var c: ClimaActual? = null) : CacheClima {
    override suspend fun leer() = c
    override suspend fun guardar(c: ClimaActual) { this.c = c }
}

class ClimaRepositorioTest {
    private val t0 = 1_800_000_000_000L
    private var urls = mutableListOf<String>()
    private fun fuenteOk() = OpenMeteoFuenteClima { urls += it; FIXTURE_OPEN_METEO }

    @Test fun leeLaFixtureReal() {
        val l = OpenMeteoFuenteClima.leerJson(FIXTURE_OPEN_METEO)
        assertEquals(16.1, l.temperatura); assertEquals(3, l.codigoWmo); assertEquals(false, l.esDeDia)
    }

    @Test fun exitoSinPermisoUsaLojaYGuarda() = runTest {
        val cache = CacheMemoria()
        val c = ClimaRepositorioOpenMeteo(fuenteOk(), cache).actual(t0)
        assertEquals(ClimaActual(16, "nublado", "Loja", t0), c)
        assertEquals(c, cache.c)
        assertEquals("https://api.open-meteo.com/v1/forecast?latitude=-3.99&longitude=-79.2&current=temperature_2m,weather_code,is_day", urls.single())
    }

    @Test fun fueraDeLojaUsaNombreDePlataformaYCoordenadasAproximadas() = runTest {
        val repo = ClimaRepositorioOpenMeteo(fuenteOk(), CacheMemoria(), ubicacion = { Coordenadas(-0.180653, -78.467834) }, nombreLugar = { "Quito" })
        assertEquals("Quito", repo.actual(t0)?.lugar)
        assertEquals(true, urls.single().contains("latitude=-0.18&longitude=-78.47"))
        val sinNombre = ClimaRepositorioOpenMeteo(fuenteOk(), CacheMemoria(), ubicacion = { Coordenadas(-2.17, -79.92) }, nombreLugar = { error("sin geocoder") })
        assertEquals("Tu zona", sinNombre.actual(t0)?.lugar)
    }

    @Test fun fallaLaUbicacionUsaLoja() = runTest {
        assertEquals("Loja", ClimaRepositorioOpenMeteo(fuenteOk(), CacheMemoria(), ubicacion = { error("gps") }).actual(t0)?.lugar)
    }

    @Test fun errorDeRedDevuelveLaCache() = runTest {
        val viejo = ClimaActual(12, "lluvia", "Loja", t0 - 3_600_000L)
        val repo = ClimaRepositorioOpenMeteo(FuenteClima { error("sin red") }, CacheMemoria(viejo))
        assertEquals(viejo, repo.actual(t0))
    }

    @Test fun sinCacheNiRedDevuelveNull() = runTest {
        assertNull(ClimaRepositorioOpenMeteo(FuenteClima { error("sin red") }, CacheMemoria()).actual(t0))
    }

    @Test fun timeoutDe5Segundos() = runTest {
        val viejo = ClimaActual(12, "lluvia", "Loja", t0 - 3_600_000L)
        val repo = ClimaRepositorioOpenMeteo(FuenteClima { awaitCancellation() }, CacheMemoria(viejo))
        assertEquals(viejo, repo.actual(t0))
        assertEquals(5_000L, testScheduler.currentTime)
    }

    @Test fun cacheRecienteNoLlamaALaRed() = runTest {
        val reciente = ClimaActual(18, "despejado", "Loja", t0 - 10 * 60_000L)
        val repo = ClimaRepositorioOpenMeteo(fuenteOk(), CacheMemoria(reciente))
        assertEquals(reciente, repo.actual(t0))
        assertEquals(0, urls.size)
        assertEquals(16, repo.actual(t0 + 5 * 60_000L)?.temperatura)
        assertEquals(1, urls.size)
    }

    @Test fun trasUnFalloEspera15MinAntesDeReintentar() = runTest {
        var llamadas = 0
        val repo = ClimaRepositorioOpenMeteo(FuenteClima { llamadas++; error("x") }, CacheMemoria())
        repo.actual(t0); repo.actual(t0 + 60_000L)
        assertEquals(1, llamadas)
        repo.actual(t0 + 15 * 60_000L)
        assertEquals(2, llamadas)
    }
}
