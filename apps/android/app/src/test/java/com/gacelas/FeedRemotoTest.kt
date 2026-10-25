package com.gacelas

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.comunidad.data.comentarioDesdeFila
import com.gacelas.comunidad.data.publicacionDesdeFilaFeed
import com.gacelas.comunidad.domain.modelo.FiltroFeed
import com.gacelas.comunidad.domain.reglas.filtrarFeed
import com.gacelas.comunidad.domain.reglas.unirEventos
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.presentation.publicacionesFeed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feed conectado a Supabase: mapeo de la RPC feed_comunidad y union con carreras locales. */
class FeedRemotoTest {
    private fun fila(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject
    private val ahora = 1_760_961_600_000L // 2025-10-20T12:00:00Z

    private val otra = fila(
        """{"id":"11111111-1111-1111-1111-111111111111","client_id":"x1","user_id":"u2","autor":"Ana","title":"Fondo Jipiro",
        "start_time":"2025-10-20T11:00:00Z","distance_meters":8400,"duration_seconds":2520,"likes_count":3,"comments_count":2,
        "public_points":[{"lat":-4.0,"lng":-79.2,"tiempoMs":0},{"lat":-4.01,"lng":-79.21,"tiempoMs":1000}],
        "me_gusta":true,"sigo":true,"propia":false}""",
    )

    @Test fun mapeaFilaDelFeed() {
        val p = publicacionDesdeFilaFeed(otra, ahora = ahora)
        assertEquals("11111111-1111-1111-1111-111111111111", p.id)
        assertEquals("Ana", p.autor)
        assertEquals("u2", p.autorId)
        assertEquals(8.4, p.distanciaKm, 0.001)
        assertEquals(300, p.ritmoSegKm)
        assertEquals(3, p.likesBase)
        assertEquals(2, p.comentariosBase)
        assertEquals(2, p.puntos.size)
        assertTrue(p.remota && p.meGusta)
        assertEquals("hace 1h", p.cuando)
        val propia = publicacionDesdeFilaFeed(fila("""{"id":"a","user_id":"yo","autor":"X","title":"","propia":true,"public_points":null}"""), "Jhona", ahora)
        assertEquals("Jhona", propia.autor)
        assertEquals("Carrera", propia.titulo)
        assertNull(propia.autorId)
        assertEquals("Luis", comentarioDesdeFila(fila("""{"id":"c","user_id":"u3","autor":"Luis","body":"Bien"}""")).autor)
    }

    @Test fun camposSocialesDelFeed() {
        val f = fila(
            """{"id":"a","user_id":"u2","autor":"Ana","title":"T","elevation_gain":31,"images":["post-images/u2/a/1.jpg"],
            "reposts_count":2,"reposteada":true,"autor_km":42.5,"ultimo_comentario":{"autor":"Luis","texto":"Bien"}}""",
        )
        val p = publicacionDesdeFilaFeed(f, ahora = ahora, url = { "https://x/$it" })
        assertEquals(31, p.desnivelM)
        assertEquals(listOf("https://x/post-images/u2/a/1.jpg"), p.imagenes)
        assertEquals(2, p.compartidos)
        assertTrue(p.compartida)
        assertEquals("Luis" to "Bien", p.ultimoComentario)
        assertEquals("Gacela Intermedia", com.gacelas.comunidad.domain.modelo.nivelCorredor(p.autorKm ?: 0.0))
        assertEquals("Gacela Principiante", com.gacelas.comunidad.domain.modelo.nivelCorredor(0.0))
        assertEquals("Gacela Élite", com.gacelas.comunidad.domain.modelo.nivelCorredor(600.0))
    }

    @Test fun conServidorNoHayEjemplosNiDuplicados() {
        val remota = publicacionDesdeFilaFeed(otra, ahora = ahora)
        val subida = Carrera("x1", "Ya subida", inicioMs = ahora - 3_600_000, segundos = 1800, distanciaKm = 6.0)
        val pendiente = Carrera("p1", "Pendiente", inicioMs = ahora - 60_000, segundos = 600, distanciaKm = 2.0)
        val feed = publicacionesFeed(listOf(subida, pendiente), "Yo", remotas = listOf(remota))
        assertEquals(listOf("carrera-p1", remota.id), feed.map { it.id })
        assertTrue(publicacionesFeed(emptyList(), "Yo", remotas = emptyList()).isEmpty())
        assertEquals(listOf(remota.id), filtrarFeed(feed, FiltroFeed.Siguiendo, emptySet(), setOf("u2")).map { it.id })
    }

    @Test fun eventosSinDuplicar() {
        val base = listOf(Evento("1", "Base", "", "", "", "Loja", 10, 5))
        val ejemplo = listOf(Evento("1", "Ejemplo", "", "", "", "Loja", 3, 5), Evento("9", "Otro", "", "", "", "Loja", 0, 5))
        assertEquals(listOf("Base", "Otro"), unirEventos(base, ejemplo).map { it.titulo })
    }
}
