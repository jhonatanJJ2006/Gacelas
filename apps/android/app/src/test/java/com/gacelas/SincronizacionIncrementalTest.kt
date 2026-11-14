package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.data.remoto.fila
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import com.gacelas.identidad.infrastructure.SupabaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Arranque: la segunda sync no vuelve a bajar el historial; si, lo nuevo y los borrados. */
class SincronizacionIncrementalTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("sync.preferences_pb") } }
    private val backend = FakeBackend()
    private val c by lazy { Contenedor(store, backend) }

    @After fun cerrar() = scope.cancel()

    private fun carrera(id: String, inicio: Long) = Carrera(
        id = id, titulo = "Carrera $id", inicioMs = inicio, segundos = 600, distanciaKm = 5.0,
        puntos = listOf(PuntoGps(-3.99, -79.2, inicio)),
    )

    private suspend fun entrar() = assertTrue(c.auth.registrar("Ana Runner", "ana@test.ec", "gacela123") is ResultadoAuth.Ok)

    @Test fun arranqueQuietoNoReDescargaElHistorial() = runBlocking {
        backend.ahoraIso = "2020-01-01T00:00:00Z"
        entrar()
        c.carreras.guardar(carrera("a", 1_000))
        c.carreras.guardar(carrera("b", 2_000))
        c.carreras.guardar(carrera("c", 3_000))
        assertEquals(3, backend.tablas["activities"]!!.size)
        assertEquals(3, backend.tablas["activity_tracks"]!!.size)

        // Pasa el margen de 5 minutos: lo ya guardado queda detras de la marca.
        backend.ahoraIso = "2020-01-01T00:10:00Z"
        assertTrue(c.sincronizarDatos())
        backend.reiniciarLecturas()
        assertTrue(c.sincronizarDatos())

        assertEquals(0, backend.filasLeidas["activities"] ?: 0)
        assertEquals(0, backend.filasLeidas["activity_tracks"] ?: 0)
        assertTrue(backend.consultas.filter { it.tabla == "activities" }.all { it.marcaExclusiva != null })
        val quietas = c.carreras.carreras.first()
        assertEquals(listOf("c", "b", "a"), quietas.map { it.id })
        assertEquals(1, quietas.first { it.id == "a" }.puntos.size)

        backend.ahoraIso = "2020-01-01T02:00:00Z"
        val uid = (c.auth as SupabaseAuthRepository).userId()!!
        backend.insert("activities", listOf(fila(
            "user_id" to uid, "client_id" to "nueva", "title" to "Nueva",
            "start_time" to "2020-01-01T02:00:00Z", "duration_seconds" to 100, "distance_meters" to 1000,
        )))
        backend.reiniciarLecturas()
        assertTrue(c.sincronizarDatos())
        assertEquals(1, backend.filasLeidas["activities"])
        assertEquals(0, backend.filasLeidas["activity_tracks"] ?: 0)
        assertEquals(4, c.carreras.carreras.first().size)

        backend.ahoraIso = "2020-01-01T03:00:00Z"
        backend.delete("activities", mapOf("client_id" to "nueva"))
        assertTrue(c.sincronizarDatos())
        val trasBorrar = c.carreras.carreras.first()
        assertEquals(listOf("c", "b", "a"), trasBorrar.map { it.id })
        assertEquals(1, trasBorrar.first { it.id == "a" }.puntos.size)
    }

    @Test fun cerrarSesionOtraCuentaYRecuperacionBajanTodo() = runBlocking {
        backend.ahoraIso = "2020-01-01T00:00:00Z"
        entrar()
        c.carreras.guardar(carrera("a", 1_000))
        backend.ahoraIso = "2020-01-01T00:10:00Z"
        assertTrue(c.sincronizarDatos())

        c.auth.cerrarSesion()
        backend.reiniciarLecturas()
        assertTrue(c.auth.iniciarSesion("ana@test.ec", "gacela123") is ResultadoAuth.Ok)
        assertTrue(backend.consultas.any { it.tabla == "activities" && it.marcaExclusiva == null })
        assertEquals(listOf("a"), c.carreras.carreras.first().map { it.id })
        assertTrue((backend.filasLeidas["activities"] ?: 0) >= 1)

        backend.reiniciarLecturas()
        assertTrue(c.recuperarDatos())
        assertTrue(backend.consultas.any { it.tabla == "activities" && it.marcaExclusiva == null })
        assertEquals(listOf("a"), c.carreras.carreras.first().map { it.id })

        c.auth.cerrarSesion()
        assertTrue(c.auth.registrar("Otra Persona", "otra@test.ec", "gacela123") is ResultadoAuth.Ok)
        assertTrue(c.carreras.carreras.first().none { it.id == "a" })
        assertEquals("otra@test.ec", c.auth.sesion.first()?.email)
    }
}
