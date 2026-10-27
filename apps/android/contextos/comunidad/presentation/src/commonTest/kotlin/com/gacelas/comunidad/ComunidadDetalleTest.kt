package com.gacelas.comunidad

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.EstadoEvento
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.FiltroFeed
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.SesionEnVivo
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.domain.repositorio.EnVivoRepository
import com.gacelas.comunidad.presentation.DetalleClubViewModel
import com.gacelas.comunidad.presentation.DetalleEnVivoViewModel
import com.gacelas.comunidad.presentation.DetalleEventoViewModel
import com.gacelas.comunidad.presentation.EventoEnVivoViewModel
import com.gacelas.comunidad.presentation.FeedViewModel
import com.gacelas.comunidad.presentation.ListaEnVivoViewModel
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import com.gacelas.identidad.domain.repositorio.AjustesRepository
import com.gacelas.identidad.domain.repositorio.AuthRepository
import com.gacelas.data.mock.Actividad
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class AjustesEnMemoria : AjustesRepository {
    override val ajustes = MutableStateFlow(Ajustes())
    override suspend fun actualizar(transformar: (Ajustes) -> Ajustes) { ajustes.value = transformar(ajustes.value) }
}

private class CarrerasEnMemoria(c: List<Carrera>) : CarrerasRepository {
    override val carreras = MutableStateFlow(c)
    override suspend fun guardar(carrera: Carrera) { carreras.value += carrera }
    override suspend fun eliminar(id: String) { carreras.value = carreras.value.filter { it.id != id } }
}

private class AuthFija(p: Perfil?) : AuthRepository {
    override val sesion = MutableStateFlow(p)
    override suspend fun iniciarSesion(email: String, password: String) = ResultadoAuth.Error("")
    override suspend fun registrar(nombre: String, email: String, password: String) = ResultadoAuth.Error("")
    override suspend fun recuperarContrasena(email: String) = ResultadoAuth.Error("")
    override suspend fun actualizarPerfil(perfil: Perfil) {}
    override suspend fun cerrarSesion() {}
}

