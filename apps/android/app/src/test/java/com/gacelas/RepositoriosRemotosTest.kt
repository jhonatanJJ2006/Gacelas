package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.acceso.GoogleAcceso
import com.gacelas.identidad.domain.modelo.Visibilidad
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.actividad.infrastructure.carreraDesdeFila
import com.gacelas.comunidad.infrastructure.eventoDesdeFila
import com.gacelas.data.remoto.fila
import com.gacelas.actividad.infrastructure.filaDesdeCarrera
import com.gacelas.data.remoto.traducirError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RepositoriosRemotosTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("remoto.preferences_pb") } }
    private val backend = FakeBackend()
    private val c by lazy { Contenedor(store, backend) }

    @After fun cerrar() = scope.cancel()

    private fun carrera(id: String, inicio: Long = 1_700_000_000_000) = Carrera(
        id = id, titulo = "Carrera $id", nota = "suave", inicioMs = inicio, segundos = 1500, distanciaKm = 5.25, desnivelM = 40,
        parciales = listOf(Parcial(1, 1.0, 300)), puntos = listOf(PuntoGps(-3.99, -79.2, inicio)),
    )

    private suspend fun registrar() = assertTrue(c.auth.registrar("Jhona Runner", "Jhona@UTPL.edu.ec", "gacela123") is ResultadoAuth.Ok)

    @Test fun auth_registroSesionYCierre() = runBlocking {
        registrar()
        assertEquals("Jhona Runner", c.auth.sesion.first()?.nombre)
        assertEquals("Ya existe una cuenta con ese correo", (c.auth.registrar("Otro", "jhona@utpl.edu.ec", "gacela123") as ResultadoAuth.Error).mensaje)
        c.auth.cerrarSesion()
        assertNull(c.auth.sesion.first())
        assertEquals("Correo o contraseña incorrectos", (c.auth.iniciarSesion("jhona@utpl.edu.ec", "otra12345") as ResultadoAuth.Error).mensaje)
        assertTrue(c.auth.iniciarSesion("jhona@utpl.edu.ec", "gacela123") is ResultadoAuth.Ok)
        val p = c.auth.sesion.first()!!
        c.auth.actualizarPerfil(p.copy(bio = "Fondista"))
        assertEquals("Fondista", backend.tablas["profiles"]!!.first().let { (it["bio"] as kotlinx.serialization.json.JsonPrimitive).content })
    }

    @Test fun auth_confirmacionRecuperarYGoogle() = runBlocking {
        backend.confirmarCorreo = true
        val r = c.auth.registrar("Ana", "ana@test.ec", "gacela123")
        assertTrue((r as ResultadoAuth.Error).mensaje.contains("confirmar"))
        assertTrue(c.auth.recuperarContrasena("ana@test.ec") is ResultadoAuth.Ok)
        assertTrue(c.auth.recuperarContrasena("malo") is ResultadoAuth.Error)
        assertTrue(c.auth.googleDisponible)
        assertTrue(c.auth.entrarConGoogle("token", "nonce") is ResultadoAuth.Ok)
        assertEquals("token" to "nonce", backend.tokensGoogle.single())
        c.auth.cerrarSesion()
        backend.googleActivo = false
        assertTrue((c.auth.entrarConGoogle("t", null) as ResultadoAuth.Error).mensaje.contains("no está activado"))
    }

    @Test fun carreras_subenYSeDescargan() = runBlocking {
        registrar()
        c.carreras.guardar(carrera("a"))
        assertEquals(1, backend.tablas["activities"]!!.size)
        assertEquals(1, backend.tablas["activity_tracks"]!!.size)
        // Otro telefono: cache vacia, se descarga del servidor al restaurar la sesion.
        val otro = Contenedor(PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("otro.preferences_pb") }, backend)
        assertTrue(otro.auth.iniciarSesion("jhona@utpl.edu.ec", "gacela123") is ResultadoAuth.Ok)
        val lista = otro.carreras.carreras.first()
        assertEquals("a", lista.single().id)
        assertEquals(5.25, lista.single().distanciaKm, 0.001)
        assertEquals(1, lista.single().puntos.size)
        otro.carreras.eliminar("a")
        assertTrue(backend.tablas["activities"]!!.isEmpty())
    }

    @Test fun carreras_sinRedSeEncolan() = runBlocking {
        registrar()
        backend.sinRed = true
        c.carreras.guardar(carrera("b"))
        assertEquals(1, c.carreras.carreras.first().size)
        assertTrue(backend.tablas["activities"].isNullOrEmpty())
        backend.sinRed = false
        c.sincronizar()
        assertEquals(1, backend.tablas["activities"]!!.size)
        assertEquals(listOf("b"), c.carreras.carreras.first().map { it.id })
    }

    @Test fun rutasAjustesYEventos() = runBlocking {
        c.rutas.alternarGuardada("9") // sin sesion: solo local
        registrar()
        c.rutas.alternarGuardada("1")
        assertEquals(1, backend.tablas["saved_routes"]!!.size)
        c.ajustes.actualizar { it.copy(visibilidad = Visibilidad.Publico, radioHogarM = 500, likes = setOf("1", "2")) }
        c.ajustes.actualizar { it.copy(likes = setOf("2"), comentarios = mapOf("2" to listOf("Bien"))) }
        assertEquals(1, backend.tablas["post_likes"]!!.size)
        assertEquals(1, backend.tablas["post_comments"]!!.size)
        c.eventos.crear(Evento("ev1", "Fondo", "", "Sábado", "6:00 AM", "Jipiro", 0, 10, creadoPorUsuario = true))
        c.eventos.alternarInscripcion("1")
        c.eventos.alternarInscripcion("ev1")
        val otro = Contenedor(PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("otro2.preferences_pb") }, backend)
        otro.auth.iniciarSesion("jhona@utpl.edu.ec", "gacela123")
        assertEquals(setOf("1"), otro.rutas.guardadas.first())
        val a = otro.ajustes.ajustes.first()
        assertEquals(Visibilidad.Publico, a.visibilidad)
        assertEquals(500, a.radioHogarM)
        assertEquals(setOf("2"), a.likes)
        assertEquals(mapOf("2" to listOf("Bien")), a.comentarios)
        assertEquals("ev1", otro.eventos.creados.first().single().id)
        assertEquals(setOf("1"), otro.eventos.inscritos.first())
    }

    @Test fun utilidades() {
        assertTrue(traducirError("Invalid login credentials").startsWith("Correo"))
        assertTrue(traducirError("Unable to resolve host").startsWith("Sin conexión"))
        assertTrue(traducirError("provider is not enabled").contains("Google"))
        val f = filaDesdeCarrera(carrera("x"), "u1")
        val vuelta = carreraDesdeFila(fila(*f.entries.map { it.key to it.value }.toTypedArray(), "start_time" to "2023-11-14T22:13:20Z"), emptyList())
        assertEquals("x", vuelta.id)
        assertEquals(1_700_000_000_000, vuelta.inicioMs)
        assertEquals(1, vuelta.parciales.size)
        assertTrue(eventoDesdeFila(fila("id" to "9", "title" to "t", "starts_at" to "2000-01-01T00:00:00Z")).pasado)
        assertEquals(1_700_000_000_000, com.gacelas.data.remoto.FechaIso.leer("2023-11-14 22:13:20.123456+00"))
        assertEquals("2023-11-14T22:13:20Z", com.gacelas.data.remoto.FechaIso.formatear(1_700_000_000_000))
        assertNull(com.gacelas.data.remoto.FechaIso.leer("no"))
        assertFalse(GoogleAcceso.configurado(""))
        assertEquals(64, GoogleAcceso.sha256("x").length)
    }
}
