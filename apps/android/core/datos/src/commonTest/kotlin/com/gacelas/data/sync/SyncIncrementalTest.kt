package com.gacelas.data.sync

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.data.remoto.fila
import com.gacelas.data.remoto.texto
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncIncrementalTest {
    private fun carrera(id: String, puntos: List<PuntoGps> = emptyList(), inicio: Long = 1_000) =
        Carrera(id = id, titulo = id, inicioMs = inicio, segundos = 10, distanciaKm = 1.0, puntos = puntos)

    private fun punto(n: Int) = PuntoGps(0.0, 0.0, n.toLong())

    @Test fun modoSegunMarcaUsuarioYRecuperacion() {
        assertEquals(ModoDescarga.COMPLETA, modoDescarga(null, "a"))
        assertEquals(ModoDescarga.COMPLETA, modoDescarga(MarcaSync("a", "2020-01-01T00:00:00Z"), "b"))
        assertEquals(ModoDescarga.COMPLETA, modoDescarga(MarcaSync("a", "2020-01-01T00:00:00Z"), "a", forzarCompleta = true))
        assertEquals(ModoDescarga.INCREMENTAL, modoDescarga(MarcaSync("a", "2020-01-01T00:00:00Z"), "a"))
    }

    @Test fun marcaRestaElMargenAlRelojDelServidor() {
        assertEquals("2020-01-01T00:55:00Z", marcaSiguiente("2020-01-01T01:00:00Z"))
        assertEquals("2020-01-01T00:55:00Z", marcaSiguiente("2020-01-01 01:00:00+00"))
        assertNull(marcaSiguiente(null))
        assertNull(marcaSiguiente("no-es-fecha"))
    }

    @Test fun empateDeMarcaLoDesempataLaClave() {
        assertTrue(esPosterior("2020-01-02T00:00:00Z", "a", "2020-01-01T00:00:00Z", null))
        assertFalse(esPosterior("2020-01-01T00:00:00Z", "a", "2020-01-01T00:00:00Z", null))
        assertFalse(esPosterior("2020-01-01T00:00:00Z", "a", "2020-01-01T00:00:00Z", "a"))
        assertTrue(esPosterior("2020-01-01T00:00:00Z", "b", "2020-01-01T00:00:00Z", "a"))
        assertTrue(esPosterior("2020-01-01T00:00:00.9Z", "2", "2020-01-01T00:00:00Z", "1"))
        assertTrue(comparaClave("10", "9") > 0)
        assertTrue(comparaClave("b", "a") > 0)
    }

    @Test fun paginacionCubreEmpatesYNoRepite() = runBlocking {
        val filas = listOf(
            fila("updated_at" to "2020-01-01T00:00:00Z", "id" to "a"),
            fila("updated_at" to "2020-01-01T00:00:00Z", "id" to "b"),
            fila("updated_at" to "2020-01-01T00:00:00Z", "id" to "c"),
            fila("updated_at" to "2020-01-02T00:00:00Z", "id" to "d"),
            fila("updated_at" to "2020-01-03T00:00:00Z", "id" to "e"),
        )
        val consultas = mutableListOf<ConsultaPagina>()
        val todas = bajarTodo({ q ->
            consultas += q
            filas.filter { esPosterior(it.texto("updated_at"), it.texto("id"), q.marcaExclusiva, q.claveExclusiva) }
                .sortedWith { x, y ->
                    val c = comparaMarca(x.texto("updated_at").orEmpty(), y.texto("updated_at").orEmpty())
                    if (c != 0) c else comparaClave(x.texto("id").orEmpty(), y.texto("id").orEmpty())
                }
                .take(q.limite)
        }, ConsultaPagina("activities", marcaExclusiva = "2019-01-01T00:00:00Z", limite = 2))
        assertEquals(listOf("a", "b", "c", "d", "e"), todas.map { it.texto("id") })
        assertEquals(5, todas.map { it.texto("id") }.distinct().size)
        assertEquals(3, consultas.size)
        assertEquals("b", consultas[1].claveExclusiva)
    }

    @Test fun clavesIncrementalesSonIdempotentesYElBorradoGanaElEmpate() {
        val local = setOf("vieja", "queda")
        val altas = listOf(CambioClave("nueva", 20), CambioClave("queda", 10), CambioClave("rehecha", 30))
        val bajas = listOf(CambioClave("vieja", 15), CambioClave("rehecha", 30))
        val una = fusionarClaves(local, altas, bajas, completa = false)
        assertEquals(setOf("queda", "nueva"), una)
        assertEquals(una, fusionarClaves(una, altas, bajas, completa = false))
        assertEquals(setOf("nueva", "queda", "rehecha"), fusionarClaves(local, altas, emptyList(), completa = true))
    }

    @Test fun filasIncrementalesQuitanLoBorradoYDejanLoQueNoCambio() {
        val local = listOf("a" to 1, "b" to 1)
        val cambios = listOf(("b" to 2) to 20L, ("c" to 1) to 20L)
        val bajas = listOf(CambioClave("a", 15), CambioClave("c", 25))
        val r = fusionarFilas(local, { it.first }, cambios, bajas, completa = false)
        assertEquals(listOf("b" to 2), r)
        assertEquals(listOf("b" to 2, "c" to 1), fusionarFilas(local, { it.first }, cambios, emptyList(), completa = true))
    }

    @Test fun carrerasQuietasConservanPuntosYLosPendientesGanan() {
        val local = listOf(carrera("vieja", listOf(punto(1))), carrera("cambia", listOf(punto(2))))
        val cambios = listOf(CambioCarrera(carrera("cambia", emptyList(), inicio = 2_000), marcaMs = 50, traePuntos = false))
        val sueltos = mapOf("vieja" to listOf(punto(9)))
        val r = fusionarCarreras(local, cambios, sueltos, emptyList(), completa = false, emptyList(), emptySet())
        assertEquals(listOf(punto(9)), r.first { it.id == "vieja" }.puntos)
        assertEquals(listOf(punto(2)), r.first { it.id == "cambia" }.puntos)
        val conPendiente = fusionarCarreras(
            local, cambios, emptyMap(), listOf(CambioClave("vieja", 40)),
            completa = false,
            pendientesGuardar = listOf(carrera("local"), carrera("vieja", listOf(punto(7)))),
            pendientesBorrar = setOf("cambia"),
        )
        assertEquals(setOf("vieja", "local"), conPendiente.map { it.id }.toSet())
        assertEquals(listOf(punto(7)), conPendiente.first { it.id == "vieja" }.puntos)
    }

    @Test fun borradoDeCarreraGanaSiEsMasNuevoYLaCompletaReemplaza() {
        val local = listOf(carrera("a", listOf(punto(1))), carrera("b"))
        val cambios = listOf(CambioCarrera(carrera("a", listOf(punto(3))), marcaMs = 10, traePuntos = true))
        val borrada = fusionarCarreras(local, cambios, emptyMap(), listOf(CambioClave("a", 10)), completa = false, emptyList(), emptySet())
        assertEquals(listOf("b"), borrada.map { it.id })
        val recreada = fusionarCarreras(local, cambios, emptyMap(), listOf(CambioClave("a", 5)), completa = false, emptyList(), emptySet())
        assertEquals(listOf(punto(3)), recreada.first { it.id == "a" }.puntos)
        val completa = fusionarCarreras(local, listOf(CambioCarrera(carrera("z"), 1, true)), emptyMap(), emptyList(), completa = true, emptyList(), emptySet())
        assertEquals(listOf("z"), completa.map { it.id })
    }

    @Test fun borradoDesdeFilaIgnoraLoIncompleto() {
        val ok = borradoDesdeFila(fila("tabla" to "activities", "clave" to "c1", "deleted_at" to "2020-01-01T00:00:00Z"))
        assertEquals(BorradoSync("activities", "c1", com.gacelas.data.remoto.FechaIso.leer("2020-01-01T00:00:00Z")!!), ok)
        assertNull(borradoDesdeFila(fila("tabla" to "activities")))
        assertEquals(listOf(CambioClave("c1", ok!!.marcaMs)), borradosDe(listOf(ok, BorradoSync("events", "e", 1)), "activities"))
    }
}
