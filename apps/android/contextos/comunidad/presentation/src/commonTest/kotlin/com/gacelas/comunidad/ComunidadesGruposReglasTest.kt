package com.gacelas.comunidad

import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.NivelGrupo
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.TipoClub
import com.gacelas.comunidad.domain.reglas.accionClub
import com.gacelas.comunidad.domain.reglas.borradorDesdeClub
import com.gacelas.comunidad.domain.reglas.estaLleno
import com.gacelas.comunidad.domain.reglas.filtrarClubes
import com.gacelas.comunidad.domain.reglas.leerRitmo
import com.gacelas.comunidad.domain.reglas.mensajeErrorClub
import com.gacelas.comunidad.domain.reglas.textoHorario
import com.gacelas.comunidad.domain.reglas.textoRitmo
import com.gacelas.comunidad.domain.reglas.validarClub
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.presentation.ClubesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComunidadesGruposReglasTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    private val jipiro = Club("g1", "Gacelas del Jipiro", "Fondos", "Loja", false, 14, dias = listOf(2, 6), hora = "06:00",
        nivel = NivelGrupo.principiante, ritmoSegKm = 390, cupo = 15, lugarEncuentro = "Parque Jipiro")
    private val comunidad = Club("c1", "Runners Loja", "Abierta", "Loja", false, 120, tipo = TipoClub.comunidad)

    @Test fun validacion() {
        assertNull(validarClub(BorradorClub(nombre = "Trail Villonaco", ritmo = "6:00", cupo = "10", hora = "05:30"), listOf(jipiro)))
        assertTrue(validarClub(BorradorClub(nombre = "gacelas  del JIPIRO"), listOf(jipiro))!!.contains("Ya existe"))
        assertNull(validarClub(BorradorClub(nombre = "Gacelas del Jipiro"), listOf(jipiro), editandoId = "g1"))
        assertNull(validarClub(BorradorClub(tipo = TipoClub.comunidad, nombre = "Gacelas del Jipiro"), listOf(jipiro)))
        assertTrue(validarClub(BorradorClub(nombre = "Uno", ritmo = "1:00"), emptyList())!!.contains("ritmo"))
        assertTrue(validarClub(BorradorClub(nombre = "Uno", cupo = "1"), emptyList())!!.contains("cupo"))
        assertTrue(validarClub(BorradorClub(nombre = "Uno", hora = "25:00"), emptyList())!!.contains("hora"))
        assertTrue(validarClub(BorradorClub(nombre = "Uno", ciudad = " "), emptyList())!!.contains("ciudad"))
    }

    @Test fun textos() {
        assertEquals("Mar y Sáb · 06:00", textoHorario(listOf(6, 2), "06:00:00"))
        assertEquals("Lun, Mié y Vie", textoHorario(listOf(1, 3, 5), null))
        assertEquals("Sin horario fijo", textoHorario(emptyList(), null))
        assertEquals("6:30 min/km", textoRitmo(390))
        assertEquals(390, leerRitmo("6:30"))
        assertNull(leerRitmo("rapido"))
        assertEquals("Ya existe uno con ese nombre", mensajeErrorClub("duplicate key value violates unique constraint \"clubs_nombre_unico\""))
    }

    @Test fun accionSegunTipoYCupo() {
        val yo = MiembroClub("a", "A", RolClub.member, true)
        assertEquals("Salir del grupo", accionClub(yo, false))
        assertEquals("Salir de la comunidad", accionClub(yo, false, TipoClub.comunidad))
        assertEquals("Grupo lleno", accionClub(null, false, TipoClub.grupo, lleno = true))
        assertTrue(estaLleno(jipiro.copy(miembros = 15)))
    }

    @Test fun filtrosYBorrador() {
        val todos = listOf(jipiro, comunidad, jipiro.copy(id = "g2", nombre = "Trail Villonaco", nivel = NivelGrupo.avanzado, lugarEncuentro = "Villonaco"))
        assertEquals(listOf("g1", "g2"), filtrarClubes(todos, TipoClub.grupo, "").map { it.id })
        assertEquals(listOf("g2"), filtrarClubes(todos, TipoClub.grupo, "villo").map { it.id })
        assertEquals(listOf("g1"), filtrarClubes(todos, TipoClub.grupo, "", NivelGrupo.principiante).map { it.id })
        assertEquals(listOf("c1"), filtrarClubes(todos, TipoClub.comunidad, "loja").map { it.id })
        val b = borradorDesdeClub(jipiro)
        assertEquals("6:30", b.ritmo); assertEquals("15", b.cupo); assertEquals("06:00", b.hora)
    }

    @Test fun viewModelRegistraYAvisaErrores() {
        var creado: BorradorClub? = null
        val vm = ClubesViewModel(object : ClubesRepository {
            override suspend fun listar() = listOf(jipiro) to emptySet<String>()
            override suspend fun crear(b: BorradorClub): String { creado = b; return "nuevo" }
        })
        var id: String? = null
        var error: String? = null
        vm.crear(BorradorClub(nombre = "Gacelas del Jipiro"), { id = it }, { error = it })
        assertTrue(error!!.contains("Ya existe")); assertNull(creado)
        vm.crear(BorradorClub(nombre = "Trail Villonaco"), { id = it }, { error = it })
        assertEquals("nuevo", id); assertEquals("Trail Villonaco", creado?.nombre)
    }
}
