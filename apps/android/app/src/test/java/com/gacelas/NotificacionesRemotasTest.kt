package com.gacelas

import com.gacelas.data.mock.TipoNotificacion
import com.gacelas.data.notificaciones.NotificacionesRepository
import com.gacelas.data.notificaciones.haceCuanto
import com.gacelas.data.notificaciones.notificacionDesdeFila
import com.gacelas.data.remoto.UsuarioRemoto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificacionesRemotasTest {
    @Test fun convierteFila() {
        val f = Json.parseToJsonElement("""{"id":"n1","type":"streak","actor_name":"Gacelas","message":"Racha de 3 días","read":false,"created_at":"2026-10-11T05:00:00Z"}""").jsonObject
        val n = notificacionDesdeFila(f, com.gacelas.data.remoto.FechaIso.leer("2026-10-11T08:00:00Z")!!)!!
        assertEquals(TipoNotificacion.Racha, n.tipo)
        assertEquals("hace 3h", n.tiempo)
        assertNull(notificacionDesdeFila(JsonObject(mapOf("type" to JsonPrimitive("otro")))))
    }

    @Test fun tiempos() {
        assertEquals("ahora", haceCuanto(1000, 1000))
        assertEquals("hace 5 min", haceCuanto(0, 5 * 60_000))
        assertEquals("hace 2d", haceCuanto(0, 2 * 86_400_000L))
    }

    @Test fun cargaYMarcaLeidas() = runBlocking {
        val b = FakeBackend().apply { actual = UsuarioRemoto("u-1", "a@b.c") }
        b.tablas["notifications"] = mutableListOf(
            Json.parseToJsonElement("""{"id":"n1","user_id":"u-1","type":"live","actor_name":"Ana","message":"está corriendo","read":false,"created_at":"2026-10-11T05:00:00Z"}""").jsonObject,
            Json.parseToJsonElement("""{"id":"n2","user_id":"u-2","type":"live","actor_name":"X","message":"ajena","read":false,"created_at":"2026-10-11T05:00:00Z"}""").jsonObject,
        )
        val r = NotificacionesRepository(b) { "u-1" }
        r.refrescar()
        assertEquals(1, r.lista.value.size)
        r.marcarLeidas(listOf("n1"))
        assertTrue(r.lista.value.first().leida)
        assertEquals("true", (b.tablas["notifications"]!!.first()["read"] as JsonPrimitive).content)
    }
}
