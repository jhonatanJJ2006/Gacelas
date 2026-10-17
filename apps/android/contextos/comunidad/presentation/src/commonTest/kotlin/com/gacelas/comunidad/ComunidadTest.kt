package com.gacelas.comunidad

import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.modelo.TipoNotificacion
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.NotificacionesRepository
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.comunidad.presentation.EventosViewModel
import com.gacelas.comunidad.presentation.NotificacionesViewModel
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class AjustesMemoria : AjustesRepository {
    override val ajustes = MutableStateFlow(Ajustes())
    override suspend fun actualizar(transformar: (Ajustes) -> Ajustes) { ajustes.value = transformar(ajustes.value) }
}

private class EventosMemoria : EventosRepository {
    override val creados = MutableStateFlow<List<Evento>>(emptyList())
    override val inscritos = MutableStateFlow<Set<String>>(emptySet())
    override suspend fun crear(evento: Evento) { creados.value += evento; inscritos.value += evento.id }
    override suspend fun alternarInscripcion(id: String) { inscritos.value = inscritos.value.let { if (id in it) it - id else it + id } }
}

private fun evento(id: String, inicio: Long) = Evento(id, "E$id", "", "", "", "Loja", 0, 5, inicioMs = inicio)

@OptIn(ExperimentalCoroutinesApi::class)
class ComunidadTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test fun eventosPestanasInscripcionYCrear() = runTest {
        val repo = EventosMemoria()
        val vm = EventosViewModel(repo)
        val lista = listOf(evento("a", 2_000), evento("b", 500))
        assertEquals(listOf("a"), vm.visibles(lista, emptySet(), ahora = 1_000).map { it.id })
        vm.pestana(PestanaEventos.Pasados)
        assertEquals(listOf("b"), vm.visibles(lista, emptySet(), ahora = 1_000).map { it.id })
        vm.alternarInscripcion("a")
        assertEquals(setOf("a"), repo.inscritos.value)
        vm.crear(evento("c", 3_000))
        assertEquals(PestanaEventos.MisEventos, vm.pestana.value)
        assertTrue("c" in repo.inscritos.value)
    }

    @Test fun notificacionesFiltranYMarcan() = runTest {
        val n = listOf(
            Notificacion("1", TipoNotificacion.Like, "Ana", "x", "", false),
            Notificacion("2", TipoNotificacion.EnVivo, "Luis", "y", "", false),
            Notificacion("3", TipoNotificacion.Comentario, "Eva", "z", "", true),
        )
        val fuente = object : NotificacionesRepository {
            override val lista = MutableStateFlow(n)
            var refrescos = 0; val marcadas = mutableListOf<String>()
            override suspend fun refrescar() { refrescos++ }
            override suspend fun marcarLeidas(ids: Collection<String>) { marcadas += ids }
        }
        val ajustes = AjustesMemoria()
        val vm = NotificacionesViewModel(fuente, ajustes, emptyList())
        assertEquals(1, fuente.refrescos)
        assertEquals(3, vm.lista.value.size)
        vm.filtro("En vivo"); assertEquals(listOf("2"), vm.lista.value.map { it.id })
        vm.filtro("Sin leer"); assertEquals(listOf("1", "2"), vm.lista.value.map { it.id })
        vm.marcar(listOf("1"))
        assertEquals(listOf("2"), vm.lista.value.map { it.id })
        ajustes.actualizar { it.copy(notifLikes = false) }
        vm.filtro("Todas"); assertEquals(listOf("2", "3"), vm.lista.value.map { it.id })
        vm.marcarTodas(); assertEquals(listOf("1", "1", "2", "3"), fuente.marcadas)
        assertEquals(2, NotificacionesViewModel(null, AjustesMemoria(), n.take(2)).visibles.value.size)
    }

    @Test fun clubes() = runTest {
        val club = Club("c", "Gacelas Loja", "", "Loja", false, 3)
        val vm = ClubesViewModel(object : ClubesRepository { override suspend fun listar() = listOf(club) to setOf("c") })
        assertEquals(setOf("c"), vm.datos.value?.second)
        assertEquals(emptyList(), ClubesViewModel(null).datos.value?.first)
        val falla = ClubesViewModel(object : ClubesRepository { override suspend fun listar(): Pair<List<Club>, Set<String>> = error("sin red") })
        assertEquals(emptyList(), falla.datos.value?.first)
    }
}
