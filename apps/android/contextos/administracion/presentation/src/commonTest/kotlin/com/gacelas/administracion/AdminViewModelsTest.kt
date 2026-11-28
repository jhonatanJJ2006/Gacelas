package com.gacelas.administracion

import com.gacelas.administracion.application.ConsultarAccesoAdmin
import com.gacelas.administracion.application.ModerarReporte
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.CLAVE_VERSION_MINIMA
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.infrastructure.AdminRepositorioMemoria
import com.gacelas.administracion.presentation.AjustesAdminViewModel
import com.gacelas.administracion.presentation.Carga
import com.gacelas.administracion.presentation.DashboardAdminViewModel
import com.gacelas.administracion.presentation.EstadisticasAdminViewModel
import com.gacelas.administracion.presentation.EventosAdminViewModel
import com.gacelas.administracion.presentation.ModeracionAdminViewModel
import com.gacelas.administracion.presentation.RangoEstadisticas
import com.gacelas.administracion.presentation.RutasClubesAdminViewModel
import com.gacelas.administracion.presentation.UsuariosAdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.gacelas.administracion.domain.CLAVE_SLA
import com.gacelas.administracion.domain.Prioridad
import com.gacelas.administracion.domain.RespuestaRapida
import com.gacelas.administracion.domain.SlaHoras
import com.gacelas.administracion.presentation.EscenarioAdminViewModel
import com.gacelas.administracion.presentation.OperacionAdminViewModel
import com.gacelas.administracion.presentation.RespuestasAdminViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelsTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    private fun <T> Carga<T>.datos(): T = assertIs<Carga.Listo<T>>(this).datos

    @Test
    fun accesoFallaCerrado() = runTest {
        val repo = AdminRepositorioMemoria(RolAdmin.Admin).apply { fallar = true }
        assertNull(ConsultarAccesoAdmin(repo)().rol)
        repo.fallar = false
        repo.rol = null
        assertTrue(!ConsultarAccesoAdmin(repo)().puedeEntrar)
        repo.rol = RolAdmin.Moderador
        assertEquals(RolAdmin.Moderador, ConsultarAccesoAdmin(repo)().rol)
    }

    @Test
    fun dashboardMuestraErrorYReintenta() = runTest {
        val repo = AdminRepositorioMemoria().apply { fallar = true }
        val vm = DashboardAdminViewModel(repo)
        assertIs<Carga.Error>(vm.estado.value)
        repo.fallar = false
        vm.recargar()
        assertEquals(2418, vm.estado.value.datos().usuarios)
    }

    @Test
    fun moderadorNoCargaDashboard() = runTest {
        val vm = DashboardAdminViewModel(AdminRepositorioMemoria(RolAdmin.Moderador))
        assertIs<Carga.Error>(vm.estado.value)
    }

    @Test
    fun usuariosBuscaSuspendeYCambiaRol() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = UsuariosAdminViewModel(repo)
        vm.buscar("kevin")
        val kevin = vm.filtrar(vm.estado.value.datos()).single()
        vm.suspender(kevin)
        assertTrue(repo.usuarios.first { it.id == kevin.id }.suspendido)
        assertEquals("Cuenta suspendida", vm.mensaje.value)
        vm.cambiarRol(kevin, RolAdmin.Moderador)
        assertEquals(RolAdmin.Moderador, repo.usuarios.first { it.id == kevin.id }.rol)
    }

    @Test
    fun moderacionFiltraYAplicaAccionConNota() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = ModeracionAdminViewModel(repo)
        // Pendientes ordenados por vencimiento del plazo: r3 vence antes que r1.
        assertEquals(listOf("r3", "r1"), vm.estado.value.datos().map { it.id })
        vm.elegirTipo("comentario")
        assertEquals(listOf("r3"), vm.estado.value.datos().map { it.id })
        val r3 = vm.estado.value.datos().single()
        vm.escribirNota("r3", "  insulto  ")
        vm.moderar(r3, AccionModeracion.OcultarContenido)
        assertEquals(Triple("r3", AccionModeracion.OcultarContenido, "insulto"), repo.moderaciones.single())
        assertTrue(vm.estado.value.datos().isEmpty())
        vm.elegirEstado(EstadoReporte.Resuelto)
        assertEquals(listOf("r3"), vm.estado.value.datos().map { it.id })
    }

    @Test
    fun soloNotaExigeTexto() = runTest {
        val repo = AdminRepositorioMemoria()
        assertFailsWith<IllegalArgumentException> { ModerarReporte(repo)(repo.reportes.first(), AccionModeracion.Nota, " ") }
    }

    @Test
    fun eventosPublicaYCrea() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = EventosAdminViewModel(repo)
        vm.publicar(repo.eventos.first { it.id == "e2" })
        assertTrue(repo.eventos.first { it.id == "e2" }.publicado)
        vm.guardar(com.gacelas.administracion.domain.EventoAdmin(null, "   "))
        assertEquals("El evento necesita un título", vm.mensaje.value)
        vm.guardar(com.gacelas.administracion.domain.EventoAdmin(null, " Nocturna "))
        assertEquals("Nocturna", vm.estado.value.datos().first().titulo)
    }

    @Test
    fun rutasYClubesSeBorran() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = RutasClubesAdminViewModel(repo)
        vm.borrarRuta(vm.estado.value.datos().rutas.first())
        vm.borrarClub(vm.estado.value.datos().clubes.first())
        assertEquals(1, vm.estado.value.datos().rutas.size)
        assertEquals(1, vm.estado.value.datos().clubes.size)
    }

    @Test
    fun ajustesValidaVersionYGuardaInterruptor() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = AjustesAdminViewModel(repo)
        vm.guardarVersionMinima("2.0")
        assertEquals("Usa el formato 1.2.3", vm.mensaje.value)
        vm.guardarVersionMinima("2.1.0")
        assertEquals(JsonPrimitive("2.1.0"), repo.ajustes.first { it.clave == CLAVE_VERSION_MINIMA }.valor)
        vm.alternar(repo.ajustes.first { it.clave == "mantenimiento" }, true)
        assertEquals(JsonPrimitive(true), repo.ajustes.first { it.clave == "mantenimiento" }.valor)
    }

    @Test
    fun estadisticasUsaElRango() = runTest {
        val vm = EstadisticasAdminViewModel(AdminRepositorioMemoria()) { LocalDate(2026, 10, 31) }
        assertEquals("2026-10-01", vm.estado.value.datos().desde)
        vm.elegir(RangoEstadisticas.Semana)
        assertEquals("2026-10-24", vm.estado.value.datos().desde)
    }

    @Test
    fun colaDeModeracionOrdenadaPorPlazoYConRespuestas() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = ModeracionAdminViewModel(repo)
        vm.elegirEstado(EstadoReporte.Pendiente)
        // r3 vence antes (29/10) que r1 (02/11).
        assertEquals(listOf("r3", "r1"), vm.estado.value.datos().map { it.id })
        assertEquals(3, vm.respuestas.value.size)
        val r = vm.estado.value.datos().first()
        vm.usarRespuesta(r.id, vm.respuestas.value.first())
        assertEquals(vm.respuestas.value.first().texto, vm.notas.value[r.id])
        vm.cambiarPrioridad(r, Prioridad.Alta)
        assertEquals(Prioridad.Alta, repo.reportes.first { it.id == r.id }.prioridad)
        assertEquals("Prioridad actualizada", vm.mensaje.value)
    }

    @Test
    fun moderadorVeLasRespuestasPeroNoLasEdita() = runTest {
        val repo = AdminRepositorioMemoria(RolAdmin.Moderador)
        assertEquals(3, ModeracionAdminViewModel(repo).respuestas.value.size)
        val vm = RespuestasAdminViewModel(repo)
        vm.guardar(RespuestaRapida(null, "Nueva", "Texto"))
        assertEquals("solo administradores", vm.mensaje.value)
        assertEquals(3, repo.respuestas.size)
    }

    @Test
    fun respuestasSeValidanAntesDeGuardar() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = RespuestasAdminViewModel(repo)
        vm.guardar(RespuestaRapida(null, "", "Texto"))
        assertEquals("Escribe un título", vm.mensaje.value)
        vm.guardar(RespuestaRapida(null, "  Duplicado  ", "  Ya hay un caso abierto por esto.  "))
        assertEquals("Respuesta guardada", vm.mensaje.value)
        assertTrue(vm.estado.value.datos().any { it.titulo == "Duplicado" && it.texto == "Ya hay un caso abierto por esto." })
        val q1 = vm.estado.value.datos().first { it.id == "q1" }
        vm.borrar(q1)
        assertTrue(vm.estado.value.datos().none { it.id == "q1" })
    }

    @Test
    fun plazosSeGuardanSoloConOrdenValido() = runTest {
        val repo = AdminRepositorioMemoria()
        val vm = AjustesAdminViewModel(repo)
        vm.guardarSla(SlaHoras(48, 24, 72))
        assertEquals("La prioridad alta debe tener el plazo más corto", vm.mensaje.value)
        vm.guardarSla(SlaHoras(2, 12, 48))
        assertEquals("Plazos guardados", vm.mensaje.value)
        assertEquals(SlaHoras(2, 12, 48), SlaHoras.desde(repo.ajustes))
        assertTrue(repo.ajustes.count { it.clave == CLAVE_SLA } == 1)
    }

    @Test
    fun escenarioYOperacionSoloParaAdmin() = runTest {
        val repo = AdminRepositorioMemoria()
        val esc = EscenarioAdminViewModel(repo).estado.value.datos()
        assertEquals(1, esc.sosActivas.size)
        assertEquals(2, esc.sos.size)
        val op = OperacionAdminViewModel(repo).estado.value.datos()
        assertEquals("1.0.0", op.versionMinima)
        assertEquals(3, op.publicadas.size)
        assertEquals(2, op.importaciones.size)
        assertIs<Carga.Error>(EscenarioAdminViewModel(AdminRepositorioMemoria(RolAdmin.Moderador)).estado.value)
    }
}
