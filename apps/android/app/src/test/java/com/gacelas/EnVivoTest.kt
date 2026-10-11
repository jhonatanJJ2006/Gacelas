package com.gacelas

import com.gacelas.data.envivo.EnVivoRepository
import com.gacelas.data.envivo.sesionDesdeFila
import com.gacelas.data.remoto.UsuarioRemoto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnVivoTest {
    private val fila = Json.parseToJsonElement(
        """{"id":"s1","user_id":"u1","audience":"followers","started_at":"2026-10-11T05:00:00Z","ended_at":null,
           "hidden":false,"last_at":"2026-10-11T05:10:00Z","last_lat":-3.99,"last_lng":-79.2,
           "points":[{"lat":-3.99,"lng":-79.2}],"distance_m":2350,"elapsed_s":600}""",
    ).jsonObject

    @Test fun convierteFila() {
        val s = sesionDesdeFila(fila, "Ana")
        assertEquals(2.35, s.distanciaKm, 1e-9)
        assertEquals(600, s.segundos)
        assertEquals(1, s.puntos.size)
        assertNotNull(s.posicion)
    }

    @Test fun ocultaNoExponePosicion() {
        val s = sesionDesdeFila(Json.parseToJsonElement(fila.toString().replace("\"hidden\":false", "\"hidden\":true")).jsonObject)
        assertNull(s.posicion)
        assertTrue(s.oculta)
    }

    @Test fun inactivaTras15Minutos() {
        val s = sesionDesdeFila(fila)
        val base = com.gacelas.data.remoto.FechaIso.leer("2026-10-11T05:10:00Z")!!
        assertTrue(s.activa(base + 14 * 60_000))
        assertFalse(s.activa(base + 16 * 60_000))
    }

    @Test fun compartirEnviaCada5sYTermina() = runBlocking {
        val b = FakeBackend().apply { actual = UsuarioRemoto("u-1", "a@b.c") }
        var ahora = 0L
        val r = EnVivoRepository(b, { "u-1" }) { ahora }
        val id = r.iniciar()
        assertNotNull(id)
        ahora = 10_000
        assertTrue(r.enviar(-3.99, -79.2, 1.0, 300))
        ahora = 12_000
        assertFalse(r.enviar(-3.99, -79.2, 1.01, 302)) // menos de 5 s
        assertEquals(true, r.visible)
        r.terminar()
        assertNull(r.sesionId)
        assertNotNull(b.tablas["live_sessions"]!!.first()["ended_at"])
    }
}
