package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.data.ajustes.LocalAjustesRepository
import com.gacelas.data.ajustes.Visibilidad
import com.gacelas.data.auth.LocalAuthRepository
import com.gacelas.data.auth.ResultadoAuth
import com.gacelas.data.carreras.CarreraGuardada
import com.gacelas.data.carreras.LocalCarrerasRepository
import com.gacelas.data.eventos.LocalEventosRepository
import com.gacelas.data.mock.Evento
import com.gacelas.data.rutas.LocalRutasRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RepositoriosTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("test.preferences_pb") } }

    @After fun cerrar() = scope.cancel()

    @Test fun auth_registroLoginYCierre() = runBlocking {
        val auth = LocalAuthRepository(store)
        assertTrue(auth.registrar("Jhona", "correo-malo", "gacela123") is ResultadoAuth.Error)
        val ok = auth.registrar("Jhona Runner", "Jhona@UTPL.edu.ec", "gacela123")
        assertTrue(ok is ResultadoAuth.Ok)
        assertEquals("jhona@utpl.edu.ec", auth.sesion.first()?.email)
        assertTrue(auth.registrar("Otro", "jhona@utpl.edu.ec", "gacela123") is ResultadoAuth.Error)
        auth.cerrarSesion()
        assertNull(auth.sesion.first())
        assertTrue(auth.iniciarSesion("jhona@utpl.edu.ec", "incorrecta1") is ResultadoAuth.Error)
        assertTrue(auth.iniciarSesion("JHONA@utpl.edu.ec", "gacela123") is ResultadoAuth.Ok)
        val p = auth.sesion.first()!!
        auth.actualizarPerfil(p.copy(bio = "Corro en Loja"))
        assertEquals("Corro en Loja", auth.sesion.first()?.bio)
        assertTrue(auth.recuperarContrasena("jhona@utpl.edu.ec") is ResultadoAuth.Ok)
        assertTrue(auth.recuperarContrasena("nadie@x.com") is ResultadoAuth.Ok)
        assertTrue(auth.recuperarContrasena("malo") is ResultadoAuth.Error)
    }

    @Test fun carreras_guardarYEliminar() = runBlocking {
        val repo = LocalCarrerasRepository(store)
        repo.guardar(CarreraGuardada(id = "a", titulo = "A", inicioMs = 1, segundos = 60, distanciaKm = 0.2))
        repo.guardar(CarreraGuardada(id = "b", titulo = "B", inicioMs = 2, segundos = 60, distanciaKm = 0.2))
        assertEquals(listOf("b", "a"), repo.carreras.first().map { it.id })
        repo.eliminar("a")
        assertEquals(listOf("b"), repo.carreras.first().map { it.id })
    }

    @Test fun rutas_alternarGuardada() = runBlocking {
        val repo = LocalRutasRepository(store)
        repo.alternarGuardada("1")
        assertEquals(setOf("1"), repo.guardadas.first())
        repo.alternarGuardada("1")
        assertTrue(repo.guardadas.first().isEmpty())
    }

    @Test fun ajustes_persisten() = runBlocking {
        val repo = LocalAjustesRepository(store)
        assertTrue(repo.ajustes.first().modoOscuro)
        repo.actualizar { it.copy(unidadesMetricas = false, visibilidad = Visibilidad.Privado, metronomoBpm = 180) }
        val a = LocalAjustesRepository(store).ajustes.first()
        assertEquals(false, a.unidadesMetricas); assertEquals(Visibilidad.Privado, a.visibilidad); assertEquals(180, a.metronomoBpm)
    }

    @Test fun eventos_crearEInscribir() = runBlocking {
        val repo = LocalEventosRepository(store)
        repo.crear(Evento("u1", "Mi evento", "d", "1 nov", "06:00", "Loja", 1, 5, creadoPorUsuario = true))
        assertEquals(listOf("u1"), repo.creados.first().map { it.id })
        repo.alternarInscripcion("1")
        assertTrue("1" in repo.inscritos.first())
        repo.alternarInscripcion("1")
        assertTrue("1" !in repo.inscritos.first())
    }
}