private class EventosEnMemoria : EventosRepository {
    override val creados = MutableStateFlow<List<Evento>>(emptyList())
    override val inscritos = MutableStateFlow<Set<String>>(emptySet())
    override suspend fun crear(evento: Evento) { creados.value += evento }
    override suspend fun alternarInscripcion(id: String) { inscritos.value = inscritos.value.let { if (id in it) it - id else it + id } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComunidadDetalleTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test fun feedFiltraLikesComentariosYOcultas() = runTest {
        val ajustes = AjustesEnMemoria()
        val carrera = Carrera("c1", "Mi fondo", inicioMs = 0, segundos = 1800, distanciaKm = 6.0)
        val actividad = Actividad("1", "1", "Carrera matutina", 5.2, 1714, "5:30", "hace 2h", 23, 5)
        val vm = FeedViewModel(CarrerasEnMemoria(listOf(carrera, carrera.copy(id = "c2", publica = false))), AuthFija(Perfil("a@b.c", "Ana", "ana")), ajustes, listOf(actividad))
        assertEquals(listOf("carrera-c1", "act-1"), vm.publicaciones.value.map { it.id })
        assertEquals("Ana", vm.publicaciones.value.first().autor)
        vm.filtro(FiltroFeed.Mias)
        assertEquals(listOf("carrera-c1"), vm.publicaciones.value.map { it.id })
        vm.filtro(FiltroFeed.Comunidad)
        assertEquals(listOf("act-1"), vm.publicaciones.value.map { it.id })
        vm.alternarLike("act-1"); assertTrue("act-1" in ajustes.ajustes.value.likes)
        vm.alternarLike("act-1"); assertFalse("act-1" in ajustes.ajustes.value.likes)
        vm.comentar("act-1", "  ¡Bien!  "); vm.comentar("act-1", "   ")
        assertEquals(listOf("¡Bien!"), ajustes.ajustes.value.comentarios["act-1"])
        vm.ocultar("act-1")
        assertTrue(vm.publicaciones.value.isEmpty())
    }

    @Test fun detalleEventoInscribeYListaDeEspera() = runTest {
        val eventos = EventosEnMemoria()
        val ejemplo = listOf(Evento("e1", "Fondo", "", "", "", "Loja", 3, 5), Evento("e2", "Otro", "", "", "", "Loja", 0, 10))
        var lleno = false
        val clubes = object : ClubesRepository {
            override suspend fun listar() = emptyList<Club>() to emptySet<String>()
            override suspend fun estadoEvento(id: String) = EstadoEvento(1, 0, 1, if (lleno) "waitlist" else "going", if (lleno) 2 else null)
        }
        val vm = DetalleEventoViewModel("e2", eventos, clubes, ejemplo)
        assertEquals("e2", vm.evento.value.id)
        assertEquals("going", vm.estado.value?.miEstado)
        val avisos = mutableListOf<String>()
        lleno = true
        vm.alternarInscripcion { avisos += it }
        assertEquals("Evento lleno: quedaste en la lista de espera", avisos.last())
        vm.alternarInscripcion { avisos += it }
        assertEquals("Inscripción cancelada", avisos.last())
        assertEquals("e1", DetalleEventoViewModel("no-existe", eventos, null, ejemplo).evento.value.id)
        lleno = false
        val sinServidor = DetalleEventoViewModel("e1", eventos, null, ejemplo)
        assertNull(sinServidor.estado.value)
        sinServidor.alternarInscripcion { avisos += it }
        assertEquals("Inscripción confirmada", avisos.last())
    }

    @Test fun detalleClubAccionesYRecarga() = runTest {
        val miembros = mutableListOf(MiembroClub("yo", "Yo", RolClub.owner, true), MiembroClub("u2", "Luis", RolClub.member, false))
        val hechos = mutableListOf<String>()
        val fuente = object : ClubesRepository {
            override suspend fun listar() = emptyList<Club>() to emptySet<String>()
            override suspend fun miId() = "yo"
            override suspend fun detalle(id: String) = Club(id, "Runcity", "", "Loja", true, miembros.count { it.activo })
            override suspend fun miembros(id: String) = miembros.toList()
            override suspend fun aprobar(id: String, usuario: String) { hechos += "aprobar:$usuario"; miembros[1] = miembros[1].copy(activo = true) }
            override suspend fun cambiarRol(id: String, usuario: String, rol: RolClub) { hechos += "rol:$usuario:$rol" }
            override suspend fun publicar(id: String, texto: String) { hechos += "post:$texto" }
            override suspend fun salir(id: String, usuario: String?) { error("sin red") }
        }
        val vm = DetalleClubViewModel("c", fuente)
        assertTrue(vm.estado.value.gestiono)
        assertEquals(1, vm.estado.value.club?.miembros)
        val avisos = mutableListOf<String>()
        vm.aprobar("u2") { avisos += it }
        assertEquals(2, vm.estado.value.club?.miembros)
        vm.alternarAdmin(miembros[1]) { avisos += it }
        vm.escribir("Hola"); vm.publicar { avisos += it }
        assertEquals("", vm.texto.value)
        vm.accionPrincipal { avisos += it }
        assertEquals(listOf("aprobar:u2", "rol:u2:admin", "post:Hola"), hechos)
        assertEquals(listOf("Solicitud aprobada", "Rol actualizado", "Publicado", "sin red"), avisos)
        assertNull(DetalleClubViewModel("c", null).estado.value.club)
    }

    @Test fun eventoEnVivoClasificaYRepite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val corredores = listOf(
            CorredorEvento("a", "A", 1000.0, 400, null, null, false, false, emptyList()),
            CorredorEvento("b", "B", 3000.0, 900, null, null, false, false, listOf(0.0 to 0.0, 1.0 to 1.0)),
        )
        val enVivo = object : EnVivoRepository {
            override var eventoId: String? = null
            override val sesionId: String? = null
            override val visible: Boolean? = null
            override suspend fun iniciar(): String? = null
            override suspend fun enviar(lat: Double, lng: Double, distanciaKm: Double, segundos: Int) = false
            override suspend fun terminar() {}
            override suspend fun listar() = emptyList<SesionEnVivo>()
            override suspend fun leer(id: String): SesionEnVivo? = null
        }
        val vm = EventoEnVivoViewModel("ev", object : ClubesRepository {
            override suspend fun listar() = emptyList<Club>() to emptySet<String>()
            override suspend fun corredores(id: String) = corredores
        }, enVivo)
        advanceTimeBy(10)
        assertEquals(listOf("b", "a"), vm.tabla.value.map { it.usuarioId })
        vm.alternarRepeticion()
        assertEquals(0f, vm.repeticion.value)
        advanceTimeBy(81 * 10)
        assertTrue((vm.repeticion.value ?: 0f) > 0.05f)
        vm.mover(1f); vm.alternarRepeticion()
        assertEquals(0f, vm.repeticion.value)
        vm.mover(null)
        assertNull(vm.repeticion.value)
        vm.correrEnEvento()
        assertEquals("ev", enVivo.eventoId)
        vm.viewModelScope.cancel()
    }

    @Test fun enVivoSinServidor() = runTest {
        assertEquals(emptyList(), ListaEnVivoViewModel(null).sesiones.value)
        val d = DetalleEnVivoViewModel("x", null)
        assertTrue(d.cargado.value)
        assertNull(d.sesion.value)
        val terminada = SesionEnVivo("x", "u", "Ana", true, 0, 10, false, 10, null, emptyList(), 1.0, 60)
        val fuente = object : EnVivoRepository {
            override var eventoId: String? = null
            override val sesionId: String? = null
            override val visible: Boolean? = null
            override suspend fun iniciar(): String? = null
            override suspend fun enviar(lat: Double, lng: Double, distanciaKm: Double, segundos: Int) = false
            override suspend fun terminar() {}
            override suspend fun listar() = listOf(terminada)
            override suspend fun leer(id: String) = terminada
        }
        val lista = ListaEnVivoViewModel(fuente)
        assertEquals(listOf("x"), lista.sesiones.value?.map { it.id })
        lista.viewModelScope.cancel()
        assertEquals("Ana", DetalleEnVivoViewModel("x", fuente).sesion.value?.nombre)
    }
}
