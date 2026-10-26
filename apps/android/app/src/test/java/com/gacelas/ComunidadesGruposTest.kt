package com.gacelas

import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.NivelGrupo
import com.gacelas.comunidad.domain.modelo.TipoClub
import com.gacelas.comunidad.infrastructure.SupabaseClubesRepository
import com.gacelas.comunidad.infrastructure.clubDesdeFila
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.moderacion.domain.TipoContenido
import com.gacelas.moderacion.domain.motivosReporte
import com.gacelas.moderacion.domain.tablaDenuncia
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Registro de comunidades y grupos de running contra el backend falso (migracion 034). */
class ComunidadesGruposTest {
    private val grupo = BorradorClub(
        tipo = TipoClub.grupo, nombre = "Gacelas del Jipiro", descripcion = "Fondos suaves", lugarEncuentro = "Parque Jipiro",
        lat = -3.97, lng = -79.2, dias = listOf(6, 2), hora = "06:00", nivel = NivelGrupo.principiante, ritmo = "6:30", cupo = "15",
    )

    @Test fun registrarGrupoGuardaSusDatos(): Unit = runBlocking {
        val b = FakeBackend()
        val repo = SupabaseClubesRepository(b) { "u-1" }
        val id = repo.crear(grupo)
        val fila = b.select("clubs", mapOf("id" to id)).single()
        assertEquals("grupo", (fila["kind"] as JsonPrimitive).content)
        assertEquals("u-1", (fila["owner_id"] as JsonPrimitive).content)
        val c = clubDesdeFila(fila)
        assertEquals(listOf(2, 6), c.dias)
        assertEquals(390, c.ritmoSegKm)
        assertEquals(15, c.cupo)
        assertEquals(NivelGrupo.principiante, c.nivel)
        assertEquals("Parque Jipiro", c.lugarEncuentro)
    }

    @Test fun nombreRepetidoSeRechazaSinImportarMayusculas(): Unit = runBlocking {
        val repo = SupabaseClubesRepository(FakeBackend()) { "u-1" }
        repo.crear(grupo)
        try { repo.crear(grupo.copy(nombre = "  gacelas   DEL jipiro ")); fail("debia fallar") } catch (e: ErrorRemoto) { assertTrue(e.message!!.contains("Ya existe un grupo")) }
        // Una comunidad puede llamarse igual que un grupo.
        repo.crear(BorradorClub(tipo = TipoClub.comunidad, nombre = "Gacelas del Jipiro"))
    }

    @Test fun comunidadNoGuardaCamposDeGrupo(): Unit = runBlocking {
        val b = FakeBackend()
        val id = SupabaseClubesRepository(b) { "u-1" }.crear(grupo.copy(tipo = TipoClub.comunidad, nombre = "Runners Loja"))
        val c = clubDesdeFila(b.select("clubs", mapOf("id" to id)).single())
        assertEquals(TipoClub.comunidad, c.tipo)
        assertNull(c.ritmoSegKm); assertNull(c.cupo); assertEquals(emptyList<Int>(), c.dias)
    }

    @Test fun editarYEliminar(): Unit = runBlocking {
        val b = FakeBackend()
        val repo = SupabaseClubesRepository(b) { "u-1" }
        val id = repo.crear(grupo)
        repo.editar(id, grupo.copy(hora = "05:30", cupo = ""))
        val c = repo.detalle(id)!!
        assertEquals("05:30", c.hora)
        assertNull(c.cupo)
        repo.eliminar(id)
        assertNull(repo.detalle(id))
    }

    @Test fun comunidadesYGruposSeDenuncianEnLaCola() {
        assertEquals("content_reports", tablaDenuncia(TipoContenido.Grupo.objetivo("g")))
        assertEquals("comunidad", TipoContenido.Comunidad.objetivo("c").tipo)
        assertTrue(motivosReporte("grupo").any { it.first == "peligroso" })
    }
}
