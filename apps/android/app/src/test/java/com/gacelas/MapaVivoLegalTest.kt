package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.data.remoto.UsuarioRemoto
import com.gacelas.legal.Consentimiento
import com.gacelas.legal.ConsentimientosRepository
import com.gacelas.legal.TipoConsentimiento
import com.gacelas.legal.VersionesLegales
import com.gacelas.legal.otorgado
import com.gacelas.legal.requiereAceptarTerminos
import com.gacelas.data.remoto.FechaIso
import com.gacelas.vivo.CorredorEnMapa
import com.gacelas.vivo.MapaVivo
import com.gacelas.vivo.MapaVivoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MapaVivoLegalTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("legal.preferences_pb") } }
    @After fun cerrar() = scope.cancel()

    private fun corredor(id: String, hace: Long, ahora: Long) = CorredorEnMapa(id, id, null, -3.99, -79.2, true, ahora - hace)

    @Test fun visibles_ocultaCaducadosYAMiMismo() {
        val ahora = 1_000_000L
        val lista = listOf(corredor("a", 10_000, ahora), corredor("b", 121_000, ahora), corredor("yo", 1_000, ahora))
        assertEquals(listOf("a"), MapaVivo.visibles(lista, ahora, "yo").map { it.userId })
    }

    @Test fun tocaPublicar_respetaIntervalos() {
        assertTrue(MapaVivo.tocaPublicar(null, null, 0, false))
        assertFalse(MapaVivo.tocaPublicar(0, true, 10_000, true))
        assertTrue(MapaVivo.tocaPublicar(0, true, 15_000, true))
        assertFalse(MapaVivo.tocaPublicar(0, false, 30_000, false))
        assertTrue(MapaVivo.tocaPublicar(0, false, 60_000, false))
        assertTrue("cambiar de estado publica al instante", MapaVivo.tocaPublicar(0, false, 1_000, true))
    }

    @Test fun terminos_seReaceptanAlCambiarVersion() {
        assertTrue(requiereAceptarTerminos(emptyList()))
        val vieja = Consentimiento("terminos", "2020-01-01", true, 1)
        assertTrue(requiereAceptarTerminos(listOf(vieja)))
        val actual = Consentimiento("terminos", VersionesLegales.TERMINOS, true, 2)
        assertFalse(requiereAceptarTerminos(listOf(vieja, actual)))
        val revocado = Consentimiento("ubicacion_visible", VersionesLegales.UBICACION, false, 3)
        val otorgadoAntes = Consentimiento("ubicacion_visible", VersionesLegales.UBICACION, true, 1)
        assertFalse(otorgado(listOf(otorgadoAntes, revocado), TipoConsentimiento.UbicacionVisible))
    }

    @Test fun consentimientos_guardanHistorialYServidor() = runBlocking {
        val b = FakeBackend().apply { actual = UsuarioRemoto("u-1", "a@b.c") }
        val repo = ConsentimientosRepository(store, b, { "u-1" })
        assertFalse(repo.otorgado(TipoConsentimiento.UbicacionVisible))
        repo.registrar(TipoConsentimiento.UbicacionVisible, true)
        repo.registrar(TipoConsentimiento.UbicacionVisible, false)
        assertEquals(2, repo.historial().size)
        assertFalse(repo.otorgado(TipoConsentimiento.UbicacionVisible))
        assertEquals(listOf("true", "false"), b.tablas["consents"]!!.map { it["granted"].toString() })
    }

    @Test fun mapa_soloPublicaConConsentimientoYListaVisibles() = runBlocking {
        val b = FakeBackend().apply { actual = UsuarioRemoto("u-1", "a@b.c") }
        val ahora = FechaIso.leer(b.ahoraIso)!!
        var t = ahora
        val consent = ConsentimientosRepository(store, b, { "u-1" })
        val mapa = MapaVivoRepository(b, { "u-1" }, { t })
        mapa.publicar(-3.99, -79.2, true)
        assertTrue(b.tablas["runner_positions"].isNullOrEmpty())
        consent.registrar(TipoConsentimiento.UbicacionVisible, true)
        t += 20_000
        assertTrue(mapa.publicar(-3.99, -79.2, true))
        assertEquals(1, b.tablas["runner_positions"]!!.size)
        val otro = MapaVivoRepository(b, { "u-2" }, { ahora + 30_000 })
        assertEquals(listOf("u-1"), otro.listar().map { it.userId })
        assertTrue("caduca a los 2 min", MapaVivoRepository(b, { "u-2" }, { ahora + 200_000 }).listar().isEmpty())
        mapa.dejarDeCompartir()
        assertTrue(b.tablas["runner_positions"]!!.isEmpty())
    }

    @Test fun eliminarCuenta_borraDatos() = runBlocking {
        val b = FakeBackend().apply { actual = UsuarioRemoto("u-1", "a@b.c") }
        val repo = ConsentimientosRepository(store, b, { "u-1" })
        repo.registrar(TipoConsentimiento.Terminos, true)
        assertTrue(repo.eliminarCuenta())
        assertTrue(repo.historial().isEmpty())
        assertTrue(b.tablas["consents"]!!.isEmpty())
    }
}
