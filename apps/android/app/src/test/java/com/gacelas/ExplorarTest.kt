package com.gacelas

import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.data.mock.MockData
import com.gacelas.rutas.infrastructure.SupabaseExplorarRepository
import com.gacelas.rutas.domain.modelo.FiltroDistancia
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.reglas.aplicarFiltros
import com.gacelas.rutas.domain.reglas.ordenarPorCercania
import com.gacelas.rutas.infrastructure.rutaDesdeFila
import com.gacelas.rutas.domain.reglas.simplificar
import com.gacelas.rutas.domain.reglas.trazoParaRuta
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorarTest {
    private val recta = (0 until 50).map { -4 + it * 0.0001 to -79.2 }

    @Test fun douglasPeucker() {
        assertEquals(2, simplificar(recta).size)
        val ele = recta + (1..20).map { recta.last().first to -79.2 + it * 0.0001 }
        assertEquals(listOf(recta.first(), recta.last(), ele.last()), simplificar(ele))
        val zigzag = (0 until 3000).map { -4 + it * 0.00005 to -79.2 + (it % 2) * 0.001 }
        assertTrue(trazoParaRuta(zigzag).size <= 500)
    }

    @Test fun filtros() {
        assertTrue(aplicarFiltros(MockData.rutas, FiltrosRuta(dificultad = Dificultad.Facil)).all { it.dificultad == Dificultad.Facil })
        assertTrue(aplicarFiltros(MockData.rutas, FiltrosRuta(distancia = FiltroDistancia.Mas10)).all { it.distanciaKm > 10 })
        assertEquals(listOf("Parque Jipiro"), aplicarFiltros(MockData.rutas, FiltrosRuta(etiqueta = "parque")).map { it.nombre })
        assertEquals(listOf("3", "1"), ordenarPorCercania(MockData.rutas, mapOf("3" to 1.0, "1" to 4.0)).take(2).map { it.id })
    }

    @Test fun convierteFila() {
        val f = Json.parseToJsonElement("""{"id":"r1","name":"Mi ruta","distance_km":"3.40","difficulty":"Difícil","elevation_m":80,"author_id":"u-1","tags":["rio"],"path":[{"lat":-4.0,"lng":-79.2}],"likes_count":2,"rating_avg":"4.50","rating_count":2}""").jsonObject
        val r = rutaDesdeFila(f, "u-1")
        assertEquals(3.4, r.distanciaKm, 1e-9)
        assertEquals(Dificultad.Dificil, r.dificultad)
        assertEquals(4.5, r.valoracion!!, 1e-9)
        assertTrue(r.propia)
        assertEquals(listOf(-4.0 to -79.2), r.puntos)
    }

    @Test fun meGustaYResenas() = runBlocking {
        val b = FakeBackend().apply { actual = com.gacelas.data.remoto.UsuarioRemoto("u-1", "a@b.c") }
        b.tablas["routes"] = mutableListOf(Json.parseToJsonElement("""{"id":"r1","name":"Ruta","distance_km":1,"difficulty":"Fácil","likes_count":0}""").jsonObject)
        val r = SupabaseExplorarRepository(b) { "u-1" }
        r.refrescar()
        assertEquals(1, r.rutas.value!!.size)
        r.alternarMeGusta("r1")
        assertTrue("r1" in r.meGusta.value)
        assertEquals(1, b.tablas["route_likes"]!!.size)
        r.alternarMeGusta("r1")
        assertTrue(b.tablas["route_likes"].isNullOrEmpty())
        r.resenar("r1", 5, " genial ")
        assertEquals("genial", (b.tablas["route_reviews"]!!.single()["comment"] as JsonPrimitive).content)
    }
}
