package com.gacelas

import com.gacelas.comunidad.data.ClubesRepository
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.EstadoEvento
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.reglas.accionClub
import com.gacelas.comunidad.domain.reglas.calendarioMes
import com.gacelas.comunidad.domain.reglas.clasificacion
import com.gacelas.comunidad.domain.reglas.posicionRepeticion
import com.gacelas.comunidad.domain.reglas.textoCupo
import com.gacelas.comunidad.domain.reglas.validarClub
import com.gacelas.comunidad.domain.modelo.Evento
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Fase5Test {
    private fun k(id: String, km: Double, s: Int, n: Int = 0) =
        CorredorEvento(id, id, km * 1000, s, null, null, false, false, (0 until n).map { it.toDouble() to it.toDouble() })

    @Test fun botonDelClub() {
        assertEquals("Unirme", accionClub(null, false))
        assertEquals("Solicitar unirme", accionClub(null, true))
        assertEquals("Solicitud enviada", accionClub(MiembroClub("a", "A", RolClub.member, false), true))
        assertEquals("Salir del club", accionClub(MiembroClub("a", "A", RolClub.admin, true), false))
        assertNull(accionClub(MiembroClub("a", "A", RolClub.owner, true), false))
        assertTrue(validarClub("ab", "")!!.contains("3 letras"))
    }

    @Test fun cupoYEspera() {
        assertEquals("3 inscritos", textoCupo(EstadoEvento(3, 0, null, null, null)))
        assertEquals("9/10 · 1 cupo libre", textoCupo(EstadoEvento(9, 0, 10, null, null)))
        assertEquals("Lleno · 2 en espera", textoCupo(EstadoEvento(10, 2, 10, "waitlist", 2)))
    }

    @Test fun calendarioDeLunesADomingo() {
        val tz = java.util.TimeZone.getTimeZone("America/Guayaquil")
        val inicio = Calendar.getInstance(tz).apply { clear(); set(2026, 9, 15, 7, 0) }.timeInMillis
        val e = Evento("x", "X", "", "", "", "", 0, 5, inicioMs = inicio)
        val semanas = calendarioMes(2026, 9, listOf(e), kotlinx.datetime.TimeZone.of("America/Guayaquil"))
        assertEquals(31, semanas.flatten().count { it.delMes })
        val primero = Calendar.getInstance(tz).apply { timeInMillis = semanas[0][0].inicioMs }
        assertEquals(Calendar.MONDAY, primero[Calendar.DAY_OF_WEEK])
        assertEquals(listOf(e), semanas.flatten().first { it.delMes && it.dia == 15 }.eventos)
    }

    @Test fun clasificacionYRepeticion() {
        assertEquals(listOf("c", "b", "a"), clasificacion(listOf(k("a", 3.0, 900), k("b", 5.0, 1500), k("c", 5.0, 1400))).map { it.usuarioId })
        assertEquals(5.0 to 5.0, posicionRepeticion(k("a", 1.0, 1, 11), 0.5f))
        assertNull(posicionRepeticion(k("b", 1.0, 1), 0.5f))
    }

    @Test fun unirseAUnClubGuardaLaMembresia() = runBlocking {
        val b = FakeBackend().apply { actual = com.gacelas.data.remoto.UsuarioRemoto("u-1", "a@b.c") }
        val repo = ClubesRepository(b) { "u-1" }
        val id = repo.crear("Gacelas Loja", "Club", "Loja", false)
        repo.unirse(id)
        assertEquals(listOf("u-1"), b.select("club_members", mapOf("club_id" to id)).map { (it["user_id"] as kotlinx.serialization.json.JsonPrimitive).content })
    }
}
