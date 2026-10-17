package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.work.NetworkType
import com.gacelas.core.sync.Sincronizacion
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.reglas.Guia
import com.gacelas.rutas.data.OperacionRuta
import com.gacelas.rutas.data.RutasOffline
import com.gacelas.rutas.rutasNavegables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RutasOfflineTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("rutas.preferences_pb") } }
    private val backend = FakeBackend()
    private var programadas = 0
    private val c by lazy { Contenedor(store, backend) { programadas++ } }

    @After fun cerrar() = scope.cancel()

    private val trazo = listOf(-3.9746 to -79.2017, -3.9712 to -79.2001, -3.969 to -79.203)

    @Test fun lasRutasDelServidorQuedanEnElCelularYSeVenSinRed() = runBlocking {
        backend.tablas["routes"] = mutableListOf(Json.parseToJsonElement("""{"id":"r1","name":"Jipiro","distance_km":2,"difficulty":"Fácil","path":[{"lat":-3.97,"lng":-79.2},{"lat":-3.98,"lng":-79.21}]}""").jsonObject)
        c.explorar!!.refrescar()
        assertEquals(listOf("r1"), c.rutasOffline.rutas.first().map { it.id })
        // Nueva sesion sin red: la lista sale de la copia local.
        backend.sinRed = true
        val otro = Contenedor(store, backend)
        otro.explorar!!.refrescar()
        assertEquals("Jipiro", otro.explorar!!.rutas.value!!.single().nombre)
        assertEquals(2, rutasNavegables(otro.explorar!!.rutas.value, otro.rutasOffline.rutas.first(), emptySet()).first().trazo.size)
    }

    @Test fun crearSinRedQuedaPendienteYSeSubeAlVolverLaRed() = runBlocking {
        assertTrue(c.auth.registrar("Jhona Runner", "jhona@utpl.edu.ec", "gacela123") is com.gacelas.cuenta.domain.modelo.ResultadoAuth.Ok)
        backend.sinRed = true
        val id = c.explorar!!.crear("Mi ruta", "", Dificultad.Facil, emptyList(), trazo, 10, null)
        assertTrue(c.explorar!!.pendiente(id))
        assertEquals(1, programadas)
        assertTrue(c.rutasOffline.rutas.first().any { it.id == id && it.propia })
        assertEquals(1, c.subirPendientes())
        backend.sinRed = false
        assertEquals(0, c.subirPendientes())
        assertFalse(c.explorar!!.pendiente(id))
        assertEquals(id, backend.tablas["routes"]!!.single()["id"].toString().trim('"'))
    }

    @Test fun guardarYQuitarSinRedSeAnulan() = runBlocking {
        val o = RutasOffline(store)
        o.encolar(OperacionRuta("guardar", "r1"))
        o.encolar(OperacionRuta("quitar", "r1"))
        assertEquals(listOf("quitar"), o.pendientes.first().map { it.tipo })
    }

    @Test fun operacionInvalidaSeDescartaTrasVariosIntentos() = runBlocking {
        val o = RutasOffline(store)
        o.encolar(OperacionRuta("corrida", "no-existe"))
        repeat(RutasOffline.INTENTOS_MAX - 1) { assertEquals(1, o.procesar { error("falla") }) }
        assertEquals(0, o.procesar { error("falla") })
    }

    @Test fun elTrabajoEsperaRed() {
        assertEquals(NetworkType.CONNECTED, Sincronizacion.solicitud().workSpec.constraints.requiredNetworkType)
    }

    @Test fun laNavegacionUsaLaRutaElegida() {
        val r = Ruta("r9", "Elegida", 1.0, Dificultad.Facil, 0, "", puntos = trazo)
        val lista = rutasNavegables(listOf(r), emptyList(), emptySet(), elegida = "r9")
        assertEquals("r9", lista.first().id)
        assertEquals(trazo, lista.first().trazo)
        assertTrue(lista.any { it.id == "primary" && !it.real })
    }

    @Test fun guiaDetectaGiroYLlegada() {
        // Norte 220 m y luego este: giro a la derecha al final del primer tramo.
        val l = listOf(-4.0 to -79.2, -3.998 to -79.2, -3.998 to -79.198)
        val inicio = Guia.calcular(l, l[0])!!
        assertEquals("Gira a la derecha", inicio.texto)
        assertEquals(221.0, inicio.hastaGiroM, 3.0)
        val tramo2 = Guia.calcular(l, -3.998 to -79.1995)!!
        assertEquals("Continúa recto hasta la meta", tramo2.texto)
        assertTrue(Guia.calcular(l, l.last())!!.llegada)
        assertTrue(Guia.calcular(l, -3.99 to -79.21)!!.desvioM > Guia.DESVIO_M)
    }
}
